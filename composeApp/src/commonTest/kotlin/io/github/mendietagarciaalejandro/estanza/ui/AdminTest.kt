package io.github.mendietagarciaalejandro.estanza.ui

import io.github.mendietagarciaalejandro.estanza.EntornoDePrueba
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.admin.ModeloDeAdmin
import io.github.mendietagarciaalejandro.estanza.ui.admin.SeccionDeAdmin
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
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

class AdminTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val recursosJson =
        """[{"id":"1","name":"Sala Orion","type":"MeetingRoom","capacity":10}]"""

    private val diasJson =
        """[{"id":"d1","date":"2026-01-06","reason":"Reyes"}]"""

    private fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK) =
        respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.problema(estado: HttpStatusCode, detalle: String) = respondError(
        status = estado,
        content = """{"status":${estado.value},"detail":"$detalle"}""",
        headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
    )

    private suspend fun <T> StateFlow<T>.esperar(condicion: (T) -> Boolean): T = first(condicion)

    /**
     * Espera a una carga entera, de que empieza a que termina.
     *
     * Esperar solo a !cargando no vale: el launch del modelo no se ejecuta en el acto, asi
     * que en ese momento cargando todavia es false por la carga anterior y la espera vuelve
     * enseguida, antes de que salga la peticion. Al arrancar si funciona porque el estado
     * inicial ya es cargando = true.
     */
    private suspend fun ModeloDeAdmin.esperarCarga() {
        estado.esperar { it.cargando }
        estado.esperar { !it.cargando }
    }

    private fun HttpRequestData.es(metodo: HttpMethod, ruta: String) =
        method == metodo && url.encodedPath == ruta

    /** Responde a lo de siempre y delega el resto en el bloque. */
    private fun entornoDeAdmin(extra: MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) =
        EntornoDePrueba { peticion ->
            when {
                peticion.es(HttpMethod.Get, "/api/resources") -> json(recursosJson)
                peticion.es(HttpMethod.Get, "/api/admin/blocked-days") -> json(diasJson)
                else -> extra(peticion)
            }
        }

    // --- carga inicial ---

    @Test
    fun alAbrir_traeLosDiasCerradosYLosRecursos() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { json("[]") }
        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)

        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(1, estado.dias.size)
        assertEquals("Reyes", estado.dias.first().motivo)
        assertEquals(1, estado.recursos.size)
    }

    @Test
    fun lasReservasDelCoworkingNoSePidenHastaQueSeMiranEsaPestaña() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { json("[]") }
        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        // Pueden ser muchas: no se traen solo por abrir la pantalla.
        assertTrue(entorno.peticiones.none { it.url.encodedPath == "/api/admin/reservations" })

        modelo.verSeccion(SeccionDeAdmin.Reservas)
        modelo.esperarCarga()

        assertTrue(entorno.peticiones.any { it.url.encodedPath == "/api/admin/reservations" })
    }

    @Test
    fun unSocioQueLlegaAquiSeComeUnProhibido() = runTest(dispatcher) {
        // El boton no se le pinta, pero quien decide de verdad es Camar.
        val entorno = EntornoDePrueba { peticion ->
            if (peticion.url.encodedPath == "/api/resources") json(recursosJson)
            else problema(HttpStatusCode.Forbidden, "No tienes permiso para hacer esto.")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals("No tienes permiso para hacer esto.", estado.error)
    }

    // --- dias cerrados ---

    @Test
    fun cerrarUnDia_mandaLaFechaEnElFormatoDeLaApi() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.es(HttpMethod.Post, "/api/admin/blocked-days")) {
                json("""{"id":"d2","date":"2026-01-14","reason":"Obras"}""", HttpStatusCode.Created)
            } else json("[]")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        modelo.diaSiguiente()
        modelo.diaSiguiente()
        modelo.escribirMotivo("Obras")
        modelo.bloquearDia()

        val estado = modelo.estado.esperar { !it.trabajando && it.aviso != null }

        val cuerpo = (entorno.peticiones.first {
            it.es(HttpMethod.Post, "/api/admin/blocked-days")
        }.body as TextContent).text

        // Un DateOnly de .NET no traga "2026-1-14": los ceros no son opcionales.
        assertTrue(cuerpo.contains("\"date\":\"2026-01-14\""), cuerpo)
        assertEquals(2, estado.dias.size)
        assertEquals("", estado.motivo)
    }

    @Test
    fun noSePuedeCerrarUnDiaQueYaPaso() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { json("[]") }
        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        assertEquals(estado.hoy, estado.fechaABloquear)
        assertTrue(!estado.sePuedeRetroceder)

        modelo.diaAnterior()

        assertEquals(estado.hoy, modelo.estado.value.fechaABloquear)
    }

    @Test
    fun cerrarUnDiaYaCerrado_enseñaElConflictoDeCamar() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.es(HttpMethod.Post, "/api/admin/blocked-days")) {
                problema(HttpStatusCode.Conflict, "El 06/01/2026 ya estaba bloqueado.")
            } else json("[]")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        modelo.escribirMotivo("Reyes otra vez")
        modelo.bloquearDia()

        val estado = modelo.estado.esperar { !it.trabajando && it.error != null }

        assertEquals("El 06/01/2026 ya estaba bloqueado.", estado.error)
        // El dia no se añade a la lista si el servidor lo rechazo.
        assertEquals(1, estado.dias.size)
    }

    @Test
    fun reabrirUnDia_loQuitaDeLaLista() = runTest(dispatcher) {
        // El DELETE contesta 204 sin cuerpo: si se intentara deserializar algo, un borrado
        // correcto acabaria contado como fallo.
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.method == HttpMethod.Delete) respond("", HttpStatusCode.NoContent)
            else json("[]")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }

        modelo.desbloquearDia(estado.dias.first())
        val despues = modelo.estado.esperar { !it.trabajando && it.aviso != null }

        assertTrue(despues.dias.isEmpty())
        assertNull(despues.error)
    }

    // --- recursos ---

    @Test
    fun crearUnRecurso_mandaElTipoComoNumero() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.es(HttpMethod.Post, "/api/admin/resources")) {
                json("""{"id":"2","name":"Cabina 2","type":"PhoneBooth","capacity":1}""", HttpStatusCode.Created)
            } else json("[]")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        modelo.editarFormulario { copy(nombre = "Cabina 2", tipo = TipoDeRecurso.Cabina, capacidad = "1") }
        modelo.crearRecurso()

        modelo.estado.esperar { !it.trabajando && it.aviso != null }

        val cuerpo = (entorno.peticiones.first {
            it.es(HttpMethod.Post, "/api/admin/resources")
        }.body as TextContent).text

        // System.Text.Json espera el entero del enum, no su nombre.
        assertTrue(cuerpo.contains("\"type\":3"), cuerpo)
        assertEquals("", modelo.estado.value.formulario.nombre)
    }

    @Test
    fun soloSeOfrecenLosTiposQueSeSabenTraducir() {
        // "Otro" existe para enseñar un tipo desconocido, pero no se puede dar de alta:
        // no sabriamos que numero mandarle al servidor.
        assertTrue(TipoDeRecurso.Otro !in TipoDeRecurso.creables)
        assertEquals(3, TipoDeRecurso.creables.size)
        assertTrue(TipoDeRecurso.creables.all { it.codigo != null })
    }

    @Test
    fun elFormularioNoSeMandaAMedias() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { json("[]") }
        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }
        val antes = entorno.peticiones.size

        modelo.editarFormulario { copy(nombre = "Sala sin capacidad") }
        modelo.crearRecurso()

        assertTrue(!modelo.estado.value.formulario.sePuedeEnviar)
        assertEquals(antes, entorno.peticiones.size)
    }

    @Test
    fun darDeBaja_vaciaElCatalogoQueTienenLasOtrasPantallas() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.method == HttpMethod.Delete) respond("", HttpStatusCode.NoContent)
            else json("[]")
        }

        val catalogo = CatalogoDeRecursos(entorno.api)
        val modelo = ModeloDeAdmin(entorno.api, catalogo, entorno.reloj)
        val estado = modelo.estado.esperar { !it.cargando }
        val consultasAntes = entorno.peticiones.count { it.url.encodedPath == "/api/resources" }

        modelo.darDeBaja(estado.recursos.first())
        modelo.estado.esperar { !it.trabajando && it.aviso != null }
        modelo.esperarCarga()

        // Si no se vaciara, el catalogo seguiria enseñando un recurso que ya no se puede
        // reservar hasta que alguien cerrara sesion.
        assertTrue(
            entorno.peticiones.count { it.url.encodedPath == "/api/resources" } > consultasAntes
        )
    }

    // --- reservas del coworking ---

    @Test
    fun elFiltroPorRecursoViajaEnLaUrl() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { json("[]") }
        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }

        modelo.verSeccion(SeccionDeAdmin.Reservas)
        modelo.esperarCarga()
        modelo.filtrarReservasPor("1")
        modelo.esperarCarga()

        val ultima = entorno.peticiones.last { it.url.encodedPath == "/api/admin/reservations" }
        assertEquals("1", ultima.url.parameters["resourceId"])

        // Volver a pulsar el mismo filtro lo quita, igual que en el catalogo.
        modelo.filtrarReservasPor("1")
        modelo.esperarCarga()

        assertNull(
            entorno.peticiones.last { it.url.encodedPath == "/api/admin/reservations" }
                .url.parameters["resourceId"]
        )
    }

    @Test
    fun laReservaDeAdminEnseñaElPrincipioDelIdDelSocio() = runTest(dispatcher) {
        val entorno = entornoDeAdmin { peticion ->
            if (peticion.url.encodedPath == "/api/admin/reservations") {
                json(
                    """[{"id":"r1","resourceId":"1","userId":"019fdd65-9418-7cb7-82ce-224dccb8cf82",""" +
                        """"start":"2026-01-12T08:00:00+00:00","end":"2026-01-12T09:00:00+00:00",""" +
                        """"status":"Confirmed","price":18.00,"createdAt":"2026-01-11T10:00:00+00:00",""" +
                        """"cancelledAt":null,"refundAmount":null}]"""
                )
            } else json("[]")
        }

        val modelo = ModeloDeAdmin(entorno.api, CatalogoDeRecursos(entorno.api), entorno.reloj)
        modelo.estado.esperar { !it.cargando }
        modelo.verSeccion(SeccionDeAdmin.Reservas)

        val estado = modelo.estado.esperar { it.reservas.isNotEmpty() }
        val reserva = estado.reservas.first()

        // Camar no devuelve el nombre del socio, solo su id.
        assertEquals("019fdd65", reserva.socioEnCorto)
        assertNotNull(estado.nombresDeRecurso["1"])
    }
}
