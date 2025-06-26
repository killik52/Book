package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "faturas")
data class Fatura(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numeroFatura: String?,
    val cliente: String?,
    val artigos: String?,
    val subtotal: Double?,
    val desconto: Double?,
    val descontoPercent: Int?,
    val taxaEntrega: Double?,
    val saldoDevedor: Double?,
    val data: String?,
    val fotosImpressora: String?,
    val notas: String?,
    val foiEnviada: Boolean = false
) 