package org.leria.eats.project.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun WebView(
    modifier: Modifier,
    url: String,
    onSuccess: (orderId: String) -> Unit,
    onCancel: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    // Simple fallback for iOS: open checkout URL in external browser (Safari)
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("O pagamento será aberto no navegador externo.", color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl != null) {
                UIApplication.sharedApplication.openURL(nsUrl)
                // We can't extract order id here, so we simply leave it to the web flow.
                onLoadingChanged(true)
            }
        }) {
            Text("Abrir pagamento")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            onCancel()
        }) {
            Text("Cancelar")
        }
    }
}

