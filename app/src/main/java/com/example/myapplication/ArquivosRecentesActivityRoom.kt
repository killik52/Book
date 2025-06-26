package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import database.entities.Artigo
import database.repository.ArtigoRepository
import database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ArquivosRecentesActivityRoom : AppCompatActivity() {

    private var listViewArquivosRecentes: ListView? = null
    private var editTextPesquisa: EditText? = null
    private var textViewVerTudoPesquisa: TextView? = null
    private var textViewNovoArquivo: TextView? = null
    private lateinit var artigoViewModel: ArtigoViewModel
    private val artigosList = mutableListOf<ArtigoRecenteItem>()
    private var adapter: ArrayAdapter<String>? = null
    private val displayList = mutableListOf<String>()

    private var isFinishingDueToResultPropagation = false // Flag para otimizar onResume

    data class ArtigoRecenteItem(val id: Long, val nome: String, val preco: Double, val quantidade: Int, val numeroSerial: String?, val descricao: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arquivos_recentes)

        try {
            // Inicializa o ViewModel com factory
            val database = database.AppDatabase.getDatabase(this)
            val repository = ArtigoRepository(database.artigoDao())
            val factory = ArtigoViewModelFactory(repository)
            artigoViewModel = ViewModelProvider(this, factory)[ArtigoViewModel::class.java]
        } catch (e: Exception) {
            Log.e("ArquivosRecentesActivityRoom", "Erro ao inicializar ViewModel: ${e.message}")
            showToast("Erro ao inicializar o ViewModel: ${e.message}")
            finish()
            return
        }

        listViewArquivosRecentes = findViewById(R.id.listViewArquivosRecentes)
        editTextPesquisa = findViewById(R.id.editTextPesquisa)
        textViewVerTudoPesquisa = findViewById(R.id.textViewVerTudoPesquisa)
        textViewNovoArquivo = findViewById(R.id.textViewNovoArquivo)

        displayList.clear()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listViewArquivosRecentes?.adapter = adapter

        // carregarArtigos() será chamado em onResume se necessário

        listViewArquivosRecentes?.setOnItemClickListener { _, _, position, _ ->
            try {
                val nomeArtigoSelecionado = displayList.getOrNull(position)
                val artigoSelecionado = artigosList.find { it.nome == nomeArtigoSelecionado }

                if (artigoSelecionado != null) {
                    val intent = Intent().apply {
                        putExtra("artigo_id", artigoSelecionado.id)
                        putExtra("nome_artigo", artigoSelecionado.nome)
                        putExtra("quantidade", artigoSelecionado.quantidade)
                        putExtra("preco_unitario_artigo", artigoSelecionado.preco)
                        putExtra("valor", artigoSelecionado.preco * artigoSelecionado.quantidade)
                        putExtra("numero_serial", artigoSelecionado.numeroSerial)
                        putExtra("descricao", artigoSelecionado.descricao)
                        putExtra("salvar_fatura", true)
                    }
                    isFinishingDueToResultPropagation = true
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    Log.w("ArquivosRecentesActivityRoom", "Artigo selecionado na posição $position não encontrado nos dados base.")
                    showToast("Erro ao encontrar dados do artigo selecionado.")
                }
            } catch (e: Exception) {
                Log.e("ArquivosRecentesActivityRoom", "Erro ao selecionar artigo: ${e.message}")
                showToast("Erro ao selecionar artigo: ${e.message}")
            }
        }

        textViewNovoArquivo?.setOnClickListener {
            val intent = Intent(this, CriarNovoArtigoActivityRoom::class.java)
            startActivityForResult(intent, 792)
        }

        editTextPesquisa?.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)) {
                val query = editTextPesquisa?.text.toString().trim()
                filtrarArtigos(query)
                true
            } else {
                false
            }
        }

        textViewVerTudoPesquisa?.setOnClickListener {
            editTextPesquisa?.setText("")
            filtrarArtigos("")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishingDueToResultPropagation) {
            carregarArtigos()
        }
        // A flag será resetada naturalmente na próxima vez que onCreate for chamado,
        // ou se a atividade for retomada sem estar finalizando por propagação.
    }

    override fun onPause() {
        super.onPause()
        // Se a atividade está finalizando por propagação, resetamos a flag
        // para que um próximo onResume (se ocorrer por algum motivo inesperado)
        // não pule o carregamento. Mas normalmente finish() já resolve.
        if (isFinishing && isFinishingDueToResultPropagation) {
            isFinishingDueToResultPropagation = false
        }
    }

    private fun carregarArtigos() {
        artigosList.clear()
        displayList.clear()
        
        lifecycleScope.launch {
            artigoViewModel.allArtigos.collectLatest { artigos ->
                artigos?.let { listaArtigos ->
                    // Filtra apenas artigos que devem ser guardados para faturas
                    val artigosRecentes = listaArtigos.filter { it.guardarFatura == true }
                        .sortedByDescending { it.id }
                        .take(20) // Limita aos 20 mais recentes
                    
                    artigosRecentes.forEach { artigo ->
                        val nome = artigo.nome ?: "Artigo sem nome"
                        val preco = artigo.preco ?: 0.0
                        val quantidade = artigo.quantidade ?: 1
                        val numeroSerial = artigo.numeroSerial
                        val descricao = artigo.descricao
                        
                        artigosList.add(ArtigoRecenteItem(artigo.id, nome, preco, quantidade, numeroSerial, descricao))
                        displayList.add(nome)
                    }
                    
                    adapter?.notifyDataSetChanged()
                    Log.d("ArquivosRecentesActivityRoom", "Carregados ${artigosRecentes.size} artigos recentes")
                }
            }
        }
    }

    private fun filtrarArtigos(query: String) {
        val filteredListNomes = if (query.isEmpty()) {
            artigosList.map { it.nome }
        } else {
            artigosList.filter {
                it.nome.contains(query, ignoreCase = true) ||
                        (it.numeroSerial?.contains(query, ignoreCase = true) == true) ||
                        (it.descricao?.contains(query, ignoreCase = true) == true)
            }.map { it.nome }
        }

        displayList.clear()
        displayList.addAll(filteredListNomes)
        adapter?.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 792) { // Retorno de CriarNovoArtigoActivityRoom
            if (resultCode == RESULT_OK && data != null) {
                isFinishingDueToResultPropagation = true
                setResult(RESULT_OK, data)
                finish()
            } else {
                isFinishingDueToResultPropagation = false // Garante que onResume recarregue se o usuário cancelou
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 