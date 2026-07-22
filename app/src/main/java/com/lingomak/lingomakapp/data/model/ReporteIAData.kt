package com.lingomak.lingomakapp.data.model

import org.json.JSONObject

data class ReporteIAData(
    val sintoma: String = "",
    val causa: String = "",
    val acciones: String = "",
    val resultado: String = ""
) {
    fun aJson(): String {
        return try {
            val json = JSONObject()
            json.put("sintoma", sintoma)
            json.put("causa", causa)
            json.put("acciones", acciones)
            json.put("resultado", resultado)
            json.toString()
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        fun desdeJson(jsonStr: String?): ReporteIAData? {
            if (jsonStr.isNullOrEmpty()) return null
            return try {
                val json = JSONObject(jsonStr)
                ReporteIAData(
                    sintoma = json.optString("sintoma", ""),
                    causa = json.optString("causa", ""),
                    acciones = json.optString("acciones", ""),
                    resultado = json.optString("resultado", "")
                )
            } catch (e: Exception) {
                null
            }
        }
        
        fun desdeMapa(map: Map<*, *>?): ReporteIAData? {
            if (map == null) return null
            return ReporteIAData(
                sintoma = map["sintoma"] as? String ?: "",
                causa = map["causa"] as? String ?: "",
                acciones = map["acciones"] as? String ?: "",
                resultado = map["resultado"] as? String ?: ""
            )
        }
    }
}
