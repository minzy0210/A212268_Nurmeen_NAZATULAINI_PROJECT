package com.example.a212268_nazatulaini_lab1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoryScreen(
    filter: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onFoodItemClick: (String) -> Unit = {},
    onNonFoodItemClick: (String) -> Unit = {},
    viewModel: ReServeViewModel
) {
    val userListedItems    by viewModel.userListedItems.collectAsStateWithLifecycle()
    val reservedQuantities by viewModel.reservedQuantities.collectAsStateWithLifecycle()
    val borrowedItems      by viewModel.borrowedItems.collectAsStateWithLifecycle()

    // Recomputes when userListedItems changes (new item added)
    val items = remember(userListedItems, filter) {
        when (filter) {
            "Food"     -> viewModel.getFoodItems().map { it.name }
            "Non-food" -> viewModel.getNonFoodItems().map { it.name }
            "My Listings" -> userListedItems.map { it.name }
            else       -> emptyList()
        }
    }

    val isSoldOut = { name: String ->
        val reserved = reservedQuantities[name] ?: 0
        val total = viewModel.getUserListedItem(name)?.quantity
            ?: getFoodItemData(name).quantity
        reserved >= total
    }
    val isBorrowed = { name: String -> name in borrowedItems }
    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                CustomBottomNavigation(
                    onHomeClick = onHomeClick,
                    onSearchClick = onHomeClick,
                    onEmailClick = onHomeClick,
                    onAddClick = onHomeClick
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            top = 48.dp, start = 8.dp,
                            end = 16.dp, bottom = 12.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                filter,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "${items.size} items available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Items list
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items.forEach { name ->
                        FullWidthItemCard(
                            name = name,
                            imageRes = getItemImage(name),
                            isSoldOut = isSoldOut(name),
                            isBorrowed = isBorrowed(name),
                            photoUriString = viewModel.getPhotoUri(name),
                            onItemClick = {
                                val userItem = userListedItems.firstOrNull { it.name == name }
                                when {
                                    filter == "Non-food" -> onNonFoodItemClick(name)
                                    filter == "My Listings" && userItem != null ->
                                        if (userItem.category.equals("Food", ignoreCase = true)) onFoodItemClick(name)
                                        else onNonFoodItemClick(name)
                                    else -> onFoodItemClick(name)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}