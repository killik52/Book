package database.dao

import androidx.room.*
import database.entities.FaturaFoto
import kotlinx.coroutines.flow.Flow

@Dao
interface FaturaFotoDao {
    @Query("SELECT * FROM fatura_fotos WHERE faturaId = :faturaId")
    fun getFaturaFotosByFaturaId(faturaId: Long): Flow<List<FaturaFoto>>

    @Query("SELECT * FROM fatura_fotos WHERE id = :id")
    suspend fun getFaturaFotoById(id: Long): FaturaFoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaFoto(faturaFoto: FaturaFoto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaturaFotos(faturaFotos: List<FaturaFoto>)

    @Update
    suspend fun updateFaturaFoto(faturaFoto: FaturaFoto)

    @Delete
    suspend fun deleteFaturaFoto(faturaFoto: FaturaFoto)

    @Query("DELETE FROM fatura_fotos WHERE faturaId = :faturaId")
    suspend fun deleteFaturaFotosByFaturaId(faturaId: Long)

    @Query("DELETE FROM fatura_fotos WHERE id = :id")
    suspend fun deleteFaturaFotoById(id: Long)

    @Query("DELETE FROM fatura_fotos")
    suspend fun deleteAllFaturaFotos()

    @Query("SELECT COUNT(*) FROM fatura_fotos WHERE faturaId = :faturaId")
    suspend fun getFaturaFotoCountByFaturaId(faturaId: Long): Int
} 