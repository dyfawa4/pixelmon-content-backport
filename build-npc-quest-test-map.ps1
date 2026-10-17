[CmdletBinding()]
param([ValidatePattern('^r[0-9]+$')][string] $Revision)
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskVersion=([regex]::Match((Get-Content -LiteralPath (Join-Path $PSScriptRoot 'resources/META-INF/mods.toml') -Raw),'(?m)^version="([^"]+)"')).Groups[1].Value
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskJar=Join-Path (Split-Path $taskBuild.java) 'jar.exe'
$taskStage=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot ('.cache/npc-map-'+[guid]::NewGuid().ToString('N')))).FullName
$taskClasses=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'classes')).FullName
$taskCP=(@($taskBuild.classes,$taskBuild.classpath)) -join [IO.Path]::PathSeparator
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskClasses (Join-Path $PSScriptRoot 'tests/NpcQuestTestMap.java')
if($LASTEXITCODE -ne 0){throw 'NPC map helper compilation failed'}
python -X utf8 (Join-Path $PSScriptRoot 'tools/create_npc_quest_test_map.py')
if($LASTEXITCODE -ne 0){throw 'NPC initial world generation failed'}
$taskPackage=(New-Item -ItemType Directory -Force -Path (Join-Path $taskStage 'package')).FullName
$taskMods=(New-Item -ItemType Directory -Force -Path (Join-Path $taskPackage 'mods')).FullName
$taskResources=Join-Path $PSScriptRoot 'test-maps/npc-quest-resources'
$taskHelper=Join-Path $taskMods "backport-npc-quest-test-1.16.5-$taskVersion.jar"
& $taskJar cf $taskHelper -C $taskClasses . -C $taskResources .
if($LASTEXITCODE -ne 0){throw 'NPC map helper packaging failed'}
Copy-Item -LiteralPath $taskBuild.jar -Destination $taskMods
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/npc-quest-saves-v145') -Destination (Join-Path $taskPackage 'saves') -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'NPC_QUEST_TEST_MAP.md') -Destination (Join-Path $taskPackage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE.txt') -Destination $taskPackage
$taskSuffix=if($Revision){'-'+$Revision}else{''}
$taskOutput=Join-Path $PSScriptRoot ("dist/Pixelmon-Backport-npc-quest-test-map-$taskVersion"+$taskSuffix+'.zip')
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(Test-Path -LiteralPath $taskOutput){throw 'NPC test ZIP already exists; choose a new output name rather than overwrite a delivered map'}
[System.IO.Compression.ZipFile]::CreateFromDirectory($taskPackage,$taskOutput,[System.IO.Compression.CompressionLevel]::Optimal,$false)
Copy-Item -LiteralPath $taskHelper -Destination (Join-Path $PSScriptRoot ".cache/backport-npc-quest-test-1.16.5-$taskVersion.jar")
Write-Output "NPC map package: $taskOutput"
