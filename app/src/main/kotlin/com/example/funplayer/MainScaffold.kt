package com.example.funplayer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlin.OptIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    videos: List<VideoItem>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onVideoClick: (Int) -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVideosUpdate: (List<VideoItem>) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentTab = currentTab, onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.Device -> DeviceSettingsScreen()
                MainTab.Home -> HomeScreen(
                    videos = videos,
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    selectedTags = selectedTags,
                    onTagToggled = onTagToggled,
                    onVideoClick = onVideoClick
                )
                MainTab.Settings -> AppSettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onVideosScanned = onVideosUpdate,
                    onDeveloperModeChange = onDeveloperModeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTab == MainTab.Device,
            onClick = { onTabSelected(MainTab.Device) },
            icon = { Icon(Icons.Filled.Devices, contentDescription = stringResource(R.string.tab_device)) },
            label = { Text(stringResource(R.string.tab_device)) }
        )
        NavigationBarItem(
            selected = currentTab == MainTab.Home,
            onClick = { onTabSelected(MainTab.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.tab_home)) },
            label = { Text(stringResource(R.string.tab_home)) }
        )
        NavigationBarItem(
            selected = currentTab == MainTab.Settings,
            onClick = { onTabSelected(MainTab.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.tab_settings)) },
            label = { Text(stringResource(R.string.tab_settings)) }
        )
    }
}
