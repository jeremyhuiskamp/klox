plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "ca.kamper"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "ca.kamper.klox.LoxKt"
}

// https://blog.thecodewhisperer.com/permalink/stdin-gradle-kotlin-dsl
tasks.run<JavaExec> {
    standardInput = System.`in`
}