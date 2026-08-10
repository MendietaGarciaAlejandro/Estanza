package io.github.mendietagarciaalejandro.estanza.ui

import io.github.mendietagarciaalejandro.estanza.EntornoDePrueba
import io.github.mendietagarciaalejandro.estanza.datos.CampoDeAlta
import io.github.mendietagarciaalejandro.estanza.ui.acceso.ModeloDeAcceso
import io.github.mendietagarciaalejandro.estanza.ui.alta.ModeloDeAlta
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los modelos usan viewModelScope, que va contra Dispatchers.Main, y en un test no hay Main
 * de verdad: hay que ponerle uno.
 *
 * Se usa el Unconfined y no el Standard porque asi el launch de los modelos empieza a
 * ejecutarse en el acto. Con el Standard no arranca hasta que se le dice, y entonces al
 * mirar el estado justo despues de llamar al modelo todavia se ve el de antes de empezar.
 */
class ModelosDePantallaTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun antes() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun MockRequestHandleScope.autenticacion() = respond(
        content = """{"token":"jwt.de.prueba","expiresAt":"2026-01-12T09:00:00.8979484+00:00",""" +
            """"userId":"019fdd65-9418-7cb7-82ce-224dccb8cf82","role":"Member"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun MockRequestHandleScope.problema(estado: HttpStatusCode, detalle: String) = respondError(
        status = estado,
        content = """{"title":"Vaya","status":${estado.value},"detail":"$detalle"}""",
        headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
    )

    // --- acceso ---

    @Test
    fun acceso_conCredencialesBuenas_abreLaSesion() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { autenticacion() }
        val modelo = ModeloDeAcceso(entorno.api, entorno.sesiones)

        modelo.escribirEmail("luis@camar.test")
        modelo.escribirContrasena("camar-demo-2026")
        modelo.entrar()
        modelo.estado.esperar { !it.entrando }

        // Nadie navega a mano: abrir la sesion es lo que mueve la aplicacion.
        assertNotNull(entorno.sesiones.sesion.value)
        assertNull(modelo.estado.value.error)
        // La contrasena no tiene por que seguir en memoria una vez hay token.
        assertEquals("", modelo.estado.value.contrasena)
    }

    @Test
    fun acceso_conCredencialesMalas_dejaElErrorYNoAbreSesion() = runTest(dispatcher) {
        val entorno = EntornoDePrueba {
            problema(HttpStatusCode.Unauthorized, "Email o contrasena incorrectos.")
        }
        val modelo = ModeloDeAcceso(entorno.api, entorno.sesiones)

        modelo.escribirEmail("luis@camar.test")
        modelo.escribirContrasena("noesesta")
        modelo.entrar()
        modelo.estado.esperar { !it.entrando }

        assertNull(entorno.sesiones.sesion.value)
        assertEquals("Email o contrasena incorrectos.", modelo.estado.value.error)
        assertFalse(modelo.estado.value.entrando)
    }

    @Test
    fun acceso_conElFormularioAMedias_niSiquieraLoIntenta() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { autenticacion() }
        val modelo = ModeloDeAcceso(entorno.api, entorno.sesiones)

        modelo.escribirEmail("luis@camar.test")
        modelo.entrar()

        assertFalse(modelo.estado.value.sePuedeEnviar)
        assertTrue(entorno.peticiones.isEmpty())
    }

    // --- alta ---

    @Test
    fun alta_conDatosMalos_seQuedaEnCasa() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { autenticacion() }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        modelo.editar(CampoDeAlta.Documento) { copy(documento = "12345678A") }
        modelo.enviar()

        // Lo importante es esto: la revision de validadores-es evita la ida y vuelta.
        assertTrue(entorno.peticiones.isEmpty())
        assertTrue(CampoDeAlta.Documento in modelo.estado.value.fallos)
    }

    @Test
    fun alta_alEscribirEnUnCampo_soloSeBorraSuFallo() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { autenticacion() }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        modelo.enviar()
        val fallosIniciales = modelo.estado.value.fallos.keys
        assertTrue(fallosIniciales.size > 1)

        modelo.editar(CampoDeAlta.Email) { copy(email = "socio@camar.test") }

        // Los demas campos siguen estando mal; hacerlos desaparecer despistaria.
        assertEquals(fallosIniciales - CampoDeAlta.Email, modelo.estado.value.fallos.keys)
    }

    @Test
    fun alta_correcta_entraDirectamente() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { autenticacion() }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        rellenar(modelo)
        modelo.enviar()
        modelo.estado.esperar { !it.enviando }

        // Camar devuelve el token en el alta, asi que no hay que pedir la contrasena otra vez.
        assertNotNull(entorno.sesiones.sesion.value)
        assertEquals(1, entorno.peticiones.size)
    }

    @Test
    fun alta_conDocumentoYaRegistrado_señalaElCampo() = runTest(dispatcher) {
        val entorno = EntornoDePrueba {
            problema(HttpStatusCode.Conflict, "Ese documento fiscal ya esta registrado.")
        }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        rellenar(modelo)
        modelo.enviar()
        modelo.estado.esperar { !it.enviando }

        // El 409 no dice que campo choca, se deduce del mensaje. Mejor eso que un aviso
        // suelto que obliga al usuario a adivinar cual de los ocho campos repite.
        assertEquals(
            "Ese documento fiscal ya esta registrado.",
            modelo.estado.value.fallos[CampoDeAlta.Documento],
        )
        assertNull(modelo.estado.value.error)
    }

    @Test
    fun alta_conUn400_traduceLosNombresDelContratoACampos() = runTest(dispatcher) {
        val entorno = EntornoDePrueba {
            respondError(
                status = HttpStatusCode.BadRequest,
                content = """{"status":400,"errors":{"TaxId":["The field is too long."]}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/problem+json"),
            )
        }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        rellenar(modelo)
        modelo.enviar()
        modelo.estado.esperar { !it.enviando }

        // Camar los nombra en ingles porque asi esta el contrato; aqui hay que devolverlos
        // al campo que le toca en la pantalla.
        assertEquals("The field is too long.", modelo.estado.value.fallos[CampoDeAlta.Documento])
    }

    @Test
    fun alta_siNoHayServidor_elAvisoNoCuelgaDeNingunCampo() = runTest(dispatcher) {
        val entorno = EntornoDePrueba { throw IllegalStateException("Connection refused") }
        val modelo = ModeloDeAlta(entorno.api, entorno.sesiones)

        rellenar(modelo)
        modelo.enviar()
        modelo.estado.esperar { !it.enviando }

        assertTrue(modelo.estado.value.fallos.isEmpty())
        assertNotNull(modelo.estado.value.error)
        assertFalse(modelo.estado.value.enviando)
    }

    /**
     * El motor de Ktor resuelve en hilos de verdad, no en el planificador del test, asi que
     * adelantar el tiempo virtual no sirve de nada: hay que esperar a que el estado cambie.
     *
     * Sin withTimeout a proposito: dentro de runTest el tiempo de los timeouts es virtual y
     * el planificador lo adelanta de golpe, con lo que saltaria siempre. Si algo se colgara
     * de verdad lo cortaria el limite de tiempo real que runTest ya trae de serie.
     */
    private suspend fun <T> StateFlow<T>.esperar(condicion: (T) -> Boolean): T = first(condicion)

    private fun rellenar(modelo: ModeloDeAlta) {
        modelo.editar(CampoDeAlta.NombreCompleto) { copy(nombreCompleto = "Alejandro Mendieta") }
        modelo.editar(CampoDeAlta.Email) { copy(email = "socio@camar.test") }
        modelo.editar(CampoDeAlta.Contrasena) { copy(contrasena = "clave1234") }
        modelo.editar(CampoDeAlta.Documento) { copy(documento = "12345678Z") }
        modelo.editar(CampoDeAlta.Telefono) { copy(telefono = "600112233") }
        modelo.editar(CampoDeAlta.CodigoPostal) { copy(codigoPostal = "28001") }
    }
}
