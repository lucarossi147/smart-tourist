val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization").version("1.6.20")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}


dependencies {

    implementation("org.litote.kmongo:kmongo-id:4.6.0")

    //Cors
    implementation("io.ktor:ktor-server-cors:$ktor_version")

    // For kotling mongo impl
    implementation("org.litote.kmongo:kmongo:4.5.1")
    implementation("io.ktor:ktor-client-java-jvm:2.0.1")
    implementation("io.ktor:ktor-server-core-jvm:2.0.1")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.2")
    implementation("io.ktor:ktor-client-cio-jvm:2.0.1")

    //For Ktor testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

    //For Java engine

    //For html responses
    implementation("io.ktor:ktor-server-html-builder:$ktor_version")

    //For CIO engine

    //
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    //From ktor docs
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.0.1")
}