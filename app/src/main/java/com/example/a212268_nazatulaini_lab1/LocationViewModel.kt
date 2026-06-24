package com.example.a212268_nazatulaini_lab1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationViewModel(app: Application) : AndroidViewModel(app) {

    private val helper = LocationHelper(app)

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation

    private val _placeName = MutableStateFlow<String?>(null)
    val placeName: StateFlow<String?> = _placeName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Requests a fresh HIGH-ACCURACY GPS fix, then reverse-geocodes it.
     * Safe to call multiple times; skips if already in progress.
     */
    fun fetchLocation() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val loc = helper.getCurrentLocation()   // <-- real-time fix
            if (loc != null) {
                _userLocation.value = loc
                _placeName.value = helper.getPlaceName(loc.latitude, loc.longitude)
            } else {
                _error.value = "Could not get location. Make sure GPS is enabled."
            }
            _isLoading.value = false
        }
    }

    /**
     * Returns a formatted distance string from the user's location to a
     * "lat, lon" coordinate string stored in an item's location field.
     * Returns null if user location is unavailable or the string can't be parsed.
     */
    fun distanceTo(targetCoordString: String): String? {
        val user = _userLocation.value ?: return null
        val parts = targetCoordString.split(",").map { it.trim().toDoubleOrNull() }
        if (parts.size < 2 || parts[0] == null || parts[1] == null) return null
        return helper.formatDistance(user.latitude, user.longitude, parts[0]!!, parts[1]!!)
    }

    class Factory(private val app: Application) : ViewModelProvider.AndroidViewModelFactory(app) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            LocationViewModel(app) as T
    }
}