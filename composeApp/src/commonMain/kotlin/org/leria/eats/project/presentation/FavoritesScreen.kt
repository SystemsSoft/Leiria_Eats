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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Order
import org.leria.eats.project.presentation.util.formatCurrency
import org.leria.eats.project.theme.*

// ─── Aliases locais → paleta central ─────────────────────────────────────────
private val FDeepBg = KomaBg
private val FCard   = KomaCard
private val FGold   = KomaGold
private val FText   = KomaTextPrimary
private val FMuted  = KomaTextSec
@Composable
fun FavoritesScreen(
    orders: List<Order>,
    selectedOrder: Order?,
    onOrderClick: (Order) -> Unit,
    onToggleFavorite: (Order) -> Unit,
    onBackToList: () -> Unit,
    onSetNickname: (orderId: String, nickname: String) -> Unit = { _, _ -> }
) {
    var editingOrder by remember { mutableStateOf<Order?>(null) }
    var nicknameInput by remember { mutableStateOf("") }
    if (editingOrder != null) {
        AlertDialog(
            onDismissRequest = { editingOrder = null },
            containerColor = FCard,
            titleContentColor = FText,
            textContentColor = FText,
            title = {
                Text("Apelido do Favorito", fontWeight = FontWeight.Bold, color = FGold)
            },
            text = {
                Column {
                    Text(
                        "Dê um apelido ao pedido de ${editingOrder!!.restaurantName} para o identificar facilmente.",
                        fontSize = 12.sp,
                        color = FMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        placeholder = { Text("Ex: Jantar em família", color = FMuted.copy(alpha = 0.6f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FGold,
                            unfocusedBorderColor = FMuted.copy(alpha = 0.3f),
                            focusedTextColor = FText,
                            unfocusedTextColor = FText,
                            cursorColor = FGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingOrder?.let { order -> onSetNickname(order.id, nicknameInput.trim()) }
                    editingOrder = null
                }) {
                    Text("Guardar", color = FGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingOrder = null }) {
                    Text("Cancelar", color = FMuted)
                }
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FDeepBg)
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(FGold.copy(alpha = 0.04f), CircleShape)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(listOf(FGold.copy(alpha = 0.25f), Color.Transparent)),
                                CircleShape
                            )
                            .border(1.dp, FGold.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = FGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Pedidos Favoritos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FText)
                        Text("${orders.size} favorito(s)", fontSize = 11.sp, color = FMuted)
                    }
                }
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(FCard, CircleShape)
                                    .border(1.dp, FGold.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = FMuted, modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Nenhum favorito ainda.", color = FText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Toque ⭐ num pedido para guardar aqui.", color = FMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(14.dp).background(FGold, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Os seus favoritos", color = FMuted, fontSize = 12.sp)
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(orders.reversed()) { order ->
                            FavoriteOrderCard(
                                order = order,
                                onClick = { onOrderClick(order) },
                                onToggleFavorite = { onToggleFavorite(order) },
                                onEditNickname = {
                                    nicknameInput = order.nickname
                                    editingOrder = order
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun FavoriteOrderCard(
    order: Order,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditNickname: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FCard)
            .border(1.dp, FGold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            // Nickname badge (shown when nickname is set)
            if (order.nickname.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FGold.copy(alpha = 0.12f))
                            .border(1.dp, FGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "\u2726 ${order.nickname}",
                            color = FGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FCard)
                            .border(1.dp, FMuted.copy(alpha = 0.25f), CircleShape)
                            .clickable { onEditNickname() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar apelido", tint = FMuted, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.restaurantName, color = FText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(order.restaurantCategory, color = FMuted, fontSize = 11.sp)
                    Text(order.deliveryAddress, color = FMuted.copy(alpha = 0.6f), fontSize = 10.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Edit/add nickname button (only shown when no nickname yet)
                if (order.nickname.isBlank()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(FCard)
                            .border(1.dp, FMuted.copy(alpha = 0.2f), CircleShape)
                            .clickable { onEditNickname() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Adicionar apelido", tint = FMuted, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // Unfavorite button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(FGold.copy(alpha = 0.12f))
                        .border(1.dp, FGold.copy(alpha = 0.4f), CircleShape)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Remover favorito", tint = FGold, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val summary = order.items.joinToString(" · ") { "${it.quantity}x ${it.product_name}" }
            Text(text = summary, color = FMuted, fontSize = 12.sp, maxLines = 1)
            HorizontalDivider(color = FGold.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.date, color = FMuted.copy(alpha = 0.7f), fontSize = 11.sp)
                Text(
                    formatCurrency(order.total),
                    color = FGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}