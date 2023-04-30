import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("org.jetbrains.dokka")
}


subprojects {
    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            includes.from("README.md")
        }
    }
}

tasks.dokkaHtmlMultiModule.configure {
    moduleName.set("Hydra tools for modules")
    outputDirectory.set(file("$rootDir/docs"))
    includes.from("README.md")
}