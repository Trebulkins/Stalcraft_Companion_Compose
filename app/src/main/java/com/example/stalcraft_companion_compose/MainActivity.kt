package com.example.stalcraft_companion_compose

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.stalcraft_companion_compose.data.models.Item
import com.example.stalcraft_companion_compose.interf.ItemViewModel
import com.example.stalcraft_companion_compose.ui.theme.Stalcraft_Companion_ComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[ItemViewModel::class.java]
        setContent {
            Stalcraft_Companion_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(
                        modifier = Modifier.padding(innerPadding),
                        context = this,
                        owner = this,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}


private fun loadData(viewModel: ItemViewModel, owner: LifecycleOwner) {
    viewModel.items.observe(owner) { items ->
        if (items.isNotEmpty()) {

        }
    }
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
fun AppContent(modifier: Modifier, context: Context, viewModel: ItemViewModel, owner: LifecycleOwner) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // При запуске приложения проверяем обновление
    LaunchedEffect(Unit) {
        showUpdateDialog = checkForUpdates(
            viewModel = viewModel,
            owner = owner,
            netAvailable = viewModel.isNetworkAvailable(context)
        )
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
                        showProgressDialog = !coroutineScope.launch {
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
                        text = "${progress.value.first} / ${progress.value.second} (${progress.value.first.toFloat() / progress.value.second.toFloat() * 100}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цвет редкости
            Box(
                modifier = Modifier
                    .size(20.dp)
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

            // Изображение
            AsyncImage(
                model = item.iconPath,
                contentDescription = item.name.toString(),
                placeholder = painterResource(id = R.drawable.radioactive_icon),
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Информация о предмете
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.name.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Загрузка данных...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}