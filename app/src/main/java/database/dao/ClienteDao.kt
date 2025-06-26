package database.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import database.entities.Cliente

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientes(): LiveData<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun getClienteById(id: Long): LiveData<Cliente?>

    @Query("SELECT * FROM clientes WHERE nome LIKE '%' || :searchQuery || '%' OR email LIKE '%' || :searchQuery || '%' OR telefone LIKE '%' || :searchQuery || '%'")
    fun searchClientes(searchQuery: String): LiveData<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE numeroSerial = :numeroSerial")
    fun getClienteByNumeroSerial(numeroSerial: String): LiveData<Cliente?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCliente(cliente: Cliente): Long

    @Update
    suspend fun updateCliente(cliente: Cliente)

    @Delete
    suspend fun deleteCliente(cliente: Cliente)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun deleteClienteById(id: Long)

    @Query("DELETE FROM clientes")
    suspend fun deleteAllClientes()

    @Query("SELECT COUNT(*) FROM clientes")
    fun getClienteCount(): LiveData<Int>

    @Query("SELECT * FROM clientes ORDER BY id DESC LIMIT :limit")
    fun getRecentClientes(limit: Int): LiveData<List<Cliente>>
} 