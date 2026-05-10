package com.semseytech.rtsdevicesuitepro.diagnostics.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.diagnostics.DiagnosticsViewModel
import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueCategory
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueSeverity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DIAGNOSTICS SUITE", fontFamily = FontFamily.Monospace) },
                actions = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.runScan() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ActivityLog(uiState.logMessages)
            
            Divider()
            
            if (uiState.issues.isEmpty() && !uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No issues detected. Run a scan to begin.")
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.issues) { issue ->
                        IssueItem(
                            issue = issue,
                            isFixing = uiState.fixingIssueId == issue.id,
                            onFix = { viewModel.applyFix(issue) },
                            explanation = viewModel.getExplanation(issue),
                            risk = viewModel.getRisk(issue),
                            fixImpact = viewModel.getFixImpact(issue)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IssueItem(
    issue: DiagnosticsIssue,
    isFixing: Boolean,
    onFix: () -> Unit,
    explanation: String,
    risk: String,
    fixImpact: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityBadge(issue.severity)
                Spacer(modifier = Modifier.width(8.dp))
                CategoryBadge(issue.category)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = issue.description,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "EXPLANATION: $explanation", fontSize = 14.sp)
                    Text(text = "RISK: $risk", fontSize = 14.sp, color = getSeverityColor(issue.severity))
                    Text(text = "IMPACT: $fixImpact", fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "TECHNICAL DETAILS:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = issue.technicalDetails,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.1f)).padding(4.dp).fillMaxWidth()
                    )

                    if (issue.safeToAutoFix) {
                        Button(
                            onClick = onFix,
                            enabled = !isFixing,
                            modifier = Modifier.padding(top = 8.dp).align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA00))
                        ) {
                            if (isFixing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Build, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AUTO-FIX")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: IssueSeverity) {
    Surface(
        color = getSeverityColor(severity),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = severity.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryBadge(category: IssueCategory) {
    Surface(
        color = if (category == IssueCategory.OFFENSIVE) Color(0xFFCC0000) else Color(0xFF0000CC),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = category.name,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActivityLog(messages: List<String>) {
    Column(modifier = Modifier.height(100.dp).fillMaxWidth().background(Color.DarkGray).padding(8.dp)) {
        Text("CONSOLE LOG", color = Color.Green, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        LazyColumn(reverseLayout = true) {
            items(messages.reversed()) { msg ->
                Text(text = "> $msg", color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

fun getSeverityColor(severity: IssueSeverity): Color = when (severity) {
    IssueSeverity.CRITICAL -> Color(0xFF880000)
    IssueSeverity.HIGH -> Color(0xFFCC0000)
    IssueSeverity.MEDIUM -> Color(0xFFCC6600)
    IssueSeverity.LOW -> Color(0xFFCCCC00)
    IssueSeverity.INFO -> Color(0xFF0000CC)
}
