package io.github.mendietagarciaalejandro.estanza.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Estanza es el cliente de un coworking, no una aplicacion de banca: la paleta tira a
 * terracota y oliva en vez del morado que trae Material por defecto.
 *
 * La estructura, en cambio, se parece a la de iOS: el fondo es un tono calido apagado y las
 * tarjetas van encima en blanco, con mucho redondeo y separadores finos en lugar de bordes.
 * Asi el color se usa como acento y no como bloque, que es lo que hace que una pantalla
 * llena de datos siga siendo comoda de leer.
 */
private val Terracota = Color(0xFFB4522F)
private val TerracotaSuave = Color(0xFFFFDBCF)
private val Oliva = Color(0xFF5F6B52)
private val OlivaSuave = Color(0xFFDCE7CD)
private val Mostaza = Color(0xFFA67C3D)

private val esquemaClaro = lightColorScheme(
    primary = Terracota,
    onPrimary = Color.White,
    primaryContainer = TerracotaSuave,
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Oliva,
    onSecondary = Color.White,
    secondaryContainer = OlivaSuave,
    onSecondaryContainer = Color(0xFF141F0C),
    tertiary = Mostaza,

    // El fondo no es blanco: las tarjetas blancas necesitan algo mas oscuro debajo para
    // que se vean como piezas separadas sin tener que ponerles borde.
    background = Color(0xFFF5F1ED),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFEDE7E1),
    onSurfaceVariant = Color(0xFF7A6E67),

    // Los separadores de las listas agrupadas: una linea de un pixel, no un borde.
    outlineVariant = Color(0xFFE3DAD3),
    outline = Color(0xFFB9ADA5),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val esquemaOscuro = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5D1900),
    primaryContainer = Color(0xFF7C2E14),
    onPrimaryContainer = TerracotaSuave,
    secondary = Color(0xFFBDCBAE),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A35),
    onSecondaryContainer = OlivaSuave,
    tertiary = Color(0xFFE5C089),

    background = Color(0xFF121110),
    onBackground = Color(0xFFF0EAE6),
    surface = Color(0xFF1E1C1A),
    onSurface = Color(0xFFF0EAE6),
    surfaceVariant = Color(0xFF2A2725),
    onSurfaceVariant = Color(0xFFA8998F),

    outlineVariant = Color(0xFF35302D),
    outline = Color(0xFF6B615B),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Redondeos generosos. Material por defecto se queda en 12 para las tarjetas grandes; aqui
 * se sube porque es lo que hace que una lista de tarjetas parezca un bloque blando y no una
 * tabla.
 */
private val formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * La escala de texto no es la de Material sino una mas parecida a la de iOS: cuerpo de 17,
 * titulos muy grandes y con el espaciado entre letras apretado, y secundarios de 15 y 13.
 *
 * No se empaqueta ninguna fuente: se usa la del sistema, que en cada plataforma es la que el
 * usuario espera ver. Meter un fichero de fuente daria mas caracter, pero engorda la
 * descarga de la version web y no compensaba.
 */
private val tipografia = Typography().let { base ->
    Typography(
        displaySmall = TextStyle(
            fontSize = 34.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp,
        ),
        headlineLarge = TextStyle(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = base.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium),
        // Los rotulos en mayusculas necesitan aire entre letras o se leen como un bloque.
        labelSmall = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        ),
    )
}

/**
 * El tema no lee el color dinamico de Android a proposito: la aplicacion se ve igual en el
 * movil, en el escritorio y en el navegador, y eso vale mas aqui que adaptarse al fondo de
 * pantalla en una sola de las tres.
 */
@Composable
fun TemaEstanza(oscuro: Boolean, contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) esquemaOscuro else esquemaClaro,
        shapes = formas,
        typography = tipografia,
        content = contenido,
    )
}
