package io.github.mendietagarciaalejandro.estanza.ui.acceso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoDeAcceso(
    val email: String = "",
    val contrasena: String = "",
    val entrando: Boolean = false,
    val error: String? = null,
) {
    /** No valida nada: solo evita mandar una peticion con el formulario a medias. */
    val sePuedeEnviar: Boolean
        get() = !entrando && email.isNotBlank() && contrasena.isNotBlank()
}

class ModeloDeAcceso(
    private val api: ApiDeCamar,
    private val sesiones: AlmacenDeSesion,
) : ViewModel() {

    private val flujo = MutableStateFlow(EstadoDeAcceso())
    val estado: StateFlow<EstadoDeAcceso> = flujo.asStateFlow()

    fun escribirEmail(texto: String) {
        flujo.value = flujo.value.copy(email = texto, error = null)
    }

    fun escribirContrasena(texto: String) {
        flujo.value = flujo.value.copy(contrasena = texto, error = null)
    }

    /**
     * No hace falta avisar a nadie de que ha salido bien: al abrir la sesion cambia el
     * StateFlow del almacen, y es eso lo que hace que la aplicacion pase a las pantallas
     * de dentro. Asi solo hay un sitio que decida si estas dentro o fuera.
     */
    fun entrar() {
        if (!flujo.value.sePuedeEnviar) return

        viewModelScope.launch {
            flujo.value = flujo.value.copy(entrando = true, error = null)

            val estadoActual = flujo.value
            when (val respuesta = api.acceder(estadoActual.email.trim(), estadoActual.contrasena)) {
                is Respuesta.Exito -> {
                    sesiones.abrir(respuesta.valor)
                    // Se apaga el indicador despues de abrir la sesion, no antes: asi
                    // "ya no esta entrando" significa de verdad que ha terminado todo.
                    // La contrasena deja de hacer falta en cuanto hay token.
                    flujo.value = flujo.value.copy(entrando = false, contrasena = "")
                }

                // El mensaje de Camar se enseña tal cual. El del 401 dice "Email o
                // contrasena incorrectos." sin decir cual de los dos, y esta bien que sea
                // asi: separarlos dejaria averiguar que emails estan dados de alta.
                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    entrando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }
}
