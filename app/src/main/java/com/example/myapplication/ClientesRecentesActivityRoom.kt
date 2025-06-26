package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityClientesRecentesBinding

class ClientesRecentesActivityRoom : AppCompatActivity() {

    private lateinit var binding: ActivityClientesRecentesBinding
    private lateinit var clienteViewModel: ClienteViewModel
    private val clientesList = mutableListOf<ClienteRecenteItem>()
    private var adapter: ArrayAdapter<String>? = null
    private val displayList = mutableListOf<String>()

    // Data class para representar um item de cliente recente
    data class ClienteRecenteItem(val id: Long, val nome: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientesRecentesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa o ViewModel
        clienteViewModel = ViewModelProvider(this)[ClienteViewModel::class.java]

        // Inicializa a lista e o adapter
        displayList.clear()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        binding.listViewClientesRecentes.adapter = adapter

        // Observa mudanças nos clientes
        clienteViewModel.allClientes.observe(this) { clientes ->
            carregarClientesRecentes(clientes)
        }

        // Listener para selecionar um cliente da lista
        binding.listViewClientesRecentes.setOnItemClickListener { _, _, position, _ ->
            try {
                val nomeClienteSelecionado = displayList.getOrNull(position)
                if (nomeClienteSelecionado != null) {
                    val clienteSelecionado = clientesList.find { it.nome == nomeClienteSelecionado }

                    if (clienteSelecionado != null) {
                        val resultIntent = Intent().apply {
                            putExtra("cliente_id", clienteSelecionado.id)
                            putExtra("nome_cliente", clienteSelecionado.nome)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    } else {
                        Log.w("ClientesRecentes", "Cliente selecionado na posição $position não encontrado nos dados base.")
                        showToast("Erro ao encontrar dados do cliente selecionado.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ClientesRecentes", "Erro ao selecionar cliente: ${e.message}")
                showToast("Erro ao selecionar cliente: ${e.message}")
            }
        }

        // Listener para "Criar um Novo Cliente"
        binding.textViewNovoCliente.setOnClickListener {
            val intent = Intent(this, CriarNovoClienteActivityRoom::class.java)
            startActivityForResult(intent, 123)
        }

        // Listener para o campo de pesquisa
        binding.editTextPesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarClientes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener para o botão "Ver tudo" na pesquisa
        binding.textViewVerTudoPesquisa.setOnClickListener {
            binding.editTextPesquisa.setText("")
            filtrarClientes("")
        }
    }

    private fun carregarClientesRecentes(clientes: List<database.entities.Cliente>) {
        clientesList.clear()
        displayList.clear()
        
        try {
            // Pega os 20 clientes mais recentes (ordenados por ID decrescente)
            val clientesRecentes = clientes.sortedByDescending { it.id }.take(20)
            
            clientesRecentes.forEach { cliente ->
                val nomeCliente = cliente.nome ?: "Cliente sem nome"
                clientesList.add(ClienteRecenteItem(cliente.id, nomeCliente))
                displayList.add(nomeCliente)
            }
            
            adapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("ClientesRecentes", "Erro ao carregar clientes recentes: ${e.message}")
            showToast("Erro ao carregar clientes recentes: ${e.message}")
        }
    }

    private fun filtrarClientes(query: String) {
        val filteredListNomes = if (query.isEmpty()) {
            clientesList.map { it.nome }
        } else {
            clientesList.filter {
                it.nome.contains(query, ignoreCase = true)
            }.map { it.nome }
        }

        displayList.clear()
        displayList.addAll(filteredListNomes)
        adapter?.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) { // Retorno de CriarNovoClienteActivity
            if (resultCode == RESULT_OK && data != null) {
                setResult(RESULT_OK, data) // Repassa o resultado para SecondScreenActivity
                finish()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 