package org.leria.eats.project.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

// Variável global para armazenar o contexto (será inicializada no Application)
private var appContext: Context? = null

fun setApplicationContext(context: Context) {
    appContext = context
}

actual fun getProfileImageDirectory(): String {
    return try {
        val context = appContext ?: throw IllegalStateException("ApplicationContext não inicializado")
        val profileDir = File(context.filesDir, "profile_images")
        if (!profileDir.exists()) {
            profileDir.mkdirs()
        }
        profileDir.absolutePath
    } catch (e: Exception) {
        println("Erro ao obter diretório de imagens: ${e.message}")
        ""
    }
}

actual fun saveBytesToFile(directory: String, fileName: String, bytes: ByteArray): String {
    return try {
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)

        // Decodificar apenas as dimensões primeiro (eficiente em memória)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        // Redimensionar para no máximo 1024px (suficiente para avatar)
        val reqWidth = 1024
        val reqHeight = 1024
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        // Decodificar a imagem real com o sampleSize calculado
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        
        if (bitmap != null) {
            FileOutputStream(file).use { out ->
                // Comprimir para JPEG com 85% de qualidade para reduzir o tamanho em disco
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            file.absolutePath
        } else {
            // Se falhar em decodificar como bitmap, salva os bytes originais (fallback)
            file.writeBytes(bytes)
            file.absolutePath
        }
    } catch (e: Exception) {
        println("Erro ao salvar arquivo: ${e.message}")
        ""
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

actual fun doesImageExist(imagePath: String): Boolean {
    return if (imagePath.isEmpty()) false else File(imagePath).exists()
}

actual fun deleteImage(imagePath: String) {
    try {
        if (imagePath.isNotEmpty()) {
            File(imagePath).delete()
        }
    } catch (e: Exception) {
        println("Erro ao deletar imagem: ${e.message}")
    }
}

actual fun mapPathToImageSource(path: String): Any {
    val cleanPath = path.removePrefix("file://")
    return File(cleanPath)
}
