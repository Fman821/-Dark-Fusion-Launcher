package com.fman821.darkfusion

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WorkspaceBlack = Color(0xFF070914)
private val WorkspacePanel = Color(0xFF11182A)
private val WorkspaceViolet = Color(0xFF9D75FF)
private val WorkspaceCyan = Color(0xFF56E5E0)
private val WorkspacePink = Color(0xFFFF70C8)

@Composable
fun ProHomeWorkspace(
    apps: List<LauncherApp>,
    favoriteApps: List<LauncherApp>,
    onLaunchApp: (String) -> Unit,
    onChooseHome: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    dockCount: Int,
    dockVisible: Boolean,
    dockOpacity: Float,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    widgetIds: List<Int>,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onMoveWidget: (Int, Int) -> Unit,
    onDoubleTapHome: () -> Unit,
    onSwipeUpHome: () -> Unit = {},
    onSwipeDownHome: () -> Unit = {},
    iconPackPackage: String? = null,
    iconPackIcons: Map<String, Int> = emptyMap(),
    showAppLabels: Boolean = true,
    gridColumns: Int = 3,
    homeIconSize: Float = 58f,
    homeGridSpacing: Float = 12f,
    widgetWidth: Float = 280f,
    widgetHeight: Float = 150f,
    showWidgetShelf: Boolean = true,
    homePageCount: Int = 3
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("dark_fusion_settings", android.content.Context.MODE_PRIVATE)
    }
    var orderedApps by remember(apps) {
        val saved = preferences.getString("home_app_order", null)
            ?.split("|")?.filter { it.isNotBlank() }.orEmpty()
        val byPackage = apps.associateBy { it.packageName }
        val restored = saved.mapNotNull { byPackage[it] }
        mutableStateOf(restored + apps.filterNot { saved.contains(it.packageName) })
    }
    val pages = remember(orderedApps) { orderedApps.chunked(12) }
    val pageCount = maxOf(homePageCount.coerceIn(3, 7), pages.size)
    fun moveHomeApp(fromPackage: String, toPackage: String) {
        val updated = orderedApps.toMutableList()
        val from = updated.indexOfFirst { it.packageName == fromPackage }
        val to = updated.indexOfFirst { it.packageName == toPackage }
        if (from >= 0 && to >= 0 && from != to) {
            val moved = updated.removeAt(from)
            updated.add(to, moved)
            orderedApps = updated
            preferences.edit().putString("home_app_order",
                updated.joinToString("|") { it.packageName }).apply()
        }
    }
    var homeFolders by remember {
        val raw = preferences.getString("home_app_folders", "").orEmpty()
        val parsed = raw.split(";").filter { it.contains("=") }.associate { entry ->
            val parts = entry.split("=", limit = 2)
            parts[0] to parts[1].split(",").filter { it.isNotBlank() }
        }
        mutableStateOf(parsed)
    }
    var showFolderEditor by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var selectedFolderApps by remember { mutableStateOf(setOf<String>()) }
    var openedFolder by remember { mutableStateOf<String?>(null) }
    fun saveHomeFolders(updated: Map<String, List<String>>) {
        homeFolders = updated
        val encoded = updated.entries.joinToString(";") { (name, packages) ->
            "$name=${packages.joinToString(",")}"
        }
        preferences.edit().putString("home_app_folders", encoded).apply()
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val dockApps = (favoriteApps + apps.filterNot { app ->
        favoriteApps.any { it.packageName == app.packageName }
    }).take(dockCount.coerceIn(3, 6))
    var clock by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()))
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
            kotlinx.coroutines.delay(30_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().pointerInput(onDoubleTapHome) {
            detectTapGestures(onDoubleTap = { onDoubleTapHome() })
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().pointerInput(onSwipeUpHome, onSwipeDownHome) {
                var verticalDistance = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        verticalDistance += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            verticalDistance < -60f -> onSwipeUpHome()
                            verticalDistance > 60f -> onSwipeDownHome()
                        }
                        verticalDistance = 0f
                    },
                    onDragCancel = { verticalDistance = 0f }
                )
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("YOUR PERSONAL UNIVERSE", color = WorkspaceCyan, fontSize = 10.sp,
                    letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("DARK FUSION", color = Color.White, fontSize = 19.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(clock, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(currentDate, color = WorkspaceCyan, fontSize = 10.sp,
                    fontWeight = FontWeight.Medium)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WorkspacePanel,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, WorkspaceViolet.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("WELCOME TO YOUR SPACE", color = WorkspaceCyan,
                        fontSize = 9.sp, letterSpacing = 1.2.sp)
                    Text("Swipe to explore your home pages", color = Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("✧", color = WorkspacePink, fontSize = 28.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HOME PAGES", color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("${pagerState.currentPage + 1} / $pageCount",
                color = WorkspaceCyan, fontSize = 11.sp)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { page -> page }
        ) { page ->
            val pageApps = pages.getOrNull(page).orEmpty()
            if (page == 0) {
                Column(Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkspaceQuickTile("SEARCH", "Find apps", WorkspaceViolet, onOpenApps,
                            Modifier.weight(1f))
                        WorkspaceQuickTile("CALENDAR", "Open calendar", WorkspacePink, onOpenCalendar,
                            Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkspaceBottomAction("＋ NEW FOLDER", {
                            folderName = ""
                            selectedFolderApps = emptySet()
                            showFolderEditor = true
                        }, Modifier.weight(1f))
                        homeFolders.keys.forEach { name ->
                            WorkspaceBottomAction("▣ $name", { openedFolder = name },
                                Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (showWidgetShelf) {
                        WidgetShelf(
                            host = appWidgetHost,
                            manager = appWidgetManager,
                            widgetIds = widgetIds,
                            onAddWidget = onAddWidget,
                            onRemoveWidget = onRemoveWidget,
                            onMoveWidget = onMoveWidget,
                            widgetWidth = widgetWidth,
                            widgetHeight = widgetHeight
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Text("YOUR SHORTCUTS", color = WorkspaceCyan, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    WorkspaceAppGrid(pageApps.ifEmpty { orderedApps.take(12) }, onLaunchApp, ::moveHomeApp, iconPackPackage, iconPackIcons, showAppLabels, gridColumns, homeIconSize, homeGridSpacing)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        if (page < pages.size) "APP SPACE ${page + 1}" else "EMPTY SPACE",
                        color = WorkspaceCyan, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    if (pageApps.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Your next home page is ready.", color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        WorkspaceAppGrid(pageApps, onLaunchApp, ::moveHomeApp, iconPackPackage, iconPackIcons, showAppLabels, gridColumns, homeIconSize, homeGridSpacing)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { page ->
                Box(
                    Modifier.padding(horizontal = 4.dp).size(if (pagerState.currentPage == page) 9.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == page) WorkspaceCyan else Color(0xFF596078))
                )
            }
        }

        if (dockVisible) Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF11182A).copy(alpha = dockOpacity.coerceIn(0.25f, 1f)),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, WorkspaceCyan.copy(alpha = 0.38f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockApps.forEach { app ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .clickable { onLaunchApp(app.packageName) }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WorkspaceIconImage(
                            packageName = app.packageName,
                            iconPackPackage = iconPackPackage,
                            iconPackIconId = iconPackIcons[app.packageName],
                            size = (homeIconSize * 0.52f).toInt().coerceIn(22, 40)
                        )
                        if (showAppLabels) Text(app.label, color = Color.White, fontSize = 8.sp, maxLines = 1)
                    }
                }
                if (dockApps.isEmpty()) {
                    Text("Dock", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkspaceBottomAction("APPS", onOpenApps, Modifier.weight(1f))
            WorkspaceBottomAction("SETTINGS", onOpenSettings, Modifier.weight(1f))
            WorkspaceBottomAction("SET HOME", onChooseHome, Modifier.weight(1f))
        }
    }

    if (showFolderEditor) {
        AlertDialog(
            onDismissRequest = { showFolderEditor = false },
            title = { Text("Create home folder", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.take(20).filter { ch -> ch != ';' && ch != '=' } },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Choose apps", color = WorkspaceCyan, fontSize = 12.sp)
                    LazyColumn(Modifier.height(220.dp)) {
                        items(orderedApps, key = { it.packageName }) { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedFolderApps.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        selectedFolderApps = if (checked)
                                            selectedFolderApps + app.packageName
                                        else selectedFolderApps - app.packageName
                                    }
                                )
                                Text(app.label, color = Color.White, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = folderName.trim()
                    if (name.isNotEmpty() && selectedFolderApps.isNotEmpty()) {
                        saveHomeFolders(homeFolders + (name to selectedFolderApps.toList()))
                        showFolderEditor = false
                    }
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showFolderEditor = false }) { Text("CANCEL") }
            },
            containerColor = WorkspacePanel
        )
    }

    openedFolder?.let { name ->
        val folderApps = homeFolders[name].orEmpty().mapNotNull { pkg ->
            orderedApps.firstOrNull { it.packageName == pkg }
        }
        AlertDialog(
            onDismissRequest = { openedFolder = null },
            title = { Text(name, color = Color.White) },
            text = {
                Column {
                    folderApps.forEach { app ->
                        TextButton(onClick = {
                            openedFolder = null
                            onLaunchApp(app.packageName)
                        }) { Text(app.label, color = WorkspaceCyan) }
                    }
                    if (folderApps.isEmpty()) Text("No apps in this folder", color = Color.LightGray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    saveHomeFolders(homeFolders - name)
                    openedFolder = null
                }) { Text("DELETE FOLDER") }
            },
            dismissButton = {
                TextButton(onClick = { openedFolder = null }) { Text("CLOSE") }
            },
            containerColor = WorkspacePanel
        )
    }
}

@Composable
private fun WorkspaceAppGrid(
    apps: List<LauncherApp>,
    onLaunchApp: (String) -> Unit,
    onMoveApp: (String, String) -> Unit,
    iconPackPackage: String?,
    iconPackIcons: Map<String, Int>,
    showAppLabels: Boolean,
    gridColumns: Int,
    homeIconSize: Float,
    homeGridSpacing: Float
) {
    val gridState = rememberLazyGridState()
    var draggingPackage by remember { mutableStateOf<String?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns.coerceIn(3, 5)),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(homeGridSpacing.coerceIn(4f, 20f).dp),
        horizontalArrangement = Arrangement.spacedBy(homeGridSpacing.coerceIn(4f, 20f).dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            val isDragging = draggingPackage == app.packageName
            Column(
                modifier = Modifier.fillMaxWidth()
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragDelta.x
                            translationY = dragDelta.y
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(app.packageName, apps) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localStart ->
                                draggingPackage = app.packageName
                                dragOrigin = localStart
                                dragDelta = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += amount
                                val current = gridState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == draggingPackage }
                                if (current != null) {
                                    val center = Offset(
                                        current.offset.x + dragOrigin.x + dragDelta.x,
                                        current.offset.y + dragOrigin.y + dragDelta.y
                                    )
                                    val target = gridState.layoutInfo.visibleItemsInfo
                                        .filter { it.key != draggingPackage }
                                        .minByOrNull { item ->
                                            val itemCenter = Offset(
                                                item.offset.x + item.size.width / 2f,
                                                item.offset.y + item.size.height / 2f
                                            )
                                            (itemCenter - center).getDistance()
                                        }
                                    target?.key?.toString()?.let { targetPackage ->
                                        if (targetPackage != draggingPackage) {
                                            draggingPackage?.let { onMoveApp(it, targetPackage) }
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingPackage = null
                                dragDelta = Offset.Zero
                            },
                            onDragCancel = {
                                draggingPackage = null
                                dragDelta = Offset.Zero
                            }
                        )
                    }
                    .clickable(enabled = !isDragging) { onLaunchApp(app.packageName) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(homeIconSize.coerceIn(44f, 76f).dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(
                            WorkspaceViolet.copy(alpha = 0.55f),
                            Color(0xFF17243A), Color(0xFF0A1020)
                        )))
                        .border(1.dp, if (isDragging) WorkspaceCyan else WorkspaceCyan.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    WorkspaceIconImage(
                        packageName = app.packageName,
                        iconPackPackage = iconPackPackage,
                        iconPackIconId = iconPackIcons[app.packageName],
                        size = (homeIconSize * 0.69f).toInt().coerceIn(30, 52)
                    )
                }
                if (showAppLabels) {
                    Spacer(Modifier.height(5.dp))
                    Text(app.label, color = Color(0xFFECEBFA), fontSize = 10.sp,
                        maxLines = 1)
                }
                if (isDragging) {
                    Text("MOVING", color = WorkspaceCyan, fontSize = 8.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceIconImage(
    packageName: String,
    iconPackPackage: String?,
    iconPackIconId: Int?,
    size: Int
) {
    val context = LocalContext.current
    val bitmap = remember(packageName, iconPackPackage, iconPackIconId) {
        runCatching {
            val drawable = if (iconPackPackage != null && iconPackIconId != null) {
                context.packageManager.getResourcesForApplication(iconPackPackage)
                    .getDrawable(iconPackIconId, null)
            } else {
                context.packageManager.getApplicationIcon(packageName)
            }
            val image = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            image.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = packageName, modifier = Modifier.size(size.dp))
    } else {
        Text(packageName.substringAfterLast('.').take(1).uppercase(),
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkspaceQuickTile(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = WorkspacePanel,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun WorkspaceBottomAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = WorkspacePanel,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
