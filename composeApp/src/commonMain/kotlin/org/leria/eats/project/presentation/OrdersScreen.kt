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
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.leria.eats.project.data.Order

@Composable
fun OrdersScreen(
    orders: List<Order>,
    isLoading: Boolean,
    selectedOrder: Order? = null,
    onRefresh: () -> Unit = {},
    onOrderClick: (Order) -> Unit = {},
    onBackToList: () -> Unit = {},
    onToggleFavorite: (Order) -> Unit,
    isFiltered: Boolean,
    onFilterToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        if (selectedOrder != null) {
            OrderDetailView(order = selectedOrder, onBack = onBackToList)
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    Row {
                        IconButton(onClick = onFilterToggle) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filtrar por entregues",
                                tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && orders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhum pedido realizado ainda.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val filteredOrders = if (isFiltered) orders.filter { it.status == "Entregue" } else orders
                    items(filteredOrders) { order ->
                        OrderItemCard(
                            order = order,
                            onClick = { onOrderClick(order) },
                            onToggleFavorite = { onToggleFavorite(order) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailView(order: Order, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape).size(36.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Detalhes do Pedido",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ID: ${order.id}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Status: ${order.status}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(vertical = 16.dp))

            Text("Itens:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KamelImage(
                        asyncPainterResource(item.imageUrl),
                        contentDescription = item.product_name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.product_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "R$ ${item.price}0",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "x${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("R$ ${order.total}0", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val statusColor = getStatusColor(order.status)
    val statusIcon = getStatusIcon(order.status)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KamelImage(
                    asyncPainterResource(order.restaurantImageUrl),
                    contentDescription = order.restaurantName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.restaurantName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = order.restaurantCategory,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = order.deliveryAddress,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (order.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favoritar",
                        tint = if (order.isFavorite) Color.Yellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val summary = order.items.joinToString(", ") { "${it.quantity}x ${it.product_name}" }

            Text(text = summary, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp, lineHeight = 20.sp)

            HorizontalDivider(color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(vertical = 12.dp))

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
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        "Pendente" -> MaterialTheme.colorScheme.primary
        "Em Preparo" -> Color(0xFF64B5F6) // Light Blue
        "Saiu para Entrega" -> Color(0xFF81C784) // Light Green
        "Entregue" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        "Cancelado" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
}

private fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "Pendente" -> Icons.Default.HourglassEmpty
        "Em Preparo" -> Icons.Default.Restaurant
        "Saiu para Entrega" -> Icons.Default.DeliveryDining
        "Entregue" -> Icons.Default.CheckCircle
        "Cancelado" -> Icons.Default.Close
        else -> Icons.Default.Info
    }
}