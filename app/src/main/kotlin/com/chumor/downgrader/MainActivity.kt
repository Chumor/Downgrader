package com.chumor.downgrader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chumor.downgrader.presentation.DowngraderViewModel
import com.chumor.downgrader.presentation.DowngraderViewModelFactory
import com.chumor.downgrader.presentation.ui.DowngraderRoute
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val factory = remember { DowngraderViewModelFactory(applicationContext) }
            val viewModel: DowngraderViewModel = viewModel(factory = factory)
            val themeController = remember { ThemeController(ColorSchemeMode.System) }

            MiuixTheme(controller = themeController) {
                DowngraderRoute(viewModel = viewModel)
            }
        }
    }
}
