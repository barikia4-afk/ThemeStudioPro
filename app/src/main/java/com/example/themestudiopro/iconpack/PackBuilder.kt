package com.example.themestudiopro.iconpack

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.example.themestudiopro.core.model.IconStyle
import com.example.themestudiopro.core.model.MatchResult
import com.example.themestudiopro.core.model.PackMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PackBuilder {

    suspend fun exportIconsZip(
        ctx: Context,
        matches: List<MatchResult>,
        style: IconStyle,
        packMeta: PackMeta?,
        fileName: String = "ThemeStudio_Icons.zip"
    ): File = withContext(Dispatchers.IO) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val outputFile = File(downloadDir, fileName)

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. Write appfilter.xml
            val appFilterXml = StringBuilder().apply {
                append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                append("<resources>\n")
                for (match in matches) {
                    val safeName = sanitizeDrawableName(match.app.packageName)
                    append("    <item component=\"ComponentInfo{${match.app.componentString}}\" drawable=\"$safeName\"/>\n")
                }
                append("</resources>\n")
            }.toString()

            zos.putNextEntry(ZipEntry("appfilter.xml"))
            zos.write(appFilterXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Render and write PNG for each app (Guaranteed 100% coverage)
            for (match in matches) {
                val bitmap = Renderer.renderIcon(ctx, match, style, packMeta, 192)
                val safeName = sanitizeDrawableName(match.app.packageName)
                val entry = ZipEntry("res/drawable-nodpi-v4/$safeName.png")
                zos.putNextEntry(entry)

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                zos.write(baos.toByteArray())
                zos.closeEntry()
            }
        }

        outputFile
    }

    fun sanitizeDrawableName(pkg: String): String {
        return "icon_" + pkg.replace('.', '_').replace('-', '_').lowercase()
    }
}
