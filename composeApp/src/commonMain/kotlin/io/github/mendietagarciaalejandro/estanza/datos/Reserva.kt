package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.ReservaDto
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Igual que con los tipos de recurso, hay una salida para lo que no se conozca: un estado
 * nuevo en el servidor no puede tumbar la pantalla de reservas.
 */
enum class EstadoDeReserva(val etiqueta: String) {
    Confirmada("Confirmada"),
    Cancelada("Cancelada"),
    Completada("Completada"),
    NoPresentado("No se presento"),
    Otro("Otro"),
    ;

    companion object {
        fun desde(deLaApi: String) = when (deLaApi) {
            "Confirmed" -> Confirmada
            "Cancelled" -> Cancelada
            "Completed" -> Completada
            "NoShow" -> NoPresentado
            else -> Otro
        }
    }
}

data class Reserva(
    val id: String,
    val idRecurso: String,
    val inicio: Instant,
    val fin: Instant,
    val estado: EstadoDeReserva,
    val precio: Importe,
    val reembolso: Importe?,
) {
    val fecha: LocalDate get() = inicio.fechaDeCamar()

    fun comoFranja(): String = "${inicio.comoHora()} - ${fin.comoHora()}"

    /** Solo se puede cancelar lo que sigue en pie. */
    val sePuedeCancelar: Boolean get() = estado == EstadoDeReserva.Confirmada
}

fun ReservaDto.aReserva() = Reserva(
    id = id,
    idRecurso = idRecurso,
    inicio = Instant.parse(inicio),
    fin = Instant.parse(fin),
    estado = EstadoDeReserva.desde(estado),
    precio = precio,
    reembolso = reembolso,
)
