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
        binding = ActivityCriarNovoArtigoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("CriarNovoArtigoActivityRoom", "onCreate chamado")

        // Inicializa os ViewModels
        val database = database.AppDatabase.getDatabase(this)
        val artigoRepository = ArtigoRepository(database.artigoDao())
        val artigoFactory = ArtigoViewModelFactory(artigoRepository)
        artigoViewModel = ViewModelProvider(this, artigoFactory)[ArtigoViewModel::class.java]
        
        clienteBloqueadoViewModel = ViewModelProvider(this)[ClienteBloqueadoViewModel::class.java]

        artigoId = intent.getLongExtra("artigo_id", -1)
        Log.d("CriarNovoArtigo", "Recebido artigo_id: $artigoId")

        if (artigoId != -1L) {
            binding.textViewArtigoTitolo.text = "Editar Artigo"
            loadArtigoData(artigoId)
        } else {
            binding.textViewArtigoTitolo.text = "Novo Artigo"
            intent.getStringExtra("nome_artigo")?.let { binding.editTextNome.setText(it) }
            intent.getIntExtra("quantidade", 1).let { binding.editTextQtd.setText(it.toString()) }
            val precoUnitarioIntent = intent.getDoubleExtra("valor", 0.0)
            binding.editTextPreco.setText(if (precoUnitarioIntent == 0.0) "" else decimalFormat.format(precoUnitarioIntent))
            intent.getStringExtra("numero_serial")?.let { binding.editTextNumeroSerial.setText(it) }
            intent.getStringExtra("descricao")?.let { binding.editTextDescricao.setText(it) }
        }

        val sharedPreferences = getSharedPreferences("DefinicoesGuardarArtigo", MODE_PRIVATE)
        val guardarArtigoPadrao = sharedPreferences.getBoolean("guardar_artigo_padrao", true)
        binding.switchGuardarFatura.isChecked = guardarArtigoPadrao
        Log.d("CriarNovoArtigo", "SwitchGuardarArtigo definido para: $guardarArtigoPadrao")

        atualizarValorTotal()

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
                                showToast("Artigo removido dos itens recentes.")
                                Log.d("CriarNovoArtigo", "Artigo ID $id marcado para não ser guardado para recentes.")
                                
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
                                finish()
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
        val nome = binding.editTextNome.text.toString().trim()
        val precoStrInput = binding.editTextPreco.text.toString().trim().replace("R$\\s*".toRegex(), "")
        val quantidadeStr = binding.editTextQtd.text.toString().trim()
        val numeroSerial = binding.editTextNumeroSerial.text.toString().trim()
        val descricao = binding.editTextDescricao.text.toString().trim()
        val guardarParaRecentes = binding.switchGuardarFatura.isChecked

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

        // Se o artigo já existe (artigoId != -1), apenas atualizar se necessário
        if (artigoId != -1L) {
            Log.d("CriarNovoArtigo", "Artigo já existe (ID: $artigoId), apenas retornando dados atualizados")
            // Não criar novo artigo, apenas usar o ID existente
            idParaRetorno = artigoId
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
                            idParaRetorno = id
                            showToast("Novo artigo salvo e guardado para recentes!")
                        },
                        onError = { exception ->
                            Log.e("CriarNovoArtigo", "Erro ao inserir artigo: ${exception.message}")
                            showToast("Erro ao salvar artigo")
                            // Mesmo com erro, continuar com ID temporário
                            idParaRetorno = -System.currentTimeMillis()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("CriarNovoArtigo", "Erro ao salvar artigo no DB: ${e.message}")
                    showToast("Erro ao interagir com o banco de dados para 'Recentes'.")
                    idParaRetorno = -System.currentTimeMillis()
                }
            } else {
                // Artigo novo mas não guardar para recentes
                idParaRetorno = -System.currentTimeMillis()
                showToast("Artigo será usado apenas na fatura atual.")
            }
        }

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
        finish()
    }

    private fun loadArtigoData(id: Long) {
        artigoViewModel.getArtigoById(id,
            onSuccess = { artigo ->
                artigo?.let { artigoEncontrado ->
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
                    Log.d("CriarNovoArtigo", "Dados do artigo ID $id carregados. Guardar Fatura: ${artigoEncontrado.guardarFatura}")
                }
            },
            onError = { exception ->
                Log.e("CriarNovoArtigo", "Erro ao carregar artigo: ${exception.message}")
                showToast("Erro ao carregar dados do artigo")
            }
        )
        atualizarValorTotal()
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
        val nome = binding.editTextNome.text.toString().trim()
        if (nome.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Salvar Artigo")
                .setMessage("Deseja salvar as alterações antes de sair?")
                .setPositiveButton("Sim") { _, _ ->
                    salvarArtigo()
                }
                .setNegativeButton("Não") { _, _ ->
                    super.onBackPressed()
                }
                .show()
        } else {
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
} 