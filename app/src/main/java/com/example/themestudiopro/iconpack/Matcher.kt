package com.example.themestudiopro.iconpack

import android.content.Context
import com.example.themestudiopro.core.model.AppItem
import com.example.themestudiopro.core.model.CoverageStats
import com.example.themestudiopro.core.model.IconOverride
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.Source
import java.util.Locale

object Matcher {

    fun matchAllApps(
        ctx: Context,
        apps: List<AppItem>,
        primaryPack: String?,
        secondaryPacks: List<String> = emptyList(),
        overrides: Map<String, IconOverride> = emptyMap()
    ): List<MatchResult> {
        val results = ArrayList<MatchResult>(apps.size)

        val primaryFilter = primaryPack?.let { PackReader.getOrParseAppFilter(ctx, it) }
        val secondaryFilters = secondaryPacks.map { it to PackReader.getOrParseAppFilter(ctx, it) }

        for (app in apps) {
            val result = matchSingleApp(
                app = app,
                primaryPack = primaryPack,
                primaryFilter = primaryFilter,
                secondaryFilters = secondaryFilters,
                override = overrides[app.componentString]
            )
            results.add(result)
        }

        return results
    }

    fun matchSingleApp(
        app: AppItem,
        primaryPack: String?,
        primaryFilter: ParsedAppFilter?,
        secondaryFilters: List<Pair<String, ParsedAppFilter>>,
        override: IconOverride?
    ): MatchResult {
        // Priority 1: User Override
        if (override != null && override.source.isNotBlank()) {
            return MatchResult(
                app = app,
                resolvedDrawableName = override.source,
                resolvedFromPack = null,
                source = Source.OVERRIDE,
                confidence = 1.0f
            )
        }

        // Priority 2: Primary Pack - Exact Component Match
        if (primaryPack != null && primaryFilter != null) {
            primaryFilter.componentMap[app.componentString]?.let { drawable ->
                return MatchResult(
                    app = app,
                    resolvedDrawableName = drawable,
                    resolvedFromPack = primaryPack,
                    source = Source.PRIMARY,
                    confidence = 1.0f
                )
            }

            // Priority 3: Primary Pack - Package Match
            primaryFilter.packageMap[app.packageName]?.let { drawable ->
                return MatchResult(
                    app = app,
                    resolvedDrawableName = drawable,
                    resolvedFromPack = primaryPack,
                    source = Source.PRIMARY_PKG,
                    confidence = 0.95f
                )
            }
        }

        // Priority 4: Secondary Packs (Exact or Package Match)
        for ((secPkg, secFilter) in secondaryFilters) {
            secFilter.componentMap[app.componentString]?.let { drawable ->
                return MatchResult(
                    app = app,
                    resolvedDrawableName = drawable,
                    resolvedFromPack = secPkg,
                    source = Source.SECONDARY,
                    confidence = 0.9f
                )
            }
            secFilter.packageMap[app.packageName]?.let { drawable ->
                return MatchResult(
                    app = app,
                    resolvedDrawableName = drawable,
                    resolvedFromPack = secPkg,
                    source = Source.SECONDARY,
                    confidence = 0.85f
                )
            }
        }

        // Priority 5: Fuzzy Match on primary pack drawables
        if (primaryPack != null && primaryFilter != null && primaryFilter.allDrawables.isNotEmpty()) {
            val fuzzyDrawable = findFuzzyMatch(app, primaryFilter.allDrawables)
            if (fuzzyDrawable != null) {
                return MatchResult(
                    app = app,
                    resolvedDrawableName = fuzzyDrawable,
                    resolvedFromPack = primaryPack,
                    source = Source.FUZZY,
                    confidence = 0.75f
                )
            }
        }

        // Priority 6: Generated Fallback (Always Succeeds - 100% coverage guarantee)
        return MatchResult(
            app = app,
            resolvedDrawableName = null,
            resolvedFromPack = primaryPack,
            source = Source.FALLBACK,
            confidence = 1.0f
        )
    }

    private fun findFuzzyMatch(app: AppItem, drawables: Set<String>): String? {
        val labelClean = app.label.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
        val pkgEnd = app.packageName.substringAfterLast('.').lowercase(Locale.ROOT)

        for (drawable in drawables) {
            val dClean = drawable.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            if (dClean == labelClean || dClean == pkgEnd) {
                return drawable
            }
            if (labelClean.length >= 4 && dClean.contains(labelClean)) {
                return drawable
            }
            if (pkgEnd.length >= 4 && dClean.contains(pkgEnd)) {
                return drawable
            }
        }
        return null
    }

    fun calculateStats(matches: List<MatchResult>): CoverageStats {
        var fromPack = 0
        var secondary = 0
        var guessed = 0
        var fallback = 0
        var overridden = 0

        for (m in matches) {
            when (m.source) {
                Source.OVERRIDE -> overridden++
                Source.PRIMARY, Source.PRIMARY_PKG -> fromPack++
                Source.SECONDARY -> secondary++
                Source.FUZZY -> guessed++
                Source.FALLBACK -> fallback++
            }
        }

        return CoverageStats(
            totalApps = matches.size,
            fromPack = fromPack,
            secondary = secondary,
            guessed = guessed,
            fallback = fallback,
            overridden = overridden
        )
    }
}
