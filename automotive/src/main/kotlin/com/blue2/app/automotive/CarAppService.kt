package com.blue2.app.automotive

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.blue2.app.automotive.screens.HomeCarScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CarAppService : CarAppService() {

    override fun onCreateSession(): Session = Blue2CarSession()

    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
}

class Blue2CarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeCarScreen(carContext)
}
