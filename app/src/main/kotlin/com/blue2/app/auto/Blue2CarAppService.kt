package com.blue2.app.auto

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class Blue2CarAppService : CarAppService() {
    override fun onCreateSession(): Session = Blue2CarSession()
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
}

class Blue2CarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeCarScreen(carContext)
}
