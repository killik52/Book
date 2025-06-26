package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fatura_itens_lixeira")
data class FaturaItemLixeira(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faturaLixeiraId: Long,
    val artigoId: Long,
    val quantidade: Int,
    val preco: Double,
    val clienteId: Long?
) 