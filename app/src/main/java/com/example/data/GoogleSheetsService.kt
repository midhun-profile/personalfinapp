package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GoogleSheetsService {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("SheetsAPI", "--> ${request.method} ${request.url}")
            val response = chain.proceed(request)
            Log.d("SheetsAPI", "<-- ${response.code} ${request.method} ${request.url}")
            response
        }
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun findSpreadsheet(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?q=name='Personal Finance Tracker' and mimeType='application/vnd.google-apps.spreadsheet' and trashed=false"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SheetsAPI", "findSpreadsheet failed with code: ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return files.getJSONObject(0).getString("id")
                }
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "findSpreadsheet exception", e)
        }
        return null
    }

    suspend fun createSpreadsheet(accessToken: String): String? {
        val url = "https://sheets.googleapis.com/v4/spreadsheets"
        
        val bodyJson = JSONObject().apply {
            put("properties", JSONObject().apply {
                put("title", "Personal Finance Tracker")
            })
            put("sheets", JSONArray().apply {
                put(JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("title", "Expenses")
                    })
                })
                put(JSONObject().apply {
                    put("properties", JSONObject().apply {
                        put("title", "Goals")
                    })
                })
            })
        }

        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SheetsAPI", "createSpreadsheet failed with code: ${response.code} - ${response.body?.string()}")
                    return null
                }
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val spreadsheetId = json.getString("spreadsheetId")
                
                // Now write headers
                writeHeaders(accessToken, spreadsheetId)
                return spreadsheetId
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "createSpreadsheet exception", e)
        }
        return null
    }

    private suspend fun writeHeaders(accessToken: String, spreadsheetId: String) {
        val expensesHeaders = JSONArray().apply {
            put("ID")
            put("Date")
            put("Amount")
            put("Description")
            put("Category")
        }
        
        val goalsHeaders = JSONArray().apply {
            put("ID")
            put("Goal Name")
            put("Target Amount")
            put("Current Amount")
            put("Target Date")
            put("Category")
            put("Status")
        }

        updateRange(accessToken, spreadsheetId, "Expenses!A1:E1", expensesHeaders)
        updateRange(accessToken, spreadsheetId, "Goals!A1:G1", goalsHeaders)
    }

    private suspend fun updateRange(accessToken: String, spreadsheetId: String, range: String, rowData: JSONArray) {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range?valueInputOption=USER_ENTERED"
        val bodyJson = JSONObject().apply {
            put("values", JSONArray().apply {
                put(rowData)
            })
        }
        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SheetsAPI", "updateRange failed for range $range with code: ${response.code} - ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "updateRange exception", e)
        }
    }

    suspend fun getExpenses(accessToken: String, spreadsheetId: String): List<Expense> {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Expenses!A2:E"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val list = mutableListOf<Expense>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SheetsAPI", "getExpenses failed with code: ${response.code}")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val values = json.optJSONArray("values") ?: return emptyList()
                for (i in 0 until values.length()) {
                    val row = values.getJSONArray(i)
                    if (row.length() >= 5) {
                        val id = row.getString(0)
                        val date = row.getString(1)
                        val amount = row.optDouble(2, 0.0)
                        val desc = row.getString(3)
                        val cat = row.getString(4)
                        list.add(Expense(id, amount, desc, cat, date))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "getExpenses exception", e)
        }
        return list
    }

    suspend fun getGoals(accessToken: String, spreadsheetId: String): List<Goal> {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Goals!A2:G"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val list = mutableListOf<Goal>()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SheetsAPI", "getGoals failed with code: ${response.code}")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val values = json.optJSONArray("values") ?: return emptyList()
                for (i in 0 until values.length()) {
                    val row = values.getJSONArray(i)
                    if (row.length() >= 7) {
                        val id = row.getString(0)
                        val name = row.getString(1)
                        val targetAmount = row.optDouble(2, 0.0)
                        val currentAmount = row.optDouble(3, 0.0)
                        val targetDate = row.getString(4)
                        val cat = row.getString(5)
                        val status = row.getString(6)
                        list.add(Goal(id, name, targetAmount, currentAmount, targetDate, cat, status))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "getGoals exception", e)
        }
        return list
    }

    suspend fun addExpense(accessToken: String, spreadsheetId: String, expense: Expense): Boolean {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Expenses!A2:E:append?valueInputOption=USER_ENTERED"
        val rowData = JSONArray().apply {
            put(expense.id)
            put(expense.date)
            put(expense.amount)
            put(expense.description)
            put(expense.category)
        }
        val bodyJson = JSONObject().apply {
            put("values", JSONArray().apply {
                put(rowData)
            })
        }
        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "addExpense exception", e)
        }
        return false
    }

    suspend fun addGoal(accessToken: String, spreadsheetId: String, goal: Goal): Boolean {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Goals!A2:G:append?valueInputOption=USER_ENTERED"
        val rowData = JSONArray().apply {
            put(goal.id)
            put(goal.name)
            put(goal.targetAmount)
            put(goal.currentAmount)
            put(goal.targetDate)
            put(goal.category)
            put(goal.status)
        }
        val bodyJson = JSONObject().apply {
            put("values", JSONArray().apply {
                put(rowData)
            })
        }
        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "addGoal exception", e)
        }
        return false
    }

    suspend fun updateExpenseRow(accessToken: String, spreadsheetId: String, expense: Expense): Boolean {
        val rowNum = findRowNumberById(accessToken, spreadsheetId, "Expenses!A:A", expense.id) ?: return false
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Expenses!A$rowNum:E$rowNum?valueInputOption=USER_ENTERED"
        val rowData = JSONArray().apply {
            put(expense.id)
            put(expense.date)
            put(expense.amount)
            put(expense.description)
            put(expense.category)
        }
        val bodyJson = JSONObject().apply {
            put("values", JSONArray().apply {
                put(rowData)
            })
        }
        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "updateExpenseRow exception", e)
        }
        return false
    }

    suspend fun updateGoalRow(accessToken: String, spreadsheetId: String, goal: Goal): Boolean {
        val rowNum = findRowNumberById(accessToken, spreadsheetId, "Goals!A:A", goal.id) ?: return false
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Goals!A$rowNum:G$rowNum?valueInputOption=USER_ENTERED"
        val rowData = JSONArray().apply {
            put(goal.id)
            put(goal.name)
            put(goal.targetAmount)
            put(goal.currentAmount)
            put(goal.targetDate)
            put(goal.category)
            put(goal.status)
        }
        val bodyJson = JSONObject().apply {
            put("values", JSONArray().apply {
                put(rowData)
            })
        }
        val requestBody = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "updateGoalRow exception", e)
        }
        return false
    }

    suspend fun deleteExpenseRow(accessToken: String, spreadsheetId: String, expenseId: String): Boolean {
        val rowNum = findRowNumberById(accessToken, spreadsheetId, "Expenses!A:A", expenseId) ?: return false
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Expenses!A$rowNum:E$rowNum:clear"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "deleteExpenseRow exception", e)
        }
        return false
    }

    suspend fun deleteGoalRow(accessToken: String, spreadsheetId: String, goalId: String): Boolean {
        val rowNum = findRowNumberById(accessToken, spreadsheetId, "Goals!A:A", goalId) ?: return false
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Goals!A$rowNum:G$rowNum:clear"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "deleteGoalRow exception", e)
        }
        return false
    }

    private suspend fun findRowNumberById(accessToken: String, spreadsheetId: String, range: String, targetId: String): Int? {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val values = json.optJSONArray("values") ?: return null
                for (i in 0 until values.length()) {
                    val row = values.getJSONArray(i)
                    if (row.length() > 0 && row.getString(0) == targetId) {
                        return i + 1 // 1-based index
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SheetsAPI", "findRowNumberById exception", e)
        }
        return null
    }

    // Sync whole local data to Sheets by fully rewriting range
    suspend fun syncAllToSheets(
        accessToken: String, 
        spreadsheetId: String, 
        expenses: List<Expense>, 
        goals: List<Goal>
    ): Boolean {
        try {
            // Write Expenses
            val expensesRows = JSONArray().apply {
                for (exp in expenses) {
                    put(JSONArray().apply {
                        put(exp.id)
                        put(exp.date)
                        put(exp.amount)
                        put(exp.description)
                        put(exp.category)
                    })
                }
            }
            
            // Clear current sheet first then write
            clearRange(accessToken, spreadsheetId, "Expenses!A2:E1000")
            if (expenses.isNotEmpty()) {
                val urlExp = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Expenses!A2?valueInputOption=USER_ENTERED"
                val bodyExp = JSONObject().apply { put("values", expensesRows) }
                val requestExp = Request.Builder()
                    .url(urlExp)
                    .put(bodyExp.toString().toRequestBody(jsonMediaType))
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                client.newCall(requestExp).execute().close()
            }

            // Write Goals
            val goalsRows = JSONArray().apply {
                for (g in goals) {
                    put(JSONArray().apply {
                        put(g.id)
                        put(g.name)
                        put(g.targetAmount)
                        put(g.currentAmount)
                        put(g.targetDate)
                        put(g.category)
                        put(g.status)
                    })
                }
            }
            
            clearRange(accessToken, spreadsheetId, "Goals!A2:G1000")
            if (goals.isNotEmpty()) {
                val urlG = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/Goals!A2?valueInputOption=USER_ENTERED"
                val bodyG = JSONObject().apply { put("values", goalsRows) }
                val requestG = Request.Builder()
                    .url(urlG)
                    .put(bodyG.toString().toRequestBody(jsonMediaType))
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                client.newCall(requestG).execute().close()
            }

            return true
        } catch (e: Exception) {
            Log.e("SheetsAPI", "syncAllToSheets exception", e)
            return false
        }
    }

    private suspend fun clearRange(accessToken: String, spreadsheetId: String, range: String) {
        val url = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values/$range:clear"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $accessToken")
            .build()
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("SheetsAPI", "clearRange failed", e)
        }
    }
}
