# Correção: Separação de Microfones por Contexto

## Problema Identificado
Quando o usuário falava pelo microfone no cadastro do perfil (OnboardingScreen), o texto aparecia também na tela de realizar pedido (AI Search).

## Causa do Problema
Os `LaunchedEffect` que processavam o texto de voz (`voiceText`) no arquivo `MainScreenWithAI.kt` não estavam verificando o contexto antes de atualizar o campo de entrada. Isso fazia com que QUALQUER texto reconhecido fosse enviado para o viewModel, independentemente de qual tela estava ativa.

## Solução Implementada

### 1. Correção nos LaunchedEffect (linhas 131-159)

#### Antes:
```kotlin
LaunchedEffect(voiceText) {
    if (voiceText.isNotEmpty()) {
        viewModel.updateInputFromVoice(voiceText)
    }
}
```

#### Depois:
```kotlin
LaunchedEffect(voiceText, voiceContext) {
    // Só atualiza o input se o contexto for de IA ou Home Search
    if (voiceText.isNotEmpty() && (voiceContext == VoiceContext.AI_SEARCH || voiceContext == VoiceContext.HOME_SEARCH)) {
        viewModel.updateInputFromVoice(voiceText)
    }
}
```

### 2. Filtragem em Todos os LaunchedEffect

Foram atualizados 3 `LaunchedEffect` principais:

1. **Processamento inicial de voz**: Só atualiza o input quando o contexto é `AI_SEARCH` ou `HOME_SEARCH`
2. **Auto-send após pausa**: Só envia automaticamente quando o contexto é `AI_SEARCH` ou `HOME_SEARCH`
3. **Envio ao parar de ouvir**: Só envia quando o contexto é `AI_SEARCH` ou `HOME_SEARCH`

### 3. Como Funciona Agora

#### OnboardingScreen (Cadastro de Perfil)
- Contexto: `VoiceContext.ONBOARDING`
- O texto reconhecido é passado diretamente via parâmetro `recognizedText`
- Os LaunchedEffect do MainScreenWithAI **IGNORAM** o texto quando o contexto é ONBOARDING
- O texto NÃO chega ao `viewModel.updateInputFromVoice()`

#### AI Search Screen (Realizar Pedido)
- Contexto: `VoiceContext.AI_SEARCH`
- Os LaunchedEffect **PROCESSAM** o texto normalmente
- O texto é enviado para o viewModel e a busca é realizada

#### Home Search Screen
- Contexto: `VoiceContext.HOME_SEARCH`
- Os LaunchedEffect **PROCESSAM** o texto normalmente
- O texto é enviado para o viewModel e a busca é realizada

## Fluxo de Processamento

```
┌─────────────────────────────────────────────────────────┐
│ Usuário fala no microfone                                │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ VoiceRecognizer reconhece e armazena com contexto       │
│ - voiceText: "texto reconhecido"                        │
│ - voiceContext: ONBOARDING | AI_SEARCH | HOME_SEARCH    │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ LaunchedEffect verifica o contexto                      │
└───────────────────┬─────────────────────────────────────┘
                    │
        ┌───────────┴──────────┐
        │                      │
        ▼                      ▼
┌──────────────┐      ┌────────────────────┐
│ ONBOARDING   │      │ AI_SEARCH ou       │
│              │      │ HOME_SEARCH        │
│ IGNORA       │      │                    │
│ (não envia   │      │ PROCESSA           │
│ para o       │      │ (envia para o      │
│ viewModel)   │      │ viewModel)         │
└──────────────┘      └────────────────────┘
```

## Testes Recomendados

1. **Teste no Onboarding**:
   - Falar "João Silva" no campo de nome
   - Trocar para a tela de IA
   - Verificar que "João Silva" NÃO aparece no campo de realizar pedido

2. **Teste na IA**:
   - Falar "pizza de calabresa"
   - Verificar que a busca é realizada corretamente
   - Trocar para a tela de perfil (se já tiver perfil)
   - Voltar para IA e verificar que o campo foi limpo

3. **Teste de Troca de Tabs**:
   - Falar algo em qualquer tela
   - Trocar de tab
   - Verificar que o reconhecimento de voz é parado
   - Verificar que os resultados são limpos

## Resultado Final
✅ Microfones completamente isolados por contexto
✅ Não há mais interferência entre telas
✅ Cada tela processa apenas seu próprio contexto de voz

