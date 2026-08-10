package io.github.mendietagarciaalejandro.estanza.red

import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor separa la API del cliente del motor que hace las peticiones de verdad, y el motor
 * si depende de la plataforma: en el navegador no hay sockets y hay que pasar por fetch.
 *
 * Se devuelve el motor ya creado y no la factoria para que los tests puedan pasar un
 * MockEngine y probar el cliente con la misma configuracion que usa la aplicacion.
 */
expect fun motorHttp(): HttpClientEngine
