package org.leria.eats.project.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.leria.eats.project.data.UserProfile

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    onSave: (String, String, String) -> Unit,
    onGetLocation: ( (String) -> Unit ) -> Unit
) {
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var phone by remember(userProfile) { mutableStateOf(userProfile.phone) }
    var address by remember(userProfile) { mutableStateOf(userProfile.address) }
    var isLocating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F3460)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF4CB5F5),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Meu Perfil",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
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
            icon = Icons.Default.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Endereço de Entrega", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF4CB5F5)) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        isLocating = true
                        onGetLocation { foundAddress ->
                            isLocating = false
                            if (foundAddress.isNotEmpty()) {
                                address = foundAddress
                            }
                        }
                    },
                    enabled = !isLocating
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFE94560),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Usar localização atual",
                            tint = Color(0xFFE94560)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE94560),
                focusedBorderColor = Color(0xFFE94560),
                unfocusedBorderColor = Color(0xFF0F3460),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        // ---------------------------------------------

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSave(name, phone, address) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Salvar Dados", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF4CB5F5)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFE94560),
            focusedBorderColor = Color(0xFFE94560),
            unfocusedBorderColor = Color(0xFF0F3460),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}