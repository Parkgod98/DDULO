package com.example.ddulo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ddulo.data.local.database.FavoriteDatabase

class FavoriteViewModelFactory(
    context: Context
) : ViewModelProvider.Factory {

    private val dao =
        FavoriteDatabase.get(context.applicationContext).favoriteDao()

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
