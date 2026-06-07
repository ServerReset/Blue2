package com.blue2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.ui.navigation.AppNavigation
import com.blue2.app.ui.navigation.Destinations
import com.blue2.app.ui.theme.Blue2Theme
import com.blue2.app.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        val isLoggedIn = runBlocking { prefs.isLoggedIn.first() }

        enableEdgeToEdge()
        setContent {
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val dynamicColor by prefs.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            val amoledMode by prefs.amoledMode.collectAsStateWithLifecycle(initialValue = false)
            val useAtkinsonFont by prefs.useAtkinsonFont.collectAsStateWithLifecycle(initialValue = false)

            Blue2Theme(
                themeMode = when (themeMode) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
                dynamicColor = dynamicColor,
                amoledMode = amoledMode,
                useAtkinsonFont = useAtkinsonFont,
            ) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    startDestination = if (isLoggedIn) Destinations.HOME else Destinations.LOGIN,
                )
            }
        }
    }
}
