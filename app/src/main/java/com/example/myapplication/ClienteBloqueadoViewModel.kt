package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import database.entities.ClienteBloqueado
import database.repository.ClienteBloqueadoRepository
import database.AppDatabase
import kotlinx.coroutines.launch

class ClienteBloqueadoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: ClienteBloqueadoRepository
    val allClientesBloqueados: LiveData<List<ClienteBloqueado>>
    
    init {
        val dao = AppDatabase.getDatabase(application).clienteBloqueadoDao()
        repository = ClienteBloqueadoRepository(dao)
        allClientesBloqueados = repository.allClientesBloqueados
    }
    
    fun insert(clienteBloqueado: ClienteBloqueado) = viewModelScope.launch {
        repository.insert(clienteBloqueado)
    }
    
    fun update(clienteBloqueado: ClienteBloqueado) = viewModelScope.launch {
        repository.update(clienteBloqueado)
    }
    
    fun delete(clienteBloqueado: ClienteBloqueado) = viewModelScope.launch {
        repository.delete(clienteBloqueado)
    }
    
    fun deleteById(id: Long) = viewModelScope.launch {
        repository.deleteById(id)
    }
    
    fun getClienteBloqueadoById(id: Long): LiveData<ClienteBloqueado?> {
        return repository.getClienteBloqueadoById(id)
    }
    
    fun getClienteBloqueadoByNumeroSerial(numeroSerial: String): LiveData<ClienteBloqueado?> {
        return repository.getClienteBloqueadoByNumeroSerial(numeroSerial)
    }
} 