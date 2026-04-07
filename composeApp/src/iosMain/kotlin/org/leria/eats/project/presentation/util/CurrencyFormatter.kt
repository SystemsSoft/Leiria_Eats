package org.leria.eats.project.presentation.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSLocale

actual fun formatCurrency(value: Double): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterCurrencyStyle
    formatter.locale = NSLocale(localeIdentifier = "pt_PT")
    return formatter.stringFromNumber(NSNumber(double = value)) ?: "€${value}"
}

