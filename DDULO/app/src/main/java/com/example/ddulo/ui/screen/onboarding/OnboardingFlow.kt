package com.example.ddulo.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.ddulo.data.preferences.OnboardingPreferences

/**
 * 온보딩 완료 여부를 확인하고, 미완료 시 온보딩 화면을 보여주고
 * 완료 시 메인(홈)으로 이동합니다.
 */
@Composable
fun OnboardingFlow(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences(context) }

    var hasChecked by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val completed = onboardingPreferences.isOnboardingCompleted()
        hasChecked = true
        showOnboarding = !completed
    }

    LaunchedEffect(hasChecked, showOnboarding) {
        if (hasChecked && !showOnboarding) {
            navController.navigate("search_graph") {
                popUpTo("onboarding_flow") { inclusive = true }
            }
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            navController = navController,
            onboardingPreferences = onboardingPreferences,
            modifier = modifier
        )
    }
}
