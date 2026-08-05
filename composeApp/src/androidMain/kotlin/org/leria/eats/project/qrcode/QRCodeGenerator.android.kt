package org.leria.eats.project.qrcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

actual class QRCodeGenerator {
    actual fun generate(data: String): Array<BooleanArray> {
        return try {
            val writer = QRCodeWriter()
            val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 0

            // Gerar QR code com ZXing
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 0, 0, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height

            // Converter BitMatrix para Array<BooleanArray>
            Array(height) { row ->
                BooleanArray(width) { col ->
                    bitMatrix[col, row]
                }
            }
        } catch (_: Exception) {
            // Em caso de erro, retornar array vazio
            emptyArray()
        }
    }
}

