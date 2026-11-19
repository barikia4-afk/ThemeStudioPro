package com.example.themestudiopro.ui.coverage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.themestudiopro.R
import com.example.themestudiopro.core.model.CoverageStats
import com.example.themestudiopro.core.model.IconStyle
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.PackMeta
import com.example.themestudiopro.core.model.Source
import com.example.themestudiopro.iconpack.Renderer
import com.example.themestudiopro.ui.components.GlassCard
import com.example.themestudiopro.ui.components.StatusBadge
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverageScreen(
    matches: List<MatchResult>,
    stats: CoverageStats,
    style: IconStyle,
    packMeta: PackMeta?,
    onConfirmGuessed: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredMatches = remember(matches, searchQuery) {
        if (searchQuery.isBlank()) matches
        else {
            val q = searchQuery.lowercase(Locale.ROOT)
            matches.filter {
                it.app.label.lowercase(Locale.ROOT).contains(q) ||
                        it.app.packageName.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.coverage_audit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = stringResource(R.string.coverage_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Overview Card
            item {
                GlassCard(cornerRadius = 24) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Total Covered: ${stats.totalApps} Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Guaranteed complete coverage with fallbacks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(title = stringResource(R.string.stat_from_pack), count = stats.fromPack, color = Color(0xFF4CAF50))
                            StatItem(title = stringResource(R.string.stat_fuzzy), count = stats.guessed, color = Color(0xFFFFB300))
                            StatItem(title = stringResource(R.string.stat_fallback), count = stats.fallback, color = Color(0xFF29B6F6))
                            StatItem(title = stringResource(R.string.stat_overridden), count = stats.overridden, color = Color(0xFFAB47BC))
                        }

                        if (stats.guessed > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onConfirmGuessed,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.fix_all_guessed),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_apps)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            // App items list
            items(filteredMatches, key = { it.app.componentString }) { match ->
                AppMatchRow(match = match, style = style, packMeta = packMeta)
            }
        }
    }
}

@Composable
fun StatItem(title: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppMatchRow(match: MatchResult, style: IconStyle, packMeta: PackMeta?) {
    val context = LocalContext.current
    val renderedBitmap = remember(match, style, packMeta) {
        Renderer.renderIcon(context, match, style, packMeta, 128)
    }

    GlassCard(cornerRadius = 16) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                bitmap = renderedBitmap.asImageBitmap(),
                contentDescription = match.app.label,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = match.app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            when (match.source) {
                Source.PRIMARY, Source.PRIMARY_PKG -> StatusBadge(
                    text = stringResource(R.string.stat_from_pack),
                    color = Color(0xFF4CAF50)
                )
                Source.SECONDARY -> StatusBadge(
                    text = stringResource(R.string.stat_secondary),
                    color = Color(0xFF81C784)
                )
                Source.FUZZY -> StatusBadge(
                    text = stringResource(R.string.stat_fuzzy),
                    color = Color(0xFFFFB300)
                )
                Source.FALLBACK -> StatusBadge(
                    text = stringResource(R.string.stat_fallback),
                    color = Color(0xFF29B6F6)
                )
                Source.OVERRIDE -> StatusBadge(
                    text = stringResource(R.string.stat_overridden),
                    color = Color(0xFFAB47BC)
                )
            }
        }
    }
}
