package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResumoClienteItem(
    val nomeCliente: String,
    val totalCompras: Double, // Propriedade para o total gasto pelo cliente
    val isBlocked: Boolean? = null // Propriedade para o status de bloqueio (nullable)
) : Parcelable