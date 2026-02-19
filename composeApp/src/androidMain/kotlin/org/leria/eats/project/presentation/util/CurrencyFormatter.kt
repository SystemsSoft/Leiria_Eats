package org.leria.eats.project.presentation.util

import java.text.NumberFormat
import java.util.Locale

actual fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
