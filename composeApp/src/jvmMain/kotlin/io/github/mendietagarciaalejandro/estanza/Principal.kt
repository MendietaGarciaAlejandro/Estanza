package io.github.mendietagarciaalejandro.estanza

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.mendietagarciaalejandro.estanza.di.iniciarKoin

fun main() {
    iniciarKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Estanza",
            state = rememberWindowState(size = DpSize(900.dp, 700.dp)),
        ) {
            Aplicacion()
        }
    }
}
