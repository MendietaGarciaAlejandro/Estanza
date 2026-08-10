package io.github.mendietagarciaalejandro.estanza.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaDeFormulario

@Composable
fun PantallaDeAcceso(
    estado: EstadoDeAcceso,
    alEscribirEmail: (String) -> Unit,
    alEscribirContrasena: (String) -> Unit,
    alEntrar: () -> Unit,
    alIrAlAlta: () -> Unit,
    alIrAAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ColumnaDeFormulario(
        titulo = "Estanza",
        subtitulo = "Entra con tu cuenta del coworking.",
        modifier = modifier,
    ) {
        CampoDeTexto(
            valor = estado.email,
            alCambiar = alEscribirEmail,
            etiqueta = "Email",
            tipoDeTeclado = KeyboardType.Email,
        )

        CampoDeTexto(
            valor = estado.contrasena,
            alCambiar = alEscribirContrasena,
            etiqueta = "Contrasena",
            esContrasena = true,
            tipoDeTeclado = KeyboardType.Password,
        )

        if (estado.error != null) AvisoDeError(estado.error)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = alEntrar, enabled = estado.sePuedeEnviar) {
                Text("Entrar")
            }

            if (estado.entrando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        TextButton(onClick = alIrAlAlta) {
            Text("No tengo cuenta, quiero darme de alta")
        }

        // El acceso a los ajustes tiene que estar aqui: si la direccion del servidor esta
        // mal, esta es la unica pantalla que se ve y desde ella hay que poder arreglarlo.
        TextButton(onClick = alIrAAjustes) {
            Text("Ajustes de conexion")
        }
    }
}
