package com.example.themestudiopro.core.model

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

enum class IconShape {
    SQUARE,
    ROUNDED,
    CIRCLE,
    SQUIRCLE,
    TEARDROP
}

enum class ColorMode {
    ORIGINAL,
    PACK,
    TINT,
    DUOTONE,
    MONOCHROME,
    PALETTE
}

enum class Source {
    OVERRIDE,
    PRIMARY,
    PRIMARY_PKG,
    SECONDARY,
    FUZZY,
    FALLBACK
}

@Serializable
data class Adjust(
    val hue: Float = 0f,          // -180..180
    val sat: Float = 0f,          // -100..100
    val bri: Float = 0f,          // -100..100
    val con: Float = 0f,          // -100..100
    val opacity: Float = 100f,    // 0..100
    val gray: Boolean = false,
    val invert: Boolean = false,
    val scale: Float = 100f,      // 60..120
    val padding: Float = 0f
)

@Serializable
data class IconStyle(
    val shape: IconShape = IconShape.ROUNDED,
    val radiusPct: Float = 22f,   // 0..50%
    val squircleN: Float = 4.0f,  // 2.0..6.0
    val colorMode: ColorMode = ColorMode.ORIGINAL,
    val tint: Int? = null,
    val tint2: Int? = null,
    val reshapePackIcons: Boolean = false,
    val adjust: Adjust = Adjust()
)

data class IconOverride(
    val component: String,
    val source: String, // "PACK:<pkg>:<drawable>" | "URI:<uri>" | "ORIGINAL"
    val styleJson: String? = null
)

data class PackPriority(
    val pkg: String,
    val order: Int,
    val enabled: Boolean
)

data class PackInfo(
    val pkg: String,
    val label: String,
    val icon: Drawable? = null,
    val iconCount: Int = 0,
    val coveragePct: Int = 0,
    val isPrimary: Boolean = false,
    val previewDrawables: List<String> = emptyList()
)

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val componentString: String = "$packageName/$activityName",
    val originalIcon: Drawable? = null
)

data class MatchResult(
    val app: AppItem,
    val resolvedDrawableName: String? = null,
    val resolvedFromPack: String? = null,
    val source: Source = Source.FALLBACK,
    val confidence: Float = 1.0f
)

data class PackMeta(
    val back: List<String> = emptyList(),
    val upon: String? = null,
    val mask: String? = null,
    val scale: Float = 1.0f
)

data class CoverageStats(
    val totalApps: Int = 0,
    val fromPack: Int = 0,
    val secondary: Int = 0,
    val guessed: Int = 0,
    val fallback: Int = 0,
    val overridden: Int = 0
) {
    val coveragePct: Int
        get() = if (totalApps > 0) 100 else 0 // Guaranteed 100% via fallback chain
    val exactCoveragePct: Int
        get() = if (totalApps > 0) ((fromPack + secondary + overridden) * 100) / totalApps else 0
}
