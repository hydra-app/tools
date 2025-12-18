plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("maven-publish")
}

android {
    namespace = "knf.hydra.tools.core.ktx"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:${rootProject.extra["dokka_version"]}")
}

dokka {
    dokkaSourceSets.main {
        includes.from("README.md")
        enableAndroidDocumentationLink = true
        suppressGeneratedFiles = true
        sourceLink {
            localDirectory.set(file("$rootDir/libs/core/core-ktx/src"))
            remoteUrl("https://github.com/hydra-app/tools/tree/main/libs/core/core-ktx/src")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        outputDirectory.set(file("$rootDir/libs/core/core-ktx/docs"))
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaGeneratePublicationJavadoc)
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "knf.hydra.tools"
            artifactId = "core-ktx"
            version = rootProject.extra["lib_version"] as String?
            afterEvaluate {
                from(components["release"])
                artifact(tasks.named("dokkaHtmlJar"))
                artifact(tasks.named("dokkaJavadocJar"))
            }
            pom {
                name.set("Hydra Tools - Core")
                description.set("Convenient Kotlin extensions")
                url.set("https://github.com/hydra-app/tools/tree/main/libs/core/core-ktx")
                licenses {
                    license {
                        name.set("GPL-3.0 license")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                developers {
                    developer {
                        id.set("unbarredstream")
                        name.set("KNF APPS")
                        email.set("knf.apps@gmail.com")
                    }
                }
            }
        }
    }
}
