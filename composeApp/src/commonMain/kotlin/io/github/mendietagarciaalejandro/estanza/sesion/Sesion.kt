package io.github.mendietagarciaalejandro.estanza.sesion

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * La sesion abierta: el token que Camar emitio y lo poco que hace falta saber de quien lo
 * pidio.
 *
 * El token es un JWT y lleva dentro el id y el rol, pero aqui no se abre: firmarlo lo firma
 * el servidor y es el servidor quien decide si vale. Leerlo por nuestra cuenta solo serviria
 * para creernos algo que no hemos comprobado.
 */
data class Sesion(
    val token: String,
    val caducaEn: Instant,
    val idUsuario: String,
    val rol: String,
) {
    val esAdministrador: Boolean get() = rol.equals("Admin", ignoreCase = true)

    /**
     * Camar valida el token con ClockSkew a cero, asi que caduca al segundo exacto. Se
     * descuenta un margen para no mandar una peticion que ya sabemos que va a dar 401.
     */
    fun vigenteEn(momento: Instant): Boolean = momento + MARGEN < caducaEn

    private companion object {
        val MARGEN = 30.seconds
    }
}
