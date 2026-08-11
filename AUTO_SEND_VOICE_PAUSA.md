# 🎤 Auto-Envio por Detecção de Pausa na Fala (Estilo Gemini)

## 📝 Resumo
Implementado sistema de detecção automática de pausa na fala que envia automaticamente a pesquisa quando o usuário para de falar por 1.8 segundos, similar ao comportamento do Gemini.

## 🔧 Mudanças Implementadas

### 1. **Interface VoiceRecognizer** (`VoiceRecognizer.kt`)
- ✅ Adicionado `shouldAutoSend: StateFlow<Boolean>` para sinalizar quando detectar pausa

### 2. **AndroidVoiceRecognizer** (`AndroidVoiceRecognizer.kt`)

#### Novos Estados:
- `_shouldAutoSend`: Flow que indica quando deve enviar automaticamente
- `lastTextReceived`: Guarda o último texto recebido para comparação
- `autoPauseTimer`: Timer para detectar pausas na fala

#### Tempo de Silêncio Ajustado:
- **Antes**: 5000ms (5 segundos)
- **Agora**: 1800ms (1.8 segundos) - detecção mais rápida

#### Nova Lógica:
```kotlin
// Durante resultados parciais (enquanto fala)
onPartialResults() {
    - Cancela timer anterior
    - Agenda novo timer de 1.8s
    - Se não receber novos resultados em 1.8s → envia automaticamente
}

// Ao detectar fim de frase
onResults() {
    - Salva o texto acumulado
    - Agenda timer de pausa
    - Reinicia listening (modo contínuo)
}
```

#### Funções Adicionadas:
- `scheduleAutoPauseTimer()`: Agenda envio automático após 1.8s
- `cancelAutoPauseTimer()`: Cancela o timer quando nova fala é detectada

### 3. **IosVoiceRecognizer** (`IosVoiceRecognizer.kt`)
- ✅ Adicionado `shouldAutoSend` flow (estrutura básica)
- ✅ Reset do flag ao iniciar listening

### 4. **MainScreenWithAI** (`MainScreenWithAI.kt`)

#### Novo LaunchedEffect:
```kotlin
LaunchedEffect(shouldAutoSend) {
    if (shouldAutoSend) {
        val capturedText = voiceText.trim()
        if (capturedText.isNotEmpty()) {
            viewModel.updateInputFromVoice(capturedText)
            delay(300)
            viewModel.sendSearch()
        }
    }
}
```

## 🎯 Como Funciona

### Fluxo de Detecção:
1. **Usuário começa a falar** → Microfone ativo
2. **Durante a fala** → Timer é cancelado e reagendado continuamente
3. **Usuário para de falar** → Timer começa a contar 1.8 segundos
4. **1.8s de silêncio** → `shouldAutoSend = true`
5. **MainScreen detecta** → Envia pesquisa automaticamente
6. **Microfone para** → Aguarda próxima interação

### Exemplo de Uso:
```
👤 Usuário: "Quero uma pizza de calabresa"
   [fala... pausa de 1.8s]
🤖 Sistema: [Envia automaticamente] → "pizza de calabresa"
   
👤 Usuário: "com borda recheada"
   [fala... pausa de 1.8s]
🤖 Sistema: [Envia automaticamente] → "pizza de calabresa com borda recheada"
```

## ⚙️ Parâmetros Configuráveis

### Tempo de Pausa (AndroidVoiceRecognizer.kt):
```kotlin
// Linha 138: Tempo de silêncio do Android Speech API
EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = 1800

// Linha 192: Timer de auto-pausa
mainHandler.postDelayed(autoPauseTimer!!, 1800)
```

**Ajuste**: Para pausas mais longas/curtas, modifique estes valores (em milissegundos)

## 📱 Compatibilidade
- ✅ **Android**: Totalmente implementado com detecção de pausa
- ⚠️ **iOS**: Estrutura básica (pode ser expandida futuramente)

## 🔍 Testes Sugeridos
1. Falar uma frase curta e aguardar → Deve enviar automaticamente
2. Falar múltiplas frases com pausas → Deve enviar após cada pausa
3. Interromper manualmente (botão) → Deve enviar imediatamente
4. Falar sem pausas longas → Não deve enviar até pausar

## 📊 Comparação: Antes vs Agora

| Aspecto | Antes | Agora |
|---------|-------|-------|
| Tempo de silêncio | 5 segundos | 1.8 segundos |
| Envio | Manual (botão) | Automático após pausa |
| Experiência | Similar apps tradicionais | Similar ao Gemini |
| Fluidez | Requer interação | Natural e fluída |

---

**Data de Implementação**: 11 de Agosto de 2026
**Arquivos Modificados**: 4
**Linhas Adicionadas**: ~80

