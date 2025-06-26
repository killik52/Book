package database.repository

import androidx.lifecycle.LiveData
import database.dao.ClienteBloqueadoDao
import database.entities.ClienteBloqueado

class ClienteBloqueadoRepository(private val clienteBloqueadoDao: ClienteBloqueadoDao) {
    
    val allClientesBloqueados: LiveData<List<ClienteBloqueado>> = clienteBloqueadoDao.getAllClientesBloqueados()
    
    suspend fun insert(clienteBloqueado: ClienteBloqueado) {
        clienteBloqueadoDao.insert(clienteBloqueado)
    }
    
    suspend fun update(clienteBloqueado: ClienteBloqueado) {
        clienteBloqueadoDao.update(clienteBloqueado)
    }
    
    suspend fun delete(clienteBloqueado: ClienteBloqueado) {
        clienteBloqueadoDao.delete(clienteBloqueado)
    }
    
    suspend fun deleteById(id: Long) {
        clienteBloqueadoDao.deleteById(id)
    }
    
    fun getClienteBloqueadoById(id: Long): LiveData<ClienteBloqueado?> {
        return clienteBloqueadoDao.getClienteBloqueadoById(id)
    }
    
    fun getClienteBloqueadoByNumeroSerial(numeroSerial: String): LiveData<ClienteBloqueado?> {
        return clienteBloqueadoDao.getClienteBloqueadoByNumeroSerial(numeroSerial)
    }
} 