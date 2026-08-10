package io.github.mendietagarciaalejandro.estanza.datos

import com.russhwolf.settings.Settings
import io.github.mendietagarciaalejandro.estanza.plataforma.Plataforma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda a que servidor apunta la aplicacion.
 *
 * Camar no esta desplegado en ningun sitio fijo: corre en el portatil, en una maquina
 * virtual o donde toque, y la direccion cambia cada dos por tres. Dejarla escrita en el
 * codigo obligaba a recompilar las tres versiones cada vez, asi que se guarda como
 * preferencia y se puede cambiar desde la propia aplicacion.
 */
class AjustesDeConexion(
    private val almacen: Settings,
    private val plataforma: Plataforma,
) {
    private val flujo = MutableStateFlow(almacen.getStringOrNull(CLAVE) ?: plataforma.urlBasePorDefecto)

    /** La direccion que esta usando ahora mismo el cliente, ya normalizada. */
    val urlBase: StateFlow<String> = flujo.asStateFlow()

    val urlPorDefecto: String get() = plataforma.urlBasePorDefecto

    /** true cuando el usuario ha tocado la direccion y ya no es la de fabrica. */
    val estaPersonalizada: Boolean get() = almacen.getStringOrNull(CLAVE) != null

    /**
     * Devuelve el motivo del rechazo, o null si la direccion se ha guardado.
     *
     * No valida gran cosa a proposito: comprobar que una URL responde es cosa de intentar
     * hablar con ella, no de mirarla. Aqui solo se descartan los errores de dedo evidentes.
     */
    fun guardar(texto: String): String? {
        val limpia = normalizar(texto)

        if (limpia.isEmpty()) return "Escribe la direccion del servidor."
        if (!limpia.startsWith("http://") && !limpia.startsWith("https://")) {
            return "Tiene que empezar por http:// o https://"
        }
        // Despues del esquema hace falta al menos un host.
        if (limpia.substringAfter("//").isEmpty()) return "Falta el nombre del servidor."

        almacen.putString(CLAVE, limpia)
        flujo.value = limpia

        return null
    }

    /** Vuelve a la direccion que le corresponde a esta plataforma. */
    fun restablecer() {
        almacen.remove(CLAVE)
        flujo.value = plataforma.urlBasePorDefecto
    }

    private companion object {
        const val CLAVE = "url_base_api"

        /**
         * Quita espacios y la barra final. Sin esto, "http://casa:5106/" y
         * "http://casa:5106" acabarian construyendo rutas con doble barra.
         */
        fun normalizar(texto: String) = texto.trim().trimEnd('/')
    }
}
