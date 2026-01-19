plugins {
    kotlin("jvm") version "2.3.20-Beta1"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "pl.tenfajnybartek"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    // Używamy 1.21-R0.1-SNAPSHOT dla kompatybilności z 1.21.x
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // Adventure API jest już w Paper - używamy compileOnly
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // Database - HikariCP connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")

    // SQLite driver (domyślna baza danych)
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")

    // MySQL driver (opcjonalnie)
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    // Pakujemy tylko Kotlin stdlib i database libraries
    archiveClassifier.set("")

    // Relocate tylko Kotlin i HikariCP
    // SQLite i MySQL NIE mogą być relocate ze względu na native libraries
    relocate("kotlin", "pl.tenfajnybartek.funnymisc.libs.kotlin")
    relocate("com.zaxxer.hikari", "pl.tenfajnybartek.funnymisc.libs.hikari")

    // Merge service files
    mergeServiceFiles()

    // Exclude niepotrzebne pliki
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

