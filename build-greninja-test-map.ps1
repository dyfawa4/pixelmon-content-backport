[CmdletBinding()]
param([ValidatePattern('^r[0-9]+$')][string]$Revision='r1')
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw|ConvertFrom-Json
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/greninja-map-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
& (Join-Path (Split-Path $taskBuild.java) 'javac.exe') --release 11 -proc:none -encoding UTF8 -cp ($taskBuild.classes+';'+$taskBuild.classpath) -d $taskClasses (Join-Path $PSScriptRoot 'tests/GreninjaTestMap.java') (Join-Path $PSScriptRoot 'tests/GreninjaNativeAudit.java')
if($LASTEXITCODE -ne 0){throw 'Greninja map helper compile failed'}
python -X utf8 (Join-Path $PSScriptRoot 'tools/create_greninja_test_map.py')
if($LASTEXITCODE -ne 0){throw 'Greninja world metadata failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
& (Join-Path (Split-Path $taskBuild.java) 'jar.exe') cf (Join-Path $taskMods 'backport-greninja-test-1.16.5-1.6.0.jar') -C $taskClasses . -C (Join-Path $PSScriptRoot 'test-maps/greninja-resources') .
if($LASTEXITCODE -ne 0){throw 'Greninja map helper packaging failed'}
Copy-Item -LiteralPath $taskBuild.jar -Destination $taskMods
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/greninja-saves-v160') -Destination (Join-Path $taskPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'GRENINJA_TEST_MAP.md') -Destination (Join-Path $taskPackage 'README.md')
$taskOutput=Join-Path $PSScriptRoot ('dist/Pixelmon-Backport-greninja-test-map-1.6.0-'+$Revision+'.zip')
if(Test-Path -LiteralPath $taskOutput){throw 'Choose another revision; existing test archive is preserved'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
$taskHash=(Get-FileHash -LiteralPath $taskOutput -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText($taskOutput+'.sha256',$taskHash+'  '+[IO.Path]::GetFileName($taskOutput)+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
Write-Output "Greninja test map: $taskOutput"
