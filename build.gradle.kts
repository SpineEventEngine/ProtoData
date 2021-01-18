import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

group = "io.spine.protodata"
version = "0.0.1"

subprojects {
    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(kotlin("test-junit5"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23")
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = "1.8"
    }
}
