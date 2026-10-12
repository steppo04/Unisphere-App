package com.example.unisphere.ui.screen.map

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.network.CreatePoiRequest
import com.example.unisphere.db.network.PointOfInterestDto
import com.example.unisphere.repository.PoiRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class MapState(
    val pois: List<PointOfInterestDto> = emptyList(), // Sostituita la Entity col DTO
    val showAddDialog: Boolean = false,
    val selectedPoi: PointOfInterestDto? = null,
    val newPoiName: String = "",
    val newPoiAddress: String = "",
    val newPoiNotes: String = "",
    val addressSuggestions: List<String> = emptyList(),
    val isLocating: Boolean = false,
    val isSearchingSuggestions: Boolean = false
)

sealed interface MapAction {
    data class OnNameChanged(val value: String) : MapAction
    data class OnAddressChanged(val value: String) : MapAction
    data class OnNotesChanged(val value: String) : MapAction
    data object OnSavePoiClicked : MapAction
    data object OnAddPoiClicked : MapAction
    data object OnDismissAddDialog : MapAction
    data class OnDeletePoiClicked(val poi: PointOfInterestDto) : MapAction // Usa il DTO
    data class OnPoiSelected(val poi: PointOfInterestDto?) : MapAction
    data class OnSuggestionSelected(val address: String) : MapAction
    data object OnUseCurrentLocation : MapAction
}

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val poiRepository: PoiRepository
) : AndroidViewModel(application) {

    // Passaggio a StateFlow
    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    init {
        fetchPois()
    }

    // Fetch esplicito invece di osservare il DB locale
    private fun fetchPois() {
        viewModelScope.launch {
            poiRepository.getPois().onSuccess { list ->
                _state.update { it.copy(pois = list) }
            }
        }
    }

    fun onAction(action: MapAction) {
        val currentState = _state.value

        when (action) {
            is MapAction.OnNameChanged -> _state.update { it.copy(newPoiName = action.value) }
            is MapAction.OnAddressChanged -> {
                _state.update { it.copy(newPoiAddress = action.value) }
                fetchAddressSuggestions(action.value)
            }
            is MapAction.OnNotesChanged -> _state.update { it.copy(newPoiNotes = action.value) }

            MapAction.OnSavePoiClicked -> {
                if (currentState.newPoiName.isNotBlank() && currentState.newPoiAddress.isNotBlank()) {
                    viewModelScope.launch {
                        val coords = getCoordinatesFromAddress(currentState.newPoiAddress)
                        val request = CreatePoiRequest(
                            name = currentState.newPoiName,
                            address = currentState.newPoiAddress,
                            notes = currentState.newPoiNotes,
                            latitude = coords?.first ?: 0.0,
                            longitude = coords?.second ?: 0.0
                        )

                        // Chiamata di rete per salvare
                        poiRepository.savePoi(request).onSuccess {
                            _state.update {
                                it.copy(
                                    showAddDialog = false,
                                    newPoiName = "",
                                    newPoiAddress = "",
                                    newPoiNotes = "",
                                    addressSuggestions = emptyList()
                                )
                            }
                            fetchPois() // Ricarica la lista aggiornata dal server
                        }
                    }
                }
            }
            MapAction.OnAddPoiClicked -> _state.update { it.copy(showAddDialog = true) }
            MapAction.OnDismissAddDialog -> {
                _state.update {
                    it.copy(
                        showAddDialog = false,
                        addressSuggestions = emptyList(),
                        newPoiName = "",
                        newPoiAddress = "",
                        newPoiNotes = ""
                    )
                }
            }
            is MapAction.OnDeletePoiClicked -> {
                viewModelScope.launch {
                    poiRepository.deletePoi(action.poi.id).onSuccess {
                        if (currentState.selectedPoi?.id == action.poi.id) {
                            _state.update { it.copy(selectedPoi = null) }
                        }
                        fetchPois() // Aggiorna la lista dopo l'eliminazione
                    }
                }
            }
            is MapAction.OnPoiSelected -> _state.update { it.copy(selectedPoi = action.poi) }
            is MapAction.OnSuggestionSelected -> _state.update { it.copy(newPoiAddress = action.address, addressSuggestions = emptyList()) }
            MapAction.OnUseCurrentLocation -> getCurrentLocation()
        }
    }

    private fun fetchAddressSuggestions(query: String) {
        if (query.length < 3) {
            _state.update { it.copy(addressSuggestions = emptyList()) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isSearchingSuggestions = true) }
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }
                val suggestions = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
                _state.update { it.copy(addressSuggestions = suggestions) }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _state.update { it.copy(isSearchingSuggestions = false) }
            }
        }
    }

    private suspend fun getCoordinatesFromAddress(addressName: String): Pair<Double, Double>? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressName, 1)
                addresses?.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
            } catch (e: Exception) { null }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true) }
            try {
                val result = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) {
                        _state.update { it.copy(newPoiAddress = addressName) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _state.update { it.copy(isLocating = false) }
            }
        }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                        }
                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                } catch (e: Exception) { continuation.resume(null) }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
                } catch (e: Exception) { null }
            }
        }
    }
}