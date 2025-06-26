package database.dao

import androidx.room.*
import database.entities.FaturaNotaLixeira

@Dao
interface FaturaNotaLixeiraDao {
    @Query("SELECT * FROM fatura_notas_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun getNotasByFaturaLixeiraId(faturaLixeiraId: Long): List<FaturaNotaLixeira>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notas: List<FaturaNotaLixeira>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(nota: FaturaNotaLixeira): Long

    @Delete
    suspend fun delete(nota: FaturaNotaLixeira)

    @Query("DELETE FROM fatura_notas_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun deleteByFaturaLixeiraId(faturaLixeiraId: Long)

    @Query("DELETE FROM fatura_notas_lixeira")
    suspend fun deleteAllFaturaNotasLixeira()
} 