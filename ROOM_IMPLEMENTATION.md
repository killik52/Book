# Implementação do Room Database

Este documento descreve a implementação do Room Database no projeto BOOKV.

## Estrutura Implementada

### 1. Dependências Adicionadas

As seguintes dependências foram adicionadas ao `app/build.gradle.kts`:

```kotlin
plugins {
    id("kotlin-kapt") // Para Room
}

dependencies {
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}
```

### 2. Entidades (Entities)

Foram criadas as seguintes entidades no pacote `database.entities`:

- **Cliente**: Representa os clientes do sistema
- **Artigo**: Representa os produtos/artigos
- **Fatura**: Representa as faturas principais
- **FaturaItem**: Representa os itens de uma fatura
- **FaturaNota**: Representa as notas de uma fatura
- **FaturaFoto**: Representa as fotos de uma fatura
- **ClienteBloqueado**: Representa clientes bloqueados
- **FaturaLixeira**: Representa faturas na lixeira

### 3. DAOs (Data Access Objects)

Foram criados DAOs para cada entidade no pacote `database.dao`:

- `ClienteDao`
- `ArtigoDao`
- `FaturaDao`
- `FaturaItemDao`
- `FaturaNotaDao`
- `FaturaFotoDao`
- `ClienteBloqueadoDao`
- `FaturaLixeiraDao`

### 4. Banco de Dados

O banco de dados principal está em `database.AppDatabase.kt` com:
- Configuração de todas as entidades
- Singleton pattern para instância única
- Configuração de migrações

### 5. Repositórios

Repositórios foram criados no pacote `database.repository` para centralizar as operações:
- `ClienteRepository`
- `ArtigoRepository`
- `FaturaRepository`

## Como Usar

### 1. Inicializar o Banco de Dados

```kotlin
class MinhaActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.getDatabase(this)
    }
}
```

### 2. Inserir Dados

```kotlin
lifecycleScope.launch {
    val novoCliente = Cliente(
        nome = "João Silva",
        email = "joao@email.com",
        telefone = "(11) 99999-9999"
        // ... outros campos
    )
    
    val clienteId = database.clienteDao().insertCliente(novoCliente)
}
```

### 3. Buscar Dados

```kotlin
// Buscar todos os clientes
database.clienteDao().getAllClientes().collect { clientes ->
    // Processar lista de clientes
}

// Buscar cliente por ID
val cliente = database.clienteDao().getClienteById(1L)

// Buscar com filtro
database.clienteDao().searchClientes("João").collect { clientes ->
    // Processar resultados
}
```

### 4. Atualizar Dados

```kotlin
lifecycleScope.launch {
    val cliente = database.clienteDao().getClienteById(1L)
    cliente?.let {
        it.email = "novo@email.com"
        database.clienteDao().updateCliente(it)
    }
}
```

### 5. Deletar Dados

```kotlin
lifecycleScope.launch {
    database.clienteDao().deleteClienteById(1L)
}
```

## Vantagens do Room

1. **Compile-time Verification**: O Room verifica as queries SQL em tempo de compilação
2. **Type Safety**: Todas as operações são type-safe
3. **Coroutines Support**: Suporte nativo a coroutines e Flow
4. **LiveData Integration**: Integração fácil com LiveData
5. **Migration Support**: Suporte a migrações de banco de dados
6. **Testing**: Fácil de testar com in-memory database

## Migração do SQLite Existente

Para migrar do SQLite existente para o Room:

1. **Backup dos dados**: Faça backup dos dados existentes
2. **Migração gradual**: Implemente o Room paralelamente ao SQLite existente
3. **Migração de dados**: Crie um script para migrar dados do SQLite para Room
4. **Testes**: Teste extensivamente antes de remover o SQLite antigo

## Exemplo de Migração de Dados

```kotlin
class DataMigrationHelper(private val context: Context) {
    fun migrateFromSQLiteToRoom() {
        val oldDbHelper = ClienteDbHelper(context)
        val oldDb = oldDbHelper.readableDatabase
        
        val roomDb = AppDatabase.getDatabase(context)
        
        lifecycleScope.launch {
            // Migrar clientes
            val cursor = oldDb.query("clientes", null, null, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val cliente = Cliente(
                        nome = it.getString(it.getColumnIndexOrThrow("nome")),
                        email = it.getString(it.getColumnIndexOrThrow("email")),
                        // ... outros campos
                    )
                    roomDb.clienteDao().insertCliente(cliente)
                }
            }
        }
    }
}
```

## Próximos Passos

1. **Implementar ViewModels**: Criar ViewModels que usem os repositórios
2. **Migração gradual**: Migrar as Activities existentes para usar Room
3. **Testes**: Implementar testes unitários para os DAOs e repositórios
4. **Otimizações**: Implementar índices e otimizações de performance
5. **Remoção do SQLite antigo**: Após migração completa, remover código SQLite antigo

## Arquivos Criados

- `app/src/main/java/database/entities/` - Todas as entidades
- `app/src/main/java/database/dao/` - Todos os DAOs
- `app/src/main/java/database/repository/` - Repositórios
- `app/src/main/java/database/AppDatabase.kt` - Banco de dados principal
- `app/src/main/java/database/Converters.kt` - Conversores de tipos
- `app/src/main/java/com/example/myapplication/RoomExampleActivity.kt` - Exemplo de uso 