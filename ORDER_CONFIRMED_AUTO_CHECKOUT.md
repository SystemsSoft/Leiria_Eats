# 🛒 Checkout Automático com order_confirmed

## 📝 Resumo
Implementado sistema de checkout automático quando a IA confirma que o pedido está pronto. Quando o backend retorna `order_confirmed: true` no `ChatResponse`, o app automaticamente agrupa os produtos por restaurante e inicia o processo de checkout.

## 🔧 Mudanças Implementadas

### 1. **Models.kt** - Adicionado campo `order_confirmed` ao ChatResponse

```kotlin
@Serializable
data class ChatResponse(
    val response: String? = null,
    val intent: String? = null,
    val restaurantResults: List<Restaurant> = emptyList(),
    val productResults: List<Product> = emptyList(),
    val products: List<Product> = emptyList(),
    @SerialName("order_confirmed")
    val orderConfirmed: Boolean = false  // NOVO: indica checkout automático
)
```

### 2. **SearchViewModel.kt** - Implementada detecção e processamento

#### Mudança na `fetchSearch()`
- Agora usa `apiClient.sendChatMessage()` ao invés do deprecated `searchRestaurants()`
- Detecta quando `chatResponse.orderConfirmed == true`
- Chama `handleOrderConfirmation()` automaticamente

```kotlin
// Usa o novo endpoint com IA Generativa
val chatResponse = apiClient.sendChatMessage(resolvedQuery.trim())

// Verifica se o pedido foi confirmado pela IA
if (chatResponse.orderConfirmed) {
    handleOrderConfirmation(chatResponse)
    return@launch
}
```

#### Nova função `handleOrderConfirmation()`
Processa a confirmação do pedido com a seguinte lógica:

**1. Adiciona mensagem de confirmação da IA**
```kotlin
val confirmationMessage = chatResponse.response ?: "Perfeito! Vou finalizar o seu pedido."
addAiMessage(text = confirmationMessage)
```

**2. Valida o carrinho**
- Verifica se há produtos no carrinho
- Identifica os restaurantes dos produtos

**3. Agrupa por restaurante**
- **Carrinho vazio**: Mostra erro
- **Múltiplos restaurantes**: Avisa usuário e navega para CART
- **Um restaurante**: Prossegue com checkout automático

**4. Inicia checkout automático**
```kotlin
_uiState.update { 
    it.copy(
        isLoading = false,
        cartRestaurantId = restaurantId,
        currentTab = MainTab.CART
    ) 
}

delay(300)  // Feedback visual
checkout()  // Inicia processo de checkout
```

## 🎯 Fluxo de Funcionamento

### Cenário 1: Pedido Normal (1 Restaurante)
```
👤 Usuário: "Quero confirmar o pedido"
🤖 IA: Detecta intenção de finalizar
📡 Backend: Retorna order_confirmed: true
📱 App: Agrupa produtos → Navega para CART → Inicia checkout()
```

### Cenário 2: Múltiplos Restaurantes
```
👤 Usuário: "Quero confirmar o pedido"
🤖 IA: Detecta intenção
📡 Backend: Retorna order_confirmed: true
📱 App: Detecta 2+ restaurantes
💬 IA: "Os produtos são de 2 restaurantes diferentes..."
📱 App: Navega para CART (usuário escolhe)
```

### Cenário 3: Carrinho Vazio
```
👤 Usuário: "Quero confirmar o pedido"
🤖 IA: Detecta intenção
📡 Backend: Retorna order_confirmed: true
📱 App: Carrinho vazio
💬 IA: "Não há produtos no carrinho para finalizar..."
```

## 🔍 Validações Implementadas

### 1. **Validação de Carrinho**
```kotlin
if (currentState.cartItems.isEmpty()) {
    addAiMessage(text = "Não há produtos no carrinho...")
    return
}
```

### 2. **Validação de Restaurante**
```kotlin
val restaurantIds = currentState.cartItems
    .mapNotNull { it.restaurant_id }
    .distinct()

if (restaurantIds.isEmpty()) {
    addAiMessage(text = "Erro: Não foi possível identificar o restaurante...")
    return
}
```

### 3. **Validação de Múltiplos Restaurantes**
```kotlin
if (restaurantIds.size > 1) {
    addAiMessage(text = "Os produtos são de ${restaurantIds.size} restaurantes diferentes...")
    onTabSelected(MainTab.CART)  // Deixa usuário escolher
    return
}
```

## 📱 Navegação Automática

Quando `order_confirmed = true`:
1. ✅ Atualiza `cartRestaurantId` com o ID do restaurante
2. ✅ Muda tab para `MainTab.CART`
3. ✅ Chama `checkout()` que mostra o sheet de endereços
4. ✅ Usuário seleciona endereço → Fluxo normal de pagamento

## 🎨 Experiência do Usuário

### Antes:
```
1. Usuário adiciona produtos ao carrinho
2. Vai manualmente para a tab CART
3. Clica no botão "Finalizar Pedido"
4. Seleciona endereço
5. Confirma pagamento
```

### Agora (com order_confirmed):
```
1. Usuário: "Quero confirmar o pedido"
2. IA: "Perfeito! Vou finalizar o seu pedido."
3. App navega automaticamente para CART
4. Sheet de endereço já aparece
5. Usuário só precisa confirmar endereço e pagamento
```

## 🔧 Backend - Quando retornar `order_confirmed: true`

O backend deve retornar `order_confirmed: true` quando detectar frases como:

- "Quero confirmar o pedido"
- "Pode finalizar"
- "Fechar pedido"
- "Quero pagar"
- "Tá bom, finaliza"
- "Sim, pode fechar"
- Etc.

### Exemplo de resposta do backend:
```json
{
  "response": "Perfeito! Vou finalizar o seu pedido agora.",
  "order_confirmed": true,
  "products": [],
  "intent": "checkout"
}
```

## 📊 Estados do Sistema

| Campo | Tipo | Quando usar |
|-------|------|-------------|
| `order_confirmed` | `boolean` | `true` quando usuário confirma que quer fechar o pedido |
| `products` | `List<Product>` | Lista de produtos sugeridos pela IA (não afeta checkout) |
| `response` | `string` | Mensagem de confirmação mostrada ao usuário |
| `intent` | `string` | Opcional: "checkout" ou similar para tracking |

## 🚨 Casos Especiais Tratados

### 1. Produtos de múltiplos restaurantes
- ✅ App detecta e avisa o usuário
- ✅ Navega para CART para visualização
- ✅ Não força checkout automático

### 2. Carrinho vazio
- ✅ Mostra mensagem de erro amigável
- ✅ Não trava o app

### 3. Restaurante não identificado
- ✅ Mostra erro específico
- ✅ Não prossegue com checkout

## 📝 Notas Técnicas

### IDs de Mensagens
- Mudado de `System.currentTimeMillis()` para `kotlin.random.Random.nextLong()`
- Compatível com Kotlin Multiplatform (KMP)

### Imports Adicionados
```kotlin
import org.leria.eats.project.data.ChatResponse
import org.leria.eats.project.data.SearchResponse
```

### Migração de API
- `searchRestaurants()` → `sendChatMessage()` (novo padrão)
- `SearchResponse` ainda existe para compatibilidade (deprecated)

---

**Data de Implementação**: 11 de Agosto de 2026  
**Arquivos Modificados**: 2
- `Models.kt` - Adicionado `orderConfirmed`
- `SearchViewModel.kt` - Lógica de checkout automático

**Linhas Adicionadas**: ~90

