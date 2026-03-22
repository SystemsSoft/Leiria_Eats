package org.leria.eats.project.presentation.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Restaurant

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = restaurant.isClosed != true) { onClick() }
                .then(if (restaurant.isClosed == true) Modifier.alpha(0.55f) else Modifier),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16213E) // Fundo azul escuro elegante
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. FOTO DO RESTAURANTE (Placeholder colorido por enquanto)
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F3460)), // Cor levemente diferente
                    contentAlignment = Alignment.Center
                ) {
                    // Aqui entraria a imagem real. Por enquanto, usamos a inicial do nome.
                    Text(
                        text = restaurant.name.take(1),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2. INFORMAÇÕES
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Nome
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Categoria • Nota
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = restaurant.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB0B0B0)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Nota",
                            tint = Color(0xFFFFC107), // Amarelo Ouro
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = restaurant.rating.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFC107),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Badge "FECHADO" — visível apenas quando o restaurante está fechado
        if (restaurant.isClosed == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "🔒 FECHADO",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}