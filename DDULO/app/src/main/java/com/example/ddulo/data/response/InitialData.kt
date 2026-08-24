package com.example.ddulo.data.response

import com.example.ddulo.domain.model.Station

// The structure of the "data" object
data class InitialData(
    val nearbyStations: List<Station>,
    val allStations: List<Station>
)