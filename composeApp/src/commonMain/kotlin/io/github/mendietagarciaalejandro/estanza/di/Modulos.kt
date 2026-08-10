package io.github.mendietagarciaalejandro.estanza.di

import io.github.mendietagarciaalejandro.estanza.datos.AjustesDeConexion
import io.github.mendietagarciaalejandro.estanza.plataforma.moduloDePlataforma
import io.github.mendietagarciaalejandro.estanza.ui.conexion.ModeloDeConexion
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Aqui no se usa Hilt como en Ocrea: Hilt genera codigo con un procesador de anotaciones
 * que solo entiende de Android, asi que en un proyecto multiplataforma no sirve. Koin
 * resuelve las dependencias en tiempo de ejecucion y por eso funciona igual en los tres
 * targets, a cambio de que un error de cableado salte al arrancar y no al compilar.
 */
val moduloComun = module {
    singleOf(::AjustesDeConexion)
    viewModelOf(::ModeloDeConexion)
}

/**
 * Cada plataforma llama a esto desde su punto de entrada. Android aprovecha [extra] para
 * meter el Context, que es lo unico que Koin no puede sacar por su cuenta.
 */
fun iniciarKoin(extra: KoinAppDeclaration? = null) {
    startKoin {
        extra?.invoke(this)
        modules(moduloDePlataforma(), moduloComun)
    }
}
