package io.github.mendietagarciaalejandro.estanza.ui.conexion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.plataforma.Plataforma
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ResultadoDeLaPrueba {
    data object Responde : ResultadoDeLaPrueba
    data class NoResponde(val motivo: String) : ResultadoDeLaPrueba
}

data class EstadoDeConexion(
    val nombreDePlataforma: String = "",
    val urlGuardada: String = "",
    val urlPorDefecto: String = "",
    val borrador: String = "",
    val error: String? = null,
    val guardado: Boolean = false,
    val comprobando: Boolean = false,
    val prueba: ResultadoDeLaPrueba? = null,
) {
    val hayCambios: Boolean get() = borrador.trim().trimEnd('/') != urlGuardada
}

class ModeloDeConexion(
    private val ajustes: AjustesDeConexion,
    private val plataforma: Plataforma,
    private val api: ApiDeCamar,
) : ViewModel() {

    private val flujo = MutableStateFlow(
        EstadoDeConexion(
            nombreDePlataforma = plataforma.nombre,
            urlGuardada = ajustes.urlBase.value,
            urlPorDefecto = ajustes.urlPorDefecto,
            borrador = ajustes.urlBase.value,
        )
    )

    val estado: StateFlow<EstadoDeConexion> = flujo.asStateFlow()

    private var comprobacion: Job? = null

    fun escribir(texto: String) {
        // Lo que se probo antes ya no vale: se esta apuntando a otro sitio.
        flujo.value = flujo.value.copy(
            borrador = texto,
            error = null,
            guardado = false,
            prueba = null,
        )
    }

    fun guardar() {
        val fallo = ajustes.guardar(flujo.value.borrador)

        flujo.value = flujo.value.copy(
            urlGuardada = ajustes.urlBase.value,
            borrador = if (fallo == null) ajustes.urlBase.value else flujo.value.borrador,
            error = fallo,
            guardado = fallo == null,
            prueba = null,
        )
    }

    fun restablecer() {
        ajustes.restablecer()

        flujo.value = flujo.value.copy(
            urlGuardada = ajustes.urlBase.value,
            borrador = ajustes.urlBase.value,
            error = null,
            guardado = false,
            prueba = null,
        )
    }

    /**
     * Manda una peticion de verdad para ver si al otro lado hay un Camar. Es la unica forma
     * de saberlo: mirar la URL solo dice si esta bien escrita.
     */
    fun probar() {
        // Si el usuario le da dos veces, la primera comprobacion sobra.
        comprobacion?.cancel()

        comprobacion = viewModelScope.launch {
            flujo.value = flujo.value.copy(comprobando = true, prueba = null)

            val resultado = when (val respuesta = api.comprobarConexion()) {
                is Respuesta.Exito -> ResultadoDeLaPrueba.Responde
                is Respuesta.Fallo -> ResultadoDeLaPrueba.NoResponde(respuesta.error.mensaje)
            }

            flujo.value = flujo.value.copy(comprobando = false, prueba = resultado)
        }
    }
}
