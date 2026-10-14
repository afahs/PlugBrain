param([string[]]$Tasks = @(':app:assembleDebug', ':app:ktlintCheck', ':app:verifyPaparazziDebug'))

$ErrorActionPreference = 'Stop'
$toolRoot = Join-Path (Split-Path $PSScriptRoot -Parent) '.tools'
$javaPath = Join-Path $toolRoot 'java/jdk-17.0.20.1+1'
$sdkPath = Join-Path $toolRoot 'android-sdk'
if (!(Test-Path (Join-Path $javaPath 'bin/java.exe')) -or !(Test-Path $sdkPath)) {
    throw 'Workspace-local build tools are missing. Use Java 17 and Android SDK 35 as described in APP_OPENING.md.'
}
$env:JAVA_HOME = $javaPath
$env:ANDROID_HOME = $sdkPath
$env:GRADLE_USER_HOME = Join-Path $toolRoot 'gradle'
Push-Location $PSScriptRoot
try {
    & ./gradlew.bat @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
