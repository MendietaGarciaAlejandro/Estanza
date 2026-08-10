package io.github.mendietagarciaalejandro.estanza.sesion

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Guarda la sesion entre arranques.
 *
 * Se guarda en las mismas preferencias que la direccion del servidor, que en Android son
 * SharedPreferences y en el navegador el localStorage: ninguno de los dos es un sitio
 * seguro. Para un proyecto de portfolio con un token de vida corta es asumible, pero en
 * algo real el token de Android tendria que ir a EncryptedSharedPreferences.
 */
class AlmacenDeSesion(
    private val almacen: Settings,
    private val reloj: Clock,
) {
    private val flujo = MutableStateFlow(leerDelAlmacen())

    /** La sesion guardada, este vigente o no. */
    val sesion: StateFlow<Sesion?> = flujo.asStateFlow()

    /** La sesion solo si todavia sirve para autenticar una peticion. */
    val vigente: Sesion? get() = flujo.value?.takeIf { it.vigenteEn(reloj.now()) }

    fun abrir(nueva: Sesion) {
        almacen.putString(TOKEN, nueva.token)
        almacen.putLong(CADUCA, nueva.caducaEn.toEpochMilliseconds())
        almacen.putString(USUARIO, nueva.idUsuario)
        almacen.putString(ROL, nueva.rol)

        flujo.value = nueva
    }

    fun cerrar() {
        // Camar no tiene lista negra de tokens: cerrar sesion es olvidarlo por aqui y
        // esperar a que caduque solo. Con tokens de una hora y sin refresh es suficiente.
        listOf(TOKEN, CADUCA, USUARIO, ROL).forEach(almacen::remove)

        flujo.value = null
    }

    private fun leerDelAlmacen(): Sesion? {
        val token = almacen.getStringOrNull(TOKEN) ?: return null
        val caduca = almacen.getLongOrNull(CADUCA) ?: return null

        return Sesion(
            token = token,
            caducaEn = Instant.fromEpochMilliseconds(caduca),
            idUsuario = almacen.getStringOrNull(USUARIO).orEmpty(),
            rol = almacen.getStringOrNull(ROL).orEmpty(),
        )
    }

    private companion object {
        const val TOKEN = "sesion_token"
        const val CADUCA = "sesion_caduca_en"
        const val USUARIO = "sesion_usuario"
        const val ROL = "sesion_rol"
    }
}
