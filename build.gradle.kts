plugins {
    id("java")
}

group = "xland.ioutils"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.jar {
    from("LICENSE.txt") {
        rename { "META-INF/LICENSE_${archiveBaseName.get()}.txt" }
    }
    manifest.attributes(
        "Main-Class" to "xland.ioutils.zip1970.Main"
    )
}

java.withSourcesJar()

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}