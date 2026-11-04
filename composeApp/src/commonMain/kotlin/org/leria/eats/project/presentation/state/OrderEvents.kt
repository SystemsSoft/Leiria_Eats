package org.leria.eats.project.presentation.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Avisos do checkout para a aba Pedidos. */
class OrderEvents {
    private val _orderPlaced = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)

    /** Um pedido foi pago; `true` = acompanhar o status de perto por alguns segundos. */
    val orderPlaced: SharedFlow<Boolean> = _orderPlaced.asSharedFlow()

    fun orderPlaced(followStatus: Boolean) {
        _orderPlaced.tryEmit(followStatus)
    }
}
