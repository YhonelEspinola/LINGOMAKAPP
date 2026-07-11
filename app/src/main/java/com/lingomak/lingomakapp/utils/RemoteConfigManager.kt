package com.lingomak.lingomakapp.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    fun init() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Valores por defecto
        val defaults = mapOf(
            "limite_busqueda_inventario" to 50,
            "mostrar_banner_promocion" to false,
            "version_minima_soporte" to 1
        )
        remoteConfig.setDefaultsAsync(defaults)
        
        remoteConfig.fetchAndActivate()
    }

    fun getLimiteBusqueda(): Int = remoteConfig.getLong("limite_busqueda_inventario").toInt()
    fun isPromocionActiva(): Boolean = remoteConfig.getBoolean("mostrar_banner_promocion")
}
