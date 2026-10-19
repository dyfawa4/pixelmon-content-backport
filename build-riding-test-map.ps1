[CmdletBinding()]
param([ValidatePattern('^r[0-9]+$')][string]$Revision='r1')
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw|ConvertFrom-Json
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/riding-map-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp ($taskBuild.classes+';'+$taskBuild.classpath) -d $taskClasses (Join-Path $PSScriptRoot 'tests/RidingTestMap.java')
if($LASTEXITCODE -ne 0){throw 'Riding map helper compile failed'}
python -X utf8 (Join-Path $PSScriptRoot 'tools/create_riding_test_map.py')
if($LASTEXITCODE -ne 0){throw 'Riding world metadata failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
$taskVersion=([regex]::Match((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources/META-INF/mods.toml') -Raw),'(?m)^version="([^"]+)"')).Groups[1].Value
$taskHelper=Join-Path $taskMods "backport-riding-test-1.16.5-$taskVersion.jar"
& $taskJar cf $taskHelper -C $taskClasses . -C (Join-Path $PSScriptRoot 'test-maps/riding-resources') .
Copy-Item -LiteralPath $taskBuild.jar -Destination $taskMods
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/riding-saves-v150') -Destination (Join-Path $taskPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'RIDING_TEST_MAP.md') -Destination (Join-Path $taskPackage 'README.md')
$taskOutput=Join-Path $PSScriptRoot ("dist/Pixelmon-Backport-riding-test-map-$taskVersion-"+$Revision+'.zip')
if(Test-Path -LiteralPath $taskOutput){throw 'Choose another revision; existing test archive is preserved'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
$taskHash=(Get-FileHash -LiteralPath $taskOutput -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText($taskOutput+'.sha256',$taskHash+'  '+[IO.Path]::GetFileName($taskOutput)+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $PSScriptRoot '.cache/riding-research/map-build.json'),(@{helper=$taskHelper;classes=$taskClasses;zip=$taskOutput;sha256=$taskHash}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
Write-Output "Riding test map: $taskOutput"
