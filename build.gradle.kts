import com.geekorum.build.SourceLicenseCheckerPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70" apply false
    kotlin("plugin.serialization") version "1.3.70" apply false
    `maven-publish`
}


allprojects {
    group = "com.geekorum.gradle.avdl"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
    }

    apply<SourceLicenseCheckerPlugin>()

    plugins.withType<MavenPublishPlugin> {
        publishing {
            repositories {
                maven {
                    this.name = "local"
                    this.url = uri("$rootDir/repo")
                }
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}
