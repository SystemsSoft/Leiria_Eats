package org.leria.eats.project.qrcode
import kotlinx.cinterop.*
import platform.CoreImage.CIFilter
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.Foundation.NSData
import platform.Foundation.create
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGImageRef
import platform.CoreImage.createCGImage
import platform.CoreImage.filterWithName
import platform.Foundation.setValue
@OptIn(ExperimentalForeignApi::class)
actual class QRCodeGenerator {
    actual fun generate(data: String): Array<BooleanArray> {
        return try {
            val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return emptyArray()
            val dataBytes = data.encodeToByteArray()
            val nsData = dataBytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = dataBytes.size.toULong())
            }
            filter.setValue(nsData, forKey = "inputMessage")
            filter.setValue("M", forKey = "inputCorrectionLevel")
            val outputImage = filter.outputImage ?: return emptyArray()
            val extent = outputImage.extent
            val qrSize = extent.useContents { size.width.toInt() }
            if (qrSize <= 0) return emptyArray()
            val context = CIContext.contextWithOptions(null)
            val cgImage = context.createCGImage(outputImage, fromRect = extent) 
                ?: return emptyArray()
            convertCGImageToMatrix(cgImage, qrSize)
        } catch (_: Exception) {
            emptyArray()
        }
    }
    @OptIn(ExperimentalForeignApi::class)
    private fun convertCGImageToMatrix(cgImage: CGImageRef, size: Int): Array<BooleanArray> {
        return try {
            memScoped {
                val colorSpace = CGColorSpaceCreateDeviceGray()
                val bytesPerRow = size
                val bitmapData = allocArray<ByteVar>(size * bytesPerRow)
                val bitmapContext = CGBitmapContextCreate(
                    data = bitmapData,
                    width = size.toULong(),
                    height = size.toULong(),
                    bitsPerComponent = 8u,
                    bytesPerRow = bytesPerRow.toULong(),
                    space = colorSpace,
                    bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNone.value
                )
                if (bitmapContext != null) {
                    val rect = cValue<CGRect> {
                        origin.x = 0.0
                        origin.y = 0.0
                        this.size.width = size.toDouble()
                        this.size.height = size.toDouble()
                    }
                    CGContextDrawImage(bitmapContext, rect, cgImage)
                    Array(size) { row ->
                        BooleanArray(size) { col ->
                            val index = row * bytesPerRow + col
                            val pixel = bitmapData[index].toInt() and 0xFF
                            pixel < 128
                        }
                    }
                } else {
                    emptyArray()
                }
            }
        } catch (_: Exception) {
            emptyArray()
        }
    }
}
