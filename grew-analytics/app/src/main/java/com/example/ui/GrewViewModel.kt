package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.GrewData
import com.example.data.GrewRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardMetric { Amount, MW, Qty }
enum class VelocityMode { Quarterly, Monthly, Weekly, Daily }

sealed class SheetSyncState {
    object Idle : SheetSyncState()
    object Syncing : SheetSyncState()
    data class Success(val count: Int, val source: String, val sheetId: String) : SheetSyncState()
    data class Error(val message: String) : SheetSyncState()
}

data class DashboardFilters(

    val selectedFY: String = "2025-26",
    val customStartDate: Date? = null,
    val customEndDate: Date? = null,
    val selectedSegments: Set<String> = setOf("Solar Modules"),
    val activeMetric: DashboardMetric = DashboardMetric.Amount,
    val matrixMonth: String? = "May", // Month name "Apr", "May" etc. or null
    val selectedQuarter: Int? = null, // 0, 1, 2, 3 or null
    val selectedWeek: Int? = null, // 1..5 or null
    val selectedDay: Int? = null, // 1..31 or null
    val salesHeads: Set<String> = emptySet(),
    val customers: Set<String> = emptySet(),
    val skus: Set<String> = emptySet(),
    val pendingOnly: Boolean = false,
    val velocityMode: VelocityMode = VelocityMode.Weekly,
    val excludedSeries: Set<String> = emptySet()
)

data class MatrixRowItem(
    val monthName: String, // "Apr", "May", ..., "Total"
    val revenueCr: Double,
    val capacityMw: Double,
    val volumeQty: Double,
    val momChange: Double?, // %
    val qoqChange: Double?, // %
    val yoyChange: Double?  // %
)

data class ContributorItem(
    val name: String,
    val value: Double,
    val percentage: Double,
    val uniqueCount: Int = 0
)

data class ConcentrationStats(
    val hhiCustomer: Double,
    val hhiProduct: Double,
    val top5CustomerShare: Double,
    val top3ProductShare: Double,
    val isDiversifiedCustomer: Boolean,
    val isDiversifiedProduct: Boolean,
    val trailing7DayVelocityProjection: Double,
    val yieldRealizationPerMw: Double // ₹ Cr per MW
)

data class VelocityPoint(
    val label: String, // e.g. "W1", "Apr", "Q1" or "10 May"
    val seriesValues: Map<String, Double> // series name -> value
)

data class DashboardStats(
    val periodSales: Double,
    val periodSalesBreakdown: Map<String, Double>,
    val mtd: Double,
    val mtdBreakdown: Map<String, Double>,
    val mtdPacingChange: Double?,
    val qtd: Double,
    val qtdBreakdown: Map<String, Double>,
    val qtdPacingChange: Double?,
    val ytd: Double,
    val ytdBreakdown: Map<String, Double>,
    val ytdPacingChange: Double?,
    val pending: Double,
    val pendingBreakdown: Map<String, Double>,
    val matrix: List<MatrixRowItem>,
    val salesLeaders: List<ContributorItem>,
    val clientDistribution: List<ContributorItem>,
    val skuDistribution: List<ContributorItem>,
    val velocitySeries: List<VelocityPoint>,
    val activeSeriesNames: List<String>,
    val concentration: ConcentrationStats,
    val anchorDate: Date,
    val applicableSkus: List<String> = emptyList()
)

class GrewViewModel : ViewModel() {
    private var allRecords: List<GrewRecord> = emptyList()
    private var calculationJob: Job? = null
    
    private val _syncState = MutableStateFlow<SheetSyncState>(SheetSyncState.Idle)
    val syncState = _syncState.asStateFlow()
    
    val allSegments get() = allRecords.map { it.segment }.distinct().sorted().ifEmpty { GrewData.segments }
    val allSalesHeads get() = allRecords.map { it.salesHead }.distinct().sorted().ifEmpty { GrewData.salesHeads }
    val allCustomers get() = allRecords.map { it.customer }.distinct().sorted().ifEmpty { GrewData.customers }
    val allSkus get() = allRecords.map { it.wp }.distinct().sorted()
    val allFinancialYears get() = allRecords.map { it.fiscalYear }.distinct().sortedDescending()

    // Find the absolute min/max dates
    val globalMinDate get() = allRecords.minByOrNull { it.date.time }?.date ?: Date()
    val globalMaxDate get() = allRecords.maxByOrNull { it.date.time }?.date ?: Date()

    private val _filters = MutableStateFlow(
        DashboardFilters(
            selectedFY = allRecords.map { it.fiscalYear }.distinct().sortedDescending().firstOrNull() ?: "2025-26",
            matrixMonth = "May",
            velocityMode = VelocityMode.Weekly
        )
    )
    val filters = _filters.asStateFlow()

    private val _stats = MutableStateFlow<DashboardStats?>(null)
    val stats = _stats.asStateFlow()

    // Determine target anchorDate for calculations based on active selections
    private var activeAnchorDate: Date = globalMaxDate

    init {
        // Fetch sheets data asynchronously
        loadSheetData()
        // Automatically default filters and range
        resetToLatestAnchor()
    }

    fun resetToLatestAnchor() {
        val maxYearCal = Calendar.getInstance().apply { time = globalMaxDate }
        val defFY = if (maxYearCal.get(Calendar.MONTH) >= 3) {
            "${maxYearCal.get(Calendar.YEAR)}-${(maxYearCal.get(Calendar.YEAR) + 1).toString().takeLast(2)}"
        } else {
            "${maxYearCal.get(Calendar.YEAR) - 1}-${maxYearCal.get(Calendar.YEAR).toString().takeLast(2)}"
        }

        _filters.update {
            it.copy(
                selectedFY = defFY,
                customStartDate = null,
                customEndDate = null,
                matrixMonth = when (maxYearCal.get(Calendar.MONTH)) {
                    Calendar.APRIL -> "Apr"
                    Calendar.MAY -> "May"
                    Calendar.JUNE -> "Jun"
                    Calendar.JULY -> "Jul"
                    Calendar.AUGUST -> "Aug"
                    Calendar.SEPTEMBER -> "Sep"
                    Calendar.OCTOBER -> "Oct"
                    Calendar.NOVEMBER -> "Nov"
                    Calendar.DECEMBER -> "Dec"
                    Calendar.JANUARY -> "Jan"
                    Calendar.FEBRUARY -> "Feb"
                    Calendar.MARCH -> "Mar"
                    else -> "Apr"
                },
                selectedSegments = setOf("Solar Modules"),
                selectedQuarter = null,
                selectedWeek = null,
                selectedDay = null,
                salesHeads = emptySet(),
                customers = emptySet(),
                skus = emptySet(),
                pendingOnly = false,
                velocityMode = VelocityMode.Weekly,
                excludedSeries = emptySet()
            )
        }
        recomputeStats()
    }

    fun updateFY(fy: String) {
        val nextMode = if (fy == (allFinancialYears.firstOrNull() ?: "")) VelocityMode.Weekly else VelocityMode.Monthly
        val defMonth = if (fy == (allFinancialYears.firstOrNull() ?: "")) {
            val maxYearCal = Calendar.getInstance().apply { time = globalMaxDate }
            getMonthName(maxYearCal.get(Calendar.MONTH))
        } else {
            null // Showing full year
        }

        _filters.update {
            it.copy(
                selectedFY = fy,
                matrixMonth = defMonth,
                selectedQuarter = null,
                selectedWeek = null,
                selectedDay = null,
                velocityMode = nextMode,
                customStartDate = null,
                customEndDate = null
            )
        }
        recomputeStats()
    }

    fun updateMetric(metric: DashboardMetric) {
        _filters.update { it.copy(activeMetric = metric) }
        recomputeStats()
    }

    fun updateVelocityMode(mode: VelocityMode) {
        _filters.update {
            it.copy(
                velocityMode = mode,
                // Default month fallback if in Daily mode and none is selected
                matrixMonth = if (mode == VelocityMode.Daily && it.matrixMonth == null) {
                    val maxYearCal = Calendar.getInstance().apply { time = globalMaxDate }
                    getMonthName(maxYearCal.get(Calendar.MONTH))
                } else it.matrixMonth
            )
        }
        recomputeStats()
    }

    fun toggleSegment(segment: String, isCtrlKey: Boolean) {
        _filters.update { current ->
            val nextSegments = if (isCtrlKey) {
                if (current.selectedSegments.contains(segment)) {
                    if (current.selectedSegments.size > 1) current.selectedSegments - segment else current.selectedSegments
                } else {
                    current.selectedSegments + segment
                }
            } else {
                setOf(segment)
            }
            current.copy(selectedSegments = nextSegments)
        }
        recomputeStats()
    }

    fun resetSegments() {
        _filters.update { it.copy(selectedSegments = allSegments.toSet()) }
        recomputeStats()
    }

    fun toggleSalesHeadFilter(name: String, isCtrlKey: Boolean) {
        _filters.update { current ->
            val next = if (isCtrlKey) {
                if (current.salesHeads.contains(name)) current.salesHeads - name else current.salesHeads + name
            } else {
                if (current.salesHeads.size == 1 && current.salesHeads.contains(name)) emptySet() else setOf(name)
            }
            current.copy(salesHeads = next)
        }
        recomputeStats()
    }

    fun toggleCustomerFilter(name: String, isCtrlKey: Boolean) {
        _filters.update { current ->
            val next = if (isCtrlKey) {
                if (current.customers.contains(name)) current.customers - name else current.customers + name
            } else {
                if (current.customers.size == 1 && current.customers.contains(name)) emptySet() else setOf(name)
            }
            current.copy(customers = next)
        }
        recomputeStats()
    }

    fun toggleSkuFilter(name: String, isCtrlKey: Boolean) {
        _filters.update { current ->
            val next = if (isCtrlKey) {
                if (current.skus.contains(name)) current.skus - name else current.skus + name
            } else {
                if (current.skus.size == 1 && current.skus.contains(name)) emptySet() else setOf(name)
            }
            current.copy(skus = next)
        }
        recomputeStats()
    }

    fun toggleMatrixMonth(month: String) {
        _filters.update { current ->
            val nextMonth = if (current.matrixMonth == month) null else month
            current.copy(
                matrixMonth = nextMonth,
                selectedQuarter = null,
                selectedWeek = null,
                selectedDay = null,
                velocityMode = if (nextMonth == null) VelocityMode.Monthly else VelocityMode.Weekly
            )
        }
        recomputeStats()
    }

    fun toggleMatrixQuarter(qIndex: Int) {
        _filters.update { current ->
            val nextQuarter = if (current.selectedQuarter == qIndex) null else qIndex
            current.copy(
                selectedQuarter = nextQuarter,
                matrixMonth = null,
                selectedWeek = null,
                selectedDay = null,
                velocityMode = if (nextQuarter == null) VelocityMode.Monthly else VelocityMode.Weekly
            )
        }
        recomputeStats()
    }

    fun selectCustomDateRange(start: Date?, end: Date?) {
        _filters.update {
            it.copy(
                customStartDate = start,
                customEndDate = end,
                // Exit matrix specific locks
                matrixMonth = null,
                selectedQuarter = null,
                selectedWeek = null,
                selectedDay = null
            )
        }
        recomputeStats()
    }

    fun toggleSeriesExclusion(series: String) {
        _filters.update { current ->
            val next = if (current.excludedSeries.contains(series)) {
                current.excludedSeries - series
            } else {
                current.excludedSeries + series
            }
            current.copy(excludedSeries = next)
        }
        recomputeStats()
    }

    fun togglePendingOnly() {
        _filters.update { it.copy(pendingOnly = !it.pendingOnly) }
        recomputeStats()
    }

    fun clearAllDrilldownFilters() {
        _filters.update {
            it.copy(
                salesHeads = emptySet(),
                customers = emptySet(),
                skus = emptySet()
            )
        }
        recomputeStats()
    }

    fun isolateLegendSeries(seriesName: String) {
        _filters.update { current ->
            val activeNames = getActiveSeriesNames(current.selectedSegments)
            val isAlreadyIsolating = current.excludedSeries.size == activeNames.size - 1 && !current.excludedSeries.contains(seriesName)
            val nextExcluded = if (isAlreadyIsolating) {
                emptySet()
            } else {
                activeNames.filter { it != seriesName }.toSet()
            }
            current.copy(excludedSeries = nextExcluded)
        }
        recomputeStats()
    }

    // Dynamic series naming logic
    private fun getActiveSeriesNames(segments: Set<String>): List<String> {
        val isOnlySolar = segments.size == 1 && segments.first().contains("Solar") && !segments.first().contains("Internal")
        return if (isOnlySolar) {
            allSkus
        } else {
            segments.toList()
        }
    }

    private fun recomputeStats() {
        val currentFilters = _filters.value
        val metric = currentFilters.activeMetric

        calculationJob?.cancel()
        calculationJob = viewModelScope.launch(Dispatchers.Default) {
            // 1. Resolve Active Range & AnchorDate
            val fyStartYear = currentFilters.selectedFY.split("-").firstOrNull()?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        
        // Find anchorDate based on filters
        activeAnchorDate = when {
            currentFilters.customEndDate != null -> currentFilters.customEndDate
            currentFilters.matrixMonth != null -> {
                val cal = Calendar.getInstance()
                val mIdx = getMonthIndexFromName(currentFilters.matrixMonth)
                val calYear = if (mIdx >= 3) fyStartYear else fyStartYear + 1
                cal.set(calYear, mIdx, 1)
                
                // Set to end of month
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                
                // Don't overshoot global max
                if (cal.time.after(globalMaxDate)) globalMaxDate else cal.time
            }
            currentFilters.selectedQuarter != null -> {
                val cal = Calendar.getInstance()
                val endMIdx = when (currentFilters.selectedQuarter) {
                    0 -> Calendar.JUNE
                    1 -> Calendar.SEPTEMBER
                    2 -> Calendar.DECEMBER
                    else -> Calendar.MARCH
                }
                val calYear = if (endMIdx >= 3) fyStartYear else fyStartYear + 1
                cal.set(calYear, endMIdx, 1)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                
                if (cal.time.after(globalMaxDate)) globalMaxDate else cal.time
            }
            else -> globalMaxDate
        }

        val anchorCal = Calendar.getInstance().apply { time = activeAnchorDate }
        val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH)
        val anchorMonth = anchorCal.get(Calendar.MONTH)
        val anchorYear = anchorCal.get(Calendar.YEAR)
        
        // Determine chronological boundaries of the active view
        var filterStartTime = 0L
        var filterEndTime = Long.MAX_VALUE

        when {
            currentFilters.customStartDate != null && currentFilters.customEndDate != null -> {
                filterStartTime = currentFilters.customStartDate.time
                filterEndTime = currentFilters.customEndDate.time
            }
            currentFilters.matrixMonth != null -> {
                val cal = Calendar.getInstance()
                val mIdx = getMonthIndexFromName(currentFilters.matrixMonth)
                val calYear = if (mIdx >= 3) fyStartYear else fyStartYear + 1
                
                cal.set(calYear, mIdx, 1, 0, 0, 0)
                filterStartTime = cal.timeInMillis
                
                if (currentFilters.selectedWeek != null) {
                    val sDay = (currentFilters.selectedWeek - 1) * 7 + 1
                    val eDay = minOf(cal.getActualMaximum(Calendar.DAY_OF_MONTH), currentFilters.selectedWeek * 7)
                    cal.set(Calendar.DAY_OF_MONTH, sDay)
                    filterStartTime = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, eDay)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    filterEndTime = cal.timeInMillis
                } else if (currentFilters.selectedDay != null) {
                    cal.set(Calendar.DAY_OF_MONTH, currentFilters.selectedDay)
                    filterStartTime = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    filterEndTime = cal.timeInMillis
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    filterEndTime = cal.timeInMillis
                }
            }
            currentFilters.selectedQuarter != null -> {
                val cal = Calendar.getInstance()
                val qMonths = when (currentFilters.selectedQuarter) {
                    0 -> listOf(Calendar.APRIL, Calendar.MAY, Calendar.JUNE)
                    1 -> listOf(Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER)
                    2 -> listOf(Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER)
                    else -> listOf(Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH)
                }
                val sYear = if (qMonths[0] >= 3) fyStartYear else fyStartYear + 1
                val eYear = if (qMonths[2] >= 3) fyStartYear else fyStartYear + 1
                
                cal.set(sYear, qMonths[0], 1, 0, 0, 0)
                filterStartTime = cal.timeInMillis
                
                cal.set(eYear, qMonths[2], 1)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                filterEndTime = cal.timeInMillis
            }
            else -> {
                // Entire FY
                val cal = Calendar.getInstance()
                cal.set(fyStartYear, Calendar.APRIL, 1, 0, 0, 0)
                filterStartTime = cal.timeInMillis
                
                cal.set(fyStartYear + 1, Calendar.MARCH, 31, 23, 59, 59)
                filterEndTime = cal.timeInMillis
            }
        }

        // Helper to extract the value of selected category
        val getValFn: (GrewRecord) -> Double = {
            when (metric) {
                DashboardMetric.Amount -> it.valCr
                DashboardMetric.MW -> it.mw
                DashboardMetric.Qty -> it.qty
            }
        }

        // Series naming setup
        val isOnlySolar = currentFilters.selectedSegments.size == 1 && 
                          currentFilters.selectedSegments.first().contains("Solar") && 
                          !currentFilters.selectedSegments.first().contains("Internal")
        
        val getPlotKey: (GrewRecord) -> String = { if (isOnlySolar) it.wp else it.segment }
        val activeSeriesNames = getActiveSeriesNames(currentFilters.selectedSegments)

        // Filter the transactions
        val filteredTx = allRecords.filter { tx ->
            // Segment restriction
            currentFilters.selectedSegments.contains(tx.segment) &&
            // Exclude series if toggled out
            !currentFilters.excludedSeries.contains(getPlotKey(tx)) &&
            // Drilldown locks
            (currentFilters.salesHeads.isEmpty() || currentFilters.salesHeads.contains(tx.salesHead)) &&
            (currentFilters.customers.isEmpty() || currentFilters.customers.contains(tx.customer)) &&
            (currentFilters.skus.isEmpty() || currentFilters.skus.contains(tx.wp))
        }

        // Dispatched or Pending filter separation
        val targetStateTx = filteredTx.filter { it.isPending == currentFilters.pendingOnly }

        // --- MICRO KPIs PACED CALCULATIONS ---
        // Sums over current view boundaries
        var periodSales = 0.0
        val periodSalesBreakdown = mutableMapOf<String, Double>()

        targetStateTx.forEach { tx ->
            if (tx.date.time in filterStartTime..filterEndTime) {
                val v = getValFn(tx)
                periodSales += v
                val k = getPlotKey(tx)
                periodSalesBreakdown[k] = (periodSalesBreakdown[k] ?: 0.0) + v
            }
        }

        // Paced pacing boundaries for micro KPIs
        val pacedCompareTx = filteredTx.filter { !it.isPending } // Paced calculations compare strictly completed revenues

        // MTD current year pacing vs previous month pacing vs previous year pacing
        fun getPacedSumForMonth(year: Int, monthIdx: Int, maxDay: Int): Double {
            return pacedCompareTx.filter {
                it.year == year && it.monthIdx == monthIdx && it.day <= maxDay
            }.sumOf(getValFn)
        }

        val mtd = getPacedSumForMonth(anchorYear, anchorMonth, anchorDay)
        
        // MTD Breakdown
        val mtdBreakdown = pacedCompareTx.filter {
            it.year == anchorYear && it.monthIdx == anchorMonth && it.day <= anchorDay
        }.groupBy { getPlotKey(it) }.mapValues { entry -> entry.value.sumOf(getValFn) }

        // MoM pacing comparison equivalent day
        val prevMonthCal = Calendar.getInstance().apply { 
            set(anchorYear, anchorMonth, 1)
            add(Calendar.MONTH, -1)
        }
        val prevMonthVal = getPacedSumForMonth(prevMonthCal.get(Calendar.YEAR), prevMonthCal.get(Calendar.MONTH), anchorDay)
        val mtdPacing = calcPercentageChange(mtd, prevMonthVal)

        // QTD pacing (sum from start of quarter up to anchorMonth and pacing day)
        fun getQTDPaced(year: Int, activeMonth: Int, maxDay: Int): Double {
            val qStartMonth = (activeMonth / 3) * 3
            var sum = 0.0
            for (m in qStartMonth..activeMonth) {
                sum += if (m == activeMonth) {
                    getPacedSumForMonth(year, m, maxDay)
                } else {
                    getPacedSumForMonth(year, m, 31) // Full prior months in quarter
                }
            }
            return sum
        }

        val qtd = getQTDPaced(anchorYear, anchorMonth, anchorDay)
        
        // QTD Breakdown
        val qtdBreakdown = pacedCompareTx.filter {
            val qStartMonth = (anchorMonth / 3) * 3
            it.year == anchorYear && it.monthIdx in qStartMonth..anchorMonth && (it.monthIdx != anchorMonth || it.day <= anchorDay)
        }.groupBy { getPlotKey(it) }.mapValues { entry -> entry.value.sumOf(getValFn) }

        val prevYearQTDPaced = getQTDPaced(anchorYear - 1, anchorMonth, anchorDay)
        val qtdPacing = calcPercentageChange(qtd, prevYearQTDPaced)

        // YTD pacing (starts April 1st of active FY)
        fun getYTDPraced(fyStartYr: Int, activeMonth: Int, maxDay: Int): Double {
            var sum = 0.0
            var curM = Calendar.APRIL
            var curY = fyStartYr
            
            var guard = 0
            while (guard++ < 240) {
                sum += if (curM == activeMonth && curY == (if (activeMonth >= 3) fyStartYr else fyStartYr + 1)) {
                    getPacedSumForMonth(curY, curM, maxDay)
                    break
                } else {
                    getPacedSumForMonth(curY, curM, 31)
                }
                
                curM++
                if (curM > Calendar.DECEMBER) {
                    curM = Calendar.JANUARY
                    curY++
                }
            }
            return sum
        }

        val ytd = getYTDPraced(fyStartYear, anchorMonth, anchorDay)

        // YTD Breakdown
        val ytdBreakdown = pacedCompareTx.filter {
            val isMatch = if (it.monthIdx >= 3) {
                it.year == fyStartYear && (it.monthIdx < anchorMonth || (it.monthIdx == anchorMonth && it.day <= anchorDay))
            } else {
                it.year == fyStartYear + 1 && (it.monthIdx < anchorMonth || (it.monthIdx == anchorMonth && it.day <= anchorDay))
            }
            isMatch
        }.groupBy { getPlotKey(it) }.mapValues { entry -> entry.value.sumOf(getValFn) }

        val prevYearYTDPaced = getYTDPraced(fyStartYear - 1, anchorMonth, anchorDay)
        val ytdPacing = calcPercentageChange(ytd, prevYearYTDPaced)

        // Pending Pipeline
        val pendingTx = filteredTx.filter { it.isPending }
        var pending = 0.0
        val pendingBreakdown = mutableMapOf<String, Double>()

        pendingTx.forEach { tx ->
            if (tx.date.time in filterStartTime..filterEndTime) {
                val v = getValFn(tx)
                pending += v
                val k = getPlotKey(tx)
                pendingBreakdown[k] = (pendingBreakdown[k] ?: 0.0) + v
            }
        }

        // --- MATRIX DATA rows (Apr-Mar) ---
        val matrixRows = mutableListOf<MatrixRowItem>()
        val monthsAbbr = listOf("Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar")

        // Build 12 chronological records
        monthsAbbr.forEachIndexed { fMonthIdx, mName ->
            val mCalIdx = (fMonthIdx + 3) % 12
            val mCalYr = if (fMonthIdx < 9) fyStartYear else fyStartYear + 1

            // Exclude future month lines in matrix if looking at active/ongoing year
            val isCurrentFY = filters.value.selectedFY == getFYString(globalMaxDate)
            val activeFMonthIdx = getFiscalMonthIndex(globalMaxDate)

            if (isCurrentFY && fMonthIdx > activeFMonthIdx) {
                matrixRows.add(
                    MatrixRowItem(
                        monthName = mName,
                        revenueCr = 0.0,
                        capacityMw = 0.0,
                        volumeQty = 0.0,
                        momChange = null,
                        qoqChange = null,
                        yoyChange = null
                    )
                )
            } else {
                // Completed sum
                val monthDataTx = filteredTx.filter {
                    it.year == mCalYr && it.monthIdx == mCalIdx && it.isPending == currentFilters.pendingOnly
                }
                
                val rev = monthDataTx.sumOf { it.valCr }
                val cap = monthDataTx.sumOf { it.mw }
                val vol = monthDataTx.sumOf { it.qty }

                // Paced calculations
                val curPaced = getPacedSumForMonth(mCalYr, mCalIdx, anchorDay)
                
                // MoM compare
                val prevCal = Calendar.getInstance().apply {
                    set(mCalYr, mCalIdx, 1)
                    add(Calendar.MONTH, -1)
                }
                val prevMonthPaced = getPacedSumForMonth(prevCal.get(Calendar.YEAR), prevCal.get(Calendar.MONTH), anchorDay)
                val mom = calcPercentageChange(curPaced, prevMonthPaced)

                // YoY compare
                val prevYearPaced = getPacedSumForMonth(mCalYr - 1, mCalIdx, anchorDay)
                val yoy = calcPercentageChange(curPaced, prevYearPaced)

                // QoQ compare (QTD cumulative paced sum vs prior year-quarter equivalent)
                val curQTDPaced = getQTDPaced(mCalYr, mCalIdx, anchorDay)
                val prevYearQTDPacedLocal = getQTDPaced(mCalYr - 1, mCalIdx, anchorDay)
                val qoq = calcPercentageChange(curQTDPaced, prevYearQTDPacedLocal)

                matrixRows.add(
                    MatrixRowItem(
                        monthName = mName,
                        revenueCr = rev,
                        capacityMw = cap,
                        volumeQty = vol,
                        momChange = mom,
                        qoqChange = qoq,
                        yoyChange = yoy
                    )
                )
            }
        }

        // Matrix Total Row
        val totalRev = matrixRows.sumOf { it.revenueCr }
        val totalCap = matrixRows.sumOf { it.capacityMw }
        val totalVol = matrixRows.sumOf { it.volumeQty }
        matrixRows.add(
            MatrixRowItem(
                monthName = "Total",
                revenueCr = totalRev,
                capacityMw = totalCap,
                volumeQty = totalVol,
                momChange = null,
                qoqChange = null,
                yoyChange = null
            )
        )

        // --- CONTRIBUTOR LEADERBOARDS ---
        // Filter rows by timestamp for widgets
        val activePeriodTx = targetStateTx.filter { it.date.time in filterStartTime..filterEndTime }

        // Sales Heads
        val salesRepLeaders = activePeriodTx.groupBy { it.salesHead }.map { (sh, txs) ->
            val v = txs.sumOf(getValFn)
            val uniqueCustomers = txs.map { it.customer }.distinct().size
            ContributorItem(sh, v, 0.0, uniqueCustomers)
        }.sortedByDescending { it.value }
        
        val totalShVal = salesRepLeaders.sumOf { it.value }
        val finalLeaders = salesRepLeaders.map {
            it.copy(percentage = if (totalShVal > 0) (it.value / totalShVal) * 100.0 else 0.0)
        }

        // Clients / Customers
        val clientRaw = activePeriodTx.groupBy { it.customer }.map { (sh, txs) ->
            ContributorItem(sh, txs.sumOf(getValFn), 0.0)
        }.sortedByDescending { it.value }

        val totalClientVal = clientRaw.sumOf { it.value }
        val finalClients = clientRaw.map {
            it.copy(percentage = if (totalClientVal > 0) (it.value / totalClientVal) * 100.0 else 0.0)
        }

        // SKUs
        val skewRaw = activePeriodTx.groupBy { it.wp }.map { (sh, txs) ->
            ContributorItem(sh, txs.sumOf(getValFn), 0.0)
        }.sortedByDescending { it.value }

        val totalSkewVal = skewRaw.sumOf { it.value }
        val finalSkus = skewRaw.map {
            it.copy(percentage = if (totalSkewVal > 0) (it.value / totalSkewVal) * 100.0 else 0.0)
        }

        // --- VELOCITY PLOT DATA POINTS ---
        val points = mutableListOf<VelocityPoint>()

        when (currentFilters.velocityMode) {
            VelocityMode.Quarterly -> {
                val qLabels = listOf("Q1 (Apr-Jun)", "Q2 (Jul-Sep)", "Q3 (Oct-Dec)", "Q4 (Jan-Mar)")
                
                // If quarter is selected, isolate it. Or show all quarters of year
                val activeQs = if (currentFilters.selectedQuarter != null) listOf(currentFilters.selectedQuarter) else listOf(0, 1, 2, 3)
                
                activeQs.forEach { qIdx ->
                    val qName = qLabels[qIdx]
                    val qMonths = when (qIdx) {
                        0 -> listOf(Calendar.APRIL, Calendar.MAY, Calendar.JUNE)
                        1 -> listOf(Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER)
                        2 -> listOf(Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER)
                        else -> listOf(Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH)
                    }
                    
                    val qTx = activePeriodTx.filter {
                        val cal = Calendar.getInstance().apply { time = it.date }
                        val yrMatch = if (cal.get(Calendar.MONTH) >= 3) fyStartYear else fyStartYear + 1
                        yrMatch == cal.get(Calendar.YEAR) && qMonths.contains(cal.get(Calendar.MONTH))
                    }
                    
                    val keyMap = qTx.groupBy { getPlotKey(it) }.mapValues { it.value.sumOf(getValFn) }
                    points.add(VelocityPoint(qName, keyMap))
                }
            }
            VelocityMode.Monthly -> {
                // If Month is selected, focus single column. Else render Apr-Mar columns
                val activeMonths = if (currentFilters.matrixMonth != null) listOf(currentFilters.matrixMonth) else monthsAbbr
                activeMonths.forEach { mName ->
                    val mCalIdx = (getMonthIndexFromName(mName))
                    val mCalYr = if (getMonthIndexFromName(mName) >= 3) fyStartYear else fyStartYear + 1
                    
                    val mTx = activePeriodTx.filter {
                        it.year == mCalYr && it.monthIdx == mCalIdx
                    }
                    val keyMap = mTx.groupBy { getPlotKey(it) }.mapValues { it.value.sumOf(getValFn) }
                    points.add(VelocityPoint(mName, keyMap))
                }
            }
            VelocityMode.Weekly -> {
                // Weeks inside the active focus months
                val wLabels = listOf("W1", "W2", "W3", "W4", "W5")
                val activeMonths = if (currentFilters.matrixMonth != null) listOf(currentFilters.matrixMonth) else monthsAbbr
                
                val weekIndices = if (currentFilters.selectedWeek != null) listOf(currentFilters.selectedWeek) else listOf(1, 2, 3, 4, 5)
                
                weekIndices.forEach { wIdx ->
                    val wName = wLabels[wIdx - 1]
                    val wTx = activePeriodTx.filter {
                        val monthLabel = getMonthName(it.monthIdx)
                        activeMonths.contains(monthLabel) && it.week == wIdx
                    }
                    val keyMap = wTx.groupBy { getPlotKey(it) }.mapValues { it.value.sumOf(getValFn) }
                    points.add(VelocityPoint(wName, keyMap))
                }
            }
            VelocityMode.Daily -> {
                // Days 1-31 inside focused month (matrixMonth is guaranteed here)
                val dayIndices = if (currentFilters.selectedDay != null) listOf(currentFilters.selectedDay) else (1..31).toList()
                dayIndices.forEach { dIdx ->
                    val dLabel = "$dIdx"
                    val dTx = activePeriodTx.filter {
                        it.day == dIdx
                    }
                    val keyMap = dTx.groupBy { getPlotKey(it) }.mapValues { it.value.sumOf(getValFn) }
                    points.add(VelocityPoint(dLabel, keyMap))
                }
            }
        }

        // --- CONCENTRATION & INTELLIGENCE DATA ---
        // Yield realization per MW: Net revenue Cr divided by capacity MW
        val aggregateMw = activePeriodTx.sumOf { it.mw }
        val aggregateRev = activePeriodTx.sumOf { it.valCr }
        val yield = if (aggregateMw > 0.0) aggregateRev / aggregateMw else 0.0

        // HHI calculation helper (Sum of squared percentages of sales)
        fun calcHHI(list: List<ContributorItem>): Double {
            val total = list.sumOf { it.value }
            if (total <= 0.0) return 0.0
            return list.sumOf {
                val share = (it.value / total) * 100.0
                share * share
            }
        }

        val hhiCust = calcHHI(finalClients)
        val hhiProd = calcHHI(finalSkus)

        val top5CusShare = finalClients.take(5).sumOf { it.percentage }
        val top3SkuShare = finalSkus.take(3).sumOf { it.percentage }

        // Trailing 7 days average velocity projection
        val dayInMillis = 24 * 60 * 60 * 1000L
        val last7DaysSalesSum = pacedCompareTx.filter {
            it.date.time in (activeAnchorDate.time - 7 * dayInMillis)..activeAnchorDate.time
        }.sumOf(getValFn)
        
        val cal = Calendar.getInstance().apply { time = activeAnchorDate }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAvg = last7DaysSalesSum / 7.0
        val velocityProjection = dailyAvg * daysInMonth

        val concentration = ConcentrationStats(
            hhiCustomer = hhiCust,
            hhiProduct = hhiProd,
            top5CustomerShare = top5CusShare,
            top3ProductShare = top3SkuShare,
            isDiversifiedCustomer = hhiCust < 1500,
            isDiversifiedProduct = hhiProd < 1500,
            trailing7DayVelocityProjection = velocityProjection,
            yieldRealizationPerMw = yield
        )

        val sFilters = currentFilters
        val skuFilteredTx = allRecords.filter { tx ->
            sFilters.selectedSegments.contains(tx.segment) &&
            (sFilters.salesHeads.isEmpty() || sFilters.salesHeads.contains(tx.salesHead)) &&
            (sFilters.customers.isEmpty() || sFilters.customers.contains(tx.customer)) &&
            tx.isPending == sFilters.pendingOnly &&
            tx.date.time in filterStartTime..filterEndTime
        }
        val applicableSkusList = skuFilteredTx.map { it.wp }.distinct().sorted()

            _stats.update {
                DashboardStats(
                    periodSales = periodSales,
                    periodSalesBreakdown = periodSalesBreakdown,
                    mtd = mtd,
                    mtdBreakdown = mtdBreakdown,
                    mtdPacingChange = mtdPacing,
                    qtd = qtd,
                    qtdBreakdown = qtdBreakdown,
                    qtdPacingChange = qtdPacing,
                    ytd = ytd,
                    ytdBreakdown = ytdBreakdown,
                    ytdPacingChange = ytdPacing,
                    pending = pending,
                    pendingBreakdown = pendingBreakdown,
                    matrix = matrixRows,
                    salesLeaders = finalLeaders,
                    clientDistribution = finalClients,
                    skuDistribution = finalSkus,
                    velocitySeries = points,
                    activeSeriesNames = activeSeriesNames,
                    concentration = concentration,
                    anchorDate = activeAnchorDate,
                    applicableSkus = applicableSkusList
                )
            }
        }
    }

    private fun calcPercentageChange(current: Double, baseline: Double): Double? {
        if (baseline <= 0.0) return if (current > 0.0) 100.0 else null
        return ((current - baseline) / baseline) * 100.0
    }

    // Month translations
    private fun getMonthIndexFromName(name: String): Int {
        return when (name) {
            "Apr" -> Calendar.APRIL
            "May" -> Calendar.MAY
            "Jun" -> Calendar.JUNE
            "Jul" -> Calendar.JULY
            "Aug" -> Calendar.AUGUST
            "Sep" -> Calendar.SEPTEMBER
            "Oct" -> Calendar.OCTOBER
            "Nov" -> Calendar.NOVEMBER
            "Dec" -> Calendar.DECEMBER
            "Jan" -> Calendar.JANUARY
            "Feb" -> Calendar.FEBRUARY
            "Mar" -> Calendar.MARCH
            else -> Calendar.APRIL
        }
    }

    private fun getMonthName(idx: Int): String {
        return when (idx) {
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            else -> "Apr"
        }
    }

    private fun getFYString(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        val m = cal.get(Calendar.MONTH)
        val y = cal.get(Calendar.YEAR)
        return if (m >= 3) {
            "$y-${(y + 1).toString().takeLast(2)}"
        } else {
            "${y - 1}-${y.toString().takeLast(2)}"
        }
    }

    private fun getFiscalMonthIndex(date: Date): Int {
        val cal = Calendar.getInstance().apply { time = date }
        val m = cal.get(Calendar.MONTH)
        return if (m >= 3) m - 3 else m + 9
    }

    private suspend fun fetchRecordsDirectlyFromSupabase(url: String, key: String, authToken: String? = null): List<GrewRecord>? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val recordTables = listOf("grew_records", "sheet_records", "records", "sheet_data")
        val authHeader = if (!authToken.isNullOrBlank()) "Bearer $authToken" else "Bearer $key"
        for (table in recordTables) {
            try {
                val req = Request.Builder()
                    .url("${url.trimEnd('/')}/rest/v1/$table")
                    .addHeader("apikey", key)
                    .addHeader("Authorization", authHeader)
                    .get()
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty() && bodyStr.trim().startsWith("[")) {
                            val arr = JSONArray(bodyStr)
                            if (arr.length() > 0) {
                                val records = mutableListOf<GrewRecord>()
                                for (i in 0 until arr.length()) {
                                    val obj = arr.optJSONObject(i) ?: continue
                                    // Parse fields dynamically with defaults
                                    var dateStr = ""
                                    val keysIter = obj.keys()
                                    while (keysIter.hasNext()) {
                                        val k = keysIter.next()
                                        if (k.equals("date", ignoreCase = true) || 
                                            k.equals("created_at", ignoreCase = true) || 
                                            k.equals("timestamp", ignoreCase = true) ||
                                            k.equals("invoicedate", ignoreCase = true) ||
                                            k.equals("invoice_date", ignoreCase = true) ||
                                            (k.contains("invoice") && k.contains("date"))) {
                                            dateStr = obj.optString(k) ?: ""
                                            break
                                        }
                                    }
                                    if (dateStr.isEmpty()) {
                                        dateStr = obj.optString("date") ?: obj.optString("created_at") ?: ""
                                    }
                                    val dateVal = parseDateString(dateStr) ?: continue
                                    
                                    val segment = obj.optString("segment") ?: "Solar Modules"
                                    
                                    var salesHead = "Amit Sharma"
                                    val keysIterSh = obj.keys()
                                    while (keysIterSh.hasNext()) {
                                        val k = keysIterSh.next()
                                        if (k.equals("sales_head", ignoreCase = true) || k.equals("saleshead", ignoreCase = true) || k.equals("agent", ignoreCase = true)) {
                                            salesHead = obj.optString(k) ?: "Amit Sharma"
                                        }
                                    }
                                    
                                    var customer = "Unknown Customer"
                                    val keysIterCust = obj.keys()
                                    while (keysIterCust.hasNext()) {
                                        val k = keysIterCust.next()
                                        if (k.equals("customer", ignoreCase = true) || k.equals("client", ignoreCase = true)) {
                                            customer = obj.optString(k) ?: "Unknown Customer"
                                        }
                                    }
                                    
                                    var wp = "Generic WP"
                                    val keysIterWp = obj.keys()
                                    while (keysIterWp.hasNext()) {
                                        val k = keysIterWp.next()
                                        if (k.equals("wp", ignoreCase = true) || k.equals("sku", ignoreCase = true) || k.equals("product", ignoreCase = true) || k.equals("wp_text", ignoreCase = true)) {
                                            wp = obj.optString(k) ?: "Generic WP"
                                        }
                                    }
                                    
                                    var valCr = 0.0
                                    val keysIterVal = obj.keys()
                                    while (keysIterVal.hasNext()) {
                                        val k = keysIterVal.next()
                                        if (k.equals("val_cr", ignoreCase = true) || k.equals("valcr", ignoreCase = true) || k.equals("value", ignoreCase = true) || k.equals("revenue", ignoreCase = true)) {
                                            val valCrStr = (obj.optString(k) ?: "0.0").replace(",", "").trim()
                                            valCr = valCrStr.toDoubleOrNull() ?: 0.0
                                        }
                                    }
                                    
                                    var qty = 0.0
                                    val keysIterQty = obj.keys()
                                    while (keysIterQty.hasNext()) {
                                        val k = keysIterQty.next()
                                        if (k.equals("qty", ignoreCase = true) || k.equals("quantity", ignoreCase = true) || k.equals("volume", ignoreCase = true)) {
                                            val qtyStr = (obj.optString(k) ?: "0.0").replace(",", "").trim()
                                            qty = qtyStr.toDoubleOrNull() ?: 0.0
                                        }
                                    }
                                    
                                    var mw = 0.0
                                    val keysIterMw = obj.keys()
                                    while (keysIterMw.hasNext()) {
                                        val k = keysIterMw.next()
                                        if (k.equals("mw", ignoreCase = true) || k.equals("capacity", ignoreCase = true)) {
                                            val mwStr = (obj.optString(k) ?: "0.0").replace(",", "").trim()
                                            mw = mwStr.toDoubleOrNull() ?: 0.0
                                        }
                                    }
                                    
                                    var isPending = false
                                    val keysIterPend = obj.keys()
                                    while (keysIterPend.hasNext()) {
                                        val k = keysIterPend.next()
                                        if (k.equals("is_pending", ignoreCase = true) || k.equals("ispending", ignoreCase = true) || k.equals("pending", ignoreCase = true)) {
                                            isPending = obj.optBoolean(k, false)
                                        } else if (k.equals("status", ignoreCase = true)) {
                                            val statusStr = obj.optString(k) ?: ""
                                            if (statusStr.equals("pending", ignoreCase = true)) {
                                                isPending = true
                                            }
                                        }
                                    }
                                                    
                                    records.add(
                                        GrewRecord(
                                            date = dateVal,
                                            segment = segment,
                                            salesHead = salesHead,
                                            customer = customer,
                                            wp = wp,
                                            valCr = valCr,
                                            qty = qty,
                                            mw = mw,
                                            isPending = isPending
                                        )
                                    )
                                }
                                if (records.isNotEmpty()) {
                                    return@withContext records
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue scanning candidates
            }
        }
        return@withContext null
    }

    private suspend fun fetchCsvDataFromSupabaseConfig(url: String, key: String, authToken: String? = null): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val configTables = listOf("app_config", "sheet_config", "settings", "config", "sheets")
        val authHeader = if (!authToken.isNullOrBlank()) "Bearer $authToken" else "Bearer $key"
        for (table in configTables) {
            try {
                val req = Request.Builder()
                    .url("${url.trimEnd('/')}/rest/v1/$table")
                    .addHeader("apikey", key)
                    .addHeader("Authorization", authHeader)
                    .get()
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty() && bodyStr.trim().startsWith("[")) {
                            val arr = JSONArray(bodyStr)
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val keysIter = obj.keys()
                                var optionKey: String? = null
                                var optionVal: String? = null
                                while (keysIter.hasNext()) {
                                    val k = keysIter.next()
                                    val v = obj.optString(k)?.trim() ?: ""
                                    if (k.equals("key", ignoreCase = true) || k.equals("name", ignoreCase = true) || k.equals("config_key", ignoreCase = true) || k.equals("configKey", ignoreCase = true)) {
                                        optionKey = v
                                    } else if (k.equals("value", ignoreCase = true) || k.equals("val", ignoreCase = true) || k.equals("config_value", ignoreCase = true) || k.equals("configValue", ignoreCase = true) || k.equals("content", ignoreCase = true)) {
                                        optionVal = v
                                    }
                                }
                                if (optionKey != null && optionVal != null) {
                                    if (optionKey.equals("SHEET_CSV", ignoreCase = true) || 
                                        optionKey.equals("CSV_DATA", ignoreCase = true) || 
                                        optionKey.equals("SHEET_DATA", ignoreCase = true) || 
                                        optionKey.equals("CSV_CONTENT", ignoreCase = true) ||
                                        optionKey.equals("SHEET_CONTENT", ignoreCase = true) ||
                                        optionKey.equals("SHEET_TEXT", ignoreCase = true)) {
                                        if (optionVal.isNotEmpty() && optionVal.contains(",") && optionVal.contains("\n")) {
                                            return@withContext optionVal
                                        }
                                    }
                                }
                                
                                // Alternatively, check all fields directly in the object for a giant CSV field
                                val objectKeys = obj.keys()
                                while (objectKeys.hasNext()) {
                                    val k = objectKeys.next()
                                    if (k.equals("csv_data", ignoreCase = true) || 
                                        k.equals("sheet_csv", ignoreCase = true) || 
                                        k.equals("csv_content", ignoreCase = true) || 
                                        k.equals("csv", ignoreCase = true)) {
                                        val valStr = obj.optString(k)?.trim() ?: ""
                                        if (valStr.isNotEmpty() && valStr.contains(",") && valStr.contains("\n")) {
                                            return@withContext valStr
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue scanning candidates
            }
        }
        return@withContext null
    }

    fun loadSheetData(context: android.content.Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SheetSyncState.Syncing
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_ANON_KEY
            val hasSupabase = url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
                              key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")
            
            var syncSource = "Supabase API Server"
            var authToken: String? = null
            if (context != null) {
                try {
                    val prefs = context.getSharedPreferences("grew_auth", android.content.Context.MODE_PRIVATE)
                    authToken = prefs.getString("grew_oauth_token", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (hasSupabase) {
                // First attempt: Try fetching records directly from Supabase tables (grew_records, records, sheet_data, etc.)
                val supabaseRecords = fetchRecordsDirectlyFromSupabase(url, key, authToken)
                if (supabaseRecords != null && supabaseRecords.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        allRecords = supabaseRecords
                        _syncState.value = SheetSyncState.Success(
                            supabaseRecords.size, 
                            "Supabase Database Table", 
                            "supabase_direct_records"
                        )
                        resetToLatestAnchor()
                    }
                    return@launch
                }

                // Second attempt: Try fetching a CSV configuration value directly from Supabase config tables
                val supabaseCsv = fetchCsvDataFromSupabaseConfig(url, key, authToken)
                if (supabaseCsv != null && supabaseCsv.isNotEmpty()) {
                    val records = parseSheetData(supabaseCsv)
                    if (records.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            allRecords = records
                            _syncState.value = SheetSyncState.Success(
                                records.size, 
                                "Supabase Config CSV", 
                                "supabase_config_csv"
                            )
                            resetToLatestAnchor()
                        }
                        return@launch
                    }
                }
            }

            // Third attempt: Fallback to reading the Sheet ID from Supabase, then fetching from Google Sheets URL
            var resolvedSheetId: String? = null
            if (hasSupabase) {
                resolvedSheetId = fetchSheetIdFromSupabase(url, key, authToken)
            }

            if (resolvedSheetId == null) {
                // Default spreadsheet ID containing live records for fallback
                resolvedSheetId = "1rLq5T_L9AAtZgSOnlD7_P0yB6xT3_XlyW-0fG34I2F4"
                syncSource = "Local Config (Offline Fallback)"
            }

            try {
                val csvUrl = "https://docs.google.com/spreadsheets/d/$resolvedSheetId/export?format=csv"
                val client = OkHttpClient()
                val request = Request.Builder().url(csvUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val csvText = response.body?.string() ?: ""
                        val records = parseSheetData(csvText)
                        withContext(Dispatchers.Main) {
                            if (records.isNotEmpty()) {
                                allRecords = records
                                _syncState.value = SheetSyncState.Success(records.size, syncSource, resolvedSheetId ?: "")
                                resetToLatestAnchor()
                            } else {
                                _syncState.value = SheetSyncState.Error("Google sheet structure parsing returned alternate format or zero records.")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            val errorReason = if (response.code == 401) {
                                "Google Sheets HTTP Error 401: Google Sheet is Private. Please share the Google Sheet as 'Anyone with the link can view' or use Supabase tables."
                            } else {
                                "Google Sheets HTTP Error: ${response.code}"
                            }
                            _syncState.value = SheetSyncState.Error(errorReason)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _syncState.value = SheetSyncState.Error("Failed to fetch Google Sheet: ${e.message ?: "Network Connection Error"}")
                }
            }
        }
    }

    private suspend fun fetchSheetIdFromSupabase(url: String, key: String, authToken: String? = null): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        // Prioritize app_config per core instruction
        val tableCandidates = listOf("app_config", "sheet_config", "settings", "config", "sheets")
        val authHeader = if (!authToken.isNullOrBlank()) "Bearer $authToken" else "Bearer $key"
        
        for (table in tableCandidates) {
            try {
                val req = Request.Builder()
                    .url("${url.trimEnd('/')}/rest/v1/$table")
                    .addHeader("apikey", key)
                    .addHeader("Authorization", authHeader)
                    .get()
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty()) {
                            try {
                                val arr = JSONArray(bodyStr)
                                
                                // First pass: look specifically for keys equal to "SHEET_ID" and extract valid sheet IDs associated in the same row
                                for (i in 0 until arr.length()) {
                                    val obj = arr.optJSONObject(i) ?: continue
                                    val keys = obj.keys()
                                    var isSheetIdKey = false
                                    var associatedValue: String? = null
                                    
                                    while (keys.hasNext()) {
                                        val k = keys.next()
                                        val v = obj.optString(k)?.trim() ?: ""
                                        if (v.equals("SHEET_ID", ignoreCase = true)) {
                                            isSheetIdKey = true
                                        } else if (isValidGoogleSheetId(v)) {
                                            associatedValue = v
                                        }
                                    }
                                    
                                    if (isSheetIdKey && associatedValue != null) {
                                        return@withContext associatedValue
                                    }
                                    
                                    // Column-specific named mapping
                                    var keyField: String? = null
                                    var valField: String? = null
                                    val keysList = mutableListOf<String>()
                                    val keysIter = obj.keys()
                                    while (keysIter.hasNext()) {
                                        keysList.add(keysIter.next())
                                    }
                                    
                                    val foundKeyName = keysList.firstOrNull { 
                                        it.equals("key", ignoreCase = true) || 
                                        it.equals("name", ignoreCase = true) || 
                                        it.equals("config_key", ignoreCase = true) || 
                                        it.equals("configKey", ignoreCase = true) 
                                    }
                                    if (foundKeyName != null) {
                                        keyField = obj.optString(foundKeyName)?.trim()
                                    }
                                    
                                    val foundValName = keysList.firstOrNull { 
                                        it.equals("value", ignoreCase = true) || 
                                        it.equals("val", ignoreCase = true) || 
                                        it.equals("config_value", ignoreCase = true) || 
                                        it.equals("configValue", ignoreCase = true) 
                                    }
                                    if (foundValName != null) {
                                        valField = obj.optString(foundValName)?.trim()
                                    }
                                    
                                    if (keyField != null && keyField.equals("SHEET_ID", ignoreCase = true)) {
                                        if (valField != null && isValidGoogleSheetId(valField)) {
                                            return@withContext valField
                                        }
                                    }
                                }
                                
                                // Second pass generic fallback: looks for candidates in value/id/spreadsheet_id case-insensitively
                                for (i in 0 until arr.length()) {
                                    val obj = arr.optJSONObject(i) ?: continue
                                    val keysIter = obj.keys()
                                    while (keysIter.hasNext()) {
                                        val k = keysIter.next()
                                        if (k.equals("sheet_id", ignoreCase = true) || 
                                            k.equals("spreadsheet_id", ignoreCase = true) || 
                                            k.equals("value", ignoreCase = true) || 
                                            k.equals("val", ignoreCase = true) ||
                                            k.equals("id", ignoreCase = true)) {
                                            val candidate = obj.optString(k)?.trim()
                                            if (candidate != null && isValidGoogleSheetId(candidate)) {
                                                return@withContext candidate
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Failover to stream matching
                            }
                            
                            val match = Regex("1[a-zA-Z0-9-_]{43}").find(bodyStr)
                            if (match != null) {
                                return@withContext match.value
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue scanning fallback candidates
            }
        }
        return@withContext null
    }

    private fun isValidGoogleSheetId(id: String): Boolean {
        return id.length == 44 && id.first() == '1' && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val char = line[i]
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(char)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    private fun parseTsvLine(line: String): List<String> {
        return line.split("\t").map { it.trim().removeSurrounding("\"") }
    }

    private fun parseDateString(dateStr: String): Date? {
        if (dateStr.trim().isEmpty()) return null
        val cleaned = dateStr.trim()
        val formats = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd-MMM-yyyy",
            "dd MMM yyyy",
            "MMM dd, yyyy",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.ENGLISH)
                return sdf.parse(cleaned)
            } catch (e: Exception) {
                // Continue
            }
        }
        val longVal = cleaned.toLongOrNull()
        if (longVal != null) {
            return Date(longVal)
        }
        return null
    }

    private fun parseSheetData(csvContent: String): List<GrewRecord> {
        val records = mutableListOf<GrewRecord>()
        val lines = csvContent.split(java.util.regex.Pattern.compile("\\r?\\n"))
        if (lines.isEmpty()) return emptyList()

        val headerLine = lines[0]
        val isTsv = headerLine.count { it == '\t' } > headerLine.count { it == ',' }
        val headers = if (isTsv) parseTsvLine(headerLine) else parseCsvLine(headerLine)
        
        val headerToIdx = headers.mapIndexed { idx, h -> 
            val cleaned = h.lowercase(Locale.ROOT).replace(" ", "").replace("_", "")
            cleaned to idx
        }.toMap()

        val dateIdx = headerToIdx["invoicedate"] ?: 
                      headerToIdx.keys.firstOrNull { it.contains("invoice") && it.contains("date") }?.let { headerToIdx[it] } ?:
                      headerToIdx["date"] ?: 
                      headerToIdx["createdat"] ?: 
                      headerToIdx.keys.firstOrNull { it.contains("date") }?.let { headerToIdx[it] } ?: -1
        
        val segmentIdx = headerToIdx["segment"] ?: 
                         headerToIdx.keys.firstOrNull { it.contains("segment") || it.contains("category") || it.contains("type") }?.let { headerToIdx[it] } ?: -1
        
        val salesHeadIdx = headerToIdx["saleshead"] ?: 
                           headerToIdx["sales_head"] ?: 
                           headerToIdx["agent"] ?: 
                           headerToIdx.keys.firstOrNull { it.contains("sales") || it.contains("head") || it.contains("manager") || it.contains("owner") }?.let { headerToIdx[it] } ?: -1
        
        val customerIdx = headerToIdx["customer"] ?: 
                          headerToIdx["client"] ?: 
                          headerToIdx.keys.firstOrNull { it.contains("customer") || it.contains("client") || it.contains("buyer") }?.let { headerToIdx[it] } ?: -1
        
        val wpIdx = headerToIdx["wp"] ?: 
                    headerToIdx["sku"] ?: 
                    headerToIdx["product"] ?: 
                    headerToIdx["wptext"] ?: 
                    headerToIdx.keys.firstOrNull { it.contains("product") || it.contains("item") || it.contains("sku") || it.contains("material") }?.let { headerToIdx[it] } ?: -1
        
        val valCrIdx = headerToIdx["valcr"] ?: 
                       headerToIdx["value"] ?: 
                       headerToIdx["revenue"] ?: 
                       headerToIdx["amount"] ?: 
                       headerToIdx.keys.firstOrNull { it.contains("revenue") || it.contains("value") || it.contains("amount") || it.contains("total") || it.contains("cr") }?.let { headerToIdx[it] } ?: -1
        
        val qtyIdx = headerToIdx["qty"] ?: 
                     headerToIdx["quantity"] ?: 
                     headerToIdx["volume"] ?: 
                     headerToIdx.keys.firstOrNull { it.contains("qty") || it.contains("quantity") || it.contains("volume") || it.contains("count") || it.contains("units") }?.let { headerToIdx[it] } ?: -1
        
        val mwIdx = headerToIdx["mw"] ?: 
                    headerToIdx["capacity"] ?: 
                    headerToIdx.keys.firstOrNull { it.contains("mw") || it.contains("capacity") || it.contains("power") || it.contains("watt") }?.let { headerToIdx[it] } ?: -1
        
        val pendingIdx = headerToIdx["ispending"] ?: 
                         headerToIdx["pending"] ?: 
                         headerToIdx["status"] ?: 
                         headerToIdx.keys.firstOrNull { it.contains("pending") || it.contains("status") || it.contains("stage") }?.let { headerToIdx[it] } ?: -1

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val cells = if (isTsv) parseTsvLine(line) else parseCsvLine(line)
            if (cells.size < 2) continue

            try {
                val rawDate = cells.getOrNull(dateIdx) ?: ""
                val parsedDate = parseDateString(rawDate) ?: if (dateIdx == -1) Date() else continue

                val segment = cells.getOrNull(segmentIdx) ?: "Solar Modules"
                val salesHead = cells.getOrNull(salesHeadIdx) ?: "Amit Sharma"
                val customer = cells.getOrNull(customerIdx) ?: "Unknown Customer"
                val wp = cells.getOrNull(wpIdx) ?: "Generic WP"

                val valCr = cells.getOrNull(valCrIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val qty = cells.getOrNull(qtyIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val mw = cells.getOrNull(mwIdx)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

                val rawPending = cells.getOrNull(pendingIdx) ?: ""
                val isPending = rawPending.equals("true", ignoreCase = true) || 
                                rawPending.equals("yes", ignoreCase = true) || 
                                rawPending.equals("pending", ignoreCase = true)

                records.add(
                    GrewRecord(
                        date = parsedDate,
                        segment = segment,
                        salesHead = salesHead,
                        customer = customer,
                        wp = wp,
                        valCr = valCr,
                        qty = qty,
                        mw = mw,
                        isPending = isPending
                    )
                )
            } catch (e: Exception) {
                // Gracefully pass bad lines
            }
        }
        return records
    }
}
