package io.github.mendietagarciaalejandro.estanza.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Estanza es un coworking, no una aplicacion de banca: la paleta tira a terracota y verde
// oliva en vez del morado que trae Material por defecto.
private val Terracota = Color(0xFF9B4A2F)
private val TerracotaClaro = Color(0xFFFFDBCF)
private val Oliva = Color(0xFF56624B)
private val OlivaClaro = Color(0xFFD9E7C9)
private val Arena = Color(0xFFFBF1EB)
private val Carbon = Color(0xFF201A18)

private val esquemaClaro = lightColorScheme(
    primary = Terracota,
    onPrimary = Color.White,
    primaryContainer = TerracotaClaro,
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Oliva,
    onSecondary = Color.White,
    secondaryContainer = OlivaClaro,
    onSecondaryContainer = Color(0xFF141F0C),
    background = Arena,
    onBackground = Carbon,
    surface = Arena,
    onSurface = Carbon,
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val esquemaOscuro = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5D1900),
    primaryContainer = Color(0xFF7C2E14),
    onPrimaryContainer = TerracotaClaro,
    secondary = Color(0xFFBDCBAE),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A35),
    onSecondaryContainer = OlivaClaro,
    background = Color(0xFF181210),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF181210),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * El tema no lee el color dinamico de Android a proposito: la aplicacion se ve igual en el
 * movil, en el escritorio y en el navegador, y eso vale mas aqui que adaptarse al fondo de
 * pantalla en una sola de las tres.
 */
@Composable
fun TemaEstanza(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) esquemaOscuro else esquemaClaro,
        content = contenido,
    )
}
