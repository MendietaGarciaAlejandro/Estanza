package io.github.mendietagarciaalejandro.estanza.red

/**
 * Los fallos que la aplicacion sabe distinguir.
 *
 * Camar contesta con ProblemDetails (RFC 7807) y usa el codigo de estado para decir de que
 * tipo de fallo se trata: 422 es una regla de negocio incumplida, 409 un choque con el
 * estado actual, 404 algo que no existe. Esa distincion se conserva aqui en vez de
 * aplanarlo todo a "ha fallado", porque la pantalla no reacciona igual a las tres cosas:
 * un 409 al reservar significa "prueba otro hueco" y un 401 significa "vuelve a entrar".
 */
sealed interface ErrorDeApi {
    /** Lo que se le enseña al usuario. */
    val mensaje: String

    /** No se llego a hablar con el servidor: esta apagado, la URL es otra, o no hay red. */
    data class SinConexion(val causa: String?) : ErrorDeApi {
        override val mensaje = "No se ha podido conectar con el servidor."
    }

    /** 401. O las credenciales no valen, o el token ha caducado. */
    data class NoAutorizado(override val mensaje: String) : ErrorDeApi

    /** 403: el token es bueno pero el rol no llega. */
    data class Prohibido(override val mensaje: String) : ErrorDeApi

    /** 404. */
    data class NoEncontrado(override val mensaje: String) : ErrorDeApi

    /** 409: el hueco ya esta cogido, el email ya existe... */
    data class Conflicto(override val mensaje: String) : ErrorDeApi

    /** 422: una regla del coworking. El detalle de Camar ya viene redactado para leerse. */
    data class ReglaDeNegocio(override val mensaje: String) : ErrorDeApi

    /**
     * 400 con los errores de validacion que genera [ApiController] por los atributos del
     * request. Vienen agrupados por campo, asi que se pueden pintar en el formulario.
     */
    data class DatosInvalidos(val porCampo: Map<String, List<String>>) : ErrorDeApi {
        override val mensaje = porCampo.values.flatten().firstOrNull()
            ?: "Hay algun dato mal."
    }

    /** 5xx y todo lo que no encaje arriba. */
    data class DelServidor(val codigo: Int, override val mensaje: String) : ErrorDeApi

    /** El servidor contesto algo que no se puede leer: no es Camar, o es otra version. */
    data class RespuestaIlegible(val causa: String?) : ErrorDeApi {
        override val mensaje = "El servidor ha contestado algo que no se entiende."
    }
}
