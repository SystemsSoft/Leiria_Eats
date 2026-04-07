package org.leria.eats.project.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val AiDeepBg    = KomaBg
private val AiSurface   = KomaSurface
private val AiCard      = KomaCard
private val AiPrimary   = KomaGold
private val AiSecondary = KomaBrandGreen
private val AiAccent    = KomaGoldDark
private val AiText      = KomaTextPrimary
private val AiTextMuted = KomaTextSec
private val AiBotBubble = KomaMintLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSearchScreen(
    uiState: SearchUiState,
    isListening: Boolean,
    permissionStatus: PermissionStatus,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onAddToCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchTypeSelected: (showRestaurants: Boolean) -> Unit,
    onDismissSearchTypeSheet: () -> Unit,
) {
    // Pulsing glow animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.15f, targetValue = 0.55f, label = "glowAlpha",
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    val hasResults = uiState.restaurantResults.isNotEmpty() || uiState.productResults.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiDeepBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── HERO HEADER ─────────────────────────────────────────────────────
            AiHeroHeader(
                uiState = uiState,
                glowAlpha = glowAlpha,
                hasResults = hasResults,
                onTextChange = onTextChange,
                onSendClick = onSendClick
            )

            // ── CONTEÚDO CENTRAL ────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> AiSemanticThinkingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    hasResults -> AiSemanticResultsBody(
                        uiState = uiState,
                        onRestaurantClick = onRestaurantClick,
                        onAddToCart = onAddToCart,
                        onViewCart = onViewCart,
                        onClearSearch = onClearSearch,
                        onTextChange = onTextChange,
                        onSendClick = onSendClick
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            AiHowItWorksCard(modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            AiFavoritesCard(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // ── INPUT BAR ───────────────────────────────────────────────────────
            AiSemanticInputBar(
                value = uiState.textInput,
                isListening = isListening,
                isLoading = uiState.isLoading,
                onValueChange = onTextChange,
                onSend = onSendClick,
                onMic = onMicClick
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = AiAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp, top = 2.dp)
                )
            }
        }
    }

    // ── BOTTOM SHEET: escolha entre restaurantes ou produtos ───────────────────
    if (uiState.showSearchTypeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissSearchTypeSheet,
            sheetState = sheetState,
            containerColor = AiSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiSurface)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AiTextMuted.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Brush.radialGradient(listOf(AiPrimary.copy(alpha = 0.3f), Color.Transparent)),
                            CircleShape
                        )
                        .border(1.dp, AiPrimary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "O que está à procura?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AiText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A IA encontrou restaurantes e produtos relacionados com a sua pesquisa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AiTextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onSearchTypeSelected(true) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(AiPrimary, KomaOrangeEnd)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍽️  Restaurantes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                    Button(
                        onClick = { onSearchTypeSelected(false) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(AiSecondary, KomaGreenDark)),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛒  Produtos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

// ─── Hero Header ──────────────────────────────────────────────────────────────
@Composable
private fun AiHeroHeader(
    uiState: SearchUiState,
    glowAlpha: Float,
    hasResults: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val displayedReply = remember { mutableStateOf("") }
    LaunchedEffect(uiState.aiReply) {
        displayedReply.value = ""
        for (i in uiState.aiReply.indices) {
            displayedReply.value = uiState.aiReply.substring(0, i + 1)
            delay(18)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AiSurface)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Ícone + título IA ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ícone pulsante
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AiPrimary.copy(alpha = glowAlpha * 0.7f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, AiPrimary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AiPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Realizar pedido",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = AiText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge "IA"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(listOf(AiPrimary, KomaGoldAccent))
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "IA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KomaGoldOnDark
                        )
                    }
                }
                Text(
                    text = "Diga livremente o que deseja comer",
                    fontSize = 11.sp,
                    color = AiTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Bubble de resposta da IA ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Brush.horizontalGradient(listOf(AiBotBubble, AiCard)))
                .border(
                    1.dp,
                    AiPrimary.copy(alpha = 0.2f),
                    RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = displayedReply.value,
                style = MaterialTheme.typography.bodyMedium,
                color = AiText
            )
        }


    }
}

// ─── Card "Como funciona" ─────────────────────────────────────────────────────
@Composable
private fun AiHowItWorksCard(modifier: Modifier = Modifier) { var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AiCard)
            .border(1.dp, AiPrimary.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }
            .padding(20.dp)
    ) {
        // ── Cabeçalho sempre visível ──────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AiPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Como funciona a IA semântica?",
                fontWeight = FontWeight.Bold,
                color = AiPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            // Seta indicadora de expandir/recolher
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = AiPrimary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        // Dica de toque apenas quando recolhido
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Text(
                text = "Toque para saber mais",
                fontSize = 10.sp,
                color = AiTextMuted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 24.dp)
            )
        }

        // ── Conteúdo expansível ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(300)) + expandVertically(tween(350, easing = EaseOutQuart)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(250, easing = EaseInQuart))
        ) {
            Column {
                Spacer(modifier = Modifier.height(14.dp))

                val steps = listOf(
                    Triple("🗣️", "Fale ou escreva livremente", "Não precisa de palavras exatas — use linguagem natural"),
                    Triple("🧠", "A IA analisa o significado", "Compreende contexto, intenção e preferências"),
                    Triple("🔍", "Compara com todo o catálogo", "Avalia todos os restaurantes e produtos disponíveis"),
                    Triple("✅", "Devolve os mais relevantes", "Ordenados por similaridade semântica com o seu pedido")
                )

                steps.forEachIndexed { index, (emoji, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.radialGradient(listOf(AiPrimary.copy(alpha = 0.2f), Color.Transparent)),
                                    CircleShape
                                )
                                .border(1.dp, AiPrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AiText)
                            Text(desc, fontSize = 11.sp, color = AiTextMuted, lineHeight = 15.sp)
                        }
                    }
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .padding(start = 15.dp)
                                .width(2.dp)
                                .height(8.dp)
                                .background(AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Diferenciação vs pesquisa normal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AiSecondary.copy(alpha = 0.08f))
                        .border(1.dp, AiSecondary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            "💡 Diferente de uma pesquisa normal",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = AiSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("❌ Pesquisa normal", fontSize = 10.sp, color = AiTextMuted, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("Precisa de escrever\nexatamente o nome", fontSize = 10.sp, color = AiTextMuted, lineHeight = 14.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(AiTextMuted.copy(alpha = 0.2f)).align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("✅ Pesquisa IA", fontSize = 10.sp, color = AiSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("Entende o que\ndeseja comer", fontSize = 10.sp, color = AiText, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Card "Favoritos com IA" ──────────────────────────────────────────────────
@Composable
private fun AiFavoritesCard(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "favArrowRotation"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }
            .padding(20.dp)
    ) {
        // ── Cabeçalho sempre visível ──────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        Brush.radialGradient(listOf(AiSecondary.copy(alpha = 0.25f), Color.Transparent)),
                        CircleShape
                    )
                    .border(1.dp, AiSecondary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Favoritos — repita o seu pedido com IA",
                fontWeight = FontWeight.Bold,
                color = AiSecondary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Recolher" else "Expandir",
                tint = AiSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        // Dica de toque apenas quando recolhido
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Text(
                text = "Toque para saber mais",
                fontSize = 10.sp,
                color = AiTextMuted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 38.dp)
            )
        }

        // ── Conteúdo expansível ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(300)) + expandVertically(tween(350, easing = EaseOutQuart)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(250, easing = EaseInQuart))
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                val steps = listOf(
                    Triple("⭐", "Favorite ao finalizar o pedido",
                        "Após concluir um pedido, toque na estrela ⭐ para o guardar nos seus favoritos."),
                    Triple("✏️", "Dê um apelido ao pedido",
                        "Em Favoritos, toque no lápis ✏️ para editar um apelido personalizado para esse pedido e toque em Guardar."),
                    Triple("🤖", "Repita com a IA quando quiser",
                        "Abra a IA e diga \"Pedir\" seguido do apelido. A IA irá repetir o pedido verificando primeiro a disponibilidade do restaurante e dos produtos.")
                )

                steps.forEachIndexed { index, (emoji, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.radialGradient(listOf(AiSecondary.copy(alpha = 0.2f), Color.Transparent)),
                                    CircleShape
                                )
                                .border(1.dp, AiSecondary.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AiText)
                            Text(desc, fontSize = 11.sp, color = AiTextMuted, lineHeight = 15.sp)
                        }
                    }
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .padding(start = 15.dp)
                                .width(2.dp)
                                .height(8.dp)
                                .background(AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Exemplo prático
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AiPrimary.copy(alpha = 0.07f))
                        .border(1.dp, AiPrimary.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Exemplo prático",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = AiPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Passo 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(AiPrimary.copy(alpha = 0.18f), CircleShape)
                                    .border(1.dp, AiPrimary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("1", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AiPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Dá o apelido  \"Lanche de sábado\"  ao pedido favorito",
                                fontSize = 11.sp,
                                color = AiTextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Passo 2
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(AiPrimary.copy(alpha = 0.18f), CircleShape)
                                    .border(1.dp, AiPrimary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AiPrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Abre a IA e diz:",
                                fontSize = 11.sp,
                                color = AiTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Frase de exemplo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiBotBubble)
                                .border(1.dp, AiSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "\"Pedir lanche de sábado\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AiSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A IA verifica a disponibilidade do restaurante e dos produtos e repete o pedido automaticamente. ✅",
                            fontSize = 10.sp,
                            color = AiTextMuted.copy(alpha = 0.8f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Thinking indicator (semântico) ──────────────────────────────────────────
@Composable
private fun AiSemanticThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AiBotBubble)
                .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(14.dp))
                    Text("A IA está a analisar", fontSize = 13.sp, color = AiTextMuted)
                    (0..2).forEach { index ->
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f, targetValue = 1f, label = "dot$index",
                            animationSpec = infiniteRepeatable(
                                tween(600, delayMillis = index * 180),
                                RepeatMode.Reverse
                            )
                        )
                        Box(modifier = Modifier.size(7.dp).background(AiPrimary.copy(alpha = alpha), CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "A comparar semanticamente com todo o catálogo...",
                    fontSize = 11.sp,
                    color = AiTextMuted.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Results body (semântico) ─────────────────────────────────────────────────
@Composable
private fun AiSemanticResultsBody(
    uiState: SearchUiState,
    onRestaurantClick: (Restaurant) -> Unit,
    onAddToCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AiSurface)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp)
        ) {
            // Header row com badge IA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resultados semânticos", color = AiTextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AiPrimary.copy(alpha = 0.15f))
                                .border(1.dp, AiPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("IA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AiPrimary)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Ver todos",
                            color = AiSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                if (!uiState.isLoading) { onTextChange("ver todos"); onSendClick() }
                            }
                        )
                        Text(
                            "Limpar",
                            color = AiTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onClearSearch() }
                        )
                    }
                }
            }

            // Query semântica usada
            if (uiState.lastSearchQuery.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AiPrimary.copy(alpha = 0.07f))
                            .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🧠 \"${uiState.lastSearchQuery}\"",
                            fontSize = 11.sp,
                            color = AiTextMuted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Restaurantes
            if (uiState.restaurantResults.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(16.dp).background(AiPrimary, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurantes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AiText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${uiState.restaurantResults.size})", fontSize = 11.sp, color = AiTextMuted)
                    }
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(max = 1000.dp)
                    ) {
                        items(uiState.restaurantResults) { restaurant ->
                            RestaurantGridItem(restaurant = restaurant, onClick = { onRestaurantClick(restaurant) })
                        }
                    }
                }
            }

            // Produtos
            if (uiState.productResults.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(16.dp).background(AiSecondary, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Produtos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AiText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(${uiState.productResults.size})", fontSize = 11.sp, color = AiTextMuted)
                    }
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(max = 1000.dp)
                    ) {
                        items(uiState.productResults) { product ->
                            ProductGridItem(product = product, onAddToCart = {
                                onAddToCart(product)
                                onViewCart()
                            })
                        }
                    }
                }
            }
        }
    }
}

// ─── Input Bar semântica ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSemanticInputBar(
    value: String,
    isListening: Boolean,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    val borderAlpha by rememberInfiniteTransition(label = "border").animateFloat(
        initialValue = 0.3f, targetValue = 0.9f, label = "borderAlpha",
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp, top = 4.dp)
    ) {
        // Label contextual acima do input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (isListening) AiAccent else AiSecondary,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isListening) "🎤 A ouvir em linguagem natural..." else "Descreva o que deseja — a IA compreende",
                fontSize = 10.sp,
                color = if (isListening) AiAccent else AiTextMuted
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(AiCard)
                .then(
                    if (isListening)
                        Modifier.border(1.5.dp, AiAccent.copy(alpha = borderAlpha), RoundedCornerShape(28.dp))
                    else
                        Modifier.border(
                            1.5.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    AiPrimary.copy(alpha = borderAlpha * 0.6f),
                                    AiSecondary.copy(alpha = borderAlpha * 0.4f)
                                )
                            ),
                            RoundedCornerShape(28.dp)
                        )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AutoAwesome icon à esquerda
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = AiPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp).padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            // Text field
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Ex: \"Uma pizza de calabresa e alguma bebida\"...",
                        color = AiTextMuted,
                        fontSize = 14.sp
                    )
                },
                enabled = !isLoading,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = AiText,
                    unfocusedTextColor = AiText,
                    cursorColor = AiPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )

            // Botão enviar
            AnimatedVisibility(visible = value.isNotBlank() && !isLoading) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(listOf(AiPrimary, AiSecondary)),
                            CircleShape
                        )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            if (value.isBlank() && isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 4.dp),
                    color = AiPrimary,
                    strokeWidth = 2.dp
                )
            }

            // Botão microfone
            IconButton(onClick = onMic, enabled = !isLoading, modifier = Modifier.size(40.dp)) {
                val micAlpha by rememberInfiniteTransition(label = "mic").animateFloat(
                    initialValue = if (isListening) 0.4f else 1f,
                    targetValue = 1f,
                    label = "micPulse",
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
                )
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microfone",
                    tint = if (isListening) AiAccent.copy(alpha = micAlpha) else AiPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

