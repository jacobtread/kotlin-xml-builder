plugins {
    kotlin("jvm") version "1.6.21"
    id("com.jfrog.bintray") version "1.8.4"
    `maven-publish`
}

group = "com.jacobtread"
version = "1.0.0"



repositories {
    mavenCentral()
}

val kotlinVersion: String = "1.6.21"

tasks {
    val jar by getting(Jar::class)

    register<Jar>("sourceJar") {
        from(sourceSets["main"].allSource)
        destinationDirectory.set(jar.destinationDirectory)
        archiveClassifier.set("sources")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", kotlinVersion))
    compileOnly(kotlin("reflect", kotlinVersion))
    testImplementation("junit:junit:4.12")
    testImplementation(kotlin("reflect", kotlinVersion))
    testImplementation(kotlin("test-junit", kotlinVersion))
}

artifacts {
    add("archives", tasks["sourceJar"])
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = "xml"
            from(components["kotlin"])

            artifact(tasks["sourceJar"]) {
                classifier = "sources"
            }
        }
    }
}
