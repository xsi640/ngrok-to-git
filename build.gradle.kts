import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.4.9"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("java")
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.spring") version "1.5.21"
    kotlin("plugin.jpa") version "1.5.21"
}

allprojects {

    apply {
        plugin("idea")
        plugin("java")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("kotlin")
        plugin("kotlin-spring")
        plugin("kotlin-jpa")
        plugin("kotlin-allopen")
        plugin("kotlin-noarg")
    }

    group = "com.github.xsi640"
    version = "1.0.0"
    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8


    val vers = mapOf(
        "commons_io" to "2.8.0",
    )

    rootProject.extra.set("vers", vers)
    rootProject.extra.set("docker_registry", "192.168.1.254:5000")
    rootProject.extra.set("docker_host", "192.168.1.254:2376")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-aop")
        implementation("org.springframework.data:spring-data-commons")
        implementation("org.springframework.boot:spring-boot-configuration-processor")
        implementation("org.springframework.boot:spring-boot-autoconfigure")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
        implementation("org.slf4j:slf4j-ext")
        implementation("commons-io:commons-io:${vers["commons_io"]}")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        implementation("org.jetbrains.kotlin:kotlin-allopen")
    }

    val user = System.getProperty("repoUser")
    val pwd = System.getProperty("repoPassword")

    repositories {
        mavenLocal()
        maven {
            credentials {
                username = user
                password = pwd
                isAllowInsecureProtocol = true
            }
//            url = uri("http://172.16.11.231:8081/nexus/repository/maven2-group/")
            url = uri("http://192.168.1.254:8081/repository/maven-group/")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    val jar: Jar by tasks
    jar.enabled = true
    val bootJar: BootJar by tasks
    bootJar.enabled = false
}

apply(from = rootProject.file("buildscript/root.gradle.kts"))