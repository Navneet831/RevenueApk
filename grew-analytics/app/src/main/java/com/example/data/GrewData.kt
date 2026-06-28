package com.example.data

import java.util.Calendar
import java.util.Date
import java.util.Random

data class GrewRecord(
    val date: Date,
    val segment: String,
    val salesHead: String,
    val customer: String,
    val wp: String, // SKU / Product
    val valCr: Double, // Revenue in Crores (₹ Cr)
    val qty: Double, // Volume in Qty
    val mw: Double, // Capacity in MW
    val isPending: Boolean
) {
    val year: Int
    val monthIdx: Int // 0-11
    val day: Int // 1-31
    val week: Int // 1-5
    val monthKey: String // YYYY-MM
    val fiscalYear: String // e.g., "2024-25"
    val quarter: Int // 0, 1, 2, 3 (Apr-Jun, Jul-Sep, Oct-Dec, Jan-Mar)
    val fiscalMonthIdx: Int // 0 to 11 starting from April

    init {
        val cal = Calendar.getInstance().apply { time = date }
        year = cal.get(Calendar.YEAR)
        monthIdx = cal.get(Calendar.MONTH)
        day = cal.get(Calendar.DAY_OF_MONTH)
        week = minOf(5, ((day - 1) / 7) + 1)
        monthKey = String.format("%d-%02d", year, monthIdx + 1)
        
        // Fiscal Year calculation (starts in April)
        fiscalYear = if (monthIdx >= 3) {
            "$year-${(year + 1).toString().takeLast(2)}"
        } else {
            "${year - 1}-${year.toString().takeLast(2)}"
        }

        // Quarter calculation (0: Q1 (Apr-Jun), 1: Q2 (Jul-Sep), 2: Q3 (Oct-Dec), 3: Q4 (Jan-Mar))
        quarter = when (monthIdx) {
            in 3..5 -> 0
            in 6..8 -> 1
            in 9..11 -> 2
            else -> 3
        }

        fiscalMonthIdx = if (monthIdx >= 3) monthIdx - 3 else monthIdx + 9
    }
}

object GrewData {
    val segments = listOf(
        "Solar Modules",
        "Solar Modules (Internal)",
        "Raw Material",
        "Scrap"
    )

    val salesHeads = listOf(
        "Amit Sharma",
        "Priya Patel",
        "Vikram Singh",
        "Nitin Gadkari",
        "Siddharth Malhotra"
    )

    val customers = listOf(
        "Adani Green",
        "Tata Power Solar",
        "Waaree Energies",
        "Suntek Power",
        "CleanMax Solar",
        "Mytrah Vayu",
        "Grew Energy Internal Dist",
        "ReNew Power"
    )

    val skusForSegment = mapOf(
        "Solar Modules" to listOf("540 WP Mono", "545 WP Bifacial", "550 WP Mono", "580 WP DCR", "585 WP TOPCon", "600 WP TOPCon"),
        "Solar Modules (Internal)" to listOf("540 WP Mono", "550 WP Mono", "585 WP TOPCon"),
        "Raw Material" to listOf("Glass Crates", "EVA Sheet Rolls", "Silicon Wafer Scraps"),
        "Scrap" to listOf("Aluminium Frame Scrap", "Glass Crates", "Silicon Wafer Scraps")
    )

    fun generateRecords(): List<GrewRecord> {
        return emptyList()
    }
}
