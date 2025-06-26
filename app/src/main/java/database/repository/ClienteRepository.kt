package database.repository

import android.util.Log
import androidx.lifecycle.LiveData
import database.dao.ClienteDao
import database.entities.Cliente

class ClienteRepository(
    private val clienteDao: ClienteDao
) {
    fun getAllClientes(): LiveData<List<Cliente>> {
        Log.d("ClienteRepository", "getAllClientes chamado")
        return clienteDao.getAllClientes()
    }
    
    fun getClienteById(id: Long): LiveData<Cliente?> = clienteDao.getClienteById(id)
    
    fun searchClientes(searchQuery: String): LiveData<List<Cliente>> = clienteDao.searchClientes(searchQuery)
    
    fun getClienteByNumeroSerial(numeroSerial: String): LiveData<Cliente?> = clienteDao.getClienteByNumeroSerial(numeroSerial)
    
    suspend fun insertCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    
    suspend fun updateCliente(cliente: Cliente) = clienteDao.updateCliente(cliente)
    
    suspend fun deleteCliente(cliente: Cliente) = clienteDao.deleteCliente(cliente)
    
    suspend fun deleteClienteById(id: Long) = clienteDao.deleteClienteById(id)
    
    fun getClienteCount(): LiveData<Int> = clienteDao.getClienteCount()
    
    fun getRecentClientes(limit: Int): LiveData<List<Cliente>> = clienteDao.getRecentClientes(limit)
} 