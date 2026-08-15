package io.github.mendietagarciaalejandro.estanza.ui.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.aReserva
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoDeReservas(
    val cargando: Boolean = true,
    val reservas: List<Reserva> = emptyList(),
    /** Nombre del recurso por id, para no enseñar un uuid en la tarjeta. */
    val nombres: Map<String, String> = emptyMap(),
    val error: String? = null,
    /** La que se esta preguntando si cancelar de verdad. */
    val porCancelar: Reserva? = null,
    val cancelando: Boolean = false,
    val aviso: String? = null,
)

class ModeloDeReservas(
    private val api: ApiDeCamar,
    private val catalogo: CatalogoDeRecursos,
) : ViewModel() {

    private val flujo = MutableStateFlow(EstadoDeReservas())
    val estado: StateFlow<EstadoDeReservas> = flujo.asStateFlow()

    private var consulta: Job? = null

    init {
        cargar()
    }

    /**
     * Se llama al crear el modelo y cada vez que se vuelve a la pestaña, asi que puede
     * haber una carga en marcha; la anterior ya no interesa.
     */
    fun cargar() {
        consulta?.cancel()

        consulta = viewModelScope.launch {
            flujo.value = flujo.value.copy(cargando = true, error = null)

            // El catalogo suele estar ya en memoria, asi que esto no gasta otra peticion.
            val recursos = catalogo.recursos()
            val nombres = if (recursos is Respuesta.Exito) {
                recursos.valor.associate { it.id to it.nombre }
            } else {
                flujo.value.nombres
            }

            flujo.value = when (val respuesta = api.misReservas()) {
                is Respuesta.Exito -> flujo.value.copy(
                    cargando = false,
                    reservas = respuesta.valor.map { it.aReserva() },
                    nombres = nombres,
                )

                is Respuesta.Fallo -> flujo.value.copy(
                    cargando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }

    /**
     * Cancelar cuesta dinero segun la antelacion, asi que no se hace de un solo toque.
     */
    fun preguntarSiCancelar(reserva: Reserva) {
        flujo.value = flujo.value.copy(porCancelar = reserva, aviso = null)
    }

    fun dejarloEstar() {
        flujo.value = flujo.value.copy(porCancelar = null)
    }

    fun confirmarCancelacion() {
        val reserva = flujo.value.porCancelar ?: return

        viewModelScope.launch {
            flujo.value = flujo.value.copy(cancelando = true, porCancelar = null, aviso = null)

            when (val respuesta = api.cancelarReserva(reserva.id)) {
                is Respuesta.Exito -> {
                    val cancelada = respuesta.valor.aReserva()
                    val devuelto = cancelada.reembolso

                    flujo.value = flujo.value.copy(
                        cancelando = false,
                        // El reembolso lo decide Camar segun la antelacion; aqui solo se
                        // cuenta lo que ha decidido, sin recalcularlo por nuestra cuenta.
                        aviso = when {
                            devuelto == null -> "Reserva cancelada."
                            devuelto.esCero -> "Reserva cancelada. Con tan poca antelacion no se devuelve nada."
                            else -> "Reserva cancelada. Se te devuelven ${devuelto.conMoneda()}."
                        },
                        // Se recarga en vez de cambiar la de la lista a mano: al cancelar
                        // se libera el hueco y el estado de verdad lo tiene el servidor.
                        reservas = flujo.value.reservas.map { if (it.id == cancelada.id) cancelada else it },
                    )
                }

                // El caso tipico es un 409 "No se puede operar sobre una reserva en estado
                // Cancelled": la cancelaste desde otro sitio mientras mirabas esta lista.
                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    cancelando = false,
                    aviso = respuesta.error.mensaje,
                )
            }
        }
    }
}
