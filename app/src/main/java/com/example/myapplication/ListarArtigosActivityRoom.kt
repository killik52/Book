package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import database.entities.Artigo
import database.repository.ArtigoRepository
import database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListarArtigosActivityRoom : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtigoAdapter
    private lateinit var artigoViewModel: ArtigoViewModel
    private var searchQuery: String = ""
    private val artigosList = mutableListOf<ArtigoItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_artigos)

        // Configurar toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_listar_artigos)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Lista de Artigos"

        // Configurar barra de navegação branca
        window.navigationBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        try {
            // Inicializa o ViewModel com factory
            val database = database.AppDatabase.getDatabase(this)
            val repository = ArtigoRepository(database.artigoDao())
            val factory = ArtigoViewModelFactory(repository)
            artigoViewModel = ViewModelProvider(this, factory)[ArtigoViewModel::class.java]
        } catch (e: Exception) {
            Log.e("ListarArtigos", "Erro ao inicializar ViewModel: ${e.message}")
            showToast("Erro ao inicializar o ViewModel: ${e.message}")
            finish()
            return
        }

        // Configura a RecyclerView
        recyclerView = findViewById(R.id.listViewArtigos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ArtigoAdapter(
            this,
            artigosList,
            { position ->
                val artigo = artigosList[position]
                abrirDetalhesArtigo(artigo)
            },
            { position ->
                // Não implementado para esta tela
            },
            { position ->
                // Não implementado para esta tela
            }
        )
        recyclerView.adapter = adapter

        // Adiciona espaçamento entre os itens
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))

        // Carrega os artigos
        carregarArtigos()
    }

    private fun carregarArtigos() {
        lifecycleScope.launch {
            artigoViewModel.allArtigos.collectLatest { artigos ->
                artigos?.let { listaArtigos ->
                    artigosList.clear()
                    artigosList.addAll(listaArtigos.map { artigo ->
                        ArtigoItem(
                            id = artigo.id,
                            nome = artigo.nome ?: "Nome não informado",
                            quantidade = artigo.quantidade ?: 1, // Valor padrão
                            preco = artigo.preco ?: 0.0, // Valor padrão
                            numeroSerial = artigo.numeroSerial,
                            descricao = artigo.descricao
                        )
                    })
                    adapter.notifyDataSetChanged()
                    Log.d("ListarArtigosActivityRoom", "Carregados ${listaArtigos.size} artigos")
                }
            }
        }
    }

    private fun filtrarArtigos(query: String) {
        lifecycleScope.launch {
            if (query.isEmpty()) {
                carregarArtigos()
            } else {
                artigoViewModel.searchArtigos(query).collectLatest { artigos ->
                    artigos?.let { listaArtigos ->
                        artigosList.clear()
                        artigosList.addAll(listaArtigos.map { artigo ->
                            ArtigoItem(
                                id = artigo.id,
                                nome = artigo.nome ?: "Nome não informado",
                                quantidade = artigo.quantidade ?: 1, // Valor padrão
                                preco = artigo.preco ?: 0.0, // Valor padrão
                                numeroSerial = artigo.numeroSerial,
                                descricao = artigo.descricao
                            )
                        })
                        adapter.notifyDataSetChanged()
                        Log.d("ListarArtigosActivityRoom", "Encontrados ${listaArtigos.size} artigos para '$query'")
                    }
                }
            }
        }
    }

    private fun abrirDetalhesArtigo(artigo: ArtigoItem) {
        val intent = Intent(this, CriarNovoArtigoActivityRoom::class.java).apply {
            putExtra("artigo_id", artigo.id)
            putExtra("nome_artigo", artigo.nome)
            putExtra("valor", artigo.preco)
            putExtra("numero_serial", artigo.numeroSerial)
            putExtra("descricao", artigo.descricao)
        }
        startActivityForResult(intent, REQUEST_EDIT_ARTIGO)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                // Reobserva os artigos com a nova query
                filtrarArtigos(searchQuery)
                return true
            }
        })
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_EDIT_ARTIGO && resultCode == RESULT_OK) {
            // Artigo foi editado, a lista será atualizada automaticamente pelo Flow
            Log.d("ListarArtigosActivityRoom", "Artigo editado, lista atualizada")
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Log.d("ListarArtigosActivityRoom", "onBackPressed disparado.")
        // Voltar para a MainActivity
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    companion object {
        private const val REQUEST_EDIT_ARTIGO = 1002
    }
} 