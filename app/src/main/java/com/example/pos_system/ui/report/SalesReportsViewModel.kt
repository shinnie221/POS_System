package com.example.pos_system.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.local.database.entity.SalesEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ReportType { DAILY, MONTHLY, YEARLY }

data class SalesSummary(
    val totalRevenue: Double = 0.0,
    val totalTransactions: Int = 0,
    val comparisonText: String = "No data"
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

class SalesReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val salesRepo = (application as POSSystem).appModule.salesRepository

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredSales: StateFlow<List<SalesEntity>> = combine(
        _reportType, _selectedMonth, _selectedYear, _paymentFilter
    ) { type, month, year, payment ->
        Quadruple(type, month, year, payment)
    }.flatMapLatest { params ->
        val range = calculateRange(params.first, params.second, params.third)
        salesRepo.salesHistory.map { history ->
            history.filter { sale ->
                val isInRange = sale.timestamp in range.first..range.second
                val matchesPayment = if (params.fourth == "All") true
                else sale.paymentType.equals(params.fourth, ignoreCase = true)
                isInRange && matchesPayment
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSummary: StateFlow<SalesSummary> = combine(filteredSales, allSales) { current, all ->
        SalesSummary(
            totalRevenue = current.sumOf { it.totalAmount },
            totalTransactions = current.size,
            comparisonText = getComparisonText(current, all)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesSummary())

    fun setReportType(type: ReportType) { _reportType.value = type }
    fun setMonth(month: Int) { _selectedMonth.value = month }
    fun setYear(year: Int) { _selectedYear.value = year }
    fun setPaymentFilter(payment: String) { _paymentFilter.value = payment }

    fun deleteSale(sale: SalesEntity) {
        viewModelScope.launch { salesRepo.deleteSale(sale) }
    }

    private fun getComparisonText(current: List<SalesEntity>, all: List<SalesEntity>): String {
        if (current.isEmpty()) return "No data"

        // Use the first item of the current filtered list to determine the reference date
        val cal = Calendar.getInstance().apply { timeInMillis = current[0].timestamp }
        val currentType = _reportType.value
        val currentTotal = current.sumOf { it.totalAmount }

        val comparisonTotal = when (currentType) {
            ReportType.MONTHLY -> {
                // Compare with PREVIOUS MONTH of the same year (or previous year if current is Jan)
                val targetMonth = cal.get(Calendar.MONTH)
                val targetYear = cal.get(Calendar.YEAR)

                val prevMonthCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    add(Calendar.MONTH, -1) // Go back 1 month
                }

                all.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    sCal.get(Calendar.YEAR) == prevMonthCal.get(Calendar.YEAR) &&
                            sCal.get(Calendar.MONTH) == prevMonthCal.get(Calendar.MONTH)
                }.sumOf { it.totalAmount }
            }
            ReportType.YEARLY -> {
                // Compare with PREVIOUS YEAR
                val targetYear = cal.get(Calendar.YEAR)
                all.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    sCal.get(Calendar.YEAR) == (targetYear - 1)
                }.sumOf { it.totalAmount }
            }
            else -> 0.0 // Daily comparison logic can be added here if needed
        }

        val label = if (currentType == ReportType.MONTHLY) "Last Month" else "Last Year"

        return if (comparisonTotal > 0) {
            val diff = ((currentTotal - comparisonTotal) / comparisonTotal) * 100
            "${if (diff >= 0) "+" else ""}${String.format("%.1f", diff)}% vs $label"
        } else {
            "No data for $label"
        }
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
                val cal = Calendar.getInstance()
                // Start of TODAY
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                // End of TODAY (Set to 23:59:59 instead of System.currentTimeMillis)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                Pair(start, end)
            }
            ReportType.MONTHLY -> {
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
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
                cal.set(Calendar.SECOND, 59)
                Pair(start, cal.timeInMillis)
            }
        }
    }

    init {
        viewModelScope.launch {
            salesRepo.startRealTimeSync()
        }
    }
}