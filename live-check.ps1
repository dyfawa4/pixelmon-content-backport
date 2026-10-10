[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string] $ServerDirectory,
    [Parameter(Mandatory=$true)][string] $PixelmonJar
)
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot '.cache/live-test-classes')).FullName
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
$taskCP=(@($taskClasses,$taskBuild.classes,$taskBuild.classpath)) -join [IO.Path]::PathSeparator
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses (Join-Path $PSScriptRoot 'tests/EvolutionVerification.java')
if($LASTEXITCODE -ne 0){throw 'Evolution tests compilation failed'}
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses (Join-Path $PSScriptRoot 'tests/WorldContentVerification.java') (Join-Path $PSScriptRoot 'tests/TeraVerification.java') (Join-Path $PSScriptRoot 'tests/TeraRaidVerification.java')
if($LASTEXITCODE -ne 0){throw 'World content tests compilation failed'}
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses (Join-Path $PSScriptRoot 'tests/NativeDataVerification.java') (Join-Path $PSScriptRoot 'tests/LiveVerification.java') (Join-Path $PSScriptRoot 'tests/ItemInteractionVerification.java') (Join-Path $PSScriptRoot 'tests/TeraVerification.java') (Join-Path $PSScriptRoot 'tests/TeraItemVerification.java') (Join-Path $PSScriptRoot 'tests/AshVerification.java') (Join-Path $PSScriptRoot 'tests/RepairVerification.java') (Join-Path $PSScriptRoot 'tests/IntegrationVerification.java') (Join-Path $PSScriptRoot 'tests/TestMaps.java') (Join-Path $PSScriptRoot 'tests/DedicatedTestMaps.java') (Join-Path $PSScriptRoot 'tests/TestMapVerification.java')
if($LASTEXITCODE -ne 0){throw 'Live tests compilation failed'}
$taskServer=(Resolve-Path -LiteralPath $ServerDirectory).Path
if(-not (Test-Path -LiteralPath (Join-Path $taskServer 'forge-1.16.5-36.2.42.jar'))) {throw 'Prepare a separate Forge 36.2.42 server first; see BUILDING.md'}
New-Item -ItemType Directory -Force -Path (Join-Path $taskServer 'mods') | Out-Null
& $taskJar cf (Join-Path $taskServer 'mods/backport-live-test.jar') -C $taskClasses . -C (Join-Path $PSScriptRoot 'tests/live-resources') .
if($LASTEXITCODE -ne 0){throw 'Test-only mod packaging failed'}
Copy-Item -LiteralPath $taskBuild.jar -Destination (Join-Path $taskServer 'mods/pixelmon-content-backport-1.16.5-1.0.0.jar') -Force
Copy-Item -LiteralPath $PixelmonJar -Destination (Join-Path $taskServer 'mods/Pixelmon-legacy.jar') -Force
python (Join-Path $PSScriptRoot 'tools/run_test_server.py') --java $taskBuild.java --server-directory $taskServer --pixelmon $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Live integration tests failed; inspect .cache/live-test.log'}
