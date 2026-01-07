package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Product
import org.leria.eats.project.data.Restaurant
import kotlin.math.round // Import para arredondar se precisar futuramente

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    cartItems: List<Product>, // Recebe o carrinho atual
    onBack: () -> Unit,
    onAdd: (Product) -> Unit,    // Ação de adicionar
    onRemove: (Product) -> Unit  // Ação de remover
) {
    // Calcula totais para exibir na barra inferior
    val totalParams = cartItems.sumOf { it.price }
    val totalCount = cartItems.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            // --- CABEÇALHO ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${restaurant.category} • ⭐ ${restaurant.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CB5F5)
                    )
                }
            }

            Divider(color = Color(0xFF0F3460))

            // --- LISTA DE PRATOS ---
            LazyColumn(
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp // Espaço extra para a barra inferior não cobrir o último item
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Cardápio",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(restaurant.menu) { product ->
                    // Quantos deste produto já estão no carrinho?
                    val qty = cartItems.count { it.name == product.name }

                    ProductItemWithCounter(
                        product = product,
                        quantity = qty,
                        onAdd = { onAdd(product) },
                        onRemove = { onRemove(product) }
                    )
                }
            }
        }

        // --- BARRA INFERIOR (CARRINHO) ---
        // Só aparece se tiver itens
        if (totalCount > 0) {
            Surface(
                color = Color(0xFF16213E), // Fundo escuro leve
                shadowElevation = 10.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE94560)) // Vermelho/Rosa destaque
                        .clickable { /* Ação de Finalizar Pedido Futura */ }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ver Sacola ($totalCount)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            // CORREÇÃO AQUI: Removemos o .format() do Java e usamos interpolação simples
                            // O "0" no final é um truque visual simples para KMM sem libs extras:
                            // Se for 25.0 -> vira 25.00. Se for 25.5 -> vira 25.50
                            Text(
                                text = "Total: R$ ${totalParams}0",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Text("Finalizar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Componente visual do Prato com Contador (+ / -)
@Composable
fun ProductItemWithCounter(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // INFO DO PRODUTO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = product.description,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Truque visual aqui também para manter consistência
                Text(
                    text = "R$ ${product.price}0",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }

            // CONTROLES DE QUANTIDADE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (quantity > 0) {
                    // Botão Menos
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF0F3460), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = quantity.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Botão Mais (Sempre visível)
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF4CB5F5), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}