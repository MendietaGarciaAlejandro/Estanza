package io.github.mendietagarciaalejandro.estanza.ui.alta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaDesplazable
import io.github.mendietagarciaalejandro.estanza.ui.comun.RotuloDeSeccion
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

@OptIn(ExperimentalLayoutApi::class)
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

    Centrado(modifier = modifier, anchoMaximo = 620.dp) {
        ColumnaDesplazable(espaciado = 12.dp) {
            IconButton(onClick = alVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }

            TituloGrande(
                "Darse de alta",
                subtitulo = "El coworking factura cada reserva, por eso se piden los datos fiscales.",
            )

            RotuloDeSeccion("Tus datos")

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

            RotuloDeSeccion("Plan")

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = formulario.plan == Plan.Flex,
                    onClick = { alElegirPlan(Plan.Flex) },
                    label = { Text("Flex") },
                    shape = MaterialTheme.shapes.small,
                )

                FilterChip(
                    selected = formulario.plan == Plan.BonoDia,
                    onClick = { alElegirPlan(Plan.BonoDia) },
                    label = { Text("Bono de dia") },
                    shape = MaterialTheme.shapes.small,
                )
            }

            RotuloDeSeccion("Facturacion")

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

            BotonPrincipal(
                texto = "Crear cuenta",
                onClick = alEnviar,
                trabajando = estado.enviando,
                modifier = Modifier.padding(top = 8.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(onClick = alVolver) { Text("Ya tengo cuenta") }
            }
        }
    }
}
