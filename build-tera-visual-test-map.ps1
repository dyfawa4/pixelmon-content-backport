[CmdletBinding()]
param([ValidatePattern('^r[0-9]+$')][string]$Revision='r1')
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskVersion=([regex]::Match((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources/META-INF/mods.toml') -Raw),'(?m)^version="([^"]+)"')).Groups[1].Value
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/tera-visual-map-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp ($taskBuild.classes+';'+$taskBuild.classpath) -d $taskClasses (Join-Path $PSScriptRoot 'tests/TeraVisualTestMap.java') (Join-Path $PSScriptRoot 'tests/TeraVisualPreviewScreen.java')
if($LASTEXITCODE -ne 0){throw 'Tera visual map helper compilation failed'}
python -X utf8 (Join-Path $PSScriptRoot 'tools/create_tera_visual_test_map.py')
if($LASTEXITCODE -ne 0){throw 'Tera visual initial world generation failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
$taskHelper=Join-Path $taskMods "backport-tera-visual-test-1.16.5-$taskVersion.jar"
& $taskJar cf $taskHelper -C $taskClasses . -C (Join-Path $PSScriptRoot 'test-maps/tera-visual-resources') .
if($LASTEXITCODE -ne 0){throw 'Tera visual helper packaging failed'}
Copy-Item -LiteralPath $taskBuild.jar -Destination $taskMods
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/tera-visual-saves-v145') -Destination (Join-Path $taskPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'TERA_VISUAL_TEST_MAP.md') -Destination (Join-Path $taskPackage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'THIRD_PARTY.md'),(Join-Path $PSScriptRoot 'LICENSE.txt') -Destination $taskPackage
$taskOutput=Join-Path $PSScriptRoot ("dist/Pixelmon-Backport-tera-visual-test-map-$taskVersion-"+$Revision+'.zip')
if(Test-Path -LiteralPath $taskOutput){throw 'Test ZIP already exists; choose another revision'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
$taskHash=(Get-FileHash -LiteralPath $taskOutput -Algorithm SHA256).Hash.ToLower()
[IO.File]::WriteAllText($taskOutput+'.sha256',$taskHash+'  '+[IO.Path]::GetFileName($taskOutput)+[Environment]::NewLine,[Text.UTF8Encoding]::new($false))
Write-Output "Tera visual sample map: $taskOutput"
