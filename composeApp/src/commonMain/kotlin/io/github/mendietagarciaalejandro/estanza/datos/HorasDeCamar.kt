package io.github.mendietagarciaalejandro.estanza.datos

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Camar manda las horas con desfase +00:00 pero queriendo decir la hora local del
 * coworking: su horario de apertura se construye asi, y la apertura de las ocho llega como
 * "08:00:00+00:00". Es una simplificacion conocida del servidor, esta escrita en su
 * OpeningHoursPolicy.
 *
 * Mientras siga siendo asi, para pintar una hora hay que leerla en UTC. Convertirla a la
 * zona del movil la corre dos horas en verano y el usuario ve que el coworking abre a las
 * diez. Este es el unico sitio del cliente que sabe de esto: el dia que Camar mande
 * instantes de verdad con su zona, se cambia aqui y ya.
 */
private val ZonaDeLasHorasDeCamar = TimeZone.UTC

/** "08:30". Es lo unico que hace falta: los huecos nunca cruzan la medianoche. */
fun Instant.comoHora(): String {
    val hora = toLocalDateTime(ZonaDeLasHorasDeCamar)

    return "${hora.hour.aDosCifras()}:${hora.minute.aDosCifras()}"
}

/** La hora del dia, de 0 a 23, segun el reloj del coworking. Para separar mañana de tarde. */
fun Instant.horaDeCamar(): Int = toLocalDateTime(ZonaDeLasHorasDeCamar).hour

/** El dia al que pertenece un instante segun el calendario del coworking. */
fun Instant.fechaDeCamar(): LocalDate = toLocalDateTime(ZonaDeLasHorasDeCamar).date

/**
 * El dia de hoy segun el calendario de quien usa la aplicacion, no segun UTC: si son las
 * doce y media de la noche en Madrid, para el usuario ya es manana.
 */
fun hoy(reloj: Clock): LocalDate = reloj.todayIn(TimeZone.currentSystemDefault())

private val diasDeLaSemana = listOf(
    "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo",
)

private val meses = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/**
 * "miercoles 12 de agosto".
 *
 * A mano y en español porque el formateo por locale no existe igual en las tres
 * plataformas, y arrastrar una libreria de internacionalizacion para una aplicacion que
 * solo habla español no compensa.
 */
fun LocalDate.comoFechaLarga(): String {
    val dia = diasDeLaSemana[dayOfWeek.ordinal]
    val mes = meses[month.ordinal]

    return "$dia $day de $mes"
}

/** El formato que espera el parametro date de la API. */
fun LocalDate.comoParametro(): String = "$year-${(month.ordinal + 1).aDosCifras()}-${day.aDosCifras()}"

private fun Int.aDosCifras() = toString().padStart(2, '0')
