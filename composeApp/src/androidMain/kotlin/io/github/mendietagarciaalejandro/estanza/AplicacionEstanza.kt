package io.github.mendietagarciaalejandro.estanza

import android.app.Application
import io.github.mendietagarciaalejandro.estanza.di.iniciarKoin
import org.koin.android.ext.koin.androidContext

class AplicacionEstanza : Application() {
    override fun onCreate() {
        super.onCreate()

        iniciarKoin {
            androidContext(this@AplicacionEstanza)
        }
    }
}
