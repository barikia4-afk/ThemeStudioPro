package com.example.themestudiopro.iconpack

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.LruCache
import androidx.palette.graphics.Palette
import com.example.themestudiopro.core.model.Adjust
import com.example.themestudiopro.core.model.ColorMode
import com.example.themestudiopro.core.model.IconShape
import com.example.themestudiopro.core.model.IconStyle
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.PackMeta
import com.example.themestudiopro.core.model.Source
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object Renderer {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory for LRU cache
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun renderIcon(
        ctx: Context,
        match: MatchResult,
        style: IconStyle,
        packMeta: PackMeta? = null,
        targetSize: Int = 192
    ): Bitmap {
        val cacheKey = "${match.app.componentString}_${match.resolvedDrawableName}_${style.hashCode()}_${targetSize}"
        bitmapCache.get(cacheKey)?.let { return it }

        var baseBitmap: Bitmap? = null

        // 1. If resolved from a pack, try to load pack drawable
        if (match.source != Source.FALLBACK && match.resolvedDrawableName != null && match.resolvedFromPack != null) {
            baseBitmap = PackReader.loadBitmap(ctx, match.resolvedFromPack, match.resolvedDrawableName, targetSize)
        }

        // 2. If no pack icon or Fallback mode, generate using Fallback pipeline
        if (baseBitmap == null) {
            baseBitmap = renderFallbackIcon(ctx, match, style, packMeta, targetSize)
        } else if (style.reshapePackIcons) {
            // Re-shape the pack's own icon if requested by user toggle
            baseBitmap = applyShapeClip(baseBitmap, style.shape, style.radiusPct, style.squircleN, targetSize)
        }

        // 3. Apply color mode & adjustment filters (Hue, Sat, Bri, Con, Opacity, Invert, Grayscale)
        val adjustedBitmap = applyAdjustments(baseBitmap, style.adjust, style.colorMode, style.tint, style.tint2)

        bitmapCache.put(cacheKey, adjustedBitmap)
        return adjustedBitmap
    }

    private fun renderFallbackIcon(
        ctx: Context,
        match: MatchResult,
        style: IconStyle,
        packMeta: PackMeta?,
        targetSize: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val originalAppBmp = match.app.originalIcon?.let { PackReader.drawableToBitmap(it, targetSize) }
            ?: Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)

        // Case A: The pack provides iconback / iconmask
        if (packMeta != null && packMeta.back.isNotEmpty() && match.resolvedFromPack != null) {
            val backIndex = abs(match.app.packageName.hashCode()) % packMeta.back.size
            val backName = packMeta.back[backIndex]
            val backBmp = PackReader.loadBitmap(ctx, match.resolvedFromPack, backName, targetSize)

            if (backBmp != null) {
                // Draw background plate
                canvas.drawBitmap(backBmp, 0f, 0f, null)

                // Scale and draw app icon
                val scale = packMeta.scale.coerceIn(0.4f, 1.0f)
                val scaledSize = (targetSize * scale).toInt()
                val offset = (targetSize - scaledSize) / 2f
                val scaledAppIcon = Bitmap.createScaledBitmap(originalAppBmp, scaledSize, scaledSize, true)

                val contentCanvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                contentCanvas.drawBitmap(scaledAppIcon, offset, offset, paint)

                // Apply iconmask if exists
                if (!packMeta.mask.isNullOrBlank()) {
                    val maskBmp = PackReader.loadBitmap(ctx, match.resolvedFromPack, packMeta.mask, targetSize)
                    if (maskBmp != null) {
                        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                        }
                        canvas.drawBitmap(maskBmp, 0f, 0f, maskPaint)
                    }
                }

                // Apply iconupon if exists
                if (!packMeta.upon.isNullOrBlank()) {
                    val uponBmp = PackReader.loadBitmap(ctx, match.resolvedFromPack, packMeta.upon, targetSize)
                    if (uponBmp != null) {
                        canvas.drawBitmap(uponBmp, 0f, 0f, null)
                    }
                }

                return output
            }
        }

        // Case B: No pack iconback - generate shape plate with dominant color
        val dominantColor = extractDominantColor(originalAppBmp)
        val plateBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val plateCanvas = Canvas(plateBitmap)

        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dominantColor
            style = Paint.Style.FILL
        }

        val shapePath = createShapePath(style.shape, style.radiusPct, style.squircleN, targetSize)
        plateCanvas.drawPath(shapePath, platePaint)

        // Draw scaled original icon centered
        val scale = 0.65f * (style.adjust.scale / 100f)
        val iconSize = (targetSize * scale).toInt().coerceAtLeast(1)
        val offset = (targetSize - iconSize) / 2f
        val scaledIcon = Bitmap.createScaledBitmap(originalAppBmp, iconSize, iconSize, true)

        plateCanvas.drawBitmap(scaledIcon, offset, offset, null)

        return plateBitmap
    }

    fun applyShapeClip(
        source: Bitmap,
        shape: IconShape,
        radiusPct: Float,
        squircleN: Float,
        targetSize: Int
    ): Bitmap {
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val path = createShapePath(shape, radiusPct, squircleN, targetSize)
        canvas.drawPath(path, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val scaledSource = Bitmap.createScaledBitmap(source, targetSize, targetSize, true)
        canvas.drawBitmap(scaledSource, 0f, 0f, paint)

        return output
    }

    fun createShapePath(shape: IconShape, radiusPct: Float, squircleN: Float, size: Int): Path {
        val path = Path()
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        when (shape) {
            IconShape.SQUARE -> {
                path.addRect(rect, Path.Direction.CW)
            }
            IconShape.ROUNDED -> {
                val radius = (size * (radiusPct / 100f)).coerceIn(0f, size / 2f)
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            IconShape.CIRCLE -> {
                path.addOval(rect, Path.Direction.CW)
            }
            IconShape.SQUIRCLE -> {
                val n = squircleN.toDouble().coerceIn(2.0, 6.0)
                val half = size / 2.0
                var first = true
                val steps = 180
                for (i in 0..steps) {
                    val theta = i * 2.0 * Math.PI / steps
                    val cosT = cos(theta)
                    val sinT = sin(theta)
                    val r = half / ((abs(cosT).pow(n) + abs(sinT).pow(n)).pow(1.0 / n))
                    val x = (half + r * cosT).toFloat()
                    val y = (half + r * sinT).toFloat()
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
            }
            IconShape.TEARDROP -> {
                val r = size / 2f
                path.moveTo(r, 0f)
                path.lineTo(size.toFloat(), 0f) // top-right sharp corner
                path.lineTo(size.toFloat(), r)
                path.arcTo(RectF(0f, 0f, size.toFloat(), size.toFloat()), 0f, 270f)
                path.close()
            }
        }
        return path
    }

    fun applyAdjustments(
        source: Bitmap,
        adjust: Adjust,
        colorMode: ColorMode,
        tint: Int?,
        tint2: Int?
    ): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Color Matrix calculations as specified in Section 7 of the brief
        val matrix = createColorMatrix(adjust.hue, adjust.sat, adjust.bri, adjust.con)

        if (adjust.gray || colorMode == ColorMode.MONOCHROME) {
            val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
            matrix.postConcat(grayMatrix)
        }

        if (adjust.invert) {
            val invertMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            matrix.postConcat(invertMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        paint.alpha = ((adjust.opacity / 100f) * 255).toInt().coerceIn(0, 255)

        // Tint / Duotone overlay
        if (colorMode == ColorMode.TINT && tint != null) {
            paint.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
        }

        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun createColorMatrix(hue: Float, sat: Float, bri: Float, con: Float): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(1f + sat / 100f) // sat -100..100

        // rotate around luminance axis by hue degrees
        val r = Math.toRadians(hue.toDouble())
        val c = cos(r).toFloat()
        val s = sin(r).toFloat()
        val h = ColorMatrix(
            floatArrayOf(
                .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
                .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
                .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        m.postConcat(h)

        // contrast & brightness
        val k = 1f + con / 100f
        val t = (bri / 100f) * 255f + (1f - k) * 127.5f
        val bc = ColorMatrix(
            floatArrayOf(
                k, 0f, 0f, 0f, t,
                0f, k, 0f, 0f, t,
                0f, 0f, k, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        m.postConcat(bc)
        return m
    }

    private fun extractDominantColor(bitmap: Bitmap): Int {
        return try {
            val palette = Palette.from(bitmap).generate()
            palette.getVibrantColor(
                palette.getDominantColor(
                    palette.getMutedColor(Color.parseColor("#4A4458"))
                )
            )
        } catch (e: Exception) {
            Color.parseColor("#4A4458")
        }
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }
}
