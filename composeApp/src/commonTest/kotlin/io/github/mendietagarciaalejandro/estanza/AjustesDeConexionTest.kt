package io.github.mendietagarciaalejandro.estanza

import com.russhwolf.settings.MapSettings
import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Estos tests corren en los tres targets. Es la ventaja de tener la logica en commonMain:
 * si el navegador se comportara distinto del escritorio, saldria aqui.
 */
class AjustesDeConexionTest {

    private fun ajustes(guardado: String? = null): AjustesDeConexion {
        val almacen = MapSettings()
        if (guardado != null) almacen.putString("url_base_api", guardado)

        return AjustesDeConexion(almacen, PlataformaDePrueba)
    }

    @Test
    fun sinNadaGuardado_usaLaDeLaPlataforma() {
        val ajustes = ajustes()

        assertEquals("http://localhost:5106", ajustes.urlBase.value)
        assertFalse(ajustes.estaPersonalizada)
    }

    @Test
    fun loGuardadoManda() {
        assertEquals("http://192.168.1.40:5106", ajustes("http://192.168.1.40:5106").urlBase.value)
    }

    @Test
    fun quitaEspaciosYLaBarraFinal() {
        val ajustes = ajustes()

        assertNull(ajustes.guardar("  http://casa:5106/  "))
        assertEquals("http://casa:5106", ajustes.urlBase.value)
    }

    @Test
    fun sinEsquema_seRechaza() {
        val ajustes = ajustes()

        // Es el error tipico: copiar la direccion de la barra del navegador sin el http.
        assertNotNull(ajustes.guardar("192.168.1.40:5106"))
        assertEquals("http://localhost:5106", ajustes.urlBase.value)
    }

    @Test
    fun enBlanco_seRechaza() {
        assertNotNull(ajustes().guardar("   "))
    }

    @Test
    fun restablecer_vuelveALaDeLaPlataforma() {
        val ajustes = ajustes("http://otro:8080")
        assertTrue(ajustes.estaPersonalizada)

        ajustes.restablecer()

        assertEquals("http://localhost:5106", ajustes.urlBase.value)
        assertFalse(ajustes.estaPersonalizada)
    }
}
