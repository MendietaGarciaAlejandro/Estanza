package io.github.mendietagarciaalejandro.estanza.plataforma

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import org.koin.core.module.Module
import org.koin.dsl.module

private object PlataformaNavegador : Plataforma {
    override val nombre = "el navegador"

    // La pagina se sirve desde el servidor de desarrollo de webpack, en otro puerto: para
    // el navegador la API es un origen distinto y por eso Camar necesita CORS.
    override val urlBasePorDefecto = "http://localhost:5106"
}

actual fun moduloDePlataforma(): Module = module {
    single<Plataforma> { PlataformaNavegador }

    // Por debajo es el localStorage del navegador.
    single<Settings> { StorageSettings() }
}
