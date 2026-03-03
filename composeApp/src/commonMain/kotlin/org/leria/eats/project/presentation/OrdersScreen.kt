package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.leria.eats.project.data.Order
import org.leria.eats.project.presentation.util.formatCurrency

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val ODeepBg    = Color(0xFF061510)
private val OSurface   = Color(0xFF0A2218)
private val OCard      = Color(0xFF0E2E20)
private val OGold      = Color(0xFFFFC107)
private val OGreen     = Color(0xFF4ADE80)
private val OAmber     = Color(0xFFFFD54F)
private val OText      = Color(0xFFF0FDF4)
private val OMuted     = Color(0xFF6EE7A0)

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ODeepBg)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(OGold.copy(alpha = 0.04f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 8.dp)
        ) {
            if (selectedOrder != null) {
                OrderDetailView(order = selectedOrder, onBack = onBackToList)
            } else {
                // ── Header ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.radialGradient(listOf(OGold.copy(alpha = 0.25f), Color.Transparent)),
                                    CircleShape
                                )
                                .border(1.dp, OGold.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, tint = OGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Meus Pedidos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OText)
                            Text("${orders.size} pedido(s)", fontSize = 11.sp, color = OMuted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = OGold, strokeWidth = 2.dp)
                        } else {
                            // Filter button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isFiltered) OGold.copy(alpha = 0.2f) else OCard)
                                    .border(1.dp, if (isFiltered) OGold.copy(alpha = 0.6f) else OMuted.copy(alpha = 0.2f), CircleShape)
                                    .clickable { onFilterToggle() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = if (isFiltered) OGold else OMuted, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Refresh button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OCard)
                                    .border(1.dp, OMuted.copy(alpha = 0.2f), CircleShape)
                                    .clickable { onRefresh() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = OMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // ── Lista ─────────────────────────────────────────────────
                if (isLoading && orders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OGold)
                    }
                } else if (orders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(OCard, CircleShape)
                                    .border(1.dp, OGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.List, contentDescription = null, tint = OMuted, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Nenhum pedido realizado ainda.", color = OText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Os seus pedidos aparecem aqui.", color = OMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    val filteredOrders = if (isFiltered) orders.filter { it.status == "Entregue" } else orders
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
}

@Composable
fun OrderDetailView(order: Order, onBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // ── Back header ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OCard)
                        .border(1.dp, OGold.copy(alpha = 0.3f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = OText, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Detalhes do Pedido", style = MaterialTheme.typography.titleLarge, color = OText, fontWeight = FontWeight.Bold)
            }

            // ── Order card ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OCard)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(OGold.copy(alpha = 0.4f), OGreen.copy(alpha = 0.3f))),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: ${order.id}", color = OGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val statusColor = getStatusColor(order.status)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(order.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = OGold.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 14.dp))

                    Text("Itens do Pedido", color = OText, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))

                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(OSurface)
                            ) {
                                KamelImage(
                                    asyncPainterResource(item.imageUrl),
                                    contentDescription = item.product_name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.description, fontSize = 11.sp, color = OMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatCurrency(item.price), fontWeight = FontWeight.Bold, color = OGreen, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OGold.copy(alpha = 0.15f))
                                    .border(1.dp, OGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("x${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OGold)
                            }
                        }
                    }

                    HorizontalDivider(color = OGold.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", color = OText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(formatCurrency(order.total), color = OGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OCard)
            .border(1.dp, OGold.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OSurface)
                ) {
                    KamelImage(
                        asyncPainterResource(order.restaurantImageUrl),
                        contentDescription = order.restaurantName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.restaurantName, color = OText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(order.restaurantCategory, color = OMuted, fontSize = 11.sp, maxLines = 1)
                    Text(order.deliveryAddress, color = OMuted.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (order.isFavorite) OGold.copy(alpha = 0.15f) else OCard)
                        .border(1.dp, if (order.isFavorite) OGold.copy(alpha = 0.5f) else OMuted.copy(alpha = 0.2f), CircleShape)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (order.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favoritar",
                        tint = if (order.isFavorite) OGold else OMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val summary = order.items.joinToString(" · ") { "${it.quantity}× ${it.product_name}" }
            Text(text = summary, color = OMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            HorizontalDivider(color = OGold.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(order.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(formatCurrency(order.total), color = OGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        "Pendente"          -> Color(0xFFFFC107)
        "Em Preparo"        -> Color(0xFF60A5FA)
        "Saiu para Entrega" -> Color(0xFF4ADE80)
        "Entregue"          -> Color(0xFFFFD54F)
        "Cancelado"         -> Color(0xFFF87171)
        else                -> Color(0xFF6EE7A0)
    }
}

private fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "Pendente"          -> Icons.Default.HourglassEmpty
        "Em Preparo"        -> Icons.Default.Restaurant
        "Saiu para Entrega" -> Icons.Default.DeliveryDining
        "Entregue"          -> Icons.Default.CheckCircle
        "Cancelado"         -> Icons.Default.Close
        else                -> Icons.Default.Info
    }
}