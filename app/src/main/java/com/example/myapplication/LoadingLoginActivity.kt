package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import database.AppDatabase
import database.utils.DataMigrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LoadingLoginActivity : AppCompatActivity() {

    private lateinit var imageViewLogo: ImageView
    private lateinit var progressBarHorizontal: ProgressBar
    private lateinit var textViewPercentage: TextView

    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    private val TOTAL_INITIALIZATION_TASKS = 3
    private var completedTasks = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_login)

        imageViewLogo = findViewById(R.id.imageViewLogoLoading)
        progressBarHorizontal = findViewById(R.id.progressBarHorizontalLoading)
        textViewPercentage = findViewById(R.id.textViewPercentage)

        val rotateAnimation: Animation = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely)
        imageViewLogo.startAnimation(rotateAnimation)

        startLoadingProcess()
    }

    private fun startLoadingProcess() {
        completedTasks = 0
        updateProgressUI()

        uiScope.launch {
            delay(300L)

            // Tarefa 1: Inicialização do Banco de Dados Room
            var initializationFailed = false
            withContext(Dispatchers.IO) {
                try {
                    // Inicializa o banco Room
                    val database = AppDatabase.getDatabase(applicationContext)
                    // Testa se o banco está funcionando - apenas verifica se consegue acessar
                    database.clienteDao()
                    Log.d("LoadingLoginActivity", "Banco Room inicializado com sucesso")
                } catch (e: Exception) {
                    initializationFailed = true
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoadingLoginActivity, "Erro crítico ao inicializar o banco de dados. O aplicativo será fechado.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (initializationFailed) {
                delay(2000L)
                finishAffinity()
                return@launch
            }

            completedTasks++
            updateProgressUI()

            // Tarefa 2: Verificação de Banco Antigo (opcional - para migração futura)
            withContext(Dispatchers.IO) {
                try {
                    val migrationHelper = DataMigrationHelper(applicationContext)
                    if (migrationHelper.hasOldDatabase()) {
                        val stats = migrationHelper.getMigrationStats()
                        Log.d("LoadingLoginActivity", "Banco antigo detectado: ${stats.totalRecords} registros encontrados")
                        // Aqui você pode adicionar migração automática no futuro se necessário
                    } else {
                        Log.d("LoadingLoginActivity", "Nenhum banco antigo encontrado - usando Room diretamente")
                    }
                } catch (e: Exception) {
                    Log.e("LoadingLoginActivity", "Erro ao verificar banco antigo: ${e.message}")
                    // Não falha a inicialização se houver erro na verificação
                }
            }

            completedTasks++
            updateProgressUI()

            // Tarefa 3: Carregamento de Preferências Iniciais
            withContext(Dispatchers.IO) {
                try {
                    val prefs = applicationContext.getSharedPreferences("InformacoesEmpresaPrefs", MODE_PRIVATE)
                    prefs.getString("nome_empresa", "")
                    val logoPrefs = applicationContext.getSharedPreferences("LogotipoPrefs", MODE_PRIVATE)
                    logoPrefs.getString("logo_uri", null)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoadingLoginActivity, "Erro ao carregar configurações iniciais. Alguns dados podem estar faltando.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            completedTasks++
            updateProgressUI()

            // Quando todas as tarefas de inicialização estiverem concluídas:
            allTasksCompleted()
        }
    }

    private fun updateProgressUI() {
        val progressPercentage = (completedTasks.toFloat() / TOTAL_INITIALIZATION_TASKS.toFloat() * 100).toInt()
        progressBarHorizontal.progress = progressPercentage
        textViewPercentage.text = String.format(Locale.getDefault(), "%d%%", progressPercentage)
    }

    private fun allTasksCompleted() {
        imageViewLogo.clearAnimation()
        
        // Sempre vai para MainActivity
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
    }
}