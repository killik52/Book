package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private var isGridViewVisible = false
    private lateinit var faturaAdapter: FaturaResumidaAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var emptyStateView: View? = null
    private var errorStateView: View? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            "Leitura cancelada".logDebug("MainActivity")
        } else {
            val barcodeValue = result.contents
            "Código de barras lido (bruto): '$barcodeValue'".logDebug("MainActivity")
            emitBeep()
            openInvoiceByBarcode(barcodeValue)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        "onCreate chamado com ViewBinding".logDebug("MainActivity")

        setupSwipeRefresh()
        setupMediaPlayer()
        setupRecyclerView()
        setupObservers()
        setupMenuGrid()
        setupClickListeners()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.carregarFaturas()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep)
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                "Erro no MediaPlayer: what=$what, extra=$extra".logError("MainActivity")
                showToast("Erro ao inicializar o som de beep")
                true
            }
        } catch (e: Exception) {
            "Erro ao inicializar MediaPlayer: ${e.message}".logError("MainActivity")
            showToast("Erro ao carregar o som de beep")
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewFaturas.layoutManager = LinearLayoutManager(this)
        faturaAdapter = FaturaResumidaAdapter(
            this,
            onItemClick = { fatura ->
                openFaturaDetails(fatura)
            },
            onItemLongClick = { fatura ->
                moveFaturaToLixeira(fatura)
            }
        )
        binding.recyclerViewFaturas.adapter = faturaAdapter

        // Adiciona espaçamento entre itens usando constantes
        val spaceInPixels = (Constants.UI.SPACE_BETWEEN_ITEMS_DP * resources.displayMetrics.density).toInt()
        binding.recyclerViewFaturas.addItemDecoration(VerticalSpaceItemDecoration(spaceInPixels))
    }

    private fun setupObservers() {
        // Observa o estado da UI
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                when (uiState) {
                    is MainActivityUiState.Loading -> {
                        showLoading()
                        "Carregando faturas...".logDebug("MainActivity")
                    }
                    is MainActivityUiState.Success -> {
                        hideLoading()
                        faturaAdapter.updateFaturas(uiState.faturas)
                        "Faturas carregadas: ${uiState.faturas.size}".logDebug("MainActivity")
                        if (uiState.faturas.isEmpty()) showEmptyState() else hideEmptyState()
                    }
                    is MainActivityUiState.Error -> {
                        hideLoading()
                        showError(uiState.message)
                        "Erro: ${uiState.message}".logError("MainActivity")
                        showErrorState(uiState.message)
                    }
                }
            }
        }

        // Observa resultados de busca
        lifecycleScope.launch {
            viewModel.searchResults.collectLatest { searchResults ->
                faturaAdapter.updateFaturas(searchResults)
                "Resultados de busca: ${searchResults.size}".logDebug("MainActivity")
            }
        }

        // Observa se está em modo de busca
        lifecycleScope.launch {
            viewModel.isSearchActive.collectLatest { isSearchActive ->
                "Modo de busca: $isSearchActive".logDebug("MainActivity")
            }
        }
    }

    private fun setupMenuGrid() {
        val menuOptionsAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.menu_options,
            R.layout.item_menu
        )
        binding.menuGridView.adapter = menuOptionsAdapter

        binding.menuGridView.setOnItemClickListener { _, _, position, _ ->
            try {
                val selectedOption = menuOptionsAdapter.getItem(position).toString()
                when (selectedOption) {
                    "Fatura" -> toggleGridView()
                    "Cliente" -> {
                        startActivity(Intent(this, ListarClientesActivityRoom::class.java))
                        toggleGridView()
                    }
                    "Artigo" -> {
                        startActivity(Intent(this, ListarArtigosActivityRoom::class.java))
                        toggleGridView()
                    }
                    "Lixeira" -> {
                        startActivity(Intent(this, LixeiraActivityRoom::class.java))
                        toggleGridView()
                    }
                }
            } catch (e: Exception) {
                "Erro ao abrir atividade: ${e.message}".logError("MainActivity")
                showToast("Erro ao abrir a tela: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        // Clique no container do título da fatura
        binding.faturaTitleContainer.setOnClickListener {
            "Clique no faturaTitleContainer detectado".logDebug("MainActivity")
            toggleGridView()
        }

        // Clique direto no texto da fatura
        binding.faturaTitleTextView.setOnClickListener {
            "Clique no faturaTitleTextView detectado".logDebug("MainActivity")
            toggleGridView()
        }

        // Clique na seta
        binding.faturaTitleArrow.setOnClickListener {
            "Clique no faturaTitleArrow detectado".logDebug("MainActivity")
            toggleGridView()
        }

        // Adicionar touch listener para debug
        binding.faturaTitleContainer.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    "Touch DOWN no faturaTitleContainer".logDebug("MainActivity")
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    "Touch UP no faturaTitleContainer".logDebug("MainActivity")
                    toggleGridView()
                    true
                }
                else -> false
            }
        }

        binding.dollarIcon.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.CODE_128)
                setPrompt("Escaneie o código de barras no PDF")
                setCameraId(0)
                setBeepEnabled(false)
                setOrientationLocked(false)
            }
            barcodeLauncher.launch(options)
        }

        binding.homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.moreIcon.setOnClickListener {
            val intent = Intent(this, DefinicoesActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.searchButton.setOnClickListener {
            showSearchDialog()
        }

        binding.graficosButton.setOnClickListener {
            "Botão de Gráficos clicado".logDebug("MainActivity")
            val intent = Intent(this, ResumoFinanceiroActivity::class.java)
            startActivity(intent)
        }

        binding.addButton.setOnClickListener {
            requestStorageAndCameraPermissions()
        }
    }

    private fun showSearchDialog() {
        "Botão de busca clicado".logDebug("MainActivity")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.search_dialog_title))

        val input = EditText(this)
        input.hint = getString(R.string.search_dialog_hint)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.search_dialog_positive_button)) { dialog, _ ->
            val query = input.textOrEmpty().trim()
            "Busca com termo: '$query'".logDebug("MainActivity")
            if (query.isEmpty()) {
                showToast(getString(R.string.search_empty_query_message))
                viewModel.limparBusca()
            } else {
                viewModel.buscarFaturas(query)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.search_dialog_negative_button)) { dialog, _ ->
            "Busca cancelada".logDebug("MainActivity")
            dialog.cancel()
        }
        builder.show()
    }

    private fun openFaturaDetails(fatura: FaturaResumidaItem) {
        viewModel.obterFaturaPorId(
            fatura.id,
            onSuccess = { faturaCompleta ->
                faturaCompleta?.let {
                    val intent = Intent(this, SecondScreenActivity::class.java).apply {
                        putExtra("fatura_id", fatura.id)
                        putExtra("foi_enviada", fatura.foiEnviada)
                    }
                    startActivityForResult(intent, Constants.SECOND_SCREEN_REQUEST_CODE)
                } ?: run {
                    showToast("Fatura não encontrada.")
                }
            },
            onError = { errorMessage ->
                showToast("Erro ao abrir fatura: $errorMessage")
            }
        )
    }

    private fun moveFaturaToLixeira(fatura: FaturaResumidaItem) {
        "Movendo fatura ID=${fatura.id} para lixeira".logDebug("MainActivity")
        viewModel.moverFaturaParaLixeira(
            fatura,
            onSuccess = {
                "Fatura movida para lixeira com sucesso".logDebug("MainActivity")
            },
            onError = { errorMessage ->
                showToast("Erro ao mover fatura: $errorMessage")
            }
        )
    }

    private fun openInvoiceByBarcode(barcodeValue: String) {
        val cleanedBarcodeValue = barcodeValue.trim()
        val faturaIdFromBarcode = cleanedBarcodeValue.toLongOrNull() 
            ?: cleanedBarcodeValue.replace("[^0-9]".toRegex(), "").toLongOrNull()

        if (faturaIdFromBarcode == null) {
            showToast("Código de barras inválido: $cleanedBarcodeValue")
            return
        }

        viewModel.obterFaturaPorId(
            faturaIdFromBarcode,
            onSuccess = { fatura ->
                fatura?.let {
                    "Fatura encontrada com ID: $faturaIdFromBarcode. Foi enviada: ${it.foiEnviada}".logDebug("MainActivity")
                    val intent = Intent(this, SecondScreenActivity::class.java).apply {
                        putExtra("fatura_id", faturaIdFromBarcode)
                        putExtra("foi_enviada", it.foiEnviada)
                    }
                    startActivityForResult(intent, Constants.SECOND_SCREEN_REQUEST_CODE)
                } ?: run {
                    "Fatura não encontrada com ID: $faturaIdFromBarcode".logError("MainActivity")
                    showToast("Fatura não encontrada para o código de barras: $cleanedBarcodeValue")
                }
            },
            onError = { errorMessage ->
                "Erro ao abrir fatura por código de barras: $errorMessage".logError("MainActivity")
                showToast("Erro ao abrir fatura: $errorMessage")
            }
        )
    }

    private fun emitBeep() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                    try {
                        player.prepare()
                    } catch (e: IllegalStateException) {
                        "Erro ao preparar MediaPlayer após stop: ${e.message}".logError("MainActivity")
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                    }
                }
                player.start()
            } ?: run {
                mediaPlayer = MediaPlayer.create(this, R.raw.beep)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            "Erro ao reproduzir som de beep: ${e.message}".logError("MainActivity")
        }
    }

    override fun onResume() {
        super.onResume()
        "onResume chamado - MainActivity recebeu foco".logDebug("MainActivity")
        viewModel.carregarFaturas()
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            val isSearchActive = viewModel.isSearchActive.first()
            if (isSearchActive) {
                "Botão Voltar pressionado, busca estava ativa. Restaurando lista completa.".logDebug("MainActivity")
                viewModel.limparBusca()
            } else if (isGridViewVisible) {
                toggleGridView()
            } else {
                // Se estamos na MainActivity e não há mais atividades na pilha, fechar o app
                if (isTaskRoot) {
                    finishAffinity()
                } else {
                    super.onBackPressed()
                }
            }
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkCameraPermission(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAndCameraPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!checkCameraPermission()) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (!checkStoragePermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), Constants.STORAGE_PERMISSION_CODE)
        } else {
            openSecondScreen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            var allEssentialPermissionsGranted = true
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]

                if (permission == Manifest.permission.CAMERA ||
                    permission == Manifest.permission.READ_MEDIA_IMAGES ||
                    permission == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allEssentialPermissionsGranted = false
                    }
                }
            }

            if (allEssentialPermissionsGranted) {
                openSecondScreen()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissões Necessárias")
            .setMessage("Para usar esta funcionalidade, você precisa conceder as permissões de câmera e armazenamento.")
            .setPositiveButton("Configurações") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openSecondScreen() {
        val intent = Intent(this, SecondScreenActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun toggleGridView() {
        "toggleGridView chamado. isGridViewVisible atual: $isGridViewVisible".logDebug("MainActivity")
        
        if (isGridViewVisible) {
            // Fechando o GridView
            "Fechando GridView com animação slide_up".logDebug("MainActivity")
            val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slideUpAnimation.duration = Constants.UI.ANIMATION_DURATION
            
            slideUpAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    "Animação slide_up iniciada".logDebug("MainActivity")
                }
                override fun onAnimationEnd(animation: Animation?) {
                    binding.menuGridView.visibility = View.GONE
                    "Animação slide_up finalizada. GridView oculto".logDebug("MainActivity")
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            
            binding.menuGridView.startAnimation(slideUpAnimation)
            isGridViewVisible = false
            
        } else {
            // Abrindo o GridView
            "Abrindo GridView com animação slide_down".logDebug("MainActivity")
            
            // Primeiro torna visível
            binding.menuGridView.visibility = View.VISIBLE
            
            val slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDownAnimation.duration = Constants.UI.ANIMATION_DURATION
            
            slideDownAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    "Animação slide_down iniciada".logDebug("MainActivity")
                }
                override fun onAnimationEnd(animation: Animation?) {
                    "Animação slide_down finalizada".logDebug("MainActivity")
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            
            binding.menuGridView.startAnimation(slideDownAnimation)
            isGridViewVisible = true
        }
        
        "Estado isGridViewVisible alterado para: $isGridViewVisible".logDebug("MainActivity")
    }

    // Novos métodos para melhorar UX
    private fun showLoading() {
        // TODO: Adicionar ProgressBar ao layout quando necessário
        // binding.progressBar?.show()
        // binding.recyclerViewFaturas.invisible()
    }

    private fun hideLoading() {
        // TODO: Adicionar ProgressBar ao layout quando necessário
        // binding.progressBar?.hide()
        // binding.recyclerViewFaturas.show()
    }

    private fun showEmptyState() {
        if (emptyStateView == null) {
            emptyStateView = LayoutInflater.from(this).inflate(R.layout.layout_empty_state, binding.root, false)
            binding.root.addView(emptyStateView)
        }
        emptyStateView?.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.GONE
        errorStateView?.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateView?.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showErrorState(message: String) {
        if (errorStateView == null) {
            errorStateView = LayoutInflater.from(this).inflate(R.layout.layout_error_state, binding.root, false)
            binding.root.addView(errorStateView)
        }
        
        val errorTextView = errorStateView?.findViewById<TextView>(R.id.textViewErrorState)
        val retryButton = errorStateView?.findViewById<Button>(R.id.buttonRetry)
        
        errorTextView?.text = message
        retryButton?.setOnClickListener {
            hideErrorState()
            viewModel.carregarFaturas()
        }
        
        errorStateView?.visibility = View.VISIBLE
        binding.swipeRefreshLayout.visibility = View.GONE
        emptyStateView?.visibility = View.GONE
    }

    private fun hideErrorState() {
        errorStateView?.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        showErrorState(message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.SECOND_SCREEN_REQUEST_CODE && resultCode == RESULT_OK) {
            viewModel.carregarFaturas()
        } else if (requestCode == Constants.LIXEIRA_REQUEST_CODE && resultCode == RESULT_OK) {
            viewModel.carregarFaturas()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}