package com.example.themestudiopro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Publish
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object IconPacks : Screen("icon_packs", com.example.themestudiopro.R.string.nav_icon_packs, Icons.Default.Layers)
    object Editor : Screen("editor", com.example.themestudiopro.R.string.nav_editor, Icons.Default.ColorLens)
    object Coverage : Screen("coverage", com.example.themestudiopro.R.string.nav_coverage, Icons.Default.CheckCircle)
    object Export : Screen("export", com.example.themestudiopro.R.string.nav_export, Icons.Default.Publish)
}

val bottomNavItems = listOf(
    Screen.IconPacks,
    Screen.Editor,
    Screen.Coverage,
    Screen.Export
)
