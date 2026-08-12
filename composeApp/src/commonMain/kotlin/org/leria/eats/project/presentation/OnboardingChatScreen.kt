package org.leria.eats.project.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.random.Random
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.imePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.leria.eats.project.data.Address
import org.leria.eats.project.theme.*
import org.leria.eats.project.voice.TextToSpeechService

// ─── Função auxiliar para limpar texto para TTS ──────────────────────────────
private fun prepareTextForTts(text: String): String =
    text
        .replace(Regex("[\\p{So}\\p{Sm}\\p{Sk}\\p{Sc}]"), "")
        .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "")
        .replace(Regex("[\u2600-\u27FF]"), "")
        .replace(Regex("[\uFE00-\uFE0F]"), "")
        .replace(Regex("\\bx1\\b", RegexOption.IGNORE_CASE), "uma")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val ODeepBg = KomaBg
private val OCard = KomaCard
private val OGold = KomaGold
private val OGreen = KomaBrandGreen
private val OText = KomaTextPrimary
private val OMuted = KomaTextSec

// Generate unique message ID
private fun generateMessageId(): String {
    return "${kotlin.random.Random.nextLong()}_${Random.nextInt()}"
}

enum class OnboardingStep {
    WELCOME,
    NAME,
    EMAIL,
    PHONE,
    ADDRESS,
    COMPLETE
}

data class OnboardingMessage(
    val id: String,
    val text: String,
    val isAi: Boolean,
    val step: OnboardingStep? = null
)

@Composable
fun OnboardingChatScreen(
    isListening: Boolean,
    recognizedText: String,
    onMicClick: () -> Unit,
    onComplete: (name: String, email: String, phone: String, address: Address?) -> Unit,
    onGetAddressFromMap: (Double, Double) -> String?,
    tts: TextToSpeechService,
    isMuted: Boolean = false
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var messages by remember { mutableStateOf<List<OnboardingMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }

    // User data collection
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userAddress by remember { mutableStateOf<Address?>(null) }

    var showMapDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastProcessedVoiceText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Add initial welcome message
    LaunchedEffect(Unit) {
        delay(500)
        addAiMessage(
            messages = messages,
            text = "Olá! 👋 Bem-vindo ao KOMA Aí, vamos começar com o teu perfil",
            step = OnboardingStep.WELCOME,
            onUpdate = { messages = it },
            tts = tts,
            isMuted = isMuted
        )
        delay(5000)
        addAiMessage(
            messages = messages,
            text = "Me diga o teu nome?",
            step = OnboardingStep.NAME,
            onUpdate = {
                messages = it
                currentStep = OnboardingStep.NAME
            },
            tts = tts,
            isMuted = isMuted
        )
    }

    // Handle voice input - mostrar em tempo real e enviar quando parar
    LaunchedEffect(recognizedText, isListening) {
        if (recognizedText.isNotEmpty()) {
            // Atualiza o campo de texto em tempo real enquanto fala
            inputText = recognizedText
            
            // Quando parar de falar, envia automaticamente após um delay
            if (!isListening && recognizedText.isNotBlank() && recognizedText != lastProcessedVoiceText) {
                delay(300) // Pequeno delay para garantir que capturou tudo
                lastProcessedVoiceText = recognizedText // Marca como processado
                // Envia automaticamente
                scope.launch {
                    processUserInput(
                        input = recognizedText,
                        currentStep = currentStep,
                        messages = messages,
                        onUpdateMessages = { messages = it },
                        onUpdateStep = { currentStep = it },
                        onUpdateName = { userName = it },
                        onUpdateEmail = { userEmail = it },
                        onUpdatePhone = { userPhone = it },
                        onUpdateAddress = { userAddress = it },
                        onComplete = { onComplete(userName, userEmail, userPhone, userAddress) },
                        tts = tts,
                        isMuted = isMuted,
                        onProcessing = { isProcessing = it }
                    )
                    inputText = "" // Limpa o campo após enviar
                }
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var pendingMapCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(pendingMapCoords) {
        val coords = pendingMapCoords ?: return@LaunchedEffect
        val selectedAddress = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            onGetAddressFromMap(coords.first, coords.second)
        }
        if (selectedAddress != null) {
            userAddress = Address("Casa", selectedAddress, latitude = coords.first, longitude = coords.second)
            messages = messages + OnboardingMessage(
                id = "${generateMessageId()}_user",
                text = selectedAddress,
                isAi = false,
                step = OnboardingStep.ADDRESS
            )
            delay(800)
            addAiMessage(
                messages = messages,
                text = "Perfeito! ✨ Tudo configurado! Vamos começar? Podes me pedir qualquer prato que quiseres!",
                step = OnboardingStep.COMPLETE,
                onUpdate = { messages = it },
                tts = tts,
                isMuted = isMuted
            )
            delay(2500)
            onComplete(userName, userEmail, userPhone, userAddress)
        }
        pendingMapCoords = null
    }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                pendingMapCoords = Pair(lat, long)
                showMapDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ODeepBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                OCard,
                                ODeepBg
                            )
                        )
                    )
                    .padding(vertical = 20.dp, horizontal = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(OGold.copy(alpha = 0.3f), OGreen.copy(alpha = 0.2f))
                                ),
                                CircleShape
                            )
                            .border(2.dp, OGold.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configuração de Perfil",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OText
                    )
                    Text(
                        "Responde às perguntas para começar",
                        fontSize = 12.sp,
                        color = OMuted
                    )
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    if (message.isAi) {
                        AiMessageBubble(
                            message = message,
                            onMapClick = if (message.step == OnboardingStep.ADDRESS) {
                                { showMapDialog = true }
                            } else null
                        )
                    } else {
                        UserMessageBubble(message = message)
                    }
                }

                if (isProcessing) {
                    item {
                        AiTypingIndicator()
                    }
                }
            }

            // Input area
            OnboardingInputArea(
                inputText = inputText,
                onInputChange = {
                    inputText = it
                    // Se o usuário começar a digitar manualmente, limpa a flag
                    if (it != recognizedText) {
                        lastProcessedVoiceText = ""
                    }
                },
                isListening = isListening,
                onMicClick = onMicClick,
                onSendClick = {
                    if (inputText.isNotBlank()) {
                        lastProcessedVoiceText = "" // Reset flag ao enviar manualmente
                        scope.launch {
                            processUserInput(
                                input = inputText,
                                currentStep = currentStep,
                                messages = messages,
                                onUpdateMessages = { messages = it },
                                onUpdateStep = { currentStep = it },
                                onUpdateName = { userName = it },
                                onUpdateEmail = { userEmail = it },
                                onUpdatePhone = { userPhone = it },
                                onUpdateAddress = { userAddress = it },
                                onComplete = { onComplete(userName, userEmail, userPhone, userAddress) },
                                tts = tts,
                                isMuted = isMuted,
                                onProcessing = { isProcessing = it }
                            )
                            inputText = ""
                        }
                    }
                },
                currentStep = currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            )
        }
    }
}

private suspend fun processUserInput(
    input: String,
    currentStep: OnboardingStep,
    messages: List<OnboardingMessage>,
    onUpdateMessages: (List<OnboardingMessage>) -> Unit,
    onUpdateStep: (OnboardingStep) -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateAddress: (Address?) -> Unit,
    onComplete: () -> Unit,
    tts: TextToSpeechService,
    isMuted: Boolean,
    onProcessing: (Boolean) -> Unit
) {
    // Add user message
    val newMessages = messages + OnboardingMessage(
        id = "${generateMessageId()}_user",
        text = input,
        isAi = false,
        step = currentStep
    )
    onUpdateMessages(newMessages)
    onProcessing(true)
    delay(800)
    onProcessing(false)

    when (currentStep) {
        OnboardingStep.NAME -> {
            onUpdateName(input)
            addAiMessage(
                messages = newMessages,
                text = "Muito bem, $input! 😊 Agora, qual é o teu email?",
                step = OnboardingStep.EMAIL,
                onUpdate = {
                    onUpdateMessages(it)
                    onUpdateStep(OnboardingStep.EMAIL)
                },
                tts = tts,
                isMuted = isMuted
            )
        }
        OnboardingStep.EMAIL -> {
            onUpdateEmail(input)
            addAiMessage(
                messages = newMessages,
                text = "Excelente! 📧 Qual é o teu número de telefone?",
                step = OnboardingStep.PHONE,
                onUpdate = {
                    onUpdateMessages(it)
                    onUpdateStep(OnboardingStep.PHONE)
                },
                tts = tts,
                isMuted = isMuted
            )
        }
        OnboardingStep.PHONE -> {
            onUpdatePhone(input)
            addAiMessage(
                messages = newMessages,
                text = "Ótimo! 📱 Por último, preciso do teu endereço de entrega. Podes escrever ou selecionar no mapa.",
                step = OnboardingStep.ADDRESS,
                onUpdate = {
                    onUpdateMessages(it)
                    onUpdateStep(OnboardingStep.ADDRESS)
                },
                tts = tts,
                isMuted = isMuted
            )
        }
        OnboardingStep.ADDRESS -> {
            onUpdateAddress(Address("Casa", input))
            addAiMessage(
                messages = newMessages,
                text = "Perfeito! ✨ Tudo configurado! Vamos começar? Podes me pedir qualquer prato que quiseres!",
                step = OnboardingStep.COMPLETE,
                onUpdate = { onUpdateMessages(it) },
                tts = tts,
                isMuted = isMuted
            )
            delay(2500)
            onComplete()
        }
        else -> {}
    }
}

private fun addAiMessage(
    messages: List<OnboardingMessage>,
    text: String,
    step: OnboardingStep,
    onUpdate: (List<OnboardingMessage>) -> Unit,
    tts: TextToSpeechService,
    isMuted: Boolean
) {
    val newMessage = OnboardingMessage(
        id = "${generateMessageId()}_ai",
        text = text,
        isAi = true,
        step = step
    )
    onUpdate(messages + newMessage)

    if (!isMuted) {
        val cleanedText = prepareTextForTts(text)
        if (cleanedText.isNotBlank()) {
            tts.speak(cleanedText)
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: OnboardingMessage,
    onMapClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                OGold.copy(alpha = 0.15f),
                                OGreen.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                OGold.copy(alpha = 0.4f),
                                OGreen.copy(alpha = 0.3f)
                            )
                        ),
                        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        "KOMA AI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OGold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        message.text,
                        fontSize = 14.sp,
                        color = OText,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Show map button if this is an address message
        if (onMapClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OGreen.copy(alpha = 0.15f))
                    .border(1.dp, OGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onMapClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Abrir Mapa",
                        tint = OGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Selecionar no Mapa",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OText
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: OnboardingMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .background(OCard)
                .border(1.dp, OGold.copy(alpha = 0.2f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
                .padding(12.dp)
        ) {
            Text(
                message.text,
                fontSize = 14.sp,
                color = OText,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AiTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            OGold.copy(alpha = 0.15f),
                            OGreen.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(OGold.copy(alpha = 0.6f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(OCard)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ODeepBg)
                    .border(1.dp, OGold.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            getPlaceholderForStep(currentStep),
                            fontSize = 14.sp,
                            color = OMuted
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when (currentStep) {
                            OnboardingStep.EMAIL -> KeyboardType.Email
                            OnboardingStep.PHONE -> KeyboardType.Phone
                            else -> KeyboardType.Text
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = OText,
                        unfocusedTextColor = OText,
                        cursorColor = OGold,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mic button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .then(
                        if (isListening)
                            Modifier.background(Brush.linearGradient(listOf(OGold, KomaOrangeEnd)))
                        else
                            Modifier.background(OCard)
                    )
                    .border(
                        1.dp,
                        if (isListening) Color.Transparent else OGold.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Microfone",
                    tint = if (isListening) KomaGoldOnDark else OGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send button
            AnimatedVisibility(
                visible = inputText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(OGold, KomaOrangeEnd)))
                        .clickable { onSendClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = KomaGoldOnDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun getPlaceholderForStep(step: OnboardingStep): String {
    return when (step) {
        OnboardingStep.NAME -> "Digite o seu nome..."
        OnboardingStep.EMAIL -> "Digite o seu email..."
        OnboardingStep.PHONE -> "Digite o seu telefone..."
        OnboardingStep.ADDRESS -> "Digite o seu endereço..."
        else -> "Digite aqui..."
    }
}

