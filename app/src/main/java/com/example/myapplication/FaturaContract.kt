package com.example.myapplication

import android.provider.BaseColumns

// Define o contrato para as tabelas de faturas no banco de dados
object FaturaContract {
    // Objeto interno que herda BaseColumns para incluir _ID
    object FaturaEntry : BaseColumns {
        // Define o nome da tabela de faturas
        const val TABLE_NAME = "faturas"
        // Define a coluna para o cliente da fatura
        const val COLUMN_NAME_CLIENTE = "cliente"
        // Define a coluna para os artigos da fatura
        const val COLUMN_NAME_ARTIGOS = "artigos"
        // Define a coluna para o subtotal da fatura
        const val COLUMN_NAME_SUBTOTAL = "subtotal"
        // Define a coluna para o desconto da fatura
        const val COLUMN_NAME_DESCONTO = "desconto"
        // Define a coluna para indicar se o desconto é percentual
        const val COLUMN_NAME_DESCONTO_PERCENT = "desconto_percent"
        // Define a coluna para a taxa de entrega
        const val COLUMN_NAME_TAXA_ENTREGA = "taxa_entrega"
        // Define a coluna para o saldo devedor
        const val COLUMN_NAME_SALDO_DEVEDOR = "saldo_devedor"
        // Define a coluna para a data da fatura
        const val COLUMN_NAME_DATA = "data"
        // Define a coluna para as notas da fatura
        const val COLUMN_NAME_NOTAS = "notas"
        // Define a coluna para o número da fatura
        const val COLUMN_NAME_NUMERO_FATURA = "numero_fatura"
        // Define a coluna para indicar se a fatura foi enviada
        const val COLUMN_NAME_FOI_ENVIADA = "foi_enviada"
        // Define a coluna para a foto da impressora
        const val COLUMN_NAME_FOTO_IMPRESSORA = "foto_impressora"

        // Define o comando SQL para criar a tabela de faturas
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_CLIENTE TEXT,
                $COLUMN_NAME_ARTIGOS TEXT,
                $COLUMN_NAME_SUBTOTAL REAL,
                $COLUMN_NAME_DESCONTO REAL,
                $COLUMN_NAME_DESCONTO_PERCENT INTEGER,
                $COLUMN_NAME_TAXA_ENTREGA REAL,
                $COLUMN_NAME_SALDO_DEVEDOR REAL,
                $COLUMN_NAME_DATA TEXT,
                $COLUMN_NAME_NOTAS TEXT,
                $COLUMN_NAME_NUMERO_FATURA TEXT,
                $COLUMN_NAME_FOI_ENVIADA INTEGER,
                $COLUMN_NAME_FOTO_IMPRESSORA TEXT
            )
        """

        // Define o comando SQL para excluir a tabela de faturas
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    // Objeto interno para a tabela de itens de fatura
    object FaturaItemEntry : BaseColumns {
        // Define o nome da tabela de itens de fatura
        const val TABLE_NAME = "fatura_items"
        // Define a coluna para o ID da fatura
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        // Define a coluna para o ID do artigo
        const val COLUMN_NAME_ARTIGO_ID = "artigo_id"
        // Define a coluna para a quantidade
        const val COLUMN_NAME_QUANTIDADE = "quantidade"
        // Define a coluna para o preço
        const val COLUMN_NAME_PRECO = "preco"
        // Define a coluna para o ID do cliente
        const val COLUMN_NAME_CLIENTE_ID = "cliente_id"

        // Define o comando SQL para criar a tabela de itens de fatura
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_ARTIGO_ID INTEGER,
                $COLUMN_NAME_QUANTIDADE INTEGER,
                $COLUMN_NAME_PRECO REAL,
                $COLUMN_NAME_CLIENTE_ID INTEGER
            )
        """

        // Define o comando SQL para excluir a tabela de itens de fatura
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    // Objeto interno para a tabela de fotos de fatura
    object FaturaFotoEntry : BaseColumns {
        // Define o nome da tabela de fotos de fatura
        const val TABLE_NAME = "fatura_fotos"
        // Define a coluna para o ID da fatura
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        // Define a coluna para o caminho da foto
        const val COLUMN_NAME_PHOTO_PATH = "photo_path"

        // Define o comando SQL para criar a tabela de fotos de fatura
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_PHOTO_PATH TEXT
            )
        """

        // Define o comando SQL para excluir a tabela de fotos de fatura
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    // Objeto interno para a tabela de notas de fatura
    object FaturaNotaEntry : BaseColumns {
        // Define o nome da tabela de notas de fatura
        const val TABLE_NAME = "fatura_notas"
        // Define a coluna para o ID da fatura
        const val COLUMN_NAME_FATURA_ID = "fatura_id"
        // Define a coluna para o texto da nota
        const val COLUMN_NAME_TEXTO = "texto"
        // Define a coluna para a data da nota
        const val COLUMN_NAME_DATA = "data"

        // Define o comando SQL para criar a tabela de notas de fatura
        const val SQL_CREATE_ENTRIES = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME_FATURA_ID INTEGER,
                $COLUMN_NAME_TEXTO TEXT,
                $COLUMN_NAME_DATA TEXT
            )
        """

        // Define o comando SQL para excluir a tabela de notas de fatura
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
} 