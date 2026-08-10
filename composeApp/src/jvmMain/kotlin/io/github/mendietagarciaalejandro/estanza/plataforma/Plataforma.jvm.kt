package io.github.mendietagarciaalejandro.estanza.plataforma

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences

private object PlataformaEscritorio : Plataforma {
    override val nombre = "escritorio"

    // La version de escritorio suele correr en la misma maquina que la API.
    override val urlBasePorDefecto = "http://localhost:5106"
}

actual fun moduloDePlataforma(): Module = module {
    single<Plataforma> { PlataformaEscritorio }

    // java.util.prefs guarda en el registro de Windows y en ~/.java en Linux; es fea pero
    // viene con la JDK y no obliga a inventarse un fichero de configuracion.
    single<Settings> {
        PreferencesSettings(Preferences.userRoot().node("io/github/mendietagarciaalejandro/estanza"))
    }
}
