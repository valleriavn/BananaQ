plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        // Windows sandbox TEMP paths can exceed the Unix-domain socket limit
        // used internally by Java 21's test-worker connection.
        if (System.getProperty("os.name").startsWith("Windows")) {
            val ipcDirectory = rootProject.layout.buildDirectory.dir("ipc").get().asFile
            systemProperty("jdk.net.unixdomain.tmpdir", ipcDirectory.absolutePath)
            doFirst { check(ipcDirectory.isDirectory || ipcDirectory.mkdirs()) }
        }
    }
}
