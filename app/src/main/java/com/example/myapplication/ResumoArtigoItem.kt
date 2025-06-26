package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResumoArtigoItem(
    val nomeArtigo: String,
    val quantidadeVendida: Int, // Propriedade para a quantidade vendida
    val valorTotalVendido: Double, // Propriedade para o valor total vendido do artigo
    val serialNumber: String? = null
) : Parcelable