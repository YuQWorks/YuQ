plugins {
    java
    kotlin("jvm") version "1.9.10"
    `java-library`
    `maven-publish`
}
val baseVersion = "1.0.0"

group = "com.IceCreamQAQ.YuQ"
version = baseVersion

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.icecreamqaq.com/repository/maven-public/")
    }
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
        plugin("maven-publish")
    }

    dependencies {
        implementation(kotlin("stdlib"))
    }

}