package io.github.mendietagarciaalejandro.estanza.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDeCatalogo(
    estado: EstadoDelCatalogo,
    alFiltrar: (TipoDeRecurso?) -> Unit,
    alAbrirRecurso: (Recurso) -> Unit,
    alReintentar: () -> Unit,
    alIrAMisReservas: () -> Unit,
    /** null cuando la cuenta no es de administracion, y entonces el boton no se pinta. */
    alIrAAdministracion: (() -> Unit)?,
    alIrAAjustes: () -> Unit,
    alSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Camar Coworking") },
                actions = {
                    TextButton(onClick = alIrAMisReservas) { Text("Mis reservas") }

                    if (alIrAAdministracion != null) {
                        TextButton(onClick = alIrAAdministracion) { Text("Admin") }
                    }

                    TextButton(onClick = alIrAAjustes) { Text("Conexion") }
                    TextButton(onClick = alSalir) { Text("Salir") }
                },
            )
        },
    ) { margenes ->
        Column(
            modifier = Modifier.padding(margenes).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                if (estado.tiposDisponibles.size > 1) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        estado.tiposDisponibles.forEach { tipo ->
                            FilterChip(
                                selected = estado.filtro == tipo,
                                onClick = { alFiltrar(tipo) },
                                label = { Text(tipo.etiqueta) },
                            )
                        }
                    }
                }

                when {
                    estado.cargando && estado.recursos.isEmpty() -> Centrado {
                        CircularProgressIndicator()
                    }

                    estado.error != null -> Centrado {
                        AvisoDeError(estado.error)
                        Button(onClick = alReintentar) { Text("Reintentar") }
                    }

                    estado.visibles.isEmpty() -> Centrado {
                        Text(
                            if (estado.filtro == null) "El coworking no tiene recursos dados de alta."
                            else "No hay ninguno de ese tipo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(estado.visibles, key = { it.id }) { recurso ->
                            TarjetaDeRecurso(recurso, onClick = { alAbrirRecurso(recurso) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDeRecurso(recurso: Recurso, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(recurso.nombre, style = MaterialTheme.typography.titleMedium)

            Text(
                "${recurso.tipo.etiqueta} - ${recurso.capacidadEnTexto}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
