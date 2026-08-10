package io.github.mendietagarciaalejandro.estanza.ui.conexion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.plataforma.Plataforma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EstadoDeConexion(
    val nombreDePlataforma: String = "",
    val urlGuardada: String = "",
    val urlPorDefecto: String = "",
    val borrador: String = "",
    val error: String? = null,
    val guardado: Boolean = false,
) {
    val hayCambios: Boolean get() = borrador.trim().trimEnd('/') != urlGuardada
}

class ModeloDeConexion(
    private val ajustes: AjustesDeConexion,
    private val plataforma: Plataforma,
) : ViewModel() {

    private val borrador = MutableStateFlow(ajustes.urlBase.value)
    private val error = MutableStateFlow<String?>(null)
    private val guardado = MutableStateFlow(false)

    val estado: StateFlow<EstadoDeConexion> =
        combine(ajustes.urlBase, borrador, error, guardado) { url, texto, fallo, ok ->
            EstadoDeConexion(
                nombreDePlataforma = plataforma.nombre,
                urlGuardada = url,
                urlPorDefecto = ajustes.urlPorDefecto,
                borrador = texto,
                error = fallo,
                guardado = ok,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoDeConexion(
                nombreDePlataforma = plataforma.nombre,
                urlGuardada = ajustes.urlBase.value,
                urlPorDefecto = ajustes.urlPorDefecto,
                borrador = ajustes.urlBase.value,
            ),
        )

    fun escribir(texto: String) {
        borrador.value = texto
        // El error de la vez anterior estorba en cuanto el usuario vuelve a escribir.
        error.value = null
        guardado.value = false
    }

    fun guardar() {
        val fallo = ajustes.guardar(borrador.value)

        error.value = fallo
        guardado.value = fallo == null

        if (fallo == null) borrador.value = ajustes.urlBase.value
    }

    fun restablecer() {
        ajustes.restablecer()
        borrador.value = ajustes.urlBase.value
        error.value = null
        guardado.value = false
    }
}
