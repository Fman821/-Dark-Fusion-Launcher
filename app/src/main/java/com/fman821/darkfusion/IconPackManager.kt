package com.fman821.darkfusion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val appIcons: Map<String, Int>
)

object IconPackManager {
    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME"
    )

    fun discover(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val packages = linkedSetOf<String>()
        themeActions.forEach { action ->
            val intent = Intent(action)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .forEach { packages.add(it.activityInfo.packageName) }
        }
        return packages.mapNotNull { packageName -> readPack(context, packageName) }
            .sortedBy { it.label.lowercase() }
    }

    private fun readPack(context: Context, packageName: String): IconPackInfo? {
        return runCatching {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val resources = pm.getResourcesForApplication(appInfo)
            val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
            val mappings = mutableMapOf<String, Int>()
            if (xmlId != 0) {
                val parser: XmlResourceParser = resources.getXml(xmlId)
                parser.use {
                    var event = it.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && it.name == "item") {
                            val component = it.getAttributeValue(null, "component").orEmpty()
                            val drawable = it.getAttributeValue(null, "drawable").orEmpty()
                            val packageFromComponent = component
                                .removePrefix("ComponentInfo{")
                                .removeSuffix("}")
                                .substringBefore("/")
                            if (packageFromComponent.isNotBlank() && drawable.isNotBlank()) {
                                val drawableId = resources.getIdentifier(drawable, "drawable", packageName)
                                if (drawableId != 0) mappings[packageFromComponent] = drawableId
                            }
                        }
                        event = it.next()
                    }
                }
            }
            IconPackInfo(packageName, pm.getApplicationLabel(appInfo).toString(), mappings)
        }.getOrNull()
    }
}
