package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityCriarNovoArtigoBinding
import database.entities.Artigo
import database.entities.ClienteBloqueado
import database.repository.ArtigoRepository
import database.AppDatabase
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class CriarNovoArtigoActivityRoom : AppCompatActivity() {

    private lateinit var binding: ActivityCriarNovoArtigoBinding
    private lateinit var artigoViewModel: ArtigoViewModel
    private lateinit var clienteBloqueadoViewModel: ClienteBloqueadoViewModel
    private var artigoId: Long = -1
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })
    private var cameraImageUri: Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            processImageForOcr(cameraImageUri!!)
        }
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processImageForOcr(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityCriarNovoArtigoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("CriarNovoArtigoActivityRoom", "🚀 onCreate iniciado")

            // Inicializa os ViewModels com tratamento de erro
            try {
                val database = database.AppDatabase.getDatabase(this)
                val artigoRepository = ArtigoRepository(database.artigoDao())
                val artigoFactory = ArtigoViewModelFactory(artigoRepository)
                artigoViewModel = ViewModelProvider(this, artigoFactory)[ArtigoViewModel::class.java]
                
                // Inicializar ClienteBloqueadoViewModel
                clienteBloqueadoViewModel = ViewModelProvider(this)[ClienteBloqueadoViewModel::class.java]
                Log.d("CriarNovoArtigo", "✅ ViewModels inicializados com sucesso")
            } catch (e: Exception) {
                Log.e("CriarNovoArtigo", "❌ Erro ao inicializar ViewModels: ${e.message}")
                e.printStackTrace()
                showToast("Erro ao inicializar componentes. A tela permanecerá aberta para edição.")
                // Tentar inicializar apenas o essencial
                try {
                    val database = database.AppDatabase.getDatabase(this)
                    val artigoRepository = ArtigoRepository(database.artigoDao())
                    val artigoFactory = ArtigoViewModelFactory(artigoRepository)
                    artigoViewModel = ViewModelProvider(this, artigoFactory)[ArtigoViewModel::class.java]
                    Log.d("CriarNovoArtigo", "✅ ArtigoViewModel inicializado com sucesso")
                } catch (e2: Exception) {
                    Log.e("CriarNovoArtigo", "❌ Erro crítico ao inicializar ArtigoViewModel: ${e2.message}")
                    showToast("Erro crítico. A tela permanecerá aberta com limitações.")
                }
            }

            artigoId = intent.getLongExtra("artigo_id", -1)
            Log.d("CriarNovoArtigo", "📥 Recebido artigo_id: $artigoId")

            // Log detalhado de todos os extras recebidos
            Log.d("CriarNovoArtigo", "📋 === EXTRAS RECEBIDOS ===")
            Log.d("CriarNovoArtigo", "📋 artigo_id: ${intent.getLongExtra("artigo_id", -1)}")
            Log.d("CriarNovoArtigo", "📋 nome_artigo: ${intent.getStringExtra("nome_artigo")}")
            Log.d("CriarNovoArtigo", "📋 quantidade_fatura: ${intent.getIntExtra("quantidade_fatura", -1)}")
            Log.d("CriarNovoArtigo", "📋 valor: ${intent.getDoubleExtra("valor", -1.0)}")
            Log.d("CriarNovoArtigo", "📋 numero_serial: ${intent.getStringExtra("numero_serial")}")
            Log.d("CriarNovoArtigo", "📋 descricao: ${intent.getStringExtra("descricao")}")
            Log.d("CriarNovoArtigo", "📋 ========================")

            // Sempre carregar dados dos extras primeiro (se existirem)
            val nomeExtra = intent.getStringExtra("nome_artigo")
            val quantidadeExtra = intent.getIntExtra("quantidade_fatura", -1)
            val precoExtra = intent.getDoubleExtra("valor", -1.0)
            val numeroSerialExtra = intent.getStringExtra("numero_serial")
            val descricaoExtra = intent.getStringExtra("descricao")

            Log.d("CriarNovoArtigo", "📊 Extras processados: nome=$nomeExtra, qtd=$quantidadeExtra, preco=$precoExtra, serial=$numeroSerialExtra, desc=$descricaoExtra")

            // Estratégia de carregamento: sempre garantir que os campos sejam preenchidos
            if (artigoId != -1L && artigoId > 0) {
                // Artigo existente no banco - tentar carregar do banco primeiro
                Log.d("CriarNovoArtigo", "🏪 Artigo ID $artigoId válido, tentando carregar do banco")
                binding.textViewArtigoTitolo.text = "Editar Artigo"
                
                // Carregar do banco, mas com fallback para extras
                loadArtigoDataWithFallback(artigoId, nomeExtra, quantidadeExtra, precoExtra, numeroSerialExtra, descricaoExtra)
            } else {
                // Artigo novo ou temporário - usar dados dos extras
                Log.d("CriarNovoArtigo", "🆕 Artigo ID $artigoId inválido, usando dados dos extras")
                binding.textViewArtigoTitolo.text = "Editar Artigo"
                
                // Preencher campos com dados dos extras
                carregarDadosDosExtras()
            }

            // Garantir que a atividade não feche automaticamente
            Log.d("CriarNovoArtigo", "🔒 Configurando listeners e garantindo que atividade permaneça aberta")
            
            try {
                val sharedPreferences = getSharedPreferences("DefinicoesGuardarArtigo", MODE_PRIVATE)
                val guardarArtigoPadrao = sharedPreferences.getBoolean("guardar_artigo_padrao", true)
                binding.switchGuardarFatura.isChecked = guardarArtigoPadrao
                Log.d("CriarNovoArtigo", "⚙️ SwitchGuardarArtigo definido para: $guardarArtigoPadrao")
            } catch (e: Exception) {
                Log.e("CriarNovoArtigo", "❌ Erro ao carregar preferências: ${e.message}")
                binding.switchGuardarFatura.isChecked = true
            }

            atualizarValorTotal()

            // Configurar listeners com tratamento de erro
            try {
                binding.editTextPreco.addTextChangedListener(object : TextWatcher {
                    private var current = ""
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString() != current) {
                            binding.editTextPreco.removeTextChangedListener(this)
                            val cleanString = s.toString().replace(Regex("[R$\\s,.]"), "")
                            if (cleanString.isNotEmpty()) {
                                try {
                                    val parsed = cleanString.toDouble() / 100.0
                                    val formatted = decimalFormat.format(parsed)
                                    current = "R$ $formatted"
                                    binding.editTextPreco.setText(current)
                                    binding.editTextPreco.setSelection(current.length)
                                } catch (e: NumberFormatException) {
                                    Log.e("CriarNovoArtigo", "Erro ao formatar preço: ${e.message}, input: $cleanString")
                                    current = s.toString()
                                    binding.editTextPreco.setText(current)
                                    binding.editTextPreco.setSelection(current.length)
                                }
                            } else {
                                current = ""
                                binding.editTextPreco.setText("")
                            }
                            binding.editTextPreco.addTextChangedListener(this)
                            atualizarValorTotal()
                        }
                    }
                })

                binding.editTextQtd.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        atualizarValorTotal()
                    }
                })

                binding.textViewGuardarArtigo.setOnClickListener {
                    salvarArtigo()
                }

                binding.buttonExcluirArtigo.setOnClickListener {
                    if (artigoId != -1L) {
                        AlertDialog.Builder(this)
                            .setTitle("Excluir Artigo dos Recentes")
                            .setMessage("Tem certeza que deseja excluir este artigo da lista de itens recentes? Ele não será removido de faturas já existentes.")
                            .setPositiveButton("Excluir dos Recentes") { _, _ ->
                                excluirArtigoDoBanco(artigoId)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        showToast("Este artigo ainda não foi salvo nos recentes.")
                    }
                }

                binding.textViewAddFoto.setOnClickListener {
                    val options = arrayOf("Tirar foto", "Escolher da galeria")
                    AlertDialog.Builder(this)
                        .setTitle("Adicionar imagem")
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> { // Tirar foto
                                    if (checkCameraPermission()) {
                                        openCameraForOcr()
                                    } else {
                                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
                                    }
                                }
                                1 -> { // Galeria
                                    openGalleryForOcr()
                                }
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
                
                Log.d("CriarNovoArtigo", "✅ Listeners configurados com sucesso")
            } catch (e: Exception) {
                Log.e("CriarNovoArtigo", "❌ Erro ao configurar listeners: ${e.message}")
                e.printStackTrace()
                showToast("Erro ao configurar interface. A tela permanecerá aberta.")
            }
            
            Log.d("CriarNovoArtigo", "✅ onCreate concluído com sucesso")
            
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "❌ ERRO CRÍTICO no onCreate: ${e.message}")
            e.printStackTrace()
            showToast("Erro ao inicializar a tela: ${e.message}")
            // NÃO fechar a tela automaticamente, apenas mostrar o erro
            // Tentar inicializar pelo menos o básico
            try {
                if (!::binding.isInitialized) {
                    binding = ActivityCriarNovoArtigoBinding.inflate(layoutInflater)
                    setContentView(binding.root)
                }
                binding.textViewArtigoTitolo.text = "Editar Artigo"
                showToast("Tela inicializada com limitações. Você pode tentar editar.")
            } catch (e2: Exception) {
                Log.e("CriarNovoArtigo", "❌ ERRO FATAL: Não foi possível inicializar nem o básico: ${e2.message}")
                showToast("Erro crítico. A tela permanecerá aberta para tentar recuperar.")
            }
        }
    }

    private fun excluirArtigoDoBanco(id: Long) {
        try {
            // Busca o artigo e atualiza o flag guardarFatura
            artigoViewModel.getArtigoById(id,
                onSuccess = { artigo ->
                    artigo?.let { artigoEncontrado ->
                        val artigoAtualizado = artigoEncontrado.copy(guardarFatura = false)
                        artigoViewModel.updateArtigo(artigoAtualizado,
                            onSuccess = {
                                val resultIntent = Intent().apply {
                                    putExtra("artigo_id", id)
                                    putExtra("salvar_fatura", false)
                                    putExtra("nome_artigo", binding.editTextNome.text.toString().trim())
                                    putExtra("quantidade", binding.editTextQtd.text.toString().trim().toIntOrNull() ?: 1)
                                    val precoUnitario = try {
                                        normalizeInput(binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")).toDouble()
                                    } catch (e: Exception) { 0.0 }
                                    putExtra("valor", precoUnitario * (binding.editTextQtd.text.toString().trim().toIntOrNull() ?: 1))
                                    putExtra("preco_unitario_artigo", precoUnitario)
                                    putExtra("numero_serial", binding.editTextNumeroSerial.text.toString().trim())
                                    putExtra("descricao", binding.editTextDescricao.text.toString().trim())
                                }
                                setResult(Activity.RESULT_OK, resultIntent)
                                // NÃO fechar automaticamente - deixar o usuário decidir quando sair
                                Log.d("CriarNovoArtigo", "Artigo ID $id removido dos recentes. Tela permanecerá aberta.")
                                showToast("Artigo removido dos itens recentes. A tela permanecerá aberta para edição.")
                            },
                            onError = { exception ->
                                showToast("Erro ao remover artigo dos recentes: ${exception.message}")
                                Log.e("CriarNovoArtigo", "Erro ao remover artigo ID $id dos recentes: ${exception.message}")
                            }
                        )
                    }
                },
                onError = { exception ->
                    showToast("Erro ao buscar artigo: ${exception.message}")
                    Log.e("CriarNovoArtigo", "Erro ao buscar artigo ID $id: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            showToast("Erro ao remover artigo dos recentes: ${e.message}")
            Log.e("CriarNovoArtigo", "Erro ao remover artigo ID $id dos recentes: ${e.message}")
        }
    }

    private fun salvarArtigo() {
        Log.d("CriarNovoArtigo", "🔄 === INICIANDO salvarArtigo ===")
        val nome = binding.editTextNome.text.toString().trim()
        val precoStrInput = binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")
        val quantidadeStr = binding.editTextQtd.text.toString().trim()
        val numeroSerial = binding.editTextNumeroSerial.text.toString().trim()
        val descricao = binding.editTextDescricao.text.toString().trim()
        val guardarParaRecentes = binding.switchGuardarFatura.isChecked

        Log.d("CriarNovoArtigo", "📝 Dados para salvar: nome='$nome', preco='$precoStrInput', qtd='$quantidadeStr', guardar=$guardarParaRecentes")

        if (nome.isEmpty()) {
            showToast("Por favor, insira o nome do artigo.")
            return
        }

        val quantidade = if (quantidadeStr.isNotEmpty()) {
            try {
                quantidadeStr.toInt().coerceAtLeast(1)
            } catch (e: NumberFormatException) {
                showToast("Quantidade inválida.")
                return
            }
        } else {
            showToast("A quantidade é obrigatória.")
            return
        }

        val precoUnitario = try {
            val normalizedPreco = normalizeInput(precoStrInput)
            normalizedPreco.toDouble().coerceAtLeast(0.0)
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "Erro ao parsear preço em salvar: ${e.message}, input: $precoStrInput")
            showToast("Preço inválido.")
            return
        }

        Log.d("CriarNovoArtigo", "Salvando: nome=$nome, qtd=$quantidade, precoUnit=$precoUnitario, guardar=$guardarParaRecentes, artigoId=$artigoId")

        val valorTotalItem = precoUnitario * quantidade
        var idParaRetorno = artigoId

        // Se o artigo já existe (artigoId válido), atualizar no banco
        if (artigoId != -1L && artigoId > 0) {
            Log.d("CriarNovoArtigo", "Artigo já existe (ID: $artigoId), atualizando no banco")
            
            // Buscar o artigo existente e atualizar
            artigoViewModel.getArtigoById(artigoId,
                onSuccess = { artigoExistente ->
                    Log.d("CriarNovoArtigo", "Callback onSuccess getArtigoById chamado")
                    artigoExistente?.let { artigo ->
                        val artigoAtualizado = artigo.copy(
                            nome = nome,
                            preco = precoUnitario,
                            quantidade = quantidade,
                            descricao = descricao,
                            numeroSerial = numeroSerial,
                            guardarFatura = guardarParaRecentes
                        )
                        
                        artigoViewModel.updateArtigo(artigoAtualizado,
                            onSuccess = {
                                Log.d("CriarNovoArtigo", "Callback onSuccess updateArtigo chamado")
                                idParaRetorno = artigoId
                                showToast("Artigo atualizado com sucesso!")
                                Log.d("CriarNovoArtigo", "Artigo ID $artigoId atualizado no banco")
                                
                                // Retornar resultado após atualizar
                                Log.d("CriarNovoArtigo", "Chamando retornarResultado após atualizar")
                                retornarResultado(idParaRetorno, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                            },
                            onError = { exception ->
                                Log.e("CriarNovoArtigo", "Callback onError updateArtigo chamado: ${exception.message}")
                                showToast("Erro ao atualizar artigo: ${exception.message}")
                                // Mesmo com erro, retornar com dados atualizados
                                Log.d("CriarNovoArtigo", "Chamando retornarResultado mesmo com erro")
                                retornarResultado(artigoId, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                            }
                        )
                    } ?: run {
                        Log.w("CriarNovoArtigo", "Artigo ID $artigoId não encontrado no banco")
                        showToast("Artigo não encontrado no banco")
                        // Retornar com ID temporário
                        Log.d("CriarNovoArtigo", "Chamando retornarResultado com ID temporário")
                        retornarResultado(-System.currentTimeMillis(), nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                    }
                },
                onError = { exception ->
                    Log.e("CriarNovoArtigo", "Callback onError getArtigoById chamado: ${exception.message}")
                    showToast("Erro ao buscar artigo: ${exception.message}")
                    // Mesmo com erro, retornar com dados atualizados
                    Log.d("CriarNovoArtigo", "Chamando retornarResultado mesmo com erro de busca")
                    retornarResultado(artigoId, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                }
            )
        } else {
            // Artigo novo - criar no banco apenas se guardarParaRecentes for true
            if (guardarParaRecentes) {
                try {
                    val novoArtigo = Artigo(
                        id = 0, // Room irá gerar o ID
                        nome = nome,
                        preco = precoUnitario,
                        quantidade = 1,
                        desconto = 0.0,
                        descricao = descricao,
                        guardarFatura = true,
                        numeroSerial = numeroSerial
                    )
                    artigoViewModel.insertArtigo(novoArtigo,
                        onSuccess = { id ->
                            Log.d("CriarNovoArtigo", "Callback onSuccess insertArtigo chamado com ID: $id")
                            idParaRetorno = id
                            showToast("Novo artigo salvo e guardado para recentes!")
                            Log.d("CriarNovoArtigo", "Chamando retornarResultado após inserir")
                            retornarResultado(idParaRetorno, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                        },
                        onError = { exception ->
                            Log.e("CriarNovoArtigo", "Callback onError insertArtigo chamado: ${exception.message}")
                            showToast("Erro ao salvar artigo")
                            // Mesmo com erro, continuar com ID temporário
                            idParaRetorno = -System.currentTimeMillis()
                            Log.d("CriarNovoArtigo", "Chamando retornarResultado com ID temporário após erro")
                            retornarResultado(idParaRetorno, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("CriarNovoArtigo", "Erro ao salvar artigo no DB: ${e.message}")
                    showToast("Erro ao interagir com o banco de dados para 'Recentes'.")
                    idParaRetorno = -System.currentTimeMillis()
                    Log.d("CriarNovoArtigo", "Chamando retornarResultado com ID temporário após exceção")
                    retornarResultado(idParaRetorno, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
                }
            } else {
                // Artigo novo mas não guardar para recentes
                idParaRetorno = -System.currentTimeMillis()
                showToast("Artigo será usado apenas na fatura atual.")
                Log.d("CriarNovoArtigo", "Chamando retornarResultado para artigo temporário")
                retornarResultado(idParaRetorno, nome, quantidade, valorTotalItem, precoUnitario, numeroSerial, guardarParaRecentes, descricao)
            }
        }
        Log.d("CriarNovoArtigo", "🔄 === FIM salvarArtigo ===")
    }

    private fun retornarResultado(idParaRetorno: Long, nome: String, quantidade: Int, valorTotalItem: Double, precoUnitario: Double, numeroSerial: String?, guardarParaRecentes: Boolean, descricao: String?) {
        Log.d("CriarNovoArtigo", "🔄 === INICIANDO retornarResultado ===")
        Log.d("CriarNovoArtigo", "🔄 Parâmetros: id=$idParaRetorno, nome='$nome', qtd=$quantidade, valor=$valorTotalItem")
        Log.d("CriarNovoArtigo", "🔄 artigoId atual: $artigoId, idParaRetorno: $idParaRetorno, guardarParaRecentes: $guardarParaRecentes")
        
        val resultIntent = Intent().apply {
            putExtra("artigo_id", idParaRetorno)
            putExtra("nome_artigo", nome)
            putExtra("quantidade", quantidade)
            putExtra("valor", valorTotalItem)
            putExtra("preco_unitario_artigo", precoUnitario)
            putExtra("numero_serial", numeroSerial)
            putExtra("salvar_fatura", guardarParaRecentes)
            putExtra("descricao", descricao)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        
        Log.d("CriarNovoArtigo", "🔄 === DECISÃO DE NAVEGAÇÃO ===")
        Log.d("CriarNovoArtigo", "🔄 Condição 1 - artigoId > 0: ${artigoId > 0}")
        Log.d("CriarNovoArtigo", "🔄 Condição 2 - idParaRetorno > 0: ${idParaRetorno > 0}")
        Log.d("CriarNovoArtigo", "🔄 Condição 3 - guardarParaRecentes: $guardarParaRecentes")
        
        // LÓGICA MELHORADA: Considerar o contexto completo
        val isEditandoArtigoExistente = artigoId > 0
        val isNovoArtigoSalvoNoBanco = idParaRetorno > 0 && guardarParaRecentes
        val isNovoArtigoTemporario = idParaRetorno <= 0 && !guardarParaRecentes
        
        Log.d("CriarNovoArtigo", "🔄 isEditandoArtigoExistente: $isEditandoArtigoExistente")
        Log.d("CriarNovoArtigo", "🔄 isNovoArtigoSalvoNoBanco: $isNovoArtigoSalvoNoBanco")
        Log.d("CriarNovoArtigo", "🔄 isNovoArtigoTemporario: $isNovoArtigoTemporario")
        
        // DECISÃO FINAL: Fechar a tela nos casos apropriados
        if (isEditandoArtigoExistente) {
            Log.d("CriarNovoArtigo", "🔄 DECISÃO: Editando artigo existente (ID: $artigoId), fechando tela e voltando para SecondScreen")
            showToast("Artigo atualizado e retornando para SecondScreen!")
            Log.d("CriarNovoArtigo", "🔄 Chamando finish()...")
            finish()
            Log.d("CriarNovoArtigo", "🔄 finish() chamado com sucesso")
        } else if (isNovoArtigoSalvoNoBanco) {
            Log.d("CriarNovoArtigo", "🔄 DECISÃO: Novo artigo salvo no banco, fechando tela e voltando para SecondScreen")
            showToast("Artigo salvo e retornando para SecondScreen!")
            Log.d("CriarNovoArtigo", "🔄 Chamando finish()...")
            finish()
            Log.d("CriarNovoArtigo", "🔄 finish() chamado com sucesso")
        } else if (idParaRetorno > 0) {
            // Artigo novo com ID válido (mesmo que guardarParaRecentes seja false)
            Log.d("CriarNovoArtigo", "🔄 DECISÃO: Artigo novo com ID válido, fechando tela e voltando para SecondScreen")
            showToast("Artigo salvo e retornando para SecondScreen!")
            Log.d("CriarNovoArtigo", "🔄 Chamando finish()...")
            finish()
            Log.d("CriarNovoArtigo", "🔄 finish() chamado com sucesso")
        } else {
            // Apenas artigos temporários permanecem abertos
            Log.d("CriarNovoArtigo", "🔄 DECISÃO: Artigo temporário, tela permanecerá aberta para continuar criando")
            Log.d("CriarNovoArtigo", "🔄 NÃO chamando finish() - tela permanecerá aberta")
        }
        
        Log.d("CriarNovoArtigo", "🔄 === FIM retornarResultado ===")
    }

    private fun loadArtigoDataWithFallback(id: Long, nomeExtra: String?, quantidadeExtra: Int, precoExtra: Double, numeroSerialExtra: String?, descricaoExtra: String?) {
        Log.d("CriarNovoArtigo", "=== INICIANDO loadArtigoDataWithFallback ===")
        Log.d("CriarNovoArtigo", "Tentando carregar artigo ID $id do banco com fallback para extras")
        Log.d("CriarNovoArtigo", "Parâmetros: nomeExtra='$nomeExtra', qtd=$quantidadeExtra, preco=$precoExtra, serial='$numeroSerialExtra', desc='$descricaoExtra'")
        
        // ATENÇÃO: NUNCA feche a tela automaticamente aqui! Mesmo que não encontre o artigo, mantenha a tela aberta para edição.
        try {
            artigoViewModel.getArtigoById(id,
                onSuccess = { artigo ->
                    Log.d("CriarNovoArtigo", "Callback onSuccess chamado")
                    try {
                        artigo?.let { artigoEncontrado ->
                            Log.d("CriarNovoArtigo", "✅ Artigo encontrado no banco: ${artigoEncontrado.nome}")
                            // Carregar dados do banco normalmente
                            binding.editTextNome.setText(artigoEncontrado.nome)
                            binding.editTextPreco.setText(if (artigoEncontrado.preco == 0.0) "" else decimalFormat.format(artigoEncontrado.preco))
                            val quantidadeParaExibir = if (intent.hasExtra("quantidade_fatura")) {
                                intent.getIntExtra("quantidade_fatura", 1)
                            } else {
                                artigoEncontrado.quantidade
                            }
                            binding.editTextQtd.setText(quantidadeParaExibir.toString())
                            binding.editTextDescricao.setText(artigoEncontrado.descricao)
                            binding.editTextNumeroSerial.setText(artigoEncontrado.numeroSerial)
                            binding.switchGuardarFatura.isChecked = artigoEncontrado.guardarFatura ?: false
                            Log.d("CriarNovoArtigo", "✅ Dados do artigo ID $id carregados do banco. Guardar Fatura: ${artigoEncontrado.guardarFatura}")
                        } ?: run {
                            Log.w("CriarNovoArtigo", "⚠️ Artigo ID $id não encontrado no banco, usando dados dos extras. A tela permanecerá aberta para edição!")
                            carregarDadosDosExtras()
                        }
                    } catch (e: Exception) {
                        Log.e("CriarNovoArtigo", "❌ Erro ao processar artigo encontrado: ${e.message}")
                        e.printStackTrace()
                        carregarDadosDosExtras()
                    }
                },
                onError = { exception ->
                    Log.e("CriarNovoArtigo", "❌ Callback onError chamado: ${exception.message}")
                    Log.e("CriarNovoArtigo", "❌ Erro ao carregar artigo do banco: ${exception.message}")
                    showToast("Erro ao carregar dados do artigo do banco, usando dados dos extras. Você pode editar normalmente.")
                    carregarDadosDosExtras()
                }
            )
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "❌ ERRO CRÍTICO ao chamar getArtigoById: ${e.message}")
            e.printStackTrace()
            showToast("Erro ao buscar artigo no banco, usando dados dos extras.")
            carregarDadosDosExtras()
        }
        
        Log.d("CriarNovoArtigo", "=== FIM loadArtigoDataWithFallback ===")
        atualizarValorTotal()
    }

    private fun carregarDadosDosExtras() {
        Log.d("CriarNovoArtigo", "🔄 === INICIANDO carregarDadosDosExtras ===")
        
        val nomeExtra = intent.getStringExtra("nome_artigo")
        val quantidadeExtra = intent.getIntExtra("quantidade_fatura", -1)
        val precoExtra = intent.getDoubleExtra("valor", -1.0)
        val numeroSerialExtra = intent.getStringExtra("numero_serial")
        val descricaoExtra = intent.getStringExtra("descricao")

        Log.d("CriarNovoArtigo", "📥 Dados dos extras: nome='$nomeExtra', qtd=$quantidadeExtra, preco=$precoExtra, serial='$numeroSerialExtra', desc='$descricaoExtra'")

        try {
            // Preencher campos com dados dos extras
            nomeExtra?.let { 
                binding.editTextNome.setText(it)
                Log.d("CriarNovoArtigo", "✅ Nome preenchido: '$it'")
            } ?: run {
                Log.w("CriarNovoArtigo", "⚠️ Nome extra é null")
            }
            
            if (quantidadeExtra != -1) {
                binding.editTextQtd.setText(quantidadeExtra.toString())
                Log.d("CriarNovoArtigo", "✅ Quantidade preenchida: $quantidadeExtra")
            } else {
                binding.editTextQtd.setText("1")
                Log.d("CriarNovoArtigo", "⚠️ Quantidade extra inválida, usando padrão: 1")
            }
            
            if (precoExtra != -1.0) {
                binding.editTextPreco.setText(decimalFormat.format(precoExtra))
                Log.d("CriarNovoArtigo", "✅ Preço preenchido: $precoExtra")
            } else {
                Log.w("CriarNovoArtigo", "⚠️ Preço extra inválido: $precoExtra")
            }
            
            numeroSerialExtra?.let { 
                binding.editTextNumeroSerial.setText(it)
                Log.d("CriarNovoArtigo", "✅ Número serial preenchido: '$it'")
            } ?: run {
                Log.d("CriarNovoArtigo", "ℹ️ Número serial extra é null")
            }
            
            descricaoExtra?.let { 
                binding.editTextDescricao.setText(it)
                Log.d("CriarNovoArtigo", "✅ Descrição preenchida: '$it'")
            } ?: run {
                Log.d("CriarNovoArtigo", "ℹ️ Descrição extra é null")
            }
            
            Log.d("CriarNovoArtigo", "✅ === FIM carregarDadosDosExtras - CAMPOS PREENCHIDOS ===")
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "❌ Erro ao carregar dados dos extras: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun verificarArtigoBloqueadoPorSerial(numeroSerialArtigo: String, callback: (Boolean, ClienteBloqueado?) -> Unit) {
        if (numeroSerialArtigo.isBlank()) {
            callback(false, null)
            return
        }

        // Busca cliente bloqueado pelo número serial
        clienteBloqueadoViewModel.getClienteBloqueadoByNumeroSerial(numeroSerialArtigo).observe(this) { clienteBloqueado ->
            callback(clienteBloqueado != null, clienteBloqueado)
        }
    }

    private fun mostrarDialogoArtigoBloqueado(clienteBloqueado: ClienteBloqueado, numeroSerial: String) {
        AlertDialog.Builder(this)
            .setTitle("Cliente Bloqueado Encontrado")
            .setMessage("O número serial '$numeroSerial' está associado ao cliente bloqueado '${clienteBloqueado.nome}'. Deseja continuar mesmo assim?")
            .setPositiveButton("Continuar") { _, _ ->
                salvarArtigo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun atualizarValorTotal() {
        try {
            val precoStr = binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")
            val quantidadeStr = binding.editTextQtd.text.toString().trim()

            if (precoStr.isNotEmpty() && quantidadeStr.isNotEmpty()) {
                val precoUnitario = normalizeInput(precoStr).toDouble()
                val quantidade = quantidadeStr.toInt()
                val valorTotal = precoUnitario * quantidade
                // TODO: Adicionar TextView para mostrar valor total se necessário
                Log.d("CriarNovoArtigo", "Valor total calculado: R$ ${decimalFormat.format(valorTotal)}")
            }
        } catch (e: Exception) {
            Log.e("CriarNovoArtigo", "Erro ao calcular valor total: ${e.message}")
        }
    }

    private fun normalizeInput(input: String): String {
        return input
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "") // Remove separador de milhar
            .replace(",", ".") // Troca vírgula por ponto
            .trim()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        Log.d("CriarNovoArtigo", "🔙 onBackPressed chamado")
        val nome = binding.editTextNome.text.toString().trim()
        Log.d("CriarNovoArtigo", "🔙 Nome atual: '$nome'")
        
        if (nome.isNotEmpty()) {
            Log.d("CriarNovoArtigo", "🔙 Mostrando diálogo de confirmação")
            AlertDialog.Builder(this)
                .setTitle("Salvar Artigo")
                .setMessage("Deseja salvar as alterações antes de sair?")
                .setPositiveButton("Sim") { _, _ ->
                    Log.d("CriarNovoArtigo", "🔙 Usuário escolheu salvar")
                    salvarArtigo()
                }
                .setNegativeButton("Não") { _, _ ->
                    Log.d("CriarNovoArtigo", "🔙 Usuário escolheu não salvar")
                    super.onBackPressed()
                }
                .show()
        } else {
            Log.d("CriarNovoArtigo", "🔙 Nome vazio, fechando diretamente")
            super.onBackPressed()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCameraForOcr() {
        val imageUri = createImageUri() ?: return
        cameraImageUri = imageUri
        cameraLauncher.launch(imageUri)
    }

    private fun createImageUri(): Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ocr_temp_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun processImageForOcr(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val allBlocks = visionText.textBlocks.flatMap { it.lines.map { line -> line.text } }
                    if (allBlocks.isNotEmpty()) {
                        showOcrResultDialog(allBlocks)
                    } else {
                        showToast("Nenhum texto encontrado na imagem.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Erro ao processar imagem: ${e.message}")
                }
        } catch (e: Exception) {
            showToast("Erro ao carregar imagem para OCR: ${e.message}")
        }
    }

    private fun showOcrResultDialog(texts: List<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecione o texto para o número serial")
        builder.setItems(texts.toTypedArray()) { _, which ->
            binding.editTextNumeroSerial.setText(texts[which])
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun openGalleryForOcr() {
        galleryLauncher.launch("image/*")
    }

    private fun loadArtigoData(id: Long) {
        Log.d("CriarNovoArtigo", "=== INICIANDO loadArtigoData ===")
        Log.d("CriarNovoArtigo", "Tentando carregar artigo ID $id do banco")
        
        artigoViewModel.getArtigoById(id,
            onSuccess = { artigo ->
                Log.d("CriarNovoArtigo", "Callback onSuccess chamado")
                artigo?.let { artigoEncontrado ->
                    Log.d("CriarNovoArtigo", "Artigo encontrado no banco: ${artigoEncontrado.nome}")
                    binding.editTextNome.setText(artigoEncontrado.nome)
                    binding.editTextPreco.setText(if (artigoEncontrado.preco == 0.0) "" else decimalFormat.format(artigoEncontrado.preco))

                    val quantidadeParaExibir = if (intent.hasExtra("quantidade_fatura")) {
                        intent.getIntExtra("quantidade_fatura", 1)
                    } else {
                        artigoEncontrado.quantidade
                    }
                    binding.editTextQtd.setText(quantidadeParaExibir.toString())

                    binding.editTextDescricao.setText(artigoEncontrado.descricao)
                    binding.editTextNumeroSerial.setText(artigoEncontrado.numeroSerial)
                    binding.switchGuardarFatura.isChecked = artigoEncontrado.guardarFatura ?: false
                    Log.d("CriarNovoArtigo", "Dados do artigo ID $id carregados do banco. Guardar Fatura: ${artigoEncontrado.guardarFatura}")
                } ?: run {
                    Log.w("CriarNovoArtigo", "Artigo ID $id não encontrado no banco, usando dados dos extras")
                    carregarDadosDosExtras()
                }
            },
            onError = { exception ->
                Log.e("CriarNovoArtigo", "Callback onError chamado: ${exception.message}")
                Log.e("CriarNovoArtigo", "Erro ao carregar artigo do banco: ${exception.message}")
                showToast("Erro ao carregar dados do artigo do banco, usando dados dos extras")
                carregarDadosDosExtras()
            }
        )
        Log.d("CriarNovoArtigo", "=== FIM loadArtigoData ===")
        atualizarValorTotal()
    }

    override fun onResume() {
        super.onResume()
        Log.d("CriarNovoArtigo", "🔄 onResume chamado - atividade está ativa")
    }

    override fun onPause() {
        Log.d("CriarNovoArtigo", "⏸️ onPause chamado - isFinishing: $isFinishing")
        super.onPause()
    }

    override fun onStop() {
        Log.d("CriarNovoArtigo", "🛑 onStop chamado - isFinishing: $isFinishing")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d("CriarNovoArtigo", "💀 onDestroy chamado - isFinishing: $isFinishing, isChangingConfigurations: $isChangingConfigurations")
        super.onDestroy()
    }
} 