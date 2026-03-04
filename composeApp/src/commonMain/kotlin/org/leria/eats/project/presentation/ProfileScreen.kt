package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.UserProfile

// ─── Paleta KOMAAI ────────────────────────────────────────────────────────────
private val PDeepBg  = Color(0xFF061510)
private val PSurface = Color(0xFF0A2218)
private val PCard    = Color(0xFF0E2E20)
private val PGold    = Color(0xFFFFC107)
private val PGreen   = Color(0xFF4ADE80)
private val PAmber   = Color(0xFFFFD54F)
private val PText    = Color(0xFFF0FDF4)
private val PMuted   = Color(0xFF6EE7A0)

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSave: (String, String, List<Address>) -> Unit,
    onGetLocation: ((String) -> Unit) -> Unit,
    onGetAddressFromMap: (Double, Double) -> String?,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addresses by remember { mutableStateOf<List<Address>>(emptyList()) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(userProfile) {
        if (userProfile.name.isNotEmpty()) name = userProfile.name
        if (userProfile.phone.isNotEmpty()) phone = userProfile.phone
        if (userProfile.addresses.isNotEmpty()) addresses = userProfile.addresses
    }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                val selectedAddress = onGetAddressFromMap(lat, long)
                if (selectedAddress != null) {
                    addresses = addresses + Address("Novo Endereço do Mapa", selectedAddress)
                }
                showMapDialog = false
            }
        )
    }

    if (showAddAddressDialog || addressToEdit != null) {
        AddressEntryDialog(
            address = addressToEdit,
            onDismiss = { showAddAddressDialog = false; addressToEdit = null },
            onSave = { newAddress ->
                if (addressToEdit != null) {
                    // If the new address is marked as default, remove default from all others
                    addresses = if (newAddress.isDefault) {
                        addresses.map {
                            if (it == addressToEdit) newAddress
                            else it.copy(isDefault = false)
                        }
                    } else {
                        addresses.map { if (it == addressToEdit) newAddress else it }
                    }
                } else {
                    // If the new address is marked as default, remove default from all existing ones
                    addresses = if (newAddress.isDefault) {
                        addresses.map { it.copy(isDefault = false) } + newAddress
                    } else {
                        addresses + newAddress
                    }
                }
                showAddAddressDialog = false
                addressToEdit = null
            },
            onGetLocation = onGetLocation,
            onGetAddressFromMap = onGetAddressFromMap
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PDeepBg)
    ) {
        // Ambient glow top-right
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(PGold.copy(alpha = 0.05f), CircleShape)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // ── Avatar ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(
                            Brush.radialGradient(listOf(PGold.copy(alpha = 0.25f), Color.Transparent)),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(PGold, PGreen)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .background(PCard, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PMuted, modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Meu Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (name.isNotBlank()) name else "Utilizador KOMAAI",
                    fontSize = 12.sp, color = PMuted
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Section: dados ────────────────────────────────────────
                ProfileSectionLabel(label = "Dados Pessoais")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome Completo",
                    icon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProfileTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Telefone / WhatsApp",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
                Spacer(modifier = Modifier.height(28.dp))

                // ── Section: endereços ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileSectionLabel(label = "Meus Endereços")
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PGold.copy(alpha = 0.15f))
                            .border(1.dp, PGold.copy(alpha = 0.4f), CircleShape)
                            .clickable { showAddAddressDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Endereço", tint = PGold, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (addresses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PCard)
                            .border(1.dp, PGold.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nenhum endereço adicionado ainda.", color = PMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(addresses) { address ->
                    AddressItem(
                        address = address,
                        onEdit = { addressToEdit = it },
                        onDelete = { addresses = addresses - it },
                        onSetDefault = { selectedAddress ->
                            // Remove default from all addresses and set the selected one as default
                            addresses = addresses.map {
                                it.copy(isDefault = it.address == selectedAddress.address && it.name == selectedAddress.name)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // ── Save FAB ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PGold, Color(0xFFE65100))))
                .clickable { onSave(name, phone, addresses) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF061510), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Perfil ✦", color = Color(0xFF061510), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun ProfileSectionLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(PGold, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PText)
    }
}

@Composable
fun AddressItem(
    address: Address,
    onEdit: (Address) -> Unit,
    onDelete: (Address) -> Unit,
    onSetDefault: (Address) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PCard)
            .border(
                1.dp,
                if (address.isDefault) PGold.copy(alpha = 0.4f) else PGold.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PGreen.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, PGreen.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = PGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            address.name,
                            fontWeight = FontWeight.Bold,
                            color = PText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (address.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "PADRÃO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PGold
                                )
                            }
                        }
                    }
                    Text(address.address, color = PMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (!address.isDefault) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(PGold.copy(alpha = 0.1f))
                            .clickable { onSetDefault(address) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Definir como padrão", tint = PGold, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PGold.copy(alpha = 0.1f))
                        .clickable { onEdit(address) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PGold, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF87171).copy(alpha = 0.1f))
                        .clickable { onDelete(address) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color(0xFFF87171), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEntryDialog(
    address: Address?,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit,
    onGetLocation: ((String) -> Unit) -> Unit,
    onGetAddressFromMap: (Double, Double) -> String?,
) {
    var name by remember { mutableStateOf(address?.name ?: "") }
    var addressValue by remember { mutableStateOf(address?.address ?: "") }
    var isDefault by remember { mutableStateOf(address?.isDefault ?: false) }
    var isLocating by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                val selectedAddress = onGetAddressFromMap(lat, long)
                if (selectedAddress != null) addressValue = selectedAddress
                showMapDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PCard,
        titleContentColor = PText,
        textContentColor = PMuted,
        title = {
            Text(
                if (address == null) "Adicionar Endereço" else "Editar Endereço",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome (ex: Casa)"
                )
                Spacer(modifier = Modifier.height(10.dp))
                ProfileTextField(
                    value = addressValue,
                    onValueChange = { addressValue = it },
                    label = "Endereço",
                    trailingContent = {
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (!isLocating) PGold.copy(alpha = 0.15f) else PCard)
                                    .clickable(enabled = !isLocating) {
                                        isLocating = true
                                        onGetLocation { foundAddress ->
                                            isLocating = false
                                            if (foundAddress.isNotEmpty()) addressValue = foundAddress
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLocating)
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PGold, strokeWidth = 2.dp)
                                else
                                    Icon(Icons.Default.Home, contentDescription = "Localização atual", tint = PGold, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PGreen.copy(alpha = 0.15f))
                                    .clickable { showMapDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Map, contentDescription = "Mapa", tint = PGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isDefault = !isDefault }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PGold,
                            uncheckedColor = PMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Definir como endereço padrão",
                        color = PText,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(PGold, Color(0xFFE65100))))
                    .clickable {
                        onSave(Address(
                            name = name.ifEmpty { "Endereço Sem Nome" },
                            address = addressValue,
                            isDefault = isDefault
                        ))
                    }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Guardar", color = Color(0xFF061510), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PMuted)
            }
        }
    )
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PCard)
            .border(1.dp, PGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = PMuted, modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailingContent,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = PText,
                unfocusedTextColor = PText,
                cursorColor = PGold,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedLabelColor = PGold,
                unfocusedLabelColor = PMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
expect fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
)