[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/maps-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
$taskCP=(@($taskBuild.classes,$taskBuild.classpath)) -join [IO.Path]::PathSeparator
$taskSources=@('MapSupport.java','FeatureTestMaps.java','FeatureStations.java','BadgeGallery.java','GymTestMaps.java','TestMaps.java','DedicatedTestMaps.java','PrepareTestMaps.java') | ForEach-Object {Join-Path $PSScriptRoot ('tests/'+$_)}
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses @taskSources
if($LASTEXITCODE -ne 0){throw 'Map-control compilation failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
& $taskJar cf (Join-Path $taskMods 'backport-test-maps-1.16.5-1.2.0.jar') -C $taskClasses . -C (Join-Path $PSScriptRoot 'test-maps/resources') .
if($LASTEXITCODE -ne 0){throw 'Map-control packaging failed'}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/saves') -Destination $taskPackage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'TEST_MAPS.md') -Destination (Join-Path $taskPackage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'FEATURE_TEST_MAPS.md') -Destination $taskPackage
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE.txt') -Destination $taskPackage
$taskDist=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'dist')).FullName
$taskOutput=Join-Path $taskDist 'Pixelmon-Backport-test-maps-1.2.0.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(Test-Path -LiteralPath $taskOutput){Remove-Item -LiteralPath $taskOutput -Force}
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
Write-Output "Map package: $taskOutput"
$taskGymPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'gym-package')).FullName
Copy-Item -LiteralPath $taskMods -Destination $taskGymPackage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/gym-saves') -Destination (Join-Path $taskGymPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'GYM_TEST_MAPS.md') -Destination (Join-Path $taskGymPackage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'GYM_GENERATION.md') -Destination $taskGymPackage
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE.txt') -Destination $taskGymPackage
$taskGymOutput=Join-Path $taskDist 'Pixelmon-Backport-gym-test-maps-1.2.0.zip'
if(Test-Path -LiteralPath $taskGymOutput){Remove-Item -LiteralPath $taskGymOutput -Force}
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskGymPackage,$taskGymOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
Write-Output "Gym map package: $taskGymOutput"
