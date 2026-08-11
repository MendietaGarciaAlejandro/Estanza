package io.github.mendietagarciaalejandro.estanza.ui.recurso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(estado.recurso?.nombre ?: "Recurso") },
                navigationIcon = {
                    TextButton(onClick = alVolver) { Text("Volver") }
                },
            )
        },
    ) { margenes ->
        Column(
            modifier = Modifier.padding(margenes).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val recurso = estado.recurso
                if (recurso != null) {
                    Text(
                        "${recurso.tipo.etiqueta} - ${recurso.capacidadEnTexto}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = alDiaAnterior, enabled = estado.sePuedeRetroceder) {
                        Text("< Anterior")
                    }

                    Text(
                        if (estado.esHoy) "hoy, ${estado.fecha.comoFechaLarga()}"
                        else estado.fecha.comoFechaLarga(),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    TextButton(onClick = alDiaSiguiente) {
                        Text("Siguiente >")
                    }
                }

                when {
                    estado.cargando -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }

                    estado.error != null -> Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AvisoDeError(estado.error)
                        Button(onClick = alReintentar) { Text("Reintentar") }
                    }

                    // Camar devuelve la lista vacia tanto si el coworking cierra ese dia
                    // como si no queda un hueco libre, y desde aqui no se distingue. Se
                    // dice lo que se sabe en vez de repetir el horario de apertura, que es
                    // del servidor y podria cambiar sin que nos enterasemos.
                    estado.disponibilidad?.huecos.isNullOrEmpty() -> Text(
                        "Ese dia no queda nada libre. Puede que el coworking este cerrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> {
                        val huecos = estado.huecos

                        Text(
                            if (huecos.size == 1) "Queda 1 hueco de media hora"
                            else "Quedan ${huecos.size} huecos de media hora",
                            style = MaterialTheme.typography.labelLarge,
                        )

                        Text(
                            "Toca un hueco y luego otro mas tarde para reservar un rato seguido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            huecos.forEachIndexed { indice, hueco ->
                                FilterChip(
                                    selected = estado.seleccion?.contains(indice) == true,
                                    onClick = { alPulsarHueco(indice) },
                                    enabled = !estado.reservando,
                                    label = { Text(hueco.comoFranja()) },
                                )
                            }
                        }
                    }
                }

                val resumen = estado.resumenDeLaSeleccion
                if (resumen != null) {
                    HorizontalDivider()

                    Text("Vas a reservar $resumen.", style = MaterialTheme.typography.bodyMedium)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = alReservar, enabled = !estado.reservando) {
                            Text("Reservar")
                        }

                        if (estado.reservando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }

                // El texto sale de Camar sin tocarlo: "Ese hueco ya esta reservado.", "Una
                // reserva de HotDesk dura entre 240 y 780 minutos.". Reescribirlo aqui
                // seria copiarse sus reglas y arriesgarse a que dejen de coincidir.
                if (estado.errorDeReserva != null) AvisoDeError(estado.errorDeReserva)

                if (estado.reservaHecha != null) {
                    Text(
                        estado.reservaHecha,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
