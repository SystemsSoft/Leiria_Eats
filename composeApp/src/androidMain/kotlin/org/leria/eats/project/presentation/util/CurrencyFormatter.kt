package org.leria.eats.project.presentation.util

import java.text.NumberFormat
import java.util.Locale

actual fun formatCurrency(value: Double): String {
    // Use Portuguese (Portugal) locale so the currency symbol becomes Euro ("€")
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-PT")).format(value)
}
