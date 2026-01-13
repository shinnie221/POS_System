package com.example.pos_system.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.local.database.entity.ItemEntity
import com.example.pos_system.data.model.CartItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val appModule = (application as POSSystem).appModule
    private val categoryRepo = appModule.categoryRepository
    private val itemRepo = appModule.itemRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
        refreshDataFromFirebase()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepo.allCategories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                if (categories.isNotEmpty() && _uiState.value.selectedCategoryId == null) {
                    selectCategory(categories.first().id)
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, isLoading = true) }
        viewModelScope.launch {
            itemRepo.getItemsByCategory(categoryId).collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun addToCart(cartItem: CartItem) {
        _uiState.update { it.copy(cartItems = it.cartItems + cartItem) }
    }

    fun refreshDataFromFirebase() {
        viewModelScope.launch {
            try {
                categoryRepo.syncCategories()
                itemRepo.syncItems()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Sync Error") }
            }
        }
    }
}
