plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.myapplication" // Este é o namespace que define o pacote do BuildConfig
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" //
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false // Desabilitado para evitar conflitos
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Configurações específicas para debug (opcional, mas bom para desenvolvimento)
            applicationIdSuffix = ".debug" // Sufixo para diferenciar o ID do app em debug
            isDebuggable = true

            // Para debug, você pode querer desabilitar a minificação para facilitar a depuração
            // isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true // Habilita o ViewBinding
        buildConfig = true // Garante que BuildConfig.APPLICATION_ID esteja disponível
        dataBinding = false // Desabilitado temporariamente para resolver conflito de versão
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    lint {
        abortOnError = false
    }
}

// Forçar resolução de versão do Kotlin para 1.9.23
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.23")
        }
        // Forçar versão compatível do databinding
        if (requested.group == "androidx.databinding" && requested.name == "databinding-ktx") {
            useVersion("8.9.2")
        }
    }
}

dependencies {
    // Dependências do AndroidX e Material Design
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.activity)

    // Dependência Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit e Gson para APIs Web (MANTER PARA A FUNCIONALIDADE DE CNPJ)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // logging-interceptor apenas para builds de debug (MANTER PARA A FUNCIONALIDADE DE CNPJ)
    debugImplementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Outras dependências do seu projeto
    implementation("com.google.mlkit:text-recognition:16.0.0") // Para OCR
    // implementation("io.getstream:photoview:1.0.3") // REMOVIDO - compilado com Kotlin 2.x
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0") //
    implementation("com.google.zxing:core:3.5.3") // Para geração/leitura de códigos de barras
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") // Para scanner de código de barras embutido

    // ViewPager2 (Se ainda for usado em alguma parte do app)
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Glide (REMOVA OU COMENTE AS DUAS LINHAS ABAIXO SE NÃO ESTIVER MAIS USANDO GLIDE EM NENHUM OUTRO LUGAR)
    // implementation("com.github.bumptech.glide:glide:4.12.0")
    // annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    // Coil para lazy loading de imagens
    implementation("io.coil-kt:coil:2.4.0")

    // SwipeRefreshLayout para pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // >>>>> ADIÇÃO PARA GRÁFICOS: MPAndroidChart <<<<<
    // Adicione esta linha para a biblioteca de gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}