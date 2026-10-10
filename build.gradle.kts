plugins {
    java
    kotlin("jvm") version "2.4.0"
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
        maven("https://maven.cnb.cool/IceCream/maven/-/packages/")
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