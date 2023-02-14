import kr.entree.spigradle.kotlin.*

plugins {
    kotlin("jvm") version "1.7.21"
    id("kr.entree.spigradle") version "2.4.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "kr.kua"
version = "1.0.1-SNAPSHOT"

tasks.compileJava.get().options.encoding = "UTF-8"

repositories {
    protocolLib()
//    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
//    maven("https://oss.jfrog.org/oss-snapshot-local/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(spigotAll("1.16.5"))
//    compileOnly("br.com.devsrsouza.kotlinbukkitapi:core:0.2.0-SNAPSHOT") // core
}

spigot {
    description = "우왁굳 시참 컨텐츠 플러그인"
    depends = listOf("ProtocolLib")
    authors = listOf("Kua_")
    apiVersion = "1.16"
    load = kr.entree.spigradle.data.Load.STARTUP

    commands {
        create("wak") {
            aliases = listOf("giv", "i")
            description = "A give command."
            permission = "wakgood.give"
            permissionMessage = "You do not have the permission!"
        }
    }

    permissions {
        create("wakgood.give") {
            description = "Allows give command"
            defaults = "true"
        }
        create("wakgood.*") {
            description = "Wildcard permission"
            defaults = "op"
            children = mapOf("test.foo" to true)
        }
    }

}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    if (JavaVersion.current() < JavaVersion.VERSION_11) {
        throw GradleException("Java 11 or higher is required to build this project.")
    }
}

val shade = configurations.create("shade")
shade.extendsFrom(configurations.implementation.get())

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        manifest {
            attributes("Main-Class': 'kr.kua.Main")
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//        from(
//            shade.map {
//                if (it.isDirectory)
//                    it
//                else
//                    zipTree(it)
//            }
//        )
    }
}