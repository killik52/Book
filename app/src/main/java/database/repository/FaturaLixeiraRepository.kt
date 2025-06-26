package database.repository

import database.dao.FaturaLixeiraDao
import database.entities.FaturaLixeira
import kotlinx.coroutines.flow.Flow

class FaturaLixeiraRepository(
    private val faturaLixeiraDao: FaturaLixeiraDao
) {
    fun getAllFaturasLixeira(): Flow<List<FaturaLixeira>> = faturaLixeiraDao.getAllFaturasLixeira()
    
    suspend fun getFaturaLixeiraById(id: Long): FaturaLixeira? = faturaLixeiraDao.getFaturaLixeiraById(id)
    
    fun searchFaturasLixeira(searchQuery: String): Flow<List<FaturaLixeira>> = faturaLixeiraDao.searchFaturasLixeira(searchQuery)
    
    fun getFaturasLixeiraByDateRange(startDate: String, endDate: String): Flow<List<FaturaLixeira>> = faturaLixeiraDao.getFaturasLixeiraByDateRange(startDate, endDate)
    
    suspend fun insertFaturaLixeira(faturaLixeira: FaturaLixeira): Long = faturaLixeiraDao.insertFaturaLixeira(faturaLixeira)
    
    suspend fun updateFaturaLixeira(faturaLixeira: FaturaLixeira) = faturaLixeiraDao.updateFaturaLixeira(faturaLixeira)
    
    suspend fun deleteFaturaLixeira(faturaLixeira: FaturaLixeira) = faturaLixeiraDao.deleteFaturaLixeira(faturaLixeira)
    
    suspend fun deleteFaturaLixeiraById(id: Long) = faturaLixeiraDao.deleteFaturaLixeiraById(id)
    
    suspend fun getFaturaLixeiraCount(): Int = faturaLixeiraDao.getFaturaLixeiraCount()
    
    suspend fun deleteAllFaturasLixeira() = faturaLixeiraDao.deleteAllFaturasLixeira()
} 