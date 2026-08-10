package io.github.mendietagarciaalejandro.estanza.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * El marco que comparten todas las pantallas de formulario.
 *
 * Lo del ancho maximo no es un capricho: la misma pantalla se ve en un movil de 360 px y en
 * una ventana de escritorio de 1600, y un formulario estirado de lado a lado es ilegible.
 */
@Composable
fun ColumnaDeFormulario(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
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
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(titulo, style = MaterialTheme.typography.headlineMedium)

                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                contenido()
            }
        }
    }
}

/**
 * Campo de texto con hueco fijo para el mensaje de error.
 *
 * El texto de apoyo se pinta siempre, aunque este vacio, para que el formulario no pegue un
 * salto cada vez que aparece o desaparece un error.
 */
@Composable
fun CampoDeTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = { Text(error ?: ayuda.orEmpty()) },
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipoDeTeclado),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Mensaje de error de los que no cuelgan de ningun campo. */
@Composable
fun AvisoDeError(mensaje: String, modifier: Modifier = Modifier) {
    Text(
        mensaje,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}
