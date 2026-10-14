package com.example.dripguardai.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DryingRequest(
    val cityName: String,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val rainProbabilityPercent: Int,
    val fabrics: List<String>,
    val hangingLocation: String
)

data class DryingRecommendation(
    val rainRisk: String,
    val dryingDurationHours: Double,
    val dryingIndex: Int = 75,
    val dryingIndexLabel: String = "Good",
    val optimalDryingWindow: String,
    val actionableTips: List<String>,
    val summaryText: String,
    val quickAiAlert: String = "Weather conditions are suitable for drying outdoors."
)

class GeminiLaundryService(
    private val apiKey: String
) {
    private val gson = Gson()

    suspend fun getLaundryRecommendation(request: DryingRequest): DryingRecommendation {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || apiKey == "your_gemini_api_key_here") {
                return@withContext calculateFallbackRecommendation(request)
            }

            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )

                val prompt = buildString {
                    appendLine("Act as an expert physics-based laundry drying and meteorology AI assistant.")
                    appendLine("Analyze the following weather metrics and user laundry setup:")
                    appendLine("- City: ${request.cityName}")
                    appendLine("- Temperature: ${request.temperature}°C")
                    appendLine("- Relative Humidity: ${request.humidity}%")
                    appendLine("- Wind Speed: ${request.windSpeed} m/s")
                    appendLine("- Max Rain Probability (next 12h): ${request.rainProbabilityPercent}%")
                    appendLine("- Selected Fabrics: ${request.fabrics.joinToString(", ")}")
                    appendLine("- Hanging Location: ${request.hangingLocation}")
                    appendLine()
                    appendLine("CRITICAL INSTRUCTION FOR HANGING LOCATION:")
                    appendLine("If Hanging Location is 'Indoor' (e.g. 'Indoor Well-Ventilated', 'Indoor Enclosed'):")
                    appendLine("- Rain Risk MUST be 'LOW' because indoor garments are fully sheltered from rain.")
                    appendLine("- Drying duration MUST be noticeably LONGER (add +1.5 to +3.0 hours) due to lack of direct sunlight and lower air movement.")
                    appendLine("- quickAiAlert MUST explicitly highlight indoor protection from rain and estimated indoor drying time.")
                    appendLine("If Hanging Location is 'Outdoor' (e.g. 'Outdoor Direct Sun', 'Outdoor Covered'):")
                    appendLine("- Rain Risk MUST accurately reflect the outdoor rain probability (${request.rainProbabilityPercent}%).")
                    appendLine("- Drying duration MUST be SHORTER due to outdoor solar radiation and wind speed.")
                    appendLine()
                    appendLine("Respond STRICTLY with a raw JSON object matching this schema without markdown code blocks:")
                    appendLine("{")
                    appendLine("  \"rainRisk\": \"LOW\" | \"MODERATE\" | \"HIGH\",")
                    appendLine("  \"dryingDurationHours\": <number, e.g. 3.5>,")
                    appendLine("  \"dryingIndex\": <number from 0 to 100>,")
                    appendLine("  \"dryingIndexLabel\": \"Excellent\" | \"Good\" | \"Fair\" | \"Poor\",")
                    appendLine("  \"optimalDryingWindow\": \"<string, e.g. 'Next 3-4 hours (10:00 AM - 2:00 PM)'>\",")
                    appendLine("  \"actionableTips\": [\"<tip 1>\", \"<tip 2>\"],")
                    appendLine("  \"summaryText\": \"<short narrative summary>\",")
                    appendLine("  \"quickAiAlert\": \"<short 1-sentence urgent alert or recommendation summary>\"")
                    appendLine("}")
                }

                val response = generativeModel.generateContent(prompt)
                val rawText = response.text ?: ""
                parseResponseJson(rawText) ?: calculateFallbackRecommendation(request)
            } catch (e: Exception) {
                e.printStackTrace()
                calculateFallbackRecommendation(request)
            }
        }
    }

    private fun parseResponseJson(rawResponse: String): DryingRecommendation? {
        return try {
            val cleanedJson = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()
            gson.fromJson(cleanedJson, DryingRecommendation::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun calculateFallbackRecommendation(request: DryingRequest): DryingRecommendation {
        val isIndoor = request.hangingLocation.contains("Indoor", ignoreCase = true)

        val rainRisk = if (isIndoor) {
            "LOW" // Fully sheltered indoors
        } else {
            when {
                request.rainProbabilityPercent >= 60 -> "HIGH"
                request.rainProbabilityPercent >= 30 -> "MODERATE"
                else -> "LOW"
            }
        }

        var baseHours = 3.5
        if (request.humidity > 70) baseHours += 1.5 else if (request.humidity < 40) baseHours -= 0.8
        if (request.temperature > 28) baseHours -= 0.8 else if (request.temperature < 15) baseHours += 1.5
        if (request.windSpeed > 4.0) baseHours -= 0.5

        if (isIndoor) {
            baseHours += 2.5 // Indoor drying takes significantly longer
        } else {
            baseHours -= 0.5 // Outdoor direct sun / breeze speeds drying up
        }

        if (request.fabrics.contains("Denim/Jeans") || request.fabrics.contains("Towels")) baseHours += 1.2

        val finalHours = baseHours.coerceAtLeast(1.5)

        var index = 80
        if (!isIndoor) {
            index -= (request.rainProbabilityPercent * 0.5).toInt()
        }
        index -= ((request.humidity - 50).coerceAtLeast(0) * 0.5).toInt()
        index += ((request.temperature - 20) * 1.2).toInt()
        if (!isIndoor) {
            index += (request.windSpeed * 2.5).toInt()
        }
        val dryingIndex = index.coerceIn(10, 98)

        val dryingLabel = when {
            dryingIndex >= 75 -> "Excellent"
            dryingIndex >= 55 -> "Good"
            dryingIndex >= 35 -> "Fair"
            else -> "Poor"
        }

        val tips = mutableListOf<String>()
        if (isIndoor) {
            tips.add("Indoor Drying: Use a standing rack near a window or fan to improve airflow.")
            if (request.humidity > 60) {
                tips.add("High indoor humidity (${request.humidity}%): Space garments widely to prevent damp odor.")
            }
        } else {
            if (rainRisk == "HIGH") {
                tips.add("⚠️ High outdoor rain chance (${request.rainProbabilityPercent}%). Move clothes indoors or under shelter.")
            } else {
                tips.add("Outdoor Sun: Secure garments with clips for good breeze airflow.")
            }
        }

        val locationText = if (isIndoor) "indoors" else "outdoors"
        val summary = "With ambient temperature at ${request.temperature}°C, humidity at ${request.humidity}%, and setup set to $locationText, garments will take approximately ${"%.1f".format(finalHours)} hours to dry."

        val alertText = if (isIndoor) {
            "🏠 Indoor Setup Active: Clothes are safe from outdoor rain (${request.rainProbabilityPercent}% chance). Est. drying time: ${"%.1f".format(finalHours)}h."
        } else if (rainRisk == "HIGH") {
            "⚠️ Outdoor Setup Warning: High Rain Chance (${request.rainProbabilityPercent}%). Switch setup to Indoor or stay watchful!"
        } else if (rainRisk == "MODERATE") {
            "🌤️ Outdoor Setup: Moderate Rain Risk (${request.rainProbabilityPercent}%). Est. drying time: ${"%.1f".format(finalHours)}h."
        } else {
            "☀️ Outdoor Setup Active: Ideal solar & wind drying conditions! Est. drying time: ${"%.1f".format(finalHours)}h."
        }

        return DryingRecommendation(
            rainRisk = rainRisk,
            dryingDurationHours = finalHours,
            dryingIndex = dryingIndex,
            dryingIndexLabel = dryingLabel,
            optimalDryingWindow = if (isIndoor) "Anytime indoors with ventilation" else if (rainRisk == "HIGH") "Indoor drying recommended today" else "Next ${finalHours.toInt()}-${finalHours.toInt() + 1} hours",
            actionableTips = tips,
            summaryText = summary,
            quickAiAlert = alertText
        )
    }
}
