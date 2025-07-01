plugins {
    alias(libs.plugins.android.library)  // Asegúrate de que sea 'android.library'
    id("maven-publish")
}

android {
    namespace = "com.jef.sqlite.management"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Configuración de la publicación en Maven
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["androidLibrary"])

                groupId = "com.github.ferneyholguin"
                artifactId = "sqlite_management"
                version = "1.0.7"

                // Agregar metadatos POM para JitPack
                pom {
                    name.set("SQLite Management")
                    description.set("Android SQLite management library")
                    url.set("https://github.com/ferneyholguin/sqlite_management")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("ferneyholguin")
                            name.set("Ferney Holguin")
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
