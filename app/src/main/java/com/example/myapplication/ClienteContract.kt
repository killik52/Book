package com.example.myapplication

import android.provider.BaseColumns

// Define o contrato para a tabela de clientes no banco de dados
object ClienteContract {
    // Objeto interno que herda BaseColumns para incluir _ID
    object ClienteEntry : BaseColumns {
        // Define o nome da tabela de clientes
        const val TABLE_NAME = "clientes"
        // Define a coluna para o nome do cliente
        const val COLUMN_NAME_NOME = "nome"
        // Define a coluna para o email do cliente
        const val COLUMN_NAME_EMAIL = "email"
        // Define a coluna para o telefone do cliente
        const val COLUMN_NAME_TELEFONE = "telefone"
        // Define a coluna para informações adicionais do cliente
        const val COLUMN_NAME_INFORMACOES_ADICIONAIS = "informacoes_adicionais"
        // Define a coluna para o CPF do cliente
        const val COLUMN_NAME_CPF = "cpf"
        // Define a coluna para o CNPJ do cliente
        const val COLUMN_NAME_CNPJ = "cnpj"
        // Define a coluna para o logradouro do endereço
        const val COLUMN_NAME_LOGRADOURO = "logradouro"
        // Define a coluna para o número do endereço
        const val COLUMN_NAME_NUMERO = "numero"
        // Define a coluna para o complemento do endereço
        const val COLUMN_NAME_COMPLEMENTO = "complemento"
        // Define a coluna para o bairro do endereço
        const val COLUMN_NAME_BAIRRO = "bairro"
        // Define a coluna para o município do endereço
        const val COLUMN_NAME_MUNICIPIO = "municipio"
        // Define a coluna para a UF (estado) do endereço
        const val COLUMN_NAME_UF = "uf"
        // Define a coluna para o CEP do endereço
        const val COLUMN_NAME_CEP = "cep"
        // Define a coluna para o(s) número(s) serial(is) do cliente
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"

        // Define o comando SQL para criar a tabela de clientes
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
                $COLUMN_NAME_EMAIL TEXT,
                $COLUMN_NAME_TELEFONE TEXT,
                $COLUMN_NAME_INFORMACOES_ADICIONAIS TEXT,
                $COLUMN_NAME_CPF TEXT,
                $COLUMN_NAME_CNPJ TEXT,
                $COLUMN_NAME_LOGRADOURO TEXT,
                $COLUMN_NAME_NUMERO TEXT,
                $COLUMN_NAME_COMPLEMENTO TEXT,
                $COLUMN_NAME_BAIRRO TEXT,
                $COLUMN_NAME_MUNICIPIO TEXT,
                $COLUMN_NAME_UF TEXT,
                $COLUMN_NAME_CEP TEXT,
                $COLUMN_NAME_NUMERO_SERIAL TEXT
            )
        """

        // Define o comando SQL para excluir a tabela de clientes
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
} 