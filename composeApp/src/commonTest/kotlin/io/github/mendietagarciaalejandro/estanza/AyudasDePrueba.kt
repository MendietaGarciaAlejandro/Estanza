package io.github.mendietagarciaalejandro.estanza

import com.russhwolf.settings.MapSettings
import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.plataforma.Plataforma
import io.github.mendietagarciaalejandro.estanza.red.ApiDeCamar
import io.github.mendietagarciaalejandro.estanza.red.crearClienteHttp
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.request.HttpRequestData
import kotlin.time.Clock
import kotlin.time.Instant

internal val AHORA: Instant = Instant.parse("2026-01-12T08:00:00Z")

internal object PlataformaDePrueba : Plataforma {
    override val nombre = "test"
    override val urlBasePorDefecto = "http://localhost:5106"
}

internal class RelojFijo(var momento: Instant = AHORA) : Clock {
    override fun now() = momento
}

/**
 * Un Camar de mentira con sus preferencias, para probar los modelos de pantalla sin
 * levantar nada. Guarda las peticiones que le llegan porque en varios casos lo que hay que
 * comprobar es justo que *no* se ha llamado a la red.
 */
internal class EntornoDePrueba(responder: MockRequestHandler) {
    val preferencias = MapSettings()
    val reloj = RelojFijo()
    val peticiones = mutableListOf<HttpRequestData>()

    val sesiones = AlmacenDeSesion(preferencias, reloj)

    val api = ApiDeCamar(
        cliente = crearClienteHttp(
            MockEngine { peticion ->
                peticiones += peticion
                responder(peticion)
            }
        ),
        ajustes = AjustesDeConexion(preferencias, PlataformaDePrueba),
        sesiones = sesiones,
    )
}
