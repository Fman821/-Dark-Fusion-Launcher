package com.fman821.darkfusion

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FusionBackground = Color(0xFF080A12)
private val FusionPanel = Color(0xFF151927)
private val FusionPurple = Color(0xFF9B7BFF)
private val FusionCyan = Color(0xFF52D6D0)

data class LauncherApp(
    val label: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {

    private fun loadApps(): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map {
                LauncherApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = loadApps()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = FusionBackground,
                surface = FusionPanel,
                primary = FusionPurple,
                secondary = FusionCyan
            )) {
                FusionHome(
                    apps = apps,
                    onLaunchApp = { packageName ->
                        packageManager.getLaunchIntentForPackage(packageName)?.let {
                            startActivity(it)
                        }
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
    onLaunchApp: (String) -> Unit,
    onChooseHome: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Home") }

    val filteredApps = apps.filter {
        it.label.contains(search, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FusionBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF17152B), FusionBackground, Color(0xFF080A12))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("DARK FUSION", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("YOUR SPACE. YOUR STYLE.", color = FusionCyan, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Text("✦", color = FusionPurple, fontSize = 34.sp)
            }

            Spacer(Modifier.size(28.dp))

            Text("Make it yours.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("A new home for your Android.", color = Color.LightGray, fontSize = 14.sp)

            Spacer(Modifier.size(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Home", "Apps", "Customize").forEach { tab ->
                    Button(
                        onClick = { selectedTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == tab) FusionPurple else FusionPanel
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(tab, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.size(18.dp))

            when (selectedTab) {
                "Home" -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FusionPanel),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("MONDAY • YOUR DAY", color = FusionCyan, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text("Welcome back", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Explore your apps and make this space yours.", color = Color.LightGray, fontSize = 13.sp)
                            Spacer(Modifier.size(16.dp))
                            Button(onClick = onChooseHome) {
                                Text("Set as default launcher")
                            }
                        }
                    }
                    Spacer(Modifier.size(18.dp))
                    Text("QUICK ACCESS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.size(10.dp))
                    Text("Open the Apps tab to find and launch apps installed on your phone.", color = Color.LightGray, fontSize = 13.sp)
                }

                "Apps" -> {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search apps") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLaunchApp(app.packageName) },
                                colors = CardDefaults.cardColors(containerColor = FusionPanel),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        app.label,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    Text("CUSTOMIZATION", color = FusionCyan, fontSize = 12.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.size(12.dp))
                    listOf(
                        "🎨  Themes & accent colors",
                        "🖼️  Wallpapers",
                        "🔤  Fonts & typography",
                        "▦  Icon layout",
                        "✨  Motion & animations"
                    ).forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = FusionPanel),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(item, color = Color.White, modifier = Modifier.padding(18.dp), fontSize = 14.sp)
                        }
                    }
                    Text("More customization options are coming in the next versions.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}
