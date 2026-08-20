package org.leria.eats.project.presentation
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
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
import org.leria.eats.project.data.SubOrder
import org.leria.eats.project.presentation.components.QRCodeView
import org.leria.eats.project.presentation.util.formatCurrency
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val ODeepBg    = KomaBg
private val OSurface   = KomaSurface
private val OCard      = KomaCard
private val OGold      = KomaGold
private val OGreen     = KomaBrandGreen
private val OAmber     = KomaGoldDark
private val OText      = KomaTextPrimary
private val OMuted     = KomaTextSec

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
    orderItemRatings: Map<String, Int> = emptyMap(),
    onRateItem: (orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) -> Unit = { _, _, _, _, _ -> },
    onMarkDelivered: (orderId: String) -> Unit = {}
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
                OrderDetailView(
                    order = selectedOrder,
                    onBack = onBackToList,
                    orderItemRatings = orderItemRatings,
                    onRateItem = onRateItem,
                    onMarkDelivered = onMarkDelivered
                )
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
fun OrderDetailView(
    order: Order,
    onBack: () -> Unit,
    orderItemRatings: Map<String, Int> = emptyMap(),
    onRateItem: (orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) -> Unit = { _, _, _, _, _ -> },
    onMarkDelivered: (orderId: String) -> Unit = {}
) {
    val displayStatus = getDisplayStatus(order.status, order.deliveryType)
    var showExpandedQr by remember { mutableStateOf(false) }

    // Calcula o total apenas dos sub-pedidos ativos (não cancelados)
    val activeSubOrdersTotal = remember(order.subOrders) {
        order.subOrders.filter { it.status != "Cancelado" }.sumOf { it.total }
    }
    // Total ajustado: Sub-pedidos ativos + taxas (taxas geralmente são fixas ou reembolsadas parcialmente, mas aqui subtraímos o valor do item cancelado)
    val adjustedGrandTotal = activeSubOrdersTotal + order.totalDeliveryFee + order.totalServiceFee

    // ── Expanded QR Dialog ──────────────────────────────────────────────────
    if (showExpandedQr) {
        AlertDialog(
            onDismissRequest = { showExpandedQr = false },
            containerColor = OCard,
            confirmButton = {
                TextButton(onClick = { showExpandedQr = false }) {
                    Text("Fechar", color = OGold, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Apresente este código ao estafeta",
                        color = OText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        QRCodeView(data = order.trackingCode, size = 216)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        order.trackingCode,
                        color = OGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
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

            // ── Master Order info card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OCard)
                    .border(1.dp, OGold.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: ${order.gid}", color = OMuted, fontSize = 12.sp)
                        val statusColor = getStatusColor(displayStatus)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(displayStatus, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (order.trackingCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OGold.copy(alpha = 0.05f))
                                .border(1.dp, OGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { showExpandedQr = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Código para o estafeta", color = OMuted, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.ZoomIn,
                                            contentDescription = null,
                                            tint = OGold.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(order.trackingCode, color = OGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    QRCodeView(data = order.trackingCode, size = 70)
                                    // Pequeno marcador de expansão
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 4.dp, y = 4.dp)
                                            .size(20.dp)
                                            .background(OGold, CircleShape)
                                            .border(2.dp, OCard, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Fullscreen, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = OGold.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = OGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(order.deliveryAddress, color = OText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Lojas e Itens", color = OText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
        }

        // ── Sub-orders (one per restaurant) ──────────────────────────────────
        items(order.subOrders) { subOrder ->
            SubOrderCard(
                subOrder = subOrder,
                masterOrderId = order.gid,
                orderItemRatings = orderItemRatings,
                onRateItem = onRateItem
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            // ── Final Summary ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(OCard.copy(alpha = 0.6f))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Subtotal dos produtos", formatCurrency(activeSubOrdersTotal))
                    SummaryRow("Total de entrega", formatCurrency(order.totalDeliveryFee))
                    SummaryRow("Taxa de serviço Koma", formatCurrency(order.totalServiceFee))
                    
                    HorizontalDivider(color = OGold.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pago", color = OText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(formatCurrency(adjustedGrandTotal), color = OGold, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubOrderCard(
    subOrder: SubOrder,
    masterOrderId: String,
    orderItemRatings: Map<String, Int>,
    onRateItem: (orderId: String, productGid: String, restaurantGid: String, productName: String, rating: Int) -> Unit
) {
    val subStatus = getDisplayStatus(subOrder.status, null)
    val isCancelled = subOrder.status == "Cancelado"
    val statusColor = getStatusColor(subStatus)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCancelled) OCard.copy(alpha = 0.5f) else OCard)
            .border(
                width = 1.dp,
                color = if (isCancelled) KomaSoftRed.copy(alpha = 0.4f) else OGold.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OSurface)
                ) {
                    KamelImage(
                        asyncPainterResource(subOrder.restaurantImageUrl ?: ""),
                        contentDescription = subOrder.restaurantName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(subOrder.restaurantName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isCancelled) OMuted else OText)
                    Text(subOrder.restaurantCategory, fontSize = 11.sp, color = OMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(subStatus, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isCancelled) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KomaSoftRed.copy(alpha = 0.1f))
                        .border(1.dp, KomaSoftRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = KomaSoftRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Este item foi cancelado por solicitação do restaurante. O reembolso já está a ser processado para o seu método de pagamento.",
                            color = KomaSoftRed,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            subOrder.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("x${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OGold, modifier = Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.product_name, fontSize = 13.sp, color = OText, fontWeight = FontWeight.Medium)
                        if (item.observation?.isNotBlank() == true) {
                            Text("Obs: ${item.observation}", fontSize = 11.sp, color = OAmber)
                        }
                    }
                    Text(formatCurrency(item.price * item.quantity), fontSize = 13.sp, color = OGreen)
                }
                
                // Rating for each item if delivered
                if (subOrder.status == "Entregue") {
                    val ratingKey = "$masterOrderId::${item.product_name}"
                    val savedRating = orderItemRatings[ratingKey]
                    OrderItemRatingRow(
                        currentRating = savedRating,
                        onRatingSelected = { stars ->
                            onRateItem(masterOrderId, item.productGid, subOrder.restaurantGid, item.product_name, stars)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            if (subOrder.driverName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OGreen.copy(alpha = 0.05f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeliveryDining, null, tint = OGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Estafeta: ${subOrder.driverName}", fontSize = 12.sp, color = OGreen, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OMuted, fontSize = 13.sp)
        Text(value, color = OText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrderItemCard(
    order: Order,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val displayStatus = getDisplayStatus(order.status, order.deliveryType)
    val statusColor = getStatusColor(displayStatus)
    val statusIcon = getStatusIcon(displayStatus)

    // Calcula o total ajustado para o card da lista
    val adjustedTotal = remember(order) {
        val activeSubTotal = order.subOrders.filter { it.status != "Cancelado" }.sumOf { it.total }
        activeSubTotal + order.totalDeliveryFee + order.totalServiceFee
    }

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
                // Image from the first restaurant
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OSurface)
                ) {
                    KamelImage(
                        asyncPainterResource(order.subOrders.firstOrNull()?.restaurantImageUrl ?: ""),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val restaurantNames = order.subOrders.joinToString(", ") { it.restaurantName }
                    Text(restaurantNames, color = OText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${order.subOrders.size} restaurante(s)", color = OMuted, fontSize = 11.sp)
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

            val totalItems = order.subOrders.sumOf { it.items.sumOf { item -> item.quantity } }
            Text(text = "$totalItems itens no pedido", color = OMuted, fontSize = 12.sp)

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
                    Text(displayStatus, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(formatCurrency(adjustedTotal), color = OGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun OrderItemRatingRow(
    currentRating: Int?,
    onRatingSelected: (Int) -> Unit
) {
    val isRated = currentRating != null && currentRating > 0
    var hoveredStar by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isRated) OGreen.copy(alpha = 0.07f) else OGold.copy(alpha = 0.07f))
            .border(
                1.dp,
                if (isRated) OGreen.copy(alpha = 0.3f) else OGold.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = if (isRated) "✅ A sua avaliação" else "⭐ Avalie este produto",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isRated) OGreen else OGold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..5).forEach { star ->
                    val isFilled = star <= (currentRating ?: hoveredStar)
                    val scale by animateFloatAsState(
                        targetValue = if (isFilled) 1.25f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "star$star"
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "$star estrelas",
                        tint = if (isFilled) OGold else OMuted.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size((22 * scale).dp)
                            .clickable { onRatingSelected(star) }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (currentRating) {
                        1 -> "😞 Mau"
                        2 -> "😐 Regular"
                        3 -> "🙂 Razoável"
                        4 -> "😊 Bom"
                        5 -> "🤩 Excelente!"
                        else -> "Toque para avaliar"
                    },
                    fontSize = 12.sp,
                    color = if (isRated) OGreen else OMuted.copy(alpha = 0.6f),
                    fontWeight = if (isRated) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        "Pendente"              -> KomaStatusPendente
        "Em Preparo"            -> KomaStatusPreparo
        "estafeta chegou ao seu endereco"     -> KomaStatusEntrega
        "Entregue"              -> KomaStatusEntregue
        "Pronto para Recolha"   -> KomaStatusRecolha
        "Cancelado"             -> KomaStatusCancelado
        else                    -> KomaStatusDefault
    }
}

private fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "Pendente"              -> Icons.Default.HourglassEmpty
        "Em Preparo"            -> Icons.Default.Restaurant
        "estafeta chegou ao seu endereco"     -> Icons.Default.DeliveryDining
        "Entregue"              -> Icons.Default.CheckCircle
        "Pronto para Recolha"   -> Icons.Default.StoreMallDirectory
        "Cancelado"             -> Icons.Default.Close
        else                    -> Icons.Default.Info
    }
}

/** Returns the status label to show in the UI.
 *  For pickup orders, "Entregue" is displayed as "Pronto para Recolha". */
private fun getDisplayStatus(status: String, deliveryType: String?): String =
    if (deliveryType == "pickup" && status == "Entregue") "Pronto para Recolha"
    else status
