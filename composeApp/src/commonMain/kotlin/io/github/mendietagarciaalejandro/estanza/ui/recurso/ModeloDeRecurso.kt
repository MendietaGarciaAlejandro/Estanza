package io.github.mendietagarciaalejandro.estanza.ui.recurso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.Disponibilidad
import io.github.mendietagarciaalejandro.estanza.datos.Hueco
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.aDisponibilidad
import io.github.mendietagarciaalejandro.estanza.datos.aReserva
import io.github.mendietagarciaalejandro.estanza.datos.comoHora
import io.github.mendietagarciaalejandro.estanza.datos.hoy
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/**
 * Los huecos elegidos, por posicion dentro de la lista de libres. Siempre son seguidos:
 * una reserva es un periodo, no una coleccion de ratos sueltos.
 */
data class Seleccion(val desde: Int, val hasta: Int) {
    val cuantos: Int get() = hasta - desde + 1

    operator fun contains(indice: Int) = indice in desde..hasta
}

data class EstadoDelRecurso(
    val recurso: Recurso? = null,
    val fecha: LocalDate,
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val disponibilidad: Disponibilidad? = null,
    val error: String? = null,
    val seleccion: Seleccion? = null,
    val reservando: Boolean = false,
    val errorDeReserva: String? = null,
    val reservaHecha: String? = null,
) {
    val huecos: List<Hueco> get() = disponibilidad?.huecos.orEmpty()

    val sePuedeRetroceder: Boolean get() = fecha > hoy

    val esHoy: Boolean get() = fecha == hoy

    /** "de 10:00 a 12:30, 2 h 30 min". Lo que se va a mandar, dicho en claro. */
    val resumenDeLaSeleccion: String?
        get() {
            val elegida = seleccion ?: return null
            val inicio = huecos.getOrNull(elegida.desde) ?: return null
            val fin = huecos.getOrNull(elegida.hasta) ?: return null

            val minutos = elegida.cuantos * 30
            val duracion = when {
                minutos < 60 -> "$minutos min"
                minutos % 60 == 0 -> "${minutos / 60} h"
                else -> "${minutos / 60} h ${minutos % 60} min"
            }

            return "de ${inicio.inicio.comoHora()} a ${fin.fin.comoHora()}, $duracion"
        }
}

class ModeloDeRecurso(
    private val idRecurso: String,
    private val catalogo: CatalogoDeRecursos,
    private val api: ApiDeCamar,
    reloj: Clock,
) : ViewModel() {

    private val diaDeHoy = hoy(reloj)

    private val flujo = MutableStateFlow(EstadoDelRecurso(fecha = diaDeHoy, hoy = diaDeHoy))
    val estado: StateFlow<EstadoDelRecurso> = flujo.asStateFlow()

    private var consulta: Job? = null

    init {
        viewModelScope.launch {
            // Normalmente ya esta en memoria porque se viene del catalogo, asi que la ficha
            // aparece con su nombre puesto y sin parpadeo.
            val encontrado = catalogo.recurso(idRecurso)

            if (encontrado is Respuesta.Exito) {
                flujo.value = flujo.value.copy(recurso = encontrado.valor)
            }
        }

        consultar(diaDeHoy)
    }

    fun diaAnterior() {
        if (flujo.value.sePuedeRetroceder) consultar(flujo.value.fecha.menosUnDia())
    }

    fun diaSiguiente() = consultar(flujo.value.fecha.masUnDia())

    fun reintentar() = consultar(flujo.value.fecha)

    /**
     * Primer toque: se elige ese hueco. Segundo toque mas adelante: se estira la seleccion
     * hasta el, siempre que no haya un rato ocupado en medio. Volver a tocar lo ya elegido
     * lo suelta.
     *
     * Hace falta poder elegir un rato largo y no un bloque suelto porque las mesas
     * flexibles tienen un minimo de varias horas; con media hora Camar contesta un 422.
     */
    fun pulsarHueco(indice: Int) {
        val actual = flujo.value
        val elegida = actual.seleccion

        val nueva = when {
            elegida == null -> Seleccion(indice, indice)
            indice in elegida -> null
            indice > elegida.hasta && sonSeguidos(actual.huecos, elegida.desde, indice) ->
                Seleccion(elegida.desde, indice)
            // Hacia atras, o con algo ocupado en medio, se empieza de cero en vez de
            // inventarse un rango que el usuario no ha pedido.
            else -> Seleccion(indice, indice)
        }

        flujo.value = actual.copy(seleccion = nueva, errorDeReserva = null, reservaHecha = null)
    }

    fun reservar() {
        val actual = flujo.value
        val elegida = actual.seleccion ?: return
        if (actual.reservando) return

        val inicio = actual.huecos.getOrNull(elegida.desde)?.inicio ?: return
        val fin = actual.huecos.getOrNull(elegida.hasta)?.fin ?: return

        viewModelScope.launch {
            flujo.value = actual.copy(reservando = true, errorDeReserva = null, reservaHecha = null)

            when (val respuesta = api.crearReserva(idRecurso, inicio, fin)) {
                is Respuesta.Exito -> {
                    val reserva = respuesta.valor.aReserva()

                    flujo.value = flujo.value.copy(
                        reservando = false,
                        seleccion = null,
                        reservaHecha = "Reservado ${reserva.comoFranja()}. Son ${reserva.precio.conMoneda()}.",
                    )

                    // El hueco ya no esta libre: hay que volver a preguntar en vez de
                    // quitarlo de la lista por nuestra cuenta y fiarnos de haber acertado.
                    consultar(flujo.value.fecha, conservarAvisos = true)
                }

                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    reservando = false,
                    // El mensaje de Camar ya viene redactado: "Ese hueco ya esta
                    // reservado.", "El plan Flex reserva como mucho con N dias de
                    // antelacion.". Repetirlo aqui seria copiar sus reglas.
                    errorDeReserva = respuesta.error.mensaje,
                )
            }
        }
    }

    private fun consultar(fecha: LocalDate, conservarAvisos: Boolean = false) {
        // Si se pulsa la flecha varias veces seguidas solo importa la ultima fecha.
        consulta?.cancel()

        consulta = viewModelScope.launch {
            val previo = flujo.value

            flujo.value = previo.copy(
                fecha = fecha,
                cargando = true,
                error = null,
                seleccion = null,
                errorDeReserva = if (conservarAvisos) previo.errorDeReserva else null,
                reservaHecha = if (conservarAvisos) previo.reservaHecha else null,
            )

            flujo.value = when (val respuesta = api.disponibilidad(idRecurso, fecha)) {
                is Respuesta.Exito -> flujo.value.copy(
                    cargando = false,
                    disponibilidad = respuesta.valor.aDisponibilidad(),
                )

                is Respuesta.Fallo -> flujo.value.copy(
                    cargando = false,
                    disponibilidad = null,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }
}

/**
 * Camar devuelve los huecos ordenados, pero no tienen por que ser consecutivos: entre dos
 * puede haber un rato ya reservado. Se comprueba que cada uno termine justo donde empieza
 * el siguiente.
 */
private fun sonSeguidos(huecos: List<Hueco>, desde: Int, hasta: Int): Boolean {
    for (i in desde until hasta) {
        if (huecos[i].fin != huecos[i + 1].inicio) return false
    }

    return true
}

// kotlinx-datetime no tiene un "dia siguiente" a secas para LocalDate en todas las
// versiones, y hacerlo con periodos aqui deja el modelo mas legible.
private fun LocalDate.masUnDia() = LocalDate.fromEpochDays(toEpochDays() + 1)
private fun LocalDate.menosUnDia() = LocalDate.fromEpochDays(toEpochDays() - 1)
