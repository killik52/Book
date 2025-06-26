package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LixeiraActivity : AppCompatActivity() {

    private var dbHelper: ClienteDbHelper? = null
    private lateinit var faturasRecyclerView: RecyclerView
    private lateinit var faturaLixeiraAdapter: FaturaLixeiraAdapter
    private val RESTORE_FATURA_REQUEST_CODE = 790

    /**
     * [1] Inicializa a atividade, configurando o layout, banco de dados, RecyclerView e adaptador.
     * Carrega as faturas da lixeira e exibe na interface.
     * @param savedInstanceState Estado salvo da atividade, usado para restaurar dados após mudanças de configuração (não utilizado aqui).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lixeira)

        dbHelper = ClienteDbHelper(this)

        faturasRecyclerView = findViewById(R.id.faturasLixeiraRecyclerView)
        faturasRecyclerView.layoutManager = LinearLayoutManager(this)
        faturaLixeiraAdapter = FaturaLixeiraAdapter(
            emptyList(),
            onRestoreClick = { faturaId -> restaurarFatura(faturaId) },
            onLongClick = { faturaId -> excluirFaturaPermanente(faturaId) }
        )
        faturasRecyclerView.adapter = faturaLixeiraAdapter

        carregarFaturasLixeira()
    }

    /**
     * [2] Carrega as faturas da tabela de lixeira do banco de dados e atualiza o RecyclerView.
     * Exibe uma mensagem se a lixeira estiver vazia ou em caso de erro no acesso ao banco.
     */
    private fun carregarFaturasLixeira() {
        val db = dbHelper?.readableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        // Verifica se a tabela fatura_lixeira existe
        if (!tabelaExiste(db, FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME)) {
            Log.w("LixeiraActivity", "Tabela ${FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME} não existe, criando...")
            try {
                db.execSQL(FaturaLixeiraContract.FaturaLixeiraEntry.SQL_CREATE_ENTRIES)
                Log.d("LixeiraActivity", "Tabela ${FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME} criada com sucesso")
            } catch (e: Exception) {
                Log.e("LixeiraActivity", "Erro ao criar tabela ${FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME}: ${e.message}")
                Toast.makeText(this, "Erro ao inicializar lixeira", Toast.LENGTH_LONG).show()
                return
            }
        }

        val faturas = mutableListOf<FaturaLixeiraItem>()
        val cursor = db.query(
            FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA} DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(android.provider.BaseColumns._ID))
                val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA))
                val cliente = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE))
                val data = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA))
                faturas.add(FaturaLixeiraItem(id, numeroFatura, cliente, data))
            }
        }
        cursor?.close()

        faturaLixeiraAdapter.updateFaturas(faturas)
        Log.d("LixeiraActivity", "Faturas carregadas da lixeira: ${faturas.size} itens")
        if (faturas.isEmpty()) {
            Toast.makeText(this, "Nenhuma fatura na lixeira", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Verifica se uma tabela existe no banco de dados
     */
    private fun tabelaExiste(db: android.database.sqlite.SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    /**
     * [3] Move uma fatura da tabela principal para a tabela de lixeira.
     * @param faturaId O ID da fatura a ser movida para a lixeira.
     */
    fun excluirFatura(faturaId: Long) {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Inicia transação para garantir consistência
            db.beginTransaction()
            
            val cursor = db.query(
                FaturaContract.FaturaEntry.TABLE_NAME,
                null,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString()),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val values = ContentValues().apply {
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_ARTIGOS,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SUBTOTAL,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO_PERCENT,
                            it.getInt(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SALDO_DEVEDOR,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_DATA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA)))
                        put(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS,
                            it.getString(it.getColumnIndexOrThrow(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS)))
                    }

                    val newRowId = db.insert(FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        // Mover fotos para a lixeira
                        moverFotosParaLixeira(faturaId, newRowId, db)
                        
                        // Mover notas para a lixeira
                        moverNotasParaLixeira(faturaId, newRowId, db)
                        
                        // Mover itens para a lixeira
                        moverItensParaLixeira(faturaId, newRowId, db)
                        
                        val rowsDeleted = db.delete(
                            FaturaContract.FaturaEntry.TABLE_NAME,
                            "${android.provider.BaseColumns._ID} = ?",
                            arrayOf(faturaId.toString())
                        )
                        if (rowsDeleted > 0) {
                            // Confirma a transação
                            db.setTransactionSuccessful()
                            Log.d("LixeiraActivity", "Fatura ID=$faturaId movida para a lixeira com sucesso")
                            Toast.makeText(this, "Fatura movida para a lixeira", Toast.LENGTH_SHORT).show()
                            carregarFaturasLixeira()
                        } else {
                            Log.e("LixeiraActivity", "Erro ao remover fatura ID=$faturaId da tabela faturas")
                            Toast.makeText(this, "Erro ao mover fatura para a lixeira", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("LixeiraActivity", "Erro ao inserir fatura ID=$faturaId na tabela faturas_lixeira")
                        Toast.makeText(this, "Erro ao mover fatura para a lixeira", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na tabela faturas")
                    Toast.makeText(this, "Fatura não encontrada", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Erro ao excluir fatura: ${e.message}")
            Toast.makeText(this, "Erro ao mover fatura para a lixeira: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // Finaliza a transação
            if (db.inTransaction()) {
                db.endTransaction()
            }
        }
    }

    /**
     * Move as fotos de uma fatura para a lixeira
     */
    private fun moverFotosParaLixeira(faturaId: Long, faturaLixeiraId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            val cursor = db.query(
                "faturas_fotos",
                null,
                "fatura_id = ?",
                arrayOf(faturaId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val fotoPath = it.getString(it.getColumnIndexOrThrow("foto_path"))
                    val values = ContentValues().apply {
                        put("fatura_id", faturaLixeiraId)
                        put("foto_path", fotoPath)
                    }
                    db.insert("faturas_fotos_lixeira", null, values)
                }
            }
            
            // Remove as fotos da tabela original
            db.delete("faturas_fotos", "fatura_id = ?", arrayOf(faturaId.toString()))
            Log.d("LixeiraActivity", "Fotos movidas para lixeira para fatura ID=$faturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao mover fotos para lixeira: ${e.message}")
        }
    }

    /**
     * Move as notas de uma fatura para a lixeira
     */
    private fun moverNotasParaLixeira(faturaId: Long, faturaLixeiraId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            val cursor = db.query(
                "faturas_notas",
                null,
                "fatura_id = ?",
                arrayOf(faturaId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val nota = it.getString(it.getColumnIndexOrThrow("nota"))
                    val values = ContentValues().apply {
                        put("fatura_id", faturaLixeiraId)
                        put("nota", nota)
                    }
                    db.insert("faturas_notas_lixeira", null, values)
                }
            }
            
            // Remove as notas da tabela original
            db.delete("faturas_notas", "fatura_id = ?", arrayOf(faturaId.toString()))
            Log.d("LixeiraActivity", "Notas movidas para lixeira para fatura ID=$faturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao mover notas para lixeira: ${e.message}")
        }
    }

    /**
     * Move os itens de uma fatura para a lixeira
     */
    private fun moverItensParaLixeira(faturaId: Long, faturaLixeiraId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            val cursor = db.query(
                "faturas_itens",
                null,
                "fatura_id = ?",
                arrayOf(faturaId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val nome = it.getString(it.getColumnIndexOrThrow("nome"))
                    val quantidade = it.getInt(it.getColumnIndexOrThrow("quantidade"))
                    val preco = it.getDouble(it.getColumnIndexOrThrow("preco"))
                    val numeroSerial = it.getString(it.getColumnIndexOrThrow("numero_serial"))
                    val descricao = it.getString(it.getColumnIndexOrThrow("descricao"))
                    
                    val values = ContentValues().apply {
                        put("fatura_id", faturaLixeiraId)
                        put("nome", nome)
                        put("quantidade", quantidade)
                        put("preco", preco)
                        put("numero_serial", numeroSerial)
                        put("descricao", descricao)
                    }
                    db.insert("faturas_itens_lixeira", null, values)
                }
            }
            
            // Remove os itens da tabela original
            db.delete("faturas_itens", "fatura_id = ?", arrayOf(faturaId.toString()))
            Log.d("LixeiraActivity", "Itens movidos para lixeira para fatura ID=$faturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao mover itens para lixeira: ${e.message}")
        }
    }

    /**
     * [4] Restaura uma fatura da lixeira, movendo-a de volta para a tabela principal de faturas.
     * Inclui logs detalhados para depurar falhas na restauração.
     * @param faturaId O ID da fatura a ser restaurada.
     */
    private fun restaurarFatura(faturaId: Long) {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados: dbHelper.writableDatabase é nulo")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Inicia transação para garantir consistência
            db.beginTransaction()
            
            // Consulta a fatura na tabela de lixeira
            Log.d("LixeiraActivity", "Consultando fatura ID=$faturaId na tabela faturas_lixeira")
            val cursor = db.query(
                FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                null,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString()),
                null,
                null,
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    // Log dos dados da fatura para depuração
                    val numeroFatura = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NUMERO_FATURA))
                    val cliente = it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_CLIENTE))
                    Log.d("LixeiraActivity", "Fatura encontrada: ID=$faturaId, numero_fatura=$numeroFatura, cliente=$cliente")

                    // Prepara os dados da fatura para inserção na tabela principal
                    val values = ContentValues().apply {
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_NUMERO_FATURA, numeroFatura)
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_CLIENTE, cliente)
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_ARTIGOS,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_ARTIGOS)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_SUBTOTAL,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SUBTOTAL)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DESCONTO_PERCENT,
                            it.getInt(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DESCONTO_PERCENT)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_TAXA_ENTREGA,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_TAXA_ENTREGA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_SALDO_DEVEDOR,
                            it.getDouble(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_SALDO_DEVEDOR)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_DATA,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_DATA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_FOTO_IMPRESSORA,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_FOTO_IMPRESSORA)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_NOTAS,
                            it.getString(it.getColumnIndexOrThrow(FaturaLixeiraContract.FaturaLixeiraEntry.COLUMN_NAME_NOTAS)))
                        put(FaturaContract.FaturaEntry.COLUMN_NAME_FOI_ENVIADA, 0) // Reset para não enviada
                    }

                    // Log dos valores a serem inseridos
                    Log.d("LixeiraActivity", "Valores preparados para inserção: $values")

                    // Insere a fatura na tabela principal
                    val newRowId = db.insert(FaturaContract.FaturaEntry.TABLE_NAME, null, values)
                    if (newRowId != -1L) {
                        Log.d("LixeiraActivity", "Fatura inserida na tabela faturas com novo ID=$newRowId")
                        
                        // Restaurar fotos da fatura
                        restaurarFotosFatura(faturaId, newRowId, db)
                        
                        // Restaurar notas da fatura
                        restaurarNotasFatura(faturaId, newRowId, db)
                        
                        // Restaurar itens da fatura
                        restaurarItensFatura(faturaId, newRowId, db)
                        
                        // Remove a fatura da lixeira
                        val rowsDeleted = db.delete(
                            FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                            "${android.provider.BaseColumns._ID} = ?",
                            arrayOf(faturaId.toString())
                        )
                        if (rowsDeleted > 0) {
                            // Confirma a transação
                            db.setTransactionSuccessful()
                            Log.d("LixeiraActivity", "Fatura ID=$faturaId removida da lixeira com sucesso")
                            Toast.makeText(this, "Fatura restaurada com sucesso", Toast.LENGTH_SHORT).show()
                            carregarFaturasLixeira()
                            // Configura o resultado para a atividade chamadora
                            val resultIntent = Intent().apply {
                                putExtra("fatura_restaurada", true)
                                putExtra("fatura_id", newRowId)
                            }
                            Log.d("LixeiraActivity", "Resultado configurado: fatura_restaurada=true, fatura_id=$newRowId")
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            Log.e("LixeiraActivity", "Erro ao remover fatura ID=$faturaId da tabela faturas_lixeira: nenhuma linha afetada")
                            Toast.makeText(this, "Erro ao remover fatura da lixeira", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("LixeiraActivity", "Erro ao inserir fatura ID=$faturaId na tabela faturas: inserção falhou")
                        Toast.makeText(this, "Erro ao inserir fatura na tabela principal", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na tabela faturas_lixeira")
                    Toast.makeText(this, "Fatura não encontrada na lixeira", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Log.e("LixeiraActivity", "Cursor nulo ao consultar fatura ID=$faturaId")
                Toast.makeText(this, "Erro ao consultar fatura", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Exceção ao restaurar fatura ID=$faturaId: ${e.message}", e)
            Toast.makeText(this, "Erro ao restaurar fatura: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // Finaliza a transação
            if (db.inTransaction()) {
                db.endTransaction()
            }
        }
    }

    /**
     * Restaura as fotos de uma fatura da lixeira
     */
    private fun restaurarFotosFatura(faturaLixeiraId: Long, novaFaturaId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            // Buscar fotos da fatura na lixeira (se existir tabela de fotos da lixeira)
            val cursor = db.query(
                "faturas_fotos_lixeira", // Assumindo que existe uma tabela de fotos da lixeira
                null,
                "fatura_id = ?",
                arrayOf(faturaLixeiraId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val fotoPath = it.getString(it.getColumnIndexOrThrow("foto_path"))
                    val values = ContentValues().apply {
                        put("fatura_id", novaFaturaId)
                        put("foto_path", fotoPath)
                    }
                    db.insert("faturas_fotos", null, values)
                }
            }
            Log.d("LixeiraActivity", "Fotos restauradas para fatura ID=$novaFaturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao restaurar fotos da fatura: ${e.message}")
            // Não falha a restauração se não conseguir restaurar as fotos
        }
    }

    /**
     * Restaura as notas de uma fatura da lixeira
     */
    private fun restaurarNotasFatura(faturaLixeiraId: Long, novaFaturaId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            // Buscar notas da fatura na lixeira (se existir tabela de notas da lixeira)
            val cursor = db.query(
                "faturas_notas_lixeira", // Assumindo que existe uma tabela de notas da lixeira
                null,
                "fatura_id = ?",
                arrayOf(faturaLixeiraId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val nota = it.getString(it.getColumnIndexOrThrow("nota"))
                    val values = ContentValues().apply {
                        put("fatura_id", novaFaturaId)
                        put("nota", nota)
                    }
                    db.insert("faturas_notas", null, values)
                }
            }
            Log.d("LixeiraActivity", "Notas restauradas para fatura ID=$novaFaturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao restaurar notas da fatura: ${e.message}")
            // Não falha a restauração se não conseguir restaurar as notas
        }
    }

    /**
     * Restaura os itens de uma fatura da lixeira
     */
    private fun restaurarItensFatura(faturaLixeiraId: Long, novaFaturaId: Long, db: android.database.sqlite.SQLiteDatabase) {
        try {
            // Buscar itens da fatura na lixeira (se existir tabela de itens da lixeira)
            val cursor = db.query(
                "faturas_itens_lixeira", // Assumindo que existe uma tabela de itens da lixeira
                null,
                "fatura_id = ?",
                arrayOf(faturaLixeiraId.toString()),
                null, null, null
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val nome = it.getString(it.getColumnIndexOrThrow("nome"))
                    val quantidade = it.getInt(it.getColumnIndexOrThrow("quantidade"))
                    val preco = it.getDouble(it.getColumnIndexOrThrow("preco"))
                    val numeroSerial = it.getString(it.getColumnIndexOrThrow("numero_serial"))
                    val descricao = it.getString(it.getColumnIndexOrThrow("descricao"))
                    
                    val values = ContentValues().apply {
                        put("fatura_id", novaFaturaId)
                        put("nome", nome)
                        put("quantidade", quantidade)
                        put("preco", preco)
                        put("numero_serial", numeroSerial)
                        put("descricao", descricao)
                    }
                    db.insert("faturas_itens", null, values)
                }
            }
            Log.d("LixeiraActivity", "Itens restaurados para fatura ID=$novaFaturaId")
        } catch (e: Exception) {
            Log.w("LixeiraActivity", "Erro ao restaurar itens da fatura: ${e.message}")
            // Não falha a restauração se não conseguir restaurar os itens
        }
    }

    /**
     * [5] Exclui uma fatura permanentemente da tabela de lixeira.
     * @param faturaId O ID da fatura a ser excluída.
     * @return True se a exclusão for bem-sucedida, false caso contrário.
     */
    private fun excluirFaturaPermanente(faturaId: Long): Boolean {
        val db = dbHelper?.writableDatabase
        if (db == null) {
            Log.e("LixeiraActivity", "Erro ao acessar o banco de dados")
            Toast.makeText(this, "Erro ao acessar o banco de dados", Toast.LENGTH_LONG).show()
            return false
        }

        try {
            val rowsDeleted = db.delete(
                FaturaLixeiraContract.FaturaLixeiraEntry.TABLE_NAME,
                "${android.provider.BaseColumns._ID} = ?",
                arrayOf(faturaId.toString())
            )
            if (rowsDeleted > 0) {
                Log.d("LixeiraActivity", "Fatura ID=$faturaId excluída permanentemente com sucesso")
                Toast.makeText(this, "Fatura excluída permanentemente", Toast.LENGTH_SHORT).show()
                carregarFaturasLixeira()
                return true
            } else {
                Log.w("LixeiraActivity", "Fatura ID=$faturaId não encontrada na lixeira")
                Toast.makeText(this, "Fatura não encontrada na lixeira", Toast.LENGTH_LONG).show()
                return false
            }
        } catch (e: Exception) {
            Log.e("LixeiraActivity", "Erro ao excluir fatura permanentemente: ${e.message}")
            Toast.makeText(this, "Erro ao excluir fatura: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    override fun onBackPressed() {
        Log.d("LixeiraActivity", "onBackPressed disparado.")
        // Voltar para a MainActivity
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

data class FaturaLixeiraItem(
    val id: Long,
    val numeroFatura: String,
    val cliente: String,
    val data: String
)

class FaturaLixeiraAdapter(
    private var faturas: List<FaturaLixeiraItem>,
    private val onRestoreClick: (Long) -> Unit,
    private val onLongClick: (Long) -> Boolean
) : RecyclerView.Adapter<FaturaLixeiraAdapter.FaturaViewHolder>() {

    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numeroFaturaTextView: TextView = itemView.findViewById(R.id.numeroFaturaTextView)
        val clienteTextView: TextView = itemView.findViewById(R.id.clienteTextView)
        val dataTextView: TextView = itemView.findViewById(R.id.dataTextView)
        val restoreButton: Button = itemView.findViewById(R.id.restoreButton)
    }

    /**
     * [6] Cria um novo ViewHolder para um item da lista de faturas.
     * @param parent O ViewGroup pai onde a view será inflada.
     * @param viewType O tipo de view (não utilizado aqui).
     * @return Um novo FaturaViewHolder com a view inflada.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fatura_lixeira, parent, false)
        return FaturaViewHolder(view)
    }

    /**
     * [7] Vincula os dados de uma fatura ao ViewHolder correspondente.
     * Configura os listeners de clique e clique longo.
     * @param holder O ViewHolder que será atualizado.
     * @param position A posição do item na lista.
     */
    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        val fatura = faturas[position]
        holder.numeroFaturaTextView.text = fatura.numeroFatura
        holder.clienteTextView.text = fatura.cliente
        holder.dataTextView.text = fatura.data
        holder.restoreButton.setOnClickListener {
            onRestoreClick(fatura.id)
        }
        holder.itemView.setOnLongClickListener {
            onLongClick(fatura.id)
            true
        }
    }

    /**
     * [8] Retorna o número total de faturas na lista.
     * @return O tamanho da lista de faturas.
     */
    override fun getItemCount(): Int = faturas.size

    /**
     * [9] Atualiza a lista de faturas no adaptador e notifica a UI sobre mudanças.
     * @param newFaturas A nova lista de faturas a ser exibida.
     */
    fun updateFaturas(newFaturas: List<FaturaLixeiraItem>) {
        faturas = newFaturas
        notifyDataSetChanged()
    }
}