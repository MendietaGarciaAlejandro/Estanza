package io.github.mendietagarciaalejandro.estanza.ui.recurso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.Hueco
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.datos.comoHora
import io.github.mendietagarciaalejandro.estanza.datos.horaDeCamar
import io.github.mendietagarciaalejandro.estanza.tema.color
import io.github.mendietagarciaalejandro.estanza.tema.inicial
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoBueno
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.Distintivo
import io.github.mendietagarciaalejandro.estanza.ui.comun.Grupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.margenesSeguros
import io.github.mendietagarciaalejandro.estanza.ui.comun.RotuloDeSeccion
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaDeRecurso(
    estado: EstadoDelRecurso,
    alDiaAnterior: () -> Unit,
    alDiaSiguiente: () -> Unit,
    alPulsarHueco: (Int) -> Unit,
    alReservar: () -> Unit,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Centrado(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .margenesSeguros()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(onClick = alVolver, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }

            val recurso = estado.recurso
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (recurso != null) {
                    Distintivo(recurso.tipo.inicial, recurso.tipo.color())
                }

                TituloGrande(
                    texto = recurso?.nombre ?: "Espacio",
                    subtitulo = recurso?.let { "${it.tipo.etiqueta} · ${it.capacidadEnTexto}" },
                )
            }

            SelectorDeDia(estado, alDiaAnterior, alDiaSiguiente)

            when {
                estado.cargando -> Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }

                estado.error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvisoDeError(estado.error)
                    BotonPrincipal("Reintentar", alReintentar)
                }

                // Camar devuelve la lista vacia tanto si el coworking cierra ese dia como si
                // no queda un hueco libre, y desde aqui no se distingue. Se dice lo que se
                // sabe en vez de repetir el horario de apertura, que es del servidor.
                estado.huecos.isEmpty() -> Grupo {
                    Text(
                        "Ese dia no queda nada libre.\nPuede que el coworking este cerrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }

                else -> Huecos(estado, alPulsarHueco)
            }

            val resumen = estado.resumenDeLaSeleccion
            if (resumen != null) {
                Grupo {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Vas a reservar", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(resumen, style = MaterialTheme.typography.headlineSmall)

                        BotonPrincipal(
                            texto = "Reservar",
                            onClick = alReservar,
                            trabajando = estado.reservando,
                        )
                    }
                }
            }

            // El texto sale de Camar sin tocarlo: "Ese hueco ya esta reservado.", "Una
            // reserva de mesa flexible dura entre 4 y 13 horas.". Reescribirlo aqui seria
            // copiarse sus reglas y arriesgarse a que dejen de coincidir.
            if (estado.errorDeReserva != null) AvisoDeError(estado.errorDeReserva)

            if (estado.reservaHecha != null) AvisoBueno(estado.reservaHecha)
        }
    }
}

@Composable
private fun SelectorDeDia(
    estado: EstadoDelRecurso,
    alDiaAnterior: () -> Unit,
    alDiaSiguiente: () -> Unit,
) {
    Grupo {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = alDiaAnterior, enabled = estado.sePuedeRetroceder) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (estado.esHoy) {
                    Text("HOY", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }

                Text(
                    estado.fecha.comoFechaLarga(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            IconButton(onClick = alDiaSiguiente) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Dia siguiente")
            }
        }
    }
}

/**
 * Los huecos, separados en mañana y tarde y con la hora de empiece nada mas.
 *
 * Antes cada ficha ponia "08:00 - 08:30" y una jornada entera eran veintiseis fichas con el
 * mismo aspecto: imposible de recorrer con la vista. Con solo la hora de empiece se leen
 * como un horario, que es lo que son, y el rango completo ya se dice abajo al elegir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Huecos(estado: EstadoDelRecurso, alPulsarHueco: (Int) -> Unit) {
    val (manana, tarde) = estado.huecos
        .withIndex()
        .partition { it.value.inicio.horaDeCamar() < 14 }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (estado.huecos.size == 1) "Queda 1 hueco de media hora"
            else "Quedan ${estado.huecos.size} huecos de media hora",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            "Toca una hora y luego otra mas tarde para reservar un rato seguido.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        listOf("Mañana" to manana, "Tarde" to tarde).forEach { (rotulo, tramo) ->
            if (tramo.isNotEmpty()) {
                RotuloDeSeccion(rotulo)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tramo.forEach { (indice, hueco) ->
                        FichaDeHueco(
                            hueco = hueco,
                            elegida = estado.seleccion?.contains(indice) == true,
                            habilitada = !estado.reservando,
                            onClick = { alPulsarHueco(indice) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FichaDeHueco(
    hueco: Hueco,
    elegida: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = elegida,
        onClick = onClick,
        enabled = habilitada,
        label = {
            Text(
                hueco.inicio.comoHora(),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
