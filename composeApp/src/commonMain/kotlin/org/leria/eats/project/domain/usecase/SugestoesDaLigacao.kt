package org.leria.eats.project.domain.usecase

import org.leria.eats.project.domain.model.Product

/**
 * Decide quais cards de sugestão ficam na tela da ligação quando o servidor manda uma nova lista.
 *
 * Os cards são a "vitrine" da conversa: ao escolher um deles (toque no +) a IA passa a falar só do
 * produto escolhido, e a lista nova viria com ele sozinho — a vitrine encolheria. Então, se todos os
 * produtos da lista nova já estão na vitrine atual, ela é mantida como está. Só é substituída
 * quando o cliente pede algo que não estava nela (lista nova com produto desconhecido).
 */
fun mesclarSugestoesDaLigacao(atuais: List<Product>, novas: List<Product>): List<Product> {
    if (atuais.isEmpty() || novas.isEmpty()) return novas.ifEmpty { atuais }
    val idsAtuais = atuais.map { it.gid }.toSet()
    return if (novas.all { it.gid in idsAtuais }) atuais else novas
}
