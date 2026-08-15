package io.github.mendietagarciaalejandro.estanza.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.DiaBloqueado
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.tema.color
import io.github.mendietagarciaalejandro.estanza.tema.inicial
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoBueno
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaDesplazable
import io.github.mendietagarciaalejandro.estanza.ui.comun.Distintivo
import io.github.mendietagarciaalejandro.estanza.ui.comun.Etiqueta
import io.github.mendietagarciaalejandro.estanza.ui.comun.FilaDeGrupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.Grupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.RotuloDeSeccion
import io.github.mendietagarciaalejandro.estanza.ui.comun.SeparadorDeGrupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

/**
 * Las devoluciones de llamada van juntas en vez de sueltas en la firma: son once, y una
 * pantalla con once parametros de funcion no la lee nadie.
 */
data class AccionesDeAdmin(
    val alEscribirMotivo: (String) -> Unit,
    val alDiaAnterior: () -> Unit,
    val alDiaSiguiente: () -> Unit,
    val alBloquear: () -> Unit,
    val alDesbloquear: (DiaBloqueado) -> Unit,
    val alEscribirNombre: (String) -> Unit,
    val alElegirTipo: (TipoDeRecurso) -> Unit,
    val alEscribirCapacidad: (String) -> Unit,
    val alCrearRecurso: () -> Unit,
    val alDarDeBaja: (Recurso) -> Unit,
    val alFiltrarReservas: (String?) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaDeAdmin(
    estado: EstadoDeAdmin,
    alCambiarSeccion: (SeccionDeAdmin) -> Unit,
    acciones: AccionesDeAdmin,
    modifier: Modifier = Modifier,
) {
    Centrado(modifier = modifier, anchoMaximo = 760.dp) {
        ColumnaDesplazable(espaciado = 12.dp) {
            TituloGrande("Gestion", subtitulo = "Solo para cuentas de administracion.")

            // Un control segmentado y no pestañas: son tres apartados de una misma pantalla,
            // no tres pantallas distintas, y asi no compite con la barra de navegacion.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionDeAdmin.entries.forEach { seccion ->
                    FilterChip(
                        selected = estado.seccion == seccion,
                        onClick = { alCambiarSeccion(seccion) },
                        label = { Text(seccion.etiqueta) },
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }

            // Los avisos van fuera de los apartados para que no dependan de en cual estes
            // cuando termine la peticion.
            if (estado.error != null) AvisoDeError(estado.error)
            if (estado.aviso != null) AvisoBueno(estado.aviso)

            when (estado.seccion) {
                SeccionDeAdmin.Dias -> SeccionDeDias(estado, acciones)
                SeccionDeAdmin.Recursos -> SeccionDeRecursos(estado, acciones)
                SeccionDeAdmin.Reservas -> SeccionDeReservas(estado, acciones)
            }
        }
    }
}

@Composable
private fun SeccionDeDias(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    RotuloDeSeccion("Cerrar un dia")

    Grupo {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = acciones.alDiaAnterior, enabled = estado.sePuedeRetroceder) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
            }

            Text(
                estado.fechaABloquear.comoFechaLarga(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            IconButton(onClick = acciones.alDiaSiguiente) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Dia siguiente")
            }
        }
    }

    CampoDeTexto(
        valor = estado.motivo,
        alCambiar = acciones.alEscribirMotivo,
        etiqueta = "Motivo",
        marcador = "Festivo, obras, mudanza...",
    )

    BotonPrincipal(
        texto = "Cerrar ese dia",
        onClick = acciones.alBloquear,
        habilitado = estado.sePuedeBloquear,
        trabajando = estado.trabajando,
    )

    RotuloDeSeccion("Dias ya cerrados")

    when {
        estado.cargando && estado.dias.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        estado.dias.isEmpty() -> Grupo {
            Text(
                "No hay ningun dia cerrado.\nLos domingos ya cierra por horario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }

        else -> Grupo {
            estado.dias.forEachIndexed { indice, dia ->
                if (indice > 0) SeparadorDeGrupo()

                FilaDeGrupo(
                    titulo = dia.fecha.comoFechaLarga(),
                    subtitulo = dia.motivo,
                    detras = {
                        TextButton(
                            onClick = { acciones.alDesbloquear(dia) },
                            enabled = !estado.trabajando,
                        ) {
                            Text("Reabrir")
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionDeRecursos(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    RotuloDeSeccion("Dar de alta un espacio")

    CampoDeTexto(
        valor = estado.formulario.nombre,
        alCambiar = acciones.alEscribirNombre,
        etiqueta = "Nombre",
        marcador = "Sala Antares",
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Solo los tipos que este cliente sabe traducir a un numero para la API.
        TipoDeRecurso.creables.forEach { tipo ->
            FilterChip(
                selected = estado.formulario.tipo == tipo,
                onClick = { acciones.alElegirTipo(tipo) },
                label = { Text(tipo.etiqueta) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }

    CampoDeTexto(
        valor = estado.formulario.capacidad,
        alCambiar = { texto ->
            // Se filtra al escribir en vez de validar al enviar: asi el teclado numerico de
            // Android tampoco puede colar un signo raro.
            acciones.alEscribirCapacidad(texto.filter { it.isDigit() }.take(3))
        },
        etiqueta = "Capacidad",
        ayuda = "En personas.",
        tipoDeTeclado = KeyboardType.Number,
    )

    BotonPrincipal(
        texto = "Dar de alta",
        onClick = acciones.alCrearRecurso,
        habilitado = estado.formulario.sePuedeEnviar,
        trabajando = estado.trabajando,
    )

    RotuloDeSeccion("Espacios activos")

    Grupo {
        estado.recursos.forEachIndexed { indice, recurso ->
            if (indice > 0) SeparadorDeGrupo(sangria = 68.dp)

            FilaDeGrupo(
                titulo = recurso.nombre,
                subtitulo = "${recurso.tipo.etiqueta} · ${recurso.capacidadEnTexto}",
                delante = { Distintivo(recurso.tipo.inicial, recurso.tipo.color()) },
                detras = {
                    TextButton(
                        onClick = { acciones.alDarDeBaja(recurso) },
                        enabled = !estado.trabajando,
                    ) {
                        Text("Baja", color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionDeReservas(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        estado.recursos.forEach { recurso ->
            FilterChip(
                selected = estado.filtroDeRecurso == recurso.id,
                onClick = { acciones.alFiltrarReservas(recurso.id) },
                label = { Text(recurso.nombre) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }

    when {
        estado.cargando -> Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        estado.reservas.isEmpty() -> Grupo {
            Text(
                "No hay reservas que enseñar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }

        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            estado.reservas.forEach { reserva ->
                TarjetaDeReservaDeAdmin(reserva, estado.nombresDeRecurso[reserva.idRecurso])
            }
        }
    }
}

@Composable
private fun TarjetaDeReservaDeAdmin(reserva: Reserva, nombreDelRecurso: String?) {
    Grupo {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    nombreDelRecurso ?: "Espacio dado de baja",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                Etiqueta(reserva.estado.etiqueta, reserva.estado.color())
            }

            Text(
                "${reserva.fecha.comoFechaLarga()} · ${reserva.comoFranja()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                // Camar no devuelve el nombre del socio, solo su id.
                "socio ${reserva.socioEnCorto} · ${reserva.precio.conMoneda()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
