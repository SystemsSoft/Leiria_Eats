package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.leria.eats.project.data.Address
import org.leria.eats.project.data.UserProfile

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
            onDismiss = {
                showAddAddressDialog = false
                addressToEdit = null
            },
            onSave = { newAddress ->
                if (addressToEdit != null) {
                    addresses = addresses.map { if (it == addressToEdit) newAddress else it }
                } else {
                    addresses = addresses + newAddress
                }
                showAddAddressDialog = false
                addressToEdit = null
            },
            onGetLocation = onGetLocation,
            onGetAddressFromMap = onGetAddressFromMap
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Meu Perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProfileTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nome Completo",
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Telefone / WhatsApp",
            icon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Meus Endereços",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(addresses) { address ->
                AddressItem(
                    address = address,
                    onEdit = { addressToEdit = it },
                    onDelete = { addresses = addresses - it }
                )
            }
            item {
                Button(
                    onClick = { showAddAddressDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Endereço")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Endereço")
                }
            }
        }



        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onSave(name, phone, addresses)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Salvar Dados", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddressItem(address: Address, onEdit: (Address) -> Unit, onDelete: (Address) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(address.name, fontWeight = FontWeight.Bold)
                Text(address.address)
            }
            IconButton(onClick = { onEdit(address) }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Endereço")
            }
            IconButton(onClick = { onDelete(address) }) {
                Icon(Icons.Default.Delete, contentDescription = "Deletar Endereço")
            }
        }
    }
}


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
    var isLocating by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }

    if (showMapDialog) {
        MapDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, long ->
                val selectedAddress = onGetAddressFromMap(lat, long)
                if (selectedAddress != null) {
                    addressValue = selectedAddress
                }
                showMapDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Adicionar Endereço" else "Editar Endereço") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Endereço (ex: Casa)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addressValue,
                    onValueChange = { addressValue = it },
                    label = { Text("Endereço") },
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    isLocating = true
                                    onGetLocation { foundAddress ->
                                        isLocating = false
                                        if (foundAddress.isNotEmpty()) {
                                            addressValue = foundAddress
                                        }
                                    }
                                },
                                enabled = !isLocating
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Home, // Changed to Home as Map icon is for opening map
                                        contentDescription = "Buscar localização atual",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showMapDialog = true } // Added to open MapDialog
                            ) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = "Selecionar no mapa",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(Address(name.ifEmpty { "Endereço Sem Nome" }, addressValue))
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
expect fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
)