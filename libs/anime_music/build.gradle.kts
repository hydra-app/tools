plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("maven-publish")
}

android {
    namespace = "knf.hydra.tools.anime_music"
    compileSdk = 34

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
        singleVariant("release")
    }
}

dependencies {
    api("com.github.hydra-app:core:${rootProject.extra["core_version"]}")
    api(project(":libs:core:anime"))
    api(project(":libs:core:core-ktx"))
    dokkaPlugin("org.jetbrains.dokka:android-documentation-plugin:${rootProject.extra["dokka_version"]}")
}

dokka {
    dokkaSourceSets.main {
        includes.from("README.md")
        enableAndroidDocumentationLink = false
        suppressGeneratedFiles = true
        sourceLink {
            localDirectory.set(file("$rootDir/libs/anime_music/src"))
            remoteUrl("https://github.com/hydra-app/tools/tree/main/libs/anime_music/src")
            remoteLineSuffix.set("#L")
        }
    }
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        outputDirectory.set(file("$rootDir/libs/anime_music/docs"))
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

afterEvaluate {
    publishing {
        repositories {
            maven {
                url = uri(layout.buildDirectory.dir("repository"))
            }
        }
        publications {
            create<MavenPublication>("release") {
                groupId = "knf.hydra.tools"
                artifactId = "anime_music"
                version = rootProject.extra["lib_version"] as String?
                artifact(tasks.named("androidSourcesJar"))
                artifact(tasks.named("dokkaHtmlJar"))
                artifact(tasks.named("dokkaJavadocJar"))
            }
        }
    }
}
