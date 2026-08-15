package io.github.mendietagarciaalejandro.estanza.ui.conexion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mendietagarciaalejandro.estanza.sesion.Sesion
import io.github.mendietagarciaalejandro.estanza.ui.Centrado
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoBueno
import io.github.mendietagarciaalejandro.estanza.ui.comun.AvisoDeError
import io.github.mendietagarciaalejandro.estanza.ui.comun.BotonPrincipal
import io.github.mendietagarciaalejandro.estanza.ui.comun.CampoDeTexto
import io.github.mendietagarciaalejandro.estanza.ui.comun.ColumnaDesplazable
import io.github.mendietagarciaalejandro.estanza.ui.comun.FilaDeGrupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.Grupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.RotuloDeSeccion
import io.github.mendietagarciaalejandro.estanza.ui.comun.SeparadorDeGrupo
import io.github.mendietagarciaalejandro.estanza.ui.comun.TituloGrande

/**
 * Ajustes: a que servidor apunta la aplicacion y, si hay sesion abierta, quien eres y como
 * salir.
 *
 * Cerrar sesion vive aqui y no en una barra superior porque es una accion que se usa una vez
 * cada mucho: tenerla siempre a un toque de distancia solo servia para pulsarla sin querer.
 */
@Composable
fun PantallaDeAjustes(
    estado: EstadoDeConexion,
    sesion: Sesion?,
    alEscribir: (String) -> Unit,
    alGuardar: () -> Unit,
    alRestablecer: () -> Unit,
    alProbar: () -> Unit,
    alSalir: (() -> Unit)?,
    alVolver: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Centrado(modifier = modifier, anchoMaximo = 640.dp) {
        ColumnaDesplazable(espaciado = 12.dp) {
            if (alVolver != null) {
                IconButton(onClick = alVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }

            TituloGrande("Ajustes", subtitulo = "Ejecutandose en ${estado.nombreDePlataforma}.")

            RotuloDeSeccion("Servidor")

            Grupo {
                FilaDeGrupo(
                    titulo = "En uso ahora",
                    subtitulo = estado.urlGuardada,
                )
            }

            CampoDeTexto(
                valor = estado.borrador,
                alCambiar = alEscribir,
                etiqueta = "Direccion de la API",
                marcador = estado.urlPorDefecto,
                error = estado.error,
                ayuda = if (estado.guardado) "Guardado." else "Por ejemplo: ${estado.urlPorDefecto}",
                tipoDeTeclado = KeyboardType.Uri,
            )

            BotonPrincipal(
                texto = "Guardar",
                onClick = alGuardar,
                habilitado = estado.hayCambios,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = alProbar,
                    // Probar el borrador sin guardar engaña: se comprueba lo que hay
                    // guardado, que es lo que usaran las demas pantallas.
                    enabled = !estado.comprobando && !estado.hayCambios,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Probar conexion")
                }

                if (estado.comprobando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }

                TextButton(
                    onClick = alRestablecer,
                    enabled = estado.urlGuardada != estado.urlPorDefecto,
                ) {
                    Text("La de esta plataforma")
                }
            }

            when (val prueba = estado.prueba) {
                is ResultadoDeLaPrueba.Responde -> AvisoBueno("El servidor responde.")
                is ResultadoDeLaPrueba.NoResponde -> AvisoDeError(prueba.motivo)
                null -> Unit
            }

            if (sesion != null) {
                RotuloDeSeccion("Sesion")

                Grupo {
                    FilaDeGrupo(
                        titulo = "Tu cuenta",
                        subtitulo = if (sesion.esAdministrador) "Administracion" else "Socio",
                    )

                    SeparadorDeGrupo()

                    if (alSalir != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = alSalir)
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                        ) {
                            Text(
                                "Cerrar sesion",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
