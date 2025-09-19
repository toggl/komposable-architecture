package com.toggl.komposable.sample.petsnavigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.toggl.komposable.sample.petsnavigation.ui.theme.PetsTheme

class PetsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetsTheme {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                    navigationBarStyle = SystemBarStyle.light(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                )
                PetsNavigationApp()
            }
        }
    }
}
