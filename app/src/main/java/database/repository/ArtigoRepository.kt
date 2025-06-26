package database.repository

import database.dao.ArtigoDao
import database.entities.Artigo
import kotlinx.coroutines.flow.Flow

class ArtigoRepository(
    private val artigoDao: ArtigoDao
) {
    fun getAllArtigos(): Flow<List<Artigo>> = artigoDao.getAllArtigos()
    
    suspend fun getArtigoById(id: Long): Artigo? = artigoDao.getArtigoById(id)
    
    fun searchArtigos(searchQuery: String): Flow<List<Artigo>> = artigoDao.searchArtigos(searchQuery)
    
    suspend fun getArtigoByNumeroSerial(numeroSerial: String): Artigo? = artigoDao.getArtigoByNumeroSerial(numeroSerial)
    
    suspend fun insertArtigo(artigo: Artigo): Long = artigoDao.insertArtigo(artigo)
    
    suspend fun updateArtigo(artigo: Artigo) = artigoDao.updateArtigo(artigo)
    
    suspend fun deleteArtigo(artigo: Artigo) = artigoDao.deleteArtigo(artigo)
    
    suspend fun deleteArtigoById(id: Long) = artigoDao.deleteArtigoById(id)
    
    suspend fun getArtigoCount(): Int = artigoDao.getArtigoCount()
    
    fun getRecentArtigos(limit: Int): Flow<List<Artigo>> = artigoDao.getRecentArtigos(limit)
} 