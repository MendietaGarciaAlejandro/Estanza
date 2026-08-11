package io.github.mendietagarciaalejandro.estanza.ui

import io.github.mendietagarciaalejandro.estanza.EntornoDePrueba
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.EstadoDeReserva
import io.github.mendietagarciaalejandro.estanza.ui.recurso.ModeloDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.reservas.ModeloDeReservas
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReservarTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val recursosJson =
        """[{"id":"1","name":"Sala Orion","type":"MeetingRoom","capacity":10}]"""

    /** Cuatro medias horas seguidas y una suelta despues de un rato ya ocupado. */
    private val huecosConHueco = listOf(
        "08:00" to "08:30",
        "08:30" to "09:00",
        "09:00" to "09:30",
        "09:30" to "10:00",
        // Aqui falta de 10:00 a 11:00: alguien lo tiene reservado.
        "11:00" to "11:30",
    ).joinToString(",") { (a, b) ->
        """{"start":"2026-01-12T$a:00+00:00","end":"2026-01-12T$b:00+00:00"}"""
    }

    private fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK) =
        respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.problema(estado: HttpStatusCode, detalle: String) = respondError(
        status = estado,
        content = """{"status":${estado.value},"detail":"$detalle"}""",
        headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
    )

    private fun reservaJson(
        id: String = "r1",
        inicio: String = "08:00",
        fin: String = "09:00",
        estado: String = "Confirmed",
        precio: String = "18.000",
        reembolso: String? = null,
    ) = """{"id":"$id","resourceId":"1","userId":"u1",""" +
        """"start":"2026-01-12T$inicio:00+00:00","end":"2026-01-12T$fin:00+00:00",""" +
        """"status":"$estado","price":$precio,"createdAt":"2026-01-11T10:00:00+00:00",""" +
        """"cancelledAt":${if (reembolso == null) "null" else "\"2026-01-11T12:00:00+00:00\""},""" +
        """"refundAmount":${reembolso ?: "null"}}"""

    private suspend fun <T> StateFlow<T>.esperar(condicion: (T) -> Boolean): T = first(condicion)

    private fun HttpRequestData.esCreacionDeReserva() =
        method == HttpMethod.Post && url.encodedPath == "/api/reservations"

    private fun fichaDe(entorno: EntornoDePrueba) =
        ModeloDeRecurso("1", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)

    // --- seleccion de huecos ---

    @Test
    fun elSegundoToqueEstiraLaSeleccion() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
            } else json(recursosJson)
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(0)
        assertEquals("de 08:00 a 08:30, 30 min", modelo.estado.value.resumenDeLaSeleccion)

        modelo.pulsarHueco(2)
        assertEquals("de 08:00 a 09:30, 1 h 30 min", modelo.estado.value.resumenDeLaSeleccion)
    }

    @Test
    fun noSePuedeSaltarPorEncimaDeUnRatoOcupado() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
            } else json(recursosJson)
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(0)
        // El indice 4 son las 11:00, y entre medias hay una hora reservada por otro. En vez
        // de inventarse un rango que pisaria esa reserva, se empieza de cero desde ahi.
        modelo.pulsarHueco(4)

        assertEquals("de 11:00 a 11:30, 30 min", modelo.estado.value.resumenDeLaSeleccion)
    }

    @Test
    fun volverATocarLoElegidoLoSuelta() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
            } else json(recursosJson)
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(1)
        modelo.pulsarHueco(1)

        assertNull(modelo.estado.value.seleccion)
    }

    // --- reservar ---

    @Test
    fun reservar_mandaElPeriodoEnteroYNoElPrimerBloque() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.esCreacionDeReserva() -> json(reservaJson(), HttpStatusCode.Created)
                peticion.url.encodedPath.endsWith("/availability") ->
                    json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
                else -> json(recursosJson)
            }
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(0)
        modelo.pulsarHueco(1)
        modelo.reservar()
        modelo.estado.esperar { !it.reservando && it.reservaHecha != null }

        // El toString del OutgoingContent solo dice el tipo, hay que sacar el texto.
        val cuerpo = (entorno.peticiones.first { it.esCreacionDeReserva() }.body as TextContent).text
        // Una mesa flexible tiene un minimo de cuatro horas: si se mandara solo el primer
        // bloque de media hora, Camar contestaria 422 siempre.
        assertTrue(cuerpo.contains("2026-01-12T08:00:00Z"), cuerpo)
        assertTrue(cuerpo.contains("2026-01-12T09:00:00Z"), cuerpo)
    }

    @Test
    fun reservar_conExito_avisaDelPrecioYVuelveAPreguntarLosHuecos() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.esCreacionDeReserva() -> json(reservaJson(), HttpStatusCode.Created)
                peticion.url.encodedPath.endsWith("/availability") ->
                    json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
                else -> json(recursosJson)
            }
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }
        val consultasAntes = entorno.peticiones.count { it.url.encodedPath.endsWith("/availability") }

        modelo.pulsarHueco(0)
        modelo.reservar()
        val estado = modelo.estado.esperar { it.reservaHecha != null }

        assertEquals("Reservado 08:00 - 09:00. Son 18,00 €.", estado.reservaHecha)
        assertNull(estado.seleccion)

        // El hueco ya no esta libre: se vuelve a preguntar en vez de quitarlo de la lista
        // por nuestra cuenta y fiarnos de haber acertado. Se espera a que la recarga
        // empiece y termine, que si no la cuenta se mira antes de que salga la peticion.
        modelo.estado.esperar { it.cargando }
        modelo.estado.esperar { !it.cargando }

        assertTrue(
            entorno.peticiones.count { it.url.encodedPath.endsWith("/availability") } > consultasAntes
        )
    }

    @Test
    fun reservar_conElHuecoPisado_enseñaElMensajeDeCamar() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.esCreacionDeReserva() ->
                    problema(HttpStatusCode.Conflict, "Ese hueco ya esta reservado.")
                peticion.url.encodedPath.endsWith("/availability") ->
                    json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
                else -> json(recursosJson)
            }
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(0)
        modelo.reservar()
        val estado = modelo.estado.esperar { !it.reservando && it.errorDeReserva != null }

        assertEquals("Ese hueco ya esta reservado.", estado.errorDeReserva)
        assertNull(estado.reservaHecha)
    }

    @Test
    fun reservar_queIncumpleUnaRegla_enseñaElMensajeDeCamar() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.esCreacionDeReserva() -> problema(
                    HttpStatusCode.UnprocessableEntity,
                    "Una reserva de HotDesk dura entre 240 y 780 minutos.",
                )
                peticion.url.encodedPath.endsWith("/availability") ->
                    json("""{"resourceId":"1","date":"2026-01-12","freeSlots":[$huecosConHueco]}""")
                else -> json(recursosJson)
            }
        }

        val modelo = fichaDe(entorno)
        modelo.estado.esperar { !it.cargando }

        modelo.pulsarHueco(0)
        modelo.reservar()
        val estado = modelo.estado.esperar { !it.reservando && it.errorDeReserva != null }

        // Las duraciones minimas son politica del servidor: no se repiten aqui, se enseña
        // lo que conteste.
        assertEquals("Una reserva de HotDesk dura entre 240 y 780 minutos.", estado.errorDeReserva)
    }

    // --- mis reservas ---

    @Test
    fun laListaEnseñaElNombreDelRecursoYNoSuId() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath == "/api/reservations") json("[${reservaJson()}]")
            else json(recursosJson)
        }

        val modelo = ModeloDeReservas(entorno.api, CatalogoDeRecursos(entorno.api))
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(1, estado.reservas.size)
        assertEquals("Sala Orion", estado.nombres["1"])
        assertEquals(EstadoDeReserva.Confirmada, estado.reservas.first().estado)
    }

    @Test
    fun cancelar_pideConfirmacionAntes() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath == "/api/reservations") json("[${reservaJson()}]")
            else json(recursosJson)
        }

        val modelo = ModeloDeReservas(entorno.api, CatalogoDeRecursos(entorno.api))
        val estado = modelo.estado.esperar { !it.cargando }
        val peticionesAntes = entorno.peticiones.size

        modelo.preguntarSiCancelar(estado.reservas.first())

        // Cancelar cuesta dinero segun la antelacion: no se hace de un solo toque.
        assertNotNull(modelo.estado.value.porCancelar)
        assertEquals(peticionesAntes, entorno.peticiones.size)
    }

    @Test
    fun cancelar_cuentaCuantoDevuelveCamar() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.url.encodedPath.endsWith("/cancel") ->
                    json(reservaJson(estado = "Cancelled", precio = "18.00", reembolso = "9.00"))
                peticion.url.encodedPath == "/api/reservations" -> json("[${reservaJson()}]")
                else -> json(recursosJson)
            }
        }

        val modelo = ModeloDeReservas(entorno.api, CatalogoDeRecursos(entorno.api))
        modelo.preguntarSiCancelar(modelo.estado.esperar { !it.cargando }.reservas.first())
        modelo.confirmarCancelacion()

        val estado = modelo.estado.esperar { !it.cancelando && it.aviso != null }

        // El importe lo decide la politica de cancelacion del servidor; aqui solo se cuenta.
        assertEquals("Reserva cancelada. Se te devuelven 9,00 €.", estado.aviso)
        assertEquals(EstadoDeReserva.Cancelada, estado.reservas.first().estado)
    }

    @Test
    fun cancelar_conMuyPocaAntelacion_loDiceDeOtraManera() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.url.encodedPath.endsWith("/cancel") ->
                    json(reservaJson(estado = "Cancelled", precio = "18.00", reembolso = "0.00"))
                peticion.url.encodedPath == "/api/reservations" -> json("[${reservaJson()}]")
                else -> json(recursosJson)
            }
        }

        val modelo = ModeloDeReservas(entorno.api, CatalogoDeRecursos(entorno.api))
        modelo.preguntarSiCancelar(modelo.estado.esperar { !it.cargando }.reservas.first())
        modelo.confirmarCancelacion()

        val estado = modelo.estado.esperar { !it.cancelando && it.aviso != null }

        assertEquals(
            "Reserva cancelada. Con tan poca antelacion no se devuelve nada.",
            estado.aviso,
        )
    }

    @Test
    fun cancelar_loQueYaEstabaCancelado_enseñaElConflicto() = runTest(dispatcher) {
        // Pasa de verdad: la cancelas desde el movil y luego le das al boton en el
        // escritorio, que todavia tiene la lista de antes.
        val entorno = EntornoDePrueba { peticion ->
            when {
                peticion.url.encodedPath.endsWith("/cancel") -> problema(
                    HttpStatusCode.Conflict,
                    "No se puede operar sobre una reserva en estado Cancelled.",
                )
                peticion.url.encodedPath == "/api/reservations" -> json("[${reservaJson()}]")
                else -> json(recursosJson)
            }
        }

        val modelo = ModeloDeReservas(entorno.api, CatalogoDeRecursos(entorno.api))
        modelo.preguntarSiCancelar(modelo.estado.esperar { !it.cargando }.reservas.first())
        modelo.confirmarCancelacion()

        val estado = modelo.estado.esperar { !it.cancelando && it.aviso != null }

        assertEquals("No se puede operar sobre una reserva en estado Cancelled.", estado.aviso)
    }
}
