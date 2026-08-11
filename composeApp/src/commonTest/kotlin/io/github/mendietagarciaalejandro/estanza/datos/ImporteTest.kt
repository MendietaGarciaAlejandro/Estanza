package io.github.mendietagarciaalejandro.estanza.datos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImporteTest {

    @Test
    fun camarMandaElMismoPrecioConDosDecimalesYConTres() {
        // Al crear la reserva llega "18.000" y al listarla "18.00". Las dos son 18 euros.
        assertEquals("18,00 €", Importe("18.000").conMoneda())
        assertEquals("18,00 €", Importe("18.00").conMoneda())
        assertEquals("18,00 €", Importe("18").conMoneda())
    }

    @Test
    fun losCentimosNoSePierden() {
        assertEquals("9,50 €", Importe("9.5").conMoneda())
        assertEquals("0,05 €", Importe("0.05").conMoneda())
        assertEquals("1234,56 €", Importe("1234.56").conMoneda())
    }

    @Test
    fun elTercerDecimalRedondea() {
        // Media hora de sala a 36 euros la hora sale a 18; si algun precio saliera con
        // milesimas, se cobra al centimo mas cercano y no se trunca.
        assertEquals("1,01 €", Importe("1.005").conMoneda())
        assertEquals("1,00 €", Importe("1.004").conMoneda())
    }

    @Test
    fun elCeroSeReconoce() {
        // Cancelar con menos de tres horas de antelacion devuelve cero, y eso se dice de
        // otra manera que devolver una cantidad.
        assertTrue(Importe("0.00").esCero)
        assertTrue(Importe("0").esCero)
        assertFalse(Importe("0.01").esCero)
    }
}
