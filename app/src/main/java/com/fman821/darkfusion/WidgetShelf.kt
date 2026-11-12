package com.fman821.darkfusion

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WidgetShelf(
    host: AppWidgetHost,
    manager: AppWidgetManager,
    widgetIds: List<Int>,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF10182A),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x669D75FF))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WIDGET SPACE", color = Color(0xFF56E5E0), fontSize = 10.sp)
                Button(
                    onClick = onAddWidget,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6546B8)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("+ ADD", fontSize = 9.sp) }
            }
            if (widgetIds.isEmpty()) {
                Text("Add clock, calendar, weather and other Android widgets.",
                    color = Color.LightGray, fontSize = 10.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    widgetIds.forEach { id ->
                        val info = manager.getAppWidgetInfo(id)
                        if (info != null) {
                            Column(
                                modifier = Modifier.padding(bottom = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AndroidView(
                                    factory = { context -> host.createView(context, id, info) },
                                    modifier = Modifier.width(280.dp).height(150.dp)
                                )
                                Button(
                                    onClick = { onRemoveWidget(id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34213B)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) { Text("Remove widget", fontSize = 9.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
