package com.example.myapplication

import android.provider.BaseColumns

// Define o contrato para a tabela de artigos no banco de dados
object ArtigoContract {
    // Objeto interno que herda BaseColumns para incluir _ID
    object ArtigoEntry : BaseColumns {
        // Define o nome da tabela de artigos
        const val TABLE_NAME = "artigos"
        // Define a coluna para o nome do artigo
        const val COLUMN_NAME_NOME = "nome"
        // Define a coluna para o preço do artigo
        const val COLUMN_NAME_PRECO = "preco"
        // Define a coluna para a quantidade do artigo
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        // Define a coluna para o desconto do artigo
        const val COLUMN_NAME_DESCONTO = "desconto"
        // Define a coluna para a descrição do artigo
        const val COLUMN_NAME_DESCRICAO = "descricao"
        // Define a coluna para indicar se deve guardar na fatura
        const val COLUMN_NAME_GUARDAR_FATURA = "guardar_fatura"
        // Define a coluna para o número serial do artigo
        const val COLUMN_NAME_NUMERO_SERIAL = "numero_serial"

        // Define o comando SQL para criar a tabela de artigos
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_NOME TEXT,
                $COLUMN_NAME_PRECO REAL,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_DESCONTO REAL,
                $COLUMN_NAME_DESCRICAO TEXT,
                $COLUMN_NAME_GUARDAR_FATURA INTEGER,
                $COLUMN_NAME_NUMERO_SERIAL TEXT
            )
        """

        // Define o comando SQL para excluir a tabela de artigos
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
} 