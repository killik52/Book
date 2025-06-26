package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fatura_fotos_lixeira")
data class FaturaFotoLixeira(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faturaLixeiraId: Long,
    val photoPath: String
) 