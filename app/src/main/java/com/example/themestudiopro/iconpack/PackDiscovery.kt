package com.example.themestudiopro.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.themestudiopro.core.model.AppItem
import com.example.themestudiopro.core.model.PackInfo
import java.util.Locale

object PackDiscovery {

    val ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.dlto.atom.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "org.adw.launcher.icons.ACTION_PICK_ICON"
    )

    fun discoverPacks(ctx: Context, primaryPackPkg: String? = null): List<PackInfo> {
        val pm = ctx.packageManager
        val found = LinkedHashMap<String, PackInfo>()
        val installedApps = getInstalledApps(ctx)

        for (action in ACTIONS) {
            val intent = Intent(action)
            val activities = try {
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }

            for (ri in activities) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName) continue // Skip our own app

                if (!found.containsKey(pkg)) {
                    val label = try {
                        ri.loadLabel(pm).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                    val icon = try {
                        ri.loadIcon(pm)
                    } catch (e: Exception) {
                        null
                    }

                    // Parse filter to calculate coverage & previews
                    val filter = PackReader.getOrParseAppFilter(ctx, pkg)
                    val iconCount = filter.allDrawables.size

                    var matchesCount = 0
                    for (app in installedApps) {
                        if (filter.componentMap.containsKey(app.componentString) ||
                            filter.packageMap.containsKey(app.packageName)
                        ) {
                            matchesCount++
                        }
                    }

                    val coveragePct = if (installedApps.isNotEmpty()) {
                        (matchesCount * 100) / installedApps.size
                    } else 0

                    val previews = filter.allDrawables.take(16)

                    found[pkg] = PackInfo(
                        pkg = pkg,
                        label = label,
                        icon = icon,
                        iconCount = iconCount,
                        coveragePct = coveragePct,
                        isPrimary = (pkg == primaryPackPkg),
                        previewDrawables = previews
                    )
                }
            }
        }

        return found.values.sortedWith(
            compareByDescending<PackInfo> { it.isPrimary }
                .thenByDescending { it.coveragePct }
                .thenBy { it.label.lowercase(Locale.ROOT) }
        )
    }

    fun getInstalledApps(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = try {
            pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val apps = ArrayList<AppItem>()
        for (ri in resolved) {
            val pkg = ri.activityInfo.packageName
            val cls = ri.activityInfo.name
            if (pkg == ctx.packageName) continue

            val label = try {
                ri.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }

            val icon = try {
                ri.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            apps.add(
                AppItem(
                    packageName = pkg,
                    activityName = cls,
                    label = label,
                    componentString = "$pkg/$cls",
                    originalIcon = icon
                )
            )
        }

        return apps.sortedBy { it.label.lowercase(Locale.ROOT) }
    }
}
