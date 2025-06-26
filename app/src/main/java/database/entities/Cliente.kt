package database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String?,
    val email: String?,
    val telefone: String?,
    val informacoesAdicionais: String?,
    val cpf: String?,
    val cnpj: String?,
    val logradouro: String?,
    val numero: String?,
    val complemento: String?,
    val bairro: String?,
    val municipio: String?,
    val uf: String?,
    val cep: String?,
    val numeroSerial: String?
) 