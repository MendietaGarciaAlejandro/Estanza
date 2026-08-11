package io.github.mendietagarciaalejandro.estanza.red

import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.datos.comoParametro
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import io.github.mendietagarciaalejandro.estanza.sesion.Sesion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Todas las llamadas a Camar pasan por aqui.
 *
 * La direccion se lee de [AjustesDeConexion] en cada peticion y no al construir el cliente,
 * porque el usuario puede cambiarla en marcha desde la pantalla de ajustes.
 */
class ApiDeCamar(
    private val cliente: HttpClient,
    private val ajustes: AjustesDeConexion,
    private val sesiones: AlmacenDeSesion,
) {
    suspend fun registrar(datos: PeticionDeAlta): Respuesta<Sesion> =
        pedir<RespuestaDeAutenticacion> {
            post(url("/api/auth/register")) { cuerpo(datos) }
        }.map(::aSesion)

    suspend fun acceder(email: String, contrasena: String): Respuesta<Sesion> =
        pedir<RespuestaDeAutenticacion> {
            post(url("/api/auth/login")) { cuerpo(PeticionDeAcceso(email, contrasena)) }
        }.map(::aSesion)

    suspend fun recursos(): Respuesta<List<RecursoDto>> =
        pedir { get(url("/api/resources")) { autenticar() } }

    /**
     * Huecos de media hora libres de un recurso en una fecha.
     *
     * La fecha va como parametro con el formato que espera un DateOnly de .NET; si se manda
     * otra cosa Camar contesta 400 con el campo "date" en los errores.
     */
    suspend fun disponibilidad(idRecurso: String, fecha: LocalDate): Respuesta<DisponibilidadDto> =
        pedir {
            get(url("/api/resources/$idRecurso/availability")) {
                parameter("date", fecha.comoParametro())
                autenticar()
            }
        }

    /**
     * Comprueba si al otro lado de la direccion configurada hay un Camar.
     *
     * Un 401 cuenta como exito: significa que el servidor esta ahi y ha entendido la
     * peticion, solo que no hay sesion. Es justo lo que hace falta saber al configurar la
     * direccion, antes de tener credenciales.
     */
    suspend fun comprobarConexion(): Respuesta<Unit> = when (val resultado = recursos()) {
        is Respuesta.Exito -> Respuesta.Exito(Unit)
        is Respuesta.Fallo -> when (resultado.error) {
            is ErrorDeApi.NoAutorizado -> Respuesta.Exito(Unit)
            else -> resultado
        }
    }

    /**
     * Crea la reserva. Los 409 y los 422 que devuelve aqui son la parte interesante: el
     * primero es que alguien se ha adelantado, el segundo que la reserva no cumple alguna
     * norma del coworking (duracion, horario, antelacion del plan).
     */
    suspend fun crearReserva(idRecurso: String, inicio: Instant, fin: Instant): Respuesta<ReservaDto> =
        pedir {
            post(url("/api/reservations")) {
                autenticar()
                cuerpo(PeticionDeReserva(idRecurso, inicio.toString(), fin.toString()))
            }
        }

    suspend fun misReservas(): Respuesta<List<ReservaDto>> =
        pedir { get(url("/api/reservations")) { autenticar() } }

    /** Devuelve la reserva ya cancelada, con el reembolso que le corresponda. */
    suspend fun cancelarReserva(id: String): Respuesta<ReservaDto> =
        pedir { post(url("/api/reservations/$id/cancel")) { autenticar() } }

    private fun url(ruta: String) = ajustes.urlBase.value + ruta

    private fun HttpRequestBuilder.cuerpo(valor: Any) {
        contentType(ContentType.Application.Json)
        setBody(valor)
    }

    /**
     * Se pone la cabecera a mano en vez de usar el plugin Auth de Ktor: ese esta pensado
     * para pares de token de acceso y de refresco, y Camar no tiene endpoint de refresco.
     * Cuando el token caduca hay que volver a entrar, y punto.
     */
    private fun HttpRequestBuilder.autenticar() {
        sesiones.vigente?.let { bearerAuth(it.token) }
    }

    private fun aSesion(respuesta: RespuestaDeAutenticacion) = Sesion(
        token = respuesta.token,
        caducaEn = Instant.parse(respuesta.caducaEn),
        idUsuario = respuesta.idUsuario,
        rol = respuesta.rol,
    )

    private suspend inline fun <reified T> pedir(
        crossinline peticion: suspend HttpClient.() -> HttpResponse,
    ): Respuesta<T> {
        val respuesta = try {
            cliente.peticion()
        } catch (cancelacion: CancellationException) {
            // Cancelar una corrutina no es un fallo de red: tiene que seguir subiendo.
            throw cancelacion
        } catch (fallo: Exception) {
            return Respuesta.Fallo(ErrorDeApi.SinConexion(fallo.message))
        }

        if (!respuesta.status.isSuccess()) return Respuesta.Fallo(traducir(respuesta))

        return try {
            Respuesta.Exito(respuesta.body())
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (fallo: Exception) {
            // Contesto 2xx pero con algo que no encaja: normalmente es que la URL apunta a
            // otro servidor cualquiera que devuelve su pagina de inicio.
            Respuesta.Fallo(ErrorDeApi.RespuestaIlegible(fallo.message))
        }
    }

    /**
     * Convierte la respuesta de error en uno de los casos de [ErrorDeApi].
     *
     * El detalle que trae Camar esta escrito para leerse ("Ese documento fiscal ya esta
     * registrado."), asi que se usa tal cual y solo se inventa un texto cuando no viene.
     */
    private suspend fun traducir(respuesta: HttpResponse): ErrorDeApi {
        val problema = try {
            respuesta.body<DetallesDelProblema>()
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (_: Exception) {
            null
        }

        val detalle = problema?.detail?.takeIf { it.isNotBlank() }
        val codigo = respuesta.status.value

        return when (codigo) {
            400 -> problema?.errors
                ?.takeIf { it.isNotEmpty() }
                ?.let(ErrorDeApi::DatosInvalidos)
                ?: ErrorDeApi.DelServidor(400, detalle ?: "La peticion no es valida.")

            401 -> {
                // Si habia sesion guardada y el servidor la rechaza, ya no sirve para nada:
                // se tira aqui para que la aplicacion vuelva sola a la pantalla de acceso en
                // vez de dejar al usuario dentro viendo pantallas que fallan una tras otra.
                if (sesiones.sesion.value != null) sesiones.cerrar()

                ErrorDeApi.NoAutorizado(detalle ?: "Tienes que iniciar sesion.")
            }
            403 -> ErrorDeApi.Prohibido(detalle ?: "No tienes permiso para hacer esto.")
            404 -> ErrorDeApi.NoEncontrado(detalle ?: "Eso ya no esta.")
            409 -> ErrorDeApi.Conflicto(detalle ?: "Alguien se ha adelantado.")
            422 -> ErrorDeApi.ReglaDeNegocio(detalle ?: "Eso no cumple las normas del coworking.")

            else -> ErrorDeApi.DelServidor(codigo, detalle ?: "El servidor ha respondido $codigo.")
        }
    }
}
