import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4")
    implementation("org.eclipse.jgit:org.eclipse.jgit:3.5.0.201409260305-r")
}

val bootJar: BootJar by tasks
bootJar.enabled = true