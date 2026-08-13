package org.leria.eats.project.data

import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Diretório base para armazenar imagens
 */
expect fun getProfileImageDirectory(): String

/**
 * Salva um arquivo de imagem selecionado e retorna o caminho completo
 * @param platformFile Arquivo selecionado via FileKit
 * @return Caminho completo do arquivo salvo ou vazio se falhar
 */
suspend fun saveImageLocally(platformFile: PlatformFile, fileName: String = ""): String {
    return withContext(Dispatchers.Default) {
        try {
            // Ler os bytes do arquivo
            val bytes = platformFile.readBytes()
            if (bytes.isEmpty()) return@withContext ""
            
            val imageDir = getProfileImageDirectory()
            val uniqueFileName = if (fileName.isEmpty()) {
                "profile_${Random.nextLong(100000, 999999)}.jpg"
            } else {
                fileName
            }
            
            saveBytesToFile(imageDir, uniqueFileName, bytes)
        } catch (e: Exception) {
            println("Erro ao salvar imagem: ${e.message}")
            ""
        }
    }
}

/**
 * Função expect para salvar bytes em arquivo (implementada por plataforma)
 */
expect fun saveBytesToFile(directory: String, fileName: String, bytes: ByteArray): String

/**
 * Valida se o arquivo existe no caminho especificado
 * @param imagePath Caminho da imagem a validar
 * @return true se o arquivo existe, false caso contrário
 */
expect fun doesImageExist(imagePath: String): Boolean

/**
 * Deleta a imagem de perfil
 * @param imagePath Caminho da imagem a deletar
 */
expect fun deleteImage(imagePath: String)

/**
 * Converte um caminho local em um objeto que o Kamel consegue carregar.
 * No Android retorna java.io.File, no iOS retorna a string com prefixo file://
 */
expect fun mapPathToImageSource(path: String): Any

