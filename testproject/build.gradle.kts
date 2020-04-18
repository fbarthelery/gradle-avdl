plugins {
    id("com.geekorum.gradle.avdl.greeting") version "1.0-SNAPSHOT"
}

avdl {
    devices {
        create("first")
        register("second")
    }
}
