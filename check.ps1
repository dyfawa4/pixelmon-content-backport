[CmdletBinding()]
param([Parameter(Mandatory=$true)][string] $PixelmonJar,[Parameter(Mandatory=$true)][string] $LibrariesDirectory)
$ErrorActionPreference='Stop'
python -X utf8 (Join-Path $PSScriptRoot 'tools/audit_npc_quests.py') --pixelmon-jar $PixelmonJar --output (Join-Path $PSScriptRoot '.cache/npc-quests-145/quest-audit.json')
if($LASTEXITCODE -ne 0){throw 'NPC quest graph/localization audit failed'}
$taskBuild=Get-Content -LiteralPath (Join-Path $PSScriptRoot '.cache/last-build.json') -Raw | ConvertFrom-Json
$taskTests=(New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot '.cache/test-classes')).FullName
$taskJavac=Join-Path (Split-Path $taskBuild.java) 'javac.exe'
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests') -Filter '*.java' -Recurse | ForEach-Object {$_.FullName})
$taskAsm=@(Get-ChildItem -LiteralPath (Join-Path $LibrariesDirectory 'org/ow2/asm') -Filter '*.jar' -Recurse | Sort-Object {[version]$_.Directory.Name} -Descending | ForEach-Object {$_.FullName})
$taskGson=@(Get-ChildItem -LiteralPath (Join-Path $LibrariesDirectory 'com/google/code/gson') -Filter '*.jar' -Recurse | Sort-Object {[version]$_.Directory.Name} -Descending | ForEach-Object {$_.FullName})
$taskDetector=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter 'PixelmonDetector-*.jar' | ForEach-Object {$_.FullName})
$taskCompass=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter '*ExplorersCompass*.jar' | ForEach-Object {$_.FullName})
$taskNature=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter '*NaturesCompass*.jar' | ForEach-Object {$_.FullName})
$taskTweaks=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter 'PixelTweaks*.jar' | ForEach-Object {$_.FullName})
$taskOptional=@(Get-ChildItem -LiteralPath (Split-Path $PixelmonJar) -Filter '*.jar' | Where-Object {$_.Name -match 'ftb[-_]|architectury|幸运方块|lootr|configured'} | ForEach-Object {$_.FullName})
$taskForgeClient=@(Get-ChildItem -LiteralPath (Join-Path $LibrariesDirectory 'net/minecraftforge/forge') -Recurse -Filter '*36.2.42-client.jar' | ForEach-Object {$_.FullName})
if($taskForgeClient.Count -ne 1){throw 'Exactly one actual Forge 36.2.42 patched client is required for template placement checks'}
$taskCP=(@($taskTests,$taskBuild.classes,(Join-Path $PSScriptRoot 'resources'),(Join-Path $PSScriptRoot 'tests/resources'))+$taskAsm+$taskGson+@($taskBuild.classpath)+$taskDetector+$taskCompass+$taskNature+$taskTweaks+$taskOptional) -join [IO.Path]::PathSeparator
$taskCompileArgs=Join-Path $taskTests 'javac.args'
$taskCompileLines=@('--release','11','-proc:none','-encoding','UTF8','-cp',('"'+$taskCP.Replace('\','/')+'"'),'-d',('"'+$taskTests.Replace('\','/')+'"'))
$taskCompileLines+=@($taskSources | ForEach-Object {'"'+$_.Replace('\','/')+'"'})
[IO.File]::WriteAllLines($taskCompileArgs,$taskCompileLines,[Text.UTF8Encoding]::new($false))
& $taskJavac ('@'+$taskCompileArgs)
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& $taskBuild.java -ea -cp $taskCP MixinApplicationVerification $PixelmonJar @taskDetector @taskCompass @taskNature @taskTweaks @taskOptional @taskForgeClient
if($LASTEXITCODE -ne 0){throw 'Mixin verification failed'}
& $taskBuild.java -ea -cp $taskCP FossilMachineVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Fossil machine checkpoint synchronization and Cap wording regression failed'}
& $taskBuild.java -ea -cp $taskCP BattleMechanicsAudit $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Battle rule regression failed'}
& $taskBuild.java -ea -cp $taskCP Version160Verification
if($LASTEXITCODE -ne 0){throw '1.6.0 raid selection and quest palette regression failed'}
& $taskBuild.java -ea -cp $taskCP TeraSnapshotVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Tera snapshot native single-type synchronization failed'}
& $taskBuild.java -ea -cp $taskCP ConfigInterfaceVerification
if($LASTEXITCODE -ne 0){throw 'Chinese configuration input and search failed'}
& $taskBuild.java -ea -cp $taskCP MegaFollowupVerification
if($LASTEXITCODE -ne 0){throw 'Mega signature and editor import policies failed'}
& $taskBuild.java -ea -cp $taskCP NativeVariantModelVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Native Mega model parsing failed'}
python -X utf8 (Join-Path $PSScriptRoot 'tools/verify_smooth_mega_variants.py')
if($LASTEXITCODE -ne 0){throw 'Smooth Mega visual scope / rig regression failed'}
& $taskBuild.java -ea -cp $taskCP ErosionVerification
if($LASTEXITCODE -ne 0){throw 'Erosion, individual extra-turn clocks and configuration persistence failed'}
& $taskBuild.java -ea -cp $taskCP GymLookupStaticVerification
if($LASTEXITCODE -ne 0){throw 'Gym lookup static verification failed'}
& $taskBuild.java -ea -cp $taskCP TeraVisualVerification
if($LASTEXITCODE -ne 0){throw 'Tera visuals geometry and lifecycle verification failed'}
& $taskBuild.java -ea -cp $taskCP Version13StaticVerification
if($LASTEXITCODE -ne 0){throw 'Version 1.3 static verification failed'}
& $taskBuild.java -ea -cp $taskCP GymItemAllowanceVerification
if($LASTEXITCODE -ne 0){throw 'Gym budget verification failed'}
Write-Output 'Offline checks complete. Gameplay and natural-world acceptance are performed manually by the user.'
& $taskBuild.java -ea -cp $taskCP UtilityCommandPolicyVerification
if($LASTEXITCODE -ne 0){throw 'Utility command target boundaries failed'}
& $taskBuild.java -ea -cp $taskCP EeveeFamilyVerification
if($LASTEXITCODE -ne 0){throw 'Eevee family criteria verification failed'}
& $taskBuild.java -ea -cp $taskCP SpecialAppearanceVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Special appearance starter/species criteria regression failed'}
& $taskBuild.java -ea -cp $taskCP NpcSwitchPolicyVerification
if($LASTEXITCODE -ne 0){throw 'NPC switch policy verification failed'}
& $taskBuild.java -ea -cp $taskCP GymTrainingVerification
if($LASTEXITCODE -ne 0){throw 'Gym training verification failed'}
& $taskBuild.java -ea -cp $taskCP AcquisitionVerification
if($LASTEXITCODE -ne 0){throw 'Acquisition verification failed'}
& $taskBuild.java -ea -cp $taskCP FieldSequenceVerification
if($LASTEXITCODE -ne 0){throw 'Field sequence verification failed'}
& $taskBuild.java -ea -cp $taskCP BurstVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Burst/species verification failed'}
& $taskBuild.java -ea -cp $taskCP CapTeraLearningVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Cap Pikachu Tera Blast learning regression failed'}
& $taskBuild.java -ea -cp $taskCP JavaRuntimeGateVerification
if($LASTEXITCODE -ne 0){throw 'Strict Java 11 world/menu gate verification failed'}
& $taskBuild.java -ea -cp $taskCP LegendaryBirdVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Legendary bird criteria verification failed'}
& $taskBuild.java -ea -cp $taskCP AcquisitionCoverageVerification
if($LASTEXITCODE -ne 0){throw 'Acquisition coverage verification failed'}
& $taskBuild.java -ea -cp $taskCP LateBloomerVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Late bloomer acquisition resources and collection verification failed'}
& $taskBuild.java -ea -cp $taskCP Legendary144Verification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Legendary 1.4.4 PCBox and Crowned completion regressions failed'}
& $taskBuild.java -ea -cp $taskCP BurstZCrashVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Z move border crash regression failed'}
& $taskBuild.java -ea -cp $taskCP BossCaptureVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Wild Boss ball selection verification failed'}
& $taskBuild.java -ea -cp $taskCP GymPrizeVerification
if($LASTEXITCODE -ne 0){throw 'Gym prize verification failed'}

& $taskBuild.java -ea -cp $taskCP BattleEvolutionVerification
if($LASTEXITCODE -ne 0){throw 'Battle evolution state verification failed'}

& $taskBuild.java -ea -cp $taskCP GymAceVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Gym mature ace verification failed'}

& $taskBuild.java -ea -cp $taskCP GymTerrainVerification
if($LASTEXITCODE -ne 0){throw 'Gym terrain generation verification failed'}
& $taskBuild.java -ea -cp $taskCP GymRegionBoundaryVerification
if($LASTEXITCODE -ne 0){throw 'Reported region boundary regression failed'}
& $taskBuild.java -ea -cp $taskCP StructureSaveNameVerification
if($LASTEXITCODE -ne 0){throw 'Structure reference save name regression failed'}
& $taskBuild.java -ea -cp $taskCP DetectorLinePrecisionVerification
if($LASTEXITCODE -ne 0){throw 'Far-coordinate detector precision failed'}
& $taskBuild.java -ea -cp $taskCP ClientDisplayVerification
if($LASTEXITCODE -ne 0){throw 'Dialogue runtime and detector stale target checks failed'}
& $taskBuild.java -ea -cp $taskCP QuestStructureVerification
if($LASTEXITCODE -ne 0){throw 'FTB registered structure fallback checks failed'}
& $taskBuild.java -ea -cp $taskCP CapCaptureWarningVerification
if($LASTEXITCODE -ne 0){throw 'Cap battle-entry capture warning checks failed'}
& $taskBuild.java -ea -cp $taskCP MergedPatchVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Integrated raid catch animation checks failed'}
& $taskBuild.java -ea -cp $taskCP StrongholdVerification
if($LASTEXITCODE -ne 0){throw 'Native stronghold ring compass regression failed'}
& $taskBuild.java -ea -cp $taskCP TeraDenLocatorVerification
if($LASTEXITCODE -ne 0){throw 'Real Tera den locator boundaries failed'}
& $taskBuild.java -ea -cp $taskCP TeraHabitatVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Tera raid biome/dimension boundaries failed'}
& $taskBuild.java -ea -cp $taskCP DenDistributionVerification minecraft:overworld
if($LASTEXITCODE -ne 0){throw 'Overworld den distribution verification failed'}
& $taskBuild.java -ea -cp $taskCP NpcLocalizationVerification
if($LASTEXITCODE -ne 0){throw 'NPC localization verification failed'}
& $taskBuild.java -ea -cp $taskCP DisguisedCaptureVerification
if($LASTEXITCODE -ne 0){throw 'Disguised capture verification failed'}
& $taskBuild.java -ea -cp $taskCP PokeChestLootVerification
if($LASTEXITCODE -ne 0){throw 'Poke chest loot verification failed'}
& $taskBuild.java -ea -cp $taskCP OriginMercyVerification
if($LASTEXITCODE -ne 0){throw 'Origin mercy verification failed'}
& $taskBuild.java -ea -cp $taskCP BattleMotionVerification
if($LASTEXITCODE -ne 0){throw 'Battle motion verification failed'}
& $taskBuild.java -ea -cp $taskCP LegendaryAcquisitionAudit $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'All legendary acquisition audit failed'}
& $taskBuild.java -ea -cp $taskCP BurstDynamicPreviewVerification
if($LASTEXITCODE -ne 0){throw 'Burst dynamic preview verification failed'}
& $taskBuild.java -ea -cp $taskCP PokedexHabitatVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Pokedex habitat verification failed'}
& $taskBuild.java -ea -cp $taskCP MultiplayerRegressionVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Multiplayer initialization regression failed'}
& $taskBuild.java -ea -cp $taskCP CapSpawnVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Cap village spawn/query regression failed'}
& $taskBuild.java -ea -cp $taskCP HotfixAdditionsVerification $PixelmonJar @taskCompass @taskNature
if($LASTEXITCODE -ne 0){throw 'Gender/compass/reward/chance hotfix checks failed'}
& $taskBuild.java -ea -cp $taskCP RayquazaMegaAdvancementVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Rayquaza Mega achievement completion checks failed'}
& $taskBuild.java -ea -cp $taskCP MegaBossAvatarVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Mega avatar verification failed'}
& $taskBuild.java -ea -cp $taskCP ZaMegaVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'ZA Mega routes and official ability verification failed'}
if($LASTEXITCODE -ne 0){throw 'All Mega Boss avatar resource checks failed'}
& $taskBuild.java -ea -cp $taskCP RaidCapExclusionVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Cap exclusion from Max/Tera dens failed'}
& $taskBuild.java -ea -cp $taskCP AshWaterBoostVerification
if($LASTEXITCODE -ne 0){throw 'Ash Water boost lifetime/isolation regression failed'}
& $taskBuild.java -ea -cp $taskCP AshBattleDisplayVerification
if($LASTEXITCODE -ne 0){throw 'Ash battle sidebar display regression failed'}
& $taskBuild.java -ea -cp $taskCP GreninjaFormDataVerification $PixelmonJar
if($LASTEXITCODE -ne 0){throw 'Greninja native appearance/manual fixture regression failed'}
& $taskBuild.java -ea -cp $taskCP V199FinishVerification
if($LASTEXITCODE -ne 0){throw 'V199 supply/quest/ability acquisition regression failed'}
