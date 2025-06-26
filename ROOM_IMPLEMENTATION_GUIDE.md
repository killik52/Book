# Guia Completo de Implementação do Room

## Visão Geral

Este guia mostra como implementar e migrar completamente para o Room Database no seu projeto Android. O Room é uma biblioteca de persistência que fornece uma camada de abstração sobre o SQLite, oferecendo:

- **Compile-time verification** de queries SQL
- **Annotations** para reduzir código boilerplate
- **Integração com Kotlin Coroutines** e **Flow**
- **Migração automática** de esquemas
- **Type converters** para tipos complexos

## Estrutura Atual vs Nova

### Estrutura Atual (SQLite Tradicional)
```
ClienteDbHelper (SQLiteOpenHelper)
├── ClienteContract
├── ArtigoContract
├── FaturaContract
└── Activities usando SQLite diretamente
```

### Nova Estrutura (Room)
```
AppDatabase (RoomDatabase)
├── Entities (Entidades)
├── DAOs (Data Access Objects)
├── Repositories (Repositórios)
├── ViewModels (ViewModel)
└── Activities usando ViewModels
```

## Passos para Implementação

### 1. Dependências (já configuradas)

As dependências do Room já estão configuradas no seu `build.gradle.kts`:

```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
```

### 2. Estrutura de Arquivos Criada

#### Application Class
- `MyApplication.kt` - Inicializa o banco de dados Room

#### ViewModels
- `ClienteViewModel.kt` - Gerencia dados de clientes com Room

#### Activities de Exemplo
- `ClienteActivityRoom.kt` - Exemplo de Activity usando Room
- `ListarClientesActivityRoom.kt` - Exemplo com RecyclerView e Flow

#### Utilitários
- `DataMigrationHelper.kt` - Migra dados do SQLite para Room

### 3. Como Usar o Room

#### Inicialização
```kotlin
// Na Application class
class MyApplication : Application() {
    lateinit var database: AppDatabase
        private set
    
    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
    }
}
```

#### Em uma Activity
```kotlin
class MinhaActivity : AppCompatActivity() {
    private lateinit var viewModel: ClienteViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa o ViewModel
        val repository = ClienteRepository((application as MyApplication).database.clienteDao())
        val factory = ClienteViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ClienteViewModel::class.java]
        
        // Observa dados
        lifecycleScope.launch {
            viewModel.allClientes.collectLatest { clientes ->
                // Atualiza UI
            }
        }
    }
}
```

### 4. Vantagens do Room

#### 1. Type Safety
```kotlin
// Room verifica em tempo de compilação
@Query("SELECT * FROM clientes WHERE nome = :nome")
suspend fun getClienteByNome(nome: String): Cliente?
```

#### 2. Observação Reativa com Flow
```kotlin
@Query("SELECT * FROM clientes ORDER BY nome ASC")
fun getAllClientes(): Flow<List<Cliente>>
```

#### 3. Coroutines Integration
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertCliente(cliente: Cliente): Long
```

#### 4. Relacionamentos
```kotlin
@Relation(
    parentColumn = "id",
    entityColumn = "fatura_id"
)
val itens: List<FaturaItem>
```

### 5. Migração de Dados

#### Verificar se existe banco antigo
```kotlin
val migrationHelper = DataMigrationHelper(this)
if (migrationHelper.hasOldDatabase()) {
    // Executar migração
}
```

#### Executar migração
```kotlin
migrationHelper.migrateAllData(
    clienteRepository,
    artigoRepository,
    faturaRepository
) { progress ->
    // Atualizar progresso na UI
}
```

### 6. Padrões Recomendados

#### Repository Pattern
```kotlin
class ClienteRepository(private val clienteDao: ClienteDao) {
    fun getAllClientes(): Flow<List<Cliente>> = clienteDao.getAllClientes()
    
    suspend fun insertCliente(cliente: Cliente): Long = clienteDao.insertCliente(cliente)
    
    suspend fun updateCliente(cliente: Cliente) = clienteDao.updateCliente(cliente)
    
    suspend fun deleteCliente(cliente: Cliente) = clienteDao.deleteCliente(cliente)
}
```

#### ViewModel Pattern
```kotlin
class ClienteViewModel(private val repository: ClienteRepository) : ViewModel() {
    val allClientes: Flow<List<Cliente>> = repository.getAllClientes()
    
    fun insertCliente(cliente: Cliente, onSuccess: (Long) -> Unit = {}, onError: (Exception) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.insertCliente(cliente)
                onSuccess(id)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}
```

### 7. Migração Gradual

#### Estratégia Recomendada
1. **Fase 1**: Implementar Room paralelamente ao SQLite
2. **Fase 2**: Migrar dados existentes
3. **Fase 3**: Migrar Activities uma por vez
4. **Fase 4**: Remover código SQLite antigo

#### Exemplo de Migração Gradual
```kotlin
class ClienteActivity : AppCompatActivity() {
    private lateinit var viewModel: ClienteViewModel
    private lateinit var dbHelper: ClienteDbHelper // Mantido temporariamente
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa ambos
        val repository = ClienteRepository((application as MyApplication).database.clienteDao())
        val factory = ClienteViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ClienteViewModel::class.java]
        
        dbHelper = ClienteDbHelper(this) // Para compatibilidade temporária
        
        // Usa Room para novas funcionalidades
        // Usa SQLite para funcionalidades existentes
    }
}
```

### 8. Testes

#### Testes Unitários
```kotlin
@RunWith(AndroidJUnit4::class)
class ClienteDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var clienteDao: ClienteDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        clienteDao = database.clienteDao()
    }
    
    @Test
    fun insertAndReadCliente() = runTest {
        val cliente = Cliente(nome = "João", email = "joao@email.com")
        val id = clienteDao.insertCliente(cliente)
        
        val loaded = clienteDao.getClienteById(id)
        assertThat(loaded?.nome).isEqualTo("João")
    }
    
    @After
    fun closeDb() {
        database.close()
    }
}
```

### 9. Performance

#### Índices
```kotlin
@Entity(
    tableName = "clientes",
    indices = [Index("nome"), Index("email")]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String?,
    val email: String?
)
```

#### Queries Otimizadas
```kotlin
@Query("SELECT * FROM clientes WHERE nome LIKE '%' || :searchQuery || '%' LIMIT 50")
fun searchClientes(searchQuery: String): Flow<List<Cliente>>
```

### 10. Troubleshooting

#### Problemas Comuns

1. **Erro de Compilação**: Verificar se `@Database` está correto
2. **Erro de Migração**: Usar `fallbackToDestructiveMigration()` para desenvolvimento
3. **Performance**: Usar índices e limitar resultados
4. **Memory Leaks**: Fechar database em `onDestroy()`

#### Logs Úteis
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "database.db")
    .setLoggingCallback(object : RoomDatabase.QueryCallback {
        override fun onQuery(sqlQuery: QueryCallbackSqlQuery) {
            Log.d("Room", "SQL: ${sqlQuery.sql}")
        }
    })
    .build()
```

## Próximos Passos

1. **Teste as Activities de exemplo** criadas
2. **Migre uma Activity por vez** do SQLite para Room
3. **Execute a migração de dados** quando estiver pronto
4. **Remova o código SQLite antigo** gradualmente
5. **Adicione testes** para as novas funcionalidades

## Recursos Adicionais

- [Documentação oficial do Room](https://developer.android.com/training/data-storage/room)
- [Codelab do Room](https://developer.android.com/codelabs/android-room-with-a-view-kotlin)
- [Guia de migração do Room](https://developer.android.com/training/data-storage/room/migrating-db-versions) 