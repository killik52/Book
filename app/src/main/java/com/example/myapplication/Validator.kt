package com.example.myapplication

/**
 * Classe para centralizar validações de dados
 */
object Validator {
    
    /**
     * Valida dados de cliente
     */
    fun validateCliente(
        nome: String,
        email: String? = null,
        telefone: String? = null,
        cpf: String? = null,
        cnpj: String? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validação do nome
        if (nome.isBlank()) {
            errors.add("Nome é obrigatório")
        } else if (!nome.isValidName()) {
            errors.add("Nome deve ter entre ${Constants.Validation.MIN_NAME_LENGTH} e ${Constants.Validation.MAX_NAME_LENGTH} caracteres")
        }
        
        // Validação do email (opcional)
        email?.let {
            if (it.isNotBlank() && !it.isValidEmail()) {
                errors.add("Email inválido")
            }
        }
        
        // Validação do telefone (opcional)
        telefone?.let {
            if (it.isNotBlank() && !it.isValidPhone()) {
                errors.add("Telefone inválido")
            }
        }
        
        // Validação do CPF (opcional)
        cpf?.let {
            if (it.isNotBlank() && !it.isValidCPF()) {
                errors.add("CPF inválido")
            }
        }
        
        // Validação do CNPJ (opcional)
        cnpj?.let {
            if (it.isNotBlank() && !it.isValidCNPJ()) {
                errors.add("CNPJ inválido")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Valida dados de fatura
     */
    fun validateFatura(
        numeroFatura: String,
        cliente: String,
        artigos: String? = null,
        subtotal: Double? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validação do número da fatura
        if (numeroFatura.isBlank()) {
            errors.add("Número da fatura é obrigatório")
        }
        
        // Validação do cliente
        if (cliente.isBlank()) {
            errors.add("Cliente é obrigatório")
        }
        
        // Validação dos artigos (opcional)
        artigos?.let {
            if (it.isBlank()) {
                errors.add("Pelo menos um artigo deve ser adicionado")
            }
        }
        
        // Validação do subtotal (opcional)
        subtotal?.let {
            if (it < 0) {
                errors.add("Subtotal não pode ser negativo")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Valida dados de artigo
     */
    fun validateArtigo(
        nome: String,
        preco: Double? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validação do nome
        if (nome.isBlank()) {
            errors.add("Nome do artigo é obrigatório")
        }
        
        // Validação do preço (opcional)
        preco?.let {
            if (it < 0) {
                errors.add("Preço não pode ser negativo")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Valida endereço
     */
    fun validateEndereco(
        logradouro: String? = null,
        numero: String? = null,
        bairro: String? = null,
        municipio: String? = null,
        uf: String? = null,
        cep: String? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validação do CEP (opcional)
        cep?.let {
            if (it.isNotBlank()) {
                val cepLimpo = it.replace("[^0-9]".toRegex(), "")
                if (cepLimpo.length != 8) {
                    errors.add("CEP deve ter 8 dígitos")
                }
            }
        }
        
        // Validação da UF (opcional)
        uf?.let {
            if (it.isNotBlank() && it.length != 2) {
                errors.add("UF deve ter 2 caracteres")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    /**
     * Valida código de barras
     */
    fun validateBarcode(barcode: String): ValidationResult {
        return if (barcode.isBlank()) {
            ValidationResult.Error(listOf("Código de barras não pode estar vazio"))
        } else {
            ValidationResult.Success
        }
    }
    
    /**
     * Valida data
     */
    fun validateDate(date: String, format: String = Constants.DateFormats.INPUT_FORMAT): ValidationResult {
        return try {
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(date)
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error(listOf("Data inválida"))
        }
    }
}

/**
 * Resultado de uma validação
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getFirstError(): String? {
        return when (this) {
            is Success -> null
            is Error -> errors.firstOrNull()
        }
    }
} 