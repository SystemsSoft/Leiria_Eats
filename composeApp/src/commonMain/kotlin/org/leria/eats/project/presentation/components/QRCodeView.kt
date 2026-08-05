package org.leria.eats.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.theme.*
import org.leria.eats.project.qrcode.QRCodeGenerator
import kotlin.math.min

@Composable
fun QRCodeView(
    data: String,
    size: Int = 200,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KomaTextSec,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(2.dp, KomaGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            SimpleQRPattern(data = data)
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KomaGold
            )
        }
    }
}

@Composable
private fun SimpleQRPattern(data: String) {
    // Gerar QR code real usando o gerador multiplataforma
    val qrCodeData = remember(data) {
        try {
            val generator = QRCodeGenerator()
            generator.generate(data)
        } catch (_: Exception) {
            // Em caso de erro, retornar um grid vazio
            emptyArray<BooleanArray>()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (qrCodeData.isEmpty()) return@Canvas

        val gridSize = qrCodeData.size
        val cellSize = min(size.width, size.height) / gridSize
        val offsetX = (size.width - (cellSize * gridSize)) / 2
        val offsetY = (size.height - (cellSize * gridSize)) / 2

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                // qrCodeData[row][col] = true significa pixel preto
                if (row < qrCodeData.size && col < qrCodeData[row].size && qrCodeData[row][col]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(
                            offsetX + col * cellSize,
                            offsetY + row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

