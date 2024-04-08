package com.bnyro.clock.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bnyro.clock.R
import com.bnyro.clock.obj.NumberKeypadOperation
import com.bnyro.clock.ui.components.ClickableIcon
import com.bnyro.clock.ui.components.FormattedTimerTime
import com.bnyro.clock.ui.components.NumberKeypad
import com.bnyro.clock.ui.components.TimePickerDial
import com.bnyro.clock.ui.components.TimerItem
import com.bnyro.clock.ui.model.TimerModel
import com.bnyro.clock.ui.nav.TopBarScaffold
import com.bnyro.clock.util.KeepScreenOn
import com.bnyro.clock.util.Preferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onClickSettings: () -> Unit, timerModel: TimerModel) {
    val context = LocalContext.current
    val useScrollPicker = Preferences.instance.getBoolean(Preferences.timerUsePickerKey, false)
    val showExampleTimers = Preferences.instance.getBoolean(Preferences.timerShowExamplesKey, true)

    LaunchedEffect(Unit) {
        timerModel.tryConnect(context)
    }
    var createNew by remember {
        mutableStateOf(false)
    }

    TopBarScaffold(title = stringResource(R.string.timer), onClickSettings, actions = {
        if (timerModel.scheduledObjects.isEmpty()) {
            ClickableIcon(
                imageVector = Icons.Rounded.AddAlarm,
                contentDescription = stringResource(R.string.add_preset_timer)
            ) {
                timerModel.addPersistentTimer(timerModel.timePickerSeconds)
            }
        } else {
            ClickableIcon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add_preset_timer)
            ) {
                createNew = true
            }
        }
    }) { paddingValues ->
        if (timerModel.scheduledObjects.isEmpty()) {
            Column(
                Modifier
                    .padding(paddingValues)
            ) {
                TimerPicker(
                    useScrollPicker,
                    timerModel,
                    showExampleTimers,
                    context,
                    onCreateNew = {
                        createNew = false
                    },
                    showFAB = true
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Top
            ) {
                itemsIndexed(timerModel.scheduledObjects) { index, obj ->
                    TimerItem(obj, index, timerModel)
                }
            }
            KeepScreenOn()
        }
    }

    if (createNew) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { createNew = false },
            sheetState = sheetState
        ) {
            TimerPicker(
                useScrollPicker,
                timerModel,
                showExampleTimers,
                context,
                onCreateNew = {
                    createNew = false
                },
                showFAB = false
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TimerPicker(
    useScrollPicker: Boolean,
    timerModel: TimerModel,
    showExampleTimers: Boolean,
    context: Context,
    onCreateNew: () -> Unit,
    showFAB: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!useScrollPicker) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(2f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePickerDial(timerModel)
            }
        } else {
            Column(
                modifier = Modifier.weight(3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FormattedTimerTime(
                    seconds = timerModel.timePickerFakeUnits,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                NumberKeypad(
                    onOperation = { operation ->
                        when (operation) {
                            is NumberKeypadOperation.AddNumber -> timerModel.addNumber(
                                operation.number
                            )

                            is NumberKeypadOperation.Delete -> timerModel.deleteLastNumber()
                            is NumberKeypadOperation.Clear -> timerModel.clear()
                        }
                    }
                )
            }
        }
        if (showExampleTimers) {
            val haptic = LocalHapticFeedback.current
            LazyVerticalGrid(
                modifier = Modifier
                    .heightIn(0.dp, 200.dp)
                    .fillMaxWidth(),
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items = timerModel.persistentTimers) { index, timer ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = {
                                    timerModel.timePickerSeconds = timer.seconds
                                    onCreateNew.invoke()
                                    timerModel.startTimer(context)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    timerModel.removePersistentTimer(index)
                                }
                            )
                            .width(100.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            timer.formattedTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
        if (showFAB) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 16.dp)
                    .padding(end = 16.dp),
                onClick = {
                    onCreateNew.invoke()
                    timerModel.startTimer(context)
                }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.start)
                )
            }
        } else {
            Button(modifier = Modifier.padding(vertical = 16.dp), onClick = {
                onCreateNew.invoke()
                timerModel.startTimer(context)
            }) {
                Text(
                    text = stringResource(R.string.start),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
