package com.example.mygooglemaps.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.model.googlemap.DirectionsResponse
import com.example.mygooglemaps.repository.LocationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private var flag: Boolean = false

    val location: StateFlow<List<LocationEntity>> = flow {
        emitAll(locationRepository.getLocation())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = listOf()
    )

    private val _directions = MutableStateFlow<MainUiState>(MainUiState.Success(null))
    val directions: StateFlow<MainUiState> = _directions

    fun getDirectionsLocation(locationEntity: LocationEntity) {
        if (!flag) {
            flag = true
            viewModelScope.launch(Dispatchers.IO) {
                locationRepository.getDirections(
                    LatLng(locationEntity.latitude, locationEntity.longitude),
                    LatLng(12.2001, -86.3836)
                ).enqueue(object : Callback<DirectionsResponse> {
                    override fun onResponse(
                        call: Call<DirectionsResponse>,
                        response: Response<DirectionsResponse>
                    ) {
                        _directions.value = MainUiState.Success(response.body())
                        flag = false
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        _directions.value = MainUiState.Error(t)
                        flag = false
                    }
                })
            }
        }
    }
}

// Represents different states for the LatestNews screen
sealed class MainUiState {
    data class Success(val directions: DirectionsResponse?) : MainUiState()
    data class Error(val exception: Throwable) : MainUiState()
}