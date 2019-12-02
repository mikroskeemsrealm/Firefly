import java.text.SimpleDateFormat
import java.util.Date
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

apply(plugin = "com.github.johnrengelman.shadow")

val prometheusVersion = "0.6.0"

dependencies {
    implementation("io.prometheus:simpleclient:${prometheusVersion}")
    implementation("io.prometheus:simpleclient_hotspot:${prometheusVersion}")
    implementation("io.prometheus:simpleclient_httpserver:${prometheusVersion}")
    implementation("eu.mikroskeem:jvm-hiccup:1.0.0")

    // Updated dependencies
    compile("com.google.guava:guava:28.1-jre")
    compile("mysql:mysql-connector-java:8.0.17")
}

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveBaseName.set("MikroCord")
    archiveAppendix.set("")
    archiveVersion.set("")
    archiveClassifier.set("")

    manifest {
        attributes(
                "Main-Class" to "eu.mikroskeem.mikrocord.Bootstrap",
                "Implementation-Version" to "git:MikroCord-Proxy:${project.version}:${rootProject.extra["gitHash"]}:${System.getenv("BUILD_NUMBER") ?: "unknown"}",
                "Specification-Version" to SimpleDateFormat("yyyyMMdd").format(Date())
        )
    }

    transform(Log4j2PluginsCacheFileTransformer::class.java)

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
        "io.netty:*",
        "com.flowpowered:flow-nbt",
        "org.slf4j:slf4j-api",
        "org.apache.logging.log4j:log4j-iostreams",
        "org.apache.logging.log4j:log4j-jul",
        "org.apache.logging.log4j:log4j-slf4j-impl",
        "net.kyori:*",
        "org.checkerframework:checker-qual",
        "org.ow2.asm:asm",
        "io.prometheus:*",
        "eu.mikroskeem:jvm-hiccup"
    )

    dependencies {
        exclusions.asSequence().map { dependency(it.replace("*", ".*")) }.forEach {
            exclude(it)
        }
    }
}

tasks["build"].dependsOn(shadowJar)