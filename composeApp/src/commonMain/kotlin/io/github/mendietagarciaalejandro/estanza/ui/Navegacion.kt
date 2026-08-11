package io.github.mendietagarciaalejandro.estanza.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.mendietagarciaalejandro.estanza.datos.CampoDeAlta
import io.github.mendietagarciaalejandro.estanza.datos.CatalogoDeRecursos
import io.github.mendietagarciaalejandro.estanza.sesion.AlmacenDeSesion
import io.github.mendietagarciaalejandro.estanza.ui.acceso.ModeloDeAcceso
import io.github.mendietagarciaalejandro.estanza.ui.admin.AccionesDeAdmin
import io.github.mendietagarciaalejandro.estanza.ui.admin.ModeloDeAdmin
import io.github.mendietagarciaalejandro.estanza.ui.admin.PantallaDeAdmin
import io.github.mendietagarciaalejandro.estanza.ui.acceso.PantallaDeAcceso
import io.github.mendietagarciaalejandro.estanza.ui.alta.ModeloDeAlta
import io.github.mendietagarciaalejandro.estanza.ui.alta.PantallaDeAlta
import io.github.mendietagarciaalejandro.estanza.ui.conexion.ModeloDeConexion
import io.github.mendietagarciaalejandro.estanza.ui.conexion.PantallaDeConexion
import io.github.mendietagarciaalejandro.estanza.ui.catalogo.ModeloDeCatalogo
import io.github.mendietagarciaalejandro.estanza.ui.catalogo.PantallaDeCatalogo
import io.github.mendietagarciaalejandro.estanza.ui.recurso.ModeloDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.recurso.PantallaDeRecurso
import io.github.mendietagarciaalejandro.estanza.ui.reservas.ModeloDeReservas
import io.github.mendietagarciaalejandro.estanza.ui.reservas.PantallaDeReservas
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val catalogo: CatalogoDeRecursos = koinInject()
    val sesion by sesiones.sesion.collectAsStateWithLifecycle()

    // El alcance vive aqui y no dentro del grafo: al cerrar sesion el grafo desaparece de
    // la composicion, y con el se llevaria por delante la corrutina que vacia el catalogo.
    val alcance = rememberCoroutineScope()

    val abierta = sesion
    if (abierta == null) {
        GrafoDeFuera()
    } else {
        GrafoDeDentro(
            esAdministrador = abierta.esAdministrador,
            alSalir = {
                // Que el siguiente que entre no se encuentre el catalogo del anterior.
                alcance.launch { catalogo.olvidar() }
                sesiones.cerrar()
            }
        )
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
private fun GrafoDeDentro(esAdministrador: Boolean, alSalir: () -> Unit) {
    val navegador = rememberNavController()

    NavHost(navController = navegador, startDestination = Rutas.Catalogo) {
        composable<Rutas.Catalogo> {
            val modelo = koinViewModel<ModeloDeCatalogo>()
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeCatalogo(
                estado = estado,
                alFiltrar = modelo::filtrarPor,
                alAbrirRecurso = { navegador.navigate(Rutas.Recurso(it.id)) },
                alReintentar = { modelo.cargar(refrescar = true) },
                alIrAMisReservas = { navegador.navigate(Rutas.MisReservas) },
                // El rol viene del token que emitio Camar, no de nada que decida el
                // cliente. Esconder el boton es comodidad; la puerta la guarda el servidor.
                alIrAAdministracion = if (esAdministrador) {
                    { navegador.navigate(Rutas.Administracion) }
                } else {
                    null
                },
                alIrAAjustes = { navegador.navigate(Rutas.Ajustes) },
                alSalir = alSalir,
            )
        }

        composable<Rutas.Administracion> {
            val modelo = koinViewModel<ModeloDeAdmin>()
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeAdmin(
                estado = estado,
                alCambiarSeccion = modelo::verSeccion,
                acciones = AccionesDeAdmin(
                    alEscribirMotivo = modelo::escribirMotivo,
                    alDiaAnterior = modelo::diaAnterior,
                    alDiaSiguiente = modelo::diaSiguiente,
                    alBloquear = modelo::bloquearDia,
                    alDesbloquear = modelo::desbloquearDia,
                    alEscribirNombre = { texto -> modelo.editarFormulario { copy(nombre = texto) } },
                    alElegirTipo = { tipo -> modelo.editarFormulario { copy(tipo = tipo) } },
                    alEscribirCapacidad = { texto -> modelo.editarFormulario { copy(capacidad = texto) } },
                    alCrearRecurso = modelo::crearRecurso,
                    alDarDeBaja = modelo::darDeBaja,
                    alFiltrarReservas = modelo::filtrarReservasPor,
                ),
                alVolver = navegador::popBackStack,
            )
        }

        composable<Rutas.MisReservas> {
            val modelo = koinViewModel<ModeloDeReservas>()
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeReservas(
                estado = estado,
                alPedirCancelar = modelo::preguntarSiCancelar,
                alConfirmarCancelacion = modelo::confirmarCancelacion,
                alDejarloEstar = modelo::dejarloEstar,
                alReintentar = modelo::cargar,
                alVolver = navegador::popBackStack,
            )
        }

        composable<Rutas.Recurso> { entrada ->
            val ruta = entrada.toRoute<Rutas.Recurso>()

            // La clave hace que al abrir otro recurso se cree un modelo nuevo en vez de
            // reutilizar el del anterior con su fecha y sus huecos.
            val modelo = koinViewModel<ModeloDeRecurso>(key = ruta.id) { parametersOf(ruta.id) }
            val estado by modelo.estado.collectAsStateWithLifecycle()

            PantallaDeRecurso(
                estado = estado,
                alDiaAnterior = modelo::diaAnterior,
                alDiaSiguiente = modelo::diaSiguiente,
                alPulsarHueco = modelo::pulsarHueco,
                alReservar = modelo::reservar,
                alReintentar = modelo::reintentar,
                alVolver = navegador::popBackStack,
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
