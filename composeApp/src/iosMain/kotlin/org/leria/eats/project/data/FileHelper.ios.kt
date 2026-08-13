package org.leria.eats.project.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual fun getProfileImageDirectory(): String {
    return try {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsPath = paths.firstOrNull() as? String ?: return ""
        val profileDir = "$documentsPath/profile_images"
        
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(profileDir)) {
            fileManager.createDirectoryAtPath(profileDir, withIntermediateDirectories = true, attributes = null, error = null)
        }
        profileDir
    } catch (e: Exception) {
        println("Erro ao obter diretório iOS: ${e.message}")
        ""
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun saveBytesToFile(directory: String, fileName: String, bytes: ByteArray): String {
    return try {
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(directory)) {
            fileManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
        }
        
        val filePath = "$directory/$fileName"
        
        val nsData = if (bytes.isEmpty()) {
            NSData.data()
        } else {
            bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
        }
        
        if (nsData.writeToFile(filePath, true)) {
            filePath
        } else {
            ""
        }
    } catch (e: Exception) {
        println("Erro ao salvar arquivo iOS: ${e.message}")
        ""
    }
}

actual fun doesImageExist(imagePath: String): Boolean {
    return if (imagePath.isEmpty()) false else NSFileManager.defaultManager.fileExistsAtPath(imagePath)
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteImage(imagePath: String) {
    try {
        if (imagePath.isNotEmpty()) {
            NSFileManager.defaultManager.removeItemAtPath(imagePath, error = null)
        }
    } catch (e: Exception) {
        println("Erro ao deletar imagem iOS: ${e.message}")
    }
}

actual fun mapPathToImageSource(path: String): Any {
    val cleanPath = path.removePrefix("file://")
    return "file://$cleanPath"
}
