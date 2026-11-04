package org.leria.eats.project.presentation.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.leria.eats.project.R

// Mesma abordagem do VideoSplashScreen: ExoPlayer + PlayerView inflado do layout com TextureView
// (SurfaceView ignora o recorte/alpha do Compose, e aqui o vídeo é recortado em círculo).
@Composable
actual fun KomaiGalaxiaVideo(modifier: Modifier) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.komai_galaxia_k_estatico}")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f // o vídeo não tem áudio; garante que nunca dispute o som com a voz da IA
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.view_splash_player, null) as PlayerView).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // preenche o círculo, sem faixas
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }
    )
}
