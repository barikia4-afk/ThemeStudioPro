package com.example.themestudiopro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.themestudiopro.core.model.AppItem
import com.example.themestudiopro.core.model.CoverageStats
import com.example.themestudiopro.core.model.IconStyle
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.PackMeta
import com.example.themestudiopro.core.model.Source
import com.example.themestudiopro.iconpack.Matcher
import com.example.themestudiopro.iconpack.PackBuilder
import com.example.themestudiopro.iconpack.PackDiscovery
import com.example.themestudiopro.iconpack.PackReader
import com.example.themestudiopro.ui.coverage.CoverageScreen
import com.example.themestudiopro.ui.export.ExportScreen
import com.example.themestudiopro.ui.iconpacks.IconEditorScreen
import com.example.themestudiopro.ui.iconpacks.IconPacksScreen
import com.example.themestudiopro.ui.navigation.Screen
import com.example.themestudiopro.ui.navigation.bottomNavItems
import com.example.themestudiopro.ui.theme.ThemeStudioProTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ThemeStudioProTheme {
                MainContent()
            }
        }
    }

    @Composable
    fun MainContent() {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()

        var activePackPkg by remember { mutableStateOf<String?>(null) }
        var installedApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var matches by remember { mutableStateOf<List<MatchResult>>(emptyList()) }
        var currentStyle by remember { mutableStateOf(IconStyle()) }
        var packMeta by remember { mutableStateOf<PackMeta?>(null) }
        var stats by remember { mutableStateOf(CoverageStats()) }

        // Load apps and resolve matches whenever pack or apps change
        LaunchedEffect(activePackPkg) {
            val apps = withContext(Dispatchers.IO) {
                PackDiscovery.getInstalledApps(this@MainActivity)
            }
            installedApps = apps

            val meta = activePackPkg?.let {
                withContext(Dispatchers.IO) {
                    PackReader.getOrParseAppFilter(this@MainActivity, it).meta
                }
            }
            packMeta = meta

            val resolved = withContext(Dispatchers.IO) {
                Matcher.matchAllApps(
                    ctx = this@MainActivity,
                    apps = apps,
                    primaryPack = activePackPkg
                )
            }
            matches = resolved
            stats = Matcher.calculateStats(resolved)
        }

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.titleResId)) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.IconPacks.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.IconPacks.route
                ) {
                    composable(Screen.IconPacks.route) {
                        IconPacksScreen(
                            activePackPkg = activePackPkg,
                            onSelectPack = { pkg ->
                                activePackPkg = pkg
                                Toast.makeText(
                                    this@MainActivity,
                                    "Selected icon pack: $pkg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    composable(Screen.Editor.route) {
                        IconEditorScreen(
                            previewMatches = matches,
                            currentStyle = currentStyle,
                            packMeta = packMeta,
                            onStyleChange = { newStyle ->
                                currentStyle = newStyle
                            }
                        )
                    }

                    composable(Screen.Coverage.route) {
                        CoverageScreen(
                            matches = matches,
                            stats = stats,
                            style = currentStyle,
                            packMeta = packMeta,
                            onConfirmGuessed = {
                                // Bulk confirm guessed icons
                                matches = matches.map {
                                    if (it.source == Source.FUZZY) it.copy(source = Source.PRIMARY) else it
                                }
                                stats = Matcher.calculateStats(matches)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Confirmed all guessed icons!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    composable(Screen.Export.route) {
                        ExportScreen(
                            activePackPkg = activePackPkg,
                            onBuildApk = {
                                scope.launch {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Generating Icon Pack APK...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onExportZip = {
                                scope.launch {
                                    try {
                                        val file = PackBuilder.exportIconsZip(
                                            ctx = this@MainActivity,
                                            matches = matches,
                                            style = currentStyle,
                                            packMeta = packMeta
                                        )
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Exported to Downloads: ${file.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Export failed: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
