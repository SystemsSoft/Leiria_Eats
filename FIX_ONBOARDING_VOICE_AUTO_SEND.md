# Correção: Auto-envio de Voz no OnboardingScreen

## Problema Identificado
Após separar os contextos de voz, o OnboardingScreen (configuração de perfil) não estava processando o reconhecimento de voz. Quando o usuário falava no microfone, nada acontecia.

## Requisitos do Usuário
1. ✅ Mostrar o texto em tempo real no campo enquanto o usuário está falando
2. ✅ Quando o usuário parar de falar, enviar automaticamente o que foi dito
3. ✅ Evitar envios duplicados

## Solução Implementada

### 1. Atualização do LaunchedEffect (OnboardingChatScreen.kt)

#### Antes:
```kotlin
// Handle voice input
LaunchedEffect(recognizedText) {
    if (recognizedText.isNotEmpty() && !isListening) {
        inputText = recognizedText
    }
}
```

**Problemas:**
- Só atualizava o campo quando **parava** de ouvir
- Não mostrava feedback em tempo real
- Não enviava automaticamente
- Usuário tinha que clicar no botão enviar manualmente

#### Depois:
```kotlin
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
```

### 2. Adição de Flag de Controle

Adicionada variável `lastProcessedVoiceText` para evitar processar a mesma mensagem duas vezes:

```kotlin
var lastProcessedVoiceText by remember { mutableStateOf("") }
```

### 3. Proteção Contra Duplicatas

#### No envio manual (botão):
```kotlin
onSendClick = {
    if (inputText.isNotBlank()) {
        lastProcessedVoiceText = "" // Reset flag ao enviar manualmente
        scope.launch {
            processUserInput(...)
            inputText = ""
        }
    }
}
```

#### Na edição manual do campo:
```kotlin
onInputChange = { 
    inputText = it
    // Se o usuário começar a digitar manualmente, limpa a flag
    if (it != recognizedText) {
        lastProcessedVoiceText = ""
    }
}
```

## Fluxo de Funcionamento

```
┌─────────────────────────────────────────────────────────┐
│ 1. Usuário pressiona o botão do microfone              │
│    - VoiceRecognizer.startListening(VoiceContext.ONBOARDING)
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Usuário fala "João Silva"                           │
│    - recognizedText é atualizado em tempo real         │
│    - isListening = true                                 │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Campo de texto mostra "João Silva" em tempo real     │
│    - inputText = recognizedText                         │
│    - Usuário vê o que está sendo reconhecido           │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Usuário para de falar (1.8s de silêncio)            │
│    - isListening muda para false                        │
│    - VoiceRecognizer para automaticamente               │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 5. LaunchedEffect detecta que parou de ouvir           │
│    - Verifica: !isListening ✅                          │
│    - Verifica: recognizedText.isNotBlank() ✅           │
│    - Verifica: recognizedText != lastProcessedVoiceText ✅
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Espera 300ms para garantir que capturou tudo        │
│    - delay(300)                                         │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 7. Marca como processado e envia automaticamente       │
│    - lastProcessedVoiceText = "João Silva"              │
│    - processUserInput("João Silva", ...)                │
│    - inputText = "" (limpa o campo)                     │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ 8. IA processa e responde                               │
│    - Armazena userName = "João Silva"                   │
│    - Exibe: "Muito bem, João Silva! 😊 Agora, qual é   │
│      o teu email?"                                      │
└─────────────────────────────────────────────────────────┘
```

## Proteções Implementadas

### 1. Contra Processamento Duplicado
```kotlin
recognizedText != lastProcessedVoiceText
```
- Garante que a mesma mensagem não seja processada duas vezes
- Essencial quando há múltiplas atualizações do LaunchedEffect

### 2. Contra Envio Manual Duplicado
```kotlin
lastProcessedVoiceText = "" // Reset no onSendClick
```
- Se o usuário clicar no botão enviar após a voz, permite o envio manual
- Previne bloqueios acidentais

### 3. Contra Conflito de Edição
```kotlin
if (it != recognizedText) {
    lastProcessedVoiceText = ""
}
```
- Se o usuário começar a digitar/editar, reseta a flag
- Permite editar o texto reconhecido antes de enviar

## Diferença do Comportamento com AI Search

### OnboardingScreen:
- ✅ Mostra texto em tempo real
- ✅ Auto-envia após parar de falar
- ✅ Contexto isolado (ONBOARDING)
- ✅ Não interfere com outras telas

### AI Search Screen:
- ✅ Mostra texto em tempo real
- ✅ Auto-envia após parar de falar
- ✅ Contexto isolado (AI_SEARCH)
- ✅ Envia para busca semântica
- ✅ Não interfere com outras telas

## Resultado Final

✅ **Problema Resolvido**: Agora quando o usuário fala no cadastro do perfil:
1. O texto aparece em tempo real no campo
2. Quando para de falar, o texto é enviado automaticamente
3. Não há envios duplicados
4. Não há interferência com a tela de realizar pedido
5. Experiência similar à tela de IA, mas com contexto isolado

## Teste Completo

### Cenário 1: Voz no Onboarding
1. Abrir app pela primeira vez (sem perfil)
2. Ir para tela de Perfil
3. Clicar no microfone
4. Falar "João Silva"
5. ✅ Verificar que aparece no campo em tempo real
6. Parar de falar (aguardar 1-2 segundos)
7. ✅ Verificar que foi enviado automaticamente
8. ✅ Verificar que a IA respondeu pedindo o email
9. Trocar para tela de IA
10. ✅ Verificar que o campo de realizar pedido está vazio

### Cenário 2: Edição Manual Após Voz
1. Falar "Joao"
2. Antes de parar completamente, começar a digitar e corrigir para "João"
3. Clicar em enviar
4. ✅ Verificar que enviou "João" corretamente

### Cenário 3: Múltiplas Mensagens de Voz
1. Falar nome
2. Aguardar resposta da IA
3. Falar email
4. Aguardar resposta da IA
5. Falar telefone
6. ✅ Verificar que cada mensagem foi processada uma única vez

