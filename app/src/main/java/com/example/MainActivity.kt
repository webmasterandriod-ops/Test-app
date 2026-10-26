package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FFmpegErrorDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LabScreen
import com.example.ui.screens.LotteryDebugScreen
import com.example.ui.screens.LotteryScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShrinkPreviewScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SkipFrameTheme
import com.example.ui.viewmodel.SkipFrameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkipFrameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val viewModel: SkipFrameViewModel = viewModel()
                    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                    val currentFailure by viewModel.currentFailure.collectAsStateWithLifecycle()

                    // BackHandler for secondary screens
                    if (currentScreen !is Screen.Home && currentScreen !is Screen.Lab) {
                        BackHandler {
                            when (currentScreen) {
                                is Screen.ShrinkPreview -> viewModel.navigateTo(Screen.Home)
                                is Screen.Processing -> { /* handled inside ProcessingScreen */ }
                                is Screen.Results -> viewModel.navigateTo(Screen.Home)
                                is Screen.Lottery -> viewModel.navigateTo(Screen.Home)
                                is Screen.LotteryDebug -> viewModel.navigateTo(Screen.Lottery)
                                is Screen.Revision -> viewModel.navigateTo(Screen.Home)
                                is Screen.PersonalIntelligence -> viewModel.navigateTo(Screen.Home)
                                is Screen.Settings -> viewModel.navigateTo(Screen.Home)
                                else -> viewModel.navigateTo(Screen.Home)
                            }
                        }
                    }

                    // Screen switcher
                    when (currentScreen) {
                        is Screen.Home -> HomeScreen(viewModel = viewModel)
                        is Screen.ShrinkPreview -> ShrinkPreviewScreen(viewModel = viewModel)
                        is Screen.Processing -> ProcessingScreen(viewModel = viewModel)
                        is Screen.Results -> ResultsScreen(viewModel = viewModel)
                        is Screen.Lottery -> LotteryScreen(viewModel = viewModel)
                        is Screen.LotteryDebug -> LotteryDebugScreen(viewModel = viewModel)
                        is Screen.Lab -> LabScreen(viewModel = viewModel)
                        is Screen.Revision -> com.example.ui.screens.RevisionScreen(viewModel = viewModel)
                        is Screen.PersonalIntelligence -> com.example.ui.screens.PersonalIntelligenceScreen(
                            viewModel = viewModel,
                            initialTab = (currentScreen as Screen.PersonalIntelligence).initialTab
                        )
                        is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                    }

                    // Global FFmpeg Failure Dialog with full log, 40 lines and share button
                    currentFailure?.let { failure ->
                        FFmpegErrorDialog(
                            failure = failure,
                            onDismiss = { viewModel.dismissFailureDialog() }
                        )
                    }
                }
            }
        }
    }
}
