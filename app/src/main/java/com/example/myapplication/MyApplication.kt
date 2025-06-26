package com.example.myapplication

import android.app.Application
import database.AppDatabase
import android.util.Log
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MyApplication : Application() {
    
    // Instância do banco de dados Room
    lateinit var database: AppDatabase
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializa o banco de dados Room
        database = AppDatabase.getDatabase(this)
        Log.d("MyApplication", "Banco de dados Room inicializado com sucesso")
    }
} 