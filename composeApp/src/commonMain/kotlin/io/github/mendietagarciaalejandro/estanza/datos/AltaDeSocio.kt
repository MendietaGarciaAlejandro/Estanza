package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.PeticionDeAlta
import io.github.mendietagarciaalejandro.estanza.red.Plan
import io.github.mendietagarciaalejandro.validadores.Cif
import io.github.mendietagarciaalejandro.validadores.CodigoPostal
import io.github.mendietagarciaalejandro.validadores.Iban
import io.github.mendietagarciaalejandro.validadores.Motivo
import io.github.mendietagarciaalejandro.validadores.Nie
import io.github.mendietagarciaalejandro.validadores.Nif
import io.github.mendietagarciaalejandro.validadores.Resultado
import io.github.mendietagarciaalejandro.validadores.Telefono

enum class CampoDeAlta {
    Email, NombreCompleto, Contrasena, Documento, Telefono, CodigoPostal, CuentaBancaria
}

/** Lo que el usuario ha escrito, tal cual, sin normalizar todavia. */
data class FormularioDeAlta(
    val email: String = "",
    val nombreCompleto: String = "",
    val contrasena: String = "",
    val plan: Plan = Plan.Flex,
    val documento: String = "",
    val telefono: String = "",
    val codigoPostal: String = "",
    val cuentaBancaria: String = "",
)

sealed interface RevisionDelAlta {
    /** Todo cuadra. La peticion ya lleva los valores normalizados. */
    data class Correcta(val peticion: PeticionDeAlta) : RevisionDelAlta

    data class ConFallos(val porCampo: Map<CampoDeAlta, String>) : RevisionDelAlta
}

/**
 * Revisa el formulario de alta antes de mandarlo.
 *
 * Camar valida exactamente lo mismo por su cuenta y esa es la validacion que cuenta: el
 * servidor no puede fiarse de un cliente al que cualquiera puede sustituir. Lo de aqui es
 * para no gastar una ida y vuelta en decirle al usuario que le falta una cifra en el DNI, y
 * para poder señalar el campo concreto en vez de enseñar un mensaje suelto.
 *
 * Los algoritmos salen de validadores-es, la libreria del proyecto anterior. Es justo el
 * caso para el que se escribio: las mismas reglas en Android, escritorio y navegador sin
 * copiarlas tres veces.
 */
object RevisorDeAlta {

    fun revisar(formulario: FormularioDeAlta): RevisionDelAlta {
        val fallos = mutableMapOf<CampoDeAlta, String>()

        val email = formulario.email.trim().lowercase()
        // Comprobar un email de verdad es imposible sin mandarle un correo; aqui solo se
        // descartan los que ni siquiera lo parecen.
        if (email.isEmpty()) fallos[CampoDeAlta.Email] = "Falta el email."
        else if (!pareceUnEmail(email)) fallos[CampoDeAlta.Email] = "Ese email no tiene buena pinta."

        val nombre = formulario.nombreCompleto.trim()
        if (nombre.isEmpty()) fallos[CampoDeAlta.NombreCompleto] = "Falta el nombre."

        // Camar exige 8 como minimo; pedir mas aqui solo confundiria.
        if (formulario.contrasena.length < MINIMO_CONTRASENA) {
            fallos[CampoDeAlta.Contrasena] = "La contrasena necesita al menos $MINIMO_CONTRASENA caracteres."
        }

        val documento = revisarDocumento(formulario.documento)
        if (documento == null) {
            fallos[CampoDeAlta.Documento] = mensajeDelDocumento(formulario.documento)
        }

        val telefono = Telefono.validar(formulario.telefono)
        if (telefono !is Resultado.Valido) {
            fallos[CampoDeAlta.Telefono] = mensaje(motivoDe(telefono), "el telefono")
        }

        val codigoPostal = CodigoPostal.validar(formulario.codigoPostal)
        if (codigoPostal !is Resultado.Valido) {
            fallos[CampoDeAlta.CodigoPostal] = when (motivoDe(codigoPostal)) {
                // El unico caso en el que la forma es correcta pero el valor no existe es
                // la provincia: solo hay de la 01 a la 52.
                Motivo.ValorNoPermitido -> "Esas dos primeras cifras no son de ninguna provincia."
                else -> mensaje(motivoDe(codigoPostal), "el codigo postal")
            }
        }

        // La cuenta es opcional: solo hace falta si el socio domicilia los pagos.
        val cuenta = formulario.cuentaBancaria.trim()
        var iban: String? = null
        if (cuenta.isNotEmpty()) {
            when (val resultado = Iban.validar(cuenta)) {
                is Resultado.Valido -> iban = resultado.valor.valor
                is Resultado.Invalido -> fallos[CampoDeAlta.CuentaBancaria] =
                    mensaje(resultado.motivo, "la cuenta")
            }
        }

        if (fallos.isNotEmpty()) return RevisionDelAlta.ConFallos(fallos)

        return RevisionDelAlta.Correcta(
            PeticionDeAlta(
                email = email,
                nombreCompleto = nombre,
                contrasena = formulario.contrasena,
                plan = formulario.plan.codigo,
                documento = documento!!,
                telefono = (telefono as Resultado.Valido).valor.valor,
                codigoPostal = (codigoPostal as Resultado.Valido).valor.valor,
                cuentaBancaria = iban,
            )
        )
    }

    /**
     * Camar acepta los tres tipos de documento fiscal, asi que se prueban los tres y vale
     * el que pase. Devuelve el texto ya normalizado, o null si no es ninguno.
     */
    private fun revisarDocumento(texto: String): String? =
        Nif.validar(texto).valorONulo()?.valor
            ?: Nie.validar(texto).valorONulo()?.valor
            ?: Cif.validar(texto).valorONulo()?.valor

    /**
     * Con tres validaciones fallidas hay tres motivos distintos, y decir los tres no ayuda.
     * Se elige el que corresponde a lo que el usuario parecia estar escribiendo: si empieza
     * por X, Y o Z iba a por un NIE, y si empieza por una de las letras de sociedad, un CIF.
     */
    private fun mensajeDelDocumento(texto: String): String {
        val limpio = texto.trim().uppercase()

        if (limpio.isEmpty()) return "Falta el DNI, NIE o CIF."

        val resultado = when (limpio.first()) {
            in "XYZ" -> Nie.validar(limpio)
            in LETRAS_DE_CIF -> Cif.validar(limpio)
            else -> Nif.validar(limpio)
        }

        return mensaje(motivoDe(resultado), "el documento")
    }

    private fun motivoDe(resultado: Resultado<*>): Motivo =
        (resultado as? Resultado.Invalido)?.motivo ?: Motivo.FormatoIncorrecto

    private fun mensaje(motivo: Motivo, que: String): String = when (motivo) {
        Motivo.Vacio -> "Falta ${que.replaceFirstChar { it.lowercase() }}."
        Motivo.LongitudIncorrecta -> "A $que le sobran o le faltan caracteres."
        Motivo.FormatoIncorrecto -> "$que tiene caracteres donde no toca.".replaceFirstChar { it.uppercase() }
        Motivo.ControlIncorrecto -> "El control de $que no cuadra: revisa las cifras."
        Motivo.ValorNoPermitido -> "Ese valor de $que no existe."
    }

    private fun pareceUnEmail(texto: String): Boolean {
        val arroba = texto.indexOf('@')
        val punto = texto.lastIndexOf('.')

        return arroba > 0 && punto > arroba + 1 && punto < texto.length - 1 && !texto.contains(' ')
    }

    private const val MINIMO_CONTRASENA = 8
    private const val LETRAS_DE_CIF = "ABCDEFGHJNPQRSUVW"
}
