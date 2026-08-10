package io.github.mendietagarciaalejandro.estanza.red

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * El JSON que se habla con Camar.
 *
 * ignoreUnknownKeys porque los ProblemDetails de ASP.NET traen un traceId que aqui no se
 * usa, y porque asi añadir un campo en el servidor no rompe a los clientes viejos.
 */
private val formatoJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun crearClienteHttp(
    motor: HttpClientEngine = motorHttp(),
): HttpClient = HttpClient(motor) {
    // Sin esto Ktor lanza una excepcion en cuanto el estado no es 2xx, y aqui los 4xx no
    // son fallos inesperados: son la forma que tiene Camar de contestar que no.
    expectSuccess = false

    install(ContentNegotiation) {
        json(formatoJson)

        // Los errores no vienen como application/json sino como application/problem+json,
        // que es lo que manda la RFC 7807. Si no se registra aparte, ContentNegotiation no
        // los reconoce y el cuerpo del error se pierde.
        json(formatoJson, ContentType("application", "problem+json"))
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        // Corto a proposito: el caso normal de fallo es apuntar a una IP que no existe, y
        // ahi lo que se quiere es que el usuario se entere rapido para corregir la URL.
        connectTimeoutMillis = 5_000
    }
}
