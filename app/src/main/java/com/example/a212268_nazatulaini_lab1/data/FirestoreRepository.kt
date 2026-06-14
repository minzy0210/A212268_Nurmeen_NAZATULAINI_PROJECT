package com.example.a212268_nazatulaini_lab1.data

import com.example.a212268_nazatulaini_lab1.UserListedItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    // ── Points at the "mylistings" Firestore database (not the default one,
    //    since this project only has a database named "mylistings") ──────
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance("mylistings")

    // Top-level collection: "community_listings"
    // Each document ID = item name (unique per user for simplicity)
    private val collection = db.collection("community_listings")

    // ── Upload / update a listing ──────────────────────────────────────
    // Called whenever the user adds or updates an item.
    suspend fun syncToCloud(item: UserListedItem) {
        try {
            val doc = mapOf(
                "name"            to item.name,
                "category"        to item.category,
                "sellerName"      to item.sellerName,
                "location"        to item.location,
                "description"     to item.description,
                "quantity"        to item.quantity,
                "originalPrice"   to item.originalPrice,
                "discountPercent" to item.discountPercent,
                "expiresIn"       to item.expiresIn,
                "deposit"         to item.deposit,
                "maxBorrowDays"   to item.maxBorrowDays,
                "condition"       to item.condition,
                "availableUntil"  to item.availableUntil,
                "photoUri"        to (item.photoUri ?: ""),
                // Timestamp for ordering in a real app
                "uploadedAt"      to com.google.firebase.Timestamp.now()
            )
            // Use item name as the document ID — set() creates or overwrites
            collection.document(item.name).set(doc).await()
            android.util.Log.d("FirestoreSync", "Synced ${item.name} successfully")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Failed to sync ${item.name}", e)
        }
    }

    // ── Remove a listing from the cloud ───────────────────────────────
    suspend fun deleteFromCloud(itemName: String) {
        try {
            collection.document(itemName).delete().await()
        } catch (e: Exception) {
            // Same here — don't crash if cloud delete fails
        }
    }

    // ── Fetch all community listings (one-shot) ────────────────────────
    // Used by CommunityListingsViewModel to populate the explore screen.
    suspend fun fetchAllListings(): List<UserListedItem> {
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    UserListedItem(
                        name            = doc.getString("name") ?: return@mapNotNull null,
                        category        = doc.getString("category") ?: "Food",
                        sellerName      = doc.getString("sellerName") ?: "Community Member",
                        location        = doc.getString("location") ?: "",
                        description     = doc.getString("description") ?: "",
                        photoUri        = doc.getString("photoUri")?.ifBlank { null },
                        quantity        = (doc.getLong("quantity") ?: 1).toInt(),
                        originalPrice   = doc.getDouble("originalPrice") ?: 0.0,
                        discountPercent = (doc.getLong("discountPercent") ?: 0).toInt(),
                        expiresIn       = doc.getString("expiresIn") ?: "Ongoing",
                        deposit         = doc.getDouble("deposit") ?: 0.0,
                        maxBorrowDays   = (doc.getLong("maxBorrowDays") ?: 7).toInt(),
                        condition       = doc.getString("condition") ?: "Good",
                        availableUntil  = doc.getString("availableUntil") ?: "Ongoing"
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()   // gracefully degrade when offline
        }
    }
}