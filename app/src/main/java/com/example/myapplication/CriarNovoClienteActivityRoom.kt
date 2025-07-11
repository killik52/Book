package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityCriarNovoClienteBinding
import com.example.myapplication.model.CnpjData
import database.entities.Cliente
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CriarNovoClienteActivityRoom : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoClienteBinding
    private lateinit var clienteViewModel: ClienteViewModel
    private var clienteId: Long = -1

    private lateinit var cnpjTextWatcher: TextWatcher
    private lateinit var cpfTextWatcher: TextWatcher
    private lateinit var cepTextWatcher: TextWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCriarNovoClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("CriarNovoClienteActivityRoom", "onCreate chamado")

        // Inicializa o ViewModel
        clienteViewModel = ViewModelProvider(this)[ClienteViewModel::class.java]

        clienteId = intent.getLongExtra("cliente_id", -1)
        if (clienteId != -1L) {
            // Modo de edição - carregar dados do cliente
            carregarDadosCliente(clienteId)
        }

        setupInputMasks()

        binding.textViewGuardarCliente.setOnClickListener {
            salvarCliente()
        }
    }

    private fun carregarDadosCliente(id: Long) {
        clienteViewModel.getClienteById(id).observe(this) { cliente ->
            cliente?.let {
                binding.editTextNomeCliente.setText(it.nome)
                binding.editTextEmailCliente.setText(it.email)
                binding.editTextTelefoneCliente.setText(it.telefone)
                binding.editTextInformacoesAdicionais.setText(it.informacoesAdicionais)
                binding.editTextCNPJCliente.setText(it.cnpj)
                binding.editTextCPFCliente.setText(it.cpf)
                binding.editTextLogradouro.setText(it.logradouro)
                binding.editTextNumero.setText(it.numero)
                binding.editTextComplemento.setText(it.complemento)
                binding.editTextBairro.setText(it.bairro)
                binding.editTextMunicipio.setText(it.municipio)
                binding.editTextUF.setText(it.uf)
                binding.editTextCEP.setText(it.cep)
                Log.d("CriarNovoClienteActivityRoom", "Dados do cliente carregados: ${it.nome}")
            }
        }
    }

    private fun setupInputMasks() {
        // Máscara para CNPJ
        cnpjTextWatcher = object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                isUpdating = true
                val unmasked = s.toString().replace("[^0-9]".toRegex(), "")
                
                // Formatação simples e direta do CNPJ
                val formatted = when {
                    unmasked.length > 12 -> "${unmasked.substring(0, 2)}.${unmasked.substring(2, 5)}.${unmasked.substring(5, 8)}/${unmasked.substring(8, 12)}-${unmasked.substring(12)}"
                    unmasked.length > 8 -> "${unmasked.substring(0, 2)}.${unmasked.substring(2, 5)}.${unmasked.substring(5, 8)}/${unmasked.substring(8)}"
                    unmasked.length > 5 -> "${unmasked.substring(0, 2)}.${unmasked.substring(2, 5)}.${unmasked.substring(5)}"
                    unmasked.length > 2 -> "${unmasked.substring(0, 2)}.${unmasked.substring(2)}"
                    else -> unmasked
                }

                binding.editTextCNPJCliente.setText(formatted)
                binding.editTextCNPJCliente.setSelection(formatted.length)
                isUpdating = false
                
                if (unmasked.length == 14) {
                    Log.d("CriarNovoClienteActivityRoom", "CNPJ completo digitado: $unmasked, chamando buscarDadosCnpj")
                    buscarDadosCnpj(unmasked)
                }
            }
        }
        binding.editTextCNPJCliente.addTextChangedListener(cnpjTextWatcher)

        // Máscara para CPF
        cpfTextWatcher = object : TextWatcher {
            private var isUpdating = false
            private var old = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().replace("[^0-9]".toRegex(), "")
                if (isUpdating || str == old) {
                    return
                }
                isUpdating = true
                val mascara = "###.###.###-##"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if (m != '#' && str.length > old.length) {
                        novaString.append(m)
                        continue
                    }
                    try {
                        novaString.append(str[i])
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }
                binding.editTextCPFCliente.setText(novaString.toString())
                old = str
                isUpdating = false
            }
        }
        binding.editTextCPFCliente.addTextChangedListener(cpfTextWatcher)

        // Máscara para CEP
        cepTextWatcher = object : TextWatcher {
            private var isUpdating = false
            private var old = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().replace("[^0-9]".toRegex(), "")
                if (isUpdating || str == old) {
                    return
                }
                isUpdating = true
                val mascara = "#####-###"
                var i = 0
                val novaString = StringBuilder()
                for (m in mascara.toCharArray()) {
                    if (m != '#' && str.length > old.length) {
                        novaString.append(m)
                        continue
                    }
                    try {
                        novaString.append(str[i])
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }
                binding.editTextCEP.setText(novaString.toString())
                old = str
                isUpdating = false
            }
        }
        binding.editTextCEP.addTextChangedListener(cepTextWatcher)
    }

    private fun salvarCliente() {
        val nome = binding.editTextNomeCliente.text.toString().trim()
        val email = binding.editTextEmailCliente.text.toString().trim()
        val telefone = binding.editTextTelefoneCliente.text.toString().trim()
        val informacoesAdicionais = binding.editTextInformacoesAdicionais.text.toString().trim()
        val cpf = binding.editTextCPFCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
        val cnpj = binding.editTextCNPJCliente.text.toString().replace("[^0-9]".toRegex(), "").trim()
        val logradouro = binding.editTextLogradouro.text.toString().trim()
        val numero = binding.editTextNumero.text.toString().trim()
        val complemento = binding.editTextComplemento.text.toString().trim()
        val bairro = binding.editTextBairro.text.toString().trim()
        val municipio = binding.editTextMunicipio.text.toString().trim()
        val uf = binding.editTextUF.text.toString().trim()
        val cep = binding.editTextCEP.text.toString().replace("[^0-9]".toRegex(), "").trim()

        if (nome.isEmpty()) {
            showToast("Por favor, insira o nome do cliente.")
            return
        }

        val cliente = Cliente(
            id = if (clienteId != -1L) clienteId else 0,
            nome = nome,
            email = email,
            telefone = telefone,
            informacoesAdicionais = informacoesAdicionais,
            cpf = cpf,
            cnpj = cnpj,
            logradouro = logradouro,
            numero = numero,
            complemento = complemento,
            bairro = bairro,
            municipio = municipio,
            uf = uf,
            cep = cep,
            numeroSerial = null
        )

        if (clienteId != -1L) {
            // Atualizar cliente existente
            clienteViewModel.updateCliente(cliente)
            showToast("Cliente atualizado com sucesso!")
            Log.d("CriarNovoClienteActivityRoom", "Cliente atualizado: $nome")
            
            val resultIntent = Intent().apply {
                putExtra("cliente_salvo", true)
                putExtra("nome_cliente", nome)
                putExtra("cliente_id", clienteId)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            // Inserir novo cliente
            clienteViewModel.insertCliente(cliente).observe(this) { idClienteInserido ->
                if (idClienteInserido != -1L) {
                    showToast("Cliente salvo com sucesso!")
                    Log.d("CriarNovoClienteActivityRoom", "Novo cliente salvo: $nome com ID: $idClienteInserido")
                    
                    val resultIntent = Intent().apply {
                        putExtra("cliente_salvo", true)
                        putExtra("nome_cliente", nome)
                        putExtra("cliente_id", idClienteInserido)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    showToast("Erro ao salvar o cliente.")
                    Log.e("CriarNovoClienteActivityRoom", "Erro ao inserir cliente: ID retornado foi -1")
                }
            }
        }
    }

    override fun onBackPressed() {
        val nome = binding.editTextNomeCliente.text.toString().trim()
        if (nome.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Salvar Cliente")
                .setMessage("Deseja salvar as alterações antes de sair?")
                .setPositiveButton("Sim") { _, _ ->
                    salvarCliente()
                }
                .setNegativeButton("Não") { _, _ ->
                    super.onBackPressed()
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun buscarDadosCnpj(cnpj: String) {
        RetrofitClient.cnpjApiService.getCnpjData(cnpj).enqueue(object : Callback<CnpjData> {
            override fun onResponse(call: Call<CnpjData>, response: Response<CnpjData>) {
                if (response.isSuccessful) {
                    val cnpjData = response.body()
                    if (cnpjData?.status == "OK") {
                        binding.editTextNomeCliente.setText(cnpjData.nome ?: "")
                        binding.editTextEmailCliente.setText(cnpjData.email ?: "")
                        binding.editTextTelefoneCliente.setText(cnpjData.telefone ?: "")
                        binding.editTextLogradouro.setText(cnpjData.logradouro ?: "")
                        binding.editTextNumero.setText(cnpjData.numero ?: "")
                        binding.editTextComplemento.setText(cnpjData.complemento ?: "")
                        binding.editTextBairro.setText(cnpjData.bairro ?: "")
                        binding.editTextMunicipio.setText(cnpjData.municipio ?: "")
                        binding.editTextUF.setText(cnpjData.uf ?: "")

                        val cepSemFormatacao = cnpjData.cep?.replace("[^0-9]".toRegex(), "") ?: ""
                        binding.editTextCEP.removeTextChangedListener(cepTextWatcher)
                        binding.editTextCEP.setText(cepSemFormatacao)
                        binding.editTextCEP.addTextChangedListener(cepTextWatcher)

                        showToast("Dados do CNPJ carregados com sucesso!")
                        Log.d("CriarNovoClienteActivityRoom", "Dados do CNPJ carregados: Nome=${cnpjData.nome}")
                    } else {
                        showToast("CNPJ inválido ou não encontrado: ${cnpjData?.mensagem}")
                        Log.w("CriarNovoClienteActivityRoom", "CNPJ inválido: ${cnpjData?.mensagem}")
                    }
                } else {
                    showToast("Erro na resposta da API: ${response.message()}")
                    Log.e("CriarNovoClienteActivityRoom", "Erro na resposta da API: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<CnpjData>, t: Throwable) {
                Log.e("CriarNovoClienteActivityRoom", "Erro ao buscar CNPJ: ${t.message}")
                showToast("Erro ao buscar CNPJ: ${t.message}")
            }
        })
    }
}
