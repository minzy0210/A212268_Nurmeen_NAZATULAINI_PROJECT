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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.LocationOn

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AddItemScreen(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = {},
    onViewItem: (String, String) -> Unit = { _, _ -> },
    viewModel: ReServeViewModel = viewModel()
) {
    var itemName        by remember { mutableStateOf("") }
    var location        by remember { mutableStateOf("") }
    var category        by remember { mutableStateOf("") }
    var description     by remember { mutableStateOf("") }
    var photoUri        by remember { mutableStateOf<Uri?>(null) }
    var submitted       by remember { mutableStateOf(false) }

    // Food
    var quantity        by remember { mutableStateOf("") }
    var expiresIn       by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("") }
    var originalPrice   by remember { mutableStateOf("") }

    // Non-food
    var deposit         by remember { mutableStateOf("") }
    var maxBorrowDays   by remember { mutableStateOf("") }
    var condition       by remember { mutableStateOf("") }
    var availableUntil  by remember { mutableStateOf("") }

    // Calendar overlay state
    var showExpiresCalendar   by remember { mutableStateOf(false) }
    var showAvailableCalendar by remember { mutableStateOf(false) }
   var calendarMonth         by remember { mutableStateOf(YearMonth.now()) }
    // For max borrow days we pick start + end to calculate day count
   val today = remember { LocalDate.now() }
    val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { /* ignore */ }
            photoUri = uri
        }
    }

    val isFormValid = itemName.isNotBlank() &&
            location.isNotBlank() && category.isNotBlank() && description.isNotBlank() &&
            if (category == "Food")
                quantity.isNotBlank() && expiresIn.isNotBlank() &&
                        discountPercent.isNotBlank() && originalPrice.isNotBlank()
            else
                deposit.isNotBlank() && maxBorrowDays.isNotBlank() && condition.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.70f)))

        if (submitted) {
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
                Text("Item Listed!", style = MaterialTheme.typography.displaySmall.copy(color = Color.White, fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(8.dp))
                Text(itemName, style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.85f)), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Your item is now visible to the community.", color = Color.White.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Back to Listings", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { onViewItem(itemName, category) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) { Text("View My Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                Spacer(Modifier.height(14.dp))
                TextButton(onClick = {
                    itemName = ""; location = ""; category = ""
                    description = ""; photoUri = null
                    quantity = ""; expiresIn = ""; discountPercent = ""; originalPrice = ""
                    deposit = ""; maxBorrowDays = ""; condition = ""; availableUntil = ""
                    submitted = false
                }) { Text("Add Another Item", color = Color.White.copy(alpha = 0.65f)) }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top bar
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                    Row(
                        modifier = Modifier.padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("List an Item", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Photo
                    SectionCard {
                        Text("Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth().height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { photoLauncher.launch(arrayOf("image/*")) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(photoUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Tap to upload a photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Basic Info
                    SectionCard {
                        Text("Basic Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        FormTextField(value = itemName,    onValueChange = { itemName = it },    label = "Item Name",   icon = Icons.Default.Info)
                        Spacer(Modifier.height(12.dp))
                        FormTextField(value = location,    onValueChange = { location = it },    label = "Your Location (e.g. 1.2km, Taman Mulia)", icon = Icons.Default.LocationOn)
                        Spacer(Modifier.height(8.dp))
                        GpsLocationButton(onLocationFetched = { location = it })
                        Spacer(Modifier.height(12.dp))
                        FormTextField(value = description, onValueChange = { description = it }, label = "Description", icon = Icons.Default.Edit, singleLine = false, minLines = 3)
                    }

                    // Category
                    SectionCard {
                        Text("Category", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("Food", "Non-Food").forEach { cat ->
                                val selected = category == cat
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { category = cat },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (cat == "Food") Icons.Default.ShoppingCart else Icons.Default.Star,
                                            null,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(cat,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Food Details ──────────────────────────────────
                    AnimatedVisibility(visible = category == "Food") {
                        SectionCard {
                            Text("Food Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))
                            FormTextField(value = quantity,        onValueChange = { quantity = it },        label = "Quantity (units)", icon = Icons.Default.List)
                            Spacer(Modifier.height(12.dp))
                            FormTextField(value = originalPrice,   onValueChange = { originalPrice = it },   label = "Original Price (RM)", icon = Icons.Default.Info)
                            Spacer(Modifier.height(12.dp))
                            FormTextField(value = discountPercent, onValueChange = { discountPercent = it }, label = "Discount (%)", icon = Icons.Default.Star)
                            Spacer(Modifier.height(12.dp))

                            // Expires date picker button
                            Text("Expiry Date", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    calendarMonth = YearMonth.now()
                                    showExpiresCalendar = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp,
                                    if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp),
                                    tint = if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (expiresIn.isNotBlank()) expiresIn else "Select expiry date",
                                    color = if (expiresIn.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ── Non-Food Details ──────────────────────────────
                    AnimatedVisibility(visible = category == "Non-Food") {
                        SectionCard {
                            Text("Borrow Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(12.dp))

                            // Condition
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
                                        Text(cond,
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
                            FormTextField(value = deposit, onValueChange = { deposit = it }, label = "Refundable Deposit (RM, 0 if free)", icon = Icons.Default.Info)
                            Spacer(Modifier.height(12.dp))

                            // Max Borrow Days — calendar range picker
                            FormTextField(
                                value = maxBorrowDays,
                                onValueChange = { maxBorrowDays = it.filter { c -> c.isDigit() } },
                                label = "Max Borrow Days (e.g. 7)",
                                icon = Icons.Default.DateRange
                            )
                            Spacer(Modifier.height(12.dp))

                            // Available Until — single date picker
                            Text("Available Until", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    calendarMonth = YearMonth.now()
                                    showAvailableCalendar = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp,
                                    if (availableUntil.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp),
                                    tint = if (availableUntil.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (availableUntil.isNotBlank()) availableUntil else "Select available until date",
                                    color = if (availableUntil.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Submit
                    Button(
                        onClick = {
                            if (isFormValid) {
                                viewModel.addUserItem(
                                    UserListedItem(
                                        name            = itemName,
                                        category        = category,
                                        photoUri        = photoUri?.toString(),
                                        sellerName      = "Me",
                                        location        = location,
                                        quantity        = quantity.toIntOrNull() ?: 1,
                                        originalPrice   = originalPrice.toDoubleOrNull() ?: 0.0,
                                        discountPercent = discountPercent.toIntOrNull() ?: 0,
                                        expiresIn       = expiresIn.ifBlank { "Ongoing" },
                                        description     = description,
                                        deposit         = deposit.toDoubleOrNull() ?: 0.0,
                                        maxBorrowDays   = maxBorrowDays.toIntOrNull() ?: 7,
                                        condition       = condition.ifBlank { "Good" },
                                        availableUntil  = availableUntil.ifBlank { "Ongoing" }
                                    )
                                )
                                submitted = true
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
                        Icon(Icons.Default.AddCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                category.isEmpty() -> "Select a category first"
                                !isFormValid       -> "Fill in all fields"
                                else               -> "List My Item"
                            },
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Expires Calendar Overlay ───────────────────────────────────
        AnimatedVisibility(
            visible = showExpiresCalendar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            CalendarOverlay(
                title        = "Select Expiry Date",
                today        = today,
                month        = calendarMonth,
                onMonthChange = { calendarMonth = it },
                // single-date mode: start == end, no range
                startDate    = null,
                endDate      = null,
                singleDate   = true,
                onDayClick   = { date ->
                    expiresIn = date.format(displayFmt)
                    showExpiresCalendar = false
                },
                onDismiss    = { showExpiresCalendar = false }
            )
        }


        // ── Available Until Calendar Overlay ──────────────────────────
        AnimatedVisibility(
            visible = showAvailableCalendar,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            CalendarOverlay(
                title         = "Select Available Until",
                today         = today,
                month         = calendarMonth,
                onMonthChange = { calendarMonth = it },
                startDate     = null,
                endDate       = null,
                singleDate    = true,
                onDayClick    = { date ->
                    availableUntil = date.format(displayFmt)
                    showAvailableCalendar = false
                },
                onDismiss = { showAvailableCalendar = false }
            )
        }
    }
}

@Composable
fun GpsLocationButton(
    onLocationFetched: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val locationHelper = remember { LocationHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isFetching = true
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                isFetching = false
                if (loc != null) {
                    val distance = locationHelper.formatDistance(
                        loc.latitude, loc.longitude, 3.1234, 101.5678
                    )
                    onLocationFetched(distance)
                    error = null
                } else {
                    error = "Could not get location. Make sure GPS is on."
                }
            }
        } else {
            error = "Location permission denied."
        }
    }

    fun checkAndFetch() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            isFetching = true
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                isFetching = false
                if (loc != null) {
                    val distance = locationHelper.formatDistance(
                        loc.latitude, loc.longitude, 3.1234, 101.5678
                    )
                    onLocationFetched(distance)
                    error = null
                } else {
                    error = "Could not get location. Make sure GPS is on."
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { checkAndFetch() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isFetching
        ) {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isFetching) "Getting location..." else "Use My Current Location")
        }
        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
// ── Reusable calendar overlay composable ──────────────────────────────
@Composable
private fun CalendarOverlay(
    title: String,
    today: LocalDate,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    singleDate: Boolean,
    onDayClick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
            }

            // Calendar card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Day headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Su","Mo","Tu","We","Th","Fr","Sa").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Grid
                    val firstDay      = month.atDay(1)
                    val startDow      = firstDay.dayOfWeek.value % 7
                    val daysInMonth   = month.lengthOfMonth()
                    val rows          = (startDow + daysInMonth + 6) / 7

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNum = row * 7 + col - startDow + 1
                                if (dayNum < 1 || dayNum > daysInMonth) {
                                    Box(modifier = Modifier.weight(1f).height(40.dp))
                                } else {
                                    val date       = month.atDay(dayNum)
                                    val isPast     = date.isBefore(today)
                                    val isStart    = date == startDate
                                    val isEnd      = date == endDate
                                    val isInRange  = !singleDate && startDate != null && endDate != null &&
                                            date.isAfter(startDate) && date.isBefore(endDate)
                                    val isSelected = isStart || isEnd
                                    val isToday    = date == today

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isInRange  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else       -> Color.Transparent
                                                },
                                                shape = when {
                                                    isStart && endDate != null -> RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                                                    isEnd                      -> RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
                                                    isInRange                  -> RoundedCornerShape(0.dp)
                                                    isSelected                 -> CircleShape
                                                    else                       -> RoundedCornerShape(0.dp)
                                                }
                                            )
                                            .clickable(enabled = !isPast) { onDayClick(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isPast     -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday    -> MaterialTheme.colorScheme.primary
                                                isInRange  -> MaterialTheme.colorScheme.primary
                                                else       -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hint text
            Spacer(Modifier.height(20.dp))
            Text(
                if (singleDate) "Tap a date to select"
                else if (startDate == null) "Tap a start date"
                else "Now tap the end date",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun FormTextField(
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