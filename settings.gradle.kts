rootProject.name = "YuQ"

fun includeProject(name: String, dir: String? = null) {
    include(name)
    dir?.let { project(name).projectDir = file(it) }
}
includeProject(":core")
includeProject(":devtools")