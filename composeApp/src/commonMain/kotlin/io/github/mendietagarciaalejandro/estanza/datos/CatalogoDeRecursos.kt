package io.github.mendietagarciaalejandro.estanza.datos

import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.Respuesta
import io.github.mendietagarciaalejandro.estanza.red.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Los recursos del coworking, con lo ultimo que se trajo guardado.
 *
 * Camar no tiene un GET /api/resources/{id}: solo devuelve la lista entera. Sin guardarla,
 * abrir la ficha de una sala obligaria a pedir los cinco recursos otra vez solo para saber
 * como se llama el que estas mirando. Son cinco filas que no cambian casi nunca, asi que
 * se quedan en memoria y la pantalla de detalle las tiene ya.
 *
 * No se guarda en disco a proposito: al cerrar la aplicacion se olvida. Guardar el catalogo
 * entre arranques ya seria trabajo de una base de datos local, y aqui no hace falta.
 */
class CatalogoDeRecursos(private val api: ApiDeCamar) {

    private val cerrojo = Mutex()
    private var guardados: List<Recurso>? = null

    /**
     * Si dos pantallas piden la lista a la vez, el cerrojo hace que solo salga una peticion
     * y la segunda se encuentre el resultado ya puesto.
     */
    suspend fun recursos(refrescar: Boolean = false): Respuesta<List<Recurso>> = cerrojo.withLock {
        val enMemoria = guardados

        if (!refrescar && enMemoria != null) return@withLock Respuesta.Exito(enMemoria)

        api.recursos()
            .map { lista -> lista.map { it.aRecurso() } }
            .also { if (it is Respuesta.Exito) guardados = it.valor }
    }

    suspend fun recurso(id: String): Respuesta<Recurso?> = recursos().map { lista ->
        lista.firstOrNull { it.id == id }
    }

    /** Al cerrar sesion no tiene sentido enseñarle el catalogo al siguiente que entre. */
    suspend fun olvidar() = cerrojo.withLock { guardados = null }
}
