package com.example.pos_system.ui.cart

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.model.CartItem
import com.example.pos_system.data.model.Sales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

enum class DiscountType { NONE, PERCENTAGE, AMOUNT }

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: Double = 0.0,
    val discountApplied: Double = 0.0,
    val finalPrice: Double = 0.0
)

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val appModule = (application as POSSystem).appModule
    private val salesRepo = appModule.salesRepository

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
    var selectedPaymentMethod by mutableStateOf("Cash")
        private set

    private var _isCheckingOut = mutableStateOf(false)
    val isCheckingOut: State<Boolean> = _isCheckingOut


    fun setPaymentMethod(method: String) {
        selectedPaymentMethod = method
    }

    fun addToCart(cartItem: CartItem) {
        _uiState.update { currentState ->
            val updatedItems = currentState.items + cartItem
            calculateTotals(currentState.copy(items = updatedItems))
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        _uiState.update { currentState ->
            val updatedItems = currentState.items.filter { it != cartItem }
            calculateTotals(currentState.copy(items = updatedItems))
        }
    }

    fun applyDiscount(type: DiscountType, value: Double) {
        _uiState.update { currentState ->
            calculateTotals(currentState.copy(discountType = type, discountValue = value))
        }
    }

    private fun calculateTotals(state: CartUiState): CartUiState {
        val total = state.items.sumOf { it.totalPrice }
        val discountAmount = when (state.discountType) {
            DiscountType.PERCENTAGE -> total * (state.discountValue / 100)
            DiscountType.AMOUNT -> state.discountValue
            DiscountType.NONE -> 0.0
        }
        return state.copy(
            totalAmount = total,
            discountApplied = discountAmount,
            finalPrice = (total - discountAmount).coerceAtLeast(0.0)
        )
    }

    fun clearCart() {
        _uiState.update { CartUiState() }
    }

    fun checkout(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        // FIX: Prevent multiple clicks if already processing or cart is empty
        if (_isCheckingOut.value || currentState.items.isEmpty()) return

        // Lock the button
        _isCheckingOut.value = true

        viewModelScope.launch {
            val sale = Sales(
                saleId = "",
                totalAmount = currentState.totalAmount,
                discountApplied = currentState.discountApplied,
                finalPrice = currentState.finalPrice,
                items = currentState.items,
                dateTime = System.currentTimeMillis(),
                paymentType = selectedPaymentMethod
            )

            try {
                salesRepo.processCheckout(sale)
                clearCart()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("CHECKOUT_ERROR", e.message ?: "Error")
            } finally {
                // Unlock the button whether it succeeded or failed
                _isCheckingOut.value = false
            }
        }
    }
}