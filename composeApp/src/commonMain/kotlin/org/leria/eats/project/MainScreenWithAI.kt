package org.leria.eats.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.leria.eats.project.permissions.PermissionManager
import org.leria.eats.project.permissions.PermissionStatus

@Composable
fun MainScreenWithAI(permissionManager: PermissionManager) {
    val status by permissionManager.status.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Status do Microfone:",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Exibe o status atual (DEBUG)
        Text(text = status.name, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        when (status) {
            PermissionStatus.GRANTED -> {
                // --- CENÁRIO: PERMISSÃO CONCEDIDA ---
                Text("🎤 Ouvindo... Pode falar!", color = Color.Green)
            }

            PermissionStatus.DENIED -> {
                Text("Precisamos do microfone para a IA funcionar.", color = Color.Red)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { permissionManager.openSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Abrir Configurações")
                }
            }

            PermissionStatus.IDLE -> {
                // --- CENÁRIO: AGUARDANDO AÇÃO ---
                Text("Toque abaixo para ativar o assistente.")
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { permissionManager.askForPermission() }) {
                    Text("Ativar Microfone")
                }
            }
        }
    }
}