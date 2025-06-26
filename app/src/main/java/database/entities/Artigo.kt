package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artigos")
data class Artigo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String?,
    val preco: Double?,
    val quantidade: Int?,
    val desconto: Double?,
    val descricao: String?,
    val guardarFatura: Boolean?,
    val numeroSerial: String?
) 