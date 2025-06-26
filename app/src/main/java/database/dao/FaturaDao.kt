package database.dao

import androidx.room.*
import database.entities.Fatura
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaDao {
    @Query("SELECT * FROM faturas ORDER BY id DESC")
    fun getAllFaturas(): Flow<List<Fatura>>

    @Query("SELECT * FROM faturas WHERE id = :id")
    fun getFaturaById(id: Long): Flow<Fatura?>

    @Query("""
        SELECT * FROM faturas 
        WHERE numeroFatura LIKE '%' || :searchQuery || '%' 
        OR cliente LIKE '%' || :searchQuery || '%'
        OR artigos LIKE '%' || :searchQuery || '%'
        ORDER BY id DESC
    """)
    fun searchFaturas(searchQuery: String): Flow<List<Fatura>>

    @Query("""
        SELECT DISTINCT f.* FROM faturas f
        LEFT JOIN clientes c ON f.cliente = c.nome
        WHERE f.numeroFatura LIKE '%' || :searchQuery || '%'
        OR f.cliente LIKE '%' || :searchQuery || '%'
        OR f.artigos LIKE '%' || :searchQuery || '%'
        OR c.cpf LIKE '%' || :searchQuery || '%'
        OR c.cnpj LIKE '%' || :searchQuery || '%'
        OR c.telefone LIKE '%' || :searchQuery || '%'
        ORDER BY f.id DESC
    """)
    fun searchFaturasAdvanced(searchQuery: String): Flow<List<Fatura>>

    @Query("SELECT * FROM faturas WHERE foiEnviada = :foiEnviada ORDER BY id DESC")
    fun getFaturasByEnvioStatus(foiEnviada: Boolean): Flow<List<Fatura>>

    @Query("SELECT * FROM faturas WHERE data BETWEEN :startDate AND :endDate ORDER BY id DESC")
    fun getFaturasByDateRange(startDate: String, endDate: String): Flow<List<Fatura>>

    @Query("SELECT SUM(subtotal) FROM faturas WHERE data BETWEEN :startDate AND :endDate")
    fun getTotalFaturasByDateRange(startDate: String, endDate: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFatura(fatura: Fatura): Long

    @Update
    suspend fun updateFatura(fatura: Fatura)

    @Delete
    suspend fun deleteFatura(fatura: Fatura)

    @Query("DELETE FROM faturas WHERE id = :id")
    suspend fun deleteFaturaById(id: Long)

    @Query("DELETE FROM faturas")
    suspend fun deleteAllFaturas()

    @Query("SELECT COUNT(*) FROM faturas")
    fun getFaturaCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM faturas WHERE foiEnviada = 1")
    fun getFaturasEnviadasCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM faturas WHERE foiEnviada = 0")
    fun getFaturasNaoEnviadasCount(): Flow<Int>
} 