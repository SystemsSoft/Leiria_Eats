package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    isLoading: Boolean,
    selectedOrder: Order? = null,
    onRefresh: () -> Unit = {},
    onOrderClick: (Order) -> Unit = {},
    onBackToList: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp)
    ) {
        if (selectedOrder != null) {
            // --- TELA DE DETALHES DO PEDIDO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToList,
                    modifier = Modifier.background(Color(0xFF424242), CircleShape).size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Detalhes do Pedido",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ID: ${selectedOrder.id}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    Text("Status: ${selectedOrder.status}", color = Color.White, modifier = Modifier.padding(top = 4.dp))
                    
                    HorizontalDivider(color = Color(0xFF424242), modifier = Modifier.padding(vertical = 16.dp))
                    
                    Text("Itens:", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    selectedOrder.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x ${item.product_name}", color = Color.White, modifier = Modifier.weight(1f))
                        }
                        if (!item.observation.isNullOrBlank()) {
                            Text(
                                text = "Obs: ${item.observation}",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = Color(0xFF424242), modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("R$ ${selectedOrder.total}0", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        } else {
            // --- TELA DE LISTA DE PEDIDOS ---
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

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFFFD700), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && orders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                }
            } else if (orders.isEmpty()) {
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
                    items(orders.reversed()) { order ->
                        OrderItemCard(order, onClick = { onOrderClick(order) })
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Order, onClick: () -> Unit) {
    val statusColor = getStatusColor(order.status)
    val statusIcon = getStatusIcon(order.status)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333333)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
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

            val summary = order.items.joinToString(", ") { "${it.quantity}x ${it.product_name}" }

            Text(text = summary, color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)

            HorizontalDivider(color = Color(0xFF424242), modifier = Modifier.padding(vertical = 12.dp))

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
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "Pendente" -> Color(0xFFFF9800)
        "Em Preparo" -> Color(0xFF2196F3)
        "Saiu para Entrega" -> Color(0xFF9C27B0)
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