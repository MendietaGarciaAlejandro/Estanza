package io.github.mendietagarciaalejandro.estanza.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mendietagarciaalejandro.estanza.datos.CampoDeAlta
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import io.github.mendietagarciaalejandro.estanza.sesion.Sesion
import io.github.mendietagarciaalejandro.estanza.ui.acceso.ModeloDeAcceso
import io.github.mendietagarciaalejandro.estanza.ui.acceso.PantallaDeAcceso
import io.github.mendietagarciaalejandro.estanza.ui.alta.ModeloDeAlta
import io.github.mendietagarciaalejandro.estanza.ui.alta.PantallaDeAlta
import io.github.mendietagarciaalejandro.estanza.ui.conexion.ModeloDeConexion
import io.github.mendietagarciaalejandro.estanza.ui.conexion.PantallaDeConexion
import io.github.mendietagarciaalejandro.estanza.ui.inicio.PantallaDeInicio
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Reparte la aplicacion en dos grafos segun haya sesion o no.
 *
 * Podria ser un grafo unico con popUpTo al entrar y al salir, pero entonces habria que
 * acordarse de limpiar la pila en los dos sitios y ademas cuando el token caduca solo. Asi
 * la unica fuente de verdad es el StateFlow del almacen: si se abre sesion se entra, si se
 * cierra (a mano o porque un 401 la tiro) se sale, y no hay pila que arrastre pantallas de
 * la sesion anterior.
 */
@Composable
fun Navegacion() {
    val sesiones: AlmacenDeSesion = koinInject()
    val sesion by sesiones.sesion.collectAsStateWithLifecycle()

    when (val abierta = sesion) {
        null -> GrafoDeFuera()
        else -> GrafoDeDentro(sesion = abierta, alSalir = sesiones::cerrar)
    }
}

@Composable
private fun GrafoDeFuera() {
    val navegador = rememberNavController()

    NavHost(navController = navegador, startDestination = Rutas.Acceso) {
        composable<Rutas.Acceso> {
            val modelo = koinViewModel<ModeloDeAcceso>()
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeAcceso(
                estado = estado,
                alEscribirEmail = modelo::escribirEmail,
                alEscribirContrasena = modelo::escribirContrasena,
                alEntrar = modelo::entrar,
                alIrAlAlta = { navegador.navigate(Rutas.Alta) },
                alIrAAjustes = { navegador.navigate(Rutas.Ajustes) },
            )
        }

        composable<Rutas.Alta> {
            val modelo = koinViewModel<ModeloDeAlta>()
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeAlta(
                estado = estado,
                alEditarNombre = { modelo.editar(CampoDeAlta.NombreCompleto) { copy(nombreCompleto = it) } },
                alEditarEmail = { modelo.editar(CampoDeAlta.Email) { copy(email = it) } },
                alEditarContrasena = { modelo.editar(CampoDeAlta.Contrasena) { copy(contrasena = it) } },
                alElegirPlan = modelo::elegirPlan,
                alEditarDocumento = { modelo.editar(CampoDeAlta.Documento) { copy(documento = it) } },
                alEditarTelefono = { modelo.editar(CampoDeAlta.Telefono) { copy(telefono = it) } },
                alEditarCodigoPostal = { modelo.editar(CampoDeAlta.CodigoPostal) { copy(codigoPostal = it) } },
                alEditarCuenta = { modelo.editar(CampoDeAlta.CuentaBancaria) { copy(cuentaBancaria = it) } },
                alEnviar = modelo::enviar,
                alVolver = navegador::popBackStack,
            )
        }

        composable<Rutas.Ajustes> {
            Ajustes(alVolver = navegador::popBackStack)
        }
    }
}

@Composable
private fun GrafoDeDentro(sesion: Sesion, alSalir: () -> Unit) {
    val navegador = rememberNavController()

    NavHost(navController = navegador, startDestination = Rutas.Inicio) {
        composable<Rutas.Inicio> {
            PantallaDeInicio(
                sesion = sesion,
                alIrAAjustes = { navegador.navigate(Rutas.Ajustes) },
                alSalir = alSalir,
            )
        }

        composable<Rutas.Ajustes> {
            Ajustes(alVolver = navegador::popBackStack)
        }
    }
}

/** La misma pantalla de conexion sirve dentro y fuera, asi que se monta en los dos grafos. */
@Composable
private fun Ajustes(alVolver: () -> Unit) {
    val modelo = koinViewModel<ModeloDeConexion>()
    val estado by modelo.estado.collectAsStateWithLifecycle()

    PantallaDeConexion(
        estado = estado,
        alEscribir = modelo::escribir,
        alGuardar = modelo::guardar,
        alRestablecer = modelo::restablecer,
        alProbar = modelo::probar,
        alVolver = alVolver,
    )
}
