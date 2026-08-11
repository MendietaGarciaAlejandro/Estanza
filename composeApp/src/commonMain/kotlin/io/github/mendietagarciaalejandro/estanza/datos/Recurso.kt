package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.DiaBloqueadoDto
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
enum class TipoDeRecurso(val etiqueta: String, val codigo: Int?) {
    SalaDeReuniones("Sala de reuniones", 1),
    MesaFlexible("Mesa flexible", 2),
    Cabina("Cabina de llamadas", 3),

    // Sin codigo: un tipo que este cliente no conoce se puede enseñar, pero no se puede
    // dar de alta, porque no sabriamos que numero mandarle al servidor.
    Otro("Otro", null),
    ;

    companion object {
        fun desde(deLaApi: String) = when (deLaApi) {
            "MeetingRoom" -> SalaDeReuniones
            "HotDesk" -> MesaFlexible
            "PhoneBooth" -> Cabina
            else -> Otro
        }

        /** Los que se pueden crear desde la pantalla de administracion. */
        val creables: List<TipoDeRecurso> get() = entries.filter { it.codigo != null }
    }
}

data class DiaBloqueado(val id: String, val fecha: LocalDate, val motivo: String)

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

fun DiaBloqueadoDto.aDiaBloqueado() = DiaBloqueado(
    id = id,
    fecha = LocalDate.parse(fecha),
    motivo = motivo,
)

fun DisponibilidadDto.aDisponibilidad() = Disponibilidad(
    fecha = LocalDate.parse(fecha),
    huecos = huecosLibres.map { Hueco(Instant.parse(it.inicio), Instant.parse(it.fin)) },
)
