package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Order

@Composable
fun OrdersScreen(
    orders: List<Order>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
    ) {
        Text(
            text = "Meus Pedidos",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum pedido realizado ainda.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Inverte para o mais recente aparecer primeiro
                items(orders.reversed()) { order ->
                    OrderItemCard(order)
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido ${order.id}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    color = Color(0xFF0F3460),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = order.date,
                        color = Color(0xFF4CB5F5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resumo dos itens (Ex: "1x Pizza, 2x Cola...")
            val summary = order.items.groupingBy { it.name }.eachCount()
                .entries.joinToString(", ") { "${it.value}x ${it.key}" }

            Text(text = summary, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)

            Divider(color = Color(0xFF0F3460), modifier = Modifier.padding(vertical = 12.dp))

            // Rodapé: Status e Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(order.status, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Text(
                    text = "R$ ${order.total}0",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}