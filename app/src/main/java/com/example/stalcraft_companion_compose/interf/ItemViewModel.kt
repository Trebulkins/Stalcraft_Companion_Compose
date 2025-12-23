package com.example.stalcraft_companion_compose.interf

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.stalcraft_companion_compose.data.ApiClient
import com.example.stalcraft_companion_compose.data.AppDatabase
import com.example.stalcraft_companion_compose.data.ItemRepository
import com.example.stalcraft_companion_compose.data.models.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ItemViewModel"
class ItemViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ItemRepository

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    private val _expandedCategories = MutableLiveData<Set<String>>(emptySet())
    val expandedCategories: LiveData<Set<String>> = _expandedCategories

    private val _selectedItem = MutableLiveData<Item?>(null)
    val selectedItem: LiveData<Item?> = _selectedItem

    val isLoading = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    private val _progress = MutableStateFlow(Pair(0, 100))
    val progress: StateFlow<Pair<Int, Int>> = _progress

    init {
        val dao = AppDatabase.getInstance(application).itemDao()
        repository = ItemRepository(dao)
        viewModelScope.launch {
            error.value = null
            try {
                repository.getAllItems().collect { itemsList ->
                    _items.value = itemsList
                }
            } catch (e: Exception) {
                error.value = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    suspend fun checkForUpdates(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                repository.needsUpdate(getApplication())
            } catch (e: Exception) {
                false
            }
        }
    }

    fun performUpdate() {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.refreshData(getApplication()) { current, total ->
                    _progress.value = Pair(current, total)
                    println("updateProgress: ${progress.value!!.first} / ${progress.value!!.second}")
                }
                withContext(Dispatchers.Main) {
                    error.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error.value = "Update failed: ${e.message}"
                    Log.e(TAG, "Update failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                }
            }
        }
    }

    fun getItemById(id: String): LiveData<Item?> {
        return liveData {
            emit(repository.getItemById(id))
        }
    }

    fun toggleCategoryExpansion(categoryName: String) {
        val currentSet = _expandedCategories.value ?: emptySet()
        val newSet = if (currentSet.contains(categoryName)) {
            currentSet.minus(categoryName)
        } else {
            currentSet.plus(categoryName)
        }
        _expandedCategories.value = newSet
    }
    fun expandAllCategories() {
        val currentItems = _items.value ?: emptyList()
        val allCategories = currentItems.map { it.category }.distinct()
        _expandedCategories.value = allCategories.toSet()
    }
    fun collapseAllCategories() {
        _expandedCategories.value = emptySet()
    }

    fun selectItem(item: Item) {
        _selectedItem.value = item
    }

    fun clearSelectedItem() {
        _selectedItem.value = null
    }

    // Вычисляемое свойство для уникальных категорий
    val uniqueCategories: LiveData<List<String>>
        get() = _items.map { items ->
            items.map { it.category }
                .distinct()
                .sorted()
        }

    // Получаем предметы по категории
    fun getItemsByCategory(category: String): LiveData<List<Item>> {
        return _items.map { items ->
            items.filter { it.category == category }
        }
    }

    fun getLastUpdateInfo(): String {
        val repoInfo = try {
            runBlocking { ApiClient.githubApi.getRepoInfo() }
        } catch (e: Exception) {
            return ""
        }
        return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                .parse(repoInfo.updatedAt) ?: Date()
            )
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}