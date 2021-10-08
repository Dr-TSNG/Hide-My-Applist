fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = java.io.ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

val minSdkVer by extra(24)
val targetSdkVer by extra(31)
val buildToolsVer by extra("31.0.0")
val ndkVer by extra("23.0.7599858")

val appVerName by extra("2.2")
val appVerCode by extra(66)
val serviceVer by extra(66)
val minRiruVer by extra(28)
val minBackupVer by extra(65)

val gitCommitCount by extra("git rev-list HEAD --count".execute())
val gitCommitHash by extra("git rev-parse --verify --short HEAD".execute())

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}