import sc.gradle.ScriptsTask

plugins {
    java
    application
}

sourceSets.main {
    java.srcDir("src")
}

application {
    mainClass.set("sc.TestClient")
}

dependencies {
    // TODO this is only here to access some default server Configuration, move that to SDK or smth
    implementation(project(":server"))
    runtimeOnly(project(":plugin"))
}

tasks {
    val createStartScripts by creating(ScriptsTask::class) {
        destinationDir = jar.get().destinationDirectory.get().asFile
        fileName = "start-tests"
        content = "java -Dfile.encoding=UTF-8 -Dlogback.configurationFile=logback-tests.xml -jar test-client.jar"
    }
    
    val copyLogbackConfig by creating(Copy::class) {
        from("src/logback-tests.xml")
        into(jar.get().destinationDirectory)
    }
    
    jar {
        dependsOn(createStartScripts, copyLogbackConfig)
        doFirst {
            manifest.attributes(
                    "Class-Path" to configurations.default.get()
                            .map { "lib/" + it.name }
                            .plus("server.jar")
                            .joinToString(" ")
            )
        }
    }
    
    run.configure {
        dependsOn(":player:shadowJar", ":server:makeRunnable")
        doFirst {
            setArgsString(System.getProperty("args") ?: run {
                val playerLocation = project(":player").tasks.getByName<Jar>("shadowJar").archiveFile.get()
                "--start-server --tests 3 --player1 $playerLocation --player2 $playerLocation"
            })
            @Suppress("UNNECESSARY_SAFE_CALL", "SimplifyBooleanWithConstants")
            if(args?.isEmpty() == false)
                println("Using command-line arguments: $args")
        }
    }
}
