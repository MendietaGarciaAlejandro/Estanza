package io.github.mendietagarciaalejandro.estanza

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mendietagarciaalejandro.estanza.tema.TemaEstanza
import io.github.mendietagarciaalejandro.estanza.ui.conexion.ModeloDeConexion
import io.github.mendietagarciaalejandro.estanza.ui.conexion.PantallaDeConexion
import org.koin.compose.viewmodel.koinViewModel

/**
 * Raiz comun a las tres versiones. Lo unico que ponen Android, el escritorio y el navegador
 * por su cuenta es la ventana donde meter esto.
 */
@Composable
fun Aplicacion() {
    TemaEstanza(oscuro = isSystemInDarkTheme()) {
        val modelo = koinViewModel<ModeloDeConexion>()
        val estado by modelo.estado.collectAsStateWithLifecycle()

        PantallaDeConexion(
            estado = estado,
            alEscribir = modelo::escribir,
            alGuardar = modelo::guardar,
            alRestablecer = modelo::restablecer,
        )
    }
}
