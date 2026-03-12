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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.delay
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import org.leria.eats.project.permissions.PermissionStatus
import org.leria.eats.project.presentation.util.formatCurrency

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val AiDeepBg       = Color(0xFF061510)   // Deep forest black-green
private val AiSurface      = Color(0xFF0A2218)   // Dark teal surface
private val AiCard         = Color(0xFF0E2E20)   // Card teal
private val AiPrimary      = Color(0xFFFFC107)   // KOMAAI Gold
private val AiSecondary    = Color(0xFF4ADE80)   // Modern lime-green
private val AiAccent       = Color(0xFFFFD54F)   // Warm amber accent
private val AiText         = Color(0xFFF0FDF4)   // Near-white green tint
private val AiTextMuted    = Color(0xFF6EE7A0)   // Muted green
private val AiBotBubble    = Color(0xFF0D2419)   // Bot bubble dark teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: SearchUiState,
    isListening: Boolean,
    permissionStatus: PermissionStatus,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onClearSelection: () -> Unit,
    onClearSelectionAndCart: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onRemoveFromCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onSearchTypeSelected: (showRestaurants: Boolean) -> Unit,
    onDismissSearchTypeSheet: () -> Unit
) {
    LaunchedEffect(permissionStatus) { }


    // Pulsing glow animation
    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.15f, targetValue = 0.5f, label = "glowAlpha",
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiDeepBg)
    ) {



        if (uiState.selectedRestaurant != null) {
            // ── CARDÁPIO (tela de detalhe) ────────────────────────────────
            RestaurantDetailScreen(
                restaurant = uiState.selectedRestaurant,
                cartItems = uiState.cartItems,
                selectedCategory = uiState.selectedCategory,
                onCategorySelect = onCategorySelect,
                onBack = onClearSelection,
                onBackAndClearCart = onClearSelectionAndCart,
                onAdd = onAddToCart,
                onRemove = onRemoveFromCart,
                onViewCart = onViewCart
            )
        } else {
            // ── CHAT AI ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── HEADER ────────────────────────────────────────────────
                AiHeader(uiState = uiState, glowAlpha = glowAlpha)

                // ── CORPO (chat / resultados) ─────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        uiState.isLoading -> AiThinkingIndicator(modifier = Modifier.align(Alignment.Center))
                        uiState.restaurantResults.isNotEmpty() || uiState.productResults.isNotEmpty() ->
                            AiResultsBody(
                                uiState = uiState,
                                onRestaurantClick = onRestaurantClick,
                                onAddToCart = onAddToCart,
                                onViewCart = onViewCart,
                                onClearSearch = onClearSearch,
                                onTextChange = onTextChange,
                                onSendClick = onSendClick
                            )
                        else -> AiEmptyState(modifier = Modifier.align(Alignment.Center))
                    }
                }

                // ── INPUT BAR ─────────────────────────────────────────────
                AiInputBar(
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
    }

    // ── BOTTOM SHEET: escolha entre restaurantes ou produtos ──────────────────
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
                    .background(
                        Brush.verticalGradient(listOf(AiSurface, AiDeepBg))
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pill
                Box(
                    modifier = Modifier
                        .width(44.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AiTextMuted.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Ícone AI
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
                    text = "Encontrei restaurantes e produtos relacionados com a sua pesquisa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AiTextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Restaurantes
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
                                    Brush.horizontalGradient(listOf(AiPrimary, Color(0xFFE65100))),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍽️  Restaurantes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                    // Produtos
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
                                    Brush.horizontalGradient(listOf(AiSecondary, Color(0xFF15803D))),
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

// ─── Header AI ────────────────────────────────────────────────────────────────
@Composable
private fun AiHeader(uiState: SearchUiState, glowAlpha: Float) {
    val displayedText = remember { mutableStateOf("") }
    val targetText = uiState.aiReply

    LaunchedEffect(targetText) {
        displayedText.value = ""
        for (i in targetText.indices) {
            displayedText.value = targetText.substring(0, i + 1)
            delay(18)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo + brand row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 0.dp)
                .padding(bottom = 12.dp)
        ) {
            // AI orb
            Box(
                modifier = Modifier
                    .background(
                        Brush.radialGradient(
                            listOf(AiPrimary.copy(alpha = 0.7f), AiSecondary.copy(alpha = 0.3f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                KamelImage(
                    resource = asyncPainterResource("https://leiria-eats-repo.s3.us-east-2.amazonaws.com/logo%3Dpato.png"),
                    contentDescription = "Logo",
                    modifier = Modifier.size(112.dp),
                    contentScale = ContentScale.Fit,
                    onFailure = {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
        }

        // AI reply bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(AiBotBubble, AiCard)
                    )
                )
                .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = displayedText.value,
                style = MaterialTheme.typography.bodyMedium,
                color = AiText,
                textAlign = TextAlign.Start
            )
        }

        // Favorites tip — only shown on the initial greeting
        val isGreeting = uiState.aiReply.startsWith("Olá") &&
                uiState.restaurantResults.isEmpty() &&
                uiState.productResults.isEmpty() &&
                !uiState.isLoading

        AnimatedVisibility(
            visible = isGreeting,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 800)) +
                    expandVertically(animationSpec = tween(500, delayMillis = 800)),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Brush.horizontalGradient(listOf(AiBotBubble, AiCard)))
                        .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 Sabia que pode guardar favoritos?",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = AiPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Marque qualquer pedido com ⭐ e dê-lhe um apelido (ex: \"Jantar em família\"). Para o repetir, use sempre a palavra ",
                            fontSize = 13.sp,
                            color = AiText,
                            lineHeight = 18.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AiPrimary.copy(alpha = 0.18f))
                                    .border(1.dp, AiPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "pedir",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AiPrimary
                                )
                            }
                            Text(
                                text = " seguida do apelido:",
                                fontSize = 13.sp,
                                color = AiText,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiPrimary.copy(alpha = 0.12f))
                                .border(1.dp, AiPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🎤  \"pedir Jantar em família\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AiAccent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(Brush.horizontalGradient(listOf(AiBotBubble, AiCard)))
                        .border(1.dp, AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "🍽️ Quer explorar todos os restaurantes?",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = AiSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Para ver todos os restaurantes disponíveis, basta dizer ou digitar:",
                            fontSize = 13.sp,
                            color = AiText,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AiSecondary.copy(alpha = 0.12f))
                                .border(1.dp, AiSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🎤  \"ver todos\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AiSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Thinking indicator ───────────────────────────────────────────────────────
@Composable
private fun AiThinkingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AiBotBubble)
                .border(1.dp, AiPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("A pensar", fontSize = 13.sp, color = AiTextMuted)
            (0..2).forEach { index ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f, label = "dot$index",
                    animationSpec = infiniteRepeatable(
                        tween(600, delayMillis = index * 180),
                        RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AiPrimary.copy(alpha = alpha), CircleShape)
                )
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun AiEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

// ─── Results body ─────────────────────────────────────────────────────────────
@Composable
private fun AiResultsBody(
    uiState: SearchUiState,
    onRestaurantClick: (Restaurant) -> Unit,
    onAddToCart: (Product) -> Unit,
    onViewCart: () -> Unit,
    onClearSearch: () -> Unit,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(AiSurface)
            .fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp)
        ) {
            // Header row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AiPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resultados encontrados", color = AiTextMuted, fontSize = 13.sp)
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
                        Text("Limpar", color = AiTextMuted, fontSize = 12.sp, modifier = Modifier.clickable { onClearSearch() })
                    }
                }
            }

            // Restaurants
            if (uiState.restaurantResults.isNotEmpty()) {
                item {
                    AiSectionLabel(label = "Restaurantes")
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(max = 1000.dp)
                    ) {
                        items(uiState.restaurantResults) { restaurant ->
                            RestaurantGridItem(restaurant = restaurant, onClick = { onRestaurantClick(restaurant) })
                        }
                    }
                }
            }

            // Products
            if (uiState.productResults.isNotEmpty()) {
                item { AiSectionLabel(label = "Produtos", color = AiSecondary) }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .heightIn(max = 1000.dp)
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

@Composable
private fun AiSectionLabel(label: String, color: Color = AiPrimary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 10.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(16.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AiText)
    }
}

// ─── AI Input Bar ─────────────────────────────────────────────────────────────
@Composable
private fun AiInputBar(
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AiCard)
            .then(
                if (isListening)
                    Modifier.border(1.5.dp, AiAccent.copy(alpha = borderAlpha), RoundedCornerShape(28.dp))
                else
                    Modifier.border(1.5.dp, Brush.horizontalGradient(listOf(AiPrimary.copy(alpha = borderAlpha * 0.6f), AiSecondary.copy(alpha = borderAlpha * 0.4f))), RoundedCornerShape(28.dp))
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text field
        BasicAiTextField(
            value = value,
            isListening = isListening,
            isLoading = isLoading,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )

        // Send button
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

        // Mic button (right side)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicAiTextField(
    value: String,
    isListening: Boolean,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                if (isListening) "🎤 A ouvir…" else "✨ Diga-me ou digite o que deseja... vou escolher o melhor prato para si!",
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
        modifier = modifier
    )
}

// ─── Restaurant Grid Item ─────────────────────────────────────────────────────
@Composable
fun RestaurantGridItem(restaurant: Restaurant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, AiPrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = restaurant.image_url ?: ""),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize().background(AiCard), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AiPrimary) } },
                onFailure = { Box(Modifier.fillMaxSize().background(AiCard)) }
            )
            // subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AiDeepBg.copy(alpha = 0.5f))))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = restaurant.name,
            fontWeight = FontWeight.SemiBold,
            color = AiText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(9.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "${restaurant.rating ?: 5.0}",
                color = AiTextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

// ─── Product Grid Item ────────────────────────────────────────────────────────
@Composable
fun ProductGridItem(product: Product, onAddToCart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AiCard)
            .border(1.dp, AiSecondary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            KamelImage(
                resource = asyncPainterResource(data = product.image_url ?: ""),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { Box(Modifier.fillMaxSize().background(AiSurface), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AiSecondary) } },
                onFailure = { Box(Modifier.fillMaxSize().background(AiSurface)) }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AiDeepBg.copy(alpha = 0.5f))))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                color = AiText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.description,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AiTextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(product.price),
                    fontWeight = FontWeight.Bold,
                    color = AiSecondary,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            Brush.linearGradient(listOf(AiPrimary, AiSecondary)),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Adicionar",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
