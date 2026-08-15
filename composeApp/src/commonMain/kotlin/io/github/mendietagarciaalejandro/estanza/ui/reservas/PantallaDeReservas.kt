package io.github.mendietagarciaalejandro.estanza.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.EstadoDeReserva
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.tema.color
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoBueno
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.Cargando
import io.github.mendietagarciaalejandro.estanza.ui.comun.margenesSeguros
import io.github.mendietagarciaalejandro.estanza.ui.comun.EstadoVacio
import io.github.mendietagarciaalejandro.estanza.ui.comun.Etiqueta
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

@Composable
fun PantallaDeReservas(
    estado: EstadoDeReservas,
    alPedirCancelar: (Reserva) -> Unit,
    alConfirmarCancelacion: () -> Unit,
    alDejarloEstar: () -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            estado.cargando && estado.reservas.isEmpty() -> Cargando()

            estado.error != null && estado.reservas.isEmpty() -> Centrado {
                EstadoVacio(
                    titulo = "No se han podido traer tus reservas",
                    detalle = estado.error,
                    accion = {
                        BotonPrincipal("Reintentar", alReintentar, modifier = Modifier.padding(horizontal = 32.dp))
                    },
                )
            }

            estado.reservas.isEmpty() -> Centrado {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .margenesSeguros()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        TituloGrande("Reservas")
                    }

                    EstadoVacio(
                        titulo = "Todavia no has reservado nada",
                        detalle = "Cuando reserves un espacio, aparecera aqui.",
                    )
                }
            }

            else -> Centrado {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .margenesSeguros(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { TituloGrande("Reservas") }

                    if (estado.aviso != null) item { AvisoBueno(estado.aviso) }
                    if (estado.error != null) item { AvisoDeError(estado.error) }

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

    val porCancelar = estado.porCancelar
    if (porCancelar != null) {
        AlertDialog(
            onDismissRequest = alDejarloEstar,
            shape = MaterialTheme.shapes.extraLarge,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
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
                    // Si el catalogo no llego, el id no le dice nada a nadie.
                    nombreDelRecurso ?: "Espacio del coworking",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )

                Etiqueta(reserva.estado.etiqueta, reserva.estado.color())
            }

            // La hora es lo que se busca de un vistazo, asi que va en grande.
            Text(reserva.comoFranja(), style = MaterialTheme.typography.headlineSmall)

            Text(
                reserva.fecha.comoFechaLarga(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                if (reserva.estado == EstadoDeReserva.Cancelada && reserva.reembolso != null) {
                    "${reserva.precio.conMoneda()} · devueltos ${reserva.reembolso.conMoneda()}"
                } else {
                    reserva.precio.conMoneda()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (reserva.sePuedeCancelar) {
                TextButton(
                    onClick = alPedirCancelar,
                    enabled = !cancelando,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
