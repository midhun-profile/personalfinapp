package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Tailwind-Mapped Brand Palette
val BrandPrimaryLight = Color(0xFF4F46E5) // Indigo
val BrandPrimaryDark = Color(0xFF6366F1)  // Indigo Accent
val BrandSecondary = Color(0xFF06B6D4)    // Cyan
val AccentWarning = Color(0xFFF59E0B)     // Amber
val SuccessEmerald = Color(0xFF10B981)    // Emerald
val DangerRose = Color(0xFFEF4444)        // Rose

val NeutralDarkSurface = Color(0xFF0F172A) // Slate Blue Deep
val NeutralDarkCard = Color(0xFF1E293B)    // Slate Card
val NeutralLightSurface = Color(0xFFF8FAFC) // Slate Tint Light
val NeutralLightBorder = Color(0xFFF1F5F9)  // Slate Border
val NeutralLightTextSec = Color(0xFF64748B) // Secondary Slate Grey

// Categorical Colors (Strictly Enforced Map)
val ColorFood = Color(0xFF6366F1)
val ColorTransport = Color(0xFF06B6D4)
val ColorUtilities = Color(0xFFF59E0B)
val ColorEntertainment = Color(0xFFEC4899)
val ColorShopping = Color(0xFF8B5CF6)
val ColorHealth = Color(0xFF10B981)
val ColorOther = Color(0xFF64748B)

// Utility function to get categorical color
fun getCategoryColor(category: String): Color {
    return when (category.lowercase().trim()) {
        "food" -> ColorFood
        "transport" -> ColorTransport
        "utilities" -> ColorUtilities
        "entertainment" -> ColorEntertainment
        "shopping" -> ColorShopping
        "health" -> ColorHealth
        else -> ColorOther
    }
}
