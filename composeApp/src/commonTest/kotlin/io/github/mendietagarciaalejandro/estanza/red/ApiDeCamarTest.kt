package io.github.mendietagarciaalejandro.estanza.red

import com.russhwolf.settings.MapSettings
import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.plataforma.Plataforma
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import io.github.mendietagarciaalejandro.estanza.sesion.Sesion
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private object PlataformaDePrueba : Plataforma {
    override val nombre = "test"
    override val urlBasePorDefecto = "http://localhost:5106"
}

private class RelojFijo(private val momento: Instant) : Clock {
    override fun now() = momento
}

private val AHORA = Instant.parse("2026-01-12T08:00:00Z")

/**
 * Se prueba contra MockEngine y no contra un Camar de verdad: lo que interesa aqui es la
 * traduccion de las respuestas, y para eso hace falta poder devolver un 409 a voluntad.
 * Que el servidor conteste lo que dice su contrato ya lo comprueban los tests de Camar.
 */
class ApiDeCamarTest {

    private var peticiones = mutableListOf<HttpRequestData>()

    private fun api(
        sesion: Sesion? = null,
        urlGuardada: String? = null,
        responder: MockRequestHandler,
    ): ApiDeCamar {
        val preferencias = MapSettings()
        if (urlGuardada != null) preferencias.putString("url_base_api", urlGuardada)

        val sesiones = AlmacenDeSesion(preferencias, RelojFijo(AHORA))
        if (sesion != null) sesiones.abrir(sesion)

        val motor = MockEngine { peticion ->
            peticiones += peticion
            responder(peticion)
        }

        return ApiDeCamar(
            cliente = crearClienteHttp(motor),
            ajustes = AjustesDeConexion(preferencias, PlataformaDePrueba),
            sesiones = sesiones,
        )
    }

    private fun MockRequestHandleScope.problema(estado: HttpStatusCode, detalle: String) = respondError(
        status = estado,
        content = """{"type":"about:blank","title":"Vaya","status":${estado.value},""" +
            """"detail":"$detalle","traceId":"00-abc-def-01"}""",
        // Camar responde con problem+json, no con json a secas.
        headers = headersOf(HttpHeaders.ContentType, "application/problem+json; charset=utf-8"),
    )

    @Test
    fun acceso_correcto_devuelveLaSesion() = runTest {
        // La fecha va copiada tal cual de una respuesta de Camar, con sus siete decimales
        // de segundo: .NET los escribe asi y hay que asegurarse de que se saben leer.
        val api = api {
            respond(
                content = """{"token":"jwt.de.prueba","expiresAt":"2026-01-12T09:00:00.8979484+00:00",""" +
                    """"userId":"3f2504e0-4f89-11d3-9a0c-0305e82c3301","role":"Member"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val respuesta = api.acceder("socio@camar.test", "clave1234")

        val sesion = assertIs<Respuesta.Exito<Sesion>>(respuesta).valor
        assertEquals("jwt.de.prueba", sesion.token)
        assertEquals(Instant.parse("2026-01-12T09:00:00.8979484Z"), sesion.caducaEn)
        assertEquals("Member", sesion.rol)
        assertTrue(sesion.vigenteEn(AHORA))
    }

    @Test
    fun acceso_conCredencialesMalas_da401ConSuMensaje() = runTest {
        val api = api { problema(HttpStatusCode.Unauthorized, "Email o contrasena incorrectos.") }

        val error = assertIs<Respuesta.Fallo>(api.acceder("a@b.c", "loquesea")).error

        assertIs<ErrorDeApi.NoAutorizado>(error)
        assertEquals("Email o contrasena incorrectos.", error.mensaje)
    }

    @Test
    fun alta_conDocumentoRepetido_da409() = runTest {
        val api = api { problema(HttpStatusCode.Conflict, "Ese documento fiscal ya esta registrado.") }

        val error = assertIs<Respuesta.Fallo>(api.registrar(alta())).error

        assertIs<ErrorDeApi.Conflicto>(error)
        assertEquals("Ese documento fiscal ya esta registrado.", error.mensaje)
    }

    @Test
    fun alta_conNifInventado_da422() = runTest {
        val api = api {
            problema(HttpStatusCode.UnprocessableEntity, "La letra del NIF no corresponde.")
        }

        val error = assertIs<Respuesta.Fallo>(api.registrar(alta())).error

        assertIs<ErrorDeApi.ReglaDeNegocio>(error)
        assertEquals("La letra del NIF no corresponde.", error.mensaje)
    }

    @Test
    fun alta_conEmailMalFormado_devuelveLosErroresPorCampo() = runTest {
        // Esto lo genera [ApiController] con los atributos del request, antes de llegar al
        // dominio, y el cuerpo no se parece al de las excepciones: trae errors y no detail.
        val api = api {
            respondError(
                status = HttpStatusCode.BadRequest,
                content = """{"title":"One or more validation errors occurred.","status":400,""" +
                    """"errors":{"Email":["The Email field is not a valid e-mail address."]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }

        val error = assertIs<Respuesta.Fallo>(api.registrar(alta())).error

        val invalidos = assertIs<ErrorDeApi.DatosInvalidos>(error)
        assertEquals(listOf("The Email field is not a valid e-mail address."), invalidos.porCampo["Email"])
    }

    @Test
    fun cuandoNoHayServidor_daSinConexion() = runTest {
        val api = api { throw IllegalStateException("Connection refused") }

        val error = assertIs<Respuesta.Fallo>(api.recursos()).error

        assertIs<ErrorDeApi.SinConexion>(error)
    }

    @Test
    fun cuandoContestaAlgoQueNoEsCamar_daRespuestaIlegible() = runTest {
        // El caso real: la URL apunta a otro servidor cualquiera que devuelve su portada.
        val api = api {
            respond(
                content = "<html><body>Bienvenido a nginx</body></html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        assertIs<ErrorDeApi.RespuestaIlegible>(assertIs<Respuesta.Fallo>(api.recursos()).error)
    }

    @Test
    fun conSesionAbierta_seMandaLaCabeceraBearer() = runTest {
        val api = api(sesion = sesionValida()) {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        api.recursos()

        assertEquals("Bearer jwt.de.prueba", peticiones.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun conSesionCaducada_noSeMandaCabecera() = runTest {
        // Mandarla solo serviria para comerse un 401 seguro.
        val api = api(sesion = sesionValida(caducaEn = AHORA - 1.hours)) {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        api.recursos()

        assertNull(peticiones.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun laPeticionVaALaDireccionConfigurada() = runTest {
        val api = api(urlGuardada = "http://192.168.1.40:5106") {
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        api.recursos()

        assertEquals(
            "http://192.168.1.40:5106/api/resources",
            peticiones.single().url.toString(),
        )
    }

    @Test
    fun comprobarConexion_conUn401_cuentaComoQueElServidorResponde() = runTest {
        // Sin sesion la respuesta esperada es 401, y eso ya demuestra que hay un Camar ahi.
        val api = api { problema(HttpStatusCode.Unauthorized, "Tienes que iniciar sesion.") }

        assertIs<Respuesta.Exito<Unit>>(api.comprobarConexion())
    }

    @Test
    fun comprobarConexion_siNoHayNadie_falla() = runTest {
        val api = api { throw IllegalStateException("Connection refused") }

        assertIs<Respuesta.Fallo>(api.comprobarConexion())
    }

    private fun alta() = PeticionDeAlta(
        email = "socio@camar.test",
        nombreCompleto = "Socio de prueba",
        contrasena = "clave1234",
        plan = Plan.Flex.codigo,
        documento = "12345678Z",
        telefono = "600112233",
        codigoPostal = "28001",
    )

    private fun sesionValida(caducaEn: Instant = AHORA + 1.hours) = Sesion(
        token = "jwt.de.prueba",
        caducaEn = caducaEn,
        idUsuario = "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
        rol = "Member",
    )
}
