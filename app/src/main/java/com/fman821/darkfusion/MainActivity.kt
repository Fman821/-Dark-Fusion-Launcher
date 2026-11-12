package com.fman821.darkfusion

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private val SpaceBlack = Color(0xFF070914)
private val SpacePanel = Color(0xFF11182A)
private val NeonViolet = Color(0xFF9D75FF)
private val NeonCyan = Color(0xFF56E5E0)
private val NeonPink = Color(0xFFFF70C8)

data class LauncherApp(val label: String, val packageName: String)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var deviceTiltX by mutableStateOf(0f)
    private var deviceTiltY by mutableStateOf(0f)
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    override fun onStart() {
        super.onStart()
        if (::appWidgetHost.isInitialized) runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        if (::appWidgetHost.isInitialized) runCatching { appWidgetHost.stopListening() }
        super.onStop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val nextX = (event.values[0] / 6f).coerceIn(-1f, 1f)
        val nextY = (event.values[1] / 6f).coerceIn(-1f, 1f)
        // Quantize updates to keep motion smooth without excessive recomposition.
        deviceTiltX = (nextX * 10).toInt() / 10f
        deviceTiltY = (nextY * 10).toInt() / 10f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onResume() {
        super.onResume()
        if (::sensorManager.isInitialized) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        super.onPause()
    }

    private fun loadApps(): List<LauncherApp> {
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
            .map { LauncherApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        appWidgetHost = AppWidgetHost(this, 1024)
        appWidgetManager = AppWidgetManager.getInstance(this)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val installedApps = loadApps()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = SpaceBlack,
                surface = SpacePanel,
                primary = NeonViolet,
                secondary = NeonCyan
            )) {
                FusionHome(
                    apps = installedApps,
                    tiltX = deviceTiltX,
                    tiltY = deviceTiltY,
                    appWidgetHost = appWidgetHost,
                    appWidgetManager = appWidgetManager,
                    onLaunchApp = { packageName ->
                        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
                    },
                    onChooseHome = {
                        startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
                    }
                )
            }
        }
    }
}

@Composable
private fun FusionHome(
    apps: List<LauncherApp>,
    tiltX: Float,
    tiltY: Float,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    onLaunchApp: (String) -> Unit,
    onChooseHome: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Home") }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("dark_fusion_settings", android.content.Context.MODE_PRIVATE) }
    var iconSize by remember { mutableStateOf(preferences.getInt("icon_size", 76).toFloat()) }
    var gridColumns by remember { mutableStateOf(preferences.getInt("grid_columns", 3)) }
    var floatingMotion by remember { mutableStateOf(preferences.getBoolean("floating_motion", true)) }
    var dockCount by remember { mutableStateOf(preferences.getInt("dock_count", 4)) }
    var dockVisible by remember { mutableStateOf(preferences.getBoolean("dock_visible", true)) }
    var dockOpacity by remember { mutableStateOf(preferences.getFloat("dock_opacity", 0.92f)) }
    var doubleTapAction by remember { mutableStateOf(preferences.getString("gesture_double_tap", "Apps") ?: "Apps") }
    var drawerIconShape by remember { mutableStateOf(preferences.getString("drawer_icon_shape", "Circle") ?: "Circle") }
    var drawerBackground by remember { mutableStateOf(preferences.getString("drawer_background", "Cosmic") ?: "Cosmic") }
    var drawerAnimation by remember { mutableStateOf(preferences.getString("drawer_animation", "Gentle") ?: "Gentle") }
    var homeWidgetIds by remember {
        mutableStateOf(preferences.getString("home_widget_ids", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() })
    }
    var pendingWidgetId by remember { mutableStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }
    fun saveHomeWidget(id: Int) {
        homeWidgetIds = (homeWidgetIds + id).distinct()
        preferences.edit().putString("home_widget_ids", homeWidgetIds.joinToString(",")).apply()
    }
    val widgetConfigureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            saveHomeWidget(pendingWidgetId)
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }
    val widgetPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pickedId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (result.resultCode == android.app.Activity.RESULT_OK && pickedId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            pendingWidgetId = pickedId
            val provider = appWidgetManager.getAppWidgetInfo(pickedId)
            if (provider?.configure != null) {
                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(provider.configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pickedId)
                widgetConfigureLauncher.launch(configureIntent)
            } else {
                saveHomeWidget(pickedId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
        } else if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }
    fun openWidgetPicker() {
        val newId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = newId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
        widgetPickerLauncher.launch(pickIntent)
    }
    fun removeHomeWidget(id: Int) {
        homeWidgetIds = homeWidgetIds.filterNot { it == id }
        preferences.edit().putString("home_widget_ids", homeWidgetIds.joinToString(",")).apply()
        appWidgetHost.deleteAppWidgetId(id)
    }
    var favorites by remember {
        mutableStateOf(preferences.getStringSet("favorite_apps", emptySet())?.toSet() ?: emptySet())
    }
    var hiddenApps by remember {
        mutableStateOf(preferences.getStringSet("hidden_apps", emptySet())?.toSet() ?: emptySet())
    }
    var selectedCategory by remember { mutableStateOf("All") }
    fun categoryFor(app: LauncherApp): String {
        val key = (app.label + " " + app.packageName).lowercase()
        return when {
            listOf("phone", "dialer", "contacts", "message", "sms", "whatsapp", "telegram").any { key.contains(it) } -> "Communication"
            listOf("camera", "gallery", "photo", "video", "music", "youtube", "spotify").any { key.contains(it) } -> "Media"
            listOf("game", "pubg", "freefire", "roblox", "minecraft").any { key.contains(it) } -> "Games"
            listOf("setting", "file", "calculator", "clock", "calendar", "browser").any { key.contains(it) } -> "Tools"
            listOf("facebook", "instagram", "tiktok", "snapchat", "x.com").any { key.contains(it) } -> "Social"
            else -> "Other"
        }
    }
    val categories = listOf("All", "Favorites", "Communication", "Social", "Media", "Games", "Tools", "Other")
    val filteredApps = apps.filter { app ->
        !hiddenApps.contains(app.packageName) && app.label.contains(search, ignoreCase = true) &&
            when (selectedCategory) {
                "All" -> true
                "Favorites" -> favorites.contains(app.packageName)
                else -> categoryFor(app) == selectedCategory
            }
    }
    fun toggleFavorite(packageName: String) {
        val updated = favorites.toMutableSet()
        if (!updated.add(packageName)) updated.remove(packageName)
        favorites = updated
        preferences.edit().putStringSet("favorite_apps", updated).apply()
    }
    fun toggleHidden(packageName: String) {
        val updated = hiddenApps.toMutableSet()
        if (!updated.add(packageName)) updated.remove(packageName)
        hiddenApps = updated
        preferences.edit().putStringSet("hidden_apps", updated).apply()
    }

    Surface(Modifier.fillMaxSize(), color = SpaceBlack) {
        val screenBackground = when (selectedTab) {
            "Apps" -> when (drawerBackground) {
                "Midnight" -> Brush.verticalGradient(listOf(Color(0xFF101827), Color(0xFF070B13), SpaceBlack))
                "Aurora" -> Brush.linearGradient(listOf(Color(0xFF123B45), Color(0xFF17132D), SpaceBlack))
                else -> Brush.verticalGradient(listOf(Color(0xFF17132D), Color(0xFF0B1020), SpaceBlack))
            }
            else -> Brush.verticalGradient(listOf(Color(0xFF17132D), Color(0xFF0B1020), SpaceBlack))
        }
        Box(Modifier.fillMaxSize().background(screenBackground)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 26.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DARK FUSION", color = Color.White, fontSize = 22.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Text("A UNIVERSE OF YOUR OWN", color = NeonCyan, fontSize = 9.sp,
                            letterSpacing = 2.sp)
                    }
                    Text("✧", color = NeonViolet, fontSize = 34.sp)
                }

                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Apps", "Customize").forEach { tab ->
                        Button(
                            onClick = { selectedTab = tab },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == tab) NeonViolet else SpacePanel
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(tab, color = Color.White, fontSize = 11.sp) }
                    }
                }
                Spacer(Modifier.height(18.dp))

                when (selectedTab) {
                    "Home" -> {
                        ProHomeWorkspace(
                            apps = apps,
                            onLaunchApp = onLaunchApp,
                            onChooseHome = onChooseHome,
                            onOpenApps = { selectedTab = "Apps" },
                            favoriteApps = apps.filter { favorites.contains(it.packageName) && !hiddenApps.contains(it.packageName) },
                            onOpenSettings = {
                                runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
                            },
                            onOpenCalendar = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR))
                                }
                            },
                            dockCount = dockCount,
                            dockVisible = dockVisible,
                            dockOpacity = dockOpacity,
                            appWidgetHost = appWidgetHost,
                            appWidgetManager = appWidgetManager,
                            widgetIds = homeWidgetIds,
                            onAddWidget = ::openWidgetPicker,
                            onRemoveWidget = ::removeHomeWidget,
                            onDoubleTapHome = {
                                when (doubleTapAction) {
                                    "Customize" -> selectedTab = "Customize"
                                    "Home" -> selectedTab = "Home"
                                    else -> selectedTab = "Apps"
                                }
                            },
                            onSwipeUpHome = { selectedTab = "Apps" },
                            onSwipeDownHome = { selectedTab = "Customize" }
                        )
                    }
                    "Apps" -> {
                        Text("YOUR UNIVERSE", color = Color.White, fontSize = 25.sp,
                            fontWeight = FontWeight.Bold)
                        Text("${filteredApps.size} applications • FLOATING MODE",
                            color = NeonCyan, fontSize = 10.sp, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Search your universe") },
                            shape = RoundedCornerShape(18.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            categories.forEach { category ->
                                Button(
                                    onClick = { selectedCategory = category },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedCategory == category) NeonViolet else SpacePanel
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(category, color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                        Text(
                            if (selectedCategory == "Favorites") "SAVED FAVORITES" else "FLOATING APP FIELD",
                            color = NeonPink, fontSize = 10.sp, letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                                FloatingAppIcon(
                                    app = app,
                                    index = index,
                                    iconSize = iconSize.dp,
                                    floatingMotion = floatingMotion,
                                    iconShape = drawerIconShape,
                                    animationStyle = drawerAnimation,
                                    tiltX = tiltX,
                                    tiltY = tiltY,
                                    isFavorite = favorites.contains(app.packageName),
                                    onToggleFavorite = { toggleFavorite(app.packageName) },
                                    onClick = { onLaunchApp(app.packageName) }
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("SHAPE YOUR UNIVERSE", color = NeonCyan, fontSize = 11.sp,
                                letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Text("Personalize the floating app drawer. Your choices are saved on this phone.",
                                color = Color.LightGray, fontSize = 12.sp)

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.4f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("ICON SIZE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${iconSize.toInt()} dp", color = NeonCyan, fontSize = 12.sp)
                                    Slider(
                                        value = iconSize,
                                        onValueChange = {
                                            iconSize = it
                                            preferences.edit().putInt("icon_size", it.toInt()).apply()
                                        },
                                        valueRange = 64f..96f
                                    )
                                    Text("Adjust the size of floating app icons.", color = Color.LightGray, fontSize = 11.sp)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("APP GRID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Choose how many icons appear in each row.", color = Color.LightGray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        (3..5).forEach { count ->
                                            Button(
                                                onClick = {
                                                    gridColumns = count
                                                    preferences.edit().putInt("grid_columns", count).apply()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (gridColumns == count) NeonViolet else Color(0xFF273149)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("${count} columns", fontSize = 10.sp) }
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("APP DRAWER APPEARANCE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Personalize the floating universe.", color = Color.LightGray, fontSize = 11.sp)
                                    Text("ICON SHAPE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Circle", "Rounded", "Square").forEach { shape ->
                                            Button(onClick = {
                                                drawerIconShape = shape
                                                preferences.edit().putString("drawer_icon_shape", shape).apply()
                                            }, colors = ButtonDefaults.buttonColors(containerColor = if (drawerIconShape == shape) NeonViolet else Color(0xFF273149)), shape = RoundedCornerShape(12.dp)) {
                                                Text(shape, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Text("BACKGROUND", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Cosmic", "Midnight", "Aurora").forEach { style ->
                                            Button(onClick = {
                                                drawerBackground = style
                                                preferences.edit().putString("drawer_background", style).apply()
                                            }, colors = ButtonDefaults.buttonColors(containerColor = if (drawerBackground == style) NeonViolet else Color(0xFF273149)), shape = RoundedCornerShape(12.dp)) {
                                                Text(style, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Text("ICON ANIMATION", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Off", "Gentle", "Dynamic").forEach { style ->
                                            Button(onClick = {
                                                drawerAnimation = style
                                                preferences.edit().putString("drawer_animation", style).apply()
                                            }, colors = ButtonDefaults.buttonColors(containerColor = if (drawerAnimation == style) NeonViolet else Color(0xFF273149)), shape = RoundedCornerShape(12.dp)) {
                                                Text(style, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                    Text("Gentle is recommended for smoother performance on older phones.", color = Color.LightGray, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("CUSTOM DOCK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Choose how many shortcuts appear at the bottom of Home.", color = Color.LightGray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        (3..6).forEach { count ->
                                            Button(
                                                onClick = {
                                                    dockCount = count
                                                    preferences.edit().putInt("dock_count", count).apply()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (dockCount == count) NeonViolet else Color(0xFF273149)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text("$count", fontSize = 11.sp) }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                dockVisible = !dockVisible
                                                preferences.edit().putBoolean("dock_visible", dockVisible).apply()
                                            }.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Show Home Dock", color = Color.White, fontSize = 12.sp)
                                        Text(if (dockVisible) "ON" else "OFF",
                                            color = if (dockVisible) NeonCyan else NeonPink,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    Text("DOCK TRANSPARENCY  ${(dockOpacity * 100).toInt()}%",
                                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Slider(
                                        value = dockOpacity,
                                        onValueChange = {
                                            dockOpacity = it
                                            preferences.edit().putFloat("dock_opacity", it).apply()
                                        },
                                        valueRange = 0.35f..1f
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("HOME GESTURES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Choose what a double tap on an empty area of Home does.", color = Color.LightGray, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Apps", "Customize", "Home").forEach { action ->
                                            Button(
                                                onClick = {
                                                    doubleTapAction = action
                                                    preferences.edit().putString("gesture_double_tap", action).apply()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (doubleTapAction == action) NeonViolet else Color(0xFF273149)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) { Text(action, fontSize = 10.sp) }
                                        }
                                    }
                                    Text("Double-tap the Home workspace to run this action.", color = NeonCyan, fontSize = 10.sp)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        floatingMotion = !floatingMotion
                                        preferences.edit().putBoolean("floating_motion", floatingMotion).apply()
                                    }.padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("FLOATING LAYOUT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Enable the tilted, raised icon arrangement.", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    Text(if (floatingMotion) "ON" else "OFF",
                                        color = if (floatingMotion) NeonCyan else Color.LightGray,
                                        fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SpacePanel,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.35f))
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("HIDDEN APPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Tap an app to hide it from the App Drawer. Tap again to show it.", color = Color.LightGray, fontSize = 11.sp)
                                    apps.forEach { app ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                .clickable { toggleHidden(app.packageName) }
                                                .padding(vertical = 7.dp, horizontal = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(app.label, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(
                                                if (hiddenApps.contains(app.packageName)) "HIDDEN" else "VISIBLE",
                                                color = if (hiddenApps.contains(app.packageName)) NeonPink else NeonCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    iconSize = 76f
                                    gridColumns = 3
                                    floatingMotion = true
                                    dockCount = 4
                                    dockVisible = true
                                    dockOpacity = 0.92f
                                    hiddenApps = emptySet()
                                    favorites = emptySet()
                                    preferences.edit().clear().apply()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF273149)),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Restore default settings") }
                            Text("Settings are stored locally and remain after closing the app.",
                                color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    apps: List<LauncherApp>,
    onLaunchApp: (String) -> Unit,
    favoriteApps: List<LauncherApp>,
    onChooseHome: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    var clock by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var date by remember { mutableStateOf(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("YOUR PERSONAL UNIVERSE", color = NeonCyan, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF17152D),
            shape = RoundedCornerShape(30.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x669D75FF))
        ) {
            Box(
                Modifier.background(
                    Brush.linearGradient(listOf(Color(0xFF34245C), Color(0xFF132B43), Color(0xFF11182A)))
                ).padding(22.dp)
            ) {
                Column {
                    Text("FUSION TIME", color = NeonCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(clock, color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp)
                    Text(date, color = Color(0xFFD7D3EE), fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    Text("A universe made yours.", color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
                }
                Text("✧", modifier = Modifier.align(Alignment.TopEnd), color = NeonPink, fontSize = 34.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeInfoCard(Modifier.weight(1f), "☁", "WEATHER SPACE", "Live weather coming soon",
                NeonCyan, null)
            HomeInfoCard(Modifier.weight(1f), "▦", "CALENDAR", "Open your calendar",
                NeonPink, onOpenCalendar)
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("FAVORITE ORBIT", color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("Your quick-launch apps", color = Color.LightGray, fontSize = 11.sp)
            }
            Text("SEE ALL  ›", color = NeonCyan, fontSize = 10.sp,
                modifier = Modifier.clickable(onClick = onOpenApps))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            (if (favoriteApps.isNotEmpty()) favoriteApps else apps).take(4).forEach { app ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { onLaunchApp(app.packageName) },
                    color = SpacePanel,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4456E5E0))
                ) {
                    Column(Modifier.padding(vertical = 13.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(app.label.take(1).uppercase(), color = NeonCyan,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(app.label, color = Color.White, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
            if (apps.isEmpty()) {
                Text("Your apps will appear here.", color = Color.LightGray, fontSize = 12.sp)
            }
        }

        Text("QUICK ACCESS", color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeInfoCard(Modifier.weight(1f), "⌕", "APP SEARCH", "Find an app",
                NeonViolet, onOpenApps)
            HomeInfoCard(Modifier.weight(1f), "⚙", "SYSTEM", "Phone settings",
                NeonCyan, onOpenSettings)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SpacePanel,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3356E5E0))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("WEATHER WIDGET", color = NeonCyan, fontSize = 10.sp,
                    letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Your weather panel is ready for live weather connection.",
                    color = Color.White, fontSize = 13.sp)
                Text("No location is accessed unless you choose to enable it.",
                    color = Color.LightGray, fontSize = 10.sp)
            }
        }

        Button(
            onClick = onChooseHome,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Set Dark Fusion as default launcher")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeInfoCard(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    detail: String,
    accent: Color,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = if (onClick == null) modifier else modifier.clickable { onClick() },
        color = SpacePanel,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(symbol, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(detail, color = Color.LightGray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FloatingAppIcon(
    app: LauncherApp,
    index: Int,
    iconSize: androidx.compose.ui.unit.Dp,
    floatingMotion: Boolean,
    iconShape: String,
    animationStyle: String,
    tiltX: Float,
    tiltY: Float,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(140),
        label = "iconPressScale"
    )
    val floatTransition = rememberInfiniteTransition(label = "floatingIcon")
    val bobAmount by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1900 + (index % 5) * 180,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconBob"
    )
    val iconBitmap = remember(app.packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(app.packageName)
            val bitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }.getOrNull()
    }
    val accent = when (index % 3) {
        0 -> NeonViolet
        1 -> NeonCyan
        else -> NeonPink
    }
    val tilt = if (floatingMotion) when (index % 4) {
        0 -> -5f
        1 -> 4f
        2 -> 2f
        else -> -3f
    } else 0f
    val lift = if (floatingMotion && index % 2 != 0) 9.dp else 0.dp
    val shape = when (iconShape) {
        "Rounded" -> RoundedCornerShape(22.dp)
        "Square" -> RoundedCornerShape(8.dp)
        else -> CircleShape
    }
    val animationAmount = when (animationStyle) {
        "Off" -> 0f
        "Dynamic" -> 9f
        else -> 3f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = lift, bottom = 4.dp)
            .graphicsLayer { rotationZ = tilt },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .shadow(18.dp, shape, ambientColor = accent.copy(alpha = 0.42f),
                    spotColor = accent.copy(alpha = 0.55f))
                .clip(shape)
                .background(Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.38f), Color(0xFF17243A), Color(0xFF0A1020))
                ))
                .border(1.dp, Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.8f), accent.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.12f))
                ), shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .graphicsLayer {
                    val motion = if (floatingMotion) 1f else 0f
                    rotationX = tiltY * 1.4f * motion
                    rotationY = -tiltX * 1.4f * motion
                    scaleX = pressScale
                    scaleY = pressScale
                    translationY = if (animationAmount > 0f) sin(bobAmount * PI).toFloat() * animationAmount else 0f
                    cameraDistance = 26f * density
                    transformOrigin = TransformOrigin.Center
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isFavorite) "★" else "☆",
                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                    .clip(CircleShape).background(Color(0xDD080D1B))
                    .clickable(onClick = onToggleFavorite).padding(horizontal = 6.dp, vertical = 2.dp),
                color = if (isFavorite) NeonPink else Color.White,
                fontSize = 14.sp
            )
            Box(
                Modifier.size(iconSize * 0.76f).clip(CircleShape)
                    .background(Color(0xAA080D1B)),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(bitmap = iconBitmap, contentDescription = app.label,
                        modifier = Modifier.size(iconSize * 0.51f))
                } else {
                    Text(app.label.take(1).uppercase(), color = Color.White,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(app.label, color = Color(0xFFECEBFA), fontSize = 11.sp,
            maxLines = 1, fontWeight = FontWeight.Medium)
    }
}
