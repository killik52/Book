package database.dao

import androidx.room.*
import database.entities.FaturaItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaItemDao {
    @Query("SELECT * FROM fatura_itens WHERE faturaId = :faturaId")
    fun getFaturaItemsByFaturaId(faturaId: Long): Flow<List<FaturaItem>>

    @Query("SELECT * FROM fatura_itens WHERE id = :id")
    suspend fun getFaturaItemById(id: Long): FaturaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaItem(faturaItem: FaturaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaItems(faturaItems: List<FaturaItem>)

    @Update
    suspend fun updateFaturaItem(faturaItem: FaturaItem)

    @Delete
    suspend fun deleteFaturaItem(faturaItem: FaturaItem)

    @Query("DELETE FROM fatura_itens WHERE faturaId = :faturaId")
    suspend fun deleteFaturaItemsByFaturaId(faturaId: Long)

    @Query("DELETE FROM fatura_itens WHERE id = :id")
    suspend fun deleteFaturaItemById(id: Long)

    @Query("DELETE FROM fatura_itens")
    suspend fun deleteAllFaturaItens()

    @Query("SELECT COUNT(*) FROM fatura_itens WHERE faturaId = :faturaId")
    suspend fun getFaturaItemCountByFaturaId(faturaId: Long): Int
} 