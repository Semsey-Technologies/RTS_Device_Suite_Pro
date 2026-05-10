package com.semseytech.rtsdevicesuitepro.automation.flow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.automation.flow.*
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.automation.ui.ParameterPromptDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowEditorScreen(
    onBackClick: () -> Unit,
    viewModel: FlowEditorViewModel = viewModel()
) {
    var showNodePalette by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<FlowNode?>(null) }
    var connectionStartNode by remember { mutableStateOf<Pair<FlowNode, ConnectionType>?>(null) }
    var isEditingName by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isEditingName) {
                        TextField(
                            value = viewModel.flowName.value,
                            onValueChange = { viewModel.flowName.value = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White
                            ),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { isEditingName = false }) {
                                    Icon(Icons.Default.Check, "Save", tint = Color.Green)
                                }
                            }
                        )
                    } else {
                        Text(
                            text = viewModel.flowName.value,
                            color = Color.White,
                            modifier = Modifier.clickable { isEditingName = true }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (viewModel.isRunning.value) {
                        IconButton(onClick = { viewModel.stopManual() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = { viewModel.runManual() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Green)
                        }
                    }
                    IconButton(onClick = { 
                        viewModel.saveFlow {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNodePalette = true },
                containerColor = Color(0xFF00E5FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Node", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewModel.scale.value *= zoom
                        viewModel.offset.value += pan
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = viewModel.scale.value,
                        scaleY = viewModel.scale.value,
                        translationX = viewModel.offset.value.x,
                        translationY = viewModel.offset.value.y
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    viewModel.connections.forEach { conn ->
                        val fromNode = viewModel.nodes.find { it.id == conn.fromNodeId }
                        val toNode = viewModel.nodes.find { it.id == conn.toNodeId }

                        if (fromNode != null && toNode != null) {
                            val startYOffset = if (fromNode.component is Condition) {
                                if (conn.connectionType == ConnectionType.TRUE) 38.dp.toPx() else 62.dp.toPx()
                            } else {
                                50.dp.toPx()
                            }
                            
                            val start = fromNode.position + Offset(200.dp.toPx(), startYOffset)
                            val end = toNode.position + Offset(0f, 50.dp.toPx())
                            
                            drawFlowLine(start, end, conn.connectionType)
                        }
                    }
                }

                viewModel.nodes.forEach { node ->
                    NodeView(
                        node = node,
                        onMove = { delta -> viewModel.updateNodePosition(node.id, delta) },
                        onEdit = { editingNode = node },
                        onDelete = { viewModel.removeNode(node.id) },
                        onConnectStart = { type -> connectionStartNode = node to type },
                        onConnectEnd = { 
                            connectionStartNode?.let { (start, type) ->
                                if (start.id != node.id) {
                                    viewModel.addConnection(start.id, node.id, type)
                                }
                                connectionStartNode = null
                            }
                        }
                    )
                }
            }
        }

        if (showNodePalette) {
            PagedNodePalette(
                onDismiss = { showNodePalette = false },
                onSelect = { component ->
                    viewModel.addNode(component, -viewModel.offset.value / viewModel.scale.value + Offset(100f, 100f))
                    showNodePalette = false
                }
            )
        }

        editingNode?.let { node ->
            ParameterPromptDialog(
                component = node.component,
                onDismiss = { editingNode = null },
                onSave = { updatedComponent ->
                    viewModel.updateNodeComponent(node.id, updatedComponent)
                    editingNode = null
                }
            )
        }
    }
}

@Composable
fun NodeView(
    node: FlowNode,
    onMove: (Offset) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConnectStart: (ConnectionType) -> Unit,
    onConnectEnd: () -> Unit
) {
    val nodeColor = when (node.component) {
        is Trigger -> Color(0xFF2196F3)
        is Condition -> Color(0xFFFFC107)
        is Action -> Color(0xFF4CAF50)
        else -> Color(0xFF9C27B0)
    }

    val borderColor = when (node.executionStatus) {
        NodeStatus.RUNNING -> Color.Cyan
        NodeStatus.SUCCESS -> Color.Green
        NodeStatus.FAILURE -> Color.Red
        NodeStatus.STOPPED -> Color.Gray
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(node.position.x.roundToInt(), node.position.y.roundToInt()) }
            .size(200.dp, 100.dp)
            .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
            .then(
                if (borderColor != Color.Transparent) 
                    Modifier.background(borderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onEdit() },
                    onLongPress = { onDelete() }
                )
            }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.component.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (node.executionStatus == NodeStatus.RUNNING) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.Cyan)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = node.component.description,
                color = Color.LightGray,
                fontSize = 10.sp,
                maxLines = 2
            )
        }

        // Input Port
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-8).dp)
                .size(24.dp)
                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { onConnectEnd() }
                .padding(4.dp)
        ) {
            Box(Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(10.dp)))
        }
        
        // Output Ports
        if (node.component is Condition) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Port(Color.Green) { onConnectStart(ConnectionType.TRUE) }
                Port(Color.Red) { onConnectStart(ConnectionType.FALSE) }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 8.dp)
            ) {
                Port(nodeColor) { onConnectStart(ConnectionType.DEFAULT) }
            }
        }
    }
}

@Composable
fun Port(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(10.dp)))
    }
}

@Composable
fun getIconForName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "table_chart" -> Icons.Default.TableChart
        "add_comment" -> Icons.Default.AddComment
        "comment" -> Icons.Default.Comment
        "check_circle" -> Icons.Default.CheckCircle
        "person_add" -> Icons.Default.PersonAdd
        "share" -> Icons.Default.Share
        "insert_drive_file" -> Icons.Default.InsertDriveFile
        "file_present" -> Icons.Default.FilePresent
        "grid_on" -> Icons.Default.GridOn
        "create_new_folder" -> Icons.Default.CreateNewFolder
        "smartphone" -> Icons.Default.Smartphone
        "screen_lock_portrait" -> Icons.Default.ScreenLockPortrait
        "lock_open" -> Icons.Default.LockOpen
        "power" -> Icons.Default.Power
        "power_off" -> Icons.Default.PowerOff
        "battery_full" -> Icons.Default.BatteryFull
        "battery_alert" -> Icons.Default.BatteryAlert
        "battery_charging_full" -> Icons.Default.BatteryChargingFull
        "airplane_ticket" -> Icons.Default.AirplaneTicket
        "screenshot" -> Icons.Default.Screenshot
        "wifi" -> Icons.Default.Wifi
        "wifi_off" -> Icons.Default.WifiOff
        "network_wifi" -> Icons.Default.NetworkWifi
        "bluetooth" -> Icons.Default.Bluetooth
        "bluetooth_disabled" -> Icons.Default.BluetoothDisabled
        "bluetooth_connected" -> Icons.Default.BluetoothConnected
        "location_on" -> Icons.Default.LocationOn
        "location_off" -> Icons.Default.LocationOff
        "directions_walk" -> Icons.Default.DirectionsWalk
        "directions_car" -> Icons.Default.DirectionsCar
        "sms" -> Icons.Default.Sms
        "person" -> Icons.Default.Person
        "call" -> Icons.Default.Call
        "call_missed" -> Icons.Default.CallMissed
        "mms" -> Icons.Default.Mms
        "call_received" -> Icons.AutoMirrored.Filled.CallReceived
        "call_end" -> Icons.Default.CallEnd
        "voicemail" -> Icons.Default.Voicemail
        "email" -> Icons.Default.Email
        "chat" -> Icons.AutoMirrored.Filled.Chat
        "person_search" -> Icons.Default.PersonSearch
        "find_in_page" -> Icons.Default.FindInPage
        "apps" -> Icons.Default.Apps
        "notification_important" -> Icons.Default.NotificationImportant
        "schedule" -> Icons.Default.Schedule
        "wb_sunny" -> Icons.Default.WbSunny
        "nights_stay" -> Icons.Default.NightsStay
        "brightness_medium" -> Icons.Default.BrightnessMedium
        "public" -> Icons.Default.Public
        "record_voice_over" -> Icons.Default.RecordVoiceOver
        "notifications" -> Icons.Default.Notifications
        "volume_up" -> Icons.Default.VolumeUp
        "music_note" -> Icons.Default.MusicNote
        "audiotrack" -> Icons.Default.Audiotrack
        "notifications_active" -> Icons.Default.NotificationsActive
        "headset" -> Icons.Default.Headset
        "mic" -> Icons.Default.Mic
        "queue_music" -> Icons.Default.QueueMusic
        "help" -> Icons.Default.Help
        else -> Icons.Default.Circle
    }
}

@Composable
fun PagedNodePalette(
    onDismiss: () -> Unit,
    onSelect: (AutomationComponent) -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf("Triggers", "Conditions", "Actions")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Node", color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    pages.forEachIndexed { index, title ->
                        TextButton(
                            onClick = { currentPage = index },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (currentPage == index) Color(0xFF00E5FF) else Color.Gray
                            )
                        ) {
                            Text(title)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1E1E1E),
        text = {
            Column(modifier = Modifier.height(400.dp)) {
                val components = when (currentPage) {
                    0 -> listOf(
                        Trigger.SheetEdited, Trigger.MentionedInComment, Trigger.CommentAdded,
                        Trigger.CommentResolved, Trigger.AccessRequestReceived, Trigger.SpreadsheetShared,
                        Trigger.GSheetFileCreated, Trigger.CsvExported, Trigger.XlsxExported, Trigger.FileCreated(),
                        Trigger.ScreenOn, Trigger.ScreenOff, Trigger.ScreenUnlocked,
                        Trigger.PowerConnected, Trigger.PowerDisconnected,
                        Trigger.BatteryLevelAbove(), Trigger.BatteryLevelBelow(),
                        Trigger.AirplaneModeOn, Trigger.ScreenshotTaken,
                        Trigger.WiFiConnected, Trigger.WiFiDisconnected, Trigger.SpecificWiFiConnected(),
                        Trigger.BluetoothOn, Trigger.BluetoothOff, Trigger.BluetoothDeviceConnected(),
                        Trigger.GeofenceEnter(), Trigger.GeofenceExit(),
                        Trigger.ActivityWalking, Trigger.ActivityDriving,
                        Trigger.SmsReceived, Trigger.SmsFromContact(), Trigger.MmsReceived,
                        Trigger.IncomingCall, Trigger.CallAnswered, Trigger.CallEnded,
                        Trigger.MissedCall, Trigger.VoicemailReceived,
                        Trigger.EmailReceived(), Trigger.MessagingAppNotification(),
                        Trigger.ContactStatusChanged(), Trigger.NotificationKeyword(),
                        Trigger.AppOpened(), Trigger.NotificationMatches(),
                        Trigger.TimeOfDay(), Trigger.Sunrise, Trigger.Sunset,
                        Trigger.LightSensorThreshold(), Trigger.WebsiteContentChanged(),
                        Trigger.MusicStateChanged(), Trigger.AppPlayingAudio(), Trigger.VolumeChanged(),
                        Trigger.RingerModeChanged(), Trigger.AudioDeviceConnected(),
                        Trigger.MicrophoneActivated, Trigger.MediaMetadataChanged
                    )
                    1 -> listOf(
                        Condition.IsConnectedToWiFi, Condition.IsCharging, Condition.ScreenIsOn
                    )
                    2 -> listOf(
                        Action.Speak(), Action.ShowNotification(), Action.SetVolume(), Action.ToggleWiFi
                    )
                    else -> emptyList()
                }
                
                PalettePage(components, onSelect)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalettePage(options: List<AutomationComponent>, onSelect: (AutomationComponent) -> Unit) {
    val grouped = options.groupBy { it.category }
    
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (category, items) ->
            item {
                Text(
                    text = category,
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { opt ->
                        PaletteItem(opt.displayName, getIconForName(opt.icon), { onSelect(opt) })
                    }
                }
            }
        }
    }
}

@Composable
fun PaletteItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .width(80.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlowLine(start: Offset, end: Offset, type: ConnectionType) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val control1 = Offset(start.x + (end.x - start.x) / 2, start.y)
        val control2 = Offset(start.x + (end.x - start.x) / 2, end.y)
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }
    
    val color = when (type) {
        ConnectionType.TRUE -> Color.Green
        ConnectionType.FALSE -> Color.Red
        else -> Color.Cyan
    }
    
    drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
}
