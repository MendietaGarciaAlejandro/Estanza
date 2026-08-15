package io.github.mendietagarciaalejandro.estanza.ui.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * El titulo grande que abre cada pantalla.
 *
 * Va dentro del contenido y no en una barra superior a proposito: asi se desplaza con la
 * lista y deja de ocupar sitio en cuanto empiezas a leer, que es lo que hace que en un movil
 * quepa mas contenido sin que la pantalla parezca apretada.
 */
@Composable
fun TituloGrande(
    texto: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                texto,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )

            acciones()
        }

        if (subtitulo != null) {
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Los margenes del sistema que le tocan al contenido: los lados y la parte de arriba.
 *
 * El de abajo no, porque lo pone el armazon: es quien sabe si debajo hay una barra de
 * navegacion. Aplicarlo aqui tambien dejaba en Android un hueco del alto de la barra del
 * sistema por duplicado.
 */
@Composable
fun Modifier.margenesSeguros(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
)

/**
 * El cuerpo de una pantalla que se desplaza entera: respeta las zonas del sistema, deja
 * margen a los lados y separa lo que haya dentro con un ritmo constante.
 */
@Composable
fun ColumnaDesplazable(
    modifier: Modifier = Modifier,
    espaciado: androidx.compose.ui.unit.Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .margenesSeguros()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(espaciado),
        content = contenido,
    )
}

/**
 * Como [ColumnaDesplazable] pero con el contenido centrado a lo alto cuando sobra sitio.
 *
 * Es para las pantallas cortas, como la de entrar: con el contenido pegado arriba, en una
 * ventana de escritorio queda media pantalla vacia debajo y parece que falta algo.
 *
 * El heightIn con el alto de la ventana es lo que hace que funcione: dentro de un scroll la
 * altura disponible es infinita, asi que un fillMaxSize no hace nada y el centrado no tiene
 * contra que centrarse. Poniendole un minimo, se centra cuando cabe y crece y se desplaza
 * cuando no.
 */
@Composable
fun ColumnaCentrada(
    modifier: Modifier = Modifier,
    espaciado: androidx.compose.ui.unit.Dp = 12.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val altoDisponible = maxHeight

        Column(
            modifier = Modifier
                .margenesSeguros()
                .verticalScroll(rememberScrollState())
                .heightIn(min = altoDisponible)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(espaciado, Alignment.CenterVertically),
            content = contenido,
        )
    }
}

/** Rotulo de seccion, en mayusculas y pequeño, como el que encabeza cada grupo en iOS. */
@Composable
fun RotuloDeSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
    )
}

/**
 * Una tarjeta blanca sobre el fondo calido que agrupa varias filas.
 *
 * Es la pieza que mas se repite en toda la aplicacion. En vez de una tarjeta por elemento,
 * un solo bloque con separadores finos por dentro: ocupa menos, se lee mejor y es lo que
 * hace que la pantalla parezca ordenada aunque tenga muchos datos.
 */
@Composable
fun Grupo(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(content = contenido)
    }
}

/** El separador de dentro de un [Grupo]: sangrado por la izquierda, como en una lista de iOS. */
@Composable
fun SeparadorDeGrupo(sangria: androidx.compose.ui.unit.Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = sangria),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Una fila de las que van dentro de un [Grupo]. */
@Composable
fun FilaDeGrupo(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    delante: @Composable (() -> Unit)? = null,
    detras: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (delante != null) delante()

        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)

            if (subtitulo != null) {
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (detras != null) detras()
    }
}

/** El circulo de color con la inicial del tipo de recurso. */
@Composable
fun Distintivo(texto: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

/** Etiqueta pequeña de estado, con el color de fondo muy rebajado. */
@Composable
fun Etiqueta(texto: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** El boton de la accion principal: ancho completo y alto, como los de iOS. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    trabajando: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !trabajando,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (trabajando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Campo de texto.
 *
 * Con [reservaAyuda] el hueco del mensaje se pinta siempre, aunque este vacio, para que un
 * formulario largo no pegue un salto cada vez que aparece o desaparece un error. En una
 * pantalla de dos campos y sin errores por campo eso solo deja un pasillo de aire entre
 * ellos, asi que ahi se apaga.
 */
@Composable
fun CampoDeTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    marcador: String? = null,
    reservaAyuda: Boolean = true,
    tipoDeTeclado: KeyboardType = KeyboardType.Text,
    esContrasena: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        placeholder = if (marcador != null) {
            { Text(marcador) }
        } else {
            null
        },
        singleLine = true,
        isError = error != null,
        supportingText = if (reservaAyuda || error != null || ayuda != null) {
            { Text(error ?: ayuda.orEmpty()) }
        } else {
            null
        },
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            // Sin el borde gris de siempre: el campo se apoya en el relleno y solo se
            // dibuja la linea cuando esta enfocado o cuando algo esta mal.
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        visualTransformation = if (esContrasena) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = tipoDeTeclado),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Mensaje de error de los que no cuelgan de ningun campo. */
@Composable
fun AvisoDeError(mensaje: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

/** Aviso de que algo ha salido bien. */
@Composable
fun AvisoBueno(mensaje: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

/** Lo que se ve mientras se espera a la primera carga. */
@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Una lista vacia, un fallo, o cualquier sitio donde no hay nada que enseñar. */
@Composable
fun EstadoVacio(
    titulo: String,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    accion: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        if (detalle != null) {
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (accion != null) {
            Box(modifier = Modifier.padding(top = 8.dp)) { accion() }
        }
    }
}
