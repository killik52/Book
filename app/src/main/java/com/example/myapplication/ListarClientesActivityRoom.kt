package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import database.entities.Cliente
import database.repository.ClienteRepository
import database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListarClientesActivityRoom : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClienteAdapterSimples
    private lateinit var clienteViewModel: ClienteViewModel
    private var searchQuery: String = ""
    private lateinit var editTextBusca: android.widget.EditText
    private lateinit var listaClientes: MutableList<Cliente>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_clientes)

        Log.d("ListarClientesActivityRoom", "onCreate iniciado")

        // Configurar toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_listar_clientes)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Lista de Clientes"

        // Configurar barra de navegação branca
        window.navigationBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        // Inicializa o ViewModel
        try {
            clienteViewModel = ViewModelProvider(this)[ClienteViewModel::class.java]
            Log.d("ListarClientesActivityRoom", "ViewModel inicializado com sucesso")
            
            // Verifica se o banco está acessível
            val database = database.AppDatabase.getDatabase(this)
            Log.d("ListarClientesActivityRoom", "Banco de dados acessível: ${database.isOpen}")
            
        } catch (e: Exception) {
            Log.e("ListarClientesActivityRoom", "Erro ao inicializar ViewModel: ${e.message}")
            showToast("Erro ao inicializar o ViewModel: ${e.message}")
            finish()
            return
        }

        // Configura a RecyclerView
        recyclerView = findViewById(R.id.listViewClientes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ClienteAdapterSimples(this) { cliente ->
            abrirDetalhesCliente(cliente)
        }
        recyclerView.adapter = adapter

        // Adiciona espaçamento entre os itens
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(16))

        // Carrega os clientes
        carregarClientes()
        
        // Verifica se existem clientes no banco
        verificarClientesNoBanco()
        
        // Verifica se a tabela existe
        verificarTabelaClientes()
    }

    private fun carregarClientes() {
        Log.d("ListarClientesActivityRoom", "Carregando clientes...")
        clienteViewModel.allClientes.observe(this) { clientes ->
            Log.d("ListarClientesActivityRoom", "Clientes recebidos: ${clientes?.size ?: 0}")
            clientes?.let { listaClientes ->
                adapter.atualizarLista(listaClientes)
                Log.d("ListarClientesActivityRoom", "Lista atualizada no adapter com ${listaClientes.size} clientes")
            } ?: run {
                Log.d("ListarClientesActivityRoom", "Lista de clientes é null")
                adapter.atualizarLista(emptyList())
            }
        }
    }

    private fun verificarClientesNoBanco() {
        clienteViewModel.getClienteCount().observe(this) { count ->
            Log.d("ListarClientesActivityRoom", "Total de clientes no banco: $count")
            if (count == 0) {
                showToast("Nenhum cliente encontrado no banco de dados")
                // Insere um cliente de teste
                inserirClienteTeste()
            }
        }
    }

    private fun inserirClienteTeste() {
        val clienteTeste = database.entities.Cliente(
            nome = "Cliente Teste",
            email = "teste@email.com",
            telefone = "(11) 99999-9999",
            informacoesAdicionais = "Cliente de teste para verificar a lista",
            cpf = null,
            cnpj = null,
            logradouro = null,
            numero = null,
            complemento = null,
            bairro = null,
            municipio = null,
            uf = null,
            cep = null,
            numeroSerial = null
        )
        
        clienteViewModel.insertCliente(clienteTeste)
        Log.d("ListarClientesActivityRoom", "Cliente de teste inserido")
        showToast("Cliente de teste inserido para verificar a lista")
    }

    private fun filtrarClientes(query: String) {
        if (query.isEmpty()) {
            carregarClientes()
        } else {
            clienteViewModel.searchClientes(query).observe(this) { clientes ->
                clientes?.let { listaClientes ->
                    adapter.atualizarLista(listaClientes)
                }
            }
        }
    }

    private fun abrirDetalhesCliente(cliente: Cliente) {
        val intent = Intent(this, ClienteActivityRoom::class.java).apply {
            putExtra("id", cliente.id)
        }
        startActivityForResult(intent, REQUEST_EDIT_CLIENTE)
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
                // Reobserva os clientes com a nova query
                filtrarClientes(searchQuery)
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
        
        if (requestCode == REQUEST_EDIT_CLIENTE && resultCode == RESULT_OK) {
            // Cliente foi editado, a lista será atualizada automaticamente pelo Flow
            Log.d("ListarClientesActivityRoom", "Cliente editado, lista atualizada")
        }
    }

    override fun onBackPressed() {
        Log.d("ListarClientesActivityRoom", "onBackPressed disparado.")
        // Voltar para a MainActivity
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun verificarTabelaClientes() {
        lifecycleScope.launch {
            try {
                val database = database.AppDatabase.getDatabase(this@ListarClientesActivityRoom)
                val count = database.clienteDao().getClienteCount().value ?: 0
                Log.d("ListarClientesActivityRoom", "Verificação da tabela: $count clientes encontrados")
            } catch (e: Exception) {
                Log.e("ListarClientesActivityRoom", "Erro ao verificar tabela: ${e.message}")
            }
        }
    }

    companion object {
        private const val REQUEST_EDIT_CLIENTE = 1001
    }
}

// Adapter simples para testar
class ClienteAdapterSimples(
    private val context: android.content.Context,
    private val onClienteClick: (Cliente) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ClienteAdapterSimples.ClienteViewHolder>() {

    private val clientes = mutableListOf<Cliente>()

    fun atualizarLista(novaLista: List<Cliente>) {
        Log.d("ClienteAdapterSimples", "atualizarLista chamado com ${novaLista.size} itens")
        clientes.clear()
        clientes.addAll(novaLista)
        notifyDataSetChanged()
        Log.d("ClienteAdapterSimples", "Lista atualizada, notifyDataSetChanged chamado")
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ClienteViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente_lista, parent, false)
        Log.d("ClienteAdapterSimples", "ViewHolder criado")
        return ClienteViewHolder(view, onClienteClick)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = clientes[position]
        Log.d("ClienteAdapterSimples", "Vinculando cliente na posição $position: ${cliente.nome}")
        holder.bind(cliente)
    }

    override fun getItemCount(): Int {
        val count = clientes.size
        Log.d("ClienteAdapterSimples", "getItemCount: $count")
        return count
    }

    class ClienteViewHolder(
        itemView: View,
        private val onClienteClick: (Cliente) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        
        private val textView: android.widget.TextView = itemView.findViewById(R.id.text1)

        fun bind(cliente: Cliente) {
            val displayText = buildString {
                append(cliente.nome ?: "Nome não informado")
                append("\n")
                append(cliente.email ?: "Email não informado")
                append(" | ")
                append(cliente.telefone ?: "Telefone não informado")
            }
            
            textView.text = displayText
            Log.d("ClienteAdapterSimples", "Cliente vinculado: $displayText")
            
            itemView.setOnClickListener {
                onClienteClick(cliente)
            }
        }
    }
}

// Adapter para o RecyclerView usando ListAdapter
class ClienteAdapter(
    private val context: android.content.Context,
    private val onClienteClick: (Cliente) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Cliente, ClienteAdapter.ClienteViewHolder>(ClienteDiffCallback()) {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ClienteViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cliente_lista, parent, false)
        Log.d("ClienteAdapter", "ViewHolder criado")
        return ClienteViewHolder(view, onClienteClick)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = getItem(position)
        Log.d("ClienteAdapter", "Vinculando cliente na posição $position: ${cliente.nome}")
        holder.bind(cliente)
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d("ClienteAdapter", "getItemCount: $count")
        return count
    }

    override fun submitList(list: List<Cliente>?) {
        Log.d("ClienteAdapter", "submitList chamado com ${list?.size ?: 0} itens")
        super.submitList(list)
    }

    class ClienteViewHolder(
        itemView: View,
        private val onClienteClick: (Cliente) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        
        private val textView: android.widget.TextView = itemView.findViewById(R.id.text1)

        fun bind(cliente: Cliente) {
            val displayText = buildString {
                append(cliente.nome ?: "Nome não informado")
                append("\n")
                append(cliente.email ?: "Email não informado")
                append(" | ")
                append(cliente.telefone ?: "Telefone não informado")
            }
            
            textView.text = displayText
            Log.d("ClienteAdapter", "Cliente vinculado: $displayText")
            
            itemView.setOnClickListener {
                onClienteClick(cliente)
            }
        }
    }
}

// DiffCallback para otimizar atualizações do RecyclerView
class ClienteDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Cliente>() {
    override fun areItemsTheSame(oldItem: Cliente, newItem: Cliente): Boolean {
        val result = oldItem.id == newItem.id
        Log.d("ClienteDiffCallback", "areItemsTheSame: $result (${oldItem.id} vs ${newItem.id})")
        return result
    }

    override fun areContentsTheSame(oldItem: Cliente, newItem: Cliente): Boolean {
        val result = oldItem == newItem
        Log.d("ClienteDiffCallback", "areContentsTheSame: $result")
        return result
    }
} 