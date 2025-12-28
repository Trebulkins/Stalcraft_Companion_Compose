package com.example.stalcraft_companion_compose

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.stalcraft_companion_compose.api.ApiClient
import com.example.stalcraft_companion_compose.data.ItemViewModel
import com.example.stalcraft_companion_compose.data.models.Item
import com.example.stalcraft_companion_compose.data.models.TranslationString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.round
import kotlin.toBigDecimal


private fun loadData(viewModel: ItemViewModel, owner: LifecycleOwner) {
    viewModel.items.observe(owner) {  }
}


private suspend fun checkForUpdates(viewModel: ItemViewModel, owner: LifecycleOwner, netAvailable: Boolean): Boolean {
    if (!netAvailable) {
        println("Нет подключения к интернету")
        loadData(viewModel, owner)
        return false
    }

    try {
        val needsUpdate = withContext(Dispatchers.IO) { viewModel.checkForUpdates() }
        if (needsUpdate) {
            return true
        } else {
            loadData(viewModel, owner)
        }
    } catch (e: Exception) {
        println("Ошибка обновления: $e")
        Log.e("Update check", "Update error: $e")
        loadData(viewModel, owner)
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToItemDetail: (String) -> Unit,
    context: Context,
    owner: LifecycleOwner,
    viewModel: ItemViewModel
){
    val items by viewModel.items.observeAsState(emptyList())
    val uniqueCategories by viewModel.uniqueCategories.observeAsState(emptyList())
    val expandedCategories by viewModel.expandedCategories.observeAsState(emptySet())

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Группируем предметы по категориям
    val itemsByCategory = remember(items) {
        items.groupBy { it.category }
            .mapValues { (_, items) -> items }
    }

    // При запуске приложения проверяем обновление
    LaunchedEffect(Unit) {
        showUpdateDialog = checkForUpdates(
            viewModel = viewModel,
            owner = owner,
            netAvailable = viewModel.isNetworkAvailable(context)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "База предметов",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (items.isEmpty()) { LoadingOverlay() }
            else {
                // Основной контент
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)

                ) {
                    items(uniqueCategories) { category ->
                        CategorySection(
                            category = category,
                            items = itemsByCategory[category] ?: emptyList(),
                            isExpanded = expandedCategories.contains(category),
                            onCategoryClick = { viewModel.toggleCategoryExpansion(category) },
                            onItemClick = { item -> onNavigateToItemDetail(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Диалог предложения обновления
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(text = "Доступно обновление") },
            text = { Text("Обнаружена новая версия базы данных (${viewModel.getLastUpdateInfo()}). Хотите обновить?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        showProgressDialog = progress.value.first != progress.value.second
                        coroutineScope.launch {
                            viewModel.performUpdate()
                        }.isCompleted
                    }
                ) {
                    Text("Обновить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showUpdateDialog = false }
                ) {
                    Text("Позже")
                }
            }
        )
    }

    // Диалог с прогрессом загрузки
    if (showProgressDialog) {
        Dialog(
            onDismissRequest = { }
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Загрузка обновления",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    LinearProgressIndicator(
                        progress = progress.value.first.toFloat() / progress.value.second.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(2.dp)
                    )
                    // Текстовое отображение прогресса
                    Text(
                        text = "${progress.value.first} / ${progress.value.second}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "( ${progress.value.first.toFloat() / progress.value.second.toFloat() * 100}% )",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цвет редкости
            Box(
                modifier = Modifier
                    .size(4.dp, 80.dp)
                    .background(
                        color = when (item.color) {
                            "DEFAULT" -> Color(0x93B4B4B4)
                            "RANK_NEWBIE" -> Color(0xFF4CAF50)
                            "RANK_STALKER" -> Color(0xFF2196F3)
                            "RANK_VETERAN" -> Color(0xFFFF00DC)
                            "RANK_MASTER" -> Color(0xFFB00000)
                            "RANK_LEGEND" -> Color(0xFFFFEB3B)
                            else -> Color(0x93FFFFFF)
                        },
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Изображение
            GlideImage(
                model = ApiClient.DATABASE_BASE_URL + item.iconPath,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )

                when (val name = item.name) {
                    is TranslationString.Text -> name.text
                    is TranslationString.Translation -> name.lines.ru
                }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = item.id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}


@Composable
fun CategorySection(
    category: String,
    items: List<Item>,
    isExpanded: Boolean,
    onCategoryClick: () -> Unit,
    onItemClick: (Item) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            // Заголовок категории
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (items.isNotEmpty()) {
                        Text(
                            text = "${items.size} предметов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded)
                        "Свернуть"
                    else
                        "Развернуть",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Анимация раскрытия/закрытия
            AnimatedVisibility(
                visible = isExpanded && items.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        ItemCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                        if (index < items.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Загрузка данных...",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}