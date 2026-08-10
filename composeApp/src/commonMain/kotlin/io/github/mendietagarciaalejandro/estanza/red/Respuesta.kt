package io.github.mendietagarciaalejandro.estanza.red

/**
 * Lo que devuelven las llamadas a la API.
 *
 * Se devuelve el fallo en vez de lanzarlo porque un 409 al reservar no es una situacion
 * excepcional: es la respuesta normal cuando alguien se te ha adelantado, y la pantalla
 * tiene que enseñarla igual que enseña el resultado bueno. Reservar las excepciones para
 * lo que de verdad no deberia pasar hace que el compilador obligue a tratar el caso malo.
 */
sealed interface Respuesta<out T> {
    data class Exito<T>(val valor: T) : Respuesta<T>
    data class Fallo(val error: ErrorDeApi) : Respuesta<Nothing>
}

inline fun <T, R> Respuesta<T>.map(transformar: (T) -> R): Respuesta<R> = when (this) {
    is Respuesta.Exito -> Respuesta.Exito(transformar(valor))
    is Respuesta.Fallo -> this
}

/** El valor si salio bien, o null. Para cuando el fallo ya se ha tratado por otro lado. */
fun <T> Respuesta<T>.valorONull(): T? = (this as? Respuesta.Exito)?.valor
