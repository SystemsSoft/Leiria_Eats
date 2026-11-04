package org.leria.eats.project.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import komaai.composeapp.generated.resources.Res
import komaai.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.leria.eats.project.theme.KomaGold
import org.leria.eats.project.theme.KomaTextPrimary

/**
 * Topbar compartilhada pelas telas "Explorar" (HomeScreen) e "IA" (AiSearchScreen) —
 * antes existiam duas implementações privadas quase idênticas, uma em cada arquivo.
 *
 * [glowAlpha]/[isListening] controlam o brilho pulsante atrás da logo (usado só na tela de
 * IA, enquanto o microfone está ativo); nas demais telas ficam nos valores padrão e o brilho
 * fica invisível. [scrollBehavior] conecta a barra ao `Modifier.nestedScroll` do `Scaffold`
 * que a contém, para ela recolher/reaparecer ao rolar a lista/chat abaixo — precisa ser
 * criado com `TopAppBarDefaults.enterAlwaysScrollBehavior()` (ou similar) no chamador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTopBar(
    showClearButton: Boolean,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier,
    glowAlpha: Float = 0f,
    isListening: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    compact: Boolean = false,
    onMenuClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        navigationIcon = {

                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMenuClick != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onMenuClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "Koma",
                        modifier = Modifier.size(142.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
                    )
                }

        },
        title = {},
        actions = {
            if (showClearButton) {
                TextButton(
                    onClick = onClearChat,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Limpar",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = KomaTextPrimary
        )
    )
}
