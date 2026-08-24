package com.example.ddulo.viewmodel

sealed class NavigationEvent {
    object ToHome : NavigationEvent()
    object ToRouteList : NavigationEvent()
    object ToRouteDetail : NavigationEvent()
}
