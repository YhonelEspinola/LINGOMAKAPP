package com.lingomak.lingomakapp.data.model

data class ReporteIAData(
    val sintoma: String = "",
    val causa: String = "",
    val acciones: String = "",
    val resultado: String = ""
) {
    fun aJson(): String {
        return com.google.gson.Gson().toJson(this)
    }

    companion object {
        fun desdeJson(json: String?): ReporteIAData? {
            if (json.isNullOrEmpty()) return null
            return try {
                com.google.gson.Gson().fromJson(json, ReporteIAData::class.java)
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
