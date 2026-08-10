package io.github.mendietagarciaalejandro.estanza.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.sesion.Sesion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDeInicio(
    sesion: Sesion,
    alIrAAjustes: () -> Unit,
    alSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Estanza") },
                actions = {
                    // Botones de texto y no iconos: los iconos de Material no vienen en el
                    // artefacto multiplataforma y no compensa arrastrarlos por dos botones.
                    TextButton(onClick = alIrAAjustes) { Text("Conexion") }
                    TextButton(onClick = alSalir) { Text("Salir") }
                },
            )
        },
    ) { margenes ->
        Column(
            modifier = Modifier.padding(margenes).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Has entrado.", style = MaterialTheme.typography.headlineSmall)

            Text(
                if (sesion.esAdministrador) "Tu cuenta es de administracion."
                else "Tu cuenta es de socio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "El catalogo de salas y las reservas todavia no estan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
