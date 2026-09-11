package com.alarmedechegada.app

data class LocationAlarme(
    val id: Long,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raioMetros: Float,
    val ativo: Boolean
)
