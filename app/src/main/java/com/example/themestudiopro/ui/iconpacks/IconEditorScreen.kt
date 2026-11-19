package com.example.themestudiopro.ui.iconpacks

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.themestudiopro.R
import com.example.themestudiopro.core.model.Adjust
import com.example.themestudiopro.core.model.ColorMode
import com.example.themestudiopro.core.model.IconShape
import com.example.themestudiopro.core.model.IconStyle
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.PackMeta
import com.example.themestudiopro.iconpack.Renderer
import com.example.themestudiopro.ui.components.GlassCard
import com.example.themestudiopro.ui.components.GlassSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconEditorScreen(
    previewMatches: List<MatchResult>,
    currentStyle: IconStyle,
    packMeta: PackMeta?,
    onStyleChange: (IconStyle) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isComparingOriginal by remember { mutableStateOf(false) }

    val effectiveStyle = if (isComparingOriginal) {
        IconStyle(shape = IconShape.ROUNDED, colorMode = ColorMode.ORIGINAL, adjust = Adjust())
    } else currentStyle

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_editor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    OutlinedButton(
                        onClick = { onStyleChange(IconStyle()) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.reset_adjustments))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Preview Strip of Real Installed Apps
            GlassCard(
                cornerRadius = 24,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Hold to Compare Button
                        Button(
                            onClick = {},
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isComparingOriginal = true
                                        tryAwaitRelease()
                                        isComparingOriginal = false
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isComparingOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.compare_original),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(previewMatches.take(12), key = { it.app.componentString }) { match ->
                            val renderedBmp = remember(match, effectiveStyle, packMeta) {
                                Renderer.renderIcon(context, match, effectiveStyle, packMeta, 160)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(64.dp)
                            ) {
                                Image(
                                    bitmap = renderedBmp.asImageBitmap(),
                                    contentDescription = match.app.label,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = match.app.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Tabs: Shape, Colours, Size & Layout
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_shape)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_colours)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.tab_size)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> ShapeSettings(
                    style = currentStyle,
                    onStyleChange = onStyleChange
                )
                1 -> ColourSettings(
                    style = currentStyle,
                    onStyleChange = onStyleChange
                )
                2 -> SizeSettings(
                    style = currentStyle,
                    onStyleChange = onStyleChange
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ShapeSettings(
    style: IconStyle,
    onStyleChange: (IconStyle) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Select Shape",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Shape Choice Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconShape.values().forEach { shape ->
                val label = when (shape) {
                    IconShape.SQUARE -> stringResource(R.string.shape_square)
                    IconShape.ROUNDED -> stringResource(R.string.shape_rounded)
                    IconShape.CIRCLE -> stringResource(R.string.shape_circle)
                    IconShape.SQUIRCLE -> stringResource(R.string.shape_squircle)
                    IconShape.TEARDROP -> stringResource(R.string.shape_teardrop)
                }
                FilterChip(
                    selected = style.shape == shape,
                    onClick = { onStyleChange(style.copy(shape = shape)) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (style.shape == IconShape.ROUNDED) {
            GlassSlider(
                label = stringResource(R.string.corner_radius, style.radiusPct.toInt()),
                value = style.radiusPct,
                valueRange = 0f..50f,
                onValueChange = { onStyleChange(style.copy(radiusPct = it)) }
            )
        }

        if (style.shape == IconShape.SQUIRCLE) {
            GlassSlider(
                label = stringResource(R.string.squircle_n, style.squircleN),
                value = style.squircleN,
                valueRange = 2f..6f,
                onValueChange = { onStyleChange(style.copy(squircleN = it)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reshape original pack icons toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reshape_pack_icons),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = style.reshapePackIcons,
                onCheckedChange = { onStyleChange(style.copy(reshapePackIcons = it)) }
            )
        }
    }
}

@Composable
fun ColourSettings(
    style: IconStyle,
    onStyleChange: (IconStyle) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Colour Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorMode.values().take(3).forEach { mode ->
                FilterChip(
                    selected = style.colorMode == mode,
                    onClick = { onStyleChange(style.copy(colorMode = mode)) },
                    label = { Text(mode.name, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val adj = style.adjust

        GlassSlider(
            label = stringResource(R.string.slider_hue),
            value = adj.hue,
            valueRange = -180f..180f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(hue = it))) }
        )

        GlassSlider(
            label = stringResource(R.string.slider_sat),
            value = adj.sat,
            valueRange = -100f..100f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(sat = it))) }
        )

        GlassSlider(
            label = stringResource(R.string.slider_bri),
            value = adj.bri,
            valueRange = -100f..100f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(bri = it))) }
        )

        GlassSlider(
            label = stringResource(R.string.slider_con),
            value = adj.con,
            valueRange = -100f..100f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(con = it))) }
        )

        GlassSlider(
            label = stringResource(R.string.slider_opacity),
            value = adj.opacity,
            valueRange = 0f..100f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(opacity = it))) }
        )
    }
}

@Composable
fun SizeSettings(
    style: IconStyle,
    onStyleChange: (IconStyle) -> Unit
) {
    val adj = style.adjust
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        GlassSlider(
            label = stringResource(R.string.slider_scale),
            value = adj.scale,
            valueRange = 60f..120f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(scale = it))) }
        )

        GlassSlider(
            label = stringResource(R.string.slider_padding),
            value = adj.padding,
            valueRange = 0f..20f,
            onValueChange = { onStyleChange(style.copy(adjust = adj.copy(padding = it))) }
        )
    }
}
