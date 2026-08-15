package io.github.mendietagarciaalejandro.estanza.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlin.reflect.KClass

/**
 * El ancho a partir del cual se considera que hay sitio de sobra.
 *
 * Por debajo se usa una barra abajo, que es donde llega el pulgar en un movil. Por encima,
 * un rail a la izquierda, que es lo que se espera en una ventana de escritorio o en el
 * navegador. La misma aplicacion, dos formas de moverse por ella.
 *
 * Se mide el ancho con BoxWithConstraints en vez de tirar de las clases de tamaño de
 * ventana de Material: esa libreria todavia va por versiones alpha para multiplataforma, y
 * para decidir entre dos disposiciones basta con mirar cuantos dp hay.
 */
private val AnchoDeEscritorio = 700.dp

data class Apartado(
    val ruta: Any,
    val clase: KClass<*>,
    val etiqueta: String,
    val icono: ImageVector,
    val iconoActivo: ImageVector,
)

@Composable
fun apartadosDeDentro(esAdministrador: Boolean): List<Apartado> = buildList {
    add(
        Apartado(
            Rutas.Catalogo, Rutas.Catalogo::class, "Espacios",
            Icons.Outlined.Home, Icons.Filled.Home,
        )
    )
    add(
        Apartado(
            Rutas.MisReservas, Rutas.MisReservas::class, "Reservas",
            Icons.Outlined.DateRange, Icons.Filled.DateRange,
        )
    )

    // La gestion solo aparece si el token dice que la cuenta es de administracion. Es
    // comodidad: si un socio llegara igualmente, Camar contesta 403.
    if (esAdministrador) {
        add(
            Apartado(
                Rutas.Administracion, Rutas.Administracion::class, "Gestion",
                Icons.Outlined.Build, Icons.Filled.Build,
            )
        )
    }

    add(
        Apartado(
            Rutas.Ajustes, Rutas.Ajustes::class, "Ajustes",
            Icons.Outlined.Settings, Icons.Filled.Settings,
        )
    )
}

/**
 * Coloca la navegacion a un lado o abajo segun quepa, y mete el contenido en el hueco.
 */
@Composable
fun ArmazonAdaptativo(
    navegador: NavHostController,
    apartados: List<Apartado>,
    modifier: Modifier = Modifier,
    contenido: @Composable (Modifier) -> Unit,
) {
    val entrada by navegador.currentBackStackEntryAsState()
    val destino = entrada?.destination

    fun estaEn(apartado: Apartado) =
        destino?.hierarchy?.any { it.hasRoute(apartado.clase) } == true

    fun ir(apartado: Apartado) {
        if (estaEn(apartado)) return

        navegador.navigate(apartado.ruta) {
            // Sin esto, ir y venir entre apartados apila pantallas sin parar y el boton de
            // atras del movil tarda diez toques en salir. Con saveState cada apartado se
            // acuerda ademas de por donde ibas.
            popUpTo(navegador.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth < AnchoDeEscritorio) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        apartados.forEach { apartado ->
                            val activo = estaEn(apartado)

                            NavigationBarItem(
                                selected = activo,
                                onClick = { ir(apartado) },
                                icon = {
                                    Icon(
                                        if (activo) apartado.iconoActivo else apartado.icono,
                                        contentDescription = apartado.etiqueta,
                                    )
                                },
                                label = { Text(apartado.etiqueta) },
                            )
                        }
                    }
                },
            ) { margenes ->
                contenido(Modifier.padding(margenes))
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    header = {
                        // La marca vive aqui arriba en vez de en una barra superior: la
                        // barra ocupaba una franja entera solo para poner un nombre.
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "E",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        apartados.forEach { apartado ->
                            val activo = estaEn(apartado)

                            NavigationRailItem(
                                selected = activo,
                                onClick = { ir(apartado) },
                                icon = {
                                    Icon(
                                        if (activo) apartado.iconoActivo else apartado.icono,
                                        contentDescription = apartado.etiqueta,
                                    )
                                },
                                label = { Text(apartado.etiqueta) },
                            )
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Sin Scaffold aqui, el margen de abajo lo pone este Box. En escritorio
                // y en el navegador vale cero, pero en una tableta apaisada con rail hay
                // barra del sistema y sin esto el contenido quedaria debajo.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                ) {
                    contenido(Modifier)
                }
            }
        }
    }
}

/**
 * Centra el contenido y le pone un ancho maximo.
 *
 * En una ventana de escritorio a pantalla completa, un formulario estirado de lado a lado
 * es ilegible: el ojo pierde el renglon. Se limita el ancho y se deja aire a los lados.
 */
@Composable
fun Centrado(
    modifier: Modifier = Modifier,
    anchoMaximo: androidx.compose.ui.unit.Dp = 840.dp,
    contenido: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // El orden importa y cuesta un rato darse cuenta: fillMaxWidth deja la anchura
        // fijada a la del padre, y un widthIn detras ya no puede recortarla. Primero se
        // pone el tope y despues se rellena hasta el.
        Box(modifier = Modifier.widthIn(max = anchoMaximo).fillMaxWidth()) {
            contenido()
        }
    }
}
