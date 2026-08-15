package io.github.mendietagarciaalejandro.estanza.ui.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoDelCatalogo(
    val cargando: Boolean = true,
    val recursos: List<Recurso> = emptyList(),
    val filtro: TipoDeRecurso? = null,
    val error: String? = null,
) {
    val visibles: List<Recurso>
        get() = if (filtro == null) recursos else recursos.filter { it.tipo == filtro }

    /** Solo se ofrecen los filtros de los tipos que de verdad hay. */
    val tiposDisponibles: List<TipoDeRecurso>
        get() = recursos.map { it.tipo }.distinct().sortedBy { it.ordinal }
}

class ModeloDeCatalogo(private val catalogo: CatalogoDeRecursos) : ViewModel() {

    private val flujo = MutableStateFlow(EstadoDelCatalogo())
    val estado: StateFlow<EstadoDelCatalogo> = flujo.asStateFlow()

    private var consulta: Job? = null

    init {
        cargar()
    }

    fun cargar(refrescar: Boolean = false) {
        consulta?.cancel()

        consulta = viewModelScope.launch {
            flujo.value = flujo.value.copy(cargando = true, error = null)

            flujo.value = when (val respuesta = catalogo.recursos(refrescar)) {
                is Respuesta.Exito -> flujo.value.copy(cargando = false, recursos = respuesta.valor)
                // El 401 no se enseña: para cuando llega aqui la sesion ya se ha cerrado
                // sola y la aplicacion esta volviendo a la pantalla de acceso.
                is Respuesta.Fallo -> flujo.value.copy(cargando = false, error = respuesta.error.mensaje)
            }
        }
    }

    fun filtrarPor(tipo: TipoDeRecurso?) {
        // Volver a pulsar el filtro que ya estaba puesto lo quita.
        flujo.value = flujo.value.copy(filtro = if (flujo.value.filtro == tipo) null else tipo)
    }
}
