package io.github.mendietagarciaalejandro.estanza.ui.acceso

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaCentrada

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
    Centrado(modifier = modifier, anchoMaximo = 560.dp) {
        ColumnaCentrada(espaciado = 12.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Estanza",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    "Reservas de Camar Coworking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Un respiro entre la marca y el formulario, que si no parecen lo mismo.
            Spacer(Modifier.height(16.dp))

            CampoDeTexto(
                valor = estado.email,
                alCambiar = alEscribirEmail,
                etiqueta = "Email",
                reservaAyuda = false,
                tipoDeTeclado = KeyboardType.Email,
            )

            CampoDeTexto(
                valor = estado.contrasena,
                alCambiar = alEscribirContrasena,
                etiqueta = "Contrasena",
                reservaAyuda = false,
                esContrasena = true,
                tipoDeTeclado = KeyboardType.Password,
            )

            if (estado.error != null) AvisoDeError(estado.error)

            BotonPrincipal(
                texto = "Entrar",
                onClick = alEntrar,
                habilitado = estado.sePuedeEnviar,
                trabajando = estado.entrando,
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TextButton(onClick = alIrAlAlta) {
                    // Sin la negrita del estilo de boton: es la accion secundaria, no tiene
                    // que competir con "Entrar".
                    Text(
                        "No tengo cuenta, quiero darme de alta",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // El acceso a los ajustes tiene que estar aqui: si la direccion del servidor
                // esta mal, esta es la unica pantalla que se ve y desde ella hay que poder
                // arreglarlo.
                TextButton(onClick = alIrAAjustes) {
                    Text(
                        "Ajustes de conexion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
