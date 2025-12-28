package com.example.stalcraft_companion_compose.data.database

import android.content.Context
import com.example.stalcraft_companion_compose.api.ApiClient
import com.example.stalcraft_companion_compose.data.models.Item
import com.example.stalcraft_companion_compose.data.models.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ItemRepository(private val itemDao: ItemDao) {
    suspend fun needsUpdate(context: Context): Boolean {
        return try {
            val remoteInfo = ApiClient.githubApi.getRepoInfo()
            val localUpdate = Prefs.getLastUpdate(context)
            localUpdate == null || remoteInfo.updatedAt > localUpdate
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshData(context: Context, progressCallback: (Int, Int) -> Unit) {
        try {
            val listings = ApiClient.databaseApi.getItemListings()
            val totalItems = listings.size
            val items = mutableListOf<Item>()
            println("Предметов собрано: $totalItems")

            listings.forEachIndexed { index, listing ->
                val item = ApiClient.databaseApi.getItem(listing.data)
                items.add(item)
                progressCallback(index + 1, totalItems)
            }

            itemDao.clearAll()
            itemDao.insertAll(items)

            val repoInfo = ApiClient.githubApi.getRepoInfo()
            Prefs.setLastUpdate(context, repoInfo.updatedAt)
        } catch (e: Exception) {
            throw e
        }
    }

    fun getItemById(id: String): Item? = itemDao.getItemById(id)

    fun getAllItems(): Flow<List<Item>> = itemDao.getAllItems()

    fun insertAll(items: MutableList<Item>) {
        itemDao.insertAll(items)
    }

    fun insertItem(item: Item) {
        itemDao.insert(item)
    }
}