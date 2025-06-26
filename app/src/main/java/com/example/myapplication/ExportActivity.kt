package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.utils.PdfGenerationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExportActivity : AppCompatActivity() {

    private lateinit var closeExportButton: ImageButton
    private lateinit var buttonSelectExportPeriod: Button
    private lateinit var radioGroupExportOptions: RadioGroup
    private lateinit var radioOptionFatura: RadioButton
    private lateinit var radioOptionCliente: RadioButton
    private lateinit var radioOptionArtigo: RadioButton
    private lateinit var buttonExportData: Button

    private var dbHelper: ClienteDbHelper? = null
    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var dataInicioExportacao: Calendar? = null
    private var dataFimExportacao: Calendar? = null
    private var periodoExportacaoTipo: String = "Todo o Período" // Padrão de exibição, mas ajustaremos para exportação

    private var tipoDadoExportacao: String = "Fatura"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        dbHelper = ClienteDbHelper(this)

        closeExportButton = findViewById(R.id.closeExportButton)
        buttonSelectExportPeriod = findViewById(R.id.buttonSelectExportPeriod)
        radioGroupExportOptions = findViewById(R.id.radioGroupExportOptions)
        radioOptionFatura = findViewById(R.id.radioOptionFatura)
        radioOptionCliente = findViewById(R.id.buttonOptionCliente)
        radioOptionArtigo = findViewById(R.id.buttonOptionArtigo)
        buttonExportData = findViewById(R.id.buttonExportData)

        closeExportButton.setOnClickListener {
            finish()
        }

        buttonSelectExportPeriod.setOnClickListener {
            showPeriodSelectionDialogExport()
        }

        radioGroupExportOptions.setOnCheckedChangeListener { _, checkedId ->
            tipoDadoExportacao = when (checkedId) {
                R.id.radioOptionFatura -> "Fatura"
                R.id.radioOptionCliente -> "Cliente"
                R.id.radioOptionArtigo -> "Artigo"
                else -> "Fatura"
            }
            Toast.makeText(this, "Opção selecionada: $tipoDadoExportacao", Toast.LENGTH_SHORT).show()
        }

        radioOptionFatura.isChecked = true
        tipoDadoExportacao = "Fatura"

        buttonExportData.setOnClickListener {
            if (radioGroupExportOptions.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Por favor, selecione uma opção de dados para exportar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- LÓGICA DE DEFINIÇÃO DO PERÍODO PADRÃO PARA EXPORTAÇÃO ---
            // Se as datas de início/fim ainda não foram definidas (pelo customizar ou último ano)
            if (dataInicioExportacao == null || dataFimExportacao == null) {
                val calFim = Calendar.getInstance() // Hoje
                val calInicio = Calendar.getInstance()
                calInicio.set(Calendar.YEAR, calFim.get(Calendar.YEAR)) // Ano atual
                calInicio.set(Calendar.MONTH, Calendar.JANUARY) // Janeiro
                calInicio.set(Calendar.DAY_OF_MONTH, 1) // Dia 1
                calInicio.set(Calendar.HOUR_OF_DAY, 0); calInicio.set(Calendar.MINUTE, 0); calInicio.set(Calendar.SECOND, 0)

                dataInicioExportacao = calInicio
                dataFimExportacao = calFim // Até hoje
                periodoExportacaoTipo = "Último Ano (Padrão)" // Atualiza o tipo para refletir o padrão
                Toast.makeText(this, "Período não definido. Usando padrão: Último Ano.", Toast.LENGTH_SHORT).show()
            }
            // --- FIM DA LÓGICA DE DEFINIÇÃO DO PERÍODO PADRÃO ---


            // Chama a função real de geração de PDF do utilitário, AGORA com datas não nulas (se for o caso)
            PdfGenerationUtils.generateResumoPdf(this, dbHelper, tipoDadoExportacao, dataInicioExportacao, dataFimExportacao)
        }
    }

    private fun showPeriodSelectionDialogExport() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_period_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val buttonLastYear = dialogView.findViewById<Button>(R.id.buttonPeriodLastYear)
        val buttonCustom = dialogView.findViewById<Button>(R.id.buttonPeriodCustom)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonPeriodCancel)

        buttonLastYear.setOnClickListener {
            periodoExportacaoTipo = "Último Ano"
            val calFim = Calendar.getInstance()
            val calInicio = Calendar.getInstance()
            calInicio.set(Calendar.YEAR, calFim.get(Calendar.YEAR)) // Define o ano atual
            calInicio.set(Calendar.MONTH, Calendar.JANUARY) // Define o mês de janeiro
            calInicio.set(Calendar.DAY_OF_MONTH, 1) // Define o dia 1
            calInicio.set(Calendar.HOUR_OF_DAY, 0); calInicio.set(Calendar.MINUTE, 0); calInicio.set(Calendar.SECOND, 0)

            dataInicioExportacao = calInicio
            dataFimExportacao = calFim
            Toast.makeText(this, "Período de exportação: Último Ano", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        buttonCustom.setOnClickListener {
            dialog.dismiss()
            showCustomDateRangePickerExport()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showCustomDateRangePickerExport() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_range_picker, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val buttonDataInicioDialog = dialogView.findViewById<Button>(R.id.buttonDataInicioDialog)
        val buttonDataFimDialog = dialogView.findViewById<Button>(R.id.buttonDataFimDialog)
        val buttonAplicarCustomizadoDialog = dialogView.findViewById<Button>(R.id.buttonAplicarCustomizadoDialog)

        dataInicioExportacao?.let { buttonDataInicioDialog.text = dateFormatDisplay.format(it.time) }
        dataFimExportacao?.let { buttonDataFimDialog.text = dateFormatDisplay.format(it.time) }

        buttonDataInicioDialog.setOnClickListener {
            val cal = dataInicioExportacao ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataInicioExportacao = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                buttonDataInicioDialog.text = dateFormatDisplay.format(dataInicioExportacao!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonDataFimDialog.setOnClickListener {
            val cal = dataFimExportacao ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataFimExportacao = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59) }
                buttonDataFimDialog.text = dateFormatDisplay.format(dataFimExportacao!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonAplicarCustomizadoDialog.setOnClickListener {
            if (dataInicioExportacao == null || dataFimExportacao == null) {
                Toast.makeText(this, "Por favor, selecione as datas de início e fim.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dataInicioExportacao!!.after(dataFimExportacao!!)) {
                Toast.makeText(this, "Data de início não pode ser posterior à data fim.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            periodoExportacaoTipo = "Customizado"
            Toast.makeText(this, "Período de exportação: Customizado de ${dateFormatDisplay.format(dataInicioExportacao!!.time)} a ${dateFormatDisplay.format(dataFimExportacao!!.time)}", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun getPeriodoFilterDatesForExport(): Pair<String?, String?> {
        var dataInicio: String? = null
        var dataFim: String? = null

        // Esta função agora apenas reflete o estado das variáveis de exportação
        // A lógica de setar dataInicioExportacao e dataFimExportacao foi movida para o OnClickListener do botão Exportar.
        dataInicio = dataInicioExportacao?.let { dateFormatApi.format(it.time) }
        dataFim = dataFimExportacao?.let { dateFormatApi.format(it.time) }

        return Pair(dataInicio, dataFim)
    }

    override fun onDestroy() {
        dbHelper?.close()
        super.onDestroy()
    }
}