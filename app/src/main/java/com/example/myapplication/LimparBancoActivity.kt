package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import kotlinx.coroutines.launch
import java.io.File

class LimparBancoActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var dbHelper: ClienteDbHelper? = null
    private lateinit var btnLimparBanco: Button
    private lateinit var tvStatus: TextView
    private var limparApenasClientes: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_limpar_banco)

        database = AppDatabase.getDatabase(this)
        dbHelper = ClienteDbHelper(this)
        
        // Verificar se deve limpar apenas clientes
        limparApenasClientes = intent.getBooleanExtra("limpar_apenas_clientes", false)
        
        btnLimparBanco = findViewById(R.id.btnLimparBanco)
        tvStatus = findViewById(R.id.tvStatus)

        // Ajustar interface baseado no modo
        if (limparApenasClientes) {
            ajustarInterfaceParaClientes()
        }

        btnLimparBanco.setOnClickListener {
            if (limparApenasClientes) {
                mostrarDialogoConfirmacaoClientes()
            } else {
                mostrarDialogoConfirmacao()
            }
        }
    }

    private fun ajustarInterfaceParaClientes() {
        // Alterar o título
        val tituloTextView = findViewById<TextView>(R.id.tvTitulo)
        tituloTextView?.text = "Limpar Clientes Recentes"
        
        // Alterar a descrição
        val descricaoTextView = findViewById<TextView>(R.id.tvDescricao)
        descricaoTextView?.text = "Esta ação irá remover TODOS os dados de clientes, incluindo:\n\n• Banco Room (Clientes)\n• Banco SQLite (Clientes)\n• Histórico de clientes recentes\n\n⚠️ ATENÇÃO: Esta ação não pode ser desfeita!"
        
        // Alterar o texto do botão
        btnLimparBanco.text = "Limpar Clientes"
    }

    private fun mostrarDialogoConfirmacaoClientes() {
        AlertDialog.Builder(this)
            .setTitle("Limpar Clientes Recentes")
            .setMessage("Tem certeza que deseja limpar todos os dados de clientes? Esta ação irá remover:\n\n• Todos os clientes do banco Room\n• Todos os clientes do banco SQLite\n• Histórico de clientes recentes\n\nEsta ação não pode ser desfeita!")
            .setPositiveButton("Sim, Limpar") { _, _ ->
                limparApenasClientes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoConfirmacao() {
        AlertDialog.Builder(this)
            .setTitle("Limpar Banco de Dados")
            .setMessage("Tem certeza que deseja limpar todos os dados dos bancos? Esta ação irá remover:\n\n• Todos os dados do banco Room\n• Todos os dados do banco SQLite\n• SharedPreferences relacionados\n\nEsta ação não pode ser desfeita!")
            .setPositiveButton("Sim, Limpar") { _, _ ->
                limparBancoDeDados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limparBancoDeDados() {
        btnLimparBanco.isEnabled = false
        tvStatus.text = "Limpando bancos de dados..."

        lifecycleScope.launch {
            try {
                // 1. Limpar banco Room
                tvStatus.text = "Limpando banco Room..."
                database.clearAllData()
                Log.d("LimparBancoActivity", "Banco Room limpo com sucesso")

                // 2. Limpar banco SQLite
                tvStatus.text = "Limpando banco SQLite..."
                limparBancoSQLite()
                Log.d("LimparBancoActivity", "Banco SQLite limpo com sucesso")

                // 3. Limpar SharedPreferences
                tvStatus.text = "Limpando configurações..."
                limparSharedPreferences()
                Log.d("LimparBancoActivity", "SharedPreferences limpos com sucesso")

                // 4. Limpar arquivos de fotos
                tvStatus.text = "Limpando arquivos de fotos..."
                limparArquivosFotos()
                Log.d("LimparBancoActivity", "Arquivos de fotos limpos com sucesso")

                runOnUiThread {
                    tvStatus.text = "Todos os dados foram limpos com sucesso!"
                    Toast.makeText(this@LimparBancoActivity, "Todos os dados foram limpos com sucesso!", Toast.LENGTH_LONG).show()
                    btnLimparBanco.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("LimparBancoActivity", "Erro ao limpar bancos: ${e.message}", e)
                runOnUiThread {
                    tvStatus.text = "Erro ao limpar bancos: ${e.message}"
                    Toast.makeText(this@LimparBancoActivity, "Erro ao limpar bancos: ${e.message}", Toast.LENGTH_LONG).show()
                    btnLimparBanco.isEnabled = true
                }
            }
        }
    }

    private fun limparBancoSQLite() {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LimparBancoActivity", "Erro ao acessar banco SQLite")
            throw Exception("Não foi possível acessar o banco SQLite")
        }

        try {
            db.beginTransaction()

            // Limpar todas as tabelas
            val tabelas = listOf(
                "clientes",
                "artigos", 
                "faturas",
                "fatura_items",
                "fatura_notas",
                "fatura_fotos",
                "fatura_lixeira",
                "clientes_bloqueados"
            )

            tabelas.forEach { tabela ->
                try {
                    val deletedRows = db.delete(tabela, null, null)
                    Log.d("LimparBancoActivity", "Tabela $tabela: $deletedRows registros removidos")
                } catch (e: Exception) {
                    Log.w("LimparBancoActivity", "Erro ao limpar tabela $tabela: ${e.message}")
                    // Continua mesmo se uma tabela não existir
                }
            }

            // Resetar contadores de ID (AUTOINCREMENT)
            val resetQueries = listOf(
                "DELETE FROM sqlite_sequence WHERE name='clientes'",
                "DELETE FROM sqlite_sequence WHERE name='artigos'",
                "DELETE FROM sqlite_sequence WHERE name='faturas'",
                "DELETE FROM sqlite_sequence WHERE name='fatura_items'",
                "DELETE FROM sqlite_sequence WHERE name='fatura_notas'",
                "DELETE FROM sqlite_sequence WHERE name='fatura_fotos'",
                "DELETE FROM sqlite_sequence WHERE name='fatura_lixeira'",
                "DELETE FROM sqlite_sequence WHERE name='clientes_bloqueados'"
            )

            resetQueries.forEach { query ->
                try {
                    db.execSQL(query)
                } catch (e: Exception) {
                    Log.w("LimparBancoActivity", "Erro ao resetar sequência: ${e.message}")
                }
            }

            db.setTransactionSuccessful()
            Log.d("LimparBancoActivity", "Banco SQLite limpo com sucesso")
        } catch (e: Exception) {
            Log.e("LimparBancoActivity", "Erro ao limpar banco SQLite: ${e.message}", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }

    private fun limparSharedPreferences() {
        try {
            // Limpar SharedPreferences de faturas
            val faturaPrefs = getSharedPreferences("fatura_prefs", Context.MODE_PRIVATE)
            faturaPrefs.edit().clear().apply()
            Log.d("LimparBancoActivity", "SharedPreferences de faturas limpos")

            // Limpar SharedPreferences de notas padrão
            val notasPrefs = getSharedPreferences("notas_padrao_prefs", Context.MODE_PRIVATE)
            notasPrefs.edit().clear().apply()
            Log.d("LimparBancoActivity", "SharedPreferences de notas limpos")

            // Limpar outros SharedPreferences se existirem
            val outrosPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            outrosPrefs.edit().clear().apply()
            Log.d("LimparBancoActivity", "Outros SharedPreferences limpos")

        } catch (e: Exception) {
            Log.w("LimparBancoActivity", "Erro ao limpar SharedPreferences: ${e.message}")
            // Não falha a operação se houver erro nos SharedPreferences
        }
    }

    private fun limparArquivosFotos() {
        try {
            // Limpar fotos salvas no diretório interno do app
            val fotosDir = File(filesDir, "fotos")
            if (fotosDir.exists()) {
                val fotosRemovidas = fotosDir.deleteRecursively()
                Log.d("LimparBancoActivity", "Diretório de fotos removido: $fotosRemovidas")
            }

            // Limpar fotos salvas no cache
            val cacheDir = File(cacheDir, "fotos")
            if (cacheDir.exists()) {
                val cacheRemovido = cacheDir.deleteRecursively()
                Log.d("LimparBancoActivity", "Cache de fotos removido: $cacheRemovido")
            }

            // Limpar outros arquivos temporários
            val tempDir = File(cacheDir, "temp")
            if (tempDir.exists()) {
                val tempRemovido = tempDir.deleteRecursively()
                Log.d("LimparBancoActivity", "Arquivos temporários removidos: $tempRemovido")
            }

        } catch (e: Exception) {
            Log.w("LimparBancoActivity", "Erro ao limpar arquivos de fotos: ${e.message}")
            // Não falha a operação se houver erro na limpeza de arquivos
        }
    }

    // Método específico para limpar apenas clientes
    fun limparApenasClientes() {
        btnLimparBanco.isEnabled = false
        tvStatus.text = "Limpando dados de clientes..."

        lifecycleScope.launch {
            try {
                // 1. Limpar clientes do banco Room
                tvStatus.text = "Limpando clientes do Room..."
                database.clienteDao().deleteAllClientes()
                Log.d("LimparBancoActivity", "Clientes limpos do Room com sucesso")

                // 2. Limpar clientes do banco SQLite
                tvStatus.text = "Limpando clientes do SQLite..."
                limparClientesSQLite()
                Log.d("LimparBancoActivity", "Clientes limpos do SQLite com sucesso")

                runOnUiThread {
                    tvStatus.text = "Dados de clientes limpos com sucesso!"
                    Toast.makeText(this@LimparBancoActivity, "Clientes limpos com sucesso!", Toast.LENGTH_LONG).show()
                    btnLimparBanco.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("LimparBancoActivity", "Erro ao limpar clientes: ${e.message}", e)
                runOnUiThread {
                    tvStatus.text = "Erro ao limpar clientes: ${e.message}"
                    Toast.makeText(this@LimparBancoActivity, "Erro ao limpar clientes: ${e.message}", Toast.LENGTH_LONG).show()
                    btnLimparBanco.isEnabled = true
                }
            }
        }
    }

    private fun limparClientesSQLite() {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LimparBancoActivity", "Erro ao acessar banco SQLite")
            throw Exception("Não foi possível acessar o banco SQLite")
        }

        try {
            db.beginTransaction()

            // Limpar apenas a tabela de clientes
            val deletedRows = db.delete("clientes", null, null)
            Log.d("LimparBancoActivity", "Clientes removidos do SQLite: $deletedRows")

            // Resetar contador de ID para clientes
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='clientes'")

            db.setTransactionSuccessful()
            Log.d("LimparBancoActivity", "Clientes limpos do SQLite com sucesso")
        } catch (e: Exception) {
            Log.e("LimparBancoActivity", "Erro ao limpar clientes do SQLite: ${e.message}", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }

    override fun onBackPressed() {
        Log.d("LimparBancoActivity", "onBackPressed disparado.")
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onDestroy() {
        try {
            dbHelper?.close()
            Log.d("LimparBancoActivity", "ClienteDbHelper fechado")
        } catch (e: Exception) {
            Log.e("LimparBancoActivity", "Erro ao fechar banco: ${e.message}", e)
        }
        super.onDestroy()
    }
} 