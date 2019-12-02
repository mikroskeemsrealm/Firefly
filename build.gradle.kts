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
    java
    id("com.github.johnrengelman.shadow") version "5.1.0" apply false
}

extra["gitHash"] = describe()

allprojects {
    group = "eu.mikroskeem.mikrocord"
    version = "1.14-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()

        maven("https://repo.wut.ee/repository/mikroskeem-repo")
    }

    val lombokVersion = "1.18.4"

    dependencies {
        if (project !== rootProject) {
            loadDependencies("${rootProject.projectDir}/MikroCord-Proxy/pom.xml", isRoot = true)
            loadDependencies("${project.projectDir}/pom.xml")
        }

        // Special case lombok
        compileOnly("org.projectlombok:lombok:${lombokVersion}")
        annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
        testCompileOnly("org.projectlombok:lombok:${lombokVersion}")
        testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    }
}

fun String.applyReplacements(replacements: Map<String, String>): String {
    var newString = this
    for ((k, v) in replacements) {
        newString = newString.replace("\${$k}", v)
    }
    return newString
}

fun DependencyHandlerScope.loadDependencies(pomFile: String, isRoot: Boolean = false) {
    //println("Loading dependencies from $pomFile")
    val dom = parseXml(File(pomFile))
    val rootProjectProperties = if (isRoot) null else {
        project.extra.get("rootProjectProperties") as? Map<String, String>
    }
    val projectProperties = dom.search("properties").firstOrNull()?.run {
        val builtProperties = childNodes.filterElements().asSequence().map { elem ->
            elem.nodeName to elem.textContent
        }.toMap()
        if (!isRoot) {
            HashMap<String, String>().apply {
                putAll(rootProjectProperties!!)
                putAll(builtProperties)
            }
        } else {
            project.extra["rootProjectProperties"] = builtProperties
            builtProperties
        }
    } ?: rootProjectProperties ?: emptyMap()
    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return

    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val version = dependencyElem.search("version").firstOrNull()!!.textContent.applyReplacements(projectProperties)
        val scope = dependencyElem.search("scope").firstOrNull()?.textContent?.applyReplacements(projectProperties)

        // Replace subproject references
        if (groupId == "${rootProject.group}" && artifactId.startsWith("${rootProject.name}-")) {
            compile(project(":${artifactId.removePrefix("${rootProject.name}-")}"))
            return@forEach
        }

        val dependencyString = "${groupId}:${artifactId}:${version}"
        //println("-> $dependencyString")
        when (scope) {
            "compile", null -> compile(dependencyString)
            "provided" -> compileOnly(dependencyString)
            "runtime" -> runtime(dependencyString)
            "test" -> testCompile(dependencyString)
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