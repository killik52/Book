package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityClientesRecentesBinding // CORREÇÃO: Importação do binding correto

class ClientesRecentesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientesRecentesBinding // CORREÇÃO: Tipo de binding correto
    private var dbHelper: ClienteDbHelper? = null
    private val clientesList = mutableListOf<ClienteRecenteItem>()
    private var adapter: ArrayAdapter<String>? = null
    private val displayList = mutableListOf<String>()

    // Data class para representar um item de cliente recente
    data class ClienteRecenteItem(val id: Long, val nome: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientesRecentesBinding.inflate(layoutInflater) // CORREÇÃO: Infla o layout correto
        setContentView(binding.root)

        dbHelper = ClienteDbHelper(this)

        // Inicializa a lista e o adapter
        displayList.clear()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        binding.listViewClientesRecentes.adapter = adapter

        carregarClientesRecentes()

        // Listener para selecionar um cliente da lista
        binding.listViewClientesRecentes.setOnItemClickListener { _, _, position, _ ->
            try {
                val nomeClienteSelecionado = displayList.getOrNull(position)
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
            } catch (e: Exception) {
                Log.e("ClientesRecentes", "Erro ao selecionar cliente: ${e.message}")
                showToast("Erro ao selecionar cliente: ${e.message}")
            }
        }

        // Listener para "Criar um Novo Cliente"
        binding.textViewNovoCliente.setOnClickListener { // Este é o TextView "Criar um Novo Cliente" no activity_clientes_recentes.xml
            val intent = Intent(this, CriarNovoClienteActivityRoom::class.java)
            startActivityForResult(intent, 123) // Usar um request code para saber de onde veio o resultado
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

    override fun onResume() {
        super.onResume()
        carregarClientesRecentes() // Recarrega a lista ao voltar para esta Activity
    }

    private fun carregarClientesRecentes() {
        clientesList.clear()
        displayList.clear()
        try {
            val db = dbHelper?.readableDatabase
            if (db == null) {
                Log.e("ClientesRecentes", "Banco de dados não inicializado.")
                return
            }

            // Seleciona os clientes mais recentes, pode ajustar o ORDER BY ou LIMIT conforme necessário
            val cursor: Cursor? = db.rawQuery(
                "SELECT ${BaseColumns._ID}, ${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} " +
                        "FROM ${ClienteContract.ClienteEntry.TABLE_NAME} " +
                        "ORDER BY ${BaseColumns._ID} DESC LIMIT 20", // Limita aos 20 clientes mais recentes
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                    val nome = it.getString(it.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME))
                    clientesList.add(ClienteRecenteItem(id, nome))
                    displayList.add(nome)
                }
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

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}