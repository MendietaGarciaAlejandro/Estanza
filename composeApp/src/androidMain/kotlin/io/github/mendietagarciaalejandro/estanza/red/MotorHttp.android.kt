package io.github.mendietagarciaalejandro.estanza.red

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

// OkHttp es el que ya viene con el sistema en la practica y el que mejor se lleva con los
// cambios de red del movil (pasar de wifi a datos, por ejemplo).
actual fun motorHttp(): HttpClientEngine = OkHttp.create {}
