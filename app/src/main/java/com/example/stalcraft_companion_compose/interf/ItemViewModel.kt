package com.example.stalcraft_companion_compose.interf

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.stalcraft_companion_compose.data.AppDatabase
import com.example.stalcraft_companion_compose.data.ItemRepository
import com.example.stalcraft_companion_compose.data.models.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ItemViewModel"
class ItemViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ItemRepository
    val items: LiveData<List<Item>>
    val isLoading = MutableLiveData(false)
    val error = MutableLiveData<String?>()
    val progressCurrent = MutableLiveData(0)
    val progressTotal = MutableLiveData(100)

    init {
        val dao = AppDatabase.getInstance(application).itemDao()
        repository = ItemRepository(dao)
        items = repository.getAllItems().asLiveData()
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
                    progressCurrent.postValue(current)
                    progressTotal.postValue(total)
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
}