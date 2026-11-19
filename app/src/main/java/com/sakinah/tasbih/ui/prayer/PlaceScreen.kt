package com.sakinah.tasbih.ui.prayer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.DeviceLocation
import com.sakinah.tasbih.prayer.PlaceSearch
import com.sakinah.tasbih.prayer.PrayerPlace
import com.sakinah.tasbih.ui.calendar.CalendarPage
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import kotlinx.coroutines.delay

@Composable
fun PlaceScreen(
    current: PrayerPlace?,
    search: PlaceSearchState,
    onSearch: (String) -> Unit,
    onChoose: (PrayerPlace) -> Unit,
    onUseDeviceLocation: () -> Unit,
    followDevice: Boolean,
    onFollowDevice: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var askLocation by rememberSaveable { mutableStateOf(false) }
    var locationDenied by rememberSaveable { mutableStateOf(false) }
    var waitingForLocation by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { answers ->
        val granted = answers.values.any { it }
        locationDenied = !granted
        if (granted) {
            waitingForLocation = true
            onUseDeviceLocation()
        }
    }

    // A short pause after typing, so a search goes out for a word and not for every letter.
    LaunchedEffect(query) {
        if (query.trim().length >= PlaceSearch.MIN_QUERY_LENGTH) {
            delay(SEARCH_DELAY_MS)
            onSearch(query)
        }
    }
    // Back to the times once the phone's position has been saved.
    LaunchedEffect(search.locating, current) {
        if (waitingForLocation && !search.locating) {
            waitingForLocation = false
            if (!search.locationFailed) onBack()
        }
    }

    if (askLocation) {
        AlertDialog(
            onDismissRequest = { askLocation = false },
            title = { Text(stringResource(R.string.prayer_location_title)) },
            text = { Text(stringResource(R.string.prayer_location_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    askLocation = false
                    if (DeviceLocation.hasPermission(context)) {
                        waitingForLocation = true
                        onUseDeviceLocation()
                    } else {
                        permission.launch(DeviceLocation.PERMISSIONS)
                    }
                }) { Text(stringResource(R.string.prayer_location_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { askLocation = false }) { Text(stringResource(R.string.prayer_not_now)) }
            },
        )
    }

    if (renaming && current != null) {
        var name by rememberSaveable { mutableStateOf(current.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.prayer_rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("prayer_rename_field"),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onRename(name)
                        renaming = false
                    },
                    modifier = Modifier.testTag("prayer_rename_save"),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    CalendarPage(title = stringResource(R.string.prayer_place_title), tag = "prayer_place_screen", onBack = onBack) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.prayer_search_label)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (search.searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier.fillMaxWidth().testTag("prayer_search"),
                )
            }
            item {
                OutlinedButton(
                    onClick = { askLocation = true },
                    enabled = !search.locating,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp).testTag("prayer_use_location"),
                ) {
                    if (search.locating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (search.locating) R.string.prayer_locating else R.string.prayer_use_location))
                }
            }
            if (search.locationFailed || locationDenied) {
                item {
                    Message(stringResource(if (locationDenied) R.string.prayer_location_denied else R.string.prayer_location_failed))
                }
            }
            if (current != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.prayer_current_place, text.placeLine(current)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp).testTag("prayer_current_place"),
                        )
                        TextButton(onClick = { renaming = true }, modifier = Modifier.testTag("prayer_rename")) {
                            Text(stringResource(R.string.prayer_rename_place))
                        }
                    }
                }
                if (current.fromDevice) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(value = followDevice, role = Role.Switch, onValueChange = onFollowDevice)
                                .heightIn(min = 56.dp)
                                .testTag("prayer_follow_device"),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.prayer_follow_device), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.prayer_follow_device_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(checked = followDevice, onCheckedChange = null)
                        }
                    }
                }
            }
            when {
                search.searchFailed -> item { Message(stringResource(R.string.prayer_search_failed), error = true) }
                search.searched && !search.searching && search.results.isEmpty() && query.isNotBlank() ->
                    item { Message(stringResource(R.string.prayer_search_empty)) }
            }
            items(search.results, key = { "${it.latitude},${it.longitude},${it.name}" }) { place ->
                PlaceRow(place, onClick = {
                    onChoose(place)
                    onBack()
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            item {
                Text(
                    stringResource(R.string.prayer_search_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PrayerPlace, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 10.dp)
            .testTag("prayer_place_result"),
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = LocalSakinahBrandColors.current.antiqueGold)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium)
            val detail = listOf(place.region, place.country).filter { it.isNotBlank() }.distinct().joinToString("، ")
            if (detail.isNotEmpty()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Message(text: String, error: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

private const val SEARCH_DELAY_MS = 600L
private const val MAX_NAME_LENGTH = 60
