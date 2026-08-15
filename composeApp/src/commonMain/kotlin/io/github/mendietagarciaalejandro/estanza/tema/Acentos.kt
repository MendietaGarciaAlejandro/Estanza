package io.github.mendietagarciaalejandro.estanza.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.github.mendietagarciaalejandro.estanza.datos.EstadoDeReserva
import io.github.mendietagarciaalejandro.estanza.datos.TipoDeRecurso

/**
 * Un color por tipo de recurso.
 *
 * Sirve para reconocer de un vistazo si una tarjeta es una sala o una mesa sin leer la
 * etiqueta. Son los mismos tres tonos de la paleta, asi que la pantalla no se convierte en
 * un semaforo: siguen siendo terracota, oliva y mostaza.
 */
@Composable
@ReadOnlyComposable
fun TipoDeRecurso.color(): Color = when (this) {
    TipoDeRecurso.SalaDeReuniones -> MaterialTheme.colorScheme.primary
    TipoDeRecurso.MesaFlexible -> MaterialTheme.colorScheme.secondary
    TipoDeRecurso.Cabina -> MaterialTheme.colorScheme.tertiary
    // Un tipo que este cliente no conoce no merece un color propio: gris y a seguir.
    TipoDeRecurso.Otro -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * Las dos letras que van dentro del circulo de color, a modo de inicial.
 *
 * Se usan letras y no iconos porque el paquete de iconos de Material no trae nada que se
 * parezca a una cabina de llamadas ni a una mesa flexible, y dibujarlos a mano para tres
 * tipos era mas trabajo del que aportaban.
 */
val TipoDeRecurso.inicial: String
    get() = when (this) {
        TipoDeRecurso.SalaDeReuniones -> "SA"
        TipoDeRecurso.MesaFlexible -> "ME"
        TipoDeRecurso.Cabina -> "CA"
        TipoDeRecurso.Otro -> "??"
    }

/** El color del distintivo de estado de una reserva. */
@Composable
@ReadOnlyComposable
fun EstadoDeReserva.color(): Color = when (this) {
    EstadoDeReserva.Confirmada -> MaterialTheme.colorScheme.secondary
    EstadoDeReserva.Cancelada -> MaterialTheme.colorScheme.error
    EstadoDeReserva.Completada -> MaterialTheme.colorScheme.onSurfaceVariant
    EstadoDeReserva.NoPresentado -> MaterialTheme.colorScheme.tertiary
    EstadoDeReserva.Otro -> MaterialTheme.colorScheme.onSurfaceVariant
}
