package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import database.entities.Cliente
import database.repository.ClienteRepository

class ClienteActivityRoom : AppCompatActivity() {

    // Referências para os campos da UI
    private lateinit var editTextNomeDetalhe: EditText
    private lateinit var editTextEmailDetalhe: EditText
    private lateinit var editTextTelefoneDetalhe: EditText
    private lateinit var editTextInformacoesAdicionaisDetalhe: EditText
    private lateinit var editTextCPFDetalhe: EditText
    private lateinit var editTextCNPJDetalhe: EditText
    private lateinit var editTextNumeroSerialDetalhe: EditText
    private lateinit var buttonExcluirCliente: Button
    private lateinit var buttonBloquearCliente: Button
    private lateinit var editTextLogradouroDetalhe: EditText
    private lateinit var editTextBairroDetalhe: EditText

    // ViewModel para gerenciar os dados
    private lateinit var clienteViewModel: ClienteViewModel
    
    private var clienteId: Long = -1
    private var clienteAtual: Cliente? = null

    // Variáveis para armazenar os dados completos do endereço
    private var logradouroCliente: String? = null
    private var numeroCliente: String? = null
    private var complementoCliente: String? = null
    private var bairroCliente: String? = null
    private var municipioCliente: String? = null
    private var ufCliente: String? = null
    private var cepCliente: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente)

        // Inicializa o ViewModel
        clienteViewModel = ViewModelProvider(this)[ClienteViewModel::class.java]

        // Referencia todos os elementos da interface
        editTextNomeDetalhe = findViewById(R.id.editTextNomeDetalhe)
        editTextEmailDetalhe = findViewById(R.id.editTextEmailDetalhe)
        editTextTelefoneDetalhe = findViewById(R.id.editTextTelefoneDetalhe)
        editTextInformacoesAdicionaisDetalhe = findViewById(R.id.editTextInformacoesAdicionaisDetalhe)
        editTextCPFDetalhe = findViewById(R.id.editTextCPFDetalhe)
        editTextCNPJDetalhe = findViewById(R.id.editTextCNPJDetalhe)
        editTextNumeroSerialDetalhe = findViewById(R.id.editTextNumeroSerialDetalhe)
        buttonExcluirCliente = findViewById(R.id.textViewExcluirArtigo)
        buttonBloquearCliente = findViewById(R.id.buttonBloquearCliente)
        editTextLogradouroDetalhe = findViewById(R.id.editTextLogradouroDetalhe)
        editTextBairroDetalhe = findViewById(R.id.editTextBairroDetalhe)

        clienteId = intent.getLongExtra("id", -1)

        if (clienteId != -1L) {
            loadAndDisplayClientData(clienteId)
        }

        buttonExcluirCliente.setOnClickListener {
            confirmarExclusao()
        }

        buttonBloquearCliente.setOnClickListener {
            confirmarBloqueio()
        }
    }

    private fun loadAndDisplayClientData(id: Long) {
        clienteViewModel.getClienteById(id).observe(this) { cliente ->
            cliente?.let {
                clienteAtual = it
                displayClientData(it)
            }
        }
    }

    private fun displayClientData(cliente: Cliente) {
        // Carrega dados básicos
        editTextNomeDetalhe.setText(cliente.nome)
        editTextEmailDetalhe.setText(cliente.email)
        editTextTelefoneDetalhe.setText(cliente.telefone)
        editTextCPFDetalhe.setText(cliente.cpf)
        editTextCNPJDetalhe.setText(cliente.cnpj)
        editTextInformacoesAdicionaisDetalhe.setText(cliente.informacoesAdicionais)

        // Carrega dados de endereço
        logradouroCliente = cliente.logradouro
        numeroCliente = cliente.numero
        complementoCliente = cliente.complemento
        bairroCliente = cliente.bairro
        municipioCliente = cliente.municipio
        ufCliente = cliente.uf
        cepCliente = cliente.cep

        val enderecoCompleto = listOfNotNull(logradouroCliente, numeroCliente, complementoCliente).joinToString(", ")
        editTextLogradouroDetalhe.setText(enderecoCompleto)
        editTextBairroDetalhe.setText(listOfNotNull(bairroCliente, municipioCliente, ufCliente, cepCliente).joinToString(" - "))

        // Carrega número serial
        editTextNumeroSerialDetalhe.setText(cliente.numeroSerial)
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir este cliente? Esta ação não poderá ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                excluirCliente()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirCliente() {
        clienteAtual?.let { cliente ->
            clienteViewModel.deleteCliente(cliente)
            showToast("Cliente excluído com sucesso!")
            val resultIntent = Intent()
            resultIntent.putExtra("cliente_excluido", true)
            resultIntent.putExtra("cliente_id_excluido", clienteId)
            setResult(RESULT_OK, resultIntent)
            finish()
        } ?: run {
            showToast("Não é possível excluir. Cliente não salvo.")
        }
    }

    private fun confirmarBloqueio() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Bloqueio")
            .setMessage("Tem certeza que deseja bloquear este cliente? Ele será movido para a lista de bloqueados e removido da lista de clientes ativos.")
            .setPositiveButton("Bloquear") { _, _ ->
                bloquearCliente()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun bloquearCliente() {
        if (clienteId == -1L) {
            showToast("Salve o cliente antes de bloquear.")
            return
        }

        val nome = editTextNomeDetalhe.text.toString().trim()
        if (nome.isEmpty()) {
            showToast("O nome do cliente é obrigatório para bloquear.")
            return
        }

        // TODO: Implementar lógica de bloqueio usando Room
        // Por enquanto, apenas mostra uma mensagem
        showToast("Funcionalidade de bloqueio será implementada com Room")
    }

    override fun onBackPressed() {
        salvarDadosCliente()
        super.onBackPressed()
    }

    private fun salvarDadosCliente() {
        val nome = editTextNomeDetalhe.text.toString().trim()
        val email = editTextEmailDetalhe.text.toString().trim()
        val telefone = editTextTelefoneDetalhe.text.toString().trim()
        val informacoesAdicionais = editTextInformacoesAdicionaisDetalhe.text.toString().trim()
        val cpf = editTextCPFDetalhe.text.toString().trim()
        val cnpj = editTextCNPJDetalhe.text.toString().trim()

        if (nome.isNotEmpty() && clienteId != -1L) {
            val clienteAtualizado = clienteAtual?.copy(
                nome = nome,
                email = email,
                telefone = telefone,
                informacoesAdicionais = informacoesAdicionais,
                cpf = cpf,
                cnpj = cnpj
            )

            clienteAtualizado?.let { cliente ->
                clienteViewModel.updateCliente(cliente)
                val resultIntent = Intent()
                resultIntent.putExtra("cliente_atualizado", true)
                setResult(RESULT_OK, resultIntent)
                Log.d("ClienteActivityRoom", "Cliente $nome atualizado com sucesso ao sair/voltar!")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 