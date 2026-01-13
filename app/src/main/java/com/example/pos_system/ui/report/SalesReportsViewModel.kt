package com.example.pos_system.ui.salesimport

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos_system.POSSystem
import com.example.pos_system.data.local.database.entity.SalesEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ReportType { DAILY, MONTHLY, YEARLY }

class SalesReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val salesRepo = (application as POSSystem).appModule.salesRepository

    private val _reportType = MutableStateFlow(ReportType.DAILY)
    val reportType: StateFlow<ReportType> = _reportType.asStateFlow()

    private val calendar = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(calendar.get(Calendar.MONTH))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredSales: StateFlow<List<SalesEntity>> = combine(
        _reportType, _selectedMonth, _selectedYear
    ) { type, month, year ->
        Triple(type, month, year)
    }.flatMapLatest { (type, month, year) ->
        val range = calculateRange(type, month, year)
        salesRepo.salesHistory.map { allSales ->
            allSales.filter { it.timestamp in range.first..range.second }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setReportType(type: ReportType) { _reportType.value = type }
    fun setMonth(month: Int) { _selectedMonth.value = month }
    fun setYear(year: Int) { _selectedYear.value = year }

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
                Pair(start, System.currentTimeMillis())
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