package org.leria.eats.project.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Vídeo da galáxia com o "K" (komai_galaxia_K_estatico.mp4) da tela de ligação de voz: toca em loop,
 * mudo, sem controles, preenchendo o espaço dado (o vídeo é quadrado, 720×720). O recorte em círculo
 * é feito por quem chama com Modifier.clip.
 *
 * O arquivo fica em androidMain/res/raw (Android) e em commonMain/composeResources/files (iOS) — as
 * duas cópias são o mesmo H.264 de 6s; ver KomaiGalaxiaVideo.android.kt e KomaiGalaxiaVideo.ios.kt.
 */
@Composable
expect fun KomaiGalaxiaVideo(modifier: Modifier = Modifier)
