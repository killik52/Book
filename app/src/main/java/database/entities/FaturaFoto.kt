package database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "fatura_fotos",
    foreignKeys = [
        ForeignKey(
            entity = Fatura::class,
            parentColumns = ["id"],
            childColumns = ["faturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FaturaFoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faturaId: Long,
    val photoPath: String
) 