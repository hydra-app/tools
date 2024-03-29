
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

tasks {
    register("copyLogo", Copy::class) {
        from ("${rootProject.projectDir}") {
            include("logo-icon.svg")
        }
        into ("${rootProject.projectDir}/docs/images")
    }
}

afterEvaluate {
    tasks.getByName("dokkaHtmlMultiModule") {
        finalizedBy("copyLogo")
    }
}

tasks.dokkaHtmlMultiModule.configure {
    moduleName.set("Hydra tools")
    outputDirectory.set(file("$rootDir/docs"))
    includes.from("README.md")
}