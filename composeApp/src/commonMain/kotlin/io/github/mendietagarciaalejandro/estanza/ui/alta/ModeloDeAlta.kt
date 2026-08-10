package io.github.mendietagarciaalejandro.estanza.ui.alta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CampoDeAlta
import io.github.mendietagarciaalejandro.estanza.datos.FormularioDeAlta
import io.github.mendietagarciaalejandro.estanza.datos.RevisionDelAlta
import io.github.mendietagarciaalejandro.estanza.datos.RevisorDeAlta
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.ErrorDeApi
import io.github.mendietagarciaalejandro.estanza.red.Plan
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoDeAlta(
    val formulario: FormularioDeAlta = FormularioDeAlta(),
    val fallos: Map<CampoDeAlta, String> = emptyMap(),
    val enviando: Boolean = false,
    /** Lo que no se puede colgar de ningun campo: un 409, una caida de red... */
    val error: String? = null,
)

class ModeloDeAlta(
    private val api: ApiDeCamar,
    private val sesiones: AlmacenDeSesion,
) : ViewModel() {

    private val flujo = MutableStateFlow(EstadoDeAlta())
    val estado: StateFlow<EstadoDeAlta> = flujo.asStateFlow()

    /**
     * Al escribir se borra el fallo de ese campo, no todos: los demas siguen estando mal y
     * hacerlos desaparecer al tocar otra casilla despista.
     */
    fun editar(campo: CampoDeAlta, cambio: FormularioDeAlta.() -> FormularioDeAlta) {
        val actual = flujo.value

        flujo.value = actual.copy(
            formulario = actual.formulario.cambio(),
            fallos = actual.fallos - campo,
            error = null,
        )
    }

    /** El plan no se puede escribir mal, asi que no arrastra fallo que borrar. */
    fun elegirPlan(plan: Plan) {
        val actual = flujo.value

        flujo.value = actual.copy(formulario = actual.formulario.copy(plan = plan))
    }

    fun enviar() {
        if (flujo.value.enviando) return

        when (val revision = RevisorDeAlta.revisar(flujo.value.formulario)) {
            // Se revisa antes de salir a la red: si falta una cifra en el DNI no hace falta
            // molestar al servidor para saberlo.
            is RevisionDelAlta.ConFallos -> flujo.value = flujo.value.copy(fallos = revision.porCampo)

            is RevisionDelAlta.Correcta -> viewModelScope.launch {
                flujo.value = flujo.value.copy(enviando = true, fallos = emptyMap(), error = null)

                when (val respuesta = api.registrar(revision.peticion)) {
                    // Camar devuelve el token en el alta, asi que se entra directo sin
                    // pedirle al recien registrado que vuelva a escribir la contrasena.
                    is Respuesta.Exito -> {
                        sesiones.abrir(respuesta.valor)
                        flujo.value = EstadoDeAlta()
                    }

                    is Respuesta.Fallo -> flujo.value = conFalloDelServidor(respuesta.error)
                }
            }
        }
    }

    /**
     * Reparte el error del servidor entre los campos cuando se puede.
     *
     * El 409 llega con el detalle en texto, sin decir que campo choca, asi que se mira el
     * mensaje: es feo, pero la alternativa es enseñar "ya existe" sin señalar donde. El 400
     * si trae los campos, aunque con los nombres del contrato en ingles.
     */
    private fun conFalloDelServidor(error: ErrorDeApi): EstadoDeAlta {
        val actual = flujo.value.copy(enviando = false)

        return when (error) {
            is ErrorDeApi.DatosInvalidos -> actual.copy(
                fallos = error.porCampo.mapNotNull { (campo, mensajes) ->
                    campoDelContrato(campo)?.let { it to mensajes.first() }
                }.toMap(),
                error = if (error.porCampo.keys.any { campoDelContrato(it) == null }) error.mensaje else null,
            )

            is ErrorDeApi.Conflicto -> {
                val campo = when {
                    error.mensaje.contains("documento", ignoreCase = true) -> CampoDeAlta.Documento
                    error.mensaje.contains("email", ignoreCase = true) -> CampoDeAlta.Email
                    else -> null
                }

                if (campo != null) actual.copy(fallos = mapOf(campo to error.mensaje))
                else actual.copy(error = error.mensaje)
            }

            else -> actual.copy(error = error.mensaje)
        }
    }

    private fun campoDelContrato(nombre: String) = when (nombre.lowercase()) {
        "email" -> CampoDeAlta.Email
        "fullname" -> CampoDeAlta.NombreCompleto
        "password" -> CampoDeAlta.Contrasena
        "taxid" -> CampoDeAlta.Documento
        "phone" -> CampoDeAlta.Telefono
        "postalcode" -> CampoDeAlta.CodigoPostal
        "bankaccount" -> CampoDeAlta.CuentaBancaria
        else -> null
    }
}
