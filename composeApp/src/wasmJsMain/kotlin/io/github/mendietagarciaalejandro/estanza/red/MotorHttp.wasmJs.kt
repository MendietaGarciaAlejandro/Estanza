package io.github.mendietagarciaalejandro.estanza.red

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

// Por debajo es fetch. De ahi que Camar necesite CORS: aqui las reglas las pone el
// navegador y no hay forma de saltarselas desde el codigo.
actual fun motorHttp(): HttpClientEngine = Js.create {}
