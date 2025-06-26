package database.dao

import androidx.room.*
import database.entities.FaturaNota
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaNotaDao {
    @Query("SELECT * FROM fatura_notas WHERE faturaId = :faturaId")
    fun getFaturaNotasByFaturaId(faturaId: Long): Flow<List<FaturaNota>>

    @Query("SELECT * FROM fatura_notas WHERE id = :id")
    suspend fun getFaturaNotaById(id: Long): FaturaNota?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaNota(faturaNota: FaturaNota): Long

    @Update
    suspend fun updateFaturaNota(faturaNota: FaturaNota)

    @Delete
    suspend fun deleteFaturaNota(faturaNota: FaturaNota)

    @Query("DELETE FROM fatura_notas WHERE faturaId = :faturaId")
    suspend fun deleteFaturaNotasByFaturaId(faturaId: Long)

    @Query("DELETE FROM fatura_notas WHERE id = :id")
    suspend fun deleteFaturaNotaById(id: Long)

    @Query("DELETE FROM fatura_notas")
    suspend fun deleteAllFaturaNotas()
} 