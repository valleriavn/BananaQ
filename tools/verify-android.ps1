param(
    [string[]]$Tasks = @(':feature:scanner:domain:test', ':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug'),
    [switch]$RecordScreenshots
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJavaHome = Join-Path $env:USERPROFILE '.gradle/jdks/eclipse_adoptium-21-amd64-windows.2'
if (-not (Test-Path -LiteralPath (Join-Path $taskJavaHome 'bin/java.exe'))) {
    $taskJavaHome = $env:JAVA_HOME
}
if (-not $taskJavaHome) { throw 'Set JAVA_HOME to a Java 21 installation.' }
$taskIpcDirectory = Join-Path $taskRoot 'build/ipc'
$null = New-Item -ItemType Directory -Path $taskIpcDirectory -Force
$taskOldJavaHome = $env:JAVA_HOME
$taskOldJavaOpts = $env:JAVA_OPTS
$taskOldToolOpts = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_HOME = $taskJavaHome
    # Keep the build in one JVM and put Java's internal sockets under a short path.
    # This also applies to test-worker and compiler child JVMs in sandboxed Windows.
    $env:JAVA_TOOL_OPTIONS = "$taskOldToolOpts -Djdk.net.unixdomain.tmpdir=`"$taskIpcDirectory`"".Trim()
    # Robolectric's native loader currently treats an encoded space in its Maven
    # cache URL as a literal %20. Use Windows' existing short path when available.
    $taskMavenCache = Join-Path $env:USERPROFILE '.m2/repository'
    if (Test-Path -LiteralPath $taskMavenCache) {
        if (-not ('BananaQBuild.NativePaths' -as [type])) {
            Add-Type -Namespace BananaQBuild -Name NativePaths -MemberDefinition @'
[System.Runtime.InteropServices.DllImport("kernel32.dll", CharSet = System.Runtime.InteropServices.CharSet.Unicode)]
public static extern uint GetShortPathName(string longPath, System.Text.StringBuilder shortPath, uint length);
'@
        }
        $taskShortCache = [System.Text.StringBuilder]::new(32768)
        $taskShortLength = [BananaQBuild.NativePaths]::GetShortPathName($taskMavenCache, $taskShortCache, 32768)
        if ($taskShortLength -gt 0) {
            $env:JAVA_TOOL_OPTIONS += " -Dmaven.repo.local=`"$taskShortCache`""
        }
    }
    $env:JAVA_OPTS = '-Xms64m -Xmx2048m --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.prefs/java.util.prefs=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.xml/javax.xml.namespace=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED -Dfile.encoding=UTF-8 -Duser.country=PH -Duser.language=en -Duser.variant'
    $taskArgs = $Tasks + @('--no-daemon', "-Dorg.gradle.jvmargs=$env:JAVA_OPTS", '-Dorg.gradle.internal.instrumentation.agent=false', '--console=plain')
    if ($RecordScreenshots) { $taskArgs += '-Pscreenshots=record' }
    Push-Location $taskRoot
    try { & './gradlew.bat' @taskArgs; $taskExitCode = $LASTEXITCODE }
    finally { Pop-Location }
}
finally {
    $env:JAVA_HOME = $taskOldJavaHome
    $env:JAVA_OPTS = $taskOldJavaOpts
    $env:JAVA_TOOL_OPTIONS = $taskOldToolOpts
}
exit $taskExitCode
