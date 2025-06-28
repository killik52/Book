package database.dao

import androidx.room.*
import database.entities.Artigo
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtigoDao {
    @Query("SELECT * FROM artigos ORDER BY id DESC")
    fun getAllArtigos(): Flow<List<Artigo>>

    @Query("SELECT * FROM artigos WHERE id = :id")
    suspend fun getArtigoById(id: Long): Artigo?

    @Query("SELECT * FROM artigos WHERE nome LIKE '%' || :searchQuery || '%' OR descricao LIKE '%' || :searchQuery || '%'")
    fun searchArtigos(searchQuery: String): Flow<List<Artigo>>

    @Query("SELECT * FROM artigos WHERE numeroSerial = :numeroSerial")
    suspend fun getArtigoByNumeroSerial(numeroSerial: String): Artigo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtigo(artigo: Artigo): Long

    @Update
    suspend fun updateArtigo(artigo: Artigo)

    @Delete
    suspend fun deleteArtigo(artigo: Artigo)

    @Query("DELETE FROM artigos WHERE id = :id")
    suspend fun deleteArtigoById(id: Long)

    @Query("DELETE FROM artigos")
    suspend fun deleteAllArtigos()

    @Query("SELECT COUNT(*) FROM artigos")
    suspend fun getArtigoCount(): Int

    @Query("SELECT * FROM artigos ORDER BY id DESC LIMIT :limit")
    fun getRecentArtigos(limit: Int): Flow<List<Artigo>>
} 