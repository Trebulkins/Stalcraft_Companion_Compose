package com.example.stalcraft_companion_compose

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Int) = "item_detail/$itemId"
    }
}