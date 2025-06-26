package database.repository

import database.dao.FaturaDao
import database.dao.FaturaLixeiraDao
import database.entities.Fatura
import database.entities.FaturaLixeira
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FaturaRepository(
    private val faturaDao: FaturaDao,
    private val faturaLixeiraDao: FaturaLixeiraDao? = null
) {
    fun getAllFaturas(): Flow<List<Fatura>> = faturaDao.getAllFaturas()
    
    fun getFaturaById(id: Long): Flow<Fatura?> = faturaDao.getFaturaById(id)
    
    fun searchFaturas(searchQuery: String): Flow<List<Fatura>> = faturaDao.searchFaturas(searchQuery)
    
    fun searchFaturasAdvanced(searchQuery: String): Flow<List<Fatura>> = faturaDao.searchFaturasAdvanced(searchQuery)
    
    fun getFaturasByEnvioStatus(foiEnviada: Boolean): Flow<List<Fatura>> = faturaDao.getFaturasByEnvioStatus(foiEnviada)
    
    fun getFaturasByDateRange(startDate: String, endDate: String): Flow<List<Fatura>> = faturaDao.getFaturasByDateRange(startDate, endDate)
    
    fun getTotalFaturasByDateRange(startDate: String, endDate: String): Flow<Double?> = faturaDao.getTotalFaturasByDateRange(startDate, endDate)
    
    suspend fun insertFatura(fatura: Fatura): Long = faturaDao.insertFatura(fatura)
    
    suspend fun updateFatura(fatura: Fatura) = faturaDao.updateFatura(fatura)
    
    suspend fun deleteFatura(fatura: Fatura) = faturaDao.deleteFatura(fatura)
    
    suspend fun deleteFaturaById(id: Long) = faturaDao.deleteFaturaById(id)
    
    fun getFaturaCount(): Flow<Int> = faturaDao.getFaturaCount()
    
    fun getFaturasEnviadasCount(): Flow<Int> = faturaDao.getFaturasEnviadasCount()
    
    fun getFaturasNaoEnviadasCount(): Flow<Int> = faturaDao.getFaturasNaoEnviadasCount()
    
    suspend fun insertFaturaFromLixeira(faturaLixeira: FaturaLixeira): Long {
        val fatura = Fatura(
            id = 0, // novo id
            numeroFatura = faturaLixeira.numeroFatura ?: "",
            cliente = faturaLixeira.cliente ?: "",
            artigos = faturaLixeira.artigos ?: "",
            subtotal = faturaLixeira.subtotal ?: 0.0,
            desconto = faturaLixeira.desconto ?: 0.0,
            descontoPercent = faturaLixeira.descontoPercent ?: 0,
            taxaEntrega = faturaLixeira.taxaEntrega ?: 0.0,
            saldoDevedor = faturaLixeira.saldoDevedor ?: 0.0,
            data = faturaLixeira.data ?: "",
            fotosImpressora = faturaLixeira.fotosImpressora ?: "",
            notas = faturaLixeira.notas ?: "",
            foiEnviada = false
        )
        return faturaDao.insertFatura(fatura)
    }

    suspend fun restaurarFaturaDaLixeiraRoom(
        faturaLixeira: FaturaLixeira,
        faturaItemLixeiraDao: database.dao.FaturaItemLixeiraDao,
        faturaFotoLixeiraDao: database.dao.FaturaFotoLixeiraDao,
        faturaNotaLixeiraDao: database.dao.FaturaNotaLixeiraDao,
        faturaItemDao: database.dao.FaturaItemDao,
        faturaFotoDao: database.dao.FaturaFotoDao,
        faturaNotaDao: database.dao.FaturaNotaDao,
        faturaLixeiraDao: database.dao.FaturaLixeiraDao
    ): Long {
        // 1. Restaura a fatura principal
        val fatura = Fatura(
            id = 0,
            numeroFatura = faturaLixeira.numeroFatura ?: "",
            cliente = faturaLixeira.cliente ?: "",
            artigos = faturaLixeira.artigos ?: "",
            subtotal = faturaLixeira.subtotal ?: 0.0,
            desconto = faturaLixeira.desconto ?: 0.0,
            descontoPercent = faturaLixeira.descontoPercent ?: 0,
            taxaEntrega = faturaLixeira.taxaEntrega ?: 0.0,
            saldoDevedor = faturaLixeira.saldoDevedor ?: 0.0,
            data = faturaLixeira.data ?: "",
            fotosImpressora = faturaLixeira.fotosImpressora ?: "",
            notas = faturaLixeira.notas ?: "",
            foiEnviada = false
        )
        val novoFaturaId = faturaDao.insertFatura(fatura)

        // 2. Restaura os itens
        val itensLixeira = faturaItemLixeiraDao.getItensByFaturaLixeiraId(faturaLixeira.id)
        val itens = itensLixeira.map {
            database.entities.FaturaItem(
                faturaId = novoFaturaId,
                artigoId = it.artigoId,
                quantidade = it.quantidade,
                preco = it.preco,
                clienteId = it.clienteId
            )
        }
        faturaItemDao.insertFaturaItems(itens)

        // 3. Restaura as fotos
        val fotosLixeira = faturaFotoLixeiraDao.getFotosByFaturaLixeiraId(faturaLixeira.id)
        val fotos = fotosLixeira.map {
            database.entities.FaturaFoto(
                faturaId = novoFaturaId,
                photoPath = it.photoPath
            )
        }
        faturaFotoDao.insertFaturaFotos(fotos)

        // 4. Restaura as notas
        val notasLixeira = faturaNotaLixeiraDao.getNotasByFaturaLixeiraId(faturaLixeira.id)
        val notas = notasLixeira.map {
            database.entities.FaturaNota(
                faturaId = novoFaturaId,
                nota = it.nota
            )
        }
        notas.forEach { faturaNotaDao.insertFaturaNota(it) }

        // 5. Remove dados da lixeira
        faturaItemLixeiraDao.deleteByFaturaLixeiraId(faturaLixeira.id)
        faturaFotoLixeiraDao.deleteByFaturaLixeiraId(faturaLixeira.id)
        faturaNotaLixeiraDao.deleteByFaturaLixeiraId(faturaLixeira.id)
        faturaLixeiraDao.deleteFaturaLixeira(faturaLixeira)

        return novoFaturaId
    }

    suspend fun moverFaturaParaLixeiraRoom(
        fatura: Fatura,
        faturaItemDao: database.dao.FaturaItemDao,
        faturaFotoDao: database.dao.FaturaFotoDao,
        faturaNotaDao: database.dao.FaturaNotaDao,
        faturaItemLixeiraDao: database.dao.FaturaItemLixeiraDao,
        faturaFotoLixeiraDao: database.dao.FaturaFotoLixeiraDao,
        faturaNotaLixeiraDao: database.dao.FaturaNotaLixeiraDao,
        faturaLixeiraDao: database.dao.FaturaLixeiraDao
    ) {
        // 1. Copia a fatura para a lixeira
        val faturaLixeira = FaturaLixeira(
            numeroFatura = fatura.numeroFatura,
            cliente = fatura.cliente,
            artigos = fatura.artigos,
            subtotal = fatura.subtotal,
            desconto = fatura.desconto,
            descontoPercent = fatura.descontoPercent,
            taxaEntrega = fatura.taxaEntrega,
            saldoDevedor = fatura.saldoDevedor,
            data = fatura.data,
            fotosImpressora = fatura.fotosImpressora,
            notas = fatura.notas
        )
        val faturaLixeiraId = faturaLixeiraDao.insertFaturaLixeira(faturaLixeira)

        // 2. Copia os itens
        val itens = faturaItemDao.getFaturaItemsByFaturaId(fatura.id).first()
        val itensLixeira = itens.map {
            database.entities.FaturaItemLixeira(
                faturaLixeiraId = faturaLixeiraId,
                artigoId = it.artigoId,
                quantidade = it.quantidade,
                preco = it.preco,
                clienteId = it.clienteId
            )
        }
        faturaItemLixeiraDao.insertAll(itensLixeira)

        // 3. Copia as fotos
        val fotos = faturaFotoDao.getFaturaFotosByFaturaId(fatura.id).first()
        val fotosLixeira = fotos.map {
            database.entities.FaturaFotoLixeira(
                faturaLixeiraId = faturaLixeiraId,
                photoPath = it.photoPath
            )
        }
        faturaFotoLixeiraDao.insertAll(fotosLixeira)

        // 4. Copia as notas
        val notas = faturaNotaDao.getFaturaNotasByFaturaId(fatura.id).first()
        val notasLixeira = notas.map {
            database.entities.FaturaNotaLixeira(
                faturaLixeiraId = faturaLixeiraId,
                nota = it.nota
            )
        }
        faturaNotaLixeiraDao.insertAll(notasLixeira)

        // 5. Remove a fatura e dados originais
        faturaItemDao.deleteFaturaItemsByFaturaId(fatura.id)
        faturaFotoDao.deleteFaturaFotosByFaturaId(fatura.id)
        faturaNotaDao.deleteFaturaNotasByFaturaId(fatura.id)
        faturaDao.deleteFatura(fatura)
    }
} 