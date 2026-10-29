package com.starmaen.accountingstar.data

import android.content.Context
import android.content.SharedPreferences

class SessionStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("accounting_star_session", Context.MODE_PRIVATE)

    var userId: Long
        get() = prefs.getLong("user_id", 0L)
        set(value) = prefs.edit().putLong("user_id", value).apply()

    var projectId: Long
        get() = prefs.getLong("project_id", 0L)
        set(value) = prefs.edit().putLong("project_id", value).apply()

    var branchId: Long
        get() = prefs.getLong("branch_id", 0L)
        set(value) = prefs.edit().putLong("branch_id", value).apply()

    var warehouseId: Long
        get() = prefs.getLong("warehouse_id", 0L)
        set(value) = prefs.edit().putLong("warehouse_id", value).apply()

    var thermalMac: String
        get() = prefs.getString("thermal_mac", "") ?: ""
        set(value) = prefs.edit().putString("thermal_mac", value).apply()

    var paperWidth: Int
        get() = prefs.getInt("paper_width", 58)
        set(value) = prefs.edit().putInt("paper_width", value).apply()

    var receiptFooter: String
        get() = prefs.getString("receipt_footer", "شكراً لتعاملكم معنا") ?: ""
        set(value) = prefs.edit().putString("receipt_footer", value).apply()

    var taxNumber: String
        get() = prefs.getString("tax_number", "") ?: ""
        set(value) = prefs.edit().putString("tax_number", value).apply()

    fun clearLogin() = prefs.edit().remove("user_id").apply()

    fun clearAll() = prefs.edit().clear().apply()
}
