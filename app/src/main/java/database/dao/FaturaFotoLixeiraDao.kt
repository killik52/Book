package database.dao

import androidx.room.*
import database.entities.FaturaFotoLixeira

@Dao
interface FaturaFotoLixeiraDao {
    @Query("SELECT * FROM fatura_fotos_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun getFotosByFaturaLixeiraId(faturaLixeiraId: Long): List<FaturaFotoLixeira>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fotos: List<FaturaFotoLixeira>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foto: FaturaFotoLixeira): Long

    @Delete
    suspend fun delete(foto: FaturaFotoLixeira)

    @Query("DELETE FROM fatura_fotos_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun deleteByFaturaLixeiraId(faturaLixeiraId: Long)

    @Query("DELETE FROM fatura_fotos_lixeira")
    suspend fun deleteAllFaturaFotosLixeira()
} 