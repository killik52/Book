package database.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import database.entities.Cliente
import database.entities.Artigo
import database.entities.Fatura
import database.entities.FaturaLixeira
import database.entities.ClienteBloqueado
import database.repository.ClienteRepository
import database.repository.ArtigoRepository
import database.repository.FaturaRepository
import database.repository.FaturaLixeiraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataMigrationHelper(private val context: Context) {
    
    companion object {
        private const val OLD_DATABASE_NAME = "myapplication.db"
        private const val OLD_DATABASE_VERSION = 19
    }
    
    // Helper para acessar o banco antigo
    private val oldDbHelper: SQLiteOpenHelper = object : SQLiteOpenHelper(context, OLD_DATABASE_NAME, null, OLD_DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {}
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
    
    fun hasOldDatabase(): Boolean {
        return try {
            val db = oldDbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val hasTables = cursor.count > 0
            cursor.close()
            hasTables
        } catch (e: Exception) {
            Log.e("DataMigrationHelper", "Erro ao verificar banco antigo: ${e.message}")
            false
        }
    }
    
    /**
     * Migra todos os dados do SQLite antigo para o Room (versão simplificada)
     */
    suspend fun migrateAllData() = withContext(Dispatchers.IO) {
        try {
            Log.d("DataMigrationHelper", "Iniciando migração automática de dados")
            
            // Obtém as instâncias dos repositories
            val database = database.AppDatabase.getDatabase(context)
            val clienteRepository = ClienteRepository(database.clienteDao())
            val artigoRepository = ArtigoRepository(database.artigoDao())
            val faturaRepository = FaturaRepository(database.faturaDao())
            val faturaLixeiraRepository = FaturaLixeiraRepository(database.faturaLixeiraDao())
            
            // Migra clientes
            migrateClientes(clienteRepository)
            
            // Migra artigos
            migrateArtigos(artigoRepository)
            
            // Migra faturas
            migrateFaturas(faturaRepository)
            
            // Migra faturas da lixeira
            migrateFaturaLixeira(faturaLixeiraRepository)
            
            // Migra clientes bloqueados
            migrateClientesBloqueados(clienteRepository)
            
            Log.d("DataMigrationHelper", "Migração automática finalizada com sucesso")
        } catch (e: Exception) {
            Log.e("DataMigrationHelper", "Erro durante migração automática: ${e.message}", e)
            throw e
        } finally {
            oldDbHelper.close()
        }
    }
    
    /**
     * Migra todos os dados do SQLite antigo para o Room
     */
    suspend fun migrateAllData(
        clienteRepository: ClienteRepository,
        artigoRepository: ArtigoRepository,
        faturaRepository: FaturaRepository,
        faturaLixeiraRepository: FaturaLixeiraRepository,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d("DataMigrationHelper", "Iniciando migração completa de dados")
            
            // Migra clientes
            onProgress("Migrando clientes...", 10)
            migrateClientes(clienteRepository)
            
            // Migra artigos
            onProgress("Migrando artigos...", 30)
            migrateArtigos(artigoRepository)
            
            // Migra faturas
            onProgress("Migrando faturas...", 60)
            migrateFaturas(faturaRepository)
            
            // Migra faturas da lixeira
            onProgress("Migrando lixeira...", 80)
            migrateFaturaLixeira(faturaLixeiraRepository)
            
            // Migra clientes bloqueados
            onProgress("Migrando clientes bloqueados...", 90)
            migrateClientesBloqueados(clienteRepository)
            
            onProgress("Migração concluída!", 100)
            Log.d("DataMigrationHelper", "Migração completa finalizada com sucesso")
        } catch (e: Exception) {
            Log.e("DataMigrationHelper", "Erro durante migração: ${e.message}", e)
            throw e
        } finally {
            oldDbHelper.close()
        }
    }
    
    private suspend fun migrateClientes(clienteRepository: ClienteRepository) {
        val db = oldDbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM clientes", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val cliente = Cliente(
                    id = it.getLong(it.getColumnIndexOrThrow("_id")),
                    nome = it.getString(it.getColumnIndexOrThrow("nome")) ?: "",
                    email = it.getString(it.getColumnIndexOrThrow("email")) ?: "",
                    telefone = it.getString(it.getColumnIndexOrThrow("telefone")) ?: "",
                    informacoesAdicionais = it.getString(it.getColumnIndexOrThrow("informacoes_adicionais")) ?: "",
                    cpf = it.getString(it.getColumnIndexOrThrow("cpf")) ?: "",
                    cnpj = it.getString(it.getColumnIndexOrThrow("cnpj")) ?: "",
                    numeroSerial = it.getString(it.getColumnIndexOrThrow("numero_serial")) ?: "",
                    logradouro = it.getString(it.getColumnIndexOrThrow("logradouro")) ?: "",
                    numero = it.getString(it.getColumnIndexOrThrow("numero")) ?: "",
                    complemento = it.getString(it.getColumnIndexOrThrow("complemento")) ?: "",
                    bairro = it.getString(it.getColumnIndexOrThrow("bairro")) ?: "",
                    municipio = it.getString(it.getColumnIndexOrThrow("municipio")) ?: "",
                    uf = it.getString(it.getColumnIndexOrThrow("uf")) ?: "",
                    cep = it.getString(it.getColumnIndexOrThrow("cep")) ?: ""
                )
                
                try {
                    clienteRepository.insertCliente(cliente)
                    Log.d("DataMigrationHelper", "Cliente migrado: ${cliente.nome}")
                } catch (e: Exception) {
                    Log.e("DataMigrationHelper", "Erro ao migrar cliente ${cliente.nome}: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun migrateArtigos(artigoRepository: ArtigoRepository) {
        val db = oldDbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM artigos", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val artigo = Artigo(
                    id = it.getLong(it.getColumnIndexOrThrow("_id")),
                    nome = it.getString(it.getColumnIndexOrThrow("nome")) ?: "",
                    preco = it.getDouble(it.getColumnIndexOrThrow("preco")),
                    quantidade = it.getInt(it.getColumnIndexOrThrow("quantidade")),
                    desconto = it.getDouble(it.getColumnIndexOrThrow("desconto")),
                    descricao = it.getString(it.getColumnIndexOrThrow("descricao")) ?: "",
                    guardarFatura = it.getInt(it.getColumnIndexOrThrow("guardar_fatura")) == 1,
                    numeroSerial = it.getString(it.getColumnIndexOrThrow("numero_serial")) ?: ""
                )
                
                try {
                    artigoRepository.insertArtigo(artigo)
                    Log.d("DataMigrationHelper", "Artigo migrado: ${artigo.nome}")
                } catch (e: Exception) {
                    Log.e("DataMigrationHelper", "Erro ao migrar artigo ${artigo.nome}: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun migrateFaturas(faturaRepository: FaturaRepository) {
        val db = oldDbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM faturas", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val fatura = Fatura(
                    id = it.getLong(it.getColumnIndexOrThrow("_id")),
                    numeroFatura = it.getString(it.getColumnIndexOrThrow("numero_fatura")) ?: "",
                    cliente = it.getString(it.getColumnIndexOrThrow("cliente")) ?: "",
                    artigos = it.getString(it.getColumnIndexOrThrow("artigos")) ?: "",
                    subtotal = it.getDouble(it.getColumnIndexOrThrow("subtotal")),
                    desconto = it.getDouble(it.getColumnIndexOrThrow("desconto")),
                    descontoPercent = it.getInt(it.getColumnIndexOrThrow("desconto_percent")),
                    taxaEntrega = it.getDouble(it.getColumnIndexOrThrow("taxa_entrega")),
                    saldoDevedor = it.getDouble(it.getColumnIndexOrThrow("saldo_devedor")),
                    data = it.getString(it.getColumnIndexOrThrow("data")) ?: "",
                    fotosImpressora = it.getString(it.getColumnIndexOrThrow("foto_impressora")) ?: "",
                    notas = it.getString(it.getColumnIndexOrThrow("notas")) ?: "",
                    foiEnviada = it.getInt(it.getColumnIndexOrThrow("foi_enviada")) == 1
                )
                
                try {
                    faturaRepository.insertFatura(fatura)
                    Log.d("DataMigrationHelper", "Fatura migrada: ${fatura.numeroFatura}")
                } catch (e: Exception) {
                    Log.e("DataMigrationHelper", "Erro ao migrar fatura ${fatura.numeroFatura}: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun migrateFaturaLixeira(faturaLixeiraRepository: FaturaLixeiraRepository) {
        val db = oldDbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM faturas_lixeira", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val faturaLixeira = FaturaLixeira(
                    id = it.getLong(it.getColumnIndexOrThrow("_id")),
                    numeroFatura = it.getString(it.getColumnIndexOrThrow("numero_fatura")),
                    cliente = it.getString(it.getColumnIndexOrThrow("cliente")),
                    artigos = it.getString(it.getColumnIndexOrThrow("artigos")),
                    subtotal = it.getDouble(it.getColumnIndexOrThrow("subtotal")),
                    desconto = it.getDouble(it.getColumnIndexOrThrow("desconto")),
                    descontoPercent = it.getInt(it.getColumnIndexOrThrow("desconto_percent")),
                    taxaEntrega = it.getDouble(it.getColumnIndexOrThrow("taxa_entrega")),
                    saldoDevedor = it.getDouble(it.getColumnIndexOrThrow("saldo_devedor")),
                    data = it.getString(it.getColumnIndexOrThrow("data")),
                    fotosImpressora = it.getString(it.getColumnIndexOrThrow("foto_impressora")),
                    notas = it.getString(it.getColumnIndexOrThrow("notas"))
                )
                
                try {
                    faturaLixeiraRepository.insertFaturaLixeira(faturaLixeira)
                    Log.d("DataMigrationHelper", "Fatura da lixeira migrada: ${faturaLixeira.numeroFatura}")
                } catch (e: Exception) {
                    Log.e("DataMigrationHelper", "Erro ao migrar fatura da lixeira ${faturaLixeira.numeroFatura}: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun migrateClientesBloqueados(clienteRepository: ClienteRepository) {
        val db = oldDbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM clientes_bloqueados", null)
        
        cursor.use {
            while (it.moveToNext()) {
                val clienteBloqueado = ClienteBloqueado(
                    id = it.getLong(it.getColumnIndexOrThrow("_id")),
                    nome = it.getString(it.getColumnIndexOrThrow("nome")) ?: "",
                    email = it.getString(it.getColumnIndexOrThrow("email")) ?: "",
                    telefone = it.getString(it.getColumnIndexOrThrow("telefone")) ?: "",
                    informacoesAdicionais = it.getString(it.getColumnIndexOrThrow("informacoes_adicionais")) ?: "",
                    cpf = it.getString(it.getColumnIndexOrThrow("cpf")) ?: "",
                    cnpj = it.getString(it.getColumnIndexOrThrow("cnpj")) ?: "",
                    numeroSerial = it.getString(it.getColumnIndexOrThrow("numero_serial")) ?: "",
                    logradouro = it.getString(it.getColumnIndexOrThrow("logradouro")) ?: "",
                    numero = it.getString(it.getColumnIndexOrThrow("numero")) ?: "",
                    complemento = it.getString(it.getColumnIndexOrThrow("complemento")) ?: "",
                    bairro = it.getString(it.getColumnIndexOrThrow("bairro")) ?: "",
                    municipio = it.getString(it.getColumnIndexOrThrow("municipio")) ?: "",
                    uf = it.getString(it.getColumnIndexOrThrow("uf")) ?: "",
                    cep = it.getString(it.getColumnIndexOrThrow("cep")) ?: ""
                )
                
                try {
                    // TODO: Implementar repository específico para clientes bloqueados
                    // Por enquanto, vamos apenas logar
                    Log.d("DataMigrationHelper", "Cliente bloqueado encontrado: ${clienteBloqueado.nome}")
                    // clienteRepository.insertClienteBloqueado(clienteBloqueado) // Descomentar quando implementar
                } catch (e: Exception) {
                    Log.e("DataMigrationHelper", "Erro ao migrar cliente bloqueado ${clienteBloqueado.nome}: ${e.message}")
                }
            }
        }
    }
    
    fun getMigrationStats(): MigrationStats {
        val db = oldDbHelper.readableDatabase
        return MigrationStats(
            clientesCount = getTableCount(db, "clientes"),
            artigosCount = getTableCount(db, "artigos"),
            faturasCount = getTableCount(db, "faturas"),
            lixeiraCount = getTableCount(db, "faturas_lixeira"),
            clientesBloqueadosCount = getTableCount(db, "clientes_bloqueados")
        )
    }
    
    private fun getTableCount(db: SQLiteDatabase, tableName: String): Int {
        return try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $tableName", null)
            cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Log.e("DataMigrationHelper", "Erro ao contar registros da tabela $tableName: ${e.message}")
            0
        }
    }
    
    data class MigrationStats(
        val clientesCount: Int,
        val artigosCount: Int,
        val faturasCount: Int,
        val lixeiraCount: Int,
        val clientesBloqueadosCount: Int
    ) {
        val totalRecords = clientesCount + artigosCount + faturasCount + lixeiraCount + clientesBloqueadosCount
    }
    
    /**
     * Apaga o banco de dados antigo após migração bem-sucedida
     */
    fun deleteOldDatabase() {
        try {
            oldDbHelper.close()
            val dbFile = context.getDatabasePath(OLD_DATABASE_NAME)
            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                if (deleted) {
                    Log.d("DataMigrationHelper", "Banco de dados antigo apagado com sucesso")
                } else {
                    Log.w("DataMigrationHelper", "Não foi possível apagar o banco de dados antigo")
                }
            } else {
                Log.d("DataMigrationHelper", "Arquivo do banco antigo não encontrado")
            }
        } catch (e: Exception) {
            Log.e("DataMigrationHelper", "Erro ao apagar banco antigo: ${e.message}")
        }
    }
} 