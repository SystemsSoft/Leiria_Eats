package org.leria.eats.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import komaai.composeapp.generated.resources.Res
import komaai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

/**
 * Banner de boas-vindas da tela de IA (só aparece com o chat vazio; com conversa em andamento a
 * tela volta à [AiTopBar] compacta): logo, tagline e a arte do lado direito — balão de conversa,
 * brilhos e, quando informado, o mascote.
 *
 * O gradiente verde de fundo é desenhado por quem usa o banner; aqui entram só os círculos e o
 * brilho por cima dele. As medidas vêm do layout aprovado (em dp, com a arte medida a partir da
 * base da status bar).
 *
 * [mascot] pode passar do limite de baixo do banner: a top bar é desenhada por cima do corpo
 * branco da tela, então o mascote "senta" na borda do card.
 */
@Composable
fun AiHeroBanner(
    modifier: Modifier = Modifier,
    mascot: Painter? = null
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.matchParentSize()) { desenharFundo() }

        Column(modifier = Modifier.statusBarsPadding().padding(start = 7.dp)) {
            Spacer(modifier = Modifier.height(30.dp))
            // O PNG da logo tem margem transparente grande em cima e embaixo: a altura fixa de 44dp
            // com FillWidth + centro recorta só a faixa onde a marca de fato aparece.
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "KomaAI",
                modifier = Modifier.width(190.dp).height(44.dp),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SEU PEDIDO, DO SEU JEITO",
                modifier = Modifier.padding(start = 15.dp),
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.68f)
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        Box(modifier = Modifier.matchParentSize().statusBarsPadding()) {
            Canvas(modifier = Modifier.matchParentSize()) { desenharBalaoEBrilhos() }
            if (mascot != null) {
                Image(
                    painter = mascot,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = 3.dp)
                        .width(142.dp)
                        .requiredHeight(125.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopEnd
                )
            }
        }
    }
}

/** Círculos translúcidos e brilho lima atrás do mascote (origem no topo da tela). */
private fun DrawScope.desenharFundo() {
    val u = 1.dp.toPx()
    val w = size.width
    val h = size.height

    drawCircle(Color(0xFF2FA36B).copy(alpha = 0.16f), radius = 100 * u, center = Offset(w - 72 * u, 95 * u))
    drawCircle(Color(0xFF46B67C).copy(alpha = 0.22f), radius = 70 * u, center = Offset(w - 96 * u, h + 6 * u))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFB7E37D).copy(alpha = 0.70f), Color.Transparent),
            center = Offset(w - 40 * u, h - 6 * u),
            radius = 130 * u
        ),
        radius = 130 * u,
        center = Offset(w - 40 * u, h - 6 * u)
    )
    // onda clara no canto inferior esquerdo
    drawOval(
        color = Color(0xFF2B8A5A).copy(alpha = 0.30f),
        topLeft = Offset(-80 * u, h - 34 * u),
        size = Size(340 * u, 70 * u)
    )
}

/** Balão de conversa e brilhos dourados (origem na base da status bar, ancorados à direita). */
private fun DrawScope.desenharBalaoEBrilhos() {
    val u = 1.dp.toPx()
    val w = size.width

    val bx = w - 177 * u
    val by = 14 * u
    val bw = 51 * u
    val bh = 40 * u
    val balao = Path().apply {
        addRoundRect(RoundRect(bx, by, bx + bw, by + bh, CornerRadius(15 * u)))
        moveTo(bx + bw * 0.60f, by + bh - 1 * u)
        lineTo(bx + bw * 0.80f, by + bh + 9 * u)
        lineTo(bx + bw * 0.86f, by + bh - 1 * u)
        close()
    }
    drawPath(balao, Color(0xFFEAF7EF))
    repeat(3) { i ->
        drawCircle(Color(0xFF58AF84), radius = 2.8f * u, center = Offset(bx + bw * (0.28f + 0.22f * i), by + bh / 2))
    }

    val ouro = Color(0xFFFFDD55)
    brilho(w - 32 * u, 24 * u, 12 * u, ouro)
    brilho(w - 22 * u, 15 * u, 4.5f * u, ouro)
    brilho(w - 22 * u, 33 * u, 4.5f * u, ouro)
    brilho(w - 185 * u, 60 * u, 7 * u, ouro)
}

/** Estrela de quatro pontas. */
private fun DrawScope.brilho(cx: Float, cy: Float, r: Float, cor: Color) {
    val k = r * 0.16f
    val estrela = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx + k, cy - k, cx + r, cy)
        quadraticTo(cx + k, cy + k, cx, cy + r)
        quadraticTo(cx - k, cy + k, cx - r, cy)
        quadraticTo(cx - k, cy - k, cx, cy - r)
        close()
    }
    drawPath(estrela, cor)
}
