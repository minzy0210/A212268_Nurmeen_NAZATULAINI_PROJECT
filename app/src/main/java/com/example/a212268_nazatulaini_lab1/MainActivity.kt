package com.example.a212268_nazatulaini_lab1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.a212268_nazatulaini_lab1.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database   = (application as ReServeApplication).database
        val repository = (application as ReServeApplication).repository

        val viewModel = ViewModelProvider(
            this, ReServeViewModel.Factory(repository)
        )[ReServeViewModel::class.java]

        val chatViewModel = ViewModelProvider(
            this, ChatViewModel.Factory(database.chatMessageDao())
        )[ChatViewModel::class.java]

        val profileViewModel = ViewModelProvider(
            this, ProfileViewModel.Factory(repository)
        )[ProfileViewModel::class.java]

        // ── Shared GPS ViewModel ──────────────────────────────────────
        val locationViewModel = ViewModelProvider(
            this, LocationViewModel.Factory(application)
        )[LocationViewModel::class.java]

        setContent {
            AppTheme(dynamicColor = false) {
                AppNavigation(
                    viewModel        = viewModel,
                    chatViewModel    = chatViewModel,
                    profileViewModel = profileViewModel,
                    locationViewModel = locationViewModel
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ReServeApp
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ReServeApp(
    onFoodItemClick    : (String) -> Unit = {},
    onNonFoodItemClick : (String) -> Unit = {},
    onEmailClick       : (String, String) -> Unit = { _, _ -> },
    onCartClick        : () -> Unit = {},
    onAddClick         : () -> Unit = {},
    onAllFoodClick     : () -> Unit = {},
    onAllNonFoodClick  : () -> Unit = {},
    onAllGoingSoonClick: () -> Unit = {},
    onAllMyListingsClick: () -> Unit = {},
    onHomeClick        : () -> Unit = {},
    onProfileClick     : () -> Unit = {},
    initialFilter      : String = "All",
    viewModel          : ReServeViewModel,
    chatViewModel      : ChatViewModel = viewModel(),
    locationViewModel  : LocationViewModel = viewModel()          // ← NEW
) {
    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var searchQuery    by remember { mutableStateOf("") }
    var isSearchMode   by remember { mutableStateOf(false) }
    var currentTab     by remember { mutableStateOf("home") }

    val cartItems          by viewModel.cartItems.collectAsStateWithLifecycle()
    val userListedItems    by viewModel.userListedItems.collectAsStateWithLifecycle()
    val reservedQuantities by viewModel.reservedQuantities.collectAsStateWithLifecycle()
    val borrowedItems      by viewModel.borrowedItems.collectAsStateWithLifecycle()

    val allFood    = remember(userListedItems) { viewModel.getFoodItems().map { it.name } }
    val allNonFood = remember(userListedItems) { viewModel.getNonFoodItems().map { it.name } }
    val goingSoon  = remember(userListedItems) { viewModel.getGoingSoon().map { it.name } }

    val filteredResults = remember(searchQuery, userListedItems) {
        if (searchQuery.isBlank()) emptyList()
        else viewModel.searchItems(searchQuery).map { it.name }
    }

    val isSoldOut  = { name: String ->
        val reserved = reservedQuantities[name] ?: 0
        val total    = viewModel.getUserListedItem(name)?.quantity ?: getFoodItemData(name).quantity
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
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                CustomBottomNavigation(
                    onHomeClick = {
                        currentTab     = "home"
                        isSearchMode   = false
                        selectedFilter = "All"
                        searchQuery    = ""
                        onHomeClick()
                    },
                    onSearchClick  = { currentTab = "home"; isSearchMode = true },
                    onEmailClick   = { currentTab = "messages" },
                    onAddClick     = onAddClick,
                    onProfileClick = onProfileClick
                )
            }
        ) { innerPadding ->
            if (currentTab == "messages") {
                ChatInboxScreen(
                    onConversationClick = { owner, item -> onEmailClick(owner, item) },
                    modifier            = Modifier.padding(innerPadding),
                    chatViewModel       = chatViewModel
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (isSearchMode) {
                        Text(
                            "Search Items",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Found ${filteredResults.size} items",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        filteredResults.forEach { item ->
                            FullWidthItemCard(
                                name           = item,
                                imageRes       = getItemImage(item),
                                isSoldOut      = isSoldOut(item),
                                isBorrowed     = isBorrowed(item),
                                photoUriString = viewModel.getPhotoUri(item),
                                onItemClick    = {
                                    val userItem = userListedItems.firstOrNull { it.name == item }
                                    when {
                                        userItem?.category?.equals("Food", ignoreCase = true) == true -> onFoodItemClick(item)
                                        userItem != null -> onNonFoodItemClick(item)
                                        allFood.contains(item) -> onFoodItemClick(item)
                                        else -> onNonFoodItemClick(item)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        // ── Header with live GPS place name ──────────
                        HeaderSection(
                            cartCount         = cartItems.size,
                            onCartClick       = onCartClick,
                            locationViewModel = locationViewModel       // ← NEW
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        FilterSection(selectedFilter) { selectedFilter = it }
                        Spacer(modifier = Modifier.height(24.dp))

                        if (selectedFilter == "All") {
                            PromotionSection()
                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalRow(
                                title       = "Food Items",
                                items       = allFood,
                                viewModel   = viewModel,
                                onItemClick = { onFoodItemClick(it) },
                                onAllClick  = onAllFoodClick
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalRow(
                                title       = "Non-food Items",
                                items       = allNonFood,
                                viewModel   = viewModel,
                                onItemClick = { onNonFoodItemClick(it) },
                                onAllClick  = onAllNonFoodClick
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            HorizontalRow(
                                title       = "Going Soon",
                                items       = goingSoon,
                                viewModel   = viewModel,
                                onItemClick = { onFoodItemClick(it) },
                                onAllClick  = onAllGoingSoonClick
                            )

                            if (userListedItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalRow(
                                    title     = "My Listings",
                                    items     = userListedItems.map { it.name },
                                    viewModel = viewModel,
                                    onItemClick = { itemName ->
                                        val cat = userListedItems.firstOrNull { it.name == itemName }?.category
                                        when {
                                            cat?.equals("Food", ignoreCase = true) == true -> onFoodItemClick(itemName)
                                            cat != null -> onNonFoodItemClick(itemName)
                                            allFood.contains(itemName) -> onFoodItemClick(itemName)
                                            else -> onNonFoodItemClick(itemName)
                                        }
                                    },
                                    onAllClick = onAllMyListingsClick
                                )
                            }
                        } else {
                            val itemsToShow = when (selectedFilter) {
                                "Food"       -> allFood
                                "Non-food"   -> allNonFood
                                "Going Soon" -> goingSoon
                                "My Listings" -> userListedItems.map { it.name }
                                else         -> allFood
                            }
                            Text(
                                "Category: $selectedFilter",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            itemsToShow.forEach { item ->
                                itemsToShow.forEach { item ->
                                    FullWidthItemCard(
                                        name           = item,
                                        imageRes       = getItemImage(item),
                                        isSoldOut      = isSoldOut(item),
                                        isBorrowed     = isBorrowed(item),
                                        photoUriString = viewModel.getPhotoUri(item),
                                        onItemClick    = {
                                            when (selectedFilter) {
                                                "Food"     -> onFoodItemClick(item)
                                                "Non-food" -> onNonFoodItemClick(item)
                                                "My Listings" -> {
                                                    val userItem = userListedItems.firstOrNull { it.name == item }
                                                    if (userItem?.category?.equals("Food", ignoreCase = true) == true)
                                                        onFoodItemClick(item)
                                                    else
                                                        onNonFoodItemClick(item)
                                                }
                                                else -> {
                                                    val userItem = userListedItems.firstOrNull { it.name == item }
                                                    when {
                                                        userItem?.category?.equals("Food", ignoreCase = true) == true -> onFoodItemClick(item)
                                                        userItem != null -> onNonFoodItemClick(item)
                                                        allFood.contains(item) -> onFoodItemClick(item)
                                                        else -> onFoodItemClick(item)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HeaderSection — now shows real GPS place name
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HeaderSection(
    cartCount        : Int,
    onCartClick      : () -> Unit,
    locationViewModel: LocationViewModel = viewModel()
) {
    val context       = LocalContext.current
    val placeName     by locationViewModel.placeName.collectAsStateWithLifecycle()
    val isLoading     by locationViewModel.isLoading.collectAsStateWithLifecycle()
    val userLocation  by locationViewModel.userLocation.collectAsStateWithLifecycle()

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locationViewModel.fetchLocation()
    }

    // Auto-fetch on first composition if permission already granted
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) locationViewModel.fetchLocation()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: location label ─────────────────────────────────────
        Column {
            Text(
                "Listings within 5km",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )

            // Tappable location box
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        // If no permission yet, ask; otherwise refresh
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) locationViewModel.fetchLocation()
                        else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color     = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Locating…",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        placeName != null -> Text(
                            placeName!!,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        else -> Text(
                            "Tap to detect location",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    // Small refresh icon when we already have a location
                    if (userLocation != null && !isLoading) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh location",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // ── Right: cart icon ─────────────────────────────────────────
        BadgedBox(
            badge = {
                if (cartCount > 0) Badge { Text(cartCount.toString()) }
            },
            modifier = Modifier.clickable { onCartClick() }
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = "Cart",
                tint = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Unchanged composables below
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type to search...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(28.dp),
        singleLine = true
    )
}

@Composable
fun HorizontalRow(
    title      : String,
    items      : List<String>,
    onItemClick: (String) -> Unit = {},
    onAllClick : () -> Unit = {},
    viewModel  : ReServeViewModel? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "All >",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clickable { onAllClick() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(items) { name ->
                SmallItemCard(
                    name           = name,
                    imageRes       = getItemImage(name),
                    onClick        = { onItemClick(name) },
                    isSoldOut      = viewModel?.isSoldOut(name) ?: false,
                    isBorrowed     = viewModel?.isBorrowed(name) ?: false,
                    photoUriString = viewModel?.getPhotoUri(name),
                    distance       = viewModel?.getDistance(name) ?: ""
                )
            }
        }
    }
}

@Composable
fun SmallItemCard(
    name           : String,
    imageRes       : Int,
    onClick        : () -> Unit = {},
    isSoldOut      : Boolean = false,
    isBorrowed     : Boolean = false,
    photoUriString : String? = null,
    distance       : String = ""
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column {
            Box {
                when {
                    photoUriString != null -> Image(
                        painter        = rememberAsyncImagePainter(photoUriString),
                        contentDescription = name,
                        modifier       = Modifier.height(100.dp).fillMaxWidth(),
                        contentScale   = ContentScale.Crop
                    )
                    imageRes != R.drawable.blank -> Image(
                        painter        = painterResource(id = imageRes),
                        contentDescription = name,
                        modifier       = Modifier.height(100.dp).fillMaxWidth(),
                        contentScale   = ContentScale.Crop
                    )
                    else -> Box(
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                if (isSoldOut || isBorrowed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = if (isSoldOut) MaterialTheme.colorScheme.error else Color(0xFF5C6BC0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (isSoldOut) "Sold Out" else "Borrowed",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(distance.ifBlank { "Nearby" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FullWidthItemCard(
    name           : String,
    imageRes       : Int,
    onItemClick    : () -> Unit = {},
    isSoldOut      : Boolean = false,
    isBorrowed     : Boolean = false,
    photoUriString : String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            .clickable { expanded = !expanded },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column {
            Box {
                when {
                    photoUriString != null -> Image(
                        painter = rememberAsyncImagePainter(photoUriString),
                        contentDescription = name,
                        modifier = Modifier.height(140.dp).fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    imageRes != R.drawable.blank -> Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = name,
                        modifier = Modifier.height(140.dp).fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Box(
                        modifier = Modifier
                            .height(140.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp))
                    }
                }
                if (isSoldOut || isBorrowed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = if (isSoldOut) MaterialTheme.colorScheme.error else Color(0xFF5C6BC0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (isSoldOut) "Sold Out" else "Borrowed",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    if (isSoldOut) "This item has been fully reserved."
                    else if (isBorrowed) "This item is currently being borrowed."
                    else "Available for reservation near you. Tap the button below to reserve this item before it's gone!",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isSoldOut && !isBorrowed) {
                    Button(
                        onClick = { onItemClick() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View Details →", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun getItemImage(name: String): Int = when (name) {
    "Apple"      -> R.drawable.apple
    "Bread"      -> R.drawable.bread
    "Milk"       -> R.drawable.milk
    "Cake"       -> R.drawable.cake
    "Banana"     -> R.drawable.banana
    "Pizza"      -> R.drawable.pizza
    "Guitar"     -> R.drawable.guitar
    "Trampoline" -> R.drawable.trampoline
    "Plant Pot"  -> R.drawable.plantpot
    "Chair"      -> R.drawable.chair
    "Table"      -> R.drawable.table
    "Books"      -> R.drawable.books
    else         -> R.drawable.blank
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Food", "Non-food", "Going Soon", "My Listings").forEach { tag ->
            FilterChip(
                selected = selected == tag,
                onClick  = { onSelect(tag) },
                label    = { Text(tag) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                    labelColor             = Color.White
                )
            )
        }
    }
}

@Composable
fun PromotionSection() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PromoCard("One person's surplus is another's treasure", MaterialTheme.colorScheme.tertiaryContainer) }
        item { PromoCard("Eco-friendly sharing!", MaterialTheme.colorScheme.secondaryContainer) }
    }
}

@Composable
fun PromoCard(text: String, color: Color) {
    Card(
        modifier = Modifier.size(240.dp, 100.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f))
    ) {
        Box(Modifier.padding(16.dp).fillMaxSize()) {
            Text(text, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CustomBottomNavigation(
    onHomeClick   : () -> Unit,
    onSearchClick : () -> Unit,
    onEmailClick  : () -> Unit,
    onAddClick    : () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = onHomeClick)   { Icon(Icons.Default.Home,   null, tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) }
            FloatingActionButton(
                onClick         = onAddClick,
                containerColor  = MaterialTheme.colorScheme.primary,
                shape           = RoundedCornerShape(50)
            ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary) }
            IconButton(onClick = onProfileClick) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = onEmailClick)   { Icon(Icons.Default.Email,  null, tint = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReServeAppPreview() {
    AppTheme(dynamicColor = false) {
        ReServeApp(onEmailClick = { _, _ -> }, viewModel = viewModel())
    }
}