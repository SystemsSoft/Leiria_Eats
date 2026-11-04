package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.UserProfile

/** Pedido de sugestões à IA considerando a personalização alimentar salva no perfil. */
fun buildPersonalizedSuggestionsPrompt(profile: UserProfile): String {
    val preferences = buildList {
        if (profile.allergies.isNotBlank()) add("alergias/intolerâncias: ${profile.allergies}")
        if (profile.lifestyles.isNotBlank()) add("estilo de vida: ${profile.lifestyles}")
    }
    return if (preferences.isNotEmpty()) {
        "Pode me dar sugestões do que eu poderia pedir? Considere minha personalização alimentar (${preferences.joinToString("; ")})."
    } else {
        "Pode me dar sugestões do que eu poderia pedir?"
    }
}

/**
 * Pedido de um produto sugerido, como se o usuário tivesse digitado. A quantidade vem explícita
 * para a IA adicionar direto ao carrinho sem perguntar de novo — ver regra "Exceção" em
 * gemini_sales_service.py.
 */
fun buildAddToCartPrompt(product: Product, quantity: Int): String =
    "Adicionar ${quantity.coerceAtLeast(1)} ${product.name} ao meu carrinho."

/** Limpa o texto da IA para a síntese de voz: valores em euros por extenso, sem markdown/emojis. */
fun prepareTextForSpeech(text: String): String {
    val processed = text
        .replace(Regex("""(\d+)[.,]([1-9]\d|0[1-9])\s*€""")) { m -> "${m.groupValues[1]} euros e ${m.groupValues[2]} cêntimos" }
        .replace(Regex("""(\d+)(?:[.,]00)?\s*€""")) { m -> "${m.groupValues[1]} euros" }
        .replace(Regex("""\b(\d+)[.,]00\b""")) { m -> "${m.groupValues[1]} euros" }

    return processed
        .replace("*", "")
        .replace("_", "")
        .replace("#", "")
        .replace("(", " ")
        .replace(")", " ")
        .replace(Regex("[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}]"), "")
        .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "")
        .replace(Regex("[☀-⟿]"), "")
        .replace(Regex("[︀-️]"), "")
        .replace(Regex("\\bx1\\b", RegexOption.IGNORE_CASE), "uma")
        .replace(Regex("\\bx(\\d+)\\b", RegexOption.IGNORE_CASE), "$1 unidades de")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
}
