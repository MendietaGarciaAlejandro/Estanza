package io.github.mendietagarciaalejandro.estanza.ui

import io.github.mendietagarciaalejandro.estanza.EntornoDePrueba
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.catalogo.ModeloDeCatalogo
import io.github.mendietagarciaalejandro.estanza.ui.recurso.ModeloDeRecurso
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
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

class CatalogoYRecursoTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val recursosJson = """[
        {"id":"1","name":"Sala Orion","type":"MeetingRoom","capacity":10},
        {"id":"2","name":"Mesa flexible 1","type":"HotDesk","capacity":1},
        {"id":"3","name":"Cabina","type":"PhoneBooth","capacity":1},
        {"id":"4","name":"Sofa nuevo","type":"Hammock","capacity":2}
    ]"""

    private fun MockRequestHandleScope.json(cuerpo: String) = respond(
        content = cuerpo,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun disponibilidad(fecha: String, huecos: String) =
        """{"resourceId":"1","date":"$fecha","freeSlots":[$huecos]}"""

    private suspend fun <T> StateFlow<T>.esperar(condicion: (T) -> Boolean): T = first(condicion)

    // --- catalogo ---

    @Test
    fun elCatalogoSeCargaSoloAlCrearse() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { json(recursosJson) }
        val modelo = ModeloDeCatalogo(CatalogoDeRecursos(entorno.api))

        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(4, estado.recursos.size)
        assertEquals("Sala Orion", estado.recursos.first().nombre)
    }

    @Test
    fun unTipoQueNoConocemosNoRompeNada() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { json(recursosJson) }
        val modelo = ModeloDeCatalogo(CatalogoDeRecursos(entorno.api))

        val estado = modelo.estado.esperar { !it.cargando }

        // Si el coworking da de alta una hamaca, el cliente la enseña como "Otro" en vez de
        // reventar al deserializar. Un enum sin salida obligaria a publicar version nueva.
        assertEquals(TipoDeRecurso.Otro, estado.recursos.last().tipo)
    }

    @Test
    fun elFiltroSoloOfreceLosTiposQueHay() = runTest(dispatcher) {
        val entorno = EntornoDePrueba {
            json("""[{"id":"1","name":"Sala Orion","type":"MeetingRoom","capacity":10}]""")
        }
        val modelo = ModeloDeCatalogo(CatalogoDeRecursos(entorno.api))

        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(listOf(TipoDeRecurso.SalaDeReuniones), estado.tiposDisponibles)
    }

    @Test
    fun pulsarDosVecesElMismoFiltroLoQuita() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { json(recursosJson) }
        val modelo = ModeloDeCatalogo(CatalogoDeRecursos(entorno.api))
        modelo.estado.esperar { !it.cargando }

        modelo.filtrarPor(TipoDeRecurso.MesaFlexible)
        assertEquals(1, modelo.estado.value.visibles.size)

        modelo.filtrarPor(TipoDeRecurso.MesaFlexible)
        assertEquals(4, modelo.estado.value.visibles.size)
    }

    @Test
    fun siLaListaNoLlegaSeOfreceReintentar() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { throw IllegalStateException("Connection refused") }
        val modelo = ModeloDeCatalogo(CatalogoDeRecursos(entorno.api))

        val estado = modelo.estado.esperar { !it.cargando }

        assertNotNull(estado.error)
        assertTrue(estado.recursos.isEmpty())
    }

    @Test
    fun laListaSeGuardaYNoSePideDosVeces() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { json(recursosJson) }
        val catalogo = CatalogoDeRecursos(entorno.api)

        ModeloDeCatalogo(catalogo).estado.esperar { !it.cargando }
        // Camar no tiene GET /api/resources/{id}: sin guardar la lista, abrir una ficha
        // obligaria a pedir los recursos otra vez solo para saber como se llama.
        catalogo.recurso("1")
        catalogo.recurso("2")

        assertEquals(1, entorno.peticiones.size)
    }

    @Test
    fun olvidar_obligaAPedirlaDeNuevo() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { json(recursosJson) }
        val catalogo = CatalogoDeRecursos(entorno.api)

        catalogo.recursos()
        catalogo.olvidar()
        catalogo.recursos()

        assertEquals(2, entorno.peticiones.size)
    }

    // --- ficha del recurso ---

    @Test
    fun laFichaPideLaDisponibilidadDeHoy() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                json(
                    disponibilidad(
                        "2026-01-12",
                        """{"start":"2026-01-12T08:00:00+00:00","end":"2026-01-12T08:30:00+00:00"}""",
                    )
                )
            } else {
                json(recursosJson)
            }
        }

        val modelo = ModeloDeRecurso("1", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(1, estado.disponibilidad?.huecos?.size)
        assertEquals("08:00 - 08:30", estado.disponibilidad!!.huecos.first().comoFranja())
        assertEquals("Sala Orion", estado.recurso?.nombre)
        assertTrue(estado.esHoy)
    }

    @Test
    fun laFechaVaEnLaPeticion() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                json(disponibilidad("2026-01-13", ""))
            } else {
                json(recursosJson)
            }
        }

        val modelo = ModeloDeRecurso("1", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        modelo.diaSiguiente()
        modelo.estado.esperar { !it.cargando && it.fecha.day == 13 }

        val ultima = entorno.peticiones.last { it.url.encodedPath.endsWith("/availability") }
        assertEquals("2026-01-13", ultima.url.parameters["date"])
    }

    @Test
    fun noSePuedeMirarElPasado() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) json(disponibilidad("2026-01-12", ""))
            else json(recursosJson)
        }

        val modelo = ModeloDeRecurso("1", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        // Reservar ayer no tiene sentido, asi que tampoco mirarlo.
        assertTrue(estado.esHoy)
        assertTrue(!estado.sePuedeRetroceder)

        val antes = entorno.peticiones.size
        modelo.diaAnterior()
        assertEquals(antes, entorno.peticiones.size)
    }

    @Test
    fun unDiaCerradoLlegaSinHuecos() = runTest(dispatcher) {
        // Camar contesta lo mismo si el coworking cierra que si esta completo.
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) json(disponibilidad("2026-01-12", ""))
            else json(recursosJson)
        }

        val modelo = ModeloDeRecurso("1", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(emptyList(), estado.disponibilidad?.huecos)
        assertNull(estado.error)
    }

    @Test
    fun unRecursoQueNoExiste_da404ConSuMensaje() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath.endsWith("/availability")) {
                respondError(
                    status = HttpStatusCode.NotFound,
                    content = """{"status":404,"detail":"No existe el recurso 9."}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
                )
            } else {
                json(recursosJson)
            }
        }

        val modelo = ModeloDeRecurso("9", CatalogoDeRecursos(entorno.api), entorno.api, entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals("No existe el recurso 9.", estado.error)
    }
}
