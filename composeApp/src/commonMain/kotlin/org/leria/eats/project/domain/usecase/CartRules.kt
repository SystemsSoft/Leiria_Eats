package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product

/** Regras puras da sacola (sem estado): quem guarda a sacola é a apresentação. */
object CartRules {

    /**
     * Exclusividade da Caixa Surpresa — mesma regra aplicada no backend
     * (HybridAIService._bloqueado_por_caixa_surpresa_exclusiva). Só bloqueia um produto NOVO
     * (aumentar a quantidade do próprio item já no carrinho continua permitido). O gate que não
     * pode ser contornado é o do checkout no servidor; esta checagem é só UX.
     */
    fun violatesSurpriseBoxExclusivity(cart: List<Product>, product: Product): Boolean {
        val existing = cart.find { it.gid == product.gid }
        if (existing != null || cart.isEmpty()) return false
        return cart.any { it.isSurpriseBox } || product.isSurpriseBox
    }

    /** Soma a quantidade se o produto (mesmo gid) já está na sacola; senão acrescenta. */
    fun addProduct(cart: List<Product>, product: Product): List<Product> {
        val existing = cart.find { it.gid == product.gid }
        return if (existing != null) {
            cart.map { if (it.gid == product.gid) it.copy(quantity = it.quantity + product.quantity) else it }
        } else {
            cart + product
        }
    }

    /** Tira uma unidade do produto; itens que chegam a zero saem da sacola. */
    fun removeOneUnit(cart: List<Product>, product: Product): List<Product> =
        cart.map { if (it.gid == product.gid) it.copy(quantity = it.quantity - 1) else it }
            .filter { it.quantity > 0 }

    /**
     * Sacola enviada pela IA (estado ATUAL, substitui a local): descarta itens com quantidade 0.
     *
     * Defesa em profundidade: o backend já garante que a sacola nunca mistura um item de Caixa
     * Surpresa com outro produto — se acontecer, só avisa no log (não filtra nada sozinho, para não
     * mascarar um estado que precisa ser investigado no servidor).
     */
    fun sanitizeAiCart(products: List<Product>): List<Product> {
        val filteredProducts = products.filter { it.quantity > 0 }
        if (filteredProducts.size > 1 && filteredProducts.any { it.isSurpriseBox }) {
            println("⚠️ [Cart] Sacola da IA veio com item de Caixa Surpresa + outro(s) produto(s) — " +
                "esperado apenas 1 item exclusivo. products=${filteredProducts.map { it.name }}")
        }
        return filteredProducts
    }
}
