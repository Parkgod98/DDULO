package com.example.ddulo.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ddulo.ui.theme.DDULOTheme
import com.example.ddulo.viewmodel.AppViewModel
import com.example.ddulo.viewmodel.state.InitialLoadState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your app.
            requestLocationUpdates()
        } else {
            Log.w(TAG, "Location permission denied by user.")
            // Load data without location
            appViewModel.loadInitialData(null, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setKeepOnScreenCondition {
            appViewModel.loadState is InitialLoadState.Loading
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkAndRequestLocationPermission()

        setContent {
            DDULOTheme {
                AppNavHost()
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                requestLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        // Use getCurrentLocation for a more reliable one-time location read.
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Current location found: Lat: ${location.latitude}, Lon: ${location.longitude}")
                    appViewModel.loadInitialData(location.latitude, location.longitude)
                } else {
                    Log.w(TAG, "Failed to get current location, it was null.")
                    appViewModel.loadInitialData(null, null)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to get current location.", it)
                appViewModel.loadInitialData(null, null)
            }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}