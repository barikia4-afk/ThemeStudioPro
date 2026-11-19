package com.example.themestudiopro.iconpack

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.concurrent.ConcurrentHashMap

object PackReader {

    private val filterCache = ConcurrentHashMap<String, ParsedAppFilter>()

    fun openAppFilter(ctx: Context, pack: String): XmlPullParser? {
        return try {
            val res = ctx.packageManager.getResourcesForApplication(pack)
            val id = res.getIdentifier("appfilter", "xml", pack)
            if (id != 0) {
                res.getXml(id)
            } else {
                val packContext = ctx.createPackageContext(pack, Context.CONTEXT_IGNORE_SECURITY)
                val am = packContext.assets
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(am.open("appfilter.xml"), "UTF-8")
                }
            }
        } catch (e: Exception) {
            try {
                // Secondary fallback attempt: check res/xml/drawable or assets/drawable.xml
                val packContext = ctx.createPackageContext(pack, Context.CONTEXT_IGNORE_SECURITY)
                val am = packContext.assets
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(am.open("drawable.xml"), "UTF-8")
                }
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getOrParseAppFilter(ctx: Context, pack: String): ParsedAppFilter {
        filterCache[pack]?.let { return it }
        val parser = openAppFilter(ctx, pack)
        val result = if (parser != null) {
            AppFilterParser.parse(parser)
        } else {
            ParsedAppFilter(
                componentMap = emptyMap(),
                packageMap = emptyMap(),
                meta = com.example.themestudiopro.core.model.PackMeta(),
                allDrawables = emptySet()
            )
        }
        filterCache[pack] = result
        return result
    }

    fun loadDrawable(ctx: Context, pack: String, name: String): Drawable? {
        return try {
            val res = ctx.packageManager.getResourcesForApplication(pack)
            var id = res.getIdentifier(name, "drawable", pack)
            if (id == 0) {
                id = res.getIdentifier(name, "mipmap", pack)
            }
            if (id != 0) {
                ResourcesCompat.getDrawable(res, id, null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmap(ctx: Context, pack: String, name: String, targetSize: Int = 192): Bitmap? {
        val drawable = loadDrawable(ctx, pack, name) ?: return null
        return drawableToBitmap(drawable, targetSize)
    }

    fun drawableToBitmap(drawable: Drawable, size: Int = 192): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            if (bmp.width == size && bmp.height == size) {
                return bmp
            }
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun clearCache() {
        filterCache.clear()
    }
}
