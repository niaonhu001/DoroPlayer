package com.example.funplayer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
internal fun DeviceControlSettingsCard(
    onSendCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var deviceControlExpanded by remember { mutableStateOf(true) }
    var manualAxisPositions by remember { mutableStateOf(AXIS_NAMES.associateWith { 50 }) }
    var sliderSendJob by remember { mutableStateOf<Job?>(null) }
    var selectedScriptUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedScriptName by remember { mutableStateOf<String?>(null) }
    var standaloneScriptData by remember { mutableStateOf<FunscriptData?>(null) }
    var scriptPlayPositionMs by remember { mutableStateOf(0L) }
    var scriptPlaying by remember { mutableStateOf(false) }
    var scriptPlayJob by remember { mutableStateOf<Job?>(null) }
    
    val scriptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            selectedScriptUris = uris.map { it.toString() }
            selectedScriptName = if (uris.size == 1) {
                DocumentFile.fromSingleUri(context, uris[0])?.name ?: uris[0].lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.unknown)
            } else {
                context.getString(R.string.device_control_multi_script_format, uris.size)
            }
            standaloneScriptData = null
            scriptPlayPositionMs = 0L
        }
    }
    
    LaunchedEffect(selectedScriptUris) {
        if (selectedScriptUris.isEmpty()) {
            standaloneScriptData = null
            scriptPlayPositionMs = 0L
            return@LaunchedEffect
        }
        val data = if (selectedScriptUris.size == 1) {
            withContext(Dispatchers.IO) { loadFunscriptFromUri(context, selectedScriptUris[0]) }
        } else {
            val urisByAxis = mutableMapOf<String, String>()
            for (uriStr in selectedScriptUris) {
                val name = DocumentFile.fromSingleUri(context, Uri.parse(uriStr))?.name ?: uriStr.substringAfterLast('/')
                if (!name.endsWith(".funscript", ignoreCase = true)) continue
                val rest = name.removeSuffix(".funscript").removeSuffix(".FUNSCRIPT")
                val axisId = if ('.' in rest) rest.substringAfterLast('.') else "L0"
                urisByAxis[axisId] = uriStr
            }
            if (urisByAxis.isEmpty()) null else withContext(Dispatchers.IO) { loadFunscriptMultiFromUris(context, urisByAxis) }
        }
        standaloneScriptData = data
        scriptPlayPositionMs = 0L
    }
    
    SettingsCard(title = stringResource(R.string.device_control_title)) {
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
            Text(stringResource(R.string.device_control_axis_sliders), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            // Add expand/collapse icon here
        }
        AnimatedVisibility(
            visible = deviceControlExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))
            AXIS_NAMES.forEach { axisName ->
                val pos = manualAxisPositions[axisName] ?: 50
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$axisName", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${pos}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = pos.toFloat(),
                        onValueChange = {
                            manualAxisPositions = manualAxisPositions + (axisName to it.toInt())
                            sliderSendJob?.cancel()
                            sliderSendJob = scope.launch {
                                while (isActive) {
                                    val cmd = buildAxisCommandFromPositions(context, manualAxisPositions, 500L)
                                    if (cmd.isNotEmpty()) onSendCommand(cmd)
                                    delay(100L)
                                }
                            }
                        },
                        onValueChangeFinished = {
                            sliderSendJob?.cancel()
                            sliderSendJob = null
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.device_control_script_section), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { scriptPickerLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.device_control_select_script))
                }
                Text(
                    text = selectedScriptName ?: stringResource(R.string.device_control_no_script),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (scriptPlaying) {
                            scriptPlayJob?.cancel()
                            scriptPlaying = false
                        } else {
                            val data = standaloneScriptData
                            if (data == null) {
                                Toast.makeText(context, context.getString(R.string.device_control_no_script), Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val totalMs = if (data.durationSec > 0) (data.durationSec * 1000).toLong()
                                else data.axes.flatMap { it.actions }.maxOfOrNull { it.at } ?: 1L
                            scriptPlaying = true
                            scriptPlayJob = scope.launch {
                                var pos = scriptPlayPositionMs
                                while (pos <= totalMs && isActive) {
                                    val cmd = buildAxisCommandFromScript(context, data, pos)
                                    if (cmd.isNotEmpty()) onSendCommand(cmd)
                                    delay(100L)
                                    pos += 100L
                                    scriptPlayPositionMs = pos
                                }
                                scriptPlaying = false
                            }
                        }
                    },
                    enabled = standaloneScriptData != null
                ) {
                    Text(if (scriptPlaying) stringResource(R.string.device_control_stop_script) else stringResource(R.string.device_control_play_script))
                }
                }
            }
        }
    }
}