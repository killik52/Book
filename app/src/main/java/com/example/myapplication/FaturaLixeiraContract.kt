package com.example.myapplication

import android.provider.BaseColumns

// Define o contrato para a tabela de faturas na lixeira no banco de dados
object FaturaLixeiraContract {
    // Objeto interno que herda BaseColumns para incluir _ID
    object FaturaLixeiraEntry : BaseColumns {
        // Define o nome da tabela de faturas na lixeira
        const val TABLE_NAME = "fatura_lixeira"
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

        // Define o comando SQL para criar a tabela de faturas na lixeira
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

        // Define o comando SQL para excluir a tabela de faturas na lixeira
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
} 