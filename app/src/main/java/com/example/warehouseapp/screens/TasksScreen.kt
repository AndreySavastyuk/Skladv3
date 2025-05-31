package com.example.warehouseapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.Task
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.graphics.Color

@Composable
fun TasksScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    val tasks by viewModel.tasks.collectAsState()
    var showRefreshDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Задания",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { showRefreshDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Нет доступных заданий",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            // Tasks list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onTaskClick = {
                            viewModel.selectTask(task)
                            navController.navigate("shipment")
                        }
                    )
                }
            }
        }

        // Back button
        TextButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Назад")
        }
    }

    // Refresh dialog
    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text("Обновление заданий") },
            text = { Text("Загрузка заданий с сервера...") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Refresh tasks
                        showRefreshDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onTaskClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Card(
        onClick = onTaskClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(task.createdDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    task.isCompleted -> {
                        AssistChip(
                            onClick = { },
                            label = { Text("Выполнено") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                labelColor = Color(0xFF2E7D32)
                            )
                        )
                    }
                    task.isPaused -> {
                        AssistChip(
                            onClick = { },
                            label = { Text("Отложено") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                                labelColor = Color(0xFFE65100)
                            )
                        )
                    }
                    else -> {
                        AssistChip(
                            onClick = { },
                            label = { Text("Активно") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress
            val completedItems = task.items.count { it.isCompleted }
            val totalItems = task.items.size

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { completedItems.toFloat() / totalItems.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$completedItems/$totalItems",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items preview
            Text(
                text = "Позиций: ${task.items.size}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}