package com.example.pos_system.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.local.database.entity.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddMenuViewModel(application: Application) : AndroidViewModel(application) {
    private val appModule = (application as POSSystem).appModule
    private val itemRepo = appModule.itemRepository
    private val categoryRepo = appModule.categoryRepository

    val categories = categoryRepo.allCategories
    val items = itemRepo.allItems

    private val _priceError = MutableStateFlow<String?>(null)
    val priceError = _priceError.asStateFlow()

    private val _uiState = MutableStateFlow<AddMenuUiState>(AddMenuUiState.Idle)
    val uiState: StateFlow<AddMenuUiState> = _uiState.asStateFlow()

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepo.addCategory(name)
            _uiState.value = AddMenuUiState.Success("Category Added")
        }
    }

    fun addItem(name: String, priceStr: String, categoryId: String, type: String) {
        val price = priceStr.toDoubleOrNull()

        // VALIDATION
        if (name.isBlank()) {
            _uiState.value = AddMenuUiState.Error("Product name cannot be empty")
            return
        }
        if (price == null || price <= 0) {
            _priceError.value = "Please enter a valid price (e.g. 10.50)"
            return
        }
        if (categoryId.isEmpty()) {
            _uiState.value = AddMenuUiState.Error("Please select a category")
            return
        }

        _priceError.value = null // Clear error if valid

        viewModelScope.launch {
            try {
                itemRepo.addItem(
                    name = name,
                    price = price,
                    categoryId = categoryId,
                    itemType = type,
                    imagePath = null
                )
                _uiState.value = AddMenuUiState.Success("Item Added")
            } catch (e: Exception) {
                _uiState.value = AddMenuUiState.Error(e.message ?: "Failed to add item")
            }
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            itemRepo.deleteItem(item)
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepo.deleteCategory(categoryId)
        }
    }

    fun resetState() { _uiState.value = AddMenuUiState.Idle }
}

sealed class AddMenuUiState {
    object Idle : AddMenuUiState()
    data class Success(val message: String) : AddMenuUiState()
    data class Error(val message: String) : AddMenuUiState()
}
