rootProject.name = "ngrok-to-git"

fun defineSubProject(name: String, path: String) {
    include(name)
    project(":$name").projectDir = file(path)
}

defineSubProject("ngrok-to-gists-app", "app")