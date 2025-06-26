#!/usr/bin/env kotlin

/**
 * Script para verificar dependências desatualizadas no projeto BOOKV
 * 
 * Uso: kotlin check-dependencies.kt
 * 
 * Este script analisa o build.gradle.kts e verifica se há dependências
 * que precisam ser atualizadas para manter a compatibilidade.
 */

import java.io.File
import java.net.URL
import java.net.HttpURLConnection

// Configurações do projeto
val projectDir = File(".")
val buildGradleFile = File(projectDir, "app/build.gradle.kts")

// Dependências críticas para monitorar
val criticalDependencies = mapOf(
    "androidx.room:room-runtime" to "2.6.1",
    "androidx.lifecycle:lifecycle-viewmodel-ktx" to "2.8.1",
    "androidx.appcompat:appcompat" to "1.7.0",
    "com.google.android.material:material" to "1.12.0",
    "com.squareup.retrofit2:retrofit" to "2.9.0",
    "com.google.mlkit:text-recognition" to "16.0.0",
    "com.journeyapps:zxing-android-embedded" to "4.3.0"
)

// Versões mínimas recomendadas
val recommendedVersions = mapOf(
    "androidx.room:room-runtime" to "2.6.1",
    "androidx.lifecycle:lifecycle-viewmodel-ktx" to "2.8.1",
    "androidx.appcompat:appcompat" to "1.7.0",
    "com.google.android.material:material" to "1.12.0",
    "com.squareup.retrofit2:retrofit" to "2.9.0",
    "com.google.mlkit:text-recognition" to "16.0.0",
    "com.journeyapps:zxing-android-embedded" to "4.3.0"
)

fun main() {
    println("🔍 Verificando dependências do projeto BOOKV...")
    println("=" * 50)
    
    if (!buildGradleFile.exists()) {
        println("❌ Arquivo build.gradle.kts não encontrado!")
        return
    }
    
    val content = buildGradleFile.readText()
    
    // Verificar configurações do Android
    checkAndroidConfig(content)
    
    // Verificar dependências
    checkDependencies(content)
    
    // Verificar configurações de compilação
    checkCompilationConfig(content)
    
    println("\n" + "=" * 50)
    println("✅ Verificação concluída!")
    println("\n📋 Próximos passos recomendados:")
    println("1. Atualizar dependências marcadas como ⚠️")
    println("2. Testar o app após atualizações")
    println("3. Verificar compatibilidade com diferentes versões do Android")
}

fun checkAndroidConfig(content: String) {
    println("\n📱 Configurações do Android:")
    
    val compileSdkMatch = Regex("compileSdk\\s*=\\s*(\\d+)").find(content)
    val minSdkMatch = Regex("minSdk\\s*=\\s*(\\d+)").find(content)
    val targetSdkMatch = Regex("targetSdk\\s*=\\s*(\\d+)").find(content)
    
    val compileSdk = compileSdkMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val minSdk = minSdkMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val targetSdk = targetSdkMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    
    println("   compileSdk: $compileSdk ${if (compileSdk >= 34) "✅" else "⚠️"}")
    println("   minSdk: $minSdk ${if (minSdk >= 24) "✅" else "⚠️"}")
    println("   targetSdk: $targetSdk ${if (targetSdk >= 34) "✅" else "⚠️"}")
    
    if (compileSdk < 34) {
        println("   ⚠️  Recomendado: Atualizar compileSdk para 34+")
    }
    if (targetSdk < 34) {
        println("   ⚠️  Recomendado: Atualizar targetSdk para 34+")
    }
}

fun checkDependencies(content: String) {
    println("\n📦 Dependências Críticas:")
    
    criticalDependencies.forEach { (dependency, currentVersion) ->
        val pattern = Regex("implementation\\(\"$dependency:([^\"]+)\"\\)")
        val match = pattern.find(content)
        
        if (match != null) {
            val version = match.groupValues[1]
            val status = when {
                version == currentVersion -> "✅"
                version < currentVersion -> "⚠️"
                else -> "🆕"
            }
            println("   $dependency: $version $status")
            
            if (version < currentVersion) {
                println("      ⚠️  Recomendado: Atualizar para $currentVersion")
            }
        } else {
            println("   $dependency: ❌ Não encontrado")
        }
    }
}

fun checkCompilationConfig(content: String) {
    println("\n⚙️  Configurações de Compilação:")
    
    val javaVersion = Regex("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)").find(content)
    val kotlinVersion = Regex("kotlin\\s*=\\s*\"([^\"]+)\"").find(content)
    
    val javaVer = javaVersion?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val kotlinVer = kotlinVersion?.groupValues?.get(1) ?: "Não encontrado"
    
    println("   Java Version: $javaVer ${if (javaVer >= 17) "✅" else "⚠️"}")
    println("   Kotlin Version: $kotlinVer ${if (kotlinVer >= "1.9.0") "✅" else "⚠️"}")
    
    if (javaVer < 17) {
        println("   ⚠️  Recomendado: Atualizar para Java 17+")
    }
    if (kotlinVer < "1.9.0") {
        println("   ⚠️  Recomendado: Atualizar para Kotlin 1.9.0+")
    }
}

// Função auxiliar para repetir strings
operator fun String.times(n: Int): String = repeat(n) 