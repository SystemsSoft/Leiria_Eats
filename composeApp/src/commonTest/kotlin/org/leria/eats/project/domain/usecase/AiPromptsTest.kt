package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product
import org.leria.eats.project.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class AiPromptsTest {

    @Test
    fun sugestoesSemPersonalizacao() {
        assertEquals("Pode me dar sugestões do que eu poderia pedir?", buildPersonalizedSuggestionsPrompt(UserProfile()))
    }

    @Test
    fun sugestoesComAlergiasEEstiloDeVida() {
        val prompt = buildPersonalizedSuggestionsPrompt(UserProfile(allergies = "glúten", lifestyles = "vegano"))
        assertEquals(
            "Pode me dar sugestões do que eu poderia pedir? Considere minha personalização alimentar " +
                "(alergias/intolerâncias: glúten; estilo de vida: vegano).",
            prompt
        )
    }

    @Test
    fun pedidoDeProdutoTemPeloMenosUmaUnidade() {
        val pizza = Product(name = "Pizza")
        assertEquals("Adicionar 2 Pizza ao meu carrinho.", buildAddToCartPrompt(pizza, 2))
        assertEquals("Adicionar 1 Pizza ao meu carrinho.", buildAddToCartPrompt(pizza, 0))
    }

    @Test
    fun textoParaVozLeValoresEmEuros() {
        assertEquals("Total: 12 euros e 50 cêntimos", prepareTextForSpeech("Total: 12,50 €"))
        assertEquals("Fica 8 euros", prepareTextForSpeech("Fica 8,00 €"))
    }

    @Test
    fun textoParaVozSemMarkdownEComQuantidades() {
        assertEquals("Pizza 2 unidades de", prepareTextForSpeech("**Pizza** x2"))
        assertEquals("uma Pizza", prepareTextForSpeech("x1 Pizza"))
    }
}
