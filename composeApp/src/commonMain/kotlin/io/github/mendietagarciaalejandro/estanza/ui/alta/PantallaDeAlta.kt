package io.github.mendietagarciaalejandro.estanza.ui.alta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.datos.CampoDeAlta
import io.github.mendietagarciaalejandro.estanza.red.Plan
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaDeFormulario

@Composable
fun PantallaDeAlta(
    estado: EstadoDeAlta,
    alEditarNombre: (String) -> Unit,
    alEditarEmail: (String) -> Unit,
    alEditarContrasena: (String) -> Unit,
    alElegirPlan: (Plan) -> Unit,
    alEditarDocumento: (String) -> Unit,
    alEditarTelefono: (String) -> Unit,
    alEditarCodigoPostal: (String) -> Unit,
    alEditarCuenta: (String) -> Unit,
    alEnviar: () -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formulario = estado.formulario

    ColumnaDeFormulario(
        titulo = "Darse de alta",
        subtitulo = "El coworking factura cada reserva, por eso se piden los datos fiscales.",
        modifier = modifier,
    ) {
        CampoDeTexto(
            valor = formulario.nombreCompleto,
            alCambiar = alEditarNombre,
            etiqueta = "Nombre y apellidos",
            error = estado.fallos[CampoDeAlta.NombreCompleto],
        )

        CampoDeTexto(
            valor = formulario.email,
            alCambiar = alEditarEmail,
            etiqueta = "Email",
            error = estado.fallos[CampoDeAlta.Email],
            tipoDeTeclado = KeyboardType.Email,
        )

        CampoDeTexto(
            valor = formulario.contrasena,
            alCambiar = alEditarContrasena,
            etiqueta = "Contrasena",
            error = estado.fallos[CampoDeAlta.Contrasena],
            ayuda = "Ocho caracteres como minimo.",
            esContrasena = true,
            tipoDeTeclado = KeyboardType.Password,
        )

        Text("Plan", style = MaterialTheme.typography.labelLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = formulario.plan == Plan.Flex,
                onClick = { alElegirPlan(Plan.Flex) },
                label = { Text("Flex") },
            )

            FilterChip(
                selected = formulario.plan == Plan.BonoDia,
                onClick = { alElegirPlan(Plan.BonoDia) },
                label = { Text("Bono de dia") },
            )
        }

        CampoDeTexto(
            valor = formulario.documento,
            alCambiar = alEditarDocumento,
            etiqueta = "DNI, NIE o CIF",
            error = estado.fallos[CampoDeAlta.Documento],
            ayuda = "Con la letra. Da igual si lo escribes con guiones.",
        )

        CampoDeTexto(
            valor = formulario.telefono,
            alCambiar = alEditarTelefono,
            etiqueta = "Telefono",
            error = estado.fallos[CampoDeAlta.Telefono],
            tipoDeTeclado = KeyboardType.Phone,
        )

        CampoDeTexto(
            valor = formulario.codigoPostal,
            alCambiar = alEditarCodigoPostal,
            etiqueta = "Codigo postal",
            error = estado.fallos[CampoDeAlta.CodigoPostal],
            tipoDeTeclado = KeyboardType.Number,
        )

        CampoDeTexto(
            valor = formulario.cuentaBancaria,
            alCambiar = alEditarCuenta,
            etiqueta = "IBAN (opcional)",
            error = estado.fallos[CampoDeAlta.CuentaBancaria],
            ayuda = "Solo si quieres domiciliar los pagos.",
        )

        if (estado.error != null) AvisoDeError(estado.error)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = alEnviar, enabled = !estado.enviando) {
                Text("Crear cuenta")
            }

            if (estado.enviando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        TextButton(onClick = alVolver) {
            Text("Ya tengo cuenta")
        }
    }
}
