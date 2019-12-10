import kotlinx.dom.elements
import kotlinx.dom.filterElements
import kotlinx.dom.search
import kotlinx.dom.parseXml

buildscript {
    val kotlinxDomVersion = "0.0.10"

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx.dom:${kotlinxDomVersion}")
    }
}

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
}

extra["gitHash"] = describe()
extra["overriddenRootProperties"] = mapOf<String, String>(
        "log4j2.version" to "2.12.1"
)

val originalGroup = "io.github.waterfallmc"
val originalProjectNamePrefix = "waterfall"

allprojects {
    group = "eu.mikroskeem.mikrocord"
    version = "1.15-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()

        maven("https://repo.wut.ee/repository/mikroskeem-repo")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        if (project !== rootProject) {
            loadDependencies("${rootProject.projectDir}/MikroCord-Proxy/pom.xml", isRoot = true)
            loadDependencies("${project.projectDir}/pom.xml")
        }

        api("com.google.guava:guava:28.1-jre")
        compileOnly("org.checkerframework:checker-qual:3.0.0")
    }
}

fun String.applyReplacements(replacements: Map<String, String>): String {
    var newString = this
    for ((k, v) in replacements) {
        newString = newString.replace("\${$k}", v)
    }
    return newString
}

fun Map<String, String>.merge(other: Map<String, String>): Map<String, String> {
    return HashMap<String, String>().apply {
        putAll(this@merge)
        putAll(other)
    }
}

fun DependencyHandlerScope.loadDependencies(pomFile: String, isRoot: Boolean = false) {
    //println("Loading dependencies from $pomFile")
    val dom = parseXml(File(pomFile))

    // Get root project's properties if present
    val rootProjectProperties = if (isRoot) null else {
        project.extra.get("rootProjectProperties") as? Map<String, String>
    }

    // Get current project's properties
    // Also merge properties in following priority: root < project < overriden
    val projectProperties = (dom.search("properties").firstOrNull()?.run {
        val builtProperties = childNodes.filterElements().asSequence().map { elem ->
            elem.nodeName to elem.textContent
        }.toMap()

        if (rootProjectProperties != null) {
            rootProjectProperties.merge(builtProperties)
        } else {
            project.extra["rootProjectProperties"] = builtProperties
            builtProperties
        }
    } ?: rootProjectProperties ?: emptyMap())
            .merge(rootProject.extra["overriddenRootProperties"] as Map<String, String>)

    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return

    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val version = dependencyElem.search("version").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val scope = dependencyElem.search("scope").firstOrNull()?.textContent?.applyReplacements(projectProperties)
        val classifier = dependencyElem.search("classifier").firstOrNull()?.textContent?.applyReplacements(projectProperties)

        // Replace subproject references
        if (groupId == originalGroup && artifactId.startsWith("$originalProjectNamePrefix-")) {
            api(project(":${artifactId.removePrefix("$originalProjectNamePrefix-")}"))
            return@forEach
        }

        val dependencyString = "${groupId}:${artifactId}:${version}${classifier?.run {":$this" } ?: ""}"
        //println("-> $dependencyString")
        when (scope) {
            "compile", null -> api(dependencyString)
            "provided" -> compileOnly(dependencyString)
            "runtime" -> runtimeOnly(dependencyString)
            "test" -> testImplementation(dependencyString)
        }

        // Special case lombok
        if (groupId == "org.projectlombok" && artifactId == "lombok") {
            annotationProcessor(dependencyString)
            testImplementation(dependencyString)
            testAnnotationProcessor(dependencyString)
        }
    }
}

// https://github.com/PaperMC/Paper/blob/85c254a5ac26578f7d94395ecda82e38417e8b7f/gradle/server.gradle.kts#L90
fun describe(): String {
    return cmd("git", "describe") ?: cmd("git", "log", "--pretty=format:%h") ?: "unknown"
}

fun cmd(vararg args: String): String? {
    val p = ProcessBuilder().command(*args).directory(rootProject.projectDir).start()
    p.inputStream.bufferedReader().use {
        return it.readLine()
    }
}
