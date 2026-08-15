package io.github.mendietagarciaalejandro.estanza

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mendietagarciaalejandro.estanza.tema.TemaEstanza
import io.github.mendietagarciaalejandro.estanza.ui.Navegacion

/**
 * Raiz comun a las tres versiones. Lo unico que ponen Android, el escritorio y el navegador
 * por su cuenta es la ventana donde meter esto.
 */
@Composable
fun Aplicacion() {
    TemaEstanza(oscuro = isSystemInDarkTheme()) {
        // Este Surface es el que pinta el fondo de toda la aplicacion. Sin el, lo que se ve
        // por debajo es el fondo de la ventana o el del HTML, que no sabe nada del tema: en
        // el navegador con el sistema en oscuro salian cajas negras sobre un fondo blanco.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Navegacion()
        }
    }
}
