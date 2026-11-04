package org.leria.eats.project.voice.live

import org.leria.eats.project.domain.voice.VoiceAudioOutput

/**
 * Toca os chunks de áudio PCM16 mono 24kHz que a Gemini Live devolve (confirmado
 * empiricamente: mime_type "audio/pcm;rate=24000" — ver gemini_live_bridge.py).
 * Streaming de baixa latência: cada chunk é enfileirado assim que chega, sem
 * esperar a resposta inteira.
 */
expect class LiveAudioPlayer() : VoiceAudioOutput {
    /** Toca [pcm] (já com o reforço de [VoiceBoost] aplicado dentro do player). */
    override fun play(pcm: ByteArray)
    /** Limpa qualquer áudio enfileirado ainda não tocado — usado quando o
     * utilizador interrompe a IA a falar (barge-in). */
    override fun stop()
    override fun release()

    /**
     * Começa uma ligação ao vivo (microfone aberto ao mesmo tempo): a voz passa a sair pelo
     * caminho de CHAMADA do sistema (com cancelamento de eco), no alto-falante, e o volume da
     * chamada é alinhado ao de mídia. Deve ser chamado ANTES de começar a capturar o microfone.
     * Fora de uma ligação (respostas faladas do chat, sacola, onboarding, perfil) a voz toca como
     * mídia, no volume de mídia do aparelho.
     */
    override fun startCall()

    /** Termina a ligação ao vivo e devolve ao sistema o que [startCall] mexeu (modo de áudio,
     * roteamento, volume). Seguro chamar sem [startCall] antes e mais de uma vez. */
    override fun endCall()
}
