package com.example.ddulo.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * 온보딩 완료 여부를 저장/조회합니다.
 * 최초 1회만 온보딩을 노출하기 위해 사용합니다.
 */
class OnboardingPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "ddulo_onboarding"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
