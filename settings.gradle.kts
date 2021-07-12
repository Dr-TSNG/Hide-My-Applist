rootProject.name = "Hide My Applist"
include(":app", ":dex-ptm")
val compilerLibsDir = File(settingsDir, "libs")
project(":dex-ptm").projectDir = File(compilerLibsDir, "dex-ptm")