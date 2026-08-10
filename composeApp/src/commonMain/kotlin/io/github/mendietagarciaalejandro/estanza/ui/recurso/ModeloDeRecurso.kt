package io.github.mendietagarciaalejandro.estanza.ui.recurso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.Disponibilidad
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.aDisponibilidad
import io.github.mendietagarciaalejandro.estanza.datos.hoy
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import io.github.mendietagarciaalejandro.estanza.red.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

data class EstadoDelRecurso(
    val recurso: Recurso? = null,
    val fecha: LocalDate,
    val hoy: LocalDate,
    val cargando: Boolean = true,
    val disponibilidad: Disponibilidad? = null,
    val error: String? = null,
) {
    /** No se puede reservar el pasado, asi que tampoco tiene sentido mirarlo. */
    val sePuedeRetroceder: Boolean get() = fecha > hoy

    val esHoy: Boolean get() = fecha == hoy
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
            val encontrado = catalogo.recurso(idRecurso).map { it }

            if (encontrado is Respuesta.Exito) {
                flujo.value = flujo.value.copy(recurso = encontrado.valor)
            }
        }

        consultar(diaDeHoy)
    }

    fun diaAnterior() {
        if (flujo.value.sePuedeRetroceder) consultar(flujo.value.fecha.minusUnDia())
    }

    fun diaSiguiente() = consultar(flujo.value.fecha.masUnDia())

    fun reintentar() = consultar(flujo.value.fecha)

    private fun consultar(fecha: LocalDate) {
        // Si se pulsa la flecha varias veces seguidas solo importa la ultima fecha.
        consulta?.cancel()

        consulta = viewModelScope.launch {
            flujo.value = flujo.value.copy(fecha = fecha, cargando = true, error = null)

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

// kotlinx-datetime no tiene un "dia siguiente" a secas para LocalDate en todas las
// versiones, y hacerlo con periodos aqui deja el modelo mas legible.
private fun LocalDate.masUnDia() = LocalDate.fromEpochDays(toEpochDays() + 1)
private fun LocalDate.minusUnDia() = LocalDate.fromEpochDays(toEpochDays() - 1)
