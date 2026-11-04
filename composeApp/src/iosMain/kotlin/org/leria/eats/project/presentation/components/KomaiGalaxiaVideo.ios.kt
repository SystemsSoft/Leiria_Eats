package org.leria.eats.project.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import komaai.composeapp.generated.resources.Res
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndNone
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.currentItem
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.seekToTime
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIColor

/**
 * NOTA: não foi possível validar em aparelho/simulador iOS (esta máquina não tem o Xcode completo) —
 * só a compilação. Se o vídeo não aparecer, conferir se o arquivo composeResources/files/
 * komai_galaxia_k_estatico.mp4 entrou no bundle do app (Res.getUri devolve o caminho dele).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun KomaiGalaxiaVideo(modifier: Modifier) {
    val player = remember {
        val url = NSURL.URLWithString(Res.getUri("files/komai_galaxia_k_estatico.mp4"))
        AVPlayer.playerWithURL(url!!).apply {
            muted = true
            actionAtItemEnd = AVPlayerActionAtItemEndNone // não para no fim: o loop é feito abaixo
        }
    }

    DisposableEffect(player) {
        // Loop: ao chegar no fim volta ao começo e continua tocando.
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player.currentItem,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            player.seekToTime(CMTimeMake(value = 0, timescale = 1))
            player.play()
        }
        player.play()
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            player.pause()
        }
    }

    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = false
                videoGravity = AVLayerVideoGravityResizeAspectFill
                view.backgroundColor = UIColor.clearColor
                view.clipsToBounds = true
            }
        },
        modifier = modifier,
        // O recorte em círculo do Compose não vale para views nativas: arredonda a própria view.
        onResize = { viewController, rect ->
            rect.useContents {
                viewController.view.layer.cornerRadius = minOf(size.width, size.height) / 2.0
            }
        },
    )
}
