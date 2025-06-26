package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import database.entities.Fatura
import database.entities.FaturaLixeira
import database.repository.FaturaRepository
import database.repository.FaturaLixeiraRepository
import database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FaturaViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: FaturaRepository
    private val faturaLixeiraRepository: FaturaLixeiraRepository
    
    init {
        val dao = AppDatabase.getDatabase(application).faturaDao()
        val lixeiraDao = AppDatabase.getDatabase(application).faturaLixeiraDao()
        repository = FaturaRepository(dao, lixeiraDao)
        faturaLixeiraRepository = FaturaLixeiraRepository(lixeiraDao)
    }
    
    // Função para obter todas as faturas
    fun getAllFaturas(): Flow<List<Fatura>> = repository.getAllFaturas()
    
    // Função para buscar faturas
    fun searchFaturas(query: String): Flow<List<Fatura>> = repository.searchFaturas(query)
    
    // Função para obter faturas por status de envio
    fun getFaturasByEnvioStatus(foiEnviada: Boolean): Flow<List<Fatura>> = repository.getFaturasByEnvioStatus(foiEnviada)
    
    // Função para obter faturas por período
    fun getFaturasByDateRange(startDate: String, endDate: String): Flow<List<Fatura>> = repository.getFaturasByDateRange(startDate, endDate)
    
    // Função para inserir fatura
    fun insertFatura(fatura: Fatura) = viewModelScope.launch {
        repository.insertFatura(fatura)
    }
    
    // Função para atualizar fatura
    fun updateFatura(fatura: Fatura) = viewModelScope.launch {
        repository.updateFatura(fatura)
    }
    
    // Função para deletar fatura
    fun deleteFatura(fatura: Fatura) = viewModelScope.launch {
        repository.deleteFatura(fatura)
    }
    
    // Função para obter fatura por ID
    fun getFaturaById(id: Long): Flow<Fatura?> = repository.getFaturaById(id)
    
    // Função para inserir fatura na lixeira
    fun insertFaturaLixeira(faturaLixeira: FaturaLixeira) = viewModelScope.launch {
        faturaLixeiraRepository.insertFaturaLixeira(faturaLixeira)
    }
    
    // Função para obter contagem de faturas
    fun getFaturaCount(): Flow<Int> = repository.getFaturaCount()
    
    // Função para obter total de faturas por período
    fun getTotalFaturasByDateRange(startDate: String, endDate: String): Flow<Double?> = repository.getTotalFaturasByDateRange(startDate, endDate)
    
    // Função para obter contagem de faturas enviadas
    fun getFaturasEnviadasCount(): Flow<Int> = repository.getFaturasEnviadasCount()
    
    // Função para obter contagem de faturas não enviadas
    fun getFaturasNaoEnviadasCount(): Flow<Int> = repository.getFaturasNaoEnviadasCount()
} 