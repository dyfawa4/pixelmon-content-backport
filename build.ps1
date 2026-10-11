[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string] $LibrariesDirectory,
    [Parameter(Mandatory=$true)][string] $PixelmonJar,
    [Parameter(Mandatory=$true)][string] $JdkDirectory,
    [string] $ForgeClientJar,
    [string] $JeiJar
)
$ErrorActionPreference='Stop'
$taskJava=Join-Path $JdkDirectory 'bin/java.exe'
$taskJavac=Join-Path $JdkDirectory 'bin/javac.exe'
$taskJar=Join-Path $JdkDirectory 'bin/jar.exe'
$taskVersion=(& $taskJavac -version 2>&1 | Out-String).Trim()
if($taskVersion -notmatch '^javac 11\.') {throw "Use JDK 11 for this pack; found $taskVersion"}
if(-not $ForgeClientJar) {
    $taskForges=@(Get-ChildItem -LiteralPath $LibrariesDirectory -Filter 'forge-1.16.5-36.2.42-client.jar' -Recurse)
    if($taskForges.Count -ne 1){throw 'Pass the supported ForgeClientJar explicitly'}
    $ForgeClientJar=$taskForges[0].FullName
}
$taskCache=Join-Path $PSScriptRoot '.cache'
$taskBuild=(New-Item -ItemType Directory -Force -Path (Join-Path $taskCache ('build-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskBuild 'classes')).FullName
$taskLibraries=@(Get-ChildItem -LiteralPath $LibrariesDirectory -Filter '*.jar' -Recurse | ForEach-Object {$_.FullName})
if(-not $JeiJar){
    $taskJei=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter '*jei-1.16.5-7*.jar')
    if($taskJei.Count -ne 1){throw 'Pass -JeiJar with JEI 1.16.5 7.x for the optional integration compile API'}
    $JeiJar=$taskJei[0].FullName
}
$taskClasspath=(@($ForgeClientJar,$PixelmonJar,$JeiJar)+$taskLibraries) -join [IO.Path]::PathSeparator
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Filter '*.java' -Recurse | ForEach-Object {$_.FullName})
& $taskJavac --release 11 -proc:none -encoding UTF-8 -cp $taskClasspath -d $taskClasses @taskSources
if($LASTEXITCODE -ne 0){throw 'Java 11 compilation failed'}
$taskDist=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'dist')).FullName
$taskOutput=Join-Path $taskDist 'pixelmon-content-backport-1.16.5-1.1.0.jar'
& $taskJar cfm $taskOutput (Join-Path $PSScriptRoot 'resources/META-INF/MANIFEST.MF') -C $taskClasses . -C (Join-Path $PSScriptRoot 'resources') .
if($LASTEXITCODE -ne 0){throw 'Packaging failed'}
Set-Content -LiteralPath (Join-Path $taskCache 'last-build.json') -Encoding UTF8 -Value (@{classes=$taskClasses;classpath=$taskClasspath;java=$taskJava;jar=$taskOutput}|ConvertTo-Json)
Write-Output "Release build: $taskOutput"
