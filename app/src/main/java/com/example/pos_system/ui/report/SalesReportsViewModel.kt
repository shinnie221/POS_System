package com.example.pos_system.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.local.database.entity.SalesEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ReportType { DAILY, MONTHLY, YEARLY }

data class SalesSummary(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class SalesReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val salesRepo = (application as POSSystem).appModule.salesRepository

    // Full history needed for comparison logic (e.g., Feb vs Jan)
    val allSales: StateFlow<List<SalesEntity>> = salesRepo.salesHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportType = MutableStateFlow(ReportType.DAILY)
    val reportType: StateFlow<ReportType> = _reportType.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _paymentFilter = MutableStateFlow("All")
    val paymentFilter: StateFlow<String> = _paymentFilter.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredSales: StateFlow<List<SalesEntity>> = combine(
        _reportType, _selectedMonth, _selectedYear, _paymentFilter
    ) { type, month, year, payment ->
        Quadruple(type, month, year, payment)
    }.flatMapLatest { (type, month, year, payment) ->
        val range = calculateRange(type, month, year)
        salesRepo.salesHistory.map { history ->
            history.filter { sale ->
                val isInRange = sale.timestamp in range.first..range.second
                val matchesPayment = if (payment == "All") true
                else sale.paymentType.equals(payment, ignoreCase = true)
                isInRange && matchesPayment
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSummary: StateFlow<SalesSummary> = filteredSales.map { sales ->
        SalesSummary(
            totalRevenue = sales.sumOf { it.totalAmount },
            totalTransactions = sales.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesSummary())

    fun setReportType(type: ReportType) { _reportType.value = type }
    fun setMonth(month: Int) { _selectedMonth.value = month }
    fun setYear(year: Int) { _selectedYear.value = year }
    fun setPaymentFilter(payment: String) { _paymentFilter.value = payment }

    fun deleteSale(sale: SalesEntity) {
        viewModelScope.launch { salesRepo.deleteSale(sale) }
    }

    private fun calculateRange(type: ReportType, month: Int, year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (type) {
            ReportType.DAILY -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                Pair(start, System.currentTimeMillis() + 86400000)
            }
            ReportType.MONTHLY -> {
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                Pair(start, cal.timeInMillis)
            }
            ReportType.YEARLY -> {
                cal.set(Calendar.MONTH, 0)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.MONTH, 11)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                Pair(start, cal.timeInMillis)
            }
        }
    }

    init {
        viewModelScope.launch { salesRepo.syncSales() }
    }
}