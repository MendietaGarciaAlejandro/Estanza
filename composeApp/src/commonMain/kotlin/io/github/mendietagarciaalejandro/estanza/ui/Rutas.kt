package io.github.mendietagarciaalejandro.estanza.ui

import kotlinx.serialization.Serializable

/**
 * Los destinos de la aplicacion.
 *
 * Son objetos serializables y no cadenas: asi el compilador comprueba que existe el destino
 * al que se navega, y cuando alguno lleve argumentos (el id de un recurso, por ejemplo) no
 * habra que pegarlos a mano en una ruta ni parsearlos al llegar.
 */
object Rutas {
    @Serializable
    data object Acceso

    @Serializable
    data object Alta

    @Serializable
    data object Catalogo

    /** La ficha de un recurso lleva su id; el resto se saca del catalogo ya cargado. */
    @Serializable
    data class Recurso(val id: String)

    @Serializable
    data object Ajustes
}
