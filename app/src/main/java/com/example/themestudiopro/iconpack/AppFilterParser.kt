package com.example.themestudiopro.iconpack

import com.example.themestudiopro.core.model.PackMeta
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

data class ParsedAppFilter(
    val componentMap: Map<String, String>, // "pkg/activity" -> drawable
    val packageMap: Map<String, String>,   // "pkg" -> drawable (first item seen)
    val meta: PackMeta,
    val allDrawables: Set<String>
)

object AppFilterParser {

    fun parse(parser: XmlPullParser): ParsedAppFilter {
        val componentMap = HashMap<String, String>()
        val packageMap = HashMap<String, String>()
        val allDrawables = HashSet<String>()
        val iconBacks = ArrayList<String>()
        var iconUpon: String? = null
        var iconMask: String? = null
        var scale = 1.0f

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.lowercase(Locale.ROOT)
                    when (tagName) {
                        "item" -> {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                                val normalizedComponent = normalizeComponent(component)
                                val pkgName = normalizedComponent.substringBefore('/')
                                componentMap[normalizedComponent] = drawable
                                if (!packageMap.containsKey(pkgName)) {
                                    packageMap[pkgName] = drawable
                                }
                                allDrawables.add(drawable)
                            }
                        }
                        "iconback" -> {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i).lowercase(Locale.ROOT)
                                if (attrName.startsWith("img")) {
                                    val valStr = parser.getAttributeValue(i)
                                    if (!valStr.isNullOrBlank()) {
                                        iconBacks.add(valStr)
                                        allDrawables.add(valStr)
                                    }
                                }
                            }
                        }
                        "iconupon" -> {
                            val uponVal = parser.getAttributeValue(null, "img1")
                                ?: parser.getAttributeValue(null, "img")
                            if (!uponVal.isNullOrBlank()) {
                                iconUpon = uponVal
                                allDrawables.add(uponVal)
                            }
                        }
                        "iconmask" -> {
                            val maskVal = parser.getAttributeValue(null, "img1")
                                ?: parser.getAttributeValue(null, "img")
                            if (!maskVal.isNullOrBlank()) {
                                iconMask = maskVal
                                allDrawables.add(maskVal)
                            }
                        }
                        "scale" -> {
                            val factor = parser.getAttributeValue(null, "factor")
                            if (!factor.isNullOrBlank()) {
                                factor.toFloatOrNull()?.let { scale = it }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedAppFilter(
            componentMap = componentMap,
            packageMap = packageMap,
            meta = PackMeta(
                back = iconBacks,
                upon = iconUpon,
                mask = iconMask,
                scale = scale
            ),
            allDrawables = allDrawables
        )
    }

    fun normalizeComponent(componentStr: String): String {
        var str = componentStr.trim()
        if (str.startsWith("ComponentInfo{", ignoreCase = true)) {
            str = str.substring("ComponentInfo{".length)
        }
        if (str.endsWith("}")) {
            str = str.substring(0, str.length - 1)
        }
        return str.trim()
    }
}
