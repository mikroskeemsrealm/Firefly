import java.text.SimpleDateFormat
import java.util.Date
import java.util.TreeSet
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import java.security.MessageDigest

apply(plugin = "com.github.johnrengelman.shadow")

// List of dependencies which should be downloaded on runtime
val exclusions = listOf(
        "com.google.guava:guava",
        "com.google.guava:failureaccess",
        "com.google.guava:listenablefuture",
        "com.google.code.findbugs:jsr305",
        "com.google.errorprone:error_prone_annotations",
        "com.google.j2objc:j2objc-annotations",
        "org.codehaus.mojo:animal-sniffer-annotations",
        "mysql:mysql-connector-java",
        "com.google.protobuf:protobuf-java",
        "net.sf.trove4j:core",
        "net.sf.jopt-simple:jopt-simple",
        "org.yaml:snakeyaml",
        "com.google.code.gson:gson",
        "io.netty:.*",
        "com.flowpowered:flow-nbt",
        "org.slf4j:slf4j-api",
        "org.apache.logging.log4j:log4j-iostreams",
        "org.apache.logging.log4j:log4j-jul",
        "org.apache.logging.log4j:log4j-slf4j-impl",
        "net.kyori:.*",
        "org.checkerframework:checker-qual",
        "org.ow2.asm:asm",
        "io.prometheus:.*",
        "eu.mikroskeem:jvm-hiccup"
).map { it to it.toRegex() }

val prometheusVersion = "0.6.0"

dependencies {
    implementation("io.prometheus:simpleclient:${prometheusVersion}")
    implementation("io.prometheus:simpleclient_hotspot:${prometheusVersion}")
    implementation("io.prometheus:simpleclient_httpserver:${prometheusVersion}")
    implementation("eu.mikroskeem:jvm-hiccup:1.0.0")

    // Updated dependencies
    runtimeOnly("mysql:mysql-connector-java:8.0.18")
}

val collectDependencies by tasks.creating {
    val outputFile = File(temporaryDir, "deps.txt")
    outputs.file(outputFile)

    doLast {
        val sha1Digest = MessageDigest.getInstance("SHA-1")
        val deps = HashSet<String>()

        val artifactProcessor: (Configuration) -> Unit = { config ->
            config.resolvedConfiguration.resolvedArtifacts.forEach dependencies@ { dep ->
                if (dep.type != "jar") {
                    return@dependencies
                }

                val depString = dep.id.componentIdentifier.toString()
                if (depString.startsWith("project ")) {
                    return@dependencies
                }

                // Split artifact identifier and go through exclusions
                val (groupId, artifactId, version) = depString.split(':').run { if (size > 3) subList(0, 3) else this }
                val groupAndArtifact = "$groupId:$artifactId"

                // If dependency is inside jar exclusions, then it is eligible for downloading
                var shouldAdd = false
                for ((_, regexp) in exclusions) {
                    if (regexp.matches(groupAndArtifact)) {
                        shouldAdd = true
                        break
                    }
                }

                if (shouldAdd) {
                    if (version.endsWith("-SNAPSHOT")) {
                        project.logger.warn("Dependency $depString is SNAPSHOT dependency! This is not supported yet")
                    }

                    // Calculate dependency's checksum
                    val file = dep.file
                    val bytes = file.readBytes()
                    val digest = sha1Digest.digest(bytes).joinToString("") { "%02x".format(it) }
                    sha1Digest.reset()

                    deps.add("${depString}${if (dep.classifier != null) ":${dep.classifier}" else ""}\t${digest}")
                }
            }
        }

        rootProject.subprojects.forEach { subproject ->
            artifactProcessor(subproject.configurations["runtimeClasspath"])
            artifactProcessor(subproject.configurations["compileClasspath"])
        }

        outputFile.createNewFile()
        outputFile.bufferedWriter().use { writer ->
            TreeSet(deps).forEach {
                writer.write(it)
                writer.newLine()
            }

            // TODO: pull from Gradle configuration
            val repositories = listOf(
                    "https://repo.maven.apache.org/maven2",
                    "https://repo.wut.ee/repository/mikroskeem-repo",
                    "https://oss.sonatype.org/content/repositories/snapshots"
            )

            repositories.forEach {
                writer.write("repository\t")
                writer.write(it)
                writer.newLine()
            }
        }
    }
}

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveBaseName.set("Firefly")
    archiveAppendix.set("")
    archiveVersion.set("")
    archiveClassifier.set("")
    from(collectDependencies)

    manifest {
        attributes(
                "Main-Class" to "eu.mikroskeem.firefly.Bootstrap",
                "Multi-Release" to "true",
                "Implementation-Version" to "git:Firefly-Proxy:${project.version}:${rootProject.extra["gitHash"]}:${System.getenv("BUILD_NUMBER") ?: "unknown"}",
                "Specification-Version" to SimpleDateFormat("yyyyMMdd").format(Date())
        )
    }

    transform(Log4j2PluginsCacheFileTransformer::class.java)

    dependencies {
        exclusions.asSequence().map { (raw, _) -> dependency(raw) }.forEach {
            exclude(it)
        }
    }
}

tasks["build"].dependsOn(shadowJar)