package io.github.mendietagarciaalejandro.estanza.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.datos.DiaBloqueado
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.datos.aDiaBloqueado
import io.github.mendietagarciaalejandro.estanza.datos.aReserva
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

enum class SeccionDeAdmin(val etiqueta: String) {
    Dias("Dias cerrados"),
    Recursos("Recursos"),
    Reservas("Reservas"),
}

/** El formulario de alta de recurso, tal cual lo va escribiendo el administrador. */
data class FormularioDeRecurso(
    val nombre: String = "",
    val tipo: TipoDeRecurso = TipoDeRecurso.SalaDeReuniones,
    val capacidad: String = "",
) {
    /** Solo evita mandar el formulario a medias; los limites los pone Camar. */
    val sePuedeEnviar: Boolean
        get() = nombre.isNotBlank() && capacidad.toIntOrNull()?.let { it > 0 } == true
}

data class EstadoDeAdmin(
    val seccion: SeccionDeAdmin = SeccionDeAdmin.Dias,
    val cargando: Boolean = true,
    val trabajando: Boolean = false,
    val error: String? = null,
    val aviso: String? = null,

    val dias: List<DiaBloqueado> = emptyList(),
    val fechaABloquear: LocalDate,
    val hoy: LocalDate,
    val motivo: String = "",

    val recursos: List<Recurso> = emptyList(),
    val formulario: FormularioDeRecurso = FormularioDeRecurso(),

    val reservas: List<Reserva> = emptyList(),
    val filtroDeRecurso: String? = null,
) {
    val nombresDeRecurso: Map<String, String> get() = recursos.associate { it.id to it.nombre }

    val sePuedeBloquear: Boolean get() = motivo.isNotBlank() && !trabajando

    /** No tiene sentido cerrar un dia que ya ha pasado. */
    val sePuedeRetroceder: Boolean get() = fechaABloquear > hoy
}

/**
 * La pantalla de administracion.
 *
 * Camar protege estos endpoints con [Authorize(Roles = "Admin")]. Que el boton solo aparezca
 * para los administradores es comodidad: si un socio llegara aqui de alguna forma, el
 * servidor contestaria 403 y se veria el error, que es exactamente lo que tiene que pasar.
 */
class ModeloDeAdmin(
    private val api: ApiDeCamar,
    private val catalogo: CatalogoDeRecursos,
    reloj: Clock,
) : ViewModel() {

    private val diaDeHoy = hoy(reloj)

    private val flujo = MutableStateFlow(
        EstadoDeAdmin(fechaABloquear = diaDeHoy, hoy = diaDeHoy)
    )
    val estado: StateFlow<EstadoDeAdmin> = flujo.asStateFlow()

    private var consultaDeReservas: Job? = null

    init {
        cargar()
    }

    fun verSeccion(seccion: SeccionDeAdmin) {
        flujo.value = flujo.value.copy(seccion = seccion, aviso = null, error = null)

        // Las reservas del coworking entero pueden ser muchas: solo se piden cuando se
        // mira esa pestaña, y no al abrir la pantalla.
        if (seccion == SeccionDeAdmin.Reservas && flujo.value.reservas.isEmpty()) {
            cargarReservas()
        }
    }

    fun cargar() {
        viewModelScope.launch {
            flujo.value = flujo.value.copy(cargando = true, error = null)

            val recursos = catalogo.recursos(refrescar = true)
            val dias = api.diasBloqueados()

            flujo.value = flujo.value.copy(
                cargando = false,
                recursos = (recursos as? Respuesta.Exito)?.valor ?: flujo.value.recursos,
                dias = (dias as? Respuesta.Exito)?.valor
                    ?.map { it.aDiaBloqueado() }
                    ?.sortedBy { it.fecha }
                    ?: flujo.value.dias,
                error = (dias as? Respuesta.Fallo)?.error?.mensaje
                    ?: (recursos as? Respuesta.Fallo)?.error?.mensaje,
            )
        }
    }

    // --- dias cerrados ---

    fun escribirMotivo(texto: String) {
        flujo.value = flujo.value.copy(motivo = texto, aviso = null, error = null)
    }

    fun diaSiguiente() = moverFecha(1)

    fun diaAnterior() {
        if (flujo.value.sePuedeRetroceder) moverFecha(-1)
    }

    private fun moverFecha(dias: Int) {
        val actual = flujo.value

        flujo.value = actual.copy(
            fechaABloquear = LocalDate.fromEpochDays(actual.fechaABloquear.toEpochDays() + dias),
            aviso = null,
            error = null,
        )
    }

    fun bloquearDia() {
        val actual = flujo.value
        if (!actual.sePuedeBloquear) return

        viewModelScope.launch {
            flujo.value = actual.copy(trabajando = true, error = null, aviso = null)

            when (val respuesta = api.bloquearDia(actual.fechaABloquear, actual.motivo.trim())) {
                is Respuesta.Exito -> {
                    val nuevo = respuesta.valor.aDiaBloqueado()

                    flujo.value = flujo.value.copy(
                        trabajando = false,
                        dias = (flujo.value.dias + nuevo).sortedBy { it.fecha },
                        motivo = "",
                        aviso = "Cerrado el ${nuevo.fecha}.",
                    )
                }

                // El caso tipico es el 409 de un dia que ya estaba bloqueado.
                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    trabajando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }

    fun desbloquearDia(dia: DiaBloqueado) {
        if (flujo.value.trabajando) return

        viewModelScope.launch {
            flujo.value = flujo.value.copy(trabajando = true, error = null, aviso = null)

            flujo.value = when (val respuesta = api.desbloquearDia(dia.id)) {
                is Respuesta.Exito -> flujo.value.copy(
                    trabajando = false,
                    dias = flujo.value.dias.filterNot { it.id == dia.id },
                    aviso = "El ${dia.fecha} vuelve a abrir.",
                )

                is Respuesta.Fallo -> flujo.value.copy(
                    trabajando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }

    // --- recursos ---

    fun editarFormulario(cambio: FormularioDeRecurso.() -> FormularioDeRecurso) {
        val actual = flujo.value

        flujo.value = actual.copy(
            formulario = actual.formulario.cambio(),
            aviso = null,
            error = null,
        )
    }

    fun crearRecurso() {
        val actual = flujo.value
        val formulario = actual.formulario
        if (!formulario.sePuedeEnviar || actual.trabajando) return

        // El tipo sale de la lista de creables, asi que el codigo no puede faltar.
        val codigo = formulario.tipo.codigo ?: return
        val capacidad = formulario.capacidad.toIntOrNull() ?: return

        viewModelScope.launch {
            flujo.value = actual.copy(trabajando = true, error = null, aviso = null)

            when (val respuesta = api.crearRecurso(formulario.nombre.trim(), codigo, capacidad)) {
                is Respuesta.Exito -> {
                    // El catalogo que tienen las demas pantallas se ha quedado viejo.
                    catalogo.olvidar()

                    flujo.value = flujo.value.copy(
                        trabajando = false,
                        formulario = FormularioDeRecurso(),
                        aviso = "Dado de alta ${respuesta.valor.nombre}.",
                    )

                    cargar()
                }

                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    trabajando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }

    fun darDeBaja(recurso: Recurso) {
        if (flujo.value.trabajando) return

        viewModelScope.launch {
            flujo.value = flujo.value.copy(trabajando = true, error = null, aviso = null)

            when (val respuesta = api.darDeBajaRecurso(recurso.id)) {
                is Respuesta.Exito -> {
                    catalogo.olvidar()

                    flujo.value = flujo.value.copy(
                        trabajando = false,
                        // Es una baja logica: el recurso conserva su historial, solo deja
                        // de poder reservarse, asi que desaparece de la lista de activos.
                        aviso = "${recurso.nombre} ya no se puede reservar.",
                    )

                    cargar()
                }

                is Respuesta.Fallo -> flujo.value = flujo.value.copy(
                    trabajando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }

    // --- reservas del coworking ---

    fun filtrarReservasPor(idRecurso: String?) {
        val nuevo = if (flujo.value.filtroDeRecurso == idRecurso) null else idRecurso

        flujo.value = flujo.value.copy(filtroDeRecurso = nuevo)
        cargarReservas()
    }

    private fun cargarReservas() {
        // Si se toca un filtro y luego otro, la consulta de en medio ya no interesa. Sin
        // cancelarla, las dos respuestas compiten y gana la que llegue la ultima, que no
        // tiene por que ser la del filtro que esta puesto.
        consultaDeReservas?.cancel()

        consultaDeReservas = viewModelScope.launch {
            flujo.value = flujo.value.copy(cargando = true, error = null)

            flujo.value = when (val respuesta = api.todasLasReservas(flujo.value.filtroDeRecurso)) {
                is Respuesta.Exito -> flujo.value.copy(
                    cargando = false,
                    reservas = respuesta.valor.map { it.aReserva() },
                )

                is Respuesta.Fallo -> flujo.value.copy(
                    cargando = false,
                    error = respuesta.error.mensaje,
                )
            }
        }
    }
}
