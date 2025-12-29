package org.leria.eats.project

import android.app.Application

import org.koin.android.ext.koin.androidContext
import org.leria.eats.project.di.initKoin

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializa o Koin assim que o app abre
        initKoin {
            androidContext(this@AndroidApp)
        }
    }
}