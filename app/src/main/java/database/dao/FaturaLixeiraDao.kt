package database.dao

import androidx.room.*
import database.entities.FaturaLixeira
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaLixeiraDao {
    @Query("SELECT * FROM faturas_lixeira ORDER BY data DESC")
    fun getAllFaturasLixeira(): Flow<List<FaturaLixeira>>

    @Query("SELECT * FROM faturas_lixeira WHERE id = :id")
    suspend fun getFaturaLixeiraById(id: Long): FaturaLixeira?

    @Query("SELECT * FROM faturas_lixeira WHERE numeroFatura LIKE '%' || :searchQuery || '%' OR cliente LIKE '%' || :searchQuery || '%'")
    fun searchFaturasLixeira(searchQuery: String): Flow<List<FaturaLixeira>>

    @Query("SELECT * FROM faturas_lixeira WHERE data BETWEEN :startDate AND :endDate")
    fun getFaturasLixeiraByDateRange(startDate: String, endDate: String): Flow<List<FaturaLixeira>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaLixeira(faturaLixeira: FaturaLixeira): Long

    @Update
    suspend fun updateFaturaLixeira(faturaLixeira: FaturaLixeira)

    @Delete
    suspend fun deleteFaturaLixeira(faturaLixeira: FaturaLixeira)

    @Query("DELETE FROM faturas_lixeira WHERE id = :id")
    suspend fun deleteFaturaLixeiraById(id: Long)

    @Query("SELECT COUNT(*) FROM faturas_lixeira")
    suspend fun getFaturaLixeiraCount(): Int

    @Query("DELETE FROM faturas_lixeira")
    suspend fun deleteAllFaturasLixeira()
} 