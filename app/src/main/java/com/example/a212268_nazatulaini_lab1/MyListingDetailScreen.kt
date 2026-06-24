package com.example.a212268_nazatulaini_lab1

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

private enum class MyListingDialog { NONE, DELETE, SOLD }
private enum class MyListingOverlay { NONE, RESERVE_CONFIRM, RESERVED, BORROW_CONFIRM, BORROWED }

// ── Helper: detect coordinate string and reverse-geocode it ───────────────

@Composable
private fun rememberDisplayLocation(rawLocation: String): String {
    val context = LocalContext.current
    var displayLocation by remember(rawLocation) { mutableStateOf(rawLocation) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(rawLocation) {
        // Matches patterns like "3.01580, 101.78800" or "3.1234,101.5678"
        val coordRegex = Regex("""^-?\d+(\.\d+)?\s*,\s*-?\d+(\.\d+)?$""")
        if (coordRegex.matches(rawLocation.trim())) {
            scope.launch {
                val parts = rawLocation.split(",").map { it.trim().toDoubleOrNull() }
                if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                    val helper = LocationHelper(context)
                    val name = helper.getPlaceName(parts[0]!!, parts[1]!!)
                    if (name.isNotBlank()) displayLocation = name
                }
            }
        }
        // If it's not coordinates (e.g. "1.2km, Taman Mulia"), show as-is
    }

    return displayLocation
}

@Composable
fun MyListingDetailScreen(
    itemName: String,
    category: String,
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: ReServeViewModel,
    onEdit: () -> Unit
) {
    val userListedItems    by viewModel.userListedItems.collectAsStateWithLifecycle()
    val reservedQuantities by viewModel.reservedQuantities.collectAsStateWithLifecycle()
    val borrowedItems      by viewModel.borrowedItems.collectAsStateWithLifecycle()

    val userItem      = userListedItems.firstOrNull { it.name == itemName }
    val reservedCount = reservedQuantities[itemName] ?: 0

    // ── Early return: not found / loading ──────────────────────────────
    if (userItem == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.wallpaper),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val confirmed = userListedItems.any { it.name == itemName }
                if (confirmed || userListedItems.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Listing not found", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        return
    }

    val item = userItem

    val reservedQuantitiesMap = reservedQuantities
    val borrowedItemsSet = borrowedItems

    var showDialog   by remember { mutableStateOf(MyListingDialog.NONE) }
    var overlay      by remember { mutableStateOf(MyListingOverlay.NONE) }
    var isMarkedSold by remember { mutableStateOf(false) }
    var quantity     by remember { mutableIntStateOf(1) }

    val isFood = category.equals("Food", ignoreCase = true)
    val discountedPrice = if (isFood)
        item.originalPrice * (1 - item.discountPercent / 100.0)
    else 0.0

    val remainingStock = viewModel.getRemainingStock(itemName)
    val isSoldOut      = viewModel.isSoldOut(itemName)
    val isBorrowed     = itemName in borrowedItemsSet

    if (quantity > remainingStock && remainingStock > 0) quantity = remainingStock

    // ── Resolve location: coordinates → place name, or show as-is ────
    val displayLocation = rememberDisplayLocation(item.location)

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.wallpaper),
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
                    onHomeClick   = onHomeClick,
                    onSearchClick = onHomeClick,
                    onEmailClick  = onHomeClick,
                    onAddClick    = onHomeClick
                )
            }
        ) { innerPadding ->

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Hero Image ────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {

                    if (userItem.photoUri != null) {
                        val painter = rememberAsyncImagePainter(
                            model = userItem.photoUri,
                            error = painterResource(getItemImage(itemName)),
                            fallback = painterResource(getItemImage(itemName))
                        )
                        Image(
                            painter = painter,
                            contentDescription = itemName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(getItemImage(itemName)),
                            contentDescription = itemName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.45f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("MY LISTING", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (isFood && item.discountPercent > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 100.dp, start = 16.dp),
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "-${item.discountPercent}%",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        if (isMarkedSold || isSoldOut || isBorrowed) {
                            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    if (isBorrowed) "BORROWED" else "SOLD OUT",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            itemName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp
                            )
                        )
                    }
                }

                // ── Info Card ──────────────────────────────────────────
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                            .padding(bottom = innerPadding.calculateBottomPadding() + 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        // Active / Sold status banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isMarkedSold)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else
                                Color(0xFF1B5E20).copy(alpha = 0.12f)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isMarkedSold) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    null,
                                    tint = if (isMarkedSold) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (isMarkedSold) "This listing is marked as sold / unavailable"
                                    else "Active listing — visible to the community",
                                    color = if (isMarkedSold) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Price / Deposit row
                        if (isFood) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Discounted Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "RM %.2f".format(discountedPrice),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "RM %.2f".format(item.originalPrice),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = TextDecoration.LineThrough,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        "Expires: ${item.expiresIn}",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val condColor = when (item.condition) {
                                "Excellent" -> Color(0xFF2E7D32)
                                "Good"      -> Color(0xFF1565C0)
                                else        -> Color(0xFFE65100)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Refundable Deposit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (item.deposit == 0.0) "FREE" else "RM %.2f".format(item.deposit),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.deposit == 0.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                Surface(color = condColor.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        item.condition,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = condColor, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        // Listing details grid
                        Text("Listing Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // ── Show resolved place name, not raw coordinates ──
                                DetailRow(Icons.Default.LocationOn, "Location", displayLocation)
                                DetailRow(Icons.Default.Info, "Category", item.category)
                                if (isFood) {
                                    DetailRow(Icons.Default.List,      "Quantity", "${item.quantity} units")
                                    DetailRow(Icons.Default.DateRange, "Expiry",   item.expiresIn)
                                } else {
                                    DetailRow(Icons.Default.DateRange,   "Max Borrow",      "${item.maxBorrowDays} days")
                                    DetailRow(Icons.Default.CheckCircle, "Available Until", item.availableUntil)
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text("Description", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp
                        )

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        Text("Listing Performance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard(Modifier.weight(1f), Icons.Default.Favorite, "12", "Views",    MaterialTheme.colorScheme.primary)
                            StatCard(Modifier.weight(1f), Icons.Default.Email,    "3",  "Messages", MaterialTheme.colorScheme.tertiary)
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon     = if (isFood) Icons.Default.ShoppingCart else Icons.Default.Person,
                                value    = if (isFood) "$reservedCount"
                                else if (isBorrowed) "1" else "0",
                                label    = if (isFood) "Reserved" else "Borrowed",
                                color    = Color(0xFF2E7D32)
                            )
                        }

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        if (isFood) {
                            Text("Select Quantity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedIconButton(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text("$quantity", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    OutlinedIconButton(
                                        onClick = { if (quantity < remainingStock) quantity++ },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Text(
                                    if (isSoldOut) "Sold Out" else "Available: $remainingStock",
                                    color = if (isSoldOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick  = { overlay = MyListingOverlay.RESERVE_CONFIRM },
                                enabled  = !isSoldOut && !isMarkedSold,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = if (isSoldOut || isMarkedSold) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    if (isSoldOut) "Sold Out" else "Reserve",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!isBorrowed) overlay = MyListingOverlay.BORROW_CONFIRM
                                },
                                enabled = !isBorrowed && !isMarkedSold,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBorrowed || isMarkedSold) Color.Gray
                                    else MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(
                                    if (isBorrowed) Icons.Default.Lock else Icons.Default.DateRange,
                                    null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isBorrowed) "Currently Borrowed" else "Borrow This Item",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(20.dp))

                        Text("Manage Listing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick  = { showDialog = MyListingDialog.SOLD },
                            enabled  = !isMarkedSold,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (isMarkedSold) Color.Gray else MaterialTheme.colorScheme.secondary,
                                disabledContainerColor = Color.Gray
                            )
                        ) {
                            Icon(if (isMarkedSold) Icons.Default.Lock else Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isMarkedSold) "Already Marked as Sold" else "Mark as Sold / Unavailable",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick  = { showDialog = MyListingDialog.DELETE },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            border   = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Listing", fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // ── Reserve Confirm overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = overlay == MyListingOverlay.RESERVE_CONFIRM,
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.wallpaper), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.80f)))
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(modifier = Modifier.size(110.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
                        if (item.photoUri != null) {
                            Image(rememberAsyncImagePainter(item.photoUri), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painterResource(getItemImage(itemName)), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Confirm Reservation", style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("Are you sure you want to reserve", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("$quantity × $itemName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("for RM %.2f?".format(discountedPrice * quantity), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Spacer(Modifier.height(40.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.13f)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Item", color = Color.White.copy(0.70f), fontSize = 14.sp)
                                Text(itemName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Quantity", color = Color.White.copy(0.70f), fontSize = 14.sp)
                                Text("$quantity unit(s)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Price each", color = Color.White.copy(0.70f), fontSize = 14.sp)
                                Text("RM %.2f".format(discountedPrice), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color.White.copy(0.20f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("RM %.2f".format(discountedPrice * quantity), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = { viewModel.reserveFoodItem(itemName, quantity); overlay = MyListingOverlay.RESERVED },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Yes, Reserve Now", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = { overlay = MyListingOverlay.NONE }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Reserved success overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = overlay == MyListingOverlay.RESERVED,
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.wallpaper), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B5E20).copy(alpha = 0.92f)))
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(130.dp), shape = CircleShape, color = Color.White.copy(0.10f)) {}
                        Surface(modifier = Modifier.size(96.dp),  shape = CircleShape, color = Color.White.copy(0.18f)) {}
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("Reserved!", style = MaterialTheme.typography.displaySmall.copy(color = Color.White, fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.height(10.dp))
                    Text("$quantity × $itemName", style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(0.85f), fontWeight = FontWeight.Medium), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("RM %.2f".format(discountedPrice * quantity), style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.height(36.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.13f)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            InfoRow(Icons.Default.LocationOn, "Pick up from ${item.sellerName}")
                            InfoRow(Icons.Default.DateRange,  "Collect within 2 hours")
                            InfoRow(Icons.Default.Info,       "Show this screen at pickup")
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) { Text("Back to Listings", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = { overlay = MyListingOverlay.NONE }) {
                        Text("View Item Again", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Borrow Confirm overlay ─────────────────────────────────────
        AnimatedVisibility(
            visible = overlay == MyListingOverlay.BORROW_CONFIRM,
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.wallpaper), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.80f)))
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(modifier = Modifier.size(110.dp), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
                        if (item.photoUri != null) {
                            Image(rememberAsyncImagePainter(item.photoUri), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painterResource(getItemImage(itemName)), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Confirm Borrow Request", style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("Are you sure you want to borrow", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(itemName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(40.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.13f)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Max Borrow", color = Color.White.copy(0.70f), fontSize = 14.sp)
                                Text("${item.maxBorrowDays} days", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deposit", color = Color.White.copy(0.70f), fontSize = 14.sp)
                                Text(
                                    if (item.deposit == 0.0) "FREE" else "RM %.2f".format(item.deposit),
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = { viewModel.borrowNonFoodItem(itemName); overlay = MyListingOverlay.BORROWED },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("Yes, Borrow Now", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = { overlay = MyListingOverlay.NONE }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Borrowed success overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = overlay == MyListingOverlay.BORROWED,
            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it }, animationSpec = tween(380, easing = FastOutSlowInEasing)),
            exit  = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.wallpaper), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D47A1).copy(alpha = 0.92f)))
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(modifier = Modifier.size(130.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.10f)) {}
                        Surface(modifier = Modifier.size(96.dp),  shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {}
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("Request Sent!", style = MaterialTheme.typography.displaySmall.copy(color = Color.White, fontWeight = FontWeight.ExtraBold))
                    Spacer(Modifier.height(8.dp))
                    Text(itemName, style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) { Text("Back to Listings", color = Color(0xFF0D47A1), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = { overlay = MyListingOverlay.NONE }) {
                        Text("View Item Again", color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Delete confirmation dialog ─────────────────────────────────
        if (showDialog == MyListingDialog.DELETE) {
            ConfirmDialog(
                iconRes    = Icons.Default.Delete,
                iconBg     = MaterialTheme.colorScheme.errorContainer,
                iconTint   = MaterialTheme.colorScheme.error,
                title      = "Delete Listing?",
                body       = "\"$itemName\" will be permanently removed from the community listings.",
                confirmText  = "Delete",
                confirmColor = MaterialTheme.colorScheme.error,
                onDismiss  = { showDialog = MyListingDialog.NONE },
                onConfirm  = {
                    viewModel.deleteUserItem(item)
                    showDialog = MyListingDialog.NONE
                    onDeleted()
                }
            )
        }

        if (showDialog == MyListingDialog.SOLD) {
            ConfirmDialog(
                iconRes      = Icons.Default.CheckCircle,
                iconBg       = Color(0xFF1B5E20).copy(alpha = 0.15f),
                iconTint     = Color(0xFF2E7D32),
                title        = "Mark as Sold?",
                body         = "\"$itemName\" will be marked as sold / unavailable. Others won't be able to reserve it anymore.",
                confirmText  = "Confirm",
                confirmColor = Color(0xFF2E7D32),
                onDismiss    = { showDialog = MyListingDialog.NONE },
                onConfirm    = { isMarkedSold = true; showDialog = MyListingDialog.NONE }
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    iconRes: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    body: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = iconBg) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(iconRes, null, tint = iconTint, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) { Text(confirmText, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, value: String, label: String, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.10f)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White.copy(alpha = 0.90f), fontSize = 14.sp)
    }
}