package io.github.mendietagarciaalejandro.estanza.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.EstadoDeReserva
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDeReservas(
    estado: EstadoDeReservas,
    alPedirCancelar: (Reserva) -> Unit,
    alConfirmarCancelacion: () -> Unit,
    alDejarloEstar: () -> Unit,
    alReintentar: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Mis reservas") },
                navigationIcon = { TextButton(onClick = alVolver) { Text("Volver") } },
            )
        },
    ) { margenes ->
        Column(
            modifier = Modifier.padding(margenes).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                if (estado.aviso != null) {
                    Text(
                        estado.aviso,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                when {
                    estado.cargando && estado.reservas.isEmpty() -> Centrado {
                        CircularProgressIndicator()
                    }

                    estado.error != null -> Centrado {
                        AvisoDeError(estado.error)
                        Button(onClick = alReintentar) { Text("Reintentar") }
                    }

                    estado.reservas.isEmpty() -> Centrado {
                        Text(
                            "Todavia no has reservado nada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(estado.reservas, key = { it.id }) { reserva ->
                            TarjetaDeReserva(
                                reserva = reserva,
                                nombreDelRecurso = estado.nombres[reserva.idRecurso],
                                cancelando = estado.cancelando,
                                alPedirCancelar = { alPedirCancelar(reserva) },
                            )
                        }
                    }
                }
            }
        }
    }

    val porCancelar = estado.porCancelar
    if (porCancelar != null) {
        AlertDialog(
            onDismissRequest = alDejarloEstar,
            title = { Text("Cancelar la reserva") },
            text = {
                // No se adelanta cuanto se devuelve: eso lo decide la politica de
                // cancelacion de Camar segun la antelacion, y calcularlo aqui seria
                // copiarla y arriesgarse a decir una cifra que luego no cuadre.
                Text(
                    "Vas a cancelar ${porCancelar.comoFranja()} del " +
                        "${porCancelar.fecha.comoFechaLarga()}. Lo que se devuelve depende " +
                        "de la antelacion con la que avises."
                )
            },
            confirmButton = { TextButton(onClick = alConfirmarCancelacion) { Text("Cancelar la reserva") } },
            dismissButton = { TextButton(onClick = alDejarloEstar) { Text("Dejarlo estar") } },
        )
    }
}

@Composable
private fun TarjetaDeReserva(
    reserva: Reserva,
    nombreDelRecurso: String?,
    cancelando: Boolean,
    alPedirCancelar: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // Si el catalogo no llego, el id no le dice nada a nadie.
                    nombreDelRecurso ?: "Recurso del coworking",
                    style = MaterialTheme.typography.titleMedium,
                )

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(reserva.estado.etiqueta) },
                )
            }

            Text(
                "${reserva.fecha.comoFechaLarga()}, ${reserva.comoFranja()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                if (reserva.estado == EstadoDeReserva.Cancelada && reserva.reembolso != null) {
                    "${reserva.precio.conMoneda()} - devueltos ${reserva.reembolso.conMoneda()}"
                } else {
                    reserva.precio.conMoneda()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (reserva.sePuedeCancelar) {
                TextButton(onClick = alPedirCancelar, enabled = !cancelando) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        contenido()
    }
}
