package org.leria.eats.project.qrcode

/**
 * Interface multiplataforma para geração de QR codes
 * Retorna uma matriz booleana onde true = pixel preto, false = pixel branco
 */
expect class QRCodeGenerator() {
    /**
     * Gera um QR code a partir de uma string de dados
     * @param data String a ser codificada no QR code
     * @return Matriz bidimensional onde true = módulo preto
     */
    fun generate(data: String): Array<BooleanArray>
}

