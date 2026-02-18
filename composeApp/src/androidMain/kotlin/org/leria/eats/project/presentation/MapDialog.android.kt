package org.leria.eats.project.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun MapDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit
) {
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val leiria = LatLng(39.7436, -8.8071) // Coordenadas de Leiria
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(leiria, 13f)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            selectedLocation?.let {
                                onLocationSelected(it.latitude, it.longitude)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirmar Localização")
                    }
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            selectedLocation = latLng
                        }
                    ) {
                        selectedLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Local Selecionado"
                            )
                        }
                    }
                    if (selectedLocation == null) {
                        Text(
                            "Toque no mapa para selecionar um endereço",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}