package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val expenseDao = db.expenseDao()
    private val goalDao = db.goalDao()
    private val settingDao = db.settingDao()
    private val sheetsService = GoogleSheetsService()

    // Flow listings for UI observation
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allSettings: Flow<List<Setting>> = settingDao.getAllSettingsFlow()

    // Settings helpers
    suspend fun getSettingValue(key: String): String? {
        return settingDao.getSetting(key)
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(Setting(key, value))
    }

    suspend fun deleteSetting(key: String) {
        settingDao.deleteSetting(key)
    }

    // Expense actions
    suspend fun addExpense(amount: Double, description: String, category: String, date: String) {
        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amount = amount,
            description = description,
            category = category,
            date = date
        )
        expenseDao.insertExpense(expense)
        triggerBackgroundSync { token, sheetId ->
            sheetsService.addExpense(token, sheetId, expense)
        }
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpense(id)
        triggerBackgroundSync { token, sheetId ->
            sheetsService.deleteExpenseRow(token, sheetId, id)
        }
    }

    // Goal actions
    suspend fun addGoal(name: String, targetAmount: Double, currentAmount: Double, targetDate: String, category: String) {
        val goal = Goal(
            id = UUID.randomUUID().toString(),
            name = name,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetDate = targetDate,
            category = category,
            status = if (currentAmount >= targetAmount) "completed" else "active"
        )
        goalDao.insertGoal(goal)
        triggerBackgroundSync { token, sheetId ->
            sheetsService.addGoal(token, sheetId, goal)
        }
    }

    suspend fun updateGoalAmount(id: String, newAmount: Double) {
        val currentList = goalDao.getAllGoalsList()
        val targetGoal = currentList.find { it.id == id } ?: return
        val updatedGoal = targetGoal.copy(
            currentAmount = newAmount,
            status = if (newAmount >= targetGoal.targetAmount) "completed" else "active"
        )
        goalDao.insertGoal(updatedGoal)
        triggerBackgroundSync { token, sheetId ->
            sheetsService.updateGoalRow(token, sheetId, updatedGoal)
        }
    }

    suspend fun deleteGoal(id: String) {
        goalDao.deleteGoal(id)
        triggerBackgroundSync { token, sheetId ->
            sheetsService.deleteGoalRow(token, sheetId, id)
        }
    }

    // Dynamic state triggered helper
    private suspend fun triggerBackgroundSync(action: suspend (String, String) -> Boolean) {
        val token = getSettingValue("access_token") ?: return
        val state = getSettingValue("sync_state")
        if (state == "OFFLINE") return

        if (token.startsWith("mock_")) {
            saveSetting("sync_state", "CONNECTED")
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            saveSetting("last_synced", "Last Synced: $time")
            return
        }

        val sheetId = getSettingValue("spreadsheet_id") ?: return
        saveSetting("sync_state", "SYNCING")
        try {
            val success = action(token, sheetId)
            if (success) {
                saveSetting("sync_state", "CONNECTED")
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                saveSetting("last_synced", "Last Synced: $time")
            } else {
                saveSetting("sync_state", "ERROR")
            }
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Background sync action failed", e)
            saveSetting("sync_state", "ERROR")
        }
    }

    // Full 2-Way Synchronization Engine
    suspend fun syncNow(): Boolean {
        val token = getSettingValue("access_token")
        val state = getSettingValue("sync_state")
        
        if (token.isNullOrEmpty() || state == "OFFLINE") {
            return false
        }

        if (token.startsWith("mock_")) {
            saveSetting("sync_state", "CONNECTED")
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            saveSetting("last_synced", "Last Synced: $time")
            return true
        }

        saveSetting("sync_state", "SYNCING")

        try {
            var sheetId = getSettingValue("spreadsheet_id")
            if (sheetId.isNullOrEmpty()) {
                // Find existing
                sheetId = sheetsService.findSpreadsheet(token)
                if (sheetId.isNullOrEmpty()) {
                    // Create new
                    sheetId = sheetsService.createSpreadsheet(token)
                }
                if (!sheetId.isNullOrEmpty()) {
                    saveSetting("spreadsheet_id", sheetId)
                } else {
                    saveSetting("sync_state", "ERROR")
                    return false
                }
            }

            // 1. Fetch remote content
            val remoteExpenses = sheetsService.getExpenses(token, sheetId)
            val remoteGoals = sheetsService.getGoals(token, sheetId)

            // 2. Fetch local content
            val localExpenses = expenseDao.getAllExpensesList()
            val localGoals = goalDao.getAllGoalsList()

            // 3. Merge Expenses: Keep all local. If any remote is not in local, import it.
            val localExpMap = localExpenses.associateBy { it.id }.toMutableMap()
            var modified = false
            for (re in remoteExpenses) {
                if (!localExpMap.containsKey(re.id)) {
                    expenseDao.insertExpense(re)
                    localExpMap[re.id] = re
                    modified = true
                }
            }

            // 4. Merge Goals: Keep all local. If any remote is not in local, import it.
            val localGoalMap = localGoals.associateBy { it.id }.toMutableMap()
            for (rg in remoteGoals) {
                if (!localGoalMap.containsKey(rg.id)) {
                    goalDao.insertGoal(rg)
                    localGoalMap[rg.id] = rg
                    modified = true
                }
            }

            // Get merged state lists
            val mergedExpenses = if (modified) expenseDao.getAllExpensesList() else localExpenses
            val mergedGoals = if (modified) goalDao.getAllGoalsList() else localGoals

            // 5. Push full merged set back to Google Sheets to ensure absolute parity
            val pushSuccess = sheetsService.syncAllToSheets(token, sheetId, mergedExpenses, mergedGoals)
            if (pushSuccess) {
                saveSetting("sync_state", "CONNECTED")
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                saveSetting("last_synced", "Last Synced: $time")
                return true
            } else {
                saveSetting("sync_state", "ERROR")
                return false
            }

        } catch (e: Exception) {
            Log.e("FinanceRepo", "Full sync failed", e)
            saveSetting("sync_state", "ERROR")
            return false
        }
    }

    // Force Redirection & Purge Cache
    suspend fun clearAllData() {
        expenseDao.clearAllExpenses()
        goalDao.clearAllGoals()
        settingDao.clearAllSettings()
        
        // Setup initial default values
        saveSetting("currency_code", "USD")
        saveSetting("currency_symbol", "$")
        saveSetting("budget_limit", "1500.0")
        saveSetting("sync_state", "OFFLINE")
    }

    // Initial setup with defaults if empty
    suspend fun initializeDefaults() {
        if (getSettingValue("currency_code") == null) {
            saveSetting("currency_code", "USD")
        }
        if (getSettingValue("currency_symbol") == null) {
            saveSetting("currency_symbol", "$")
        }
        if (getSettingValue("budget_limit") == null) {
            saveSetting("budget_limit", "1500.0")
        }
        if (getSettingValue("sync_state") == null) {
            saveSetting("sync_state", "OFFLINE")
        }
    }
}
