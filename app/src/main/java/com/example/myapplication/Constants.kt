package com.example.myapplication

object Constants {
    // Request Codes
    const val SECOND_SCREEN_REQUEST_CODE = 1
    const val STORAGE_PERMISSION_CODE = 100
    const val LIXEIRA_REQUEST_CODE = 1002
    
    // Date Formats
    object DateFormats {
        const val INPUT_FORMAT = "yyyy-MM-dd HH:mm:ss"
        const val OUTPUT_FORMAT = "dd MMM yy"
        const val DISPLAY_FORMAT = "dd/MM/yyyy"
    }
    
    // Database
    object Database {
        const val OLD_DATABASE_NAME = "myapplication.db"
        const val OLD_DATABASE_VERSION = 19
    }
    
    // UI
    object UI {
        const val ANIMATION_DURATION = 250L
        const val SPACE_BETWEEN_ITEMS_DP = 4f
        const val CARD_CORNER_RADIUS = 8f
        const val CARD_ELEVATION = 2f
    }
    
    // Validation
    object Validation {
        const val MIN_NAME_LENGTH = 2
        const val MAX_NAME_LENGTH = 100
        const val MIN_EMAIL_LENGTH = 5
        const val MAX_EMAIL_LENGTH = 100
    }
    
    // Messages
    object Messages {
        const val ERROR_LOADING_DATA = "Erro ao carregar dados"
        const val ERROR_SAVING_DATA = "Erro ao salvar dados"
        const val SUCCESS_SAVED = "Dados salvos com sucesso"
        const val CONFIRM_DELETE = "Tem certeza que deseja excluir?"
        const val CONFIRM_BLOCK = "Tem certeza que deseja bloquear este cliente?"
    }
} 