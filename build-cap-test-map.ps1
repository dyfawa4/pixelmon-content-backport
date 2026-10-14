[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskVersion=([regex]::Match((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources/META-INF/mods.toml') -Raw),'(?m)^version="([^"]+)"')).Groups[1].Value
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/cap-map-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
$taskCP=(@($taskBuild.classes,$taskBuild.classpath)) -join [IO.Path]::PathSeparator
$taskSources=@('MapSupport.java','CapTestMaps.java','FeatureTestMaps.java','FeatureStations.java','BadgeGallery.java','GymTestMaps.java','TestMaps.java','DedicatedTestMaps.java','PrepareTestMaps.java') | ForEach-Object {Join-Path $PSScriptRoot ('tests/'+$_)}
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses @taskSources
if($LASTEXITCODE -ne 0){throw 'Cap map-control compilation failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
& $taskJar cf (Join-Path $taskMods "backport-test-maps-1.16.5-$taskVersion.jar") -C $taskClasses . -C (Join-Path $PSScriptRoot 'test-maps/resources') .
if($LASTEXITCODE -ne 0){throw 'Cap map-control packaging failed'}
Copy-Item -LiteralPath $taskBuild.jar -Destination $taskMods
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/cap-saves-v14') -Destination (Join-Path $taskPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'CAP_TEST_MAP.md') -Destination (Join-Path $taskPackage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE.txt') -Destination $taskPackage
$taskOutput=Join-Path $PSScriptRoot "dist/Pixelmon-Backport-cap-test-map-$taskVersion.zip"
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(Test-Path -LiteralPath $taskOutput){Remove-Item -LiteralPath $taskOutput -Force}
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
Write-Output "Cap map package: $taskOutput"
