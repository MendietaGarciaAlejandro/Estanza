package io.github.mendietagarciaalejandro.estanza.red

import kotlinx.serialization.Serializable

/**
 * El cuerpo que devuelve Camar cuando algo va mal, tal cual lo define la RFC 7807.
 *
 * Todos los campos son opcionales porque ASP.NET no siempre los rellena: los errores de
 * validacion traen [errors] y no [detail], y las excepciones de dominio al reves.
 */
@Serializable
data class DetallesDelProblema(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val errors: Map<String, List<String>>? = null,
)
