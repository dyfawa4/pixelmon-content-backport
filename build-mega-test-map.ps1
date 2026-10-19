[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskOutput=Join-Path $PSScriptRoot '.cache/za-mega-160/backport-mega-test-1.16.5-1.6.0.jar'
$taskStage=Join-Path $PSScriptRoot ('.cache/mega-helper-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskStage | Out-Null
$taskCP=(@($taskBuild.classes,(Join-Path $PSScriptRoot 'resources'))+@($taskBuild.classpath)) -join [IO.Path]::PathSeparator
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
& $taskJavac --release 11 -proc:none -encoding UTF8 -cp $taskCP -d $taskStage (Join-Path $PSScriptRoot 'tests/ZaMegaTestMap.java') (Join-Path $PSScriptRoot 'tests/DenPlacementVerification.java')
if($LASTEXITCODE -ne 0){throw 'Mega gallery helper compilation failed'}
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/mega-resources/META-INF') -Destination $taskStage -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/mega-resources/pack.mcmeta') -Destination $taskStage
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'test-maps/mega-resources/mega-gallery.json') -Destination $taskStage
& (Join-Path (Split-Path $taskBuild.java) 'jar.exe') --create --file $taskOutput -C $taskStage .
if($LASTEXITCODE -ne 0){throw 'Mega gallery helper packaging failed'}
Write-Output ('Mega gallery helper: '+$taskOutput)
