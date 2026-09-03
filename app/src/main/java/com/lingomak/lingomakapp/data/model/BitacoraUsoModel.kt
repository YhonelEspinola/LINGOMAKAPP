package com.lingomak.lingomakapp.data.model

data class BitacoraUsoModel(
    val registroUso: RegistroUsoMaquinariaModel,
    val maquinaria: MaquinariaModel?
) {
    // Helper properties for easy access in adapter/card
    val uid: String get() = registroUso.uid
    val fecha: String get() = registroUso.fechaUso
    val tipoMovimiento: String get() = registroUso.tipoMovimiento
    val nombreMaquinaria: String get() = registroUso.nombreMaquinaria
    val marcaMaquinaria: String get() = maquinaria?.marca ?: "--"
    val modeloMaquinaria: String get() = maquinaria?.modelo ?: "--"
    val codigoMaquinaria: String get() = registroUso.codigoMaquinaria
    val trabajoRealizado: String get() = registroUso.trabajoRealizado
    val horometroAnterior: Double get() = registroUso.horometroAnterior
    val horometroFinal: Double get() = registroUso.horometroFinal
    val horasUso: Double get() = registroUso.horasUso
    val operarioNombre: String get() = registroUso.nombreOperario
    
    val obra: String? get() = registroUso.obra
    val contratista: String? get() = registroUso.contratista
    val ubicacion: String? get() = registroUso.ubicacion
    
    val galonesCombustible: Double get() = registroUso.galonesCombustible
    val galonesAceite: Double get() = registroUso.galonesAceite
    val tipoCombustible: String get() = registroUso.tipoCombustible
    val tipoCarga: String get() = registroUso.tipoCarga
}
