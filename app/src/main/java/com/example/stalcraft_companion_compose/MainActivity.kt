package com.example.stalcraft_companion_compose

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.stalcraft_companion_compose.data.ApiClient
import com.example.stalcraft_companion_compose.interf.ItemViewModel
import com.example.stalcraft_companion_compose.ui.theme.Stalcraft_Companion_ComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        vm = viewModel,
                    )
                }
            }
        }
    }
}

private fun getLastUpdateInfo(): String {
    val repoInfo = try {
        runBlocking { ApiClient.githubApi.getRepoInfo() }
    } catch (e: Exception) {
        return ""
    }
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        .format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            .parse(repoInfo.updatedAt.toString()) ?: Date()
        )
}

private fun loadData(viewModel: ItemViewModel, owner: LifecycleOwner) {
    viewModel.items.observe(owner) { items ->
        if (items.isNotEmpty()) {

        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connectivityManager.activeNetworkInfo
    return networkInfo != null && networkInfo.isConnected
}

private suspend fun checkForUpdates(viewModel: ItemViewModel, owner: LifecycleOwner, netAvailable: Boolean) {
    if (!netAvailable) {
        println("Нет подключения к интернету")
        loadData(viewModel, owner)
        return
    }

    try {
        val needsUpdate = withContext(Dispatchers.IO) { viewModel.checkForUpdates() }

        if (needsUpdate) {
            showUpdateDialog()
        } else {
            loadData(viewModel, owner)
        }
    } catch (e: Exception) {
        println("Ошибка обновления: $e")
        Log.e("Update check", "Update error: $e")
        loadData(viewModel, owner)
    }
}

@Composable
fun AppContent(modifier: Modifier, context: Context, viewModel: ItemViewModel, owner: LifecycleOwner) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val netAvailable = isNetworkAvailable(context)

    // При запуске приложения проверяем обновление
    LaunchedEffect(Unit) {
        checkForUpdates(viewModel = viewModel, owner = owner, netAvailable = netAvailable)
        showUpdateDialog = true
    }

    // Основной контент приложения
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Основной экран приложения")
        }
    }


    // Диалог предложения обновления
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(text = "Доступно обновление") },
            text = { Text("Обнаружена новая версия базы данных (${getLastUpdateInfo()}). Хотите обновить?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        showProgressDialog = true
                        coroutineScope.launch {
                            viewModel.performUpdate()
                            showProgressDialog = false
                        }
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
            onDismissRequest = { /* Не закрываем при клике вне диалога */ }
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Загрузка обновления",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Индикатор прогресса
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Текстовое отображение прогресса
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}