package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Product

@Composable
fun CartScreen(
    cartItems: List<Product>,
    onRemoveItem: (Product) -> Unit,
    onCheckout: () -> Unit
) {
    val total = cartItems.sumOf { it.price }
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Text(
            text = "Minha Sacola",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cartItems.isEmpty()) {
            // Estado Vazio
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Sua sacola está vazia 😔", color = Color.Gray, fontSize = 18.sp)
            }
        } else {
            // Lista de Itens
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { product ->
                    CartItemRow(product, onRemove = { onRemoveItem(product) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resumo do Pedido
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total do Pedido", color = Color.White)
                        Text(
                            "R$ ${total}0",
                            color = Color(0xFFFFD700), // Dourado
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCheckout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD), contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Finalizar Pedido", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(product: Product, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242)),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("R$ ${product.price}0", color = Color(0xFFBDBDBD), fontSize = 14.sp)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Gray)
            }
        }
    }
}