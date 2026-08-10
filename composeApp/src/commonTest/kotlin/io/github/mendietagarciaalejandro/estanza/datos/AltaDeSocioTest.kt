package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.Plan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AltaDeSocioTest {

    private fun formulario(
        email: String = "socio@camar.test",
        nombreCompleto: String = "Alejandro Mendieta",
        contrasena: String = "clave1234",
        documento: String = "12345678Z",
        telefono: String = "600112233",
        codigoPostal: String = "28001",
        cuentaBancaria: String = "",
    ) = FormularioDeAlta(
        email = email,
        nombreCompleto = nombreCompleto,
        contrasena = contrasena,
        plan = Plan.Flex,
        documento = documento,
        telefono = telefono,
        codigoPostal = codigoPostal,
        cuentaBancaria = cuentaBancaria,
    )

    private fun fallos(formulario: FormularioDeAlta) =
        assertIs<RevisionDelAlta.ConFallos>(RevisorDeAlta.revisar(formulario)).porCampo

    @Test
    fun unAltaCompletaPasaYSaleNormalizada() {
        val revision = RevisorDeAlta.revisar(
            formulario(
                email = "  Socio@Camar.TEST ",
                documento = " 12345678-z ",
                telefono = "+34 600 11 22 33",
                cuentaBancaria = "ES91 2100 0418 4502 0005 1332",
            )
        )

        val peticion = assertIs<RevisionDelAlta.Correcta>(revision).peticion

        // Normalizar aqui evita que dos socios se den de alta con el mismo email escrito
        // con mayusculas distintas.
        assertEquals("socio@camar.test", peticion.email)
        assertEquals("12345678Z", peticion.documento)
        assertEquals("600112233", peticion.telefono)
        assertEquals("ES9121000418450200051332", peticion.cuentaBancaria)
        assertEquals(1, peticion.plan)
    }

    @Test
    fun losTresTiposDeDocumentoValen() {
        // Camar acepta NIF, NIE y CIF, asi que el formulario tambien.
        listOf("12345678Z", "X1234567L", "A28015865").forEach { documento ->
            assertIs<RevisionDelAlta.Correcta>(
                RevisorDeAlta.revisar(formulario(documento = documento)),
                "deberia aceptar $documento",
            )
        }
    }

    @Test
    fun laCuentaBancariaEsOpcional() {
        val revision = RevisorDeAlta.revisar(formulario(cuentaBancaria = "   "))

        assertNull(assertIs<RevisionDelAlta.Correcta>(revision).peticion.cuentaBancaria)
    }

    @Test
    fun unaLetraDeDniQueNoCorresponde_seCazaSinLlamarALaApi() {
        val porCampo = fallos(formulario(documento = "12345678A"))

        assertEquals(setOf(CampoDeAlta.Documento), porCampo.keys)
        assertTrue(porCampo.getValue(CampoDeAlta.Documento).contains("control"))
    }

    @Test
    fun unIbanConUnaCifraCambiada_seCaza() {
        // El digito de control del IBAN existe justo para esto.
        val porCampo = fallos(formulario(cuentaBancaria = "ES9121000418450200051333"))

        assertEquals(setOf(CampoDeAlta.CuentaBancaria), porCampo.keys)
    }

    @Test
    fun unCodigoPostalDeProvinciaInexistente_seCaza() {
        val porCampo = fallos(formulario(codigoPostal = "53001"))

        assertTrue(porCampo.getValue(CampoDeAlta.CodigoPostal).contains("provincia"))
    }

    @Test
    fun unTelefonoQueNoEmpiezaPorSeisSieteOchoONueve_seCaza() {
        assertTrue(CampoDeAlta.Telefono in fallos(formulario(telefono = "100112233")))
    }

    @Test
    fun unaContrasenaCortaSeCaza() {
        // Camar pide ocho como minimo; mejor decirlo aqui que comerse un 400.
        assertTrue(CampoDeAlta.Contrasena in fallos(formulario(contrasena = "corta")))
    }

    @Test
    fun seSeñalanTodosLosCamposMalosDeUnaVez() {
        // Ir de uno en uno obligaria al usuario a mandar el formulario cinco veces.
        val porCampo = fallos(
            formulario(
                email = "esto no es un email",
                contrasena = "1234",
                documento = "12345678A",
                telefono = "1",
                codigoPostal = "00000",
            )
        )

        assertEquals(
            setOf(
                CampoDeAlta.Email,
                CampoDeAlta.Contrasena,
                CampoDeAlta.Documento,
                CampoDeAlta.Telefono,
                CampoDeAlta.CodigoPostal,
            ),
            porCampo.keys,
        )
    }
}
