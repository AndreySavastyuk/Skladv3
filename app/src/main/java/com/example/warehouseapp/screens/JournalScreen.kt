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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.warehouseapp.viewmodel.WarehouseViewModel
import com.example.warehouseapp.data.Product
import com.example.warehouseapp.data.ProductType
import java.text.SimpleDateFormat
import java.util.*

data class JournalFilter(
    val showReception: Boolean = true,
    val showShipment: Boolean = true,
    val dateFrom: Date? = null,
    val dateTo: Date? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: WarehouseViewModel,
    navController: NavController
) {
    val products by viewModel.products.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(JournalFilter()) }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Журнал операций",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            "Назад",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                actions = {
                    // Кнопка фильтра
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Filled.FilterList,
                            "Фильтр",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    // Кнопка экспорта
                    IconButton(onClick = { /* TODO: Экспорт в Excel */ }) {
                        Icon(
                            Icons.Filled.FileDownload,
                            "Экспорт",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Статистика
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Всего операций",
                    value = products.size.toString(),
                    icon = Icons.Filled.Receipt
                )
                StatCard(
                    title = "Приемка",
                    value = products.count { !it.isSynced }.toString(),
                    icon = Icons.Filled.Inventory,
                    color = Color(0xFF4CAF50)
                )
                StatCard(
                    title = "Выдача",
                    value = "0", // TODO: Добавить подсчет выданных
                    icon = Icons.Filled.ShoppingCart,
                    color = Color(0xFF2196F3)
                )
            }

            // Список операций
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Журнал пуст",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products.sortedByDescending { it.receivedDate }) { product ->
                        JournalItemCard(
                            product = product,
                            dateFormat = dateFormat,
                            onEdit = { /* TODO: Редактирование */ },
                            onDelete = { /* TODO: Удаление */ }
                        )
                    }
                }
            }
        }
    }

    // Диалог фильтра
    if (showFilterDialog) {
        FilterDialog(
            filter = filter,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter ->
                filter = newFilter
                showFilterDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalItemCard(
    product: Product,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Иконка типа операции
            Icon(
                imageVector = if (product.type == ProductType.PART)
                    Icons.Filled.Build
                else
                    Icons.Filled.ViewInAr,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (product.type == ProductType.PART)
                    Color(0xFF4CAF50)
                else
                    Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Информация о продукте
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "QR: ${product.qrCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Chip(
                        onClick = { },
                        label = { Text("Кол-во: ${product.quantity}") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Numbers,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Chip(
                        onClick = { },
                        label = { Text("Ячейка: ${product.storageLocation}") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                Text(
                    text = dateFormat.format(product.receivedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Статус синхронизации
            if (product.isSynced) {
                Icon(
                    Icons.Filled.CloudDone,
                    contentDescription = "Синхронизировано",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = "Не синхронизировано",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterDialog(
    filter: JournalFilter,
    onDismiss: () -> Unit,
    onApply: (JournalFilter) -> Unit
) {
    var showReception by remember { mutableStateOf(filter.showReception) }
    var showShipment by remember { mutableStateOf(filter.showShipment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтр журнала") },
        text = {
            Column {
                Text("Тип операций:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showReception,
                        onCheckedChange = { showReception = it }
                    )
                    Text("Приемка")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showShipment,
                        onCheckedChange = { showShipment = it }
                    )
                    Text("Выдача")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        filter.copy(
                            showReception = showReception,
                            showShipment = showShipment
                        )
                    )
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun Chip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            label()
        }
    }
}