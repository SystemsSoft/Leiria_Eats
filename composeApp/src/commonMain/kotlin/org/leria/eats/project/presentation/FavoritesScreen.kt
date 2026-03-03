package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Order

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val FDeepBg  = Color(0xFF061510)
private val FCard    = Color(0xFF0E2E20)
private val FGold    = Color(0xFFFFC107)
private val FGreen   = Color(0xFF4ADE80)
private val FText    = Color(0xFFF0FDF4)
private val FMuted   = Color(0xFF6EE7A0)

@Composable
fun FavoritesScreen(
    orders: List<Order>,
    selectedOrder: Order?,
    onOrderClick: (Order) -> Unit,
    onToggleFavorite: (Order) -> Unit,
    onBackToList: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FDeepBg)
    ) {
        // Ambient glow
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
                // ── Header ────────────────────────────────────────────────
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

                // ── Lista / empty ─────────────────────────────────────────
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
                    // Section label
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