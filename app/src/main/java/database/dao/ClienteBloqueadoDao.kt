package database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import database.entities.ClienteBloqueado

@Dao
interface ClienteBloqueadoDao {
    @Query("SELECT * FROM clientes_bloqueados ORDER BY nome ASC")
    fun getAllClientesBloqueados(): LiveData<List<ClienteBloqueado>>

    @Query("SELECT * FROM clientes_bloqueados WHERE id = :id")
    fun getClienteBloqueadoById(id: Long): LiveData<ClienteBloqueado?>

    @Query("SELECT * FROM clientes_bloqueados WHERE nome LIKE '%' || :searchQuery || '%' OR email LIKE '%' || :searchQuery || '%' OR telefone LIKE '%' || :searchQuery || '%'")
    fun searchClientesBloqueados(searchQuery: String): LiveData<List<ClienteBloqueado>>

    @Query("SELECT * FROM clientes_bloqueados WHERE numeroSerial = :numeroSerial")
    fun getClienteBloqueadoByNumeroSerial(numeroSerial: String): LiveData<ClienteBloqueado?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clienteBloqueado: ClienteBloqueado): Long

    @Update
    suspend fun update(clienteBloqueado: ClienteBloqueado)

    @Delete
    suspend fun delete(clienteBloqueado: ClienteBloqueado)

    @Query("DELETE FROM clientes_bloqueados WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM clientes_bloqueados")
    suspend fun getClienteBloqueadoCount(): Int

    @Query("DELETE FROM clientes_bloqueados WHERE id = :id")
    suspend fun deleteClienteBloqueadoById(id: Long)

    @Query("DELETE FROM clientes_bloqueados")
    suspend fun deleteAllClientesBloqueados()
} 