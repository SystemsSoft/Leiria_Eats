# 🔄 Guia de Migração - Busca Semântica → Chat com IA (Kotlin Multiplatform)

## 📋 Visão Geral

Este guia mostra **exatamente o que você precisa fazer** no seu app **Kotlin Multiplatform (KMP)** para substituir a busca semântica antiga pela nova funcionalidade de chat conversacional com IA.

---

## 🎯 O Que Vai Mudar

### **Antes (Busca Semântica)**
- Usuário digita: "pizza calabresa"
- Sistema retorna: Lista técnica de produtos
- Sem conversação

### **Depois (Chat com IA)**
- Usuário digita: "Quero uma pizza"
- IA responde: "Ótimo! Temos 3 pizzas: Margherita (R$ 32)..."
- Conversação natural + produtos

---

## 📝 MUDANÇAS NECESSÁRIAS

### **1. Endpoint da API**
```
ANTES: POST /chat
DEPOIS: POST /chat/sales
```

### **2. Request Body**
```kotlin
// ANTES
{"text": "pizza calabresa"}

// DEPOIS
{
  "message": "Quero uma pizza",
  "restaurant_id": 123,
  "session_id": "abc-123"
}
```

### **3. Response**
```kotlin
// ANTES
{
  "reply": "Encontrei o prato: Pizza Calabresa",
  "productResults": [...]
}

// DEPOIS
{
  "response": "Ótimo! Temos 3 pizzas: Margherita...",
  "products": [...],
  "intent": "product_search"
}
```

---

## ✅ CHECKLIST PASSO A PASSO

### **PASSO 1: Descobrir IP do Servidor**

No terminal do Mac:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

Anote o IP (exemplo: `192.168.1.100`)

---

## 📦 PASSO 2: Dependências (se necessário)

No arquivo `build.gradle.kts` do módulo `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // HTTP Client (Ktor)
            implementation("io.ktor:ktor-client-core:2.3.5")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
            
            // Serialização
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:2.3.5")
        }
        
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.5")
        }
    }
}
```

---

## 🔧 PASSO 3: Atualizar Models (commonMain)

### **3.1. Request Model**

**Arquivo:** `commonMain/kotlin/data/models/ChatRequest.kt`

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ANTES
@Serializable
data class SearchRequest(
    val text: String
)

// DEPOIS
@Serializable
data class ChatRequest(
    val message: String,                    // MUDOU: text → message
    @SerialName("restaurant_id")
    val restaurantId: Int? = null,         // NOVO
    @SerialName("session_id")
    val sessionId: String? = null          // NOVO
)
```

### **3.2. Response Model**

**Arquivo:** `commonMain/kotlin/data/models/ChatResponse.kt`

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ANTES
@Serializable
data class SearchResponse(
    val reply: String,
    @SerialName("productResults")
    val productResults: List<Product>,
    val intent: String
)

// DEPOIS
@Serializable
data class ChatResponse(
    val response: String,                   // MUDOU: reply → response
    val products: List<Product>,            // MUDOU: productResults → products
    val intent: String                      // MANTIDO
)
```

### **3.3. Product Model (mantém igual)**

```kotlin
@Serializable
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String? = null,
    val category: String? = null,
    val quantity: Int = 1
)
```

---

## 🌐 PASSO 4: Atualizar API Client (commonMain)

### **4.1. Configuração do HttpClient**

**Arquivo:** `commonMain/kotlin/data/api/ApiClient.kt`

```kotlin
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    // ANTES
    // private const val BASE_URL = "http://localhost:8000"
    
    // DEPOIS (substituir pelo seu IP local)
    private const val BASE_URL = "http://192.168.1.100:8000"
    
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        // Timeout para IA (pode demorar mais)
        engine {
            requestTimeout = 60_000  // 60 segundos
        }
    }
    
    // Endpoints
    const val CHAT_SALES_ENDPOINT = "$BASE_URL/chat/sales"
    const val CHAT_STATUS_ENDPOINT = "$BASE_URL/chat/status"
}
```

---

## 🔌 PASSO 5: Atualizar Repository/Service (commonMain)

### **5.1. Chat Repository**

**Arquivo:** `commonMain/kotlin/data/repository/ChatRepository.kt`

```kotlin
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ChatRepository {
    private val client = ApiClient.httpClient
    
    // ANTES
    suspend fun search(query: String): SearchResponse {
        return client.post("${ApiClient.BASE_URL}/chat") {
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(text = query))
        }.body()
    }
    
    // DEPOIS
    suspend fun sendMessage(
        message: String,
        restaurantId: Int? = null,
        sessionId: String? = null
    ): Result<ChatResponse> {
        return try {
            val response = client.post(ApiClient.CHAT_SALES_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(ChatRequest(
                    message = message,
                    restaurantId = restaurantId,
                    sessionId = sessionId
                ))
            }
            
            Result.success(response.body<ChatResponse>())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // NOVO: Verificar status do servidor
    suspend fun checkServerStatus(): Result<Boolean> {
        return try {
            val response = client.get(ApiClient.CHAT_STATUS_ENDPOINT)
            val statusData = response.body<Map<String, Any>>()
            Result.success(statusData["status"] == "ready")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🧠 PASSO 6: Criar ViewModel/Presenter (commonMain)

### **6.1. ChatViewModel**

**Arquivo:** `commonMain/kotlin/presentation/chat/ChatViewModel.kt`

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel {
    private val repository = ChatRepository()
    
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private var sessionId: String? = null
    
    init {
        sessionId = generateSessionId()
        addWelcomeMessage()
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }
    
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            text = "Olá! Como posso te ajudar hoje? 😊",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = listOf(welcomeMessage)
    }
    
    fun sendMessage(text: String, restaurantId: Int? = null) {
        if (text.isBlank()) return
        
        // Adiciona mensagem do usuário
        val userMessage = ChatMessage(
            text = text,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage
        
        // Mostra loading
        _uiState.value = ChatUiState.Loading
        
        // Envia para API
        viewModelScope.launch {
            repository.sendMessage(
                message = text,
                restaurantId = restaurantId,
                sessionId = sessionId
            ).fold(
                onSuccess = { response ->
                    // Adiciona resposta da IA
                    val aiMessage = ChatMessage(
                        text = response.response,
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        products = response.products
                    )
                    _messages.value = _messages.value + aiMessage
                    _uiState.value = ChatUiState.Success(response)
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        text = "Desculpe, ocorreu um erro. Tente novamente.",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                    _messages.value = _messages.value + errorMessage
                    _uiState.value = ChatUiState.Error(error.message ?: "Erro desconhecido")
                }
            )
        }
    }
    
    fun checkServerStatus() {
        viewModelScope.launch {
            repository.checkServerStatus().fold(
                onSuccess = { isReady ->
                    println("✅ Servidor está ${if (isReady) "pronto" else "não pronto"}")
                },
                onFailure = { error ->
                    println("❌ Erro ao verificar servidor: ${error.message}")
                }
            )
        }
    }
}

// Estados da UI
sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(val response: ChatResponse) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

// Modelo de mensagem para UI
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val products: List<Product>? = null,
    val isError: Boolean = false
)
```

---

## 🎨 PASSO 7: UI Android (Jetpack Compose)

### **7.1. ChatScreen Android**

**Arquivo:** `androidMain/kotlin/presentation/chat/ChatScreen.kt`

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    restaurantId: Int? = null
) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    
    // Auto-scroll para última mensagem
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Lista de mensagens
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
            
            // Indicador de loading
            if (uiState is ChatUiState.Loading) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IA está pensando...")
                    }
                }
            }
        }
        
        // Input de mensagem
        Surface(
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Digite sua mensagem...") },
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText, restaurantId)
                            messageText = ""
                        }
                    },
                    enabled = uiState !is ChatUiState.Loading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) {
            Alignment.End
        } else {
            Alignment.Start
        }
    ) {
        // Bolha de mensagem
        Surface(
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else if (message.isError) {
                MaterialTheme.colorScheme.error
            } else {
                Color(0xFFE0E0E0)
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // Lista de produtos (se houver)
        message.products?.let { products ->
            if (products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.widthIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    products.forEach { product ->
                        ProductCard(product = product)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall
                )
                product.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "R$ ${String.format("%.2f", product.price)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

## 🍎 PASSO 8: UI iOS (SwiftUI) - Opcional

### **8.1. ChatView iOS**

**Arquivo:** `iosMain/swift/ChatView.swift`

```swift
import SwiftUI

struct ChatView: View {
    @StateObject private var viewModel = ChatViewModel()
    @State private var messageText = ""
    let restaurantId: Int?
    
    var body: some View {
        VStack(spacing: 0) {
            // Lista de mensagens
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.messages) { message in
                            ChatBubbleView(message: message)
                                .id(message.id)
                        }
                        
                        if viewModel.isLoading {
                            HStack {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                                Text("IA está pensando...")
                                    .font(.caption)
                            }
                            .padding()
                        }
                    }
                    .padding()
                }
                .onChange(of: viewModel.messages.count) { _ in
                    if let lastMessage = viewModel.messages.last {
                        withAnimation {
                            proxy.scrollTo(lastMessage.id, anchor: .bottom)
                        }
                    }
                }
            }
            
            // Input de mensagem
            HStack {
                TextField("Digite sua mensagem...", text: $messageText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(viewModel.isLoading)
                
                Button(action: {
                    if !messageText.isEmpty {
                        viewModel.sendMessage(messageText, restaurantId: restaurantId)
                        messageText = ""
                    }
                }) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(.blue)
                }
                .disabled(viewModel.isLoading || messageText.isEmpty)
            }
            .padding()
            .background(Color(.systemBackground))
        }
    }
}

struct ChatBubbleView: View {
    let message: ChatMessage
    
    var body: some View {
        HStack {
            if message.isUser {
                Spacer()
            }
            
            VStack(alignment: message.isUser ? .trailing : .leading, spacing: 8) {
                // Bolha de mensagem
                Text(message.text)
                    .padding(12)
                    .background(message.isUser ? Color.blue : Color(.systemGray5))
                    .foregroundColor(message.isUser ? .white : .black)
                    .cornerRadius(16)
                
                // Produtos (se houver)
                if let products = message.products, !products.isEmpty {
                    ForEach(products, id: \.id) { product in
                        ProductCardView(product: product)
                    }
                }
            }
            
            if !message.isUser {
                Spacer()
            }
        }
    }
}

struct ProductCardView: View {
    let product: Product
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(product.name)
                    .font(.headline)
                if let description = product.description {
                    Text(description)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            }
            Spacer()
            Text("R$ \(String(format: "%.2f", product.price))")
                .font(.title3)
                .foregroundColor(.blue)
        }
        .padding()
        .background(Color.white)
        .cornerRadius(8)
        .shadow(radius: 2)
    }
}
```

---

## 🔍 ONDE PROCURAR NO SEU CÓDIGO

### **Buscar por estas strings:**

```kotlin
// Procurar por:
"/chat"                    // Endpoint antigo
"text"                     // Campo antigo
"reply"                    // Resposta antiga
"productResults"           // Lista antiga
"SearchRequest"            // Model antigo
"SearchResponse"           // Response antigo
```

### **Estrutura de Arquivos Esperada:**

```
shared/
├── commonMain/
│   └── kotlin/
│       ├── data/
│       │   ├── api/
│       │   │   └── ApiClient.kt          ← ATUALIZAR URL
│       │   ├── models/
│       │   │   ├── ChatRequest.kt        ← ATUALIZAR
│       │   │   ├── ChatResponse.kt       ← ATUALIZAR
│       │   │   └── Product.kt
│       │   └── repository/
│       │       └── ChatRepository.kt     ← ATUALIZAR
│       └── presentation/
│           └── chat/
│               └── ChatViewModel.kt      ← CRIAR/ATUALIZAR
├── androidMain/
│   └── kotlin/
│       └── presentation/
│           └── ChatScreen.kt             ← CRIAR/ATUALIZAR
└── iosMain/
    └── swift/
        └── ChatView.swift                ← CRIAR/ATUALIZAR
```

---

## 🧪 PASSO 9: Testar

### **9.1. Teste de Conexão (Debug)**

Adicione temporariamente:

```kotlin
// No init do ViewModel ou em algum botão
fun testConnection() {
    viewModelScope.launch {
        repository.checkServerStatus().fold(
            onSuccess = { isReady ->
                println("✅ Servidor: ${if (isReady) "PRONTO" else "NÃO PRONTO"}")
            },
            onFailure = { error ->
                println("❌ Erro: ${error.message}")
            }
        )
    }
}
```

### **9.2. Teste Simples**

```kotlin
// Enviar mensagem de teste
viewModel.sendMessage("Quero uma pizza", restaurantId = 1)
```

### **9.3. Logs para Debug**

```kotlin
// Adicionar no ChatRepository
suspend fun sendMessage(...): Result<ChatResponse> {
    return try {
        println("📤 Enviando: $message")
        val response = client.post(ApiClient.CHAT_SALES_ENDPOINT) {
            // ...
        }
        println("📥 Resposta: ${response.body<String>()}")
        Result.success(response.body<ChatResponse>())
    } catch (e: Exception) {
        println("❌ Erro: ${e.message}")
        Result.failure(e)
    }
}
```

---

## 🚀 PASSO 10: Rodar Servidor

No terminal do Mac:

```bash
cd /Users/bruno/Documents/Athenna/Koma/KomaServer/LeiriaEatsServer
uvicorn main:app --reload --host 0.0.0.0 --port 8000 --log-level info
```

**Aguarde ver:**
```
✅ E5 carregado!
✅ TinyLlama carregado com sucesso!
🎉 Sistema pronto!
```

---

## 📊 RESUMO DAS MUDANÇAS

| Item | Antes | Depois |
|------|-------|--------|
| **Endpoint** | `/chat` | `/chat/sales` |
| **Request Field** | `text` | `message` |
| **Response Field** | `reply` | `response` |
| **Products Field** | `productResults` | `products` |
| **URL** | `localhost:8000` | `192.168.1.X:8000` |
| **Model Request** | `SearchRequest` | `ChatRequest` |
| **Model Response** | `SearchResponse` | `ChatResponse` |

---

## 🐛 TROUBLESHOOTING

### **Erro: UnresolvedAddressException**
```kotlin
// Verificar:
- ✅ IP está correto
- ✅ Mac e dispositivo na mesma WiFi
- ✅ Servidor rodando com --host 0.0.0.0
```

### **Erro: 404 Not Found**
```kotlin
// Verificar endpoint:
const val CHAT_SALES_ENDPOINT = "$BASE_URL/chat/sales"  // ✅ Correto
// NÃO: "$BASE_URL/chat"  // ❌ Errado
```

### **Timeout**
```kotlin
// Aumentar timeout no HttpClient
engine {
    requestTimeout = 60_000  // 60 segundos (IA pode demorar)
}
```

### **Serialização**
```kotlin
// Verificar @SerialName nos campos
@SerialName("restaurant_id")
val restaurantId: Int? = null
```

---

## ✅ CHECKLIST FINAL

- [ ] Descobrir IP do Mac com `ifconfig`
- [ ] Atualizar `BASE_URL` no `ApiClient.kt`
- [ ] Criar/atualizar `ChatRequest.kt`
- [ ] Criar/atualizar `ChatResponse.kt`
- [ ] Atualizar endpoint `/chat` → `/chat/sales`
- [ ] Atualizar campo `text` → `message`
- [ ] Atualizar campo `reply` → `response`
- [ ] Atualizar campo `productResults` → `products`
- [ ] Criar/atualizar `ChatRepository.kt`
- [ ] Criar/atualizar `ChatViewModel.kt`
- [ ] Implementar UI (Compose/SwiftUI)
- [ ] Testar conexão com `/chat/status`
- [ ] Testar envio de mensagem
- [ ] Verificar se IA responde corretamente

---

## 🎉 PRONTO!

Após fazer essas alterações, seu app KMP estará usando o novo sistema de chat com IA conversacional!

**Teste final:**
1. ✅ Inicie o servidor
2. ✅ Abra o app
3. ✅ Digite: "Quero uma pizza"
4. ✅ Aguarde resposta da IA (~1-2s)
5. ✅ Verifique se produtos aparecem

**Documentação adicional:**
- `QUICK_START.md` - Guia do servidor
- `TINYLLAMA_DOCS.md` - Documentação completa da IA
- `README_IA.md` - Visão geral do sistema

---

**Boa sorte com a migração! 🚀**

