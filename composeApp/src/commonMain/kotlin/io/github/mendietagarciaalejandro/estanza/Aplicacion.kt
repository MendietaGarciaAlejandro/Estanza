package io.github.mendietagarciaalejandro.estanza

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.github.mendietagarciaalejandro.estanza.tema.TemaEstanza
import io.github.mendietagarciaalejandro.estanza.ui.Navegacion

/**
 * Raiz comun a las tres versiones. Lo unico que ponen Android, el escritorio y el navegador
 * por su cuenta es la ventana donde meter esto.
 */
@Composable
fun Aplicacion() {
    TemaEstanza(oscuro = isSystemInDarkTheme()) {
        Navegacion()
    }
}
