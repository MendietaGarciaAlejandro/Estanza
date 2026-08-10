package io.github.mendietagarciaalejandro.estanza

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.mendietagarciaalejandro.estanza.di.iniciarKoin
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    iniciarKoin()

    ComposeViewport(document.body!!) {
        Aplicacion()
    }
}
