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

@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    cartItems: List<Product>,
    onBack: () -> Unit,
    onAdd: (Product) -> Unit,
    onRemove: (Product) -> Unit,
    onViewCart: () -> Unit
) {
    val totalParams = cartItems.sumOf { it.price }
    val totalCount = cartItems.size
    val goldColor = Color(0xFFFFD700)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = if (totalCount > 0) 80.dp else 0.dp) // Espaço para a barra de sacola se necessário
    ) {
        // --- CABEÇALHO ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color(0xFF424242), CircleShape).size(36.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${restaurant.category} • ⭐ ${restaurant.rating}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDBDBD)
                )
            }
        }

        // --- LISTA DE PRATOS ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Text(
                    "Cardápio",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(restaurant.products) { product ->
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

    // Barra de Sacola Flutuante (DENTRO do espaço mas por cima)
    if (totalCount > 0) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = goldColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable { onViewCart() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ver Sacola ($totalCount)", color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("Total: R$ ${totalParams}0", color = Color.Black.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Finalizar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductItemWithCounter(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(product.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "R$ ${product.price}0", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (quantity > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp).background(Color(0xFF424242), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Text(quantity.toString(), color = Color.White, modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(28.dp).background(Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}