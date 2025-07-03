package com.eddyslarez.lectornfc

import android.app.Application
import com.eddyslarez.lectornfc.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MifareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MifareApplication)
            modules(appModule)
        }
    }
}
