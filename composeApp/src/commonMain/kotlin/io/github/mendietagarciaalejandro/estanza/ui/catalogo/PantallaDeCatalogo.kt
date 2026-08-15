package io.github.mendietagarciaalejandro.estanza.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.tema.color
import io.github.mendietagarciaalejandro.estanza.tema.inicial
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.Cargando
import io.github.mendietagarciaalejandro.estanza.ui.comun.margenesSeguros
import io.github.mendietagarciaalejandro.estanza.ui.comun.Distintivo
import io.github.mendietagarciaalejandro.estanza.ui.comun.EstadoVacio
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaDeCatalogo(
    estado: EstadoDelCatalogo,
    alFiltrar: (TipoDeRecurso?) -> Unit,
    alAbrirRecurso: (Recurso) -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            estado.cargando && estado.recursos.isEmpty() -> Cargando()

            estado.error != null && estado.recursos.isEmpty() -> Centrado {
                EstadoVacio(
                    titulo = "No se ve el coworking",
                    detalle = estado.error,
                    accion = {
                        BotonPrincipal("Reintentar", alReintentar, modifier = Modifier.padding(horizontal = 32.dp))
                    },
                )
            }

            else -> Centrado(anchoMaximo = 1120.dp) {
                // Una rejilla que se adapta sola: en un movil sale una columna, en una
                // ventana ancha salen tres o cuatro. No hace falta saber en que plataforma
                // estamos, solo cuanto sitio hay.
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .margenesSeguros(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TituloGrande("Espacios", subtitulo = "Camar Coworking")
                    }

                    if (estado.tiposDisponibles.size > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FlowRow(
                                modifier = Modifier.padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                estado.tiposDisponibles.forEach { tipo ->
                                    FilterChip(
                                        selected = estado.filtro == tipo,
                                        onClick = { alFiltrar(tipo) },
                                        label = { Text(tipo.etiqueta) },
                                        shape = MaterialTheme.shapes.small,
                                    )
                                }
                            }
                        }
                    }

                    if (estado.visibles.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                if (estado.filtro == null) "El coworking no tiene espacios dados de alta."
                                else "No hay ninguno de ese tipo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }

                    items(estado.visibles, key = { it.id }) { recurso ->
                        TarjetaDeRecurso(recurso, onClick = { alAbrirRecurso(recurso) })
                    }

                    // Si falla un refresco pero ya habia lista, el aviso va debajo en vez
                    // de tapar lo que el usuario estaba mirando.
                    if (estado.error != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            AvisoDeError(estado.error, Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDeRecurso(recurso: Recurso, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Distintivo(recurso.tipo.inicial, recurso.tipo.color())

            Column(modifier = Modifier.weight(1f)) {
                Text(recurso.nombre, style = MaterialTheme.typography.titleMedium)

                Text(
                    "${recurso.tipo.etiqueta} · ${recurso.capacidadEnTexto}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
