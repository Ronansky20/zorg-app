plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    // keep the "JDK 17 or newer" promise from the README, whatever JDK runs Gradle
    options.release = 17
}

application {
    mainClass = "patients.App"
}

tasks.named<JavaExec>("run") {
    // the console UI reads from stdin, and patients.json is resolved against the working directory
    standardInput = System.`in`
    workingDir = rootDir
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
