import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    application
    id("org.springframework.boot") version "4.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.google.code.gson:gson:2.14.0")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
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

// application.mainClass is the console app, so the web app's main class is set on the Spring Boot tasks
tasks.bootJar {
    mainClass = "patients.web.WebApp"
}

tasks.bootRun {
    mainClass = "patients.web.WebApp"
    workingDir = rootDir
}

tasks.register<JavaExec>("migrateToFirebase") {
    group = "application"
    description = "Copies users.json and patients.json to the Firebase database at ZORG_FIREBASE_URL"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "patients.MigrateToFirebase"
    workingDir = rootDir
}

tasks.register<JavaExec>("generateFirebaseRules") {
    group = "application"
    description = "Writes database.rules.json from ProfessionPermissions and FieldPermissions"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "patients.FirebaseRules"
    workingDir = rootDir
}

tasks.test {
    useJUnitPlatform {
        excludeTags("emulator")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<Test>("emulatorTest") {
    group = "verification"
    description = "Runs the tests tagged 'emulator' against the Firebase emulators, see the README"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("emulator")
    }
    outputs.upToDateWhen { false }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
