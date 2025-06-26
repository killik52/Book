package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fatura_notas_lixeira")
data class FaturaNotaLixeira(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faturaLixeiraId: Long,
    val nota: String
) 