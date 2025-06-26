package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
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
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.example.myapplication.BuildConfig
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.databinding.ActivityResumoFinanceiroBinding
import com.example.myapplication.utils.PdfGenerationUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class ResumoFinanceiroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumoFinanceiroBinding

    // Botões de filtro e período
    private lateinit var buttonSelectDataType: Button
    private lateinit var buttonSelectPeriod: Button

    // Elementos da UI original que foram movidos ou renomeados
    private lateinit var recyclerViewResumos: RecyclerView
    private lateinit var textViewTotalResumo: TextView
    private lateinit var textViewMainListTitle: TextView

    // Gráfico de Linhas
    private lateinit var lineChart: LineChart

    private var dbHelper: ClienteDbHelper? = null
    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateFormatMonthName = SimpleDateFormat("MMMM", Locale("pt", "BR"))
    private val dateFormatYear = SimpleDateFormat("yyyy", Locale.getDefault())

    private var dataInicioFiltroGlobal: Calendar? = null
    private var dataFimFiltroGlobal: Calendar? = null

    // Adapters existentes
    private lateinit var resumoMensalAdapter: ResumoMensalAdapter
    private lateinit var resumoClienteAdapter: ResumoClienteAdapter
    private lateinit var resumoArtigoAdapter: ResumoArtigoAdapter

    // Variáveis de estado para filtros
    private var currentSelectedDataType: String = "Fatura" // Default: Fatura
    private var currentSelectedPeriodType: String = "Todo o Período" // Default: Todo o Período

    // Constantes para os tipos de dados e períodos
    private val OPTION_FATURA = "Fatura"
    private val OPTION_CLIENTE = "Cliente"
    private val OPTION_ARTIGO = "Artigo"

    private val PERIOD_TODO_PERIODO = "Todo o Período"
    private val PERIOD_ULTIMO_ANO = "Último Ano"
    private val PERIOD_CUSTOMIZADO = "Customizado"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResumoFinanceiroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("ResumoFinanceiro", "onCreate chamado com ViewBinding")

        dbHelper = ClienteDbHelper(this)

        buttonSelectDataType = binding.buttonSelectDataType
        buttonSelectPeriod = binding.buttonSelectPeriod
        recyclerViewResumos = binding.recyclerViewResumos
        textViewTotalResumo = binding.textViewTotalResumo
        textViewMainListTitle = binding.textViewMainListTitle

        lineChart = binding.lineChartView as LineChart

        recyclerViewResumos.layoutManager = LinearLayoutManager(this)

        buttonSelectDataType.setOnClickListener {
            showDataTypeSelectionDialog()
        }
        buttonSelectPeriod.setOnClickListener {
            showPeriodSelectionDialog()
        }

        binding.searchIcon.setOnClickListener {
            showSearchDialog()
        }
        binding.moreFunctionsIcon.setOnClickListener {
            val intent = Intent(this, ExportActivity::class.java)
            startActivity(intent)
        }

        setupLineChart()
        updateMonthHeaderSummary()
        carregarDadosResumo()
        updateLineChartData()
    }

    private fun updateMonthHeaderSummary() {
        val calendar = Calendar.getInstance()
        val currentMonthCalendar = calendar.clone() as Calendar

        val currentMonthName = dateFormatMonthName.format(currentMonthCalendar.time)
        val currentYear = dateFormatYear.format(currentMonthCalendar.time)
        val currentValue = getFaturamentoMesAno(currentMonthCalendar.get(Calendar.MONTH) + 1, currentMonthCalendar.get(Calendar.YEAR))
        binding.textViewMonthCurrent.text = "${currentMonthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${currentYear}"
        binding.textViewValueCurrent.text = decimalFormat.format(currentValue)

        val pastMonthCalendar = currentMonthCalendar.clone() as Calendar
        pastMonthCalendar.add(Calendar.MONTH, -1)
        val pastMonthName = dateFormatMonthName.format(pastMonthCalendar.time)
        val pastValue = getFaturamentoMesAno(pastMonthCalendar.get(Calendar.MONTH) + 1, pastMonthCalendar.get(Calendar.YEAR))
        binding.textViewMonthPast.text = "${pastMonthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${dateFormatYear.format(pastMonthCalendar.time)}"
        binding.textViewValuePast.text = decimalFormat.format(pastValue)

        val futureMonthCalendar = currentMonthCalendar.clone() as Calendar
        futureMonthCalendar.add(Calendar.MONTH, 1)
        val futureMonthName = dateFormatMonthName.format(futureMonthCalendar.time)
        val futureValue = 0.00
        binding.textViewMonthFuture.text = "${futureMonthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }} ${dateFormatYear.format(futureMonthCalendar.time)}"
        binding.textViewValueFuture.text = decimalFormat.format(futureValue)

        val percentageChange = if (pastValue != 0.0) {
            ((currentValue - pastValue) / pastValue) * 100
        } else if (currentValue != 0.0) {
            100.0
        }
        else {
            0.0
        }

        binding.textViewPercentageCurrent.text = "${String.format(Locale.getDefault(), "%.0f", percentageChange)}%"
        if (percentageChange > 0) {
            binding.textViewPercentageCurrent.setTextColor(ContextCompat.getColor(this, R.color.positive_growth))
        } else if (percentageChange < 0) {
            binding.textViewPercentageCurrent.setTextColor(ContextCompat.getColor(this, R.color.negative_growth))
        } else {
            binding.textViewPercentageCurrent.setTextColor(ContextCompat.getColor(this, R.color.gray))
        }
    }

    private fun getFaturamentoMesAno(mes: Int, ano: Int): Double {
        val db = dbHelper?.readableDatabase ?: return 0.0
        var total = 0.0

        val query = """
            SELECT SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_mes
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            WHERE strftime('%m', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) = ?
            AND strftime('%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) = ?
        """.trimIndent()

        val mesStr = String.format(Locale.getDefault(), "%02d", mes)
        val cursor: Cursor? = db.rawQuery(query, arrayOf(mesStr, ano.toString()))

        cursor?.use {
            if (it.moveToNext()) {
                total = it.getDouble(it.getColumnIndexOrThrow("total_mes"))
            }
        }
        return total
    }

    private fun carregarDadosResumo(searchQuery: String? = null) {
        Log.d("ResumoFinanceiro", "Carregando dados: Tipo=${currentSelectedDataType}, Período=${currentSelectedPeriodType}, Query=${searchQuery ?: "N/A"}")

        // Obter as strings de data de início e fim.
        val (dataInicioStr, dataFimStr) = getPeriodoFilterDates()

        when (currentSelectedDataType) {
            OPTION_FATURA -> carregarFaturamentoMensal(dataInicioStr, dataFimStr, searchQuery)
            OPTION_CLIENTE -> carregarResumoPorCliente(dataInicioStr, dataFimStr, searchQuery)
            OPTION_ARTIGO -> carregarResumoPorArtigo(dataInicioStr, dataFimStr, searchQuery)
        }
    }

    private fun getPeriodoFilterDates(): Pair<String?, String?> {
        var dataInicio: String? = null
        var dataFim: String? = null

        when (currentSelectedPeriodType) {
            PERIOD_TODO_PERIODO -> {
                dataInicio = null
                dataFim = null
            }
            PERIOD_ULTIMO_ANO -> {
                val calFim = Calendar.getInstance()
                val calInicio = Calendar.getInstance()
                calInicio.set(Calendar.DAY_OF_YEAR, 1)
                calInicio.set(Calendar.HOUR_OF_DAY, 0); calInicio.set(Calendar.MINUTE, 0); calInicio.set(Calendar.SECOND, 0)

                dataInicio = dateFormatApi.format(calInicio.time)
                dataFim = dateFormatApi.format(calFim.time)
            }
            PERIOD_CUSTOMIZADO -> {
                if (dataInicioFiltroGlobal == null || dataFimFiltroGlobal == null) {
                    Toast.makeText(this, "Por favor, selecione as datas de início e fim customizadas.", Toast.LENGTH_SHORT).show()
                    return Pair(null, null)
                }
                if (dataInicioFiltroGlobal!!.after(dataFimFiltroGlobal!!)) {
                    Toast.makeText(this, "Data de início não pode ser posterior à data fim.", Toast.LENGTH_SHORT).show()
                    return Pair(null, null)
                }
                dataInicio = dateFormatApi.format(dataInicioFiltroGlobal!!.time)
                dataFim = dateFormatApi.format(dataFimFiltroGlobal!!.time)
            }
        }
        return Pair(dataInicio, dataFim)
    }

    private fun showDataTypeSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_options, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val buttonFatura = dialogView.findViewById<Button>(R.id.buttonOptionFatura)
        val buttonCliente = dialogView.findViewById<Button>(R.id.buttonOptionCliente)
        val buttonArtigo = dialogView.findViewById<Button>(R.id.buttonOptionArtigo)

        buttonFatura.setOnClickListener {
            currentSelectedDataType = OPTION_FATURA
            textViewMainListTitle.text = "Resumo por Fatura (Meses)"
            carregarDadosResumo()
            dialog.dismiss()
        }
        buttonCliente.setOnClickListener {
            currentSelectedDataType = OPTION_CLIENTE
            textViewMainListTitle.text = "Resumo por Cliente"
            carregarDadosResumo()
            dialog.dismiss()
        }
        buttonArtigo.setOnClickListener {
            currentSelectedDataType = OPTION_ARTIGO
            textViewMainListTitle.text = "Resumo por Artigo"
            carregarDadosResumo()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showPeriodSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_period_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val buttonLastYear = dialogView.findViewById<Button>(R.id.buttonPeriodLastYear)
        val buttonCustom = dialogView.findViewById<Button>(R.id.buttonPeriodCustom)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonPeriodCancel)

        buttonLastYear.setOnClickListener {
            currentSelectedPeriodType = PERIOD_ULTIMO_ANO
            carregarDadosResumo()
            dialog.dismiss()
            Toast.makeText(this, "Período: Último Ano", Toast.LENGTH_SHORT).show()
        }
        buttonCustom.setOnClickListener {
            dialog.dismiss()
            showCustomDateRangePicker()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showCustomDateRangePicker() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_range_picker, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val buttonDataInicioDialog = dialogView.findViewById<Button>(R.id.buttonDataInicioDialog)
        val buttonDataFimDialog = dialogView.findViewById<Button>(R.id.buttonDataFimDialog)
        val buttonAplicarCustomizadoDialog = dialogView.findViewById<Button>(R.id.buttonAplicarCustomizadoDialog)

        dataInicioFiltroGlobal?.let {
            buttonDataInicioDialog.text = dateFormatDisplay.format(it.time)
        }
        dataFimFiltroGlobal?.let {
            buttonDataFimDialog.text = dateFormatDisplay.format(it.time)
        }

        buttonDataInicioDialog.setOnClickListener {
            val cal = dataInicioFiltroGlobal ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataInicioFiltroGlobal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
                buttonDataInicioDialog.text = dateFormatDisplay.format(dataInicioFiltroGlobal!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonDataFimDialog.setOnClickListener {
            val cal = dataFimFiltroGlobal ?: Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                dataFimFiltroGlobal = Calendar.getInstance().apply { set(year, month, dayOfMonth, 23, 59, 59) }
                buttonDataFimDialog.text = dateFormatDisplay.format(dataFimFiltroGlobal!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        buttonAplicarCustomizadoDialog.setOnClickListener {
            currentSelectedPeriodType = PERIOD_CUSTOMIZADO
            carregarDadosResumo()
            dialog.dismiss()
            Toast.makeText(this, "Período: Customizado", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    // --- GRÁFICO DE LINHAS: Configuração e Atualização ---
    private fun setupLineChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setDrawGridBackground(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.textColor = Color.BLACK
        xAxis.valueFormatter = IndexAxisValueFormatter(getMonthsLabelsForChart())
        xAxis.granularity = 1f

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = Color.BLACK
        leftAxis.axisMinimum = 0f

        lineChart.axisRight.isEnabled = false

        lineChart.animateX(1500)
        lineChart.legend.isEnabled = false
    }

    private fun getMonthsLabelsForChart(): List<String> {
        val labels = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        for (i in 0 until 12) {
            labels.add(dateFormatMonthName.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            calendar.add(Calendar.MONTH, 1)
        }
        return labels
    }

    private fun updateLineChartData() {
        val db = dbHelper?.readableDatabase ?: return
        val entries = ArrayList<Entry>()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-11

        for (i in 0 until 12) {
            val targetMonth = (currentMonth - (11 - i) + 12) % 12
            val targetYear = if (currentMonth - (11 - i) < 0) currentYear - 1 else currentYear

            val value = getFaturamentoMesAno(targetMonth + 1, targetYear)
            entries.add(Entry(i.toFloat(), value.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Faturamento Mensal").apply {
            color = ContextCompat.getColor(this@ResumoFinanceiroActivity, R.color.info_blue)
            valueTextColor = Color.BLACK
            valueTextSize = 9f
            setDrawCircles(true)
            setCircleColor(Color.WHITE)
            circleRadius = 4f
            setDrawValues(true)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@ResumoFinanceiroActivity, R.color.info_blue)
            fillAlpha = 50
        }

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(dataSet)

        val lineData = LineData(dataSets)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    // --- LÓGICA DE PESQUISA ---
    private fun showSearchDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pesquisar Resumo")

        val input = EditText(this)
        input.hint = "Digite o termo de busca"
        builder.setView(input)

        builder.setPositiveButton("Pesquisar") { dialog, _ ->
            val query = input.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Termo de busca vazio. Exibindo todos os dados.", Toast.LENGTH_SHORT).show()
                carregarDadosResumo()
            } else {
                carregarDadosResumo(searchQuery = query)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun carregarFaturamentoMensal(dataInicio: String?, dataFim: String?, searchQuery: String? = null) {
        val db = dbHelper?.readableDatabase ?: return
        val resumos = mutableListOf<ResumoMensalItem>()
        var totalGeral = 0.0

        val (datePredicate, dateArgs) = PdfGenerationUtils.getDateRangePredicate(dataInicio, dataFim)
        val currentArgs = dateArgs?.toMutableList() ?: mutableListOf()
        var currentPredicate = datePredicate

        if (!searchQuery.isNullOrBlank()) {
            val searchClause = "(mes_ano_str LIKE ? OR total_mes LIKE ?)"
            if (currentPredicate != null) {
                currentPredicate += " AND $searchClause"
            } else {
                currentPredicate = searchClause
            }
            currentArgs.add("%$searchQuery%")
            currentArgs.add("%$searchQuery%")
        }

        val query = """
            SELECT
                strftime('%m/%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes_ano_str,
                strftime('%Y', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as ano,
                strftime('%m', ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA}) as mes,
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_mes
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (currentPredicate != null) "WHERE $currentPredicate" else ""}
            GROUP BY mes_ano_str, ano, mes
            ORDER BY ano DESC, mes DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, currentArgs.toTypedArray())

        cursor?.use {
            while (it.moveToNext()) {
                val mesAnoStr = it.getString(it.getColumnIndexOrThrow("mes_ano_str"))
                val valor = it.getDouble(it.getColumnIndexOrThrow("total_mes"))
                val ano = it.getInt(it.getColumnIndexOrThrow("ano"))
                val mes = it.getInt(it.getColumnIndexOrThrow("mes"))
                resumos.add(ResumoMensalItem(mesAnoStr, valor, ano, mes))
                totalGeral += valor
            }
        }
        resumoMensalAdapter = ResumoMensalAdapter(resumos) { itemClicado ->
            val intent = Intent(this, DetalhesFaturasMesActivity::class.java)
            intent.putExtra("ANO", itemClicado.ano)
            intent.putExtra("MES", itemClicado.mes)
            intent.putExtra("MES_ANO_STR", itemClicado.mesAno)
            startActivity(intent)
        }
        recyclerViewResumos.adapter = resumoMensalAdapter
        textViewTotalResumo.text = "Total Faturado: ${decimalFormat.format(totalGeral)}"
        textViewMainListTitle.text = "Resumo por Fatura (Meses)"
    }

    private fun carregarResumoPorCliente(dataInicio: String?, dataFim: String?, searchQuery: String? = null) {
        val db = dbHelper?.readableDatabase ?: return
        val resumos = mutableListOf<ResumoClienteItem>()
        var totalGeral = 0.0

        val (datePredicate, dateArgs) = PdfGenerationUtils.getDateRangePredicate(dataInicio, dataFim)
        val currentArgs = dateArgs?.toMutableList() ?: mutableListOf()
        var currentPredicate = datePredicate

        if (!searchQuery.isNullOrBlank()) {
            val searchClause = " (${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE} LIKE ? OR CAST(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR} AS TEXT) LIKE ?) "
            if (currentPredicate != null) {
                currentPredicate += " AND $searchClause"
            } else {
                currentPredicate = searchClause
            }
            currentArgs.add("%$searchQuery%")
            currentArgs.add("%$searchQuery%")
        }

        val query = """
            SELECT
                ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE},
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_gasto_cliente
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (currentPredicate != null) "WHERE $currentPredicate" else ""}
            GROUP BY ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}
            ORDER BY total_gasto_cliente DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, currentArgs.toTypedArray())
        cursor?.use {
            val nomeClienteIndex = it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)
            val totalGastoIndex = it.getColumnIndexOrThrow("total_gasto_cliente")

            while (it.moveToNext()) {
                val nomeCliente = it.getString(nomeClienteIndex) ?: "Cliente Desconhecido"
                val totalGasto = it.getDouble(totalGastoIndex)
                resumos.add(ResumoClienteItem(nomeCliente, totalGasto, null))
                totalGeral += totalGasto
            }
        }
        resumoClienteAdapter = ResumoClienteAdapter(resumos)
        recyclerViewResumos.adapter = resumoClienteAdapter
        textViewTotalResumo.text = "Total Geral Clientes: ${decimalFormat.format(totalGeral)}"
        textViewMainListTitle.text = "Resumo por Cliente"
    }

    private fun carregarResumoPorArtigo(dataInicio: String?, dataFim: String?, searchQuery: String? = null) {
        val db = dbHelper?.readableDatabase ?: return
        val artigosMap = mutableMapOf<String, ResumoArtigoItem>()
        var totalGeralVendido = 0.0

        val (datePredicate, dateArgs) = PdfGenerationUtils.getDateRangePredicate(dataInicio, dataFim)
        val currentArgs = dateArgs?.toMutableList() ?: mutableListOf()
        var currentPredicate = datePredicate

        if (!searchQuery.isNullOrBlank()) {
            val searchClause = " (${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS} LIKE ?) "
            if (currentPredicate != null) {
                currentPredicate += " AND $searchClause"
            } else {
                currentPredicate = searchClause
            }
            currentArgs.add("%$searchQuery%")
        }

        val query = "SELECT ${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS} FROM ${FaturaContract.FaturaEntry.TABLE_NAME} ${if (currentPredicate != null) "WHERE $currentPredicate" else ""}"
        val cursor: Cursor? = db.rawQuery(query, currentArgs.toTypedArray())

        cursor?.use {
            val artigosStringIndex = it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)
            while (it.moveToNext()) {
                val artigosString = it.getString(artigosStringIndex)
                if (!artigosString.isNullOrEmpty()) {
                    artigosString.split("|").forEach { artigoData ->
                        val parts = artigoData.split(",")
                        if (parts.size >= 4) {
                            val nomeArtigo = parts[1]
                            val quantidade = parts[2].toIntOrNull() ?: 0
                            val precoTotalItem = parts[3].toDoubleOrNull() ?: 0.0

                            if (nomeArtigo.isNotEmpty() && quantidade > 0) {
                                val resumoExistente = artigosMap[nomeArtigo]
                                if (resumoExistente != null) {
                                    artigosMap[nomeArtigo] = resumoExistente.copy(
                                        quantidadeVendida = resumoExistente.quantidadeVendida + quantidade,
                                        valorTotalVendido = resumoExistente.valorTotalVendido + precoTotalItem
                                    )
                                } else {
                                    artigosMap[nomeArtigo] = ResumoArtigoItem(nomeArtigo, quantidade, precoTotalItem, null)
                                }
                                totalGeralVendido += precoTotalItem
                            }
                        }
                    }
                }
            }
        }
        val resumos = artigosMap.values.sortedByDescending { it.valorTotalVendido }
        resumoArtigoAdapter = ResumoArtigoAdapter(resumos)
        recyclerViewResumos.adapter = resumoArtigoAdapter
        textViewTotalResumo.text = "Valor Total de Artigos Vendidos: ${decimalFormat.format(totalGeralVendido)}"
        textViewMainListTitle.text = "Resumo por Artigo"
    }

    private fun generateResumoPdf(dataType: String, startDate: Calendar?, endDate: Calendar?): File? {
        return PdfGenerationUtils.generateResumoPdf(this, dbHelper, dataType, startDate, endDate)
    }
}