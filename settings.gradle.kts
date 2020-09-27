rootProject.name = "find-pull-request"

pluginManagement {
    resolutionStrategy {
        val kotlinVersion: String by settings
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
            }
        }
    }

    repositories {
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }
}