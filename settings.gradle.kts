pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositório para MPAndroidChart
        maven { url = uri("https://jitpack.io") } // <<< CORREÇÃO AQUI
    }
}
rootProject.name = "BookV6" // Certifique-se que o nome do seu projeto está correto aqui
include(":app")