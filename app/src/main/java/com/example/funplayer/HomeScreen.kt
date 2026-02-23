package com.example.funplayer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun HomeScreen(
    videos: List<VideoItem>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onVideoClick: (Int) -> Unit
) {
    val allTags = remember(videos) {
        buildSet<String> {
            videos.forEach { addAll(it.tags) }
        }.toList().sorted()
    }

    val visibleTags = allTags.take(MAX_VISIBLE_TAGS_IN_ROW)
    val showMoreChip = allTags.size > MAX_VISIBLE_TAGS_IN_ROW
    var showAllTagsDialog by remember { mutableStateOf(false) }

    val filteredVideos = videos.filter { video ->
        val matchSearch = searchText.isBlank() ||
                video.name.contains(searchText, ignoreCase = true) ||
                video.tags.any { it.contains(searchText, ignoreCase = true) }

        val matchTag = selectedTags.isEmpty() || selectedTags.all { it in video.tags }
        matchSearch && matchTag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChipItem(
                text = stringResource(R.string.tag_all),
                selected = selectedTags.isEmpty(),
                onClick = { onTagToggled(TAG_KEY_ALL) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            visibleTags.forEach { tag ->
                FilterChipItem(
                    text = tag,
                    selected = tag in selectedTags,
                    onClick = { onTagToggled(tag) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showMoreChip) {
                FilterChipItem(
                    text = "…",
                    selected = false,
                    onClick = { showAllTagsDialog = true }
                )
            }
        }

        if (showAllTagsDialog) {
            AlertDialog(
                onDismissRequest = { showAllTagsDialog = false },
                title = { Text(stringResource(R.string.tag_select_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChipItem(
                                text = stringResource(R.string.tag_all),
                                selected = selectedTags.isEmpty(),
                                onClick = { onTagToggled(TAG_KEY_ALL) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                FilterChipItem(
                                    text = tag,
                                    selected = tag in selectedTags,
                                    onClick = { onTagToggled(tag) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllTagsDialog = false }) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.video_empty_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video.id) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
internal fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(video.uri) { mutableStateOf<Bitmap?>(null) }
    var loadedDuration by remember(video.uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(video.uri) {
        if (video.uri != null) {
            val b = withContext(Dispatchers.IO) { loadVideoFirstFrame(context, video.uri!!) }
            thumbnail = b
        }
    }
    LaunchedEffect(video.uri) {
        if (video.uri != null && video.duration.isEmpty() && video.uri.startsWith("smb:", ignoreCase = true)) {
            val dur = withContext(Dispatchers.IO) { getVideoDurationFormattedFromUri(context, video.uri) }
            loadedDuration = dur
        }
    }

    val displayDuration = video.duration.ifEmpty { loadedDuration ?: "…" }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF263238)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(stringResource(R.string.cover), color = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = video.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.duration_format, displayDuration),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            video.tags.take(3).forEach { tag ->
                TagChip(text = tag)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
internal fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}
