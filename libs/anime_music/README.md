# Module anime_music

Anime music searcher for Hydra modules

## Usage

[![](https://jitpack.io/v/hydra-app/tools.svg)](https://jitpack.io/#hydra-app/tools)
To depend on anime_music you will need add the jitpack.io repository in the project build.gradle/settings.gradle file:

build.gradle
```groovy
allprojects {
    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
        // ...
    }
}
```

settings.gradle
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
        // ...
    }
}
```

And then add the anime_music dependency to your module's dependencies scope:
```groovy
dependencies {
    implementation 'com.github.hydra-app.tools:anime_music:$current-version'
}
```