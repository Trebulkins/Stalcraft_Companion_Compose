package com.example.stalcraft_companion_compose

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.stalcraft_companion_compose.api.ApiClient
import com.example.stalcraft_companion_compose.data.ItemViewModel
import com.example.stalcraft_companion_compose.data.models.InfoBlock
import com.example.stalcraft_companion_compose.data.models.Item
import com.example.stalcraft_companion_compose.data.models.TranslationString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    viewModel: ItemViewModel = viewModel()
){
    // Получаем предмет по ID
    val item by remember(itemId) {
        derivedStateOf { viewModel.getItemById(itemId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val name = item?.name) {
                        is TranslationString.Text -> name.text
                        is TranslationString.Translation -> name.lines.ru
                        else -> "Предмет"
                    }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        ItemDetailContent(
            item = item!!,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ItemDetailContent(
    item: Item,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Изображение предмета
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlideImage(
                    model = ApiClient.DATABASE_BASE_URL + item.iconPath,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                )
            }
        }

        item {
            // Основная информация в карточках
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Карточка с редкостью
                InfoCard(
                    icon = Icons.Default.Star,
                    title = "Редкость",
                    content = item.color,
                    color = when (item.color) {
                        "DEFAULT" -> Color(0x93B4B4B4)
                        "RANK_NEWBIE" -> Color(0xFF4CAF50)
                        "RANK_STALKER" -> Color(0xFF2196F3)
                        "RANK_VETERAN" -> Color(0xFFFF00DC)
                        "RANK_MASTER" -> Color(0xFFB00000)
                        "RANK_LEGEND" -> Color(0xFFFFEB3B)
                        else -> Color(0x93FFFFFF)
                    }
                )

                InfoCard(
                    icon = Icons.Default.List,
                    title = "Категория",
                    content = item.category
                )

                InfoCard(
                    icon = Icons.Default.Lock,
                    title = "Статус",
                    content = item.status.state,
                    isMultiline = true
                )
            }
        }

        // Дополнительные характеристики
        if (item.infoBlocks!!.isNotEmpty()) {
            item {
                Text(
                    text = "Характеристики",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(item.infoBlocks as List<InfoBlock>) { infoblock ->
                when (infoblock.type) {
                    "text" -> TextBlockLine(infoblock as InfoBlock.TextBlock)
                    "damage" -> DamageBlockLine(infoblock as InfoBlock.DamageBlock)
                    "list" -> ListBlockLine(infoblock as InfoBlock.ListBlock)
                    "numeric" -> NumericBlockLine(infoblock as InfoBlock.NumericBlock)
                    "key-value" -> KeyValueBlockLine(infoblock as InfoBlock.KeyValueBlock)
                    "range" -> RangeBlockLine(infoblock as InfoBlock.RangeBlock)
                    "usage" -> UsageBlockLine(infoblock as InfoBlock.UsageBlock)
                    "item" -> ItemBlockLine(infoblock as InfoBlock.ItemBlock)
                }
            }
        }

        // Отступ внизу
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ItemBlockLine(block: InfoBlock.ItemBlock) {
    TODO("Not yet implemented")
}

@Composable
fun UsageBlockLine(block: InfoBlock.UsageBlock) {
    TODO("Not yet implemented")
}

@Composable
fun RangeBlockLine(block: InfoBlock.RangeBlock) {
    TODO("Not yet implemented")
}

@Composable
fun KeyValueBlockLine(block: InfoBlock.KeyValueBlock) {
    TODO("Not yet implemented")
}

@Composable
fun NumericBlockLine(block: InfoBlock.NumericBlock) {
    TODO("Not yet implemented")
}

@Composable
fun ListBlockLine(block: InfoBlock.ListBlock) {
    TODO("Not yet implemented")
}

@Composable
fun DamageBlockLine(block: InfoBlock.DamageBlock) {
    TODO("Not yet implemented")
}

@Composable
fun TextBlockLine(block: InfoBlock.TextBlock) {
    TODO("Not yet implemented")
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    color: Color = MaterialTheme.colorScheme.primary,
    isMultiline: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isMultiline) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = if (isMultiline) Modifier.padding(top = 4.dp) else Modifier
                )
            }
        }
    }
}

@Composable
fun AttributeCard(
    attributeName: String,
    attributeValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = attributeName.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = attributeValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}