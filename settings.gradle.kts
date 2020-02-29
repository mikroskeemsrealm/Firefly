rootProject.name = "firefly"

fun importProject(name: String) {
    include(name)
    project(":$name").apply {
        projectDir = File("./Firefly-Proxy/${name}")
        buildFileName = "../../gradle/subprojects/${name}-build.gradle.kts"
    }
}

importProject("api")
importProject("chat")
importProject("config")
importProject("event")
importProject("log4j")
importProject("native")
importProject("protocol")
importProject("proxy")
importProject("query")