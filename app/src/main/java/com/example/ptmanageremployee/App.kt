package com.example.ptmanageremployee

import android.app.Application
import com.example.ptmanageremployee.data.TokenStore

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(this)
    }
}
