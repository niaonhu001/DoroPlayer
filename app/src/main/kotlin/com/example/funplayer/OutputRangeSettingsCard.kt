package com.example.funplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun OutputRangeSettingsCard(
    axisRanges: Map<String, Pair<Float, Float>>,
    onAxisRangesChange: (Map<String, Pair<Float, Float>>) -> Unit
) {
    var outputRangeExpanded by remember { mutableStateOf(true) }
    
    LaunchedEffect(axisRanges) {
        onAxisRangesChange(axisRanges)
    }
    
    SettingsCard(title = stringResource(R.string.output_range_settings)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .let { modifier ->
                    // Add clickable modifier if needed
                    modifier
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.output_range_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            // Add expand/collapse icon here
        }
        AnimatedVisibility(
            visible = outputRangeExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))
            AXIS_NAMES.forEach { axisName ->
                val range = axisRanges[axisName] ?: (0f to 100f)
                var minV by remember(axisName) { mutableStateOf(range.first) }
                var maxV by remember(axisName) { mutableStateOf(range.second) }
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$axisName", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            "${minV.toInt()}% – ${maxV.toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RangeSlider(
                        value = minV..maxV,
                        onValueChange = { r ->
                            minV = r.start
                            maxV = r.endInclusive
                            onAxisRangesChange(axisRanges + (axisName to (minV to maxV)))
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
            }
        }
    }
}