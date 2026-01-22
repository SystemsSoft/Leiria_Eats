package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Order

@Composable
fun OrdersScreen(
    orders: List<Order>,
    onRefresh: () -> Unit = {}
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meus Pedidos",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhum pedido realizado ainda.", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
                    ) { 
                        Text("Buscar Pedidos", color = Color.White) 
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders.reversed()) { order ->
                    OrderItemCard(order)
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order) {
    val statusColor = getStatusColor(order.status)
    val statusIcon = getStatusIcon(order.status)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    color = Color(0xFF424242),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Ver detalhes",
                        color = Color(0xFFBDBDBD),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val summary = order.items.groupingBy { it.name }.eachCount()
                .entries.joinToString(", ") { "${it.value}x ${it.key}" }

            Text(text = summary, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)

            Divider(color = Color(0xFF424242), modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "R$ ${order.total}0",
                    color = Color(0xFFFFD700), // Dourado
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "Pendente" -> Color(0xFFFFB300)
        "Em Preparo" -> Color(0xFFBDBDBD)
        "Saiu para Entrega" -> Color(0xFF757575)
        "Entregue" -> Color(0xFF4CAF50)
        "Cancelado" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "Pendente" -> Icons.Default.HourglassEmpty
        "Em Preparo" -> Icons.Default.Restaurant
        "Saiu para Entrega" -> Icons.Default.DeliveryDining
        "Entregue" -> Icons.Default.CheckCircle
        "Cancelado" -> Icons.Default.Close
        else -> Icons.Default.CheckCircle
    }
}