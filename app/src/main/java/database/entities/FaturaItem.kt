package database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "fatura_itens",
    foreignKeys = [
        ForeignKey(
            entity = Fatura::class,
            parentColumns = ["id"],
            childColumns = ["faturaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Artigo::class,
            parentColumns = ["id"],
            childColumns = ["artigoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FaturaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faturaId: Long,
    val artigoId: Long,
    val quantidade: Int,
    val preco: Double,
    val clienteId: Long?
) 