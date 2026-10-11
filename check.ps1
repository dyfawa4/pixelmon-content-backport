[CmdletBinding()]
param([Parameter(Mandatory=$true)][string] $PixelmonJar,[Parameter(Mandatory=$true)][string] $LibrariesDirectory)
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskTests=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot '.cache/test-classes')).FullName
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests') -Filter '*.java' -Recurse | ForEach-Object {$_.FullName})
$taskAsm=@(Get-ChildItem -LiteralPath (Join-Path $LibrariesDirectory 'org/ow2/asm') -Filter '*.jar' -Recurse | Sort-Object {[version]$_.Directory.Name} -Descending | ForEach-Object {$_.FullName})
$taskGson=@(Get-ChildItem -LiteralPath (Join-Path $LibrariesDirectory 'com/google/code/gson') -Filter '*.jar' -Recurse | Sort-Object {[version]$_.Directory.Name} -Descending | ForEach-Object {$_.FullName})
$taskDetector=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter 'PixelmonDetector-*.jar' | ForEach-Object {$_.FullName})
$taskCompass=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter '*ExplorersCompass*.jar' | ForEach-Object {$_.FullName})
$taskCP=(@($taskTests,$taskBuild.classes,(Join-Path $PSScriptRoot 'resources'),(Join-Path $PSScriptRoot 'tests/resources'))+$taskAsm+$taskGson+@($taskBuild.classpath)+$taskDetector+$taskCompass) -join [IO.Path]::PathSeparator
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskTests @taskSources
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& $taskBuild.java -ea -cp $taskCP MixinApplicationVerification $PixelmonJar @taskDetector @taskCompass
if($LASTEXITCODE -ne 0){throw 'Mixin verification failed'}
& $taskBuild.java -ea -cp $taskCP GymLookupStaticVerification
if($LASTEXITCODE -ne 0){throw 'Gym lookup static verification failed'}
Write-Output 'Offline checks complete. Gameplay and natural-world acceptance are performed manually by the user.'
