package com.semseytech.rtsdevicesuitepro.ui.help

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.semseytech.rtsdevicesuitepro.model.ModelIntelligence
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionRegistry
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionRequirement
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndPermissionsScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HELP & PERMISSIONS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = currentTheme.accentColor
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .drawBehind {
                    val gridSize = 40.dp.toPx()
                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                }
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black.copy(alpha = 0.3f),
                contentColor = currentTheme.accentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = currentTheme.accentColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("HELP", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("PERMISSIONS", fontWeight = FontWeight.Bold) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    HelpSection()
                } else {
                    PermissionsSection()
                }
            }
        }
    }
}

@Composable
fun HelpSection() {
    val context = LocalContext.current
    val currentTheme = LocalTheme.current
    var searchQuery by remember { mutableStateOf("") }
    var activeTutorial by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    
    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            placeholder = { Text("Search help...", color = Color.White.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = currentTheme.accentColor) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = currentTheme.accentColor,
                unfocusedBorderColor = currentTheme.accentColor.copy(alpha = 0.3f),
                cursorColor = currentTheme.accentColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val helpItems = listOf(
                HelpItem("What is RTS Device Suite Pro?", "Think of it as a Swiss Army knife for your phone! It helps you keep your device fast, clean, and safe without needing to be a computer expert.", Icons.Default.AutoAwesome, "GETTING STARTED"),
                HelpItem("Why does it matter?", "Over time, phones get 'cluttered' with old files and background tasks. This app helps 'sweep' the digital dust away, making your battery last longer and your apps run smoother.", Icons.Default.Favorite, "GETTING STARTED"),
                HelpItem("Backup & Restore", "Saves your photos, messages, and contacts. If you ever lose your phone, you can get everything back easily. It uses storage and SMS/Contacts permissions to keep your data safe.", Icons.Default.CloudUpload, "CORE FEATURES"),
                HelpItem("Network Health", "Checks your Wi-Fi and mobile data to make sure you have the fastest connection possible for videos and games. It uses Location permission to find nearby signals.", Icons.Default.NetworkCheck, "CORE FEATURES"),
                HelpItem("Storage Cleaner", "Finds big files and 'junk' that you don't need anymore, giving you more space for the things you love. It needs Storage permission to see where the clutter is.", Icons.Default.CleaningServices, "CORE FEATURES"),
                HelpItem("Expert Mode (ADB & Terminal)", "WARNING: These tools are for experts. They are safe to explore, but dangerous to change if you aren't sure what they do. They talk directly to the system's core.", Icons.Default.Warning, "ADVANCED TOOLS")
            )

            val filteredItems = helpItems.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }

            val grouped = filteredItems.groupBy { it.category }

            grouped.forEach { (category, items) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (category == "ADVANCED TOOLS") Color.Red else currentTheme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { item ->
                            HelpCard(title = item.title, description = item.description, icon = item.icon)
                        }
                    }
                }
            }

            if (searchQuery.isEmpty()) {
                item {
                    val spec = remember { ModelIntelligence.getSpec(Build.MANUFACTURER, Build.MODEL) }
                    Text(
                        "YOUR DEVICE: ${Build.MODEL}",
                        style = MaterialTheme.typography.titleMedium,
                        color = currentTheme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HelpCard(
                        title = "Device-Specific Guidance",
                        description = spec.notes.ifEmpty { "Standard maintenance applies to your ${spec.model}." },
                        icon = Icons.Default.PhoneAndroid
                    )
                }

                item {
                    Text(
                        "COMMUNITY & SUPPORT",
                        style = MaterialTheme.typography.titleMedium,
                        color = currentTheme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CommunityLink("GitHub", Icons.Default.Code, Modifier.weight(1f)) {
                            openUrl("https://github.com/semseytech/rtsdevicesuitepro")
                        }
                        CommunityLink("Reddit", Icons.Default.Chat, Modifier.weight(1f)) {
                            openUrl("https://reddit.com/r/rtsdevicesuite")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CommunityLink("Official Website", Icons.Default.Language, Modifier.fillMaxWidth()) {
                        openUrl("https://semseytech.com/rts-device-suite-pro")
                    }
                }

                item {
                    Text(
                        "STEP-BY-STEP GUIDES",
                        style = MaterialTheme.typography.titleMedium,
                        color = currentTheme.accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TutorialPlaceholder("How to Create Your First Backup", "Walkthrough - 5 Steps") {
                        activeTutorial = "How to Create Your First Backup" to listOf(
                            "Open the Backup screen from the dashboard.",
                            "Select the categories you want to save (Photos, Contacts, etc.).",
                            "Choose your destination (Internal, SD Card, or Cloud).",
                            "Tap 'RUN BACKUP' and wait for completion.",
                            "Verify your backup in the 'View Backups' tool."
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TutorialPlaceholder("Optimizing Your Home Wi-Fi", "Walkthrough - 3 Steps") {
                        activeTutorial = "Optimizing Your Home Wi-Fi" to listOf(
                            "Go to Network Health and run a 'Wi-Fi Analysis'.",
                            "Check the 'Channel Overlap' graph for interference.",
                            "If your channel is crowded, change it in your router settings."
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TutorialPlaceholder("Enabling Developer Mode", "External Guide - System Settings") {
                        activeTutorial = "Enabling Developer Mode" to listOf(
                            "Open your phone's system Settings.",
                            "Go to 'About Phone'.",
                            "Tap 'Build Number' 7 times rapidly.",
                            "Go back to System > Developer Options.",
                            "Enable 'USB Debugging' and 'Wireless Debugging' if needed for RTS PRO tools."
                        )
                    }
                }
            }
        }
    }

    if (activeTutorial != null) {
        AlertDialog(
            onDismissRequest = { activeTutorial = null },
            title = { Text(activeTutorial!!.first, fontWeight = FontWeight.Bold, color = currentTheme.accentColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTutorial!!.second.forEachIndexed { index, step ->
                        Row {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = currentTheme.accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(step, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeTutorial = null }) {
                    Text("GOT IT", color = currentTheme.accentColor)
                }
            },
            containerColor = Color(0xFF1A1A1A),
            textContentColor = Color.White
        )
    }
}

data class HelpItem(val title: String, val description: String, val icon: ImageVector, val category: String)

@Composable
fun HelpCard(title: String, description: String, icon: ImageVector) {
    val currentTheme = LocalTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, null, tint = currentTheme.accentColor, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun CommunityLink(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val currentTheme = LocalTheme.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(icon, null, tint = currentTheme.accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun TutorialPlaceholder(title: String, type: String, onClick: () -> Unit) {
    val currentTheme = LocalTheme.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(currentTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = currentTheme.accentColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(type, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun PermissionsSection() {
    val context = LocalContext.current
    val currentTheme = LocalTheme.current
    
    val permissions = remember { PermissionRegistry.permissions }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SYSTEM PERMISSIONS",
                style = MaterialTheme.typography.titleMedium,
                color = currentTheme.accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Manage and understand how this app uses your device permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(permissions) { info ->
            PermissionItem(info)
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", context.packageName, null)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
            ) {
                Text("OPEN SYSTEM SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionItem(info: PermissionRequirement) {
    val context = LocalContext.current
    val currentTheme = LocalTheme.current
    var isGranted by remember { mutableStateOf(checkPermission(context, info.permission)) }
    var expanded by remember { mutableStateOf(false) }

    // Update state when returning to screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isGranted = checkPermission(context, info.permission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, (if (isGranted) currentTheme.accentColor else Color.Red).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (isGranted) currentTheme.accentColor else Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(info.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                
                Switch(
                    checked = isGranted,
                    onCheckedChange = {
                        if (it) {
                            // Can't directly grant from here easily for all types, 
                            // best to send to settings or use a launcher if it's standard
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = currentTheme.accentColor,
                        uncheckedThumbColor = Color.Gray
                    )
                )
            }

            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (expanded) "Show Less" else "Why is this needed?",
                    style = MaterialTheme.typography.labelSmall,
                    color = currentTheme.accentColor
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                PermissionDetail("Description", info.longDescription)
                PermissionDetail("Usage", info.usage)
                PermissionDetail("If Denied", info.consequences)
            }
        }
    }
}

@Composable
fun PermissionDetail(label: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
        Text(content, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
    }
}

fun checkPermission(context: Context, permission: String): Boolean {
    return if (permission == "android.permission.MANAGE_EXTERNAL_STORAGE") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    } else {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
