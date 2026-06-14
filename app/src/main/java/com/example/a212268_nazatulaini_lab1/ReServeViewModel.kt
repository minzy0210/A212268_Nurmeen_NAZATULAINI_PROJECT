package com.example.a212268_nazatulaini_lab1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.a212268_nazatulaini_lab1.data.ReServeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

// 5. VIEWMODEL — calls repository methods, exposes StateFlow to the UI.
//    No direct database or DAO imports here.
class ReServeViewModel(
    private val repository: ReServeRepository
) : ViewModel() {

    // ── Static (hardcoded) items — unchanged ─────────────────────────
    private val _items = MutableStateFlow(
        listOf(
            Item("Apple", "Food"), Item("Bread", "Food"), Item("Milk", "Food"),
            Item("Cake", "Food"), Item("Banana", "Food"), Item("Pizza", "Food"),
            Item("Guitar", "Non-food"), Item("Trampoline", "Non-food"),
            Item("Plant Pot", "Non-food"), Item("Chair", "Non-food"),
            Item("Table", "Non-food"), Item("Books", "Non-food")
        )
    )
    val items: StateFlow<List<Item>> = _items

    // ── User-listed items — now from Room via repository ─────────────
    // stateIn() converts the cold Flow from the DB into a hot StateFlow
    // the UI can collect with collectAsStateWithLifecycle()
    val userListedItems: StateFlow<List<UserListedItem>> =
        repository.userListedItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Cart items — now persisted in Room ───────────────────────────
    val cartItems: StateFlow<List<CartItem>> =
        repository.cartItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val reservedQuantities: StateFlow<Map<String, Int>> =
        repository.reservations
            .map { list -> list.associate { it.itemName to it.quantity } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val borrowedItems: StateFlow<Set<String>> =
        repository.borrowedItems
            .map { list -> list.map { it.itemName }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // ── Replace these two functions ───────────────────────────────────────
    fun reserveFoodItem(itemName: String, quantity: Int) {
        viewModelScope.launch { repository.reserveItem(itemName, quantity) }
    }

    fun borrowNonFoodItem(itemName: String) {
        viewModelScope.launch { repository.borrowItem(itemName) }
    }
    // ── UserListedItem operations ─────────────────────────────────────

    /** Called from AddItemScreen — launches a coroutine to insert into Room */
    fun addUserItem(item: UserListedItem) {
        val normalized = item.copy(
            category = if (item.category.equals("food", ignoreCase = true)) "Food" else "Non-food"
        )
        viewModelScope.launch { repository.addUserItem(normalized) }
    }

    fun deleteUserItem(item: UserListedItem) {
        viewModelScope.launch { repository.deleteUserItem(item) }
    }

    fun updateUserItem(item: UserListedItem) {
        viewModelScope.launch { repository.updateUserItem(item) }
    }

    // Synchronous helpers (read from the already-loaded StateFlow value)
    fun getUserListedItem(name: String): UserListedItem? =
        userListedItems.value.firstOrNull { it.name == name }

    fun getPhotoUri(name: String): String? =
        userListedItems.value.firstOrNull { it.name == name }?.photoUri

    fun getDistance(name: String): String {
        userListedItems.value.firstOrNull { it.name == name }?.let { item ->
            val parts = item.location.split(",").map { it.trim().toDoubleOrNull() }
            return if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                com.example.a212268_nazatulaini_lab1.DistanceUtils.formatDistance(
                    parts[0]!!, parts[1]!!, 3.1234, 101.5678
                )
            } else {
                item.location
            }
        }
        return when (name) {
            "Apple" -> "1.2km"; "Bread" -> "0.8km"; "Milk" -> "2.1km"
            "Cake" -> "3.5km"; "Banana" -> "0.5km"; "Pizza" -> "1.9km"
            "Guitar" -> "2.3km"; "Trampoline" -> "4.1km"; "Plant Pot" -> "0.9km"
            "Chair" -> "1.5km"; "Table" -> "3.2km"; "Books" -> "1.1km"
            else -> "N/A"
        }
    }

    // ── Cart operations ───────────────────────────────────────────────

    fun addToCart(itemName: String, quantity: Int = 1) {
        viewModelScope.launch {
            val foodItem = getFoodItemData(itemName)
            val discountedPrice = foodItem.originalPrice * (1 - foodItem.discountPercent / 100.0)
            val imageRes = getItemImage(itemName)

            val existing = cartItems.value.find { it.name == itemName }
            if (existing != null) {
                repository.updateCartItem(existing.copy(quantity = existing.quantity + quantity))
            } else {
                repository.addToCart(
                    CartItem(name = itemName, imageRes = imageRes, price = discountedPrice, quantity = quantity)
                )
            }
        }
    }

    fun removeFromCart(itemName: String) {
        viewModelScope.launch { repository.removeFromCart(itemName) }
    }

    fun clearCart() {
        viewModelScope.launch { repository.clearCart() }
    }

    fun decrementCart(itemName: String) {
        viewModelScope.launch {
            val existing = cartItems.value.find { it.name == itemName } ?: return@launch
            if (existing.quantity > 1) {
                repository.updateCartItem(existing.copy(quantity = existing.quantity - 1))
            } else {
                repository.removeFromCart(itemName)
            }
        }
    }

    fun getCartTotal(): Double = cartItems.value.sumOf { it.price * it.quantity }

    // ── Reservation / borrow (in-memory) ─────────────────────────────

    fun getRemainingStock(itemName: String): Int {
        val reserved = reservedQuantities.value[itemName] ?: 0   // ← use StateFlow value
        getUserListedItem(itemName)?.let { userItem ->
            if (userItem.category.equals("Food", ignoreCase = true)) {
                return (userItem.quantity - reserved).coerceAtLeast(0)
            }
        }
        val originalQty = getFoodItemData(itemName).quantity
        return (originalQty - reserved).coerceAtLeast(0)
    }

    fun isSoldOut(itemName: String) = getRemainingStock(itemName) <= 0

    fun isBorrowed(itemName: String) = itemName in borrowedItems.value
    // ── Category helpers ──────────────────────────────────────────────

    fun getFoodItems(): List<Item> {
        val base = _items.value.filter { it.category == "Food" }
        val userFood = userListedItems.value
            .filter { it.category.equals("Food", ignoreCase = true) }
            .map { Item(it.name, "Food") }
        return base + userFood
    }

    fun getNonFoodItems(): List<Item> {
        val base = _items.value.filter { it.category == "Non-food" }
        val userNonFood = userListedItems.value
            .filter { it.category.equals("Non-food", ignoreCase = true) ||
                    it.category.equals("Non-Food", ignoreCase = true) }
            .map { Item(it.name, "Non-food") }
        return base + userNonFood
    }

    fun searchItems(query: String): List<Item> {
        if (query.isBlank()) return emptyList()
        val all = _items.value + userListedItems.value.map { Item(it.name, it.category) }
        return all.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getGoingSoon() = _items.value.filter { it.name == "Bread" || it.name == "Milk" }

    // ── ViewModelFactory ──────────────────────────────────────────────
    // Required because ViewModel now has a constructor parameter (repository)
    class Factory(private val repository: ReServeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReServeViewModel::class.java))
                return ReServeViewModel(repository) as T
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}