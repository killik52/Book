package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import database.entities.Artigo
import database.repository.ArtigoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ArtigoViewModel(private val repository: ArtigoRepository) : ViewModel() {
    
    // Flow para observar mudanças na lista de artigos
    val allArtigos: Flow<List<Artigo>> = repository.getAllArtigos()
    
    // Função para buscar artigos
    fun searchArtigos(query: String): Flow<List<Artigo>> = repository.searchArtigos(query)
    
    // Função para obter artigos recentes
    fun getRecentArtigos(limit: Int): Flow<List<Artigo>> = repository.getRecentArtigos(limit)
    
    // Função para inserir artigo
    fun insertArtigo(artigo: Artigo, onSuccess: (Long) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.insertArtigo(artigo)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // Função para atualizar artigo
    fun updateArtigo(artigo: Artigo, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateArtigo(artigo)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // Função para deletar artigo
    fun deleteArtigo(artigo: Artigo, onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteArtigo(artigo)
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // Função para obter artigo por ID
    fun getArtigoById(id: Long, onSuccess: (Artigo?) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val artigo = repository.getArtigoById(id)
                onSuccess(artigo)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // Função para obter artigo por número serial
    fun getArtigoByNumeroSerial(numeroSerial: String, onSuccess: (Artigo?) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val artigo = repository.getArtigoByNumeroSerial(numeroSerial)
                onSuccess(artigo)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
    
    // Função para obter contagem de artigos
    fun getArtigoCount(onSuccess: (Int) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val count = repository.getArtigoCount()
                onSuccess(count)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}

// Factory para criar o ViewModel com dependências
class ArtigoViewModelFactory(private val repository: ArtigoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArtigoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArtigoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 