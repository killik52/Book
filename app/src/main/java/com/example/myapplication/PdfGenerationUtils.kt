package com.example.myapplication.utils

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
import android.os.Environment
import android.provider.BaseColumns
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
import com.example.myapplication.ClienteDbHelper
import com.example.myapplication.FaturaContract
import com.example.myapplication.FaturaResumidaItem
import com.example.myapplication.ResumoArtigoItem
import com.example.myapplication.ResumoClienteItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.Closeable
import kotlin.math.max

object PdfGenerationUtils {

    private val decimalFormat = DecimalFormat("R$ #,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))
    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun getDateRangePredicate(inicio: String?, fim: String?): Pair<String?, Array<String>?> {
        val selectionArgs = mutableListOf<String>()
        val selectionClause = StringBuilder()

        if (inicio != null && fim != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} BETWEEN ? AND ?")
            selectionArgs.add(inicio)
            selectionArgs.add(fim)
        } else if (inicio != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} >= ?")
            selectionArgs.add(inicio)
        } else if (fim != null) {
            selectionClause.append("${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} <= ?")
            selectionArgs.add(fim)
        }
        return Pair(if (selectionClause.isNotEmpty()) selectionClause.toString() else null, if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null)
    }

    fun generateResumoPdf(
        context: Context,
        dbHelper: ClienteDbHelper?,
        dataType: String,
        startDate: Calendar?,
        endDate: Calendar?
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var pageNumber = 1

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 30f
        val bottomMargin = 40f
        val contentWidth = pageWidth - 2 * margin
        var yPos: Float = margin

        val titlePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 20f
            color = Color.BLUE
            isAntiAlias = true
        }
        val companyNamePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
            isAntiAlias = true
        }
        val datePdfPaint = TextPaint().apply {
            textSize = 10f
            color = Color.DKGRAY
            isAntiAlias = true
        }
        val headerTablePaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 12f
            color = Color.DKGRAY
            isAntiAlias = true
        }
        val dataTextPaint = TextPaint().apply {
            textSize = 10f
            color = Color.BLACK
            isAntiAlias = true
        }
        val totalPdfPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
            isAntiAlias = true
        }
        val dividerLinePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 0.5f
        }
        val pageNumPaint = Paint().apply {
            textSize = 8f
            color = Color.DKGRAY
        }
        val grayBoxPaint = Paint().apply {
            color = Color.parseColor("#F9F9F9")
            style = Paint.Style.FILL
        }

        fun startNewPage(): Float {
            val pageNumText = "Página $pageNumber"
            val textWidth = pageNumPaint.measureText(pageNumText)
            canvas.drawText(pageNumText, pageWidth - margin - textWidth, pageHeight - margin + 10, pageNumPaint)

            pdfDocument.finishPage(currentPage)
            pageNumber++
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            return margin
        }

        val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        val nomeEmpresa = context.getSharedPreferences("InformacoesEmpresaPrefs", Context.MODE_PRIVATE)
            .getString("nome_empresa", "Nome da Empresa") ?: "Nome da Empresa"

        // --- CABEÇALHO DO PDF (Nome da Empresa e Data à Esquerda, Logo à Direita) ---
        var currentYForHeaderLeftBlock = yPos // Inicializa o Y para o bloco de texto esquerdo

        // Nome da Empresa (Esquerda)
        canvas.drawText(nomeEmpresa, margin, currentYForHeaderLeftBlock + companyNamePaint.textSize, companyNamePaint)
        currentYForHeaderLeftBlock += companyNamePaint.descent() - companyNamePaint.ascent() + 5f

        // Data (Esquerda, abaixo do nome da empresa)
        canvas.drawText(currentDateTime, margin, currentYForHeaderLeftBlock + datePdfPaint.textSize, datePdfPaint)
        currentYForHeaderLeftBlock += datePdfPaint.descent() - datePdfPaint.ascent() + 10f // Espaço após a data

        // Logo (Direita, top-aligned)
        val logoPrefs = context.getSharedPreferences("LogotipoPrefs", Context.MODE_PRIVATE)
        val logoUriString = logoPrefs.getString("logo_uri", null)
        var logoBottomY = yPos // Posição Y final da logo, inicializada para o caso de não ter logo

        if (logoUriString != null) {
            try {
                val logoBitmap = BitmapFactory.decodeFile(Uri.parse(logoUriString).path)
                if (logoBitmap != null) {
                    val logoDisplaySize = 120f
                    val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                    var targetWidth = logoDisplaySize
                    var targetHeight = logoDisplaySize / aspectRatio

                    if (targetHeight > logoDisplaySize) {
                        targetHeight = logoDisplaySize
                        targetWidth = targetHeight * aspectRatio
                    }

                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, targetWidth.toInt(), targetHeight.toInt(), true)
                    val roundedBitmap = Bitmap.createBitmap(scaledLogo.width, scaledLogo.height, Bitmap.Config.ARGB_8888)
                    val tempCanvas = Canvas(roundedBitmap)
                    val tempPaint = Paint().apply { isAntiAlias = true }
                    val rect = RectF(0f, 0f, scaledLogo.width.toFloat(), scaledLogo.height.toFloat())
                    tempCanvas.drawRoundRect(rect, 15f, 15f, tempPaint)
                    tempPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                    tempCanvas.drawBitmap(scaledLogo, 0f, 0f, tempPaint)

                    val logoX = pageWidth - margin - scaledLogo.width
                    val logoY = margin
                    canvas.drawBitmap(roundedBitmap, logoX, logoY, null)
                    logoBottomY = logoY + scaledLogo.height
                    logoBitmap.recycle()
                    scaledLogo.recycle()
                    roundedBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e("PdfGenerationUtils", "Erro ao carregar/processar logo para PDF: ${e.message}", e)
            }
        }

        yPos = max(currentYForHeaderLeftBlock, logoBottomY) + 30f // Usa currentYForHeaderLeftBlock

        val reportTitle = "Relatório de ${
            when(dataType) {
                "Fatura" -> "Faturas"
                "Cliente" -> "Clientes"
                "Artigo" -> "Artigos"
                else -> "Dados Financeiros"
            }
        }"
        val reportTitleWidth = titlePaint.measureText(reportTitle)
        val reportTitleX = (pageWidth - reportTitleWidth) / 2
        canvas.drawText(reportTitle, reportTitleX, yPos, titlePaint)
        yPos += titlePaint.descent() - titlePaint.ascent() + 20f

        val periodoText = when {
            startDate != null && endDate != null -> "Período: ${dateFormatDisplay.format(startDate.time)} a ${dateFormatDisplay.format(endDate.time)}"
            startDate != null -> "Período: A partir de ${dateFormatDisplay.format(startDate.time)}"
            endDate != null -> "Período: Até ${dateFormatDisplay.format(endDate.time)}"
            else -> "Período: Todo o Período"
        }
        val periodoTextWidth = datePdfPaint.measureText(periodoText)
        val periodoTextX = (pageWidth - periodoTextWidth) / 2
        canvas.drawText(periodoText, periodoTextX, yPos + datePdfPaint.textSize, datePdfPaint)
        yPos += datePdfPaint.descent() - datePdfPaint.ascent() + 30f

        val col1Width = contentWidth * 0.4f
        val col2Width = contentWidth * 0.3f
        val col3Width = contentWidth * 0.3f

        val rowHeight = 25f
        var currentY = yPos

        fun drawTableHeader(vararg headers: String) {
            var currentHeaderX = margin
            for (i in headers.indices) {
                val header = headers[i]
                val paintToUse = headerTablePaint
                val cellWidth = when(i) {
                    0 -> col1Width
                    1 -> col2Width
                    2 -> col3Width
                    else -> col1Width
                }
                val textMeasurement = paintToUse.measureText(header)
                val textX = currentHeaderX + (cellWidth - textMeasurement) / 2

                canvas.drawText(header, textX, currentY + paintToUse.textSize, paintToUse)
                currentHeaderX += cellWidth
            }
            currentY += headerTablePaint.descent() - headerTablePaint.ascent() + 10f
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerLinePaint)
            currentY += 5f
        }

        fun drawTableRow(value1: String, value2: String, value3: String? = null, isGrayRow: Boolean) {
            if (currentY + rowHeight + bottomMargin > pageHeight) {
                currentY = startNewPage()
            }
            if (isGrayRow) {
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, grayBoxPaint)
            }
            var currentCellX = margin
            val values = arrayOf(value1, value2, value3 ?: "")
            for (i in values.indices) {
                val value = values[i]
                val paintToUse = dataTextPaint
                val cellWidth = when(i) {
                    0 -> col1Width
                    1 -> col2Width
                    2 -> col3Width
                    else -> col1Width
                }
                val textMeasurement = paintToUse.measureText(value)
                val textX = currentCellX + (cellWidth - textMeasurement) / 2

                canvas.drawText(value, textX, currentY + (rowHeight / 2) - ((paintToUse.descent() + paintToUse.ascent()) / 2) + paintToUse.textSize / 2, paintToUse) // Corrigido 'paintToouse' para 'paintToUse'
                currentCellX += cellWidth
            }
            currentY += rowHeight
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerLinePaint)
            currentY += 5f
        }

        var isGrayRow = false

        when (dataType) {
            "Fatura" -> {
                val faturas = getFaturasForPdf(dbHelper, dateFormatApi.format(startDate?.time), dateFormatApi.format(endDate?.time))
                drawTableHeader("Nº Fatura", "Cliente", "Valor")
                var totalFaturadoGeral = 0.0
                faturas.forEach { fatura ->
                    val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateFormatApi.parse(fatura.data) ?: Date())
                    drawTableRow(fatura.numeroFatura, fatura.cliente, decimalFormat.format(fatura.saldoDevedor), isGrayRow)
                    totalFaturadoGeral += fatura.saldoDevedor
                    isGrayRow = !isGrayRow
                }
                currentY += 10f
                canvas.drawText("Total Faturado no Período: ${decimalFormat.format(totalFaturadoGeral)}", margin, currentY + totalPdfPaint.textSize, totalPdfPaint)
            }
            "Cliente" -> {
                val clientes = getClientesForPdf(dbHelper, dateFormatApi.format(startDate?.time), dateFormatApi.format(endDate?.time))
                drawTableHeader("Cliente", "Total Gasto", "Status")
                var totalGastoGeral = 0.0
                clientes.forEach { cliente ->
                    val statusText = if (cliente.isBlocked == true) "Bloqueado" else "Ativo"
                    drawTableRow(cliente.nomeCliente, decimalFormat.format(cliente.totalCompras), statusText, isGrayRow)
                    totalGastoGeral += cliente.totalCompras
                    isGrayRow = !isGrayRow
                }
                currentY += 10f
                canvas.drawText("Total Gasto pelos Clientes: ${decimalFormat.format(totalGastoGeral)}", margin, currentY + totalPdfPaint.textSize, totalPdfPaint)
            }
            "Artigo" -> {
                val artigos = getArtigosForPdf(dbHelper, dateFormatApi.format(startDate?.time), dateFormatApi.format(endDate?.time))
                drawTableHeader("Artigo", "Qtd. Vendida", "Valor Total")
                var totalVendidoArtigosGeral = 0.0
                artigos.forEach { artigo ->
                    drawTableRow(artigo.nomeArtigo, artigo.quantidadeVendida.toString(), decimalFormat.format(artigo.valorTotalVendido), isGrayRow)
                    totalVendidoArtigosGeral += artigo.valorTotalVendido
                    isGrayRow = !isGrayRow
                }
                currentY += 10f
                canvas.drawText("Valor Total Vendido de Artigos: ${decimalFormat.format(totalVendidoArtigosGeral)}", margin, currentY + totalPdfPaint.textSize, totalPdfPaint)
            }
        }

        val pageNumText = "Página $pageNumber"
        val textWidth = pageNumPaint.measureText(pageNumText)
        canvas.drawText(pageNumText, pageWidth - margin - textWidth, pageHeight - margin + 10, pageNumPaint)
        pdfDocument.finishPage(currentPage)

        val fileName = "${dataType}_Relatorio_${System.currentTimeMillis()}.pdf"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (storageDir == null) {
            Log.e("PdfGenerationUtils", "Diretório de armazenamento externo não disponível.")
            Toast.makeText(context, "Erro: Armazenamento externo não disponível para salvar PDF.", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("PdfGenerationUtils", "Não foi possível criar o diretório de documentos.")
            Toast.makeText(context, "Erro ao criar diretório para salvar PDF.", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }

        val file = File(storageDir, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Log.d("PdfGenerationUtils", "PDF gerado com sucesso: ${file.absolutePath}")
            Toast.makeText(context, "PDF de $dataType gerado com sucesso!", Toast.LENGTH_LONG).show()
            viewPdf(context, file)
            return file
        } catch (e: IOException) {
            Log.e("PdfGenerationUtils", "Erro de I/O ao salvar PDF: ${e.message}", e)
            Toast.makeText(context, "Erro de I/O ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } catch (e: Exception) {
            Log.e("PdfGenerationUtils", "Erro geral ao salvar PDF: ${e.message}", e)
            Toast.makeText(context, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun viewPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Nenhum visualizador de PDF encontrado.", Toast.LENGTH_LONG).show()
            Log.e("PdfGenerationUtils", "Erro ao abrir PDF: ${e.message}", e)
        }
    }

    fun getFaturasForPdf(dbHelper: ClienteDbHelper?, dataInicio: String?, dataFim: String?): List<FaturaResumidaItem> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val faturas = mutableListOf<FaturaResumidaItem>()

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = "SELECT * FROM ${FaturaContract.FaturaEntry.TABLE_NAME} ${if (datePredicate != null) "WHERE $datePredicate" else ""} ORDER BY ${FaturaContract.FaturaEntry.COLUMN_NAME_DATA} DESC"
        val cursor: Cursor? = db.rawQuery(query, dateArgs)

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(BaseColumns._ID))
                val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)) ?: "N/A"
                val cliente = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)) ?: "N/A"
                val saldoDevedor = it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR))
                val dataFatura = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)) ?: ""
                val foiEnviada = it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA)) == 1

                faturas.add(FaturaResumidaItem(id, numeroFatura, cliente, emptyList(), saldoDevedor, dataFatura, foiEnviada))
            }
        }
        return faturas
    }

    fun getClientesForPdf(dbHelper: ClienteDbHelper?, dataInicio: String?, dataFim: String?): List<ResumoClienteItem> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val clientes = mutableListOf<ResumoClienteItem>()

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = """
            SELECT
                ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE},
                SUM(${FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR}) as total_gasto_cliente
            FROM ${FaturaContract.FaturaEntry.TABLE_NAME}
            ${if (datePredicate != null) "WHERE $datePredicate" else ""}
            GROUP BY ${FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE}
            ORDER BY total_gasto_cliente DESC
        """.trimIndent()

        val cursor: Cursor? = db.rawQuery(query, dateArgs)
        cursor?.use {
            while (it.moveToNext()) {
                val nomeCliente = it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)) ?: "Desconhecido"
                val totalGasto = it.getDouble(it.getColumnIndexOrThrow("total_gasto_cliente"))
                clientes.add(ResumoClienteItem(nomeCliente, totalGasto, null))
            }
        }
        return clientes
    }

    fun getArtigosForPdf(dbHelper: ClienteDbHelper?, dataInicio: String?, dataFim: String?): List<ResumoArtigoItem> {
        val db = dbHelper?.readableDatabase ?: return emptyList()
        val artigosMap = mutableMapOf<String, ResumoArtigoItem>()

        val (datePredicate, dateArgs) = getDateRangePredicate(dataInicio, dataFim)

        val query = "SELECT ${FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS} FROM ${FaturaContract.FaturaEntry.TABLE_NAME} ${if (datePredicate != null) "WHERE $datePredicate" else ""}"
        val cursor: Cursor? = db.rawQuery(query, dateArgs)

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
                            }
                        }
                    }
                }
            }
        }
        return artigosMap.values.sortedByDescending { it.valorTotalVendido }
    }
}