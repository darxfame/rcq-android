package com.rcq.messenger.ui.contacts

import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val api: RCQApiService,
    private val locationManager: LocationManager
) : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun search(lat: Double, lon: Double, radius: Int = 1000) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resp = api.getNearbyUsers(lat, lon, radius)
                if (resp.isSuccessful) {
                    _users.value = resp.body() ?: emptyList()
                } else {
                    _error.value = "Nearby search failed: ${resp.code()}"
                    Log.e("NearbyViewModel", "getNearbyUsers failed: ${resp.code()} ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("NearbyViewModel", "getNearbyUsers exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("MissingPermission")
    fun searchLastKnown(radius: Int = 1000) {
        val location = getLastKnownLocation()
        if (location == null) {
            _error.value = "Location unavailable"
            return
        }
        search(location.latitude, location.longitude, radius)
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        return locationManager.getProviders(true)
            .asSequence()
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
