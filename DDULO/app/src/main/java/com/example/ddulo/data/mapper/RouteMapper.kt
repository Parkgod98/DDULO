package com.example.ddulo.data.mapper

import com.example.ddulo.data.response.*
import com.example.ddulo.domain.model.*

fun RouteSearchResponse.toDomain(): List<RouteResult> {
    return listOf(
        RouteResult(
            totalTime = fastestPathResponse.totalTime,
            transferCount = fastestPathResponse.transferCount,
            legs = fastestPathResponse.legs.map { it.toDomain() }
        )
    )
}

fun RouteLegResponse.toDomain(): RouteLeg {
    return RouteLeg(
        startStation = startStation,
        endStation = endStation,
        lineName = lineName,
        sectionTime = sectionTime
    )
}
