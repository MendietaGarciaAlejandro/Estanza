package io.github.mendietagarciaalejandro.estanza.plataforma

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private object PlataformaAndroid : Plataforma {
    override val nombre = "Android"

    // 10.0.2.2 es como el emulador ve el localhost del PC que lo aloja. En un movil de
    // verdad no vale y hay que poner la IP de la maquina en la red local, que cambia segun
    // donde estes; por eso la direccion se puede editar desde la propia aplicacion.
    override val urlBasePorDefecto = "http://10.0.2.2:5106"
}

actual fun moduloDePlataforma(): Module = module {
    single<Plataforma> { PlataformaAndroid }

    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("estanza", Context.MODE_PRIVATE)
        )
    }
}
