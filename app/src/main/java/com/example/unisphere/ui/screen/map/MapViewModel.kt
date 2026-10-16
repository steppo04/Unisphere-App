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
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.withTimeout
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class MapState(
    val pois: List<PointOfInterestDto> = emptyList(),
    val showAddDialog: Boolean = false,
    val selectedPoi: PointOfInterestDto? = null,
    val newPoiName: String = "",
    val newPoiAddress: String = "",
    val newPoiNotes: String = "",
    val addressSuggestions: List<String> = emptyList(),
    val isLocating: Boolean = false,
    val isSearchingSuggestions: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface MapAction {
    data class OnNameChanged(val value: String) : MapAction
    data class OnAddressChanged(val value: String) : MapAction
    data class OnNotesChanged(val value: String) : MapAction
    data object OnSavePoiClicked : MapAction
    data object OnAddPoiClicked : MapAction
    data object OnDismissAddDialog : MapAction
    data class OnDeletePoiClicked(val poi: PointOfInterestDto) : MapAction
    data class OnPoiSelected(val poi: PointOfInterestDto?) : MapAction
    data class OnSuggestionSelected(val address: String) : MapAction
    data object OnUseCurrentLocation : MapAction
}

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val poiRepository: PoiRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    init {
        fetchPois()
    }

    private fun fetchPois() {
        viewModelScope.launch {
            poiRepository.getPois()
                .onSuccess { list ->
                    _state.update { it.copy(pois = list) }
                }
                .onFailure { error ->
                    error.printStackTrace()
                    _state.update {
                        it.copy(errorMessage = "Impossibile caricare i luoghi: ${error.message ?: "errore di rete"}")
                    }
                }
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnNameChanged -> {
                _state.update { it.copy(newPoiName = action.value, errorMessage = null) }
            }

            is MapAction.OnAddressChanged -> {
                _state.update {
                    it.copy(newPoiAddress = action.value, errorMessage = null)
                }
                fetchAddressSuggestions(action.value)
            }

            is MapAction.OnNotesChanged -> {
                _state.update { it.copy(newPoiNotes = action.value) }
            }

            MapAction.OnSavePoiClicked -> savePoi()

            MapAction.OnAddPoiClicked -> {
                _state.update {
                    it.copy(showAddDialog = true, errorMessage = null)
                }
            }

            MapAction.OnDismissAddDialog -> {
                searchJob?.cancel()
                _state.update {
                    it.copy(
                        showAddDialog = false,
                        addressSuggestions = emptyList(),
                        newPoiName = "",
                        newPoiAddress = "",
                        newPoiNotes = "",
                        errorMessage = null,
                        isSaving = false
                    )
                }
            }


            is MapAction.OnDeletePoiClicked -> {
                viewModelScope.launch {
                    poiRepository.deletePoi(action.poi.id)
                        .onSuccess {
                            _state.update { current ->
                                current.copy(
                                    pois = current.pois.filter { it.id != action.poi.id },
                                    selectedPoi = if (current.selectedPoi?.id == action.poi.id) {
                                        null
                                    } else {
                                        current.selectedPoi
                                    },
                                    errorMessage = null
                                )
                            }

                            // Ricarica la lista dal backend per verificare
                            // che il punto sia stato effettivamente eliminato.
                            fetchPois()
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            _state.update {
                                it.copy(
                                    errorMessage = "Eliminazione non riuscita: ${
                                        error.message ?: "errore sconosciuto"
                                    }"
                                )
                            }
                        }
                }
            }


            is MapAction.OnPoiSelected -> {
                _state.update { it.copy(selectedPoi = action.poi) }
            }

            is MapAction.OnSuggestionSelected -> {
                searchJob?.cancel()
                _state.update {
                    it.copy(
                        newPoiAddress = action.address,
                        addressSuggestions = emptyList(),
                        errorMessage = null
                    )
                }
            }

            MapAction.OnUseCurrentLocation -> getCurrentLocation()
        }
    }

    private fun savePoi() {
        val current = _state.value
        val name = current.newPoiName.trim()
        val address = current.newPoiAddress.trim()
        val notes = current.newPoiNotes.trim()

        if (name.isBlank() || address.isBlank()) {
            _state.update {
                it.copy(errorMessage = "Inserisci il nome e l'indirizzo del luogo.")
            }
            return
        }

        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val coords = withTimeout(15_000L) {
                    getCoordinatesFromAddress(address)
                }

                if (coords == null ||
                    !coords.first.isFinite() ||
                    !coords.second.isFinite() ||
                    (coords.first == 0.0 && coords.second == 0.0)
                ) {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Non riesco a trovare questo indirizzo. Seleziona un suggerimento oppure inserisci un indirizzo più preciso."
                        )
                    }
                    return@launch
                }

                val request = CreatePoiRequest(
                    name = name,
                    address = address,
                    notes = notes,
                    latitude = coords.first,
                    longitude = coords.second
                )

                poiRepository.savePoi(request)
                    .onSuccess {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                showAddDialog = false,
                                newPoiName = "",
                                newPoiAddress = "",
                                newPoiNotes = "",
                                addressSuggestions = emptyList(),
                                errorMessage = null
                            )
                        }
                        fetchPois()
                    }
                    .onFailure { error ->
                        error.printStackTrace()
                        _state.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Salvataggio non riuscito: ${error.message ?: "errore sconosciuto"}"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                            "La ricerca dell'indirizzo è scaduta. Riprova."
                        } else {
                            "Errore durante il salvataggio: ${e.message ?: "errore sconosciuto"}"
                        }
                    )
                }
            }
        }
    }

    private fun fetchAddressSuggestions(query: String) {
        searchJob?.cancel()

        if (query.trim().length < 3) {
            _state.update {
                it.copy(addressSuggestions = emptyList(), isSearchingSuggestions = false)
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            _state.update { it.copy(isSearchingSuggestions = true) }

            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }
                val suggestions = addresses
                    ?.mapNotNull { it.getAddressLine(0) }
                    ?.distinct()
                    ?: emptyList()

                _state.update { it.copy(addressSuggestions = suggestions) }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                }
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
                geocoder.getFromLocationName(addressName, 1)
                    ?.firstOrNull()
                    ?.let { Pair(it.latitude, it.longitude) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            _state.update {
                it.copy(isLocating = true, errorMessage = null)
            }

            try {
                val result = fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await()

                if (result == null) {
                    _state.update {
                        it.copy(errorMessage = "Posizione attuale non disponibile. Controlla il GPS.")
                    }
                } else {
                    val addressName = getAddressFromLocation(
                        result.latitude,
                        result.longitude
                    )

                    if (addressName != null) {
                        _state.update {
                            it.copy(
                                newPoiAddress = addressName,
                                addressSuggestions = emptyList(),
                                errorMessage = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(errorMessage = "Non riesco a ricavare l'indirizzo dalla posizione attuale.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(errorMessage = "Errore nel recupero della posizione. Controlla i permessi e il GPS.")
                }
            } finally {
                _state.update { it.copy(isLocating = false) }
            }
        }
    }

    private suspend fun getAddressFromLocation(
        latitude: Double,
        longitude: Double
    ): String? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) {
                                    continuation.resume(
                                        addresses.firstOrNull()?.getAddressLine(0)
                                    )
                                }
                            }

                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                        ?.firstOrNull()
                        ?.getAddressLine(0)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    null
                }
            }
        }
    }
}