package io.github.mendietagarciaalejandro.estanza.sesion

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val AHORA = Instant.parse("2026-01-12T08:00:00Z")

private class RelojFijo(var momento: Instant) : Clock {
    override fun now() = momento
}

class AlmacenDeSesionTest {

    private fun sesion(caducaEn: Instant) = Sesion(
        token = "jwt.de.prueba",
        caducaEn = caducaEn,
        idUsuario = "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
        rol = "Member",
    )

    @Test
    fun laSesionSobreviveAlCierreDeLaAplicacion() {
        val preferencias = MapSettings()

        AlmacenDeSesion(preferencias, RelojFijo(AHORA)).abrir(sesion(AHORA + 1.hours))

        // Otro almacen sobre las mismas preferencias es lo que pasa al reabrir la app.
        val recuperada = AlmacenDeSesion(preferencias, RelojFijo(AHORA)).sesion.value

        assertNotNull(recuperada)
        assertEquals("jwt.de.prueba", recuperada.token)
        assertEquals(AHORA + 1.hours, recuperada.caducaEn)
        assertEquals("Member", recuperada.rol)
    }

    @Test
    fun alArrancar_unTokenYaCaducadoNiSeCarga() {
        val preferencias = MapSettings()
        AlmacenDeSesion(preferencias, RelojFijo(AHORA)).abrir(sesion(AHORA + 1.hours))

        // Vuelves a abrir la aplicacion al dia siguiente.
        val alDiaSiguiente = AlmacenDeSesion(preferencias, RelojFijo(AHORA + 24.hours))

        // Se descarta al leerlo, para que el resto de la aplicacion solo tenga que mirar si
        // hay sesion y no ademas si la que hay sirve.
        assertNull(alDiaSiguiente.sesion.value)
        assertNull(preferencias.getStringOrNull("sesion_token"))
    }

    @Test
    fun cerrar_borraElToken() {
        val preferencias = MapSettings()
        val almacen = AlmacenDeSesion(preferencias, RelojFijo(AHORA))
        almacen.abrir(sesion(AHORA + 1.hours))

        almacen.cerrar()

        assertNull(almacen.sesion.value)
        assertNull(AlmacenDeSesion(preferencias, RelojFijo(AHORA)).sesion.value)
    }

    @Test
    fun unaSesionCaducadaSeRecuerdaPeroNoEsVigente() {
        val reloj = RelojFijo(AHORA)
        val almacen = AlmacenDeSesion(MapSettings(), reloj)
        almacen.abrir(sesion(AHORA + 1.hours))

        reloj.momento = AHORA + 2.hours

        // Se sigue recordando para poder decir "se te ha caducado la sesion" en vez de
        // hacer como si nunca hubiera entrado.
        assertNotNull(almacen.sesion.value)
        assertNull(almacen.vigente)
    }

    @Test
    fun elMargenDescartaLaSesionQueEstaAPuntoDeCaducar() {
        // Camar valida con ClockSkew a cero: un token al que le quedan diez segundos se va
        // a comer un 401 en cuanto la peticion tarde un poco.
        val sesion = sesion(AHORA + 10.seconds)

        assertFalse(sesion.vigenteEn(AHORA))
        assertTrue(sesion(AHORA + 1.hours).vigenteEn(AHORA))
    }

    @Test
    fun elRolDistingueAlAdministrador() {
        assertTrue(sesion(AHORA).copy(rol = "Admin").esAdministrador)
        assertFalse(sesion(AHORA).esAdministrador)
    }
}
