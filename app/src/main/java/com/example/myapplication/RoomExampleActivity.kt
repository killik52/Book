package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import database.AppDatabase
import database.entities.Cliente
import database.entities.Artigo
import database.entities.Fatura
import kotlinx.coroutines.launch

class RoomExampleActivity : AppCompatActivity() {
    
    private lateinit var database: AppDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Usando layout existente temporariamente
        
        // Inicializar o banco de dados Room
        database = AppDatabase.getDatabase(this)
        
        // Exemplo de uso do Room Database
        exemploUsoRoom()
    }
    
    private fun exemploUsoRoom() {
        lifecycleScope.launch {
            try {
                // Exemplo: Inserir um cliente
                val novoCliente = Cliente(
                    nome = "João Silva",
                    email = "joao@email.com",
                    telefone = "(11) 99999-9999",
                    informacoesAdicionais = "Cliente VIP",
                    cpf = "123.456.789-00",
                    cnpj = null,
                    logradouro = "Rua das Flores",
                    numero = "123",
                    complemento = "Apto 45",
                    bairro = "Centro",
                    municipio = "São Paulo",
                    uf = "SP",
                    cep = "01234-567",
                    numeroSerial = "CLI001"
                )
                
                val clienteId = database.clienteDao().insertCliente(novoCliente)
                Log.d("RoomExample", "Cliente inserido com ID: $clienteId")
                
                // Exemplo: Inserir um segundo cliente
                val segundoCliente = Cliente(
                    nome = "Maria Santos",
                    email = "maria@email.com",
                    telefone = "(11) 88888-8888",
                    informacoesAdicionais = "Cliente Regular",
                    cpf = "987.654.321-00",
                    cnpj = null,
                    logradouro = "Av. Paulista",
                    numero = "456",
                    complemento = "Sala 10",
                    bairro = "Bela Vista",
                    municipio = "São Paulo",
                    uf = "SP",
                    cep = "01310-000",
                    numeroSerial = "CLI002"
                )
                
                val segundoClienteId = database.clienteDao().insertCliente(segundoCliente)
                Log.d("RoomExample", "Segundo cliente inserido com ID: $segundoClienteId")
                
                // Exemplo: Inserir um artigo
                val novoArtigo = Artigo(
                    nome = "Produto A",
                    preco = 29.99,
                    quantidade = 10,
                    desconto = 0.0,
                    descricao = "Descrição do produto A",
                    guardarFatura = true,
                    numeroSerial = "ART001"
                )
                
                val artigoId = database.artigoDao().insertArtigo(novoArtigo)
                Log.d("RoomExample", "Artigo inserido com ID: $artigoId")
                
                // Exemplo: Inserir um segundo artigo
                val segundoArtigo = Artigo(
                    nome = "Produto B",
                    preco = 49.99,
                    quantidade = 5,
                    desconto = 5.0,
                    descricao = "Descrição do produto B",
                    guardarFatura = true,
                    numeroSerial = "ART002"
                )
                
                val segundoArtigoId = database.artigoDao().insertArtigo(segundoArtigo)
                Log.d("RoomExample", "Segundo artigo inserido com ID: $segundoArtigoId")
                
                // Exemplo: Inserir uma fatura
                val novaFatura = Fatura(
                    numeroFatura = "FAT001",
                    cliente = "João Silva",
                    artigos = "Produto A",
                    subtotal = 299.90,
                    desconto = 0.0,
                    descontoPercent = 0,
                    taxaEntrega = 10.0,
                    saldoDevedor = 309.90,
                    data = "2024-01-15",
                    fotosImpressora = null,
                    notas = "Fatura de exemplo",
                    foiEnviada = false
                )
                
                val faturaId = database.faturaDao().insertFatura(novaFatura)
                Log.d("RoomExample", "Fatura inserida com ID: $faturaId")
                
                // Exemplo: Inserir uma segunda fatura
                val segundaFatura = Fatura(
                    numeroFatura = "FAT002",
                    cliente = "Maria Santos",
                    artigos = "Produto B",
                    subtotal = 249.95,
                    desconto = 12.50,
                    descontoPercent = 5,
                    taxaEntrega = 15.0,
                    saldoDevedor = 252.45,
                    data = "2024-01-16",
                    fotosImpressora = null,
                    notas = "Segunda fatura de exemplo",
                    foiEnviada = true
                )
                
                val segundaFaturaId = database.faturaDao().insertFatura(segundaFatura)
                Log.d("RoomExample", "Segunda fatura inserida com ID: $segundaFaturaId")
                
            } catch (e: Exception) {
                Log.e("RoomExample", "Erro ao usar Room Database", e)
            }
        }
        
        // Observação para clientes (LiveData)
        database.clienteDao().getAllClientes().observe(this) { clientes: List<Cliente> ->
            Log.d("RoomExample", "Total de clientes: ${clientes.size}")
            clientes.forEach { cliente: Cliente ->
                Log.d("RoomExample", "Cliente: ${cliente.nome} - ${cliente.email}")
            }
        }
        
        // Observação para artigos (Flow)
        lifecycleScope.launch {
            database.artigoDao().getAllArtigos().collect { artigos: List<Artigo> ->
                Log.d("RoomExample", "Total de artigos: ${artigos.size}")
                artigos.forEach { artigo: Artigo ->
                    Log.d("RoomExample", "Artigo: ${artigo.nome} - R$ ${artigo.preco}")
                }
            }
        }
        
        // Observação para faturas (Flow)
        lifecycleScope.launch {
            database.faturaDao().getAllFaturas().collect { faturas: List<Fatura> ->
                Log.d("RoomExample", "Total de faturas: ${faturas.size}")
                faturas.forEach { fatura: Fatura ->
                    Log.d("RoomExample", "Fatura: ${fatura.numeroFatura} - R$ ${fatura.subtotal}")
                }
            }
        }
    }

    private fun carregarClientes() {
        // TODO: Implementar quando ClienteViewModel estiver completo
        Log.d("RoomExampleActivity", "Carregamento de clientes será implementado")
    }

    private fun carregarArtigos() {
        // TODO: Implementar quando ArtigoViewModel estiver completo
        Log.d("RoomExampleActivity", "Carregamento de artigos será implementado")
    }
} 