package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.text.Editable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivitySecondScreenBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import database.AppDatabase
import database.entities.Fatura
import database.repository.FaturaRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class SecondScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondScreenBinding
    private var nomeClienteSalvo: String? = null
    private var clienteIdSalvo: Long = -1L
    private val artigosList = mutableListOf<ArtigoItem>()
    private val notasList = mutableListOf<String>()
    private val fotosList = mutableListOf<String>()
    private lateinit var artigoAdapter: ArtigoAdapter
    private lateinit var notaAdapter: NotaAdapter

    private val CLIENTES_RECENTES_REQUEST_CODE = 123 // ATUALIZADO: Código de requisição alterado
    private val ARQUIVOS_RECENTES_REQUEST_CODE = 791
    private val CRIAR_NOVO_ARTIGO_REQUEST_CODE = 792
    private val THIRD_SCREEN_REQUEST_CODE = 456
    private val GALERIA_FOTOS_REQUEST_CODE = 789

    private var dbHelper: ClienteDbHelper? = null
    // Corrigido: Usando DecimalFormatSymbols para definir o formato de moeda
    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")).apply {
        decimalSeparator = ','
        groupingSeparator = '.'
    })
    private var desconto: Double = 0.0
    private var isPercentDesconto: Boolean = false
    private var taxaEntrega: Double = 0.0
    private var descontoValor: Double = 0.0
    private var faturaId: Long = -1
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notasPadraoPreferences: SharedPreferences
    private lateinit var faturaPrefs: SharedPreferences
    private var isFaturaSaved: Boolean = false
    private var faturaEnviadaSucesso: Boolean = false

    // Variáveis para Room Database
    private lateinit var faturaRepository: FaturaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SecondScreen", "onCreate chamado")
        
        binding = ActivitySecondScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar barra de navegação branca
        window.navigationBarColor = Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        sharedPreferences = getSharedPreferences("InformacoesEmpresaPrefs", MODE_PRIVATE)
        notasPadraoPreferences = getSharedPreferences("NotasPrefs", MODE_PRIVATE)
        faturaPrefs = getSharedPreferences("FaturaPrefs", MODE_PRIVATE)

        try {
            dbHelper = ClienteDbHelper(this)
            Log.d("SecondScreen", "ClienteDbHelper inicializado com sucesso")
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao inicializar banco: ${e.message}", e)
            showToast("Erro ao inicializar o banco de dados.")
            finish()
            return
        }

        // Inicializar Room Database
        try {
            val database = AppDatabase.getDatabase(this)
            faturaRepository = FaturaRepository(database.faturaDao())
            Log.d("SecondScreen", "Room Database inicializado com sucesso")
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao inicializar Room Database: ${e.message}", e)
            showToast("Erro ao inicializar o banco de dados Room.")
            finish()
            return
        }

        updateCurrentDate(binding.dateTextViewSecondScreen)

        if (savedInstanceState != null) {
            nomeClienteSalvo = savedInstanceState.getString("nomeClienteSalvo")
            clienteIdSalvo = savedInstanceState.getLong("clienteIdSalvo", -1L)
            val artigos = savedInstanceState.getParcelableArrayList<ArtigoItem>("artigosList")
            if (artigos != null) {
                artigosList.addAll(artigos)
            }
            desconto = savedInstanceState.getDouble("desconto", 0.0)
            isPercentDesconto = savedInstanceState.getBoolean("isPercentDesconto", false)
            taxaEntrega = savedInstanceState.getDouble("taxaEntrega", 0.0)
            descontoValor = savedInstanceState.getDouble("descontoValor", 0.0)
            val notasSalvas = savedInstanceState.getStringArrayList("notasList")
            if (notasSalvas != null) {
                notasList.addAll(notasSalvas)
            }
            val fotos = savedInstanceState.getStringArrayList("fotosList")
            if (fotos != null) {
                fotosList.addAll(fotos)
            }
            isFaturaSaved = savedInstanceState.getBoolean("isFaturaSaved", false)
            faturaEnviadaSucesso = savedInstanceState.getBoolean("faturaEnviadaSucesso", false)
            Log.d("SecondScreen", "Restaurando estado: nomeClienteSalvo=$nomeClienteSalvo, clienteIdSalvo=$clienteIdSalvo")
            atualizarTopAdicionarClienteComNome()
        }

        artigoAdapter = ArtigoAdapter(
            this,
            artigosList,
            { position ->
                val artigo = artigosList[position]
                Log.d("SecondScreen", "Clicou em artigo para editar: ID=${artigo.id}, Nome='${artigo.nome}', Quantidade=${artigo.quantidade}")
                
                val intent = Intent(this, CriarNovoArtigoActivityRoom::class.java).apply {
                    putExtra("artigo_id", artigo.id)
                    putExtra("nome_artigo", artigo.nome)
                    putExtra("quantidade_fatura", artigo.quantidade) // Usando quantidade
                    val precoUnitario = if (artigo.quantidade > 0) artigo.preco / artigo.quantidade else artigo.preco
                    putExtra("valor", precoUnitario)
                    putExtra("numero_serial", artigo.numeroSerial)
                    putExtra("descricao", artigo.descricao)
                }
                
                Log.d("SecondScreen", "Enviando para CriarNovoArtigoActivityRoom: artigo_id=${artigo.id}, nome='${artigo.nome}'")
                startActivityForResult(intent, CRIAR_NOVO_ARTIGO_REQUEST_CODE)
            },
            { position ->
                artigosList.removeAt(position)
                artigoAdapter.notifyItemRemoved(position)
                artigoAdapter.notifyItemRangeChanged(position, artigosList.size - position)
                showToast("Artigo removido")
                updateSubtotal()
                isFaturaSaved = false
            },
            { position ->
                artigosList.removeAt(position)
                artigoAdapter.notifyItemRemoved(position)
                artigoAdapter.notifyItemRangeChanged(position, artigosList.size - position)
                showToast("Artigo removido ao clicar e segurar")
                updateSubtotal()
                isFaturaSaved = false
            }
        )

        binding.artigosRecyclerViewSecondScreen.layoutManager = LinearLayoutManager(this)
        binding.artigosRecyclerViewSecondScreen.adapter = artigoAdapter

        notaAdapter = NotaAdapter(notasList) { position ->
            notaAdapter.removeNota(position)
            showToast("Nota removida ao clicar e segurar")
            isFaturaSaved = false
        }
        binding.notasRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notasRecyclerView.adapter = notaAdapter

        if (savedInstanceState != null && ::notaAdapter.isInitialized) {
            notaAdapter.notifyDataSetChanged()
        }

        val itemTouchHelperCallbackArtigos = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    artigoAdapter.removeItem(position)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallbackArtigos).attachToRecyclerView(binding.artigosRecyclerViewSecondScreen)

        val itemTouchHelperCallbackNotas = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    notaAdapter.removeNota(position)
                    showToast("Nota removida ao deslizar")
                    isFaturaSaved = false
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallbackNotas).attachToRecyclerView(binding.notasRecyclerView)

        faturaId = intent.getLongExtra("fatura_id", -1)
        if (faturaId != -1L) {
            loadFaturaFromDatabase(faturaId)
            isFaturaSaved = true
        } else {
            val lastFaturaNumber = faturaPrefs.getInt("last_fatura_number", 0) + 1
            binding.invoiceNumberTextView.text = "#${lastFaturaNumber.toString().padStart(4, '0')}"
            handleClientNameAndIdFromIntent(intent)
            updateSubtotal()
            if (notasList.isEmpty()) {
                loadNotasPadraoParaNovaFatura()
            }
        }

        binding.backButtonSecondScreen.setOnClickListener {
            Log.d("SecondScreen", "Botão Voltar (ImageButton) clicado.")
            trySaveAndExit()
        }

        binding.gerImageButtonSecondScreen.setOnClickListener {
            val intent = Intent(this, ThirdScreenActivity::class.java).apply {
                putExtra("desconto", desconto)
                putExtra("isPercentDesconto", isPercentDesconto)
                putExtra("taxaEntrega", taxaEntrega)
            }
            startActivityForResult(intent, THIRD_SCREEN_REQUEST_CODE)
        }

        binding.saveTextViewSecondScreen.setOnClickListener {
            try {
                Log.d("SecondScreen", "Botão Guardar clicado - Iniciando saveFatura()")
                saveFatura()
                Log.d("SecondScreen", "saveFatura() concluído")
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao salvar fatura: ${e.message}", e)
                showToast("Erro ao salvar a fatura: ${e.message}")
            }
        }

        // Método alternativo para testar se o problema está no salvamento
        binding.saveTextViewSecondScreen.setOnLongClickListener {
            Log.d("SecondScreen", "Botão Guardar pressionado longamente - Fechando atividade diretamente")
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            true
        }

        binding.topAdicionarClienteTextViewSecondScreen.setOnClickListener {
            try {
                Log.d("SecondScreen", "Clicou em topAdicionarClienteTextViewSecondScreen. nomeClienteSalvo: '$nomeClienteSalvo', clienteIdSalvo: $clienteIdSalvo")
                // Mude para ClientesRecentesActivityRoom
                val intentParaAdicionarOuEditarCliente = Intent(this, ClientesRecentesActivityRoom::class.java) // ATUALIZADO: Chamando a nova Activity

                if (!nomeClienteSalvo.isNullOrEmpty() && nomeClienteSalvo != getString(R.string.adicionar_cliente_text) && clienteIdSalvo > 0L) {
                    Log.d("SecondScreen", "Abrindo ClientesRecentesActivityRoom para editar cliente ID: $clienteIdSalvo")
                    intentParaAdicionarOuEditarCliente.putExtra("cliente_id", clienteIdSalvo)
                } else {
                    Log.d("SecondScreen", "Nenhum cliente selecionado. Abrindo ClientesRecentesActivityRoom para adicionar um novo.")
                    // Não precisa de putExtra para um novo cliente, o ID padrão (-1L) já indica isso
                }
                startActivityForResult(intentParaAdicionarOuEditarCliente, CLIENTES_RECENTES_REQUEST_CODE)

            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro no clique do nome do cliente: ${e.message}", e)
                showToast("Erro ao processar clique no cliente: ${e.message}")
            }
        }


        binding.topAdicionarClienteTextViewSecondScreen.setOnLongClickListener {
            if (!nomeClienteSalvo.isNullOrEmpty() || artigosList.isNotEmpty() || notasList.isNotEmpty() || fotosList.isNotEmpty()) {
                nomeClienteSalvo = null
                clienteIdSalvo = -1L
                artigosList.clear()
                notasList.clear()
                loadNotasPadraoParaNovaFatura()
                fotosList.clear()
                artigoAdapter.notifyDataSetChanged()
                atualizarTopAdicionarClienteComNome()
                showToast("Cliente, artigos, notas e fotos removidos da fatura atual. Notas padrão restauradas.")
                isFaturaSaved = false
                faturaEnviadaSucesso = false
                if(faturaId != -1L) {
                    faturaId = -1L
                    val lastFaturaNumber = faturaPrefs.getInt("last_fatura_number", 0) + 1
                    binding.invoiceNumberTextView.text = "#${lastFaturaNumber.toString().padStart(4, '0')}"
                }
                true
            } else {
                false
            }
        }

        binding.adicionarArtigoContainerSecondScreen.setOnLongClickListener {
            if (!nomeClienteSalvo.isNullOrEmpty() || artigosList.isNotEmpty() || notasList.isNotEmpty() || fotosList.isNotEmpty()) {
                nomeClienteSalvo = null
                clienteIdSalvo = -1L
                artigosList.clear()
                notasList.clear()
                loadNotasPadraoParaNovaFatura()
                fotosList.clear()
                artigoAdapter.notifyDataSetChanged()
                atualizarTopAdicionarClienteComNome()
                showToast("Cliente, artigos, notas e fotos removidos da fatura atual. Notas padrão restauradas.")
                isFaturaSaved = false
                faturaEnviadaSucesso = false
                if(faturaId != -1L) {
                    faturaId = -1L
                    val lastFaturaNumber = faturaPrefs.getInt("last_fatura_number", 0) + 1
                    binding.invoiceNumberTextView.text = "#${lastFaturaNumber.toString().padStart(4, '0')}"
                }
                true
            } else {
                false
            }
        }

        binding.adicionarArtigoContainerSecondScreen.setOnClickListener {
            try {
                Log.d("SecondScreen", "Clicou em adicionarArtigoContainerSecondScreen")
                val intent = Intent(this, ArquivosRecentesActivityRoom::class.java)
                startActivityForResult(intent, ARQUIVOS_RECENTES_REQUEST_CODE)
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao abrir tela de arquivos recentes: ${e.message}", e)
                showToast("Erro ao abrir a tela de arquivos recentes: ${e.message}")
            }
        }

        binding.adicionarNotaContainer.setOnClickListener {
            showAddNotaDialog()
        }

        binding.adicionarNotaTextView.setOnClickListener {
            showAddNotaDialog()
        }

        binding.adicionarFotoTextView.setOnClickListener {
            Log.d("SecondScreen", "TextView Adicionar Foto clicado")
            if (nomeClienteSalvo.isNullOrEmpty() && faturaId == -1L) {
                showToast("Selecione um cliente antes de adicionar fotos a uma nova fatura.")
                return@setOnClickListener
            }
            val intent = Intent(this, GaleriaFotosActivity::class.java).apply {
                putExtra("fatura_id", faturaId)
                putStringArrayListExtra("photos", ArrayList(fotosList))
            }
            startActivityForResult(intent, GALERIA_FOTOS_REQUEST_CODE)
        }

        binding.adicionarFotoContainer.setOnClickListener {
            Log.d("SecondScreen", "Container Adicionar Foto clicado")
            if (nomeClienteSalvo.isNullOrEmpty() && faturaId == -1L) {
                showToast("Selecione um cliente antes de adicionar fotos a uma nova fatura.")
                return@setOnClickListener
            }
            val intent = Intent(this, GaleriaFotosActivity::class.java).apply {
                putExtra("fatura_id", faturaId)
                putStringArrayListExtra("photos", ArrayList(fotosList))
            }
            startActivityForResult(intent, GALERIA_FOTOS_REQUEST_CODE)
        }

        binding.viewIcon.setOnClickListener {
            try {
                val pdfFile = generatePDF() // Chamada para o PDF completo
                if (pdfFile != null) {
                    viewPDF(pdfFile)
                } else {
                    Log.w("SecondScreen", "generatePDF retornou nulo ao tentar visualizar.")
                }
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao gerar PDF para visualização: ${e.message}", e)
                showToast("Erro ao gerar PDF: ${e.message}")
            }
        }

        binding.sendIcon.setOnClickListener {
            try {
                val pdfFile = generatePDF() // Ainda usando o PDF completo para o ícone de envio
                if (pdfFile != null) {
                    sharePDF(pdfFile) { sucesso ->
                        if (sucesso) {
                            marcarFaturaComoEnviada(faturaId)
                            faturaEnviadaSucesso = true
                        }
                    }
                } else {
                    Log.w("SecondScreen", "generatePDF retornou nulo ao tentar compartilhar.")
                }
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao compartilhar PDF: ${e.message}", e)
                showToast("Erro ao compartilhar PDF: ${e.message}")
            }
        }

        binding.emailButton.setOnClickListener {
            try {
                val pdfFile = generateEmailPdf() // Gera o PDF simplificado
                if (pdfFile != null) {
                    viewPDF(pdfFile) // Abre o PDF diretamente
                } else {
                    Log.w("SecondScreen", "generateEmailPdf retornou nulo ao tentar abrir.")
                }
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao gerar e abrir PDF para email: ${e.message}", e)
                showToast("Erro ao gerar e abrir PDF para email: ${e.message}")
            }
        }

        // Adicionar OnClickListener para o saldoDevedorValueTextView
        binding.saldoDevedorValueTextView.setOnClickListener {
            try {
                Log.d("SecondScreen", "Saldo devedor clicado - Abrindo tela de taxas e descontos")
                val intent = Intent(this, ThirdScreenActivity::class.java).apply {
                    putExtra("desconto", desconto)
                    putExtra("isPercentDesconto", isPercentDesconto)
                    putExtra("taxaEntrega", taxaEntrega)
                    putExtra("subtotal", artigosList.sumOf { it.preco })
                }
                startActivityForResult(intent, THIRD_SCREEN_REQUEST_CODE)
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao abrir tela de taxas e descontos: ${e.message}", e)
                showToast("Erro ao abrir tela de taxas e descontos: ${e.message}")
            }
        }
    }

    private fun generateBarcode(text: String, width: Int = 300, height: Int = 60): Bitmap? {
        try {
            Log.d("SecondScreen", "Gerando código de barras com valor: '$text'")
            if (text.isEmpty()) {
                Log.w("SecondScreen", "Texto para código de barras está vazio.")
                return null
            }
            val bitMatrix: BitMatrix = MultiFormatWriter().encode( // Corrigido aqui
                text,
                BarcodeFormat.CODE_128,
                width,
                height
            )
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao gerar código de barras para '$text': ${e.message}", e)
            return null
        }
    }

    private fun generatePDF(): File? {
        if (nomeClienteSalvo.isNullOrEmpty() || nomeClienteSalvo == getString(R.string.adicionar_cliente_text)) {
            showToast("Selecione um cliente antes de gerar o PDF.")
            return null
        }
        if (artigosList.isEmpty()) {
            showToast("Adicione pelo menos um artigo para gerar o PDF.")
            return null
        }

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 25f
        val contentWidth = pageWidth - 2 * margin

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create()
        val currentPage = pdfDocument.startPage(pageInfo)
        val currentCanvas: Canvas? = currentPage.canvas
        var currentYPosition = margin

        if (currentCanvas == null) {
            Log.e("SecondScreen", "Canvas do PDF é nulo. Não é possível desenhar.")
            pdfDocument.close()
            showToast("Erro interno ao criar página do PDF.")
            return null
        }

        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 28f
            color = android.graphics.Color.BLACK
        }
        val headerPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val textPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val empresaInfoPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        val empresaNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 18f
            color = android.graphics.Color.BLACK
        }
        val labelPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#444444")
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val headerBackgroundPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val clienteNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val clienteInfoPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        val notasTextPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 9f
            color = Color.parseColor("#757575")
        }
        val saldoDevedorPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 12f
            color = android.graphics.Color.BLACK
        }
        val pageNumPaint = TextPaint().apply {
            textSize = 8f
            color = Color.DKGRAY
        }

        val yPosFaturaTitleBaseline = currentYPosition + titlePaint.textSize
        currentCanvas.drawText("Fatura", margin, yPosFaturaTitleBaseline, titlePaint)
        currentYPosition = yPosFaturaTitleBaseline + 15f

        val headerBlockTopY = currentYPosition
        var logoHeight = 0f
        var logoActualWidth = 0f
        var yPosAfterLogo = headerBlockTopY

        val logoPrefs = getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)
        val logoSizeProgress = logoPrefs.getInt("logo_size", 30)
        val minLogoDisplaySizePdf = 60f
        val maxLogoDisplaySizePdf = 300f
        val actualLogoSizeForPdf = minLogoDisplaySizePdf + (logoSizeProgress * (maxLogoDisplaySizePdf - minLogoDisplaySizePdf) / 100f)
        val logoUriString = logoPrefs.getString("logo_uri", null)

        if (logoUriString != null) {
            try {
                val logoUri = Uri.parse(logoUriString)
                contentResolver.openInputStream(logoUri)?.use { inputStream ->
                    val originalLogoBitmap = BitmapFactory.decodeStream(inputStream)
                    if (originalLogoBitmap == null) {
                        Log.w("SecondScreen", "Falha ao decodificar o bitmap do logotipo a partir do URI.")
                    } else {
                        val aspectRatio = originalLogoBitmap.width.toFloat() / originalLogoBitmap.height.toFloat()
                        var targetWidth = actualLogoSizeForPdf
                        var targetHeight = targetWidth / aspectRatio
                        val maxPermittedHeight = pageHeight * 0.30f
                        if (targetHeight > maxPermittedHeight) {
                            targetHeight = maxPermittedHeight
                            targetWidth = targetHeight * aspectRatio
                        }
                        val maxPermittedWidth = contentWidth * 0.55f
                        if (targetWidth > maxPermittedWidth) {
                            targetWidth = maxPermittedWidth
                            targetHeight = targetWidth / aspectRatio
                        }

                        if (targetWidth > 0 && targetHeight > 0) {
                            val logoBitmap = Bitmap.createScaledBitmap(originalLogoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                            logoHeight = logoBitmap.height.toFloat()
                            logoActualWidth = logoBitmap.width.toFloat()
                            yPosAfterLogo = headerBlockTopY + logoHeight

                            val roundedBitmap = Bitmap.createBitmap(logoBitmap.width, logoBitmap.height, Bitmap.Config.ARGB_8888)
                            val tempCanvas = Canvas(roundedBitmap)
                            val tempPaint = Paint().apply { isAntiAlias = true }
                            val rect = RectF(0f, 0f, logoBitmap.width.toFloat(), logoBitmap.height.toFloat())
                            tempCanvas.drawRoundRect(rect, 8f, 8f, tempPaint)
                            tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                            tempCanvas.drawBitmap(logoBitmap, 0f, 0f, tempPaint)
                            val logoLeft = pageWidth - margin - logoBitmap.width.toFloat()
                            currentCanvas.drawBitmap(roundedBitmap, logoLeft, headerBlockTopY, null)
                            logoBitmap.recycle()
                            roundedBitmap.recycle()
                        } else {
                            Log.w("SecondScreen", "Dimensões do logo calculadas são inválidas: $targetWidth x $targetHeight")
                        }
                        originalLogoBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao carregar ou processar logo: ${e.message}", e)
            }
        }

        var yPosEmpresaAtual = headerBlockTopY
        val gapEntreEmpresaELogo = 15f
        val empresaMaxWidth = if (logoActualWidth > 0) {
            pageWidth - (2 * margin) - logoActualWidth - gapEntreEmpresaELogo
        } else {
            contentWidth
        }

        val cnpj = sharedPreferences.getString("cnpj", "") ?: ""
        val nomeEmpresa = sharedPreferences.getString("nome_empresa", "") ?: ""
        val emailEmpresa = sharedPreferences.getString("email", "") ?: ""
        val telefoneEmpresa = sharedPreferences.getString("telefone", "") ?: ""
        val cepEmpresa = sharedPreferences.getString("cep", "") ?: ""
        val estadoEmpresa = sharedPreferences.getString("estado", "") ?: ""
        val cidadeEmpresa = sharedPreferences.getString("cidade", "") ?: ""

        val nomeEmpresaUpper = nomeEmpresa.uppercase(Locale.getDefault())
        val nomeEmpresaLayout = StaticLayout.Builder.obtain(nomeEmpresaUpper, 0, nomeEmpresaUpper.length, empresaNamePaint, empresaMaxWidth.toInt())
            .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
        currentCanvas.save()
        currentCanvas.translate(margin, yPosEmpresaAtual)
        nomeEmpresaLayout.draw(currentCanvas)
        currentCanvas.restore()
        yPosEmpresaAtual += nomeEmpresaLayout.height + 1f

        val empresaInfoList = mutableListOf<String>()
        if (cnpj.isNotEmpty()) empresaInfoList.add("CNPJ: $cnpj")
        if (emailEmpresa.isNotEmpty()) empresaInfoList.add("Email: $emailEmpresa")
        if (telefoneEmpresa.isNotEmpty()) empresaInfoList.add("Telefone: $telefoneEmpresa")
        var enderecoEmpresaConcatenado = ""
        if (cidadeEmpresa.isNotEmpty()) enderecoEmpresaConcatenado += "$cidadeEmpresa"
        if (estadoEmpresa.isNotEmpty()) {
            if (enderecoEmpresaConcatenado.isNotEmpty()) enderecoEmpresaConcatenado += ", "
            enderecoEmpresaConcatenado += estadoEmpresa
        }
        if (cepEmpresa.isNotEmpty()) {
            if (enderecoEmpresaConcatenado.isNotEmpty()) enderecoEmpresaConcatenado += " "
            enderecoEmpresaConcatenado += cepEmpresa
        }
        if(enderecoEmpresaConcatenado.isNotEmpty()) empresaInfoList.add(enderecoEmpresaConcatenado)

        empresaInfoList.forEach { info ->
            val infoLayout = StaticLayout.Builder.obtain(info, 0, info.length, empresaInfoPaint, empresaMaxWidth.toInt())
                .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(margin, yPosEmpresaAtual)
            infoLayout.draw(currentCanvas)
            currentCanvas.restore()
            yPosEmpresaAtual += infoLayout.height + 0.5f
        }
        val yPosAfterEmpresaInfo = yPosEmpresaAtual
        currentYPosition = maxOf(yPosAfterEmpresaInfo, yPosAfterLogo) + 15f

        val cardTop = currentYPosition
        val cardPadding = 8f
        var clienteY = cardTop + cardPadding
        val clienteMaxWidthPdf = contentWidth / 2f - cardPadding

        val db = dbHelper?.readableDatabase
        val clienteInfoList = mutableListOf<Pair<String, TextPaint>>()

        db?.rawQuery(
            "SELECT * FROM ${ClienteContract.ClienteEntry.TABLE_NAME} WHERE ${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} = ?",
            arrayOf(nomeClienteSalvo ?: "")
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nome = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME)) ?: "Cliente não especificado"
                val email = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL))
                val telefone = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE))
                val cpf = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CPF))
                val cnpjCliente = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO))
                val bairro = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_BAIRRO))
                val municipio = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_MUNICIPIO))
                val uf = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_UF))
                val cep = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CEP))

                clienteInfoList.add(nome to clienteNamePaint)
                if (email?.isNotEmpty() == true) clienteInfoList.add(email to clienteInfoPaint)
                if (telefone?.isNotEmpty() == true) clienteInfoList.add(telefone to clienteInfoPaint)
                if (cpf?.isNotEmpty() == true) clienteInfoList.add("CPF: $cpf" to clienteInfoPaint)
                if (cnpjCliente?.isNotEmpty() == true) clienteInfoList.add("CNPJ: $cnpjCliente" to clienteInfoPaint)

                var enderecoCompleto = ""
                if (logradouro?.isNotEmpty() == true) enderecoCompleto += logradouro
                if (numero?.isNotEmpty() == true) { if (enderecoCompleto.isNotEmpty()) enderecoCompleto += ", "; enderecoCompleto += numero }
                if (bairro?.isNotEmpty() == true) { if (enderecoCompleto.isNotEmpty()) enderecoCompleto += ", "; enderecoCompleto += bairro }
                if (municipio?.isNotEmpty() == true) { if (enderecoCompleto.isNotEmpty()) enderecoCompleto += " - "; enderecoCompleto += municipio }
                if (uf?.isNotEmpty() == true) { if (enderecoCompleto.isNotEmpty()) enderecoCompleto += "/$uf" else enderecoCompleto += uf }
                if (cep?.isNotEmpty() == true) { if (enderecoCompleto.isNotEmpty()) enderecoCompleto += " "; enderecoCompleto += "CEP: $cep" }
                if (enderecoCompleto.isNotEmpty()) clienteInfoList.add(enderecoCompleto.trimStart(',', ' ') to clienteInfoPaint)
            }
            cursor.close()
        }

        clienteInfoList.forEach { (info, paint) ->
            val layout = StaticLayout.Builder.obtain(info, 0, info.length, paint, clienteMaxWidthPdf.toInt())
                .setLineSpacing(0f, 0.8f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(margin + cardPadding, clienteY)
            layout.draw(currentCanvas)
            currentCanvas.restore()
            clienteY += layout.height + 1f
        }

        var invoiceY = cardTop + cardPadding
        // Posiciona os valores da fatura/data alinhados à direita do retângulo
        val rightEdgeForValues = pageWidth - margin - cardPadding // Coordenada X para o alinhamento da direita dos valores
        val labelValueSpacing = 3f

        // Fatura Nº
        val numFaturaLabel = "Fatura Nº:"
        val invoiceText = binding.invoiceNumberTextView.text?.toString() ?: "#${faturaId.toString().padStart(3, '0')}"

        val invoiceTextX = rightEdgeForValues - textPaint.measureText(invoiceText) // X para o valor do número da fatura
        val numFaturaLabelX = invoiceTextX - labelPaint.measureText(numFaturaLabel) - labelValueSpacing // X para o label

        currentCanvas.drawText(numFaturaLabel, numFaturaLabelX, invoiceY + labelPaint.textSize*0.3f, labelPaint)
        currentCanvas.drawText(invoiceText, invoiceTextX, invoiceY + labelPaint.textSize*0.3f, textPaint)
        invoiceY += labelPaint.textSize + 4f

        // Emitido
        val emitidoLabel = "Emitido:"
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
        val issuedDate = dateFormat.format(Date())

        val issuedDateTextX = rightEdgeForValues - textPaint.measureText(issuedDate) // X para o valor da data de emissão
        val emitidoLabelX = issuedDateTextX - labelPaint.measureText(emitidoLabel) - labelValueSpacing // X para o label

        currentCanvas.drawText(emitidoLabel, emitidoLabelX, invoiceY + labelPaint.textSize*0.3f, labelPaint)
        currentCanvas.drawText(issuedDate, issuedDateTextX, invoiceY + labelPaint.textSize*0.3f, textPaint)
        invoiceY += textPaint.textSize + 10f // Espaço após a data

        val cardBottom = maxOf(clienteY, invoiceY) -1f + cardPadding
        currentCanvas.drawRoundRect(margin, cardTop, pageWidth - margin, cardBottom, 5f, 5f, borderPaint)
        currentYPosition = cardBottom + 10f

        val tableHeaderPadding = 3f
        val tableHeaderHeight = textPaint.textSize + 2 * tableHeaderPadding + 4f
        val colNomeWidth = contentWidth * 0.42f
        val colQtdWidth = contentWidth * 0.13f
        val colPrecoUnitWidth = contentWidth * 0.20f

        currentCanvas.drawRoundRect(margin, currentYPosition, pageWidth - margin, currentYPosition + tableHeaderHeight, 5f, 5f, headerBackgroundPaint)
        var currentXHeader = margin
        val textYHeaderOffset = currentYPosition + tableHeaderPadding + textPaint.textSize / 2 + 2f

        currentCanvas.drawText("Nome", currentXHeader + tableHeaderPadding, textYHeaderOffset, textPaint)
        currentXHeader += colNomeWidth
        currentCanvas.drawText("Qtd", currentXHeader + tableHeaderPadding, textYHeaderOffset, textPaint)
        currentXHeader += colQtdWidth
        currentCanvas.drawText("Unit.", currentXHeader + tableHeaderPadding, textYHeaderOffset, textPaint)
        currentXHeader += colPrecoUnitWidth
        currentCanvas.drawText("Total", currentXHeader + tableHeaderPadding, textYHeaderOffset, textPaint)
        currentYPosition += tableHeaderHeight + 3f

        artigosList.forEach { artigo ->
            val artigoNome = artigo.nome ?: "N/A"
            val nomeLayout = StaticLayout.Builder.obtain(artigoNome, 0, artigoNome.length, textPaint, colNomeWidth.toInt() - (2 * tableHeaderPadding).toInt())
                .setLineSpacing(0f,0.9f).setIncludePad(false).build()
            val artigoLineHeight = nomeLayout.height.toFloat() + 5f

            val quantidadeText = artigo.quantidade.toString() // Usando 'quantidade'
            val precoUnitario = if (artigo.quantidade > 0) artigo.preco / artigo.quantidade else 0.0 // Usando 'quantidade'
            val precoUnitarioText = decimalFormat.format(precoUnitario)
            val totalArtigoText = decimalFormat.format(artigo.preco) // Usando 'preco'

            var currentX = margin
            val textYItemOffset = currentYPosition + nomeLayout.getLineBaseline(0) + 1f

            currentCanvas.save()
            currentCanvas.translate(currentX + tableHeaderPadding, currentYPosition)
            nomeLayout.draw(currentCanvas)
            currentCanvas.restore()
            currentX += colNomeWidth

            currentCanvas.drawText(quantidadeText, currentX + tableHeaderPadding, textYItemOffset, textPaint)
            currentX += colQtdWidth
            currentCanvas.drawText(precoUnitarioText, currentX + tableHeaderPadding, textYItemOffset, textPaint)
            currentX += colPrecoUnitWidth
            currentCanvas.drawText(totalArtigoText, currentX + tableHeaderPadding, textYItemOffset, textPaint)

            currentYPosition += artigoLineHeight
            currentCanvas.drawLine(margin, currentYPosition - 2f, pageWidth - margin, currentYPosition - 2f, linePaint)
        }
        currentYPosition += 12f

        val totalXPosition = pageWidth - margin - 140f
        val valueAlignX = pageWidth - margin
        val itemSpacingTotals = textPaint.textSize + 6f

        currentCanvas.drawText("Subtotal:", totalXPosition, currentYPosition + textPaint.textSize*0.3f, textPaint)
        val subtotalText = decimalFormat.format(artigosList.sumOf { it.preco })
        currentCanvas.drawText(subtotalText, valueAlignX - textPaint.measureText(subtotalText), currentYPosition + textPaint.textSize*0.3f, textPaint)
        currentYPosition += itemSpacingTotals

        currentCanvas.drawText("Desconto:", totalXPosition, currentYPosition + textPaint.textSize*0.3f, textPaint)
        val descontoText = decimalFormat.format(descontoValor)
        currentCanvas.drawText(descontoText, valueAlignX - textPaint.measureText(descontoText), currentYPosition + textPaint.textSize*0.3f, textPaint)
        currentYPosition += itemSpacingTotals

        currentCanvas.drawText("Tx. Entrega:", totalXPosition, currentYPosition + textPaint.textSize*0.3f, textPaint)
        val taxaText = decimalFormat.format(taxaEntrega)
        currentCanvas.drawText(taxaText, valueAlignX - textPaint.measureText(taxaText), currentYPosition + textPaint.textSize*0.3f, textPaint)
        currentYPosition += itemSpacingTotals
        currentYPosition += 4f

        // Calcular o saldo devedor
        val saldoDevedor = artigosList.sumOf { it.preco } - descontoValor + taxaEntrega
        val saldoText = decimalFormat.format(saldoDevedor)
        
        // Ajustar tamanho da fonte do "Saldo Devedor" conforme o valor
        val adjustedSaldoPaint = adjustSaldoPaintForPdf(saldoDevedorPaint, saldoText, valueAlignX - totalXPosition)
        
        currentCanvas.drawText("Saldo Devedor:", totalXPosition, currentYPosition + adjustedSaldoPaint.textSize*0.3f, adjustedSaldoPaint)
        currentCanvas.drawText(saldoText, valueAlignX - adjustedSaldoPaint.measureText(saldoText), currentYPosition + adjustedSaldoPaint.textSize*0.3f, adjustedSaldoPaint)
        currentYPosition += adjustedSaldoPaint.textSize + 10f

        val instrucoesPagamentoPrefs = getSharedPreferences("InstrucoesPagamentoPrefs", Context.MODE_PRIVATE)
        val pix = instrucoesPagamentoPrefs.getString("instrucoes_pix", "")
        val banco = instrucoesPagamentoPrefs.getString("instrucoes_banco", "")
        val agencia = instrucoesPagamentoPrefs.getString("instrucoes_agencia", "")
        val conta = instrucoesPagamentoPrefs.getString("instrucoes_conta", "")
        val outras = instrucoesPagamentoPrefs.getString("instrucoes_outras", "")

        val builder = StringBuilder()
        if (!pix.isNullOrEmpty()) {
            builder.append("PIX: ").append(pix).append("\n\n")
        }
        if (!banco.isNullOrEmpty() || !agencia.isNullOrEmpty() || !conta.isNullOrEmpty()) {
            builder.append("Dados Bancários:\n")
            if (!banco.isNullOrEmpty()) builder.append("Banco: ").append(banco).append("\n")
            if (!agencia.isNullOrEmpty()) builder.append("Agência: ").append(agencia).append("\n")
            if (!conta.isNullOrEmpty()) builder.append("Conta: ").append(conta).append("\n\n")
        }
        if (!outras.isNullOrEmpty()) {
            builder.append("Outras Informações:\n").append(outras)
        }

        val instrucoesPagamentoTexto = builder.toString().trim()

        if (instrucoesPagamentoTexto.isNotEmpty()) {
            currentYPosition += 3f  // Reduzido de 5f para 3f
            val instrucoesTitlePaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = 12f  // Restaurado tamanho original
                color = android.graphics.Color.BLACK
            }
            val instrucoesTextPdfPaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textSize = 7f  // Restaurado tamanho original
                color = Color.parseColor("#444444")
            }
            val titleInstrucoes = "Instruções de Pagamento"
            val titleInstrucoesHeight = instrucoesTitlePaint.textSize + 2f  // Reduzido de 3f para 2f
            currentCanvas.drawText(titleInstrucoes, margin, currentYPosition + instrucoesTitlePaint.textSize * 0.3f, instrucoesTitlePaint)
            currentYPosition += titleInstrucoesHeight

            val instrucoesLayout = StaticLayout.Builder.obtain(
                instrucoesPagamentoTexto, 0, instrucoesPagamentoTexto.length, instrucoesTextPdfPaint, contentWidth.toInt()
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.0f)  // Reduzido de 1f para 0f
                .setIncludePad(false)
                .build()

            if (currentYPosition + instrucoesLayout.height > pageHeight - margin - 30f) {
                // Handle new page logic here if necessary. For simplicity, assume it fits or is truncated.
            }

            currentCanvas.save()
            currentCanvas.translate(margin, currentYPosition)
            instrucoesLayout.draw(currentCanvas)
            currentCanvas.restore()
            currentYPosition += instrucoesLayout.height + 12f  // Aumentado de 5f para 12f
        }

        val notasPadraoString = notasPadraoPreferences.getString("notas", "") ?: ""
        val notasPadraoList = if (notasPadraoString.isNotEmpty()) notasPadraoString.split("\n").filter { it.isNotBlank() } else emptyList()
        
        // Criar paint específico para notas padrão com tonalidade mais clara
        val notasPadraoTextPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 9f
            color = Color.parseColor("#999999") // Tonalidade mais clara para notas padrão
        }

        // Seção de Notas (sem títulos, apenas separação visual)
        if (notasList.any { it.isNotBlank() } || notasPadraoList.any { it.isNotBlank() }) {
            currentYPosition += 3f
            val notasTitleHeight = headerPaint.textSize + 2f
            currentCanvas.drawText("Notas:", margin, currentYPosition + headerPaint.textSize*0.3f, headerPaint)
            currentYPosition += notasTitleHeight
            
            // Primeiro, desenhar as notas específicas da fatura
            notasList.forEach { nota ->
                if (nota.isNotBlank()) {
                    val notaLayout = StaticLayout.Builder.obtain(nota, 0, nota.length, notasTextPaint, contentWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 0.9f)
                        .setIncludePad(false)
                        .build()
                    val notaHeightIndividual = notaLayout.height + 1f
                    if (currentYPosition + notaHeightIndividual > pageHeight - margin - 30f) {
                        return@forEach
                    }
                    currentCanvas.save()
                    currentCanvas.translate(margin, currentYPosition)
                    notaLayout.draw(currentCanvas)
                    currentCanvas.restore()
                    currentYPosition += notaHeightIndividual
                }
            }
            
            // Adicionar espaço entre as notas adicionais e padrão se ambas existirem
            if (notasList.any { it.isNotBlank() } && notasPadraoList.any { it.isNotBlank() }) {
                currentYPosition += 8f // Espaço entre as seções
            }
            
            // Depois, desenhar as notas padrão com tonalidade mais clara
            notasPadraoList.forEach { notaPadrao ->
                if (notaPadrao.isNotBlank() && !notasList.contains(notaPadrao)) {
                    val notaLayout = StaticLayout.Builder.obtain(notaPadrao, 0, notaPadrao.length, notasPadraoTextPaint, contentWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 0.9f)
                        .setIncludePad(false)
                        .build()
                    val notaHeightIndividual = notaLayout.height + 1f
                    if (currentYPosition + notaHeightIndividual > pageHeight - margin - 30f) {
                        return@forEach
                    }
                    currentCanvas.save()
                    currentCanvas.translate(margin, currentYPosition)
                    notaLayout.draw(currentCanvas)
                    currentCanvas.restore()
                    currentYPosition += notaHeightIndividual
                }
            }
        }

        val barcodeText = if (faturaId != -1L) faturaId.toString() else (binding.invoiceNumberTextView.text?.toString()?.replace("#", "") ?: "NO_ID")
        val barcodeBitmap = generateBarcode(barcodeText, 160, 35)
        val barcodeX = margin
        val pageNumText = "Pág 1"
        val pageNumTextWidth = pageNumPaint.measureText(pageNumText)
        val pageNumY = pageHeight - margin - 2f
        val barcodeY = pageNumY - (barcodeBitmap?.height?.toFloat() ?: 35f) - 2f

        barcodeBitmap?.let {
            currentCanvas.drawBitmap(it, barcodeX, barcodeY, null)
            it.recycle()
        }
        currentCanvas.drawText(pageNumText, pageWidth - margin - pageNumTextWidth, pageNumY + pageNumPaint.textSize*0.3f, pageNumPaint)

        pdfDocument.finishPage(currentPage)

        val fileName = "Fatura_${binding.invoiceNumberTextView.text?.toString()?.replace("#","") ?: faturaId}_${System.currentTimeMillis()}.pdf"

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null) {
            Log.e("SecondScreen", "Diretório de armazenamento externo não disponível.")
            showToast("Erro: Armazenamento externo não disponível para salvar PDF.")
            pdfDocument.close()
            return null
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("SecondScreen", "Não foi possível criar o diretório de documentos.")
            showToast("Erro ao criar diretório para salvar PDF.")
            pdfDocument.close()
            return null
        }

        val file = File(storageDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Log.d("SecondScreen", "PDF gerado com sucesso: ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            Log.e("SecondScreen", "Erro de I/O ao salvar PDF: ${e.message}", e)
            showToast("Erro de I/O ao salvar PDF: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro geral ao salvar PDF: ${e.message}", e)
            showToast("Erro ao salvar PDF: ${e.message}")
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun generateEmailPdf(): File? {
        Log.d("SecondScreen", "Iniciando geração de PDF SIMPLIFICADO para email.")
        if (nomeClienteSalvo.isNullOrEmpty() || nomeClienteSalvo == getString(R.string.adicionar_cliente_text)) {
            showToast("Selecione um cliente antes de gerar o PDF para email.")
            Log.w("SecondScreen", "Geração de PDF para email cancelada: Cliente não selecionado.")
            return null
        }

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 30f
        val contentWidth = pageWidth - 2 * margin

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create()
        val currentPage = pdfDocument.startPage(pageInfo)
        val currentCanvas: Canvas? = currentPage.canvas
        var currentYPosition = margin

        if (currentCanvas == null) {
            Log.e("SecondScreen", "Canvas do PDF (email) é nulo. Não é possível desenhar.")
            pdfDocument.close()
            showToast("Erro interno ao criar página do PDF para email.")
            return null
        }

        // --- Styles ---
        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 28f
            color = android.graphics.Color.BLACK
        }
        val headerPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val textPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val empresaInfoPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        val empresaNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 18f
            color = android.graphics.Color.BLACK
        }
        val labelPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#444444")
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val clienteNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val clienteInfoPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#666666")
        }
        val pageNumPaint = TextPaint().apply {
            textSize = 8f
            color = Color.DKGRAY
        }

        // --- HEADER: Company Info and Logo ---
        var yPosEmpresaAtual = currentYPosition
        var logoHeight = 0f
        var logoActualWidth = 0f
        val gapBetweenCompanyAndLogo = 15f

        val logoPrefs = getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)
        val logoSizeProgress = logoPrefs.getInt("logo_size", 30)
        val minLogoDisplaySizePdf = 60f
        val maxLogoDisplaySizePdf = 300f
        val actualLogoSizeForPdf = minLogoDisplaySizePdf + (logoSizeProgress * (maxLogoDisplaySizePdf - minLogoDisplaySizePdf) / 100f)
        val logoUriString = logoPrefs.getString("logo_uri", null)

        if (logoUriString != null) {
            try {
                val logoUri = Uri.parse(logoUriString)
                contentResolver.openInputStream(logoUri)?.use { inputStream ->
                    val originalLogoBitmap = BitmapFactory.decodeStream(inputStream)
                    if (originalLogoBitmap == null) {
                        Log.w("SecondScreen", "Falha ao decodificar o bitmap do logotipo a partir do URI.")
                    } else {
                        val aspectRatio = originalLogoBitmap.width.toFloat() / originalLogoBitmap.height.toFloat()
                        var targetWidth = actualLogoSizeForPdf
                        var targetHeight = targetWidth / aspectRatio
                        val maxPermittedHeight = pageHeight * 0.15f // Smaller logo for email PDF
                        if (targetHeight > maxPermittedHeight) {
                            targetHeight = maxPermittedHeight
                            targetWidth = targetHeight * aspectRatio
                        }
                        val maxPermittedWidth = contentWidth * 0.40f // Smaller logo for email PDF
                        if (targetWidth > maxPermittedWidth) {
                            targetWidth = maxPermittedWidth
                            targetHeight = targetWidth / aspectRatio
                        }

                        if (targetWidth > 0 && targetHeight > 0) {
                            val logoBitmap = Bitmap.createScaledBitmap(originalLogoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                            logoHeight = logoBitmap.height.toFloat()
                            logoActualWidth = logoBitmap.width.toFloat()

                            val roundedBitmap = Bitmap.createBitmap(logoBitmap.width, logoBitmap.height, Bitmap.Config.ARGB_8888)
                            val tempCanvas = Canvas(roundedBitmap)
                            val tempPaint = Paint().apply { isAntiAlias = true }
                            val rect = RectF(0f, 0f, logoBitmap.width.toFloat(), logoBitmap.height.toFloat())
                            tempCanvas.drawRoundRect(rect, 8f, 8f, tempPaint)
                            tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                            tempCanvas.drawBitmap(logoBitmap, 0f, 0f, tempPaint)
                            val logoLeft = pageWidth - margin - logoBitmap.width.toFloat()
                            currentCanvas.drawBitmap(roundedBitmap, logoLeft, yPosEmpresaAtual, null)
                            logoBitmap.recycle()
                            roundedBitmap.recycle()
                        } else {
                            Log.w("SecondScreen", "Dimensões do logo calculadas são inválidas: $targetWidth x $targetHeight")
                        }
                        originalLogoBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao carregar ou processar logo para email PDF: ${e.message}", e)
            }
        }

        val empresaMaxWidth = if (logoActualWidth > 0) {
            contentWidth - logoActualWidth - gapBetweenCompanyAndLogo
        } else {
            contentWidth
        }

        val cnpjEmpresa = sharedPreferences.getString("cnpj", "") ?: ""
        val nomeEmpresa = sharedPreferences.getString("nome_empresa", "") ?: ""
        val emailEmpresa = sharedPreferences.getString("email", "") ?: ""
        val telefoneEmpresa = sharedPreferences.getString("telefone", "") ?: ""
        val cepEmpresa = sharedPreferences.getString("cep", "") ?: ""
        val estadoEmpresa = sharedPreferences.getString("estado", "") ?: ""
        val cidadeEmpresa = sharedPreferences.getString("cidade", "") ?: ""

        val nomeEmpresaUpper = nomeEmpresa.uppercase(Locale.getDefault())
        val nomeEmpresaLayout = StaticLayout.Builder.obtain(nomeEmpresaUpper, 0, nomeEmpresaUpper.length, empresaNamePaint, empresaMaxWidth.toInt())
            .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
        currentCanvas.save()
        currentCanvas.translate(margin, yPosEmpresaAtual)
        nomeEmpresaLayout.draw(currentCanvas)
        currentCanvas.restore()
        yPosEmpresaAtual += nomeEmpresaLayout.height + 1f

        val empresaInfoList = mutableListOf<String>()
        if (cnpjEmpresa.isNotEmpty()) empresaInfoList.add("CNPJ: $cnpjEmpresa")
        if (emailEmpresa.isNotEmpty()) empresaInfoList.add("Email: $emailEmpresa")
        if (telefoneEmpresa.isNotEmpty()) empresaInfoList.add("Telefone: $telefoneEmpresa")
        var enderecoEmpresaConcatenado = ""
        if (cidadeEmpresa.isNotEmpty()) enderecoEmpresaConcatenado += "$cidadeEmpresa"
        if (estadoEmpresa.isNotEmpty()) {
            if (enderecoEmpresaConcatenado.isNotEmpty()) enderecoEmpresaConcatenado += ", "
            enderecoEmpresaConcatenado += estadoEmpresa
        }
        if (cepEmpresa.isNotEmpty()) {
            if (enderecoEmpresaConcatenado.isNotEmpty()) enderecoEmpresaConcatenado += " "
            enderecoEmpresaConcatenado += "CEP: $cepEmpresa"
        }
        if(enderecoEmpresaConcatenado.isNotEmpty()) empresaInfoList.add(enderecoEmpresaConcatenado)

        empresaInfoList.forEach { info ->
            val infoLayout = StaticLayout.Builder.obtain(info, 0, info.length, empresaInfoPaint, empresaMaxWidth.toInt())
                .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(margin, yPosEmpresaAtual)
            infoLayout.draw(currentCanvas)
            currentCanvas.restore()
            yPosEmpresaAtual += infoLayout.height + 0.5f
        }
        currentYPosition = maxOf(yPosEmpresaAtual, currentYPosition + logoHeight) + 15f

        // --- CLIENT INFORMATION & INVOICE DETAILS ---
        val cardTop = currentYPosition
        val cardPadding = 8f
        var clientInfoY = currentYPosition + cardPadding
        var invoiceInfoY = currentYPosition + cardPadding

        val db = dbHelper?.readableDatabase
        var clientNome: String? = null
        var clientEmail: String? = null
        var clientTelefone: String? = null
        var clientCpf: String? = null
        var clientCnpj: String? = null
        var clientLogradouro: String? = null
        var clientNumero: String? = null
        var clientComplemento: String? = null
        var clientBairro: String? = null
        var clientMunicipio: String? = null
        var clientUf: String? = null
        var clientCep: String? = null

        db?.query(
            ClienteContract.ClienteEntry.TABLE_NAME, null,
            "${BaseColumns._ID} = ?", arrayOf(clienteIdSalvo.toString()),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                clientNome = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NOME))
                clientEmail = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_EMAIL))
                clientTelefone = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_TELEFONE))
                clientCpf = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CPF))
                clientCnpj = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CNPJ))
                val logradouro = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_LOGRADOURO))
                val numero = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_NUMERO))
                val complemento = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_COMPLEMENTO))
                val bairro = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_BAIRRO))
                val municipio = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_MUNICIPIO))
                val uf = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_UF))
                val cep = cursor.getString(cursor.getColumnIndexOrThrow(ClienteContract.ClienteEntry.COLUMN_NAME_CEP))
            }
        }

        val clientDetailsStartX = margin + cardPadding
        val clientDetailsMaxWidth = contentWidth / 2f - cardPadding

        currentCanvas.drawText("Cliente:", clientDetailsStartX, clientInfoY + labelPaint.textSize, labelPaint)
        clientInfoY += labelPaint.textSize + 2f
        currentCanvas.drawText(clientNome ?: "N/A", clientDetailsStartX, clientInfoY + textPaint.textSize, textPaint)
        clientInfoY += textPaint.textSize + 2f
        if (!clientEmail.isNullOrEmpty()) {
            val emailLayout = StaticLayout.Builder.obtain(clientEmail!!, 0, clientEmail!!.length, textPaint, clientDetailsMaxWidth.toInt())
                .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(clientDetailsStartX, clientInfoY)
            emailLayout.draw(currentCanvas)
            currentCanvas.restore()
            clientInfoY += emailLayout.height + 2f
        }
        if (!clientTelefone.isNullOrEmpty()) {
            val telLayout = StaticLayout.Builder.obtain(clientTelefone!!, 0, clientTelefone!!.length, textPaint, clientDetailsMaxWidth.toInt())
                .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(clientDetailsStartX, clientInfoY)
            telLayout.draw(currentCanvas)
            currentCanvas.restore()
            clientInfoY += telLayout.height + 2f
        }
        if (!clientCpf.isNullOrEmpty()) {
            currentCanvas.drawText("CPF: ${clientCpf!!}", clientDetailsStartX, clientInfoY + textPaint.textSize, textPaint)
            clientInfoY += textPaint.textSize + 2f
        }
        if (!clientCnpj.isNullOrEmpty()) {
            currentCanvas.drawText("CNPJ: ${clientCnpj!!}", clientDetailsStartX, clientInfoY + textPaint.textSize, textPaint)
            clientInfoY += textPaint.textSize + 2f
        }
        var clientAddress = ""
        if (!clientLogradouro.isNullOrEmpty()) clientAddress += clientLogradouro
        if (!clientNumero.isNullOrEmpty()) clientAddress += ", ${clientNumero}"
        if (!clientBairro.isNullOrEmpty()) clientAddress += " - ${clientBairro}"
        if (!clientMunicipio.isNullOrEmpty()) clientAddress += ", ${clientMunicipio}"
        if (!clientUf.isNullOrEmpty()) clientAddress += "/${clientUf}"
        if (!clientCep.isNullOrEmpty()) clientAddress += " - CEP: ${clientCep}"

        if (clientAddress.isNotEmpty()) {
            val addressLayout = StaticLayout.Builder.obtain(clientAddress.trim(), 0, clientAddress.trim().length, textPaint, clientDetailsMaxWidth.toInt())
                .setLineSpacing(0f, 0.9f).setIncludePad(false).build()
            currentCanvas.save()
            currentCanvas.translate(clientDetailsStartX, clientInfoY)
            addressLayout.draw(currentCanvas)
            currentCanvas.restore()
            clientInfoY += addressLayout.height + 2f
        }

        // Posiciona os valores da fatura/data alinhados à direita do retângulo
        val rightEdgeForValues = pageWidth - margin - cardPadding // Coordenada X para o alinhamento da direita dos valores
        val labelValueSpacing = 3f

        // Fatura Nº
        val numFaturaLabel = "Fatura Nº:"
        val invoiceText = binding.invoiceNumberTextView.text?.toString() ?: "#${faturaId.toString().padStart(3, '0')}"

        val invoiceTextX = rightEdgeForValues - textPaint.measureText(invoiceText) // X para o valor do número da fatura
        val numFaturaLabelX = invoiceTextX - labelPaint.measureText(numFaturaLabel) - labelValueSpacing // X para o label

        currentCanvas.drawText(numFaturaLabel, numFaturaLabelX, invoiceInfoY + labelPaint.textSize*0.3f, labelPaint)
        currentCanvas.drawText(invoiceText, invoiceTextX, invoiceInfoY + labelPaint.textSize*0.3f, textPaint)
        invoiceInfoY += labelPaint.textSize + 4f

        // Emitido
        val emitidoLabel = "Emitido:"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val issuedDate = dateFormat.format(Date())

        val issuedDateTextX = rightEdgeForValues - textPaint.measureText(issuedDate) // X para o valor da data de emissão
        val emitidoLabelX = issuedDateTextX - labelPaint.measureText(emitidoLabel) - labelValueSpacing // X para o label

        currentCanvas.drawText(emitidoLabel, emitidoLabelX, invoiceInfoY + labelPaint.textSize*0.3f, labelPaint)
        currentCanvas.drawText(issuedDate, issuedDateTextX, invoiceInfoY + labelPaint.textSize*0.3f, textPaint)
        invoiceInfoY += textPaint.textSize + 10f // Espaço após a data


        // Calcula a posição final do bloco de informações antes de desenhar o retângulo
        val maxBlockY = max(clientInfoY, invoiceInfoY)
        currentCanvas.drawRect(margin, cardTop, pageWidth - margin, maxBlockY + cardPadding, borderPaint) // Retângulo envolvendo as informações
        currentYPosition = maxBlockY + cardPadding + 15f // Atualiza a posição Y para desenhar abaixo do retângulo, com espaço extra

        // Barcode - AGORA DESENHADO FORA DO RETÂNGULO E CENTRALIZADO
        val barcodeText = if (faturaId != -1L) faturaId.toString() else invoiceText.replace("#", "") // Usando invoiceText
        val barcodeBitmap = generateBarcode(barcodeText, (contentWidth * 0.6f).toInt(), 50) // Ajusta largura do barcode para centralizar melhor
        barcodeBitmap?.let {
            val barcodeX = margin + (contentWidth - it.width) / 2f // Centraliza o código de barras horizontalmente
            currentCanvas.drawBitmap(it, barcodeX, currentYPosition, null)
            currentYPosition += it.height + 15f // Adiciona altura do barcode + espaço
            it.recycle()
        }

        // NO ARTICLES TABLE
        // NO TOTALS
        // NO NOTES
        // NO PAYMENT INSTRUCTIONS

        // --- FOOTER: Page Number ---
        val pageNumText = "Pág 1"
        val pageNumTextWidth = pageNumPaint.measureText(pageNumText)
        val pageNumY = pageHeight - margin - 2f
        currentCanvas.drawText(pageNumText, pageWidth - margin - pageNumTextWidth, pageNumY + pageNumPaint.textSize*0.3f, pageNumPaint)

        pdfDocument.finishPage(currentPage)

        val fileName = "Fatura_Simplificada_${binding.invoiceNumberTextView.text?.toString()?.replace("#","") ?: faturaId}_${System.currentTimeMillis()}.pdf"

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null) {
            Log.e("SecondScreen", "Diretório de armazenamento externo não disponível para PDF simplificado.")
            showToast("Erro: Armazenamento externo não disponível para salvar PDF simplificado.")
            pdfDocument.close()
            return null
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("SecondScreen", "Não foi possível criar o diretório de documentos para PDF simplificado.")
            showToast("Erro ao criar diretório para salvar PDF simplificado.")
            pdfDocument.close()
            return null
        }

        val file = File(storageDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Log.d("SecondScreen", "PDF SIMPLIFICADO para email gerado com sucesso: ${file.absolutePath}")
            return file
        } catch (e: IOException) {
            Log.e("SecondScreen", "Erro de I/O ao salvar PDF simplificado para email: ${e.message}", e)
            showToast("Erro de I/O ao salvar PDF simplificado para email: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro geral ao salvar PDF simplificado para email: ${e.message}", e)
            showToast("Erro ao salvar PDF simplificado para email: ${e.message}")
            return null
        } finally {
            pdfDocument.close()
        }
    }


    private fun trySaveAndExit() {
        val podeSalvar = !nomeClienteSalvo.isNullOrEmpty() &&
                nomeClienteSalvo != getString(R.string.adicionar_cliente_text) &&
                artigosList.isNotEmpty()

        if (podeSalvar) {
            if (!isFaturaSaved) {
                Log.d("SecondScreen", "trySaveAndExit: Tentando salvar fatura (faturaId: $faturaId, isFaturaSaved: $isFaturaSaved).")
                saveFatura(finalizarActivityAposSalvar = true)
            } else {
                Log.d("SecondScreen", "trySaveAndExit: Fatura já foi salva (isFaturaSaved=true), apenas finalizando.")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        } else {
            Log.d("SecondScreen", "trySaveAndExit: Não há dados válidos para salvar, apenas finalizando.")
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onBackPressed() {
        Log.d("SecondScreen", "onBackPressed disparado.")
        Log.d("SecondScreen", "Estado atual: nomeClienteSalvo='$nomeClienteSalvo', artigosList.size=${artigosList.size}, isFaturaSaved=$isFaturaSaved")
        
        // Verificar se há dados para salvar
        val podeSalvar = !nomeClienteSalvo.isNullOrEmpty() &&
                nomeClienteSalvo != getString(R.string.adicionar_cliente_text) &&
                artigosList.isNotEmpty()

        if (podeSalvar && !isFaturaSaved) {
            Log.d("SecondScreen", "onBackPressed: Tentando salvar fatura antes de sair.")
            // Salvar a fatura e depois finalizar
            saveFatura(finalizarActivityAposSalvar = true)
        } else {
            Log.d("SecondScreen", "onBackPressed: Finalizando atividade sem salvar.")
            Log.d("SecondScreen", "onBackPressed: Chamando finish() para voltar à MainActivity")
            // Finalizar a atividade e voltar para a MainActivity
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("SecondScreen", "onStop: isFinishing=$isFinishing, isChangingConfigurations=$isChangingConfigurations")
        // Não salvar automaticamente se a atividade está sendo finalizada ou mudando configuração
        if (!isFinishing && !isChangingConfigurations && !isDestroyed) {
            val podeSalvar = !nomeClienteSalvo.isNullOrEmpty() &&
                    nomeClienteSalvo != getString(R.string.adicionar_cliente_text) &&
                    artigosList.isNotEmpty()

            if (podeSalvar && !isFaturaSaved) {
                Log.d("SecondScreen", "onStop: Tentando salvar a fatura não salva (ou modificada).")
                saveFatura(finalizarActivityAposSalvar = false)
            }
        }
    }

    private fun atualizarTopAdicionarClienteComNome() {
        if (!nomeClienteSalvo.isNullOrEmpty() && nomeClienteSalvo != getString(R.string.adicionar_cliente_text)) {
            binding.topAdicionarClienteTextViewSecondScreen.text = nomeClienteSalvo
        } else {
            binding.topAdicionarClienteTextViewSecondScreen.text = getString(R.string.adicionar_cliente_text)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("SecondScreen", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CLIENTES_RECENTES_REQUEST_CODE -> { // ATUALIZADO: Usando o novo request code
                    Log.d("SecondScreen", "Retorno de ClientesRecentesActivity") // Log atualizado
                    val clienteBloqueadoComSucesso = data?.getBooleanExtra("cliente_bloqueado_com_sucesso", false) ?: false
                    if (clienteBloqueadoComSucesso) {
                        Log.d("SecondScreen", "Cliente foi bloqueado. Limpando campos do cliente.")
                        nomeClienteSalvo = null
                        clienteIdSalvo = -1L
                        isFaturaSaved = false
                        atualizarTopAdicionarClienteComNome()
                    } else {
                        val nomeCliente = data?.getStringExtra("nome_cliente")
                        var idCliente = data?.getLongExtra("cliente_id", -1L) ?: -1L
                        Log.d("SecondScreen", "Dados recebidos: nome_cliente=$nomeCliente, cliente_id=$idCliente")
                        
                        if (!nomeCliente.isNullOrEmpty()) {
                            // Se o cliente_id é -1 (novo cliente), buscar o ID correto no banco Room
                            if (idCliente == -1L) {
                                try {
                                    // Buscar o cliente no banco Room
                                    val clienteViewModel = ViewModelProvider(this)[ClienteViewModel::class.java]
                                    clienteViewModel.searchClientes(nomeCliente).observe(this) { clientes ->
                                        val clienteEncontrado = clientes.find { it.nome == nomeCliente }
                                        val idClienteEncontrado = clienteEncontrado?.id ?: -1L
                                        
                                        if (idClienteEncontrado != -1L) {
                                            nomeClienteSalvo = nomeCliente
                                            clienteIdSalvo = idClienteEncontrado
                                            isFaturaSaved = false
                                            Log.d("SecondScreen", "Cliente selecionado/criado: Nome='$nomeCliente', ID=$idClienteEncontrado")
                                            atualizarTopAdicionarClienteComNome()
                                        } else {
                                            Log.w("SecondScreen", "Não foi possível obter ID válido para o cliente: $nomeCliente")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("SecondScreen", "Erro ao buscar ID do novo cliente: ${e.message}")
                                }
                            } else {
                                nomeClienteSalvo = nomeCliente
                                clienteIdSalvo = idCliente
                                isFaturaSaved = false
                                Log.d("SecondScreen", "Cliente selecionado/criado: Nome='$nomeCliente', ID=$idCliente")
                                atualizarTopAdicionarClienteComNome()
                            }
                        } else {
                            Log.w("SecondScreen", "Retorno de ClientesRecentesActivity sem nome válido.")
                        }
                    }
                }
                ARQUIVOS_RECENTES_REQUEST_CODE, CRIAR_NOVO_ARTIGO_REQUEST_CODE -> {
                    Log.d("SecondScreen", "🔄 === RETORNO DE CRIAR_NOVO_ARTIGO ===")
                    data?.let {
                        val artigoIdRetornado = it.getLongExtra("artigo_id", -1L)
                        val nomeArtigo = it.getStringExtra("nome_artigo")
                        val quantidade = it.getIntExtra("quantidade", 1)
                        val precoTotalItem = it.getDoubleExtra("valor", 0.0)
                        val numeroSerial = it.getStringExtra("numero_serial")
                        val descricao = it.getStringExtra("descricao")

                        Log.d("SecondScreen", "📥 Recebido retorno: ID=$artigoIdRetornado, Nome='$nomeArtigo', Qtd=$quantidade")

                        if (!nomeArtigo.isNullOrEmpty()) {
                            // Estratégia de busca: primeiro por ID válido, depois por nome
                            var existingArtigoIndex = -1
                            
                            if (artigoIdRetornado > 0) {
                                // Se tem ID válido, procurar pelo ID primeiro
                                existingArtigoIndex = artigosList.indexOfFirst { item -> 
                                    item.id == artigoIdRetornado 
                                }
                                Log.d("SecondScreen", "🔍 Buscando por ID $artigoIdRetornado: encontrado na posição $existingArtigoIndex")
                            }
                            
                            if (existingArtigoIndex == -1) {
                                // Se não encontrou por ID, procurar por nome (para artigos temporários)
                                existingArtigoIndex = artigosList.indexOfFirst { item -> 
                                    item.nome == nomeArtigo 
                                }
                                Log.d("SecondScreen", "🔍 Buscando por nome '$nomeArtigo': encontrado na posição $existingArtigoIndex")
                            }
                            
                            if (existingArtigoIndex != -1) {
                                // Atualizar artigo existente
                                val artigoAtualizado = ArtigoItem(artigoIdRetornado, nomeArtigo, quantidade = quantidade, preco = precoTotalItem, numeroSerial = numeroSerial, descricao = descricao)
                                artigosList[existingArtigoIndex] = artigoAtualizado
                                artigoAdapter.notifyItemChanged(existingArtigoIndex)
                                Log.d("SecondScreen", "✅ Artigo atualizado na posição $existingArtigoIndex: ID=$artigoIdRetornado, Nome='$nomeArtigo'")
                            } else {
                                // Adicionar novo artigo no topo da lista
                                val novoArtigo = ArtigoItem(artigoIdRetornado, nomeArtigo, quantidade = quantidade, preco = precoTotalItem, numeroSerial = numeroSerial, descricao = descricao)
                                artigosList.add(0, novoArtigo)
                                artigoAdapter.notifyItemInserted(0)
                                Log.d("SecondScreen", "➕ Novo artigo adicionado no topo: ID=$artigoIdRetornado, Nome='$nomeArtigo'")
                            }
                            updateSubtotal()
                            isFaturaSaved = false
                            Log.d("SecondScreen", "🔄 === FIM RETORNO DE CRIAR_NOVO_ARTIGO ===")
                        } else {
                            Log.w("SecondScreen", "⚠️ Nome do artigo está vazio, ignorando retorno")
                        }
                    } ?: run {
                        Log.w("SecondScreen", "⚠️ Data é null no retorno de CriarNovoArtigo")
                    }
                }
                THIRD_SCREEN_REQUEST_CODE -> {
                    desconto = data?.getDoubleExtra("desconto", 0.0) ?: 0.0
                    isPercentDesconto = data?.getBooleanExtra("isPercentDesconto", false) ?: false
                    taxaEntrega = data?.getDoubleExtra("taxaEntrega", 0.0) ?: 0.0
                    Log.d("SecondScreen", "Retorno de ThirdScreen: Desconto=$desconto, isPercent=$isPercentDesconto, Taxa=$taxaEntrega")
                    updateSubtotal()
                    isFaturaSaved = false
                }
                GALERIA_FOTOS_REQUEST_CODE -> {
                    data?.getStringArrayListExtra("photos")?.let {
                        fotosList.clear()
                        fotosList.addAll(it)
                        Log.d("SecondScreen", "Fotos atualizadas da GaleriaFotosActivity: ${fotosList.size} fotos.")
                        isFaturaSaved = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (faturaId == -1L && notasList.isEmpty() && !isChangingConfigurations) {
            loadNotasPadraoParaNovaFatura()
        }
        atualizarTopAdicionarClienteComNome()
    }

    private fun showAddNotaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_nota, null)
        val editTextNota = dialogView.findViewById<EditText>(R.id.editTextNota)
        val buttonConfirmarNota = dialogView.findViewById<Button>(R.id.buttonConfirmarNota)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()
        editTextNota.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editTextNota.hint = if (s?.isNotEmpty() == true) "" else "Deixe sua nota aqui"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        editTextNota.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val nota = editTextNota.text.toString().trim()
                if (nota.isNotEmpty()) {
                    notaAdapter.addNota(nota)
                    showToast("Nota adicionada com sucesso!")
                    isFaturaSaved = false
                    dialog.dismiss()
                } else {
                    showToast("A nota não pode estar vazia.")
                }
                true
            } else {
                false
            }
        }
        buttonConfirmarNota.setOnClickListener {
            val nota = editTextNota.text.toString().trim()
            if (nota.isNotEmpty()) {
                notaAdapter.addNota(nota)
                showToast("Nota adicionada com sucesso!")
                isFaturaSaved = false
                dialog.dismiss()
            } else {
                showToast("A nota não pode estar vazia.")
            }
        }
        dialog.show()
    }

    private fun loadNotasPadraoParaNovaFatura() {
        val savedNotasPadrao = notasPadraoPreferences.getString("notas", "")
        Log.d("SecondScreen", "Carregando notas padrão do SharedPreferences (NotasPrefs): $savedNotasPadrao")
        if (!savedNotasPadrao.isNullOrEmpty()) {
            val notasPadraoFromPrefs = savedNotasPadrao.split("\n").filter { it.isNotEmpty() }
            if (notasList.isEmpty()) {
                notasList.addAll(notasPadraoFromPrefs)
                Log.d("SecondScreen", "Notas padrão adicionadas à notasList (específicas da fatura): $notasPadraoFromPrefs")
            } else {
                Log.d("SecondScreen", "Notas específicas da fatura já presentes, não sobrescrevendo com notas padrão.")
            }
        } else {
            Log.d("SecondScreen", "Nenhuma nota padrão encontrada no SharedPreferences (NotasPrefs).")
        }
        if (::notaAdapter.isInitialized) {
            notaAdapter.notifyDataSetChanged()
        }
        Log.d("SecondScreen", "NotaAdapter (específicas da fatura) notificado, tamanho da lista: ${notasList.size}")
    }

    private fun viewPDF(file: File) {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao abrir PDF: ${e.message}")
            showToast("Nenhum aplicativo encontrado para abrir o PDF.")
        }
    }

    private fun sharePDF(file: File, onShared: (Boolean) -> Unit) {
        val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "Fatura ${binding.invoiceNumberTextView.text?.toString() ?: faturaId.toString()}")
            putExtra(Intent.EXTRA_TEXT, "Segue em anexo a fatura ${binding.invoiceNumberTextView.text?.toString() ?: faturaId.toString()}.")
        }
        try {
            startActivity(Intent.createChooser(intent, "Compartilhar Fatura"))
            onShared(true)
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao compartilhar PDF: ${e.message}", e)
            showToast("Nenhum aplicativo encontrado para compartilhar o PDF.")
            onShared(false)
        }
    }

    private fun marcarFaturaComoEnviada(idFatura: Long) {
        if (idFatura == -1L) {
            Log.w("SecondScreen", "ID da fatura inválido, não é possível marcar como enviada.")
            if (faturaId == -1L) faturaEnviadaSucesso = true
            return
        }
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("SecondScreen", "Erro ao acessar o banco de dados para marcar fatura como enviada.")
            showToast("Erro ao acessar o banco de dados.")
            return
        }
        try {
            val values = ContentValues().apply {
                put(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA, 1)
            }
            val rowsUpdated = db.update(
                FaturaContract.FaturaEntry.TABLE_NAME,
                values,
                "${BaseColumns._ID} = ?",
                arrayOf(idFatura.toString())
            )
            if (rowsUpdated > 0) {
                Log.i("SecondScreen", "Fatura ID $idFatura marcada como enviada no banco de dados.")
                isFaturaSaved = false
            } else {
                Log.w("SecondScreen", "Nenhuma linha atualizada ao marcar fatura ID $idFatura como enviada.")
            }
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao marcar fatura como enviada: ${e.message}", e)
            showToast("Erro ao atualizar status da fatura.")
        }
    }

    override fun finish() {
        val resultIntent = Intent()
        var algumaMudancaParaRetornar = false
        if (faturaEnviadaSucesso) {
            resultIntent.putExtra("fatura_foi_enviada", true)
            resultIntent.putExtra("fatura_id_processada", faturaId)
            algumaMudancaParaRetornar = true
            Log.d("SecondScreen", "Finalizando: faturaEnviadaSucesso=true, faturaId=$faturaId")
        }
        if (isFaturaSaved ||
            (nomeClienteSalvo != intent.getStringExtra("nome_cliente_original_da_main_activity_se_houver") &&
                    !nomeClienteSalvo.isNullOrEmpty() &&
                    nomeClienteSalvo != getString(R.string.adicionar_cliente_text))
        ) {
            resultIntent.putExtra("fatura_id_processada", faturaId)
            resultIntent.putExtra("nome_cliente_fatura", nomeClienteSalvo)
            resultIntent.putExtra("saldo_devedor_fatura", artigosList.sumOf { it.preco } - descontoValor + taxaEntrega)
            resultIntent.putExtra("data_fatura", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            if (isFaturaSaved) {
                resultIntent.putExtra("fatura_foi_enviada_status_salvo", faturaEnviadaSucesso)
            }
            algumaMudancaParaRetornar = true
            Log.d("SecondScreen", "Finalizando: isFaturaSaved=$isFaturaSaved ou cliente modificado, faturaId=$faturaId")
        }
        if (algumaMudancaParaRetornar) {
            setResult(RESULT_OK, resultIntent)
        } else {
            setResult(RESULT_CANCELED)
            Log.d("SecondScreen", "Finalizando com RESULT_CANCELED")
        }
        super.finish()
    }

    private fun loadFaturaFromDatabase(faturaIdParaCarregar: Long) {
        val db = dbHelper?.readableDatabase
        if (db == null) {
            Log.e("SecondScreen", "Erro ao acessar o banco de dados para carregar fatura")
            showToast("Erro ao acessar o banco de dados.")
            return
        }
        Log.d("SecondScreen", "Carregando fatura do banco de dados com ID: $faturaIdParaCarregar")
        val cursor: Cursor? = db.query(
            FaturaContract.FaturaEntry.TABLE_NAME, null,
            "${BaseColumns._ID} = ?", arrayOf(faturaIdParaCarregar.toString()),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                nomeClienteSalvo = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE))
                Log.d("SecondScreen", "Nome do cliente carregado do banco: $nomeClienteSalvo")
                atualizarTopAdicionarClienteComNome()
                if (!nomeClienteSalvo.isNullOrEmpty()) {
                    val clienteCursor: Cursor? = db.query(
                        ClienteContract.ClienteEntry.TABLE_NAME, arrayOf(BaseColumns._ID),
                        "${ClienteContract.ClienteEntry.COLUMN_NAME_NOME} = ?", arrayOf(nomeClienteSalvo),
                        null, null, null
                    )
                    clienteCursor?.use { c ->
                        if (c.moveToFirst()) {
                            clienteIdSalvo = c.getLong(c.getColumnIndexOrThrow(BaseColumns._ID))
                            Log.d("SecondScreen", "ID do cliente ($nomeClienteSalvo) encontrado no banco: $clienteIdSalvo")
                        } else {
                            clienteIdSalvo = -1L
                            Log.w("SecondScreen", "Cliente $nomeClienteSalvo não encontrado na tabela 'clientes' para obter ID.")
                        }
                    }
                    clienteCursor?.close()
                } else {
                    clienteIdSalvo = -1L
                }
                val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA))
                binding.invoiceNumberTextView.text = numeroFatura
                Log.d("SecondScreen", "Número da fatura carregado: $numeroFatura")
                val artigosString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS))
                artigosList.clear()
                if (!artigosString.isNullOrEmpty()) {
                    Log.d("SecondScreen", "Carregando artigos do banco: $artigosString")
                    try {
                        artigosString.split("|").forEach { artigoData ->
                            val parts = artigoData.split(",")
                            if (parts.size >= 6) {
                                val idArtigo = parts[0].toLongOrNull() ?: -System.currentTimeMillis()
                                val nome = parts[1]
                                val quantidade = parts[2].toIntOrNull() ?: 1
                                val precoTotal = parts[3].toDoubleOrNull() ?: 0.0
                                val numeroSerial = parts[4].takeIf { it.isNotEmpty() && it != "null" }
                                val descricao = parts[5].takeIf { it.isNotEmpty() && it != "null" }
                                // Corrigido: Usando os nomes corretos dos parâmetros do construtor de ArtigoItem
                                artigosList.add(ArtigoItem(idArtigo, nome, quantidade = quantidade, preco = precoTotal, numeroSerial = numeroSerial, descricao = descricao))
                            } else {
                                Log.w("SecondScreen", "Formato inválido para artigo ao carregar do banco: $artigoData")
                            }
                        }
                        artigoAdapter.notifyDataSetChanged()
                        Log.d("SecondScreen", "Artigos carregados com sucesso do banco: ${artigosList.size} itens")
                    } catch (e: Exception) {
                        Log.e("SecondScreen", "Erro ao parsear artigos do banco: ${e.message}", e)
                        showToast("Erro ao carregar os artigos da fatura.")
                    }
                }
                fotosList.clear()
                val photosCursor: Cursor? = db.query(
                    FaturaContract.FaturaFotoEntry.TABLE_NAME, arrayOf(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH),
                    "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?", arrayOf(faturaIdParaCarregar.toString()),
                    null, null, null
                )
                photosCursor?.use { photoC ->
                    while (photoC.moveToNext()) {
                        val path = photoC.getString(photoC.getColumnIndexOrThrow(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH))
                        if (File(path).exists()) {
                            fotosList.add(path)
                            Log.d("SecondScreen", "Foto carregada do banco: $path para fatura ID $faturaIdParaCarregar")
                        } else {
                            Log.w("SecondScreen", "Caminho de foto inválido ou arquivo não encontrado no banco: $path para fatura ID $faturaIdParaCarregar")
                        }
                    }
                    Log.d("SecondScreen", "Fotos carregadas do banco para fatura ID $faturaIdParaCarregar: ${fotosList.size} itens")
                }
                photosCursor?.close()
                val notasString = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS))
                notasList.clear()
                if (!notasString.isNullOrEmpty()) {
                    val notasFromDB = notasString.split("|").filter { it.isNotEmpty() }
                    notasList.addAll(notasFromDB)
                    Log.d("SecondScreen", "Notas específicas da fatura ID $faturaIdParaCarregar carregadas do banco: ${notasList.size} itens")
                } else {
                    Log.d("SecondScreen", "Nenhuma nota específica no banco para fatura ID $faturaIdParaCarregar. Carregando notas padrão.")
                    loadNotasPadraoParaNovaFatura()
                }
                notaAdapter.notifyDataSetChanged()
                desconto = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO))
                isPercentDesconto = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT)) == 1
                taxaEntrega = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA))
                faturaEnviadaSucesso = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1
                updateSubtotal()
            } else {
                Log.w("SecondScreen", "Nenhuma fatura encontrada com ID=$faturaIdParaCarregar")
                showToast("Fatura não encontrada.")
                val lastFaturaNumber = faturaPrefs.getInt("last_fatura_number", 0) + 1
                binding.invoiceNumberTextView.text = "#${lastFaturaNumber.toString().padStart(4, '0')}"
                this.faturaId = -1L
                isFaturaSaved = false
                faturaEnviadaSucesso = false
                nomeClienteSalvo = null
                clienteIdSalvo = -1L
                atualizarTopAdicionarClienteComNome()
                artigosList.clear()
                artigoAdapter.notifyDataSetChanged()
                notasList.clear()
                loadNotasPadraoParaNovaFatura()
                fotosList.clear()
                updateSubtotal()
            }
        }
        cursor?.close()
    }

    private fun savePhotoToDatabase(faturaIdParaSalvar: Long, photoPath: String) {
        if (faturaIdParaSalvar == -1L) {
            Log.w("SecondScreen", "Tentativa de salvar foto para fatura com ID -1L. Foto será salva quando a fatura for salva.")
            return
        }
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("SecondScreen", "Erro ao acessar o banco de dados para salvar foto")
            showToast("Erro ao acessar o banco de dados.")
            return
        }
        try {
            val existingCursor = db.query(
                FaturaContract.FaturaFotoEntry.TABLE_NAME, arrayOf(BaseColumns._ID),
                "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ? AND ${FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH} = ?",
                arrayOf(faturaIdParaSalvar.toString(), photoPath),
                null, null, null
            )
            if (existingCursor != null && existingCursor.moveToFirst()) {
                Log.d("SecondScreen", "Foto já existe no banco para esta fatura: $photoPath")
                existingCursor.close()
                return
            }
            existingCursor?.close()
            val values = ContentValues().apply {
                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID, faturaIdParaSalvar)
                put(FaturaContract.FaturaFotoEntry.COLUMN_NAME_PHOTO_PATH, photoPath)
            }
            val newRowId = db.insert(FaturaContract.FaturaFotoEntry.TABLE_NAME, null, values)
            if (newRowId != -1L) {
                Log.d("SecondScreen", "Foto salva no banco: $photoPath, faturaId=$faturaIdParaSalvar, rowId=$newRowId")
            } else {
                Log.e("SecondScreen", "Erro ao salvar foto no banco: $photoPath, faturaId=$faturaIdParaSalvar")
                showToast("Erro ao salvar a foto no banco de dados.")
            }
        } catch (e: Exception) {
            Log.e("SecondScreen", "Exceção ao salvar foto no banco: ${e.message}", e)
            showToast("Erro ao salvar a foto: ${e.message}")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("nomeClienteSalvo", nomeClienteSalvo)
        outState.putLong("clienteIdSalvo", clienteIdSalvo)
        outState.putParcelableArrayList("artigosList", ArrayList(artigosList))
        outState.putDouble("desconto", desconto)
        outState.putBoolean("isPercentDesconto", isPercentDesconto)
        outState.putDouble("taxaEntrega", taxaEntrega)
        outState.putDouble("descontoValor", descontoValor)
        outState.putStringArrayList("notasList", ArrayList(notasList))
        outState.putStringArrayList("fotosList", ArrayList(fotosList))
        outState.putBoolean("isFaturaSaved", isFaturaSaved)
        outState.putBoolean("faturaEnviadaSucesso", faturaEnviadaSucesso)
        Log.d("SecondScreen", "onSaveInstanceState: nomeClienteSalvo=$nomeClienteSalvo, clienteIdSalvo=$clienteIdSalvo")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("SecondScreen", "onNewIntent chamado")
        val faturaIdNova = intent.getLongExtra("fatura_id", -1L)
        if (faturaIdNova != -1L && faturaIdNova != this.faturaId) {
            this.faturaId = faturaIdNova
            loadFaturaFromDatabase(this.faturaId)
            isFaturaSaved = true
        } else {
            handleClientNameAndIdFromIntent(intent)
        }
    }

    private fun handleClientNameAndIdFromIntent(intent: Intent?) {
        val nome = intent?.getStringExtra("nome_cliente")
        val id = intent?.getLongExtra("cliente_id", -1L) ?: -1L
        if (!nome.isNullOrEmpty() && id > 0L) {
            nomeClienteSalvo = nome
            clienteIdSalvo = id
            isFaturaSaved = false
            Log.d("SecondScreen", "Nome e ID do cliente recebidos do Intent: $nome, ID: $id")
        } else if (!nome.isNullOrEmpty() && nome != getString(R.string.adicionar_cliente_text)) {
            nomeClienteSalvo = nome
            clienteIdSalvo = findClienteIdPorNome(nome)
            isFaturaSaved = false
            Log.d("SecondScreen", "Apenas nome do cliente recebido do Intent: $nome. ID encontrado: $clienteIdSalvo")
        } else {
            if (faturaId == -1L) {
                nomeClienteSalvo = null
                clienteIdSalvo = -1L
            }
        }
        
        // Processar dados do artigo se recebidos via Intent
        val nomeArtigo = intent?.getStringExtra("nome_artigo")
        val quantidade = intent?.getIntExtra("quantidade", 1) ?: 1
        val valorTotal = intent?.getDoubleExtra("valor", 0.0) ?: 0.0
        val numeroSerial = intent?.getStringExtra("numero_serial")
        val descricao = intent?.getStringExtra("descricao")
        val artigoId = intent?.getLongExtra("artigo_id", -1L) ?: -1L
        
        if (!nomeArtigo.isNullOrEmpty()) {
            Log.d("SecondScreen", "Dados do artigo recebidos via Intent: nome='$nomeArtigo', qtd=$quantidade, valor=$valorTotal")
            
            // Verificar se já existe um artigo com o mesmo nome ou ID
            var existingArtigoIndex = -1
            
            if (artigoId > 0) {
                existingArtigoIndex = artigosList.indexOfFirst { item -> 
                    item.id == artigoId 
                }
            }
            
            if (existingArtigoIndex == -1) {
                existingArtigoIndex = artigosList.indexOfFirst { item -> 
                    item.nome == nomeArtigo 
                }
            }
            
            if (existingArtigoIndex != -1) {
                // Atualizar artigo existente
                val artigoAtualizado = ArtigoItem(artigoId, nomeArtigo, quantidade = quantidade, preco = valorTotal, numeroSerial = numeroSerial, descricao = descricao)
                artigosList[existingArtigoIndex] = artigoAtualizado
                artigoAdapter.notifyItemChanged(existingArtigoIndex)
                Log.d("SecondScreen", "Artigo atualizado na posição $existingArtigoIndex")
            } else {
                // Adicionar novo artigo no topo da lista
                val novoArtigo = ArtigoItem(artigoId, nomeArtigo, quantidade = quantidade, preco = valorTotal, numeroSerial = numeroSerial, descricao = descricao)
                artigosList.add(0, novoArtigo)
                artigoAdapter.notifyItemInserted(0)
                Log.d("SecondScreen", "Novo artigo adicionado no topo: $nomeArtigo")
            }
            
            updateSubtotal()
            isFaturaSaved = false
        }
        
        atualizarTopAdicionarClienteComNome()
    }

    private fun findClienteIdPorNome(nomeCliente: String?): Long {
        if (nomeCliente.isNullOrEmpty()) return -1L
        
        // Por enquanto, vamos retornar -1L e deixar que o código que chama esta função
        // use a busca assíncrona do Room quando necessário
        // Esta função é usada principalmente em handleClientNameAndIdFromIntent
        // que já foi atualizada para usar o Room
        Log.d("SecondScreen", "findClienteIdPorNome chamado para '$nomeCliente', retornando -1L (será buscado assincronamente)")
        return -1L
    }

    private fun updateSubtotal() {
        val baseSubtotal = artigosList.sumOf { it.preco }
        Log.d("SecondScreen", "Calculando subtotal. Base Subtotal (soma dos preços dos artigos): $baseSubtotal")
        binding.subtotalValueTextViewSecondScreen.text = decimalFormat.format(baseSubtotal)
        descontoValor = if (isPercentDesconto) {
            (baseSubtotal * desconto) / 100.0
        } else {
            desconto
        }
        Log.d("SecondScreen", "Desconto (valor numérico): $desconto, isPercent=$isPercentDesconto, Desconto calculado (descontoValor): $descontoValor")
        val descontoTextoExibicao = if (isPercentDesconto) {
            "${String.format(Locale("pt", "BR"), "%.2f", desconto)}% (${decimalFormat.format(descontoValor)})"
        } else {
            "(${decimalFormat.format(descontoValor)})"
        }
        binding.descontoValueTextViewSecondScreen.text = descontoTextoExibicao
        Log.d("SecondScreen", "Texto do desconto (UI): $descontoTextoExibicao")
        binding.taxaEntregaValueTextViewSecondScreen.text = decimalFormat.format(taxaEntrega)
        Log.d("SecondScreen", "Taxa de entrega (UI): ${decimalFormat.format(taxaEntrega)}")
        val saldoDevedor = baseSubtotal - descontoValor + taxaEntrega
        binding.saldoDevedorValueTextView.text = decimalFormat.format(saldoDevedor)
        Log.d("SecondScreen", "Saldo devedor calculado (UI): ${decimalFormat.format(saldoDevedor)}")
        
        // Ajustar tamanho da fonte do texto "Saldo Devedor" conforme o valor
        adjustSaldoTextSize(saldoDevedor)
    }

    private fun adjustSaldoTextSize(saldoDevedor: Double) {
        val saldoTextView = binding.saldoDevedorTextView
        val saldoValueTextView = binding.saldoDevedorValueTextView
        
        // Obter o texto formatado do valor
        val valorText = decimalFormat.format(saldoDevedor)
        
        // Definir tamanhos de fonte baseados no tamanho do valor
        val (textSize, valueTextSize) = when {
            valorText.length <= 8 -> Pair(20f, 20f) // R$ 0,00 até R$ 999,99
            valorText.length <= 10 -> Pair(18f, 18f) // R$ 1.000,00 até R$ 99.999,99
            valorText.length <= 12 -> Pair(16f, 16f) // R$ 100.000,00 até R$ 999.999,99
            valorText.length <= 14 -> Pair(14f, 14f) // R$ 1.000.000,00 até R$ 9.999.999,99
            else -> Pair(12f, 12f) // Valores muito grandes
        }
        
        // Aplicar os tamanhos de fonte
        saldoTextView.textSize = textSize
        saldoValueTextView.textSize = valueTextSize
        
        Log.d("SecondScreen", "Tamanho da fonte ajustado: texto=$textSize, valor=$valueTextSize para valor '$valorText' (${valorText.length} caracteres)")
    }

    private fun adjustSaldoPaintForPdf(originalPaint: TextPaint, saldoText: String, availableWidth: Float): TextPaint {
        // Criar uma cópia do paint original
        val adjustedPaint = TextPaint(originalPaint)
        
        // Definir tamanhos de fonte baseados no tamanho do valor para PDF
        val baseTextSize = when {
            saldoText.length <= 8 -> 12f // R$ 0,00 até R$ 999,99
            saldoText.length <= 10 -> 11f // R$ 1.000,00 até R$ 99.999,99
            saldoText.length <= 12 -> 10f // R$ 100.000,00 até R$ 999.999,99
            saldoText.length <= 14 -> 9f // R$ 1.000.000,00 até R$ 9.999.999,99
            else -> 8f // Valores muito grandes
        }
        
        // Calcular se o texto cabe no espaço disponível
        adjustedPaint.textSize = baseTextSize
        val labelText = "Saldo Devedor:"
        val totalTextWidth = adjustedPaint.measureText(labelText) + adjustedPaint.measureText(saldoText) + 10f // 10f de margem
        
        // Se não couber, reduzir ainda mais o tamanho da fonte
        if (totalTextWidth > availableWidth) {
            val scaleFactor = availableWidth / totalTextWidth
            adjustedPaint.textSize = baseTextSize * scaleFactor * 0.9f // 0.9f para garantir margem
        }
        
        Log.d("SecondScreen", "PDF - Tamanho da fonte ajustado: ${adjustedPaint.textSize} para valor '$saldoText' (${saldoText.length} caracteres)")
        
        return adjustedPaint
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateCurrentDate(dateTextView: TextView) {
        val sdf = SimpleDateFormat("dd MMM yy", Locale("pt", "BR"))
        val currentDate = sdf.format(Date())
        dateTextView.text = currentDate
    }

    private fun saveFatura(finalizarActivityAposSalvar: Boolean = true) {
        Log.d("SecondScreen", "saveFatura() iniciado - finalizarActivityAposSalvar: $finalizarActivityAposSalvar")
        
        // Evitar salvamento duplo
        if (isFaturaSaved && finalizarActivityAposSalvar) {
            Log.d("SecondScreen", "Fatura já salva, apenas finalizando atividade")
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return
        }
        
        if (nomeClienteSalvo.isNullOrEmpty() || nomeClienteSalvo == getString(R.string.adicionar_cliente_text)) {
            Log.w("SecondScreen", "Validação falhou: cliente vazio ou não selecionado")
            showToast("O nome do cliente é obrigatório para salvar a fatura!")
            if (finalizarActivityAposSalvar) {
                Log.d("SecondScreen", "Fechando atividade devido a cliente vazio")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            return
        }
        if (artigosList.isEmpty()) {
            Log.w("SecondScreen", "Validação falhou: lista de artigos vazia")
            showToast("Adicione pelo menos um artigo para salvar a fatura!")
            if (finalizarActivityAposSalvar) {
                Log.d("SecondScreen", "Fechando atividade devido a artigos vazios")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            return
        }
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("SecondScreen", "Erro ao acessar o banco de dados: dbHelper.writableDatabase retornou null")
            showToast("Erro ao acessar o banco de dados.")
            if (finalizarActivityAposSalvar) {
                Log.d("SecondScreen", "Fechando atividade devido a erro no banco")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
            return
        }
        Log.d("SecondScreen", "Validações passaram, iniciando salvamento no banco")
        try {
            db.beginTransaction()
            val baseSubtotal = artigosList.sumOf { it.preco }
            val saldoDevedorCalculado = baseSubtotal - descontoValor + taxaEntrega
            val artigosString = artigosList.joinToString(separator = "|") {
                val idArtigoParaString = if (it.id > 0) it.id.toString() else ""
                // Corrigido: Usando os nomes corretos dos campos do ArtigoItem
                "$idArtigoParaString,${it.nome},${it.quantidade},${it.preco},${it.numeroSerial ?: ""},${it.descricao ?: ""}"
            }
            Log.d("SecondScreen", "Artigos convertidos para string para salvar: $artigosString")
            val notasEspecificasString = notasList.joinToString("|").ifEmpty { "" }
            Log.d("SecondScreen", "Notas específicas da fatura para salvar: $notasEspecificasString (tamanho: ${notasList.size})")
            val values = ContentValues().apply {
                put(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE, nomeClienteSalvo)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS, artigosString)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL, baseSubtotal)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO, desconto)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT, if (isPercentDesconto) 1 else 0)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA, taxaEntrega)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR, saldoDevedorCalculado)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_DATA, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS, notasEspecificasString)
                put(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA, binding.invoiceNumberTextView.text.toString())
                put(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA, if (faturaEnviadaSucesso) 1 else 0)
            }
            Log.d("SecondScreen", "ContentValues preparado para salvar/atualizar fatura: $values")
            
            var faturaSalvaComSucesso = false
            var novoFaturaId = faturaId
            
            if (faturaId != -1L) {
                val rowsUpdated = db.update(
                    FaturaContract.FaturaEntry.TABLE_NAME, values,
                    "${BaseColumns._ID} = ?", arrayOf(faturaId.toString())
                )
                Log.d("SecondScreen", "Tentativa de atualização da fatura ID=$faturaId, linhas atualizadas: $rowsUpdated")
                if (rowsUpdated > 0) {
                    db.delete(FaturaContract.FaturaFotoEntry.TABLE_NAME, "${FaturaContract.FaturaFotoEntry.COLUMN_NAME_FATURA_ID} = ?", arrayOf(faturaId.toString()))
                    fotosList.forEach { path -> savePhotoToDatabase(faturaId, path) }
                    updateFaturaItensAssociations(db, faturaId)
                    faturaSalvaComSucesso = true
                    db.setTransactionSuccessful()
                    Log.d("SecondScreen", "Fatura atualizada com sucesso no SQLite")
                } else {
                    Log.e("SecondScreen", "Erro ao atualizar fatura ID=$faturaId: nenhuma linha afetada")
                    showToast("Erro ao atualizar a fatura.")
                }
            } else {
                val newRowId = db.insert(FaturaContract.FaturaEntry.TABLE_NAME, null, values)
                Log.d("SecondScreen", "Tentativa de inserção de nova fatura, ID retornado: $newRowId")
                if (newRowId != -1L) {
                    faturaId = newRowId
                    novoFaturaId = newRowId
                    val currentFaturaNumberStr = binding.invoiceNumberTextView.text.toString().replace("#", "")
                    if (currentFaturaNumberStr.isNotEmpty()) {
                        val currentFaturaNumber = currentFaturaNumberStr.toIntOrNull()
                        currentFaturaNumber?.let {
                            faturaPrefs.edit().putInt("last_fatura_number", it).apply()
                            Log.d("SecondScreen", "Último número de fatura salvo no SharedPreferences: $it")
                        }
                    }
                    fotosList.forEach { path -> savePhotoToDatabase(faturaId, path) }
                    updateFaturaItensAssociations(db, faturaId)
                    faturaSalvaComSucesso = true
                    db.setTransactionSuccessful()
                    Log.d("SecondScreen", "Fatura inserida com sucesso no SQLite")
                } else {
                    Log.e("SecondScreen", "Erro ao inserir fatura: inserção falhou")
                    showToast("Erro ao salvar a fatura.")
                }
            }
            
            // Se salvou com sucesso no SQLite, também salva no Room
            if (faturaSalvaComSucesso) {
                Log.d("SecondScreen", "Salvando no Room e fechando atividade")
                // Marcar como salva antes de fechar
                isFaturaSaved = true
                // Fechar a atividade imediatamente após salvar no SQLite
                if (finalizarActivityAposSalvar) {
                    Log.d("SecondScreen", "Fechando atividade imediatamente após salvar no SQLite")
                    // Salvar no Room em background (sem bloquear a UI)
                    saveFaturaToRoom(novoFaturaId, values, false)
                    // Finalizar a atividade
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                } else {
                    // Salvar no Room em background (sem bloquear a UI)
                    saveFaturaToRoom(novoFaturaId, values, false)
                }
            } else {
                // Se não salvou com sucesso, ainda fechar a atividade se solicitado
                if (finalizarActivityAposSalvar) {
                    Log.d("SecondScreen", "Fechando atividade mesmo com falha no salvamento")
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SecondScreen", "Exceção ao salvar fatura: ${e.message}", e)
            showToast("Erro ao salvar a fatura: ${e.message}")
            if (finalizarActivityAposSalvar) {
                Log.d("SecondScreen", "Fechando atividade devido a exceção")
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        } finally {
            db?.endTransaction()
            Log.d("SecondScreen", "Transação do banco finalizada")
        }
    }

    private fun saveFaturaToRoom(faturaId: Long, values: ContentValues, finalizarActivityAposSalvar: Boolean) {
        Log.d("SecondScreen", "saveFaturaToRoom() iniciado - faturaId: $faturaId, finalizarActivityAposSalvar: $finalizarActivityAposSalvar")
        
        lifecycleScope.launch {
            try {
                val fatura = Fatura(
                    id = faturaId,
                    numeroFatura = values.getAsString(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA),
                    cliente = values.getAsString(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE),
                    artigos = values.getAsString(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS),
                    subtotal = values.getAsDouble(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL),
                    desconto = values.getAsDouble(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO),
                    descontoPercent = values.getAsInteger(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT),
                    taxaEntrega = values.getAsDouble(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA),
                    saldoDevedor = values.getAsDouble(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR),
                    data = values.getAsString(FaturaContract.FaturaEntry.COLUMN_NAME_DATA),
                    fotosImpressora = "", // Será preenchido posteriormente se necessário
                    notas = values.getAsString(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS),
                    foiEnviada = values.getAsInteger(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA) == 1
                )
                
                faturaRepository.insertFatura(fatura)
                Log.d("SecondScreen", "Fatura salva no Room com sucesso: ID=$faturaId")
                
                // Mostrar toast apenas se a atividade ainda estiver ativa
                if (!isFinishing && !isDestroyed) {
                    showToast("Fatura salva com sucesso!")
                }
                
            } catch (e: Exception) {
                Log.e("SecondScreen", "Erro ao salvar fatura no Room: ${e.message}", e)
                // Não mostrar erro para o usuário pois o SQLite já foi salvo
            }
        }
    }

    private fun updateFaturaItensAssociations(db: SQLiteDatabase, faturaId: Long) {
        // Verificar se a tabela fatura_items existe
        val tableExists = try {
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(FaturaContract.FaturaItemEntry.TABLE_NAME))
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao verificar se tabela fatura_items existe: ${e.message}")
            false
        }

        if (!tableExists) {
            Log.w("SecondScreen", "Tabela fatura_items não existe, pulando atualização de associações")
            return
        }

        try {
            db.delete(
                FaturaContract.FaturaItemEntry.TABLE_NAME,
                "${FaturaContract.FaturaItemEntry.COLUMN_NAME_FATURA_ID} = ?",
                arrayOf(faturaId.toString())
            )
            artigosList.forEach { artigo ->
                val values = ContentValues().apply {
                    put(FaturaContract.FaturaItemEntry.COLUMN_NAME_FATURA_ID, faturaId)
                    put(FaturaContract.FaturaItemEntry.COLUMN_NAME_ARTIGO_ID, artigo.id.takeIf { it > 0 })
                    put(FaturaContract.FaturaItemEntry.COLUMN_NAME_QUANTIDADE, artigo.quantidade) // Usando 'quantidade'
                    put(FaturaContract.FaturaItemEntry.COLUMN_NAME_PRECO, if (artigo.quantidade > 0) artigo.preco / artigo.quantidade else artigo.preco) // Usando 'quantidade' e 'preco'
                    put(FaturaContract.FaturaItemEntry.COLUMN_NAME_CLIENTE_ID, clienteIdSalvo.takeIf { it > 0 })
                }
                val newRowId = db.insert(FaturaContract.FaturaItemEntry.TABLE_NAME, null, values)
                if (newRowId != -1L) {
                    Log.d("SecondScreen", "Item de fatura salvo: artigoId=${artigo.id}, clienteId=$clienteIdSalvo")
                } else {
                    Log.w("SecondScreen", "Falha ao salvar item de fatura: artigoId=${artigo.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("SecondScreen", "Erro ao atualizar associações de fatura: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}