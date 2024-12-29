plugins {
    kotlin("jvm") version "2.1.0"
    id ("org.graalvm.buildtools.native") version "0.10.3"
    id ("org.cyclonedx.bom") version "1.10.0"
    id ("io.spring.javaformat") version "0.0.43"
    id ("io.spring.nohttp") version "0.0.11"
//    checkstyle
}

//gradle.startParameter.excludedTaskNames += [ "checkFormatAot", "checkFormatAotTest" ]

group = "org.springframework.samples"
version = "3.3.0"

java {
  sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

repositories {
  mavenCentral()
}

//jte {
//  precompile()
//}
//
//jar {
//  dependsOn precompileJte
//  from fileTree("jte-classes") {
//    include "**/*.class"
//    include "**/*.bin" // Only required if you use binary templates
//  }
//}

val checkstyleVersion = "10.18.1"
val springJavaformatCheckstyleVersion = "0.0.43"
val webjarsFontawesomeVersion = "4.7.0"
val webjarsBootstrapVersion = "5.3.3"

dependencies {
  val javalinVersion = "6.4.0"
  implementation("io.javalin:javalin:$javalinVersion")
  implementation("com.zaxxer:HikariCP:5.1.0")
  implementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")
  implementation("org.glassfish.expressly:expressly:5.0.0")
  implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
  implementation("org.slf4j:slf4j-simple:2.0.16")
  implementation("gg.jte:jte:3.1.15")
  implementation("javax.cache:cache-api:1.1.1")
  implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  implementation(platform("org.jdbi:jdbi3-bom:3.47.0"))
  implementation("org.jdbi:jdbi3-core")
  implementation("org.jdbi:jdbi3-spring")
  runtimeOnly("org.webjars.npm:bootstrap:${webjarsBootstrapVersion}")
  runtimeOnly("org.webjars.npm:font-awesome:${webjarsFontawesomeVersion}")
  runtimeOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
  runtimeOnly("com.h2database:h2:2.3.232")
  runtimeOnly("com.mysql:mysql-connector-j:9.1.0")
  runtimeOnly("org.postgresql:postgresql:42.7.4")
  testImplementation(platform("org.junit:junit-bom:5.11.4"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("io.javalin:javalin-bundle:$javalinVersion")
  testImplementation("org.assertj:assertj-core:3.27.0")
  testImplementation("org.testcontainers:testcontainers:1.20.4")
  testImplementation("org.testcontainers:junit-jupiter:1.20.4")
  testImplementation("org.testcontainers:mysql:1.20.4")
  testImplementation("org.testcontainers:postgresql:1.20.4")
  testImplementation("com.microsoft.playwright:playwright:1.49.0")
  checkstyle("io.spring.javaformat:spring-javaformat-checkstyle:${springJavaformatCheckstyleVersion}")
  checkstyle("com.puppycrawl.tools:checkstyle:${checkstyleVersion}")
}

tasks.withType(Test::class.java) {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

checkstyle {
  configDirectory = project.file("src/checkstyle")
  configFile = file("src/checkstyle/nohttp-checkstyle.xml")
}

checkstyle {
  configDirectory = project.file("src/checkstyle")
  configFile = file("src/checkstyle/nohttp-checkstyle.xml")
}

tasks.named("formatMain").configure { dependsOn("checkstyleMain") }
tasks.named("formatMain").configure { dependsOn("checkstyleNohttp") }

tasks.named("formatTest").configure { dependsOn("checkstyleTest") }
tasks.named("formatTest").configure { dependsOn("checkstyleNohttp") }

//checkstyleAot.enabled = false
//checkstyleAotTest.enabled = false

//checkFormatAot.enabled = false
//checkFormatAotTest.enabled = false

//formatAot.enabled = false
//formatAotTest.enabled = false

graalvmNative {
  testSupport = false
}
