package io.github.mendietagarciaalejandro.estanza.datos

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import kotlin.jvm.JvmInline

/**
 * Una cantidad de dinero, guardada como los digitos exactos que mando el servidor.
 *
 * No se usa Double a proposito. Un precio no es una medida aproximada: es una cantidad
 * exacta que acaba en una factura, y en coma flotante 0.1 + 0.2 no da 0.3. En comun no hay
 * BigDecimal, asi que se conserva el texto tal cual llega y se pasa a centimos con enteros
 * solo para pintarlo.
 */
@Serializable(with = ImporteComoLlega::class)
@JvmInline
value class Importe(val texto: String) {

    /** "18,00 €". El separador decimal es la coma porque la aplicacion habla español. */
    fun conMoneda(): String {
        val centimos = aCentimos()
        val euros = centimos / 100
        val resto = (centimos % 100).toString().padStart(2, '0')

        return "$euros,$resto €"
    }

    val esCero: Boolean get() = aCentimos() == 0L

    /**
     * Camar manda el mismo precio unas veces con dos decimales y otras con tres, segun de
     * donde salga: "18.00" al listar y "18.000" al crear. Se redondea a centimos, que es la
     * unidad en la que se cobra de verdad.
     */
    private fun aCentimos(): Long {
        val negativo = texto.startsWith('-')
        val limpio = texto.trimStart('-', '+')

        val entera = limpio.substringBefore('.').ifEmpty { "0" }
        // Un tercer decimal siempre presente hace que redondear sea mirar un solo caracter.
        val decimales = limpio.substringAfter('.', "").padEnd(3, '0')

        val centimos = entera.toLong() * 100 +
            decimales.take(2).toLong() +
            if (decimales[2] >= '5') 1 else 0

        return if (negativo) -centimos else centimos
    }
}

/**
 * Lee el importe sin pasarlo por Double: el JSON lo trae como numero, y se coge su texto
 * literal en vez de dejar que el deserializador lo convierta.
 */
internal object ImporteComoLlega : KSerializer<Importe> {
    override val descriptor = PrimitiveSerialDescriptor("Importe", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Importe {
        val json = decoder as? JsonDecoder ?: return Importe(decoder.decodeString())

        return Importe(json.decodeJsonElement().jsonPrimitive.content)
    }

    override fun serialize(encoder: Encoder, value: Importe) = encoder.encodeString(value.texto)
}
