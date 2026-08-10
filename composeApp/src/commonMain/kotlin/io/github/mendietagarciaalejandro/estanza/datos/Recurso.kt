package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.DisponibilidadDto
import io.github.mendietagarciaalejandro.estanza.red.RecursoDto
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Los tipos de recurso que hay en Camar.
 *
 * [Otro] existe para que añadir un tipo en el servidor no reviente al cliente: se pinta con
 * su nombre en crudo y el resto de la aplicacion sigue funcionando. Un enum sin salida
 * obligaria a publicar una version nueva cada vez que el coworking compra un mueble.
 */
enum class TipoDeRecurso(val etiqueta: String) {
    SalaDeReuniones("Sala de reuniones"),
    MesaFlexible("Mesa flexible"),
    Cabina("Cabina de llamadas"),
    Otro("Otro"),
    ;

    companion object {
        fun desde(deLaApi: String) = when (deLaApi) {
            "MeetingRoom" -> SalaDeReuniones
            "HotDesk" -> MesaFlexible
            "PhoneBooth" -> Cabina
            else -> Otro
        }
    }
}

data class Recurso(
    val id: String,
    val nombre: String,
    val tipo: TipoDeRecurso,
    val capacidad: Int,
) {
    val capacidadEnTexto: String
        get() = if (capacidad == 1) "1 persona" else "$capacidad personas"
}

/** Media hora libre. Camar los devuelve siempre en bloques de treinta minutos. */
data class Hueco(val inicio: Instant, val fin: Instant) {
    fun comoFranja(): String = "${inicio.comoHora()} - ${fin.comoHora()}"
}

data class Disponibilidad(val fecha: LocalDate, val huecos: List<Hueco>)

fun RecursoDto.aRecurso() = Recurso(
    id = id,
    nombre = nombre,
    tipo = TipoDeRecurso.desde(tipo),
    capacidad = capacidad,
)

fun DisponibilidadDto.aDisponibilidad() = Disponibilidad(
    fecha = LocalDate.parse(fecha),
    huecos = huecosLibres.map { Hueco(Instant.parse(it.inicio), Instant.parse(it.fin)) },
)
