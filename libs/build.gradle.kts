
plugins {
    id("org.jetbrains.dokka")
}


subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

tasks {
    register("copyLogo", Copy::class) {
        from ("${rootProject.projectDir}") {
            include("logo-icon.svg")
        }
        into ("${rootProject.projectDir}/docs/images")
    }
    register("publishAllToMavenLocal") {
        group = "publishing"
        description = "Publishes all modules to Maven Local"
        dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishToMavenLocal") })
    }
}

dependencies {
    dokka(project(":libs:core:core-ktx"))
    dokka(project(":libs:mal"))
    dokka(project(":libs:anime_music"))
}

afterEvaluate {
    tasks.getByName("dokkaGenerateHtml") {
        finalizedBy("copyLogo")
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("Hydra tools")
        outputDirectory.set(file("$rootDir/docs"))
        includes.from("$rootDir/libs/README.md")
    }
}
