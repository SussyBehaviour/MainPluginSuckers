import groovy.json.JsonSlurper

plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    val json = JsonSlurper().parseText(file("src/main/resources/plugin.json").readText()) as Map<*, *>
    project.version = json["version"]!!
    val mindustryVersion = json["minGameVersion"]
    compileOnly("com.github.anuken.arc:arc-core:v$mindustryVersion")
    compileOnly("com.github.anuken.mindustry:core:v$mindustryVersion") {
        exclude(group = "com.github.Anuken.Arc")
    }
    compileOnly("com.github.anuken.mindustry:server:v$mindustryVersion") {
        exclude(group = "com.github.Anuken.Arc")
    }
    implementation("dev.morphia.morphia:morphia-core:2.3.8")
    implementation("com.discord4j:discord4j-core:3.2.6")
    runtimeOnly("io.netty:netty-transport-native-epoll::linux-aarch_64")
    implementation("org.jline:jline-reader:3.21.0")
    implementation("org.jline:jline-console:3.21.0")
    implementation("org.jline:jline-terminal-jna:3.21.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.4.Final")
    compileOnly("com.github.anuken.arc:arc-core:v146")
    compileOnly("com.github.anuken.arc:arcnet:v146")
  implementation("net.time4j:time4j-base:5.9.1")
    implementation("com.alibaba:fastjson:2.0.40")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("com.alibaba:fastjson:2.0.40")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}