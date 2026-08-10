package io.github.mendietagarciaalejandro.estanza.ui.conexion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Pantalla de ajustes de la conexion. De momento es lo unico que hay: el resto de la
 * aplicacion no sirve de nada si no sabe donde esta la API.
 */
@Composable
fun PantallaDeConexion(
    estado: EstadoDeConexion,
    alEscribir: (String) -> Unit,
    alGuardar: () -> Unit,
    alRestablecer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // En escritorio y en el navegador la ventana puede ser enorme, y un formulario
            // de 1600 px de ancho es ilegible.
            Column(
                modifier = Modifier.widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Estanza", style = MaterialTheme.typography.headlineMedium)

                Text(
                    "Cliente del coworking Camar. Ejecutandose en ${estado.nombreDePlataforma}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Servidor en uso", style = MaterialTheme.typography.labelLarge)
                        Text(estado.urlGuardada, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                OutlinedTextField(
                    value = estado.borrador,
                    onValueChange = alEscribir,
                    label = { Text("Direccion de la API") },
                    placeholder = { Text(estado.urlPorDefecto) },
                    singleLine = true,
                    isError = estado.error != null,
                    supportingText = {
                        val mensaje = estado.error
                            ?: if (estado.guardado) "Guardado." else "Por ejemplo: ${estado.urlPorDefecto}"

                        Text(mensaje)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = alGuardar, enabled = estado.hayCambios) {
                        Text("Guardar")
                    }

                    TextButton(
                        onClick = alRestablecer,
                        enabled = estado.urlGuardada != estado.urlPorDefecto,
                    ) {
                        Text("Usar la de esta plataforma")
                    }
                }
            }
        }
    }
}
