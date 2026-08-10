package io.github.mendietagarciaalejandro.estanza.red

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

// CIO es el motor propio de Ktor, escrito en Kotlin y sin dependencias de Android: en
// escritorio no hace falta arrastrar OkHttp entero.
actual fun motorHttp(): HttpClientEngine = CIO.create {}
