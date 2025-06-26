package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import database.AppDatabase
import database.entities.FaturaLixeira
import database.repository.FaturaLixeiraRepository
import database.repository.FaturaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LixeiraViewModel(application: Application) : AndroidViewModel(application) {
    private val faturaLixeiraRepository: FaturaLixeiraRepository
    private val faturaRepository: FaturaRepository

    val faturasLixeira =
        FaturaLixeiraRepository(AppDatabase.getDatabase(application).faturaLixeiraDao())
            .getAllFaturasLixeira()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val db = AppDatabase.getDatabase(application)
        faturaLixeiraRepository = FaturaLixeiraRepository(db.faturaLixeiraDao())
        faturaRepository = FaturaRepository(db.faturaDao(), db.faturaLixeiraDao())
    }

    fun restaurarFatura(fatura: FaturaLixeira) = viewModelScope.launch {
        val db = AppDatabase.getDatabase(getApplication())
        faturaRepository.restaurarFaturaDaLixeiraRoom(
            faturaLixeira = fatura,
            faturaItemLixeiraDao = db.faturaItemLixeiraDao(),
            faturaFotoLixeiraDao = db.faturaFotoLixeiraDao(),
            faturaNotaLixeiraDao = db.faturaNotaLixeiraDao(),
            faturaItemDao = db.faturaItemDao(),
            faturaFotoDao = db.faturaFotoDao(),
            faturaNotaDao = db.faturaNotaDao(),
            faturaLixeiraDao = db.faturaLixeiraDao()
        )
    }

    fun excluirFaturaPermanentemente(fatura: FaturaLixeira) = viewModelScope.launch {
        faturaLixeiraRepository.deleteFaturaLixeira(fatura)
    }
} 