package com.fman821.darkfusion

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onOpenCalendar: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("dark_fusion_settings", android.content.Context.MODE_PRIVATE)
    }
    val pages = remember(apps) { apps.chunked(12) }
    val pageCount = maxOf(3, pages.size)
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val dockApps = (favoriteApps + apps.filterNot { app ->
        favoriteApps.any { it.packageName == app.packageName }
    }).take(4)
    var clock by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("YOUR PERSONAL UNIVERSE", color = WorkspaceCyan, fontSize = 10.sp,
                    letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("DARK FUSION", color = Color.White, fontSize = 19.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            Text(clock, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
                    Spacer(Modifier.height(14.dp))
                    Text("YOUR SHORTCUTS", color = WorkspaceCyan, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    WorkspaceAppGrid(pageApps.ifEmpty { apps.take(12) }, onLaunchApp)
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
                        WorkspaceAppGrid(pageApps, onLaunchApp)
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xEE11182A),
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
                        Text(app.label.take(1).uppercase(), color = WorkspaceCyan,
                            fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(app.label, color = Color.White, fontSize = 8.sp, maxLines = 1)
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
}

@Composable
private fun WorkspaceAppGrid(
    apps: List<LauncherApp>,
    onLaunchApp: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .clickable { onLaunchApp(app.packageName) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(58.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(
                            WorkspaceViolet.copy(alpha = 0.55f),
                            Color(0xFF17243A), Color(0xFF0A1020)
                        )))
                        .border(1.dp, WorkspaceCyan.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.label.take(1).uppercase(), color = Color.White,
                        fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text(app.label, color = Color(0xFFECEBFA), fontSize = 10.sp,
                    maxLines = 1)
            }
        }
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
