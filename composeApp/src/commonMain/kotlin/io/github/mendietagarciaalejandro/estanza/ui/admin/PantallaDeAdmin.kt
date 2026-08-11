package io.github.mendietagarciaalejandro.estanza.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.DiaBloqueado
import io.github.mendietagarciaalejandro.estanza.datos.Recurso
import io.github.mendietagarciaalejandro.estanza.datos.Reserva
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso
import io.github.mendietagarciaalejandro.estanza.datos.comoFechaLarga
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDeAdmin(
    estado: EstadoDeAdmin,
    alCambiarSeccion: (SeccionDeAdmin) -> Unit,
    acciones: AccionesDeAdmin,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Administracion") },
                navigationIcon = { TextButton(onClick = alVolver) { Text("Volver") } },
            )
        },
    ) { margenes ->
        Column(
            modifier = Modifier.padding(margenes).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                TabRow(selectedTabIndex = estado.seccion.ordinal) {
                    SeccionDeAdmin.entries.forEach { seccion ->
                        Tab(
                            selected = estado.seccion == seccion,
                            onClick = { alCambiarSeccion(seccion) },
                            text = { Text(seccion.etiqueta) },
                        )
                    }
                }

                // Los avisos van fuera de las pestañas para que no dependan de en cual
                // estes cuando termine la peticion.
                if (estado.error != null) {
                    AvisoDeError(estado.error, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                if (estado.aviso != null) {
                    Text(
                        estado.aviso,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                when (estado.seccion) {
                    SeccionDeAdmin.Dias -> SeccionDeDias(estado, acciones)
                    SeccionDeAdmin.Recursos -> SeccionDeRecursos(estado, acciones)
                    SeccionDeAdmin.Reservas -> SeccionDeReservas(estado, acciones)
                }
            }
        }
    }
}

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

@Composable
private fun SeccionDeDias(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Cerrar un dia", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = acciones.alDiaAnterior, enabled = estado.sePuedeRetroceder) {
                Text("< Anterior")
            }

            Text(
                estado.fechaABloquear.comoFechaLarga(),
                style = MaterialTheme.typography.titleSmall,
            )

            TextButton(onClick = acciones.alDiaSiguiente) { Text("Siguiente >") }
        }

        OutlinedTextField(
            value = estado.motivo,
            onValueChange = acciones.alEscribirMotivo,
            label = { Text("Motivo") },
            placeholder = { Text("Festivo, obras, mudanza...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        BotonConEspera(
            texto = "Cerrar ese dia",
            onClick = acciones.alBloquear,
            habilitado = estado.sePuedeBloquear,
            trabajando = estado.trabajando,
        )

        HorizontalDivider()

        Text("Dias ya cerrados", style = MaterialTheme.typography.titleMedium)

        when {
            estado.cargando && estado.dias.isEmpty() -> CircularProgressIndicator()

            estado.dias.isEmpty() -> Text(
                "No hay ningun dia cerrado. Los domingos ya cierra por horario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(estado.dias, key = { it.id }) { dia ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    dia.fecha.comoFechaLarga(),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    dia.motivo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            TextButton(
                                onClick = { acciones.alDesbloquear(dia) },
                                enabled = !estado.trabajando,
                            ) {
                                Text("Reabrir")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionDeRecursos(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Dar de alta un recurso", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = estado.formulario.nombre,
            onValueChange = acciones.alEscribirNombre,
            label = { Text("Nombre") },
            placeholder = { Text("Sala Antares") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Solo los tipos que este cliente sabe traducir a un numero para la API.
            TipoDeRecurso.creables.forEach { tipo ->
                FilterChip(
                    selected = estado.formulario.tipo == tipo,
                    onClick = { acciones.alElegirTipo(tipo) },
                    label = { Text(tipo.etiqueta) },
                )
            }
        }

        OutlinedTextField(
            value = estado.formulario.capacidad,
            onValueChange = { texto ->
                // Se filtra al escribir en vez de validar al enviar: asi el teclado
                // numerico de Android tampoco puede colar un signo raro.
                acciones.alEscribirCapacidad(texto.filter { it.isDigit() }.take(3))
            },
            label = { Text("Capacidad") },
            suffix = { Text("personas") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        BotonConEspera(
            texto = "Dar de alta",
            onClick = acciones.alCrearRecurso,
            habilitado = estado.formulario.sePuedeEnviar && !estado.trabajando,
            trabajando = estado.trabajando,
        )

        HorizontalDivider()

        Text("Recursos activos", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(estado.recursos, key = { it.id }) { recurso ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(recurso.nombre, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${recurso.tipo.etiqueta} - ${recurso.capacidadEnTexto}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        TextButton(
                            onClick = { acciones.alDarDeBaja(recurso) },
                            enabled = !estado.trabajando,
                        ) {
                            Text("Dar de baja")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionDeReservas(estado: EstadoDeAdmin, acciones: AccionesDeAdmin) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            estado.recursos.forEach { recurso ->
                FilterChip(
                    selected = estado.filtroDeRecurso == recurso.id,
                    onClick = { acciones.alFiltrarReservas(recurso.id) },
                    label = { Text(recurso.nombre) },
                )
            }
        }

        when {
            estado.cargando -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            estado.reservas.isEmpty() -> Text(
                "No hay reservas que enseñar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(estado.reservas, key = { it.id }) { reserva ->
                    TarjetaDeReservaDeAdmin(reserva, estado.nombresDeRecurso[reserva.idRecurso])
                }
            }
        }
    }
}

@Composable
private fun TarjetaDeReservaDeAdmin(reserva: Reserva, nombreDelRecurso: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    nombreDelRecurso ?: "Recurso dado de baja",
                    style = MaterialTheme.typography.titleSmall,
                )

                Text(
                    reserva.estado.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                "${reserva.fecha.comoFechaLarga()}, ${reserva.comoFranja()}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                // Camar no devuelve el nombre del socio, solo su id.
                "socio ${reserva.socioEnCorto} - ${reserva.precio.conMoneda()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BotonConEspera(
    texto: String,
    onClick: () -> Unit,
    habilitado: Boolean,
    trabajando: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onClick, enabled = habilitado) { Text(texto) }

        if (trabajando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
