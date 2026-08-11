package io.github.mendietagarciaalejandro.estanza.red

import io.github.mendietagarciaalejandro.estanza.datos.Importe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Los nombres de las propiedades van en español y los de JSON en ingles con @SerialName,
 * porque el contrato lo pone Camar y ahi no se toca nada.
 */

/** Los planes de socio de Camar. Van por numero: la API los espera como el entero del enum. */
enum class Plan(val codigo: Int) {
    /** Reserva con antelacion, hasta cierta ventana de dias. */
    Flex(1),

    /** Bono de un dia suelto. */
    BonoDia(2),
}

@Serializable
data class PeticionDeAlta(
    val email: String,
    @SerialName("fullName") val nombreCompleto: String,
    @SerialName("password") val contrasena: String,
    @SerialName("plan") val plan: Int,
    @SerialName("taxId") val documento: String,
    @SerialName("phone") val telefono: String,
    @SerialName("postalCode") val codigoPostal: String,
    @SerialName("bankAccount") val cuentaBancaria: String? = null,
)

@Serializable
data class PeticionDeAcceso(
    val email: String,
    @SerialName("password") val contrasena: String,
)

@Serializable
data class RespuestaDeAutenticacion(
    @SerialName("token") val token: String,
    // Llega como ISO-8601 con desfase horario. Se deja en texto y se convierte al pasar a
    // Sesion, que es donde importa que sea un instante.
    @SerialName("expiresAt") val caducaEn: String,
    @SerialName("userId") val idUsuario: String,
    @SerialName("role") val rol: String,
)

@Serializable
data class RecursoDto(
    @SerialName("id") val id: String,
    @SerialName("name") val nombre: String,
    // MeetingRoom, HotDesk o PhoneBooth. Camar los serializa con ToString().
    @SerialName("type") val tipo: String,
    @SerialName("capacity") val capacidad: Int,
)

@Serializable
data class PeticionDeReserva(
    @SerialName("resourceId") val idRecurso: String,
    // ISO-8601. Se manda con "Z", que .NET entiende como desfase cero, que es justo lo que
    // Camar espera (ver la nota de zonas horarias).
    @SerialName("start") val inicio: String,
    @SerialName("end") val fin: String,
)

@Serializable
data class ReservaDto(
    @SerialName("id") val id: String,
    @SerialName("resourceId") val idRecurso: String,
    // Solo se usa en administracion, para distinguir de quien es cada reserva.
    @SerialName("userId") val idUsuario: String = "",
    @SerialName("start") val inicio: String,
    @SerialName("end") val fin: String,
    // Confirmed, Cancelled, Completed o NoShow.
    @SerialName("status") val estado: String,
    @SerialName("price") val precio: Importe,
    @SerialName("cancelledAt") val canceladaEn: String? = null,
    @SerialName("refundAmount") val reembolso: Importe? = null,
)

@Serializable
data class PeticionDeRecurso(
    @SerialName("name") val nombre: String,
    // El enum viaja como su numero, que es como lo espera System.Text.Json sin convertidor.
    @SerialName("type") val tipo: Int,
    @SerialName("capacity") val capacidad: Int,
)

@Serializable
data class PeticionDeDiaBloqueado(
    // Un DateOnly de .NET: "2026-08-15", con los ceros puestos.
    @SerialName("date") val fecha: String,
    @SerialName("reason") val motivo: String,
)

@Serializable
data class DiaBloqueadoDto(
    @SerialName("id") val id: String,
    @SerialName("date") val fecha: String,
    @SerialName("reason") val motivo: String,
)

@Serializable
data class HuecoDto(
    @SerialName("start") val inicio: String,
    @SerialName("end") val fin: String,
)

@Serializable
data class DisponibilidadDto(
    @SerialName("resourceId") val idRecurso: String,
    @SerialName("date") val fecha: String,
    // Vacio cuando el coworking cierra ese dia o cuando no queda un solo hueco: desde
    // fuera no se distingue, Camar contesta lo mismo en los dos casos.
    @SerialName("freeSlots") val huecosLibres: List<HuecoDto>,
)
