package com.semseytech.rtsdevicesuitepro.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.storage.analyzer.DeepDark
import com.semseytech.rtsdevicesuitepro.storage.analyzer.NeonBlue

data class PermissionInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val impact: String
)

@Composable
fun PermissionExplanationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val permissions = PermissionRegistry.permissions

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Security, null, tint = NeonBlue, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Permission Transparency",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "To provide professional-grade suite services, we require the following access:",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                permissions.forEach { perm ->
                    PermissionItem(
                        PermissionInfo(
                            perm.title,
                            perm.shortDescription,
                            perm.icon,
                            perm.consequences
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                TextButton(
                    onClick = onNavigateToHelp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Learn more in the Help Center",
                        color = NeonBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("NOT NOW", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PermissionRationaleDialog(
    permissions: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val permissionRequirements = permissions.mapNotNull { PermissionRegistry.getForPermission(it) }
    
    if (permissionRequirements.isEmpty()) {
        onConfirm()
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = NeonBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permission Needed", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) 
            }
        },
        containerColor = DeepDark,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "To use this feature, the app needs the following access:",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                
                permissionRequirements.forEach { req ->
                    PermissionItem(
                        PermissionInfo(
                            req.title,
                            req.shortDescription,
                            req.icon,
                            req.consequences
                        )
                    )
                }

                Text(
                    "You can manage these permissions anytime from the Help Center (the ? icon in the top banner).",
                    color = NeonBlue.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PermissionItem(perm: PermissionInfo) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(NeonBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(perm.icon, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(perm.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(perm.description, color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp)
            Text(
                "If denied: ${perm.impact}",
                color = Color(0xFFFF5252),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
