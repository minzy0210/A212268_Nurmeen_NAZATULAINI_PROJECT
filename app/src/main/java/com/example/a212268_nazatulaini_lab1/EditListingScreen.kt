package com.example.a212268_nazatulaini_lab1

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@Composable
fun EditListingScreen(
    itemName: String,
    onBack: () -> Unit,
    onSaved: (String, String) -> Unit, // (newName, category)
    viewModel: ReServeViewModel
) {
    val userItem = viewModel.getUserListedItem(itemName) ?: run {
        onBack(); return
    }

    val context = LocalContext.current
    val isFood = userItem.category.equals("Food", ignoreCase = true)

    // ── Fields ─────────────────────────────────────────────────────────
    var name            by remember { mutableStateOf(userItem.name) }
    var location        by remember { mutableStateOf(userItem.location) }
    var description     by remember { mutableStateOf(userItem.description) }
    var photoUri        by remember { mutableStateOf<Uri?>(userItem.photoUri?.let { Uri.parse(it) }) }

    // Food
    var quantity        by remember { mutableStateOf(userItem.quantity.toString()) }
    var originalPrice   by remember { mutableStateOf(userItem.originalPrice.toString()) }
    var discountPercent by remember { mutableStateOf(userItem.discountPercent.toString()) }
    var expiresIn       by remember { mutableStateOf(userItem.expiresIn) }

    // Non-food
    var deposit         by remember { mutableStateOf(userItem.deposit.toString()) }
    var maxBorrowDays   by remember { mutableStateOf(userItem.maxBorrowDays.toString()) }
    var condition       by remember { mutableStateOf(userItem.condition) }
    var availableUntil  by remember { mutableStateOf(userItem.availableUntil) }

    // Calendar state
    var showExpiresCalendar   by remember { mutableStateOf(false) }
    var showAvailableCalendar by remember { mutableStateOf(false) }
    var calendarMonth         by remember { mutableStateOf(YearMonth.now()) }
    val today = remember { LocalDate.now() }
    val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    var saved by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            photoUri = uri
        }
    }

    val isFormValid = name.isNotBlank() && location.isNotBlank() && description.isNotBlank() &&
            if (isFood) quantity.isNotBlank() && originalPrice.isNotBlank() && discountPercent.isNotBlank()
            else deposit.isNotBlank() && maxBorrowDays.isNotBlank() && condition.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.70f)))

        // ── Success screen ──────────────────────────────────────────────
        if (saved) {
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
                Text(
                    "Listing Updated!",
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = Color.White, fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.85f)),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your changes are now live in the community.",
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = { onSaved(name, userItem.category) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("View My Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onBack) {
                    Text("Back to Listings", color = Color.White.copy(alpha = 0.65f))
                }
            }
        } else {
            // ── Form ────────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(
                                "Edit Listing",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                itemName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Photo ─────────────────────────────────────────
                    EditSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { photoLauncher.launch(arrayOf("image/*")) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                photoUri != null -> {
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.Black.copy(alpha = 0.55f)
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Change Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                                userItem.photoUri != null -> {
                                    // show existing stored photo
                                    Image(
                                        painter = rememberAsyncImagePainter(userItem.photoUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.55f)) {
                                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Change Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tap to add a photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // ── Basic Info ────────────────────────────────────
                    EditSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Basic Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(12.dp))
                        EditFormTextField(value = name, onValueChange = { name = it }, label = "Item Name", icon = Icons.Default.Info)
                        Spacer(Modifier.height(12.dp))
                        EditFormTextField(value = location, onValueChange = { location = it }, label = "Location", icon = Icons.Default.LocationOn)
                        Spacer(Modifier.height(12.dp))
                        EditFormTextField(value = description, onValueChange = { description = it }, label = "Description", icon = Icons.Default.Edit, singleLine = false, minLines = 3)
                    }

                    // ── Category label (read-only) ────────────────────
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isFood) Icons.Default.ShoppingCart else Icons.Default.Star,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Category: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                userItem.category,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "Cannot change category",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Food-specific fields ──────────────────────────
                    if (isFood) {
                        EditSectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Food Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(12.dp))
                            EditFormTextField(value = quantity, onValueChange = { quantity = it }, label = "Quantity (units)", icon = Icons.Default.List)
                            Spacer(Modifier.height(12.dp))
                            EditFormTextField(value = originalPrice, onValueChange = { originalPrice = it }, label = "Original Price (RM)", icon = Icons.Default.Info)
                            Spacer(Modifier.height(12.dp))
                            EditFormTextField(value = discountPercent, onValueChange = { discountPercent = it }, label = "Discount (%)", icon = Icons.Default.Star)
                            Spacer(Modifier.height(12.dp))

                            // Discounted price preview
                            val origD = originalPrice.toDoubleOrNull() ?: 0.0
                            val discD = discountPercent.toIntOrNull() ?: 0
                            if (origD > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discounted price:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                        Text(
                                            "RM %.2f".format(origD * (1 - discD / 100.0)),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            Text("Expiry Date", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { calendarMonth = YearMonth.now(); showExpiresCalendar = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp,
                                    if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp),
                                    tint = if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(expiresIn.ifBlank { "Select expiry date" },
                                    color = if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // ── Non-food specific fields ──────────────────────
                    if (!isFood) {
                        EditSectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Borrow Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(12.dp))

                            Text("Condition", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Excellent", "Good", "Fair").forEach { cond ->
                                    val selected = condition == cond
                                    val condColor = when (cond) {
                                        "Excellent" -> Color(0xFF2E7D32)
                                        "Good"      -> Color(0xFF1565C0)
                                        else        -> Color(0xFFE65100)
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable { condition = cond },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) condColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (selected) BorderStroke(2.dp, condColor) else null
                                    ) {
                                        Text(
                                            cond,
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) condColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            EditFormTextField(value = deposit, onValueChange = { deposit = it }, label = "Refundable Deposit (RM, 0 if free)", icon = Icons.Default.Info)
                            Spacer(Modifier.height(12.dp))
                            EditFormTextField(
                                value = maxBorrowDays,
                                onValueChange = { maxBorrowDays = it.filter { c -> c.isDigit() } },
                                label = "Max Borrow Days",
                                icon = Icons.Default.DateRange
                            )
                            Spacer(Modifier.height(12.dp))

                            Text("Available Until", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { calendarMonth = YearMonth.now(); showAvailableCalendar = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp,
                                    if (availableUntil.isNotBlank() && availableUntil != "Ongoing") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(availableUntil.ifBlank { "Select available until date" },
                                    color = if (availableUntil.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // ── Save button ───────────────────────────────────
                    Button(
                        onClick = {
                            if (isFormValid) {
                                val updatedItem = userItem.copy(
                                    name            = name,
                                    location        = location,
                                    description     = description,
                                    photoUri        = photoUri?.toString() ?: userItem.photoUri,
                                    quantity        = quantity.toIntOrNull() ?: userItem.quantity,
                                    originalPrice   = originalPrice.toDoubleOrNull() ?: userItem.originalPrice,
                                    discountPercent = discountPercent.toIntOrNull() ?: userItem.discountPercent,
                                    expiresIn       = expiresIn.ifBlank { userItem.expiresIn },
                                    deposit         = deposit.toDoubleOrNull() ?: userItem.deposit,
                                    maxBorrowDays   = maxBorrowDays.toIntOrNull() ?: userItem.maxBorrowDays,
                                    condition       = condition.ifBlank { userItem.condition },
                                    availableUntil  = availableUntil.ifBlank { userItem.availableUntil }
                                )
                                viewModel.updateUserItem(updatedItem)
                                saved = true
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (!isFormValid) "Fill in all required fields" else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Expires calendar overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = showExpiresCalendar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            EditCalendarOverlay(
                title        = "Select Expiry Date",
                today        = today,
                month        = calendarMonth,
                onMonthChange = { calendarMonth = it },
                onDayClick   = { date ->
                    expiresIn = date.format(displayFmt)
                    showExpiresCalendar = false
                },
                onDismiss = { showExpiresCalendar = false }
            )
        }

        // ── Available Until calendar overlay ────────────────────────────
        AnimatedVisibility(
            visible = showAvailableCalendar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            EditCalendarOverlay(
                title        = "Select Available Until",
                today        = today,
                month        = calendarMonth,
                onMonthChange = { calendarMonth = it },
                onDayClick   = { date ->
                    availableUntil = date.format(displayFmt)
                    showAvailableCalendar = false
                },
                onDismiss = { showAvailableCalendar = false }
            )
        }
    }
}

// ── Calendar overlay ───────────────────────────────────────────────────────

@Composable
private fun EditCalendarOverlay(
    title: String,
    today: LocalDate,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)))

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            month.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()) + " " + month.year,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Su","Mo","Tu","We","Th","Fr","Sa").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val firstDay = month.atDay(1)
                    val startDow = firstDay.dayOfWeek.value % 7
                    val daysInMonth = month.lengthOfMonth()
                    val rows = (startDow + daysInMonth + 6) / 7

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNum = row * 7 + col - startDow + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).height(40.dp))
                                } else {
                                    val date = month.atDay(dayNum)
                                    val isPast = date.isBefore(today)
                                    val isToday = date == today

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(
                                                color = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = !isPast) { onDayClick(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 14.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isPast  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else    -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tap a date to select",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

@Composable
private fun EditSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun EditFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}