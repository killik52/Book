package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FaturaResumidaItem(
    val id: Long,
    val numeroFatura: String,
    val cliente: String,
    val serialNumbers: List<String?> = emptyList(), // Corrigido para não ser Nullable List, com default
    val saldoDevedor: Double,
    val data: String, // Data formatada para exibição
    val foiEnviada: Boolean
) : Parcelable