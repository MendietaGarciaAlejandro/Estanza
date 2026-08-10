package io.github.mendietagarciaalejandro.estanza.plataforma

import org.koin.core.module.Module

/**
 * Lo poco que la aplicacion necesita saber de donde se esta ejecutando.
 *
 * Solo hay una cosa que de verdad cambia entre las tres versiones, y es a que direccion
 * apunta la API por defecto: el emulador de Android no ve el "localhost" del PC, el
 * escritorio si, y en el navegador la pagina se sirve desde otro puerto distinto al de la
 * API. El nombre es para poder enseñarlo en la pantalla de ajustes y saber de un vistazo
 * cual de los tres binarios estas mirando.
 */
interface Plataforma {
    val nombre: String
    val urlBasePorDefecto: String
}

/**
 * Cada plataforma aporta su [Plataforma] y su forma de guardar preferencias, porque el
 * almacen no se construye igual en los tres sitios (Android necesita un Context, el
 * escritorio usa java.util.prefs y el navegador el localStorage).
 */
expect fun moduloDePlataforma(): Module
