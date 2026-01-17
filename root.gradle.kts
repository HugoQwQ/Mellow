plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("org.polyfrost.multi-version.root")
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("com.diffplug.spotless") version "8.1.0"
}

repositories { mavenCentral() }

preprocess {
    "1.12.2-forge"(11202, "srg") {
        "1.8.9-forge"(10809, "srg")
    }
}

spotless {
    java {
        target(fileTree(rootProject.projectDir) {
            include("src/main/java/**/*.java")
            exclude("**/build/generated/**")
        })

        importOrder()
        removeUnusedImports()
        googleJavaFormat("1.17.0").aosp()
        formatAnnotations()
    }
}

