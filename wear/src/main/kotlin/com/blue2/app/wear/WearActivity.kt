package com.blue2.app.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.blue2.app.wear.presentation.WearCarScreen
import com.blue2.app.wear.presentation.WearHomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(navController, startDestination = "home") {
                    composable("home") {
                        WearHomeScreen(
                            onSelectVehicle = { vin -> navController.navigate("car/$vin") }
                        )
                    }
                    composable("car/{vin}") { back ->
                        val vin = back.arguments?.getString("vin") ?: return@composable
                        WearCarScreen(vin = vin)
                    }
                }
            }
        }
    }
}
