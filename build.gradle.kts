plugins {
    java
    kotlin("jvm") version "1.5.30"
    `java-library`
    `maven-publish`
}
val baseVersion = "0.1.0.0"
val channel = "DEV"
val buildNum = 22

group = "com.IceCreamQAQ"
version = "$baseVersion-$channel$buildNum"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.icecreamqaq.com/repository/maven-public/")
}

dependencies {
    api(kotlin("stdlib"))
    api("com.IceCreamQAQ:Yu-Core:0.2.0.0-DEV15")
}

tasks {
    withType<JavaCompile> {

    }
}

publishing {

    publications {
        create<MavenPublication>("YuQ") {
            groupId = group.toString()
            artifactId = name
            version = project.version.toString()

            pom {
                name.set("YuQ QQ Robot Framework")
                description.set("YuQ QQ Robot Framework")
                url.set("https://github.com/YuQWorks/YuQ")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("IceCream")
                        name.set("IceCream")
                        email.set("www@withdata.net")
                    }
                }
                scm {
                    connection.set("")
                }
            }
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven {
            val snapshotsRepoUrl = "https://maven.icecreamqaq.com/repository/maven-snapshots/"
            val releasesRepoUrl = "https://maven.icecreamqaq.com/repository/maven-releases/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)


            credentials {

                val mvnInfo = readMavenUserInfo("IceCream")
                username = mvnInfo[0]
                password = mvnInfo[1]
            }
        }
    }

}
fun readMavenUserInfo(id: String) =
    fileOr(
        "mavenInfo.txt",
        "${System.getProperty("user.home")}/.m2/mvnInfo-$id.txt"
    )?.readText()?.split("|") ?: arrayListOf("", "")


fun File.check() = if (this.exists()) this else null
fun fileOr(vararg file: String): File? {
    for (s in file) {
        val f = file(s)
        if (f.exists()) return f
    }
    return null
}