// app/src/main/java/com/example/a212268_nazatulaini_lab1/data/ReServeRepository.kt
// CHANGES:
//  - Added UserProfileDao + profile save/load methods (Room, Requirement 1)
//  - Added FirestoreRepository calls in addUserItem + deleteUserItem (Requirement 2)
//  - FirestoreRepository is injected so it can be faked in tests

package com.example.a212268_nazatulaini_lab1.data

import com.example.a212268_nazatulaini_lab1.CartItem
import com.example.a212268_nazatulaini_lab1.UserListedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReServeRepository(
    private val userListedItemDao: UserListedItemDao,
    private val cartItemDao: CartItemDao,
    private val chatMessageDao: ChatMessageDao,
    private val reservationDao: ReservationDao,
    private val borrowedItemDao: BorrowedItemDao,
    private val userProfileDao: UserProfileDao,                 // ← ADD
    private val firestoreRepo: FirestoreRepository = FirestoreRepository()   // ← ADD
) {

    // ── UserListedItem ────────────────────────────────────────────────

    val userListedItems: Flow<List<UserListedItem>> =
        userListedItemDao.getAll().map { list -> list.map { it.toDomain() } }

    // Save to Room AND sync to Firestore
    suspend fun addUserItem(item: UserListedItem) {
        userListedItemDao.insert(item.toEntity())
        firestoreRepo.syncToCloud(item)                         // ← ADD
    }

    // Remove from Room AND remove from Firestore
    suspend fun deleteUserItem(item: UserListedItem) {
        userListedItemDao.deleteByName(item.name)
        firestoreRepo.deleteFromCloud(item.name)
    }

    suspend fun updateUserItem(item: UserListedItem) {
        userListedItemDao.update(item.toEntity())
        firestoreRepo.syncToCloud(item)                        // ← ADD: keep cloud in sync
    }

    suspend fun getUserItemByName(name: String): UserListedItem? =
        userListedItemDao.getByName(name)?.toDomain()

    // ── CartItem ──────────────────────────────────────────────────────

    val cartItems: Flow<List<CartItem>> =
        cartItemDao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun addToCart(item: CartItem) = cartItemDao.insert(item.toEntity())
    suspend fun updateCartItem(item: CartItem) = cartItemDao.update(item.toEntity())
    suspend fun removeFromCart(name: String) = cartItemDao.deleteByName(name)
    suspend fun clearCart() = cartItemDao.clearAll()

    // ── ChatMessage ───────────────────────────────────────────────────

    fun getMessagesForConversation(owner: String, item: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessagesForConversation(owner, item)

    fun getAllConversations(): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getAllConversations()

    suspend fun sendMessage(message: ChatMessageEntity) = chatMessageDao.insert(message)
    suspend fun deleteConversation(owner: String, item: String) =
        chatMessageDao.deleteConversation(owner, item)

    // ── Reservations ──────────────────────────────────────────────────

    val reservations: Flow<List<ReservationEntity>> = reservationDao.getAll()

    suspend fun reserveItem(itemName: String, quantity: Int) {
        val existing = reservationDao.getByName(itemName)
        reservationDao.insert(
            ReservationEntity(itemName, (existing?.quantity ?: 0) + quantity)
        )
    }
     // ── Borrowed items ────────────────────────────────────────────────

    val borrowedItems: Flow<List<BorrowedItemEntity>> = borrowedItemDao.getAll()

    suspend fun borrowItem(itemName: String) =
        borrowedItemDao.insert(BorrowedItemEntity(itemName))

    // ── User Profile (Room — Local Persistence, Requirement 1) ────────
    // Exposes the single profile row as a live Flow; null until first save.

    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getProfile()

    suspend fun saveProfile(profile: UserProfileEntity) =
        userProfileDao.upsert(profile)


    // ── Community listings from Firestore (Requirement 2) ─────────────

    suspend fun fetchCommunityListings(): List<UserListedItem> =
        firestoreRepo.fetchAllListings()
}

// ── Mappers (unchanged) ───────────────────────────────────────────────────

fun UserListedItemEntity.toDomain() = UserListedItem(
    name = name, category = category, photoUri = photoUri,
    sellerName = sellerName, location = location, description = description,
    quantity = quantity, originalPrice = originalPrice, discountPercent = discountPercent,
    expiresIn = expiresIn, deposit = deposit, maxBorrowDays = maxBorrowDays,
    condition = condition, availableUntil = availableUntil
)

fun UserListedItem.toEntity() = UserListedItemEntity(
    name = name, category = category, photoUri = photoUri,
    sellerName = sellerName, location = location, description = description,
    quantity = quantity, originalPrice = originalPrice, discountPercent = discountPercent,
    expiresIn = expiresIn, deposit = deposit, maxBorrowDays = maxBorrowDays,
    condition = condition, availableUntil = availableUntil
)

fun CartItemEntity.toDomain() = CartItem(name = name, imageRes = imageRes, price = price, quantity = quantity)
fun CartItem.toEntity() = CartItemEntity(name = name, imageRes = imageRes, price = price, quantity = quantity)