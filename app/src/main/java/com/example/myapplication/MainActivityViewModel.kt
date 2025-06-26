package com.example.myapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import database.entities.Fatura
import database.entities.FaturaLixeira
import database.repository.FaturaRepository
import database.repository.FaturaLixeiraRepository
import database.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class MainActivityUiState {
    object Loading : MainActivityUiState()
    data class Success(val faturas: List<FaturaResumidaItem>) : MainActivityUiState()
    data class Error(val message: String) : MainActivityUiState()
}

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    
    private val faturaRepository: FaturaRepository
    private val faturaLixeiraRepository: FaturaLixeiraRepository
    
    // StateFlow para o estado da UI
    private val _uiState = MutableStateFlow<MainActivityUiState>(MainActivityUiState.Loading)
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()
    
    // StateFlow para faturas filtradas (busca)
    private val _searchResults = MutableStateFlow<List<FaturaResumidaItem>>(emptyList())
    val searchResults: StateFlow<List<FaturaResumidaItem>> = _searchResults.asStateFlow()
    
    // StateFlow para indicar se está em modo de busca
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        faturaRepository = FaturaRepository(database.faturaDao(), database.faturaLixeiraDao())
        faturaLixeiraRepository = FaturaLixeiraRepository(database.faturaLixeiraDao())
        
        // Carrega faturas automaticamente
        carregarFaturas()
    }
    
    fun carregarFaturas() {
        viewModelScope.launch {
            try {
                _uiState.value = MainActivityUiState.Loading
                
                faturaRepository.getAllFaturas().collect { faturas ->
                    val faturasResumidas = faturas.map { fatura ->
                        fatura.toFaturaResumidaItem()
                    }
                    _uiState.value = MainActivityUiState.Success(faturasResumidas)
                }
            } catch (e: Exception) {
                "Erro ao carregar faturas: ${e.message}".logError("MainActivityViewModel")
                _uiState.value = MainActivityUiState.Error(Constants.Messages.ERROR_LOADING_DATA)
            }
        }
    }
    
    fun buscarFaturas(query: String) {
        if (query.isBlank()) {
            _isSearchActive.value = false
            carregarFaturas()
            return
        }
        
        viewModelScope.launch {
            try {
                _isSearchActive.value = true
                
                faturaRepository.searchFaturas(query).collect { faturas ->
                    val faturasResumidas = faturas.map { fatura ->
                        fatura.toFaturaResumidaItem()
                    }
                    _searchResults.value = faturasResumidas
                }
            } catch (e: Exception) {
                "Erro ao buscar faturas: ${e.message}".logError("MainActivityViewModel")
                _uiState.value = MainActivityUiState.Error("Erro ao buscar faturas: ${e.message}")
            }
        }
    }
    
    fun limparBusca() {
        _isSearchActive.value = false
        _searchResults.value = emptyList()
        carregarFaturas()
    }
    
    fun moverFaturaParaLixeira(fatura: FaturaResumidaItem, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                faturaRepository.getFaturaById(fatura.id).collect { faturaCompleta ->
                    faturaCompleta?.let { fatura ->
                        faturaRepository.moverFaturaParaLixeiraRoom(
                            fatura = fatura,
                            faturaItemDao = db.faturaItemDao(),
                            faturaFotoDao = db.faturaFotoDao(),
                            faturaNotaDao = db.faturaNotaDao(),
                            faturaItemLixeiraDao = db.faturaItemLixeiraDao(),
                            faturaFotoLixeiraDao = db.faturaFotoLixeiraDao(),
                            faturaNotaLixeiraDao = db.faturaNotaLixeiraDao(),
                            faturaLixeiraDao = db.faturaLixeiraDao()
                        )
                        onSuccess()
                    } ?: run {
                        onError("Fatura não encontrada")
                    }
                }
            } catch (e: Exception) {
                "Erro ao mover fatura para lixeira: \\${e.message}".logError("MainActivityViewModel")
                onError("Erro ao mover fatura: \\${e.message}")
            }
        }
    }
    
    fun obterFaturaPorId(id: Long, onSuccess: (Fatura?) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                faturaRepository.getFaturaById(id).collect { fatura ->
                    onSuccess(fatura)
                }
            } catch (e: Exception) {
                "Erro ao obter fatura por ID: ${e.message}".logError("MainActivityViewModel")
                onError("Erro ao obter fatura: ${e.message}")
            }
        }
    }
    
    private fun Fatura.toFaturaResumidaItem(): FaturaResumidaItem {
        val serialNumbers = mutableListOf<String?>()
        
        // Extrai números de série dos artigos
        artigos?.split("|")?.forEach { artigoData ->
            val parts = artigoData.split(",")
            if (parts.size >= 5) {
                val serial = parts[4].takeIf { s -> s.isNotEmpty() && s.lowercase(Locale.ROOT) != "null" }
                serialNumbers.add(serial)
            }
        }
        
        // Formata a data usando constantes
        val formattedData = data?.formatDate(
            Constants.DateFormats.INPUT_FORMAT,
            Constants.DateFormats.OUTPUT_FORMAT
        ) ?: ""
        
        return FaturaResumidaItem(
            id = id,
            numeroFatura = numeroFatura ?: "",
            cliente = cliente ?: "",
            serialNumbers = serialNumbers,
            saldoDevedor = saldoDevedor ?: 0.0,
            data = formattedData,
            foiEnviada = foiEnviada
        )
    }
}