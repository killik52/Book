package database.dao

import androidx.room.*
import database.entities.FaturaItemLixeira

@Dao
interface FaturaItemLixeiraDao {
    @Query("SELECT * FROM fatura_itens_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun getItensByFaturaLixeiraId(faturaLixeiraId: Long): List<FaturaItemLixeira>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(itens: List<FaturaItemLixeira>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FaturaItemLixeira): Long

    @Delete
    suspend fun delete(item: FaturaItemLixeira)

    @Query("DELETE FROM fatura_itens_lixeira WHERE faturaLixeiraId = :faturaLixeiraId")
    suspend fun deleteByFaturaLixeiraId(faturaLixeiraId: Long)

    @Query("DELETE FROM fatura_itens_lixeira")
    suspend fun deleteAllFaturaItensLixeira()
} 