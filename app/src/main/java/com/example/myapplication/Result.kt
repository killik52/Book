package com.example.myapplication

/**
 * Wrapper para resultados de operações que podem falhar
 * Similar ao Result do Kotlin, mas com funcionalidades adicionais
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    /**
     * Executa uma função se o resultado for sucesso
     */
    fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Executa uma função se o resultado for erro
     */
    fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) {
            action(exception)
        }
        return this
    }

    /**
     * Executa uma função se o resultado for loading
     */
    fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    /**
     * Transforma o resultado usando uma função
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    /**
     * Obtém o valor se for sucesso, ou null caso contrário
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }

    /**
     * Verifica se é sucesso
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Verifica se é erro
     */
    fun isError(): Boolean = this is Error

    /**
     * Verifica se está carregando
     */
    fun isLoading(): Boolean = this is Loading
}

/**
 * Extension para criar Result de operações que podem lançar exceções
 */
inline fun <T> runCatching(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
} 