package com.example.ui.navigation

enum class LabSubMode(val title: String, val subtitle: String) {
    DRIFT("Drift", "Radio station of moments"),
    SPLIT("Split", "Sound and picture detached"),
    MOSAIC("Mosaic", "Interactive multi-panel stream"),
    ERODE("Erode", "Movie wears away as you watch")
}

sealed class Screen {
    data object Home : Screen()
    data object ShrinkPreview : Screen()
    data object Processing : Screen()
    data object Results : Screen()
    data object Lottery : Screen()
    data object LotteryDebug : Screen()
    data class Lab(val subMode: LabSubMode = LabSubMode.DRIFT) : Screen()
    data object Revision : Screen()
    data class PersonalIntelligence(val initialTab: Int = 0) : Screen()
    data object Settings : Screen()
}
