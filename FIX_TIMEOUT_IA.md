# 🛠️ Correção de Timeout no Endpoint `/chat/sales`

## 📋 Problema Identificado

A requisição ao endpoint `/chat/sales` estava causando timeout porque o modelo TinyLlama demora um tempo considerável para gerar a resposta conversacional generativa.

**Log do servidor:**
```
🤖 [TinyLlama] Gerando resposta conversacional generativa...
[transformers] Both `max_new_tokens` (=150) and `max_length`(=2048) seem to have been set...
✅ [TinyLlama] Resposta generativa criada!
✅ Resposta final gerada
```

## ✅ Solução Implementada

### Arquivo Modificado
`composeApp/src/commonMain/kotlin/org/leria/eats/project/data/LeriaApiClient.kt`

### Mudanças Realizadas

1. **Adicionado import do plugin de timeout:**
```kotlin
import io.ktor.client.plugins.HttpTimeout
```

2. **Configurado o plugin HttpTimeout no HttpClient:**
```kotlin
install(HttpTimeout) {
    // Timeout específico para requisições de IA (2 minutos)
    requestTimeoutMillis = 120_000 // 2 minutos para a requisição completa
    connectTimeoutMillis = 30_000  // 30 segundos para conectar
    socketTimeoutMillis = 120_000   // 2 minutos para operações de socket
}
```

## ⏱️ Configuração de Timeouts

| Tipo de Timeout | Valor | Descrição |
|-----------------|-------|-----------|
| `requestTimeoutMillis` | 120.000ms (2 min) | Tempo máximo para completar toda a requisição |
| `connectTimeoutMillis` | 30.000ms (30 seg) | Tempo máximo para estabelecer conexão |
| `socketTimeoutMillis` | 120.000ms (2 min) | Tempo máximo de inatividade no socket |

## 🎯 Resultado

✅ **Compilação Android:** Bem-sucedida
✅ **Timeout configurado:** 2 minutos (suficiente para IA processar)
✅ **Endpoint afetado:** `/chat/sales` e todos os outros endpoints do cliente

## 📝 Notas Importantes

- O timeout de **2 minutos** foi escolhido considerando:
  - Tempo de processamento do modelo TinyLlama
  - Margem de segurança para variações de performance
  - Experiência do usuário (não muito longo)

- Se ainda ocorrerem timeouts, considere:
  - Aumentar `requestTimeoutMillis` e `socketTimeoutMillis` para 180.000ms (3 minutos)
  - Otimizar o modelo no backend
  - Implementar streaming de resposta (se possível)
  - Adicionar indicador de progresso mais detalhado no app

## 🔄 Como Ajustar se Necessário

Para aumentar ainda mais o timeout, edite em `LeriaApiClient.kt`:

```kotlin
install(HttpTimeout) {
    requestTimeoutMillis = 180_000 // 3 minutos
    socketTimeoutMillis = 180_000   // 3 minutos
}
```

---
**Data da correção:** 10 de Agosto, 2026
**Status:** ✅ Implementado e testado

