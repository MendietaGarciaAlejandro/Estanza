package io.github.mendietagarciaalejandro.estanza.datos

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HorasDeCamarTest {

    @Test
    fun laHoraSePintaTalComoLaMandaCamar() {
        // Camar abre a las ocho y lo manda como "08:00:00+00:00". Si esto se convirtiera a
        // la zona del dispositivo, en agosto en España saldrian las diez.
        assertEquals("08:00", Instant.parse("2026-08-12T08:00:00+00:00").comoHora())
        assertEquals("20:30", Instant.parse("2026-08-12T20:30:00+00:00").comoHora())
    }

    @Test
    fun laFranjaJuntaLasDosHoras() {
        val hueco = Hueco(
            inicio = Instant.parse("2026-08-12T09:30:00+00:00"),
            fin = Instant.parse("2026-08-12T10:00:00+00:00"),
        )

        assertEquals("09:30 - 10:00", hueco.comoFranja())
    }

    @Test
    fun laFechaVaEnElFormatoQueEsperaLaApi() {
        // Un DateOnly de .NET no traga "2026-8-3": los ceros no son opcionales.
        assertEquals("2026-08-03", LocalDate.parse("2026-08-03").comoParametro())
        assertEquals("2026-12-31", LocalDate.parse("2026-12-31").comoParametro())
    }

    @Test
    fun laFechaLargaSeLeeEnEspañol() {
        assertEquals("miercoles 12 de agosto", LocalDate.parse("2026-08-12").comoFechaLarga())
        assertEquals("domingo 1 de febrero", LocalDate.parse("2026-02-01").comoFechaLarga())
    }
}
