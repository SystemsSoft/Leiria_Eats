package org.leria.eats.project.service

import android.content.Context
import org.leria.eats.project.ActivityHolder

class AndroidMapService(
    private val context: Context
) : MapService {



    override fun openMapAndGetAddress(onAddressSelected: (String?) -> Unit) {
        val mainActivity = ActivityHolder.activity as? MapResultHandler
        mainActivity?.launchPlacePicker(onAddressSelected)
    }
}

interface MapResultHandler {
    fun launchPlacePicker(onAddressSelected: (String?) -> Unit)
}
