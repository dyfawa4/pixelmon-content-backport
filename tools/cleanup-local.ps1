[CmdletBinding(SupportsShouldProcess=$true,ConfirmImpact='Medium')]
param([switch]$Execute,[string]$PlanPath='')
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd('\')
if($taskRoot -ne 'D:\游戏\宝可梦\重铸内容回迁'){throw 'This local cleanup plan is only for the reviewed project path.'}
$taskCache=Join-Path $taskRoot '.cache'
$taskLogs=Join-Path $taskCache 'cleanup-logs'
New-Item -ItemType Directory -Path $taskLogs -Force | Out-Null
$taskStamp=Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$taskLog=Join-Path $taskLogs ($taskStamp+'.txt')
Start-Transcript -LiteralPath $taskLog | Out-Null
try {
    if(-not $PlanPath){$PlanPath=Join-Path $taskCache 'cleanup-plan.json'}
    $PlanPath=[IO.Path]::GetFullPath($PlanPath)
    if(-not $PlanPath.StartsWith($taskCache+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Plan must be inside this project cache.'}
    $taskPlan=Get-Content -LiteralPath $PlanPath -Encoding UTF8 -Raw | ConvertFrom-Json
    if([IO.Path]::GetFullPath($taskPlan.reviewedRoot).TrimEnd('\') -ne $taskRoot){throw 'Plan root differs from the reviewed source root.'}
    $taskApproval='6e32fc04ffcbc51736559ba61fc98141d6b63afde6c69318dd156d34ae32f605'
    $taskPlanTargets=@($taskPlan.targets)
    $taskHash=[Security.Cryptography.SHA256]::Create()
    try {$taskActual=[BitConverter]::ToString($taskHash.ComputeHash([Text.Encoding]::UTF8.GetBytes(($taskPlanTargets -join "`n")))).Replace('-','').ToLowerInvariant()}
    finally {$taskHash.Dispose()}
    if($taskPlanTargets.Count -ne 704 -or $taskActual -ne $taskApproval){throw 'Target list differs from the 704 entries reviewed by the user.'}
    $taskBuild=Get-Content -LiteralPath (Join-Path $taskCache 'last-build.json') -Encoding UTF8 -Raw | ConvertFrom-Json
    $taskClasses=[IO.Path]::GetFullPath($taskBuild.classes)
    if($taskClasses -notmatch ('^'+[regex]::Escape($taskCache)+'\\build-[a-fA-F0-9]{32}\\classes$')){throw 'Current build classes are outside the expected cache.'}
    $taskBuildName=Split-Path (Split-Path $taskClasses -Parent) -Leaf
    $taskKeepCache=@('manual-latest','compile-api',$taskBuildName,'last-build.json','backport-test-maps-1.16.5-1.4.4.jar','bop-mansion-registry.json','cleanup-plan.json','cleanup-result.json','cleanup-latest.json','cleanup-logs','latest-build-status.json')
    $taskKeepDist=@('pixelmon-content-backport-1.16.5-1.4.4.jar','1.4.4-离线测试包-最新.zip','1.4.4-离线测试包-最新.zip.sha256')
    $taskAllowedRoot=@('历史记录','发布记录','测试归档','logs','installer.log') | ForEach-Object {Join-Path $taskRoot $_}
    $taskSources=@('src','resources','tests','tools','docs','test-maps','pack-overrides','.git','.cache/manual-latest') | ForEach-Object {[IO.Path]::GetFullPath((Join-Path $taskRoot $_))}
    function Assert-NoReparseAncestor([string]$Path) {
        $taskWalk=[IO.Path]::GetFullPath($Path)
        while($taskWalk){
            if(Test-Path -LiteralPath $taskWalk){
                if((Get-Item -LiteralPath $taskWalk -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw ('Refused linked path component: '+$taskWalk)}
            }
            $taskNext=Split-Path $taskWalk -Parent
            if($taskNext -eq $taskWalk){break}
            $taskWalk=$taskNext
        }
    }
    Assert-NoReparseAncestor $taskRoot
    Assert-NoReparseAncestor $PlanPath
    if(@($taskPlan.sourceFiles).Count -lt 2000){throw 'Incomplete protected source snapshot.'}
    $taskTargets=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach($taskTarget in $taskPlan.targets){
        $taskFinal=[IO.Path]::GetFullPath($taskTarget).TrimEnd('\')
        $taskParent=Split-Path $taskFinal -Parent
        $taskLeaf=Split-Path $taskFinal -Leaf
        if(-not $taskFinal.StartsWith($taskRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw ('Refused target outside this project: '+$taskFinal)}
        $taskAllowed=($taskFinal -in $taskAllowedRoot) -or ($taskParent -eq $taskCache -and $taskLeaf -notin $taskKeepCache) -or ($taskParent -eq (Join-Path $taskRoot 'dist') -and $taskLeaf -notin $taskKeepDist -and $taskLeaf -notlike '*1.4.4*')
        if(-not $taskAllowed){throw ('Refused unapproved deletion target: '+$taskFinal)}
        foreach($taskSource in $taskSources){if($taskFinal -eq $taskSource -or $taskSource.StartsWith($taskFinal+'\',[StringComparison]::OrdinalIgnoreCase)){throw ('Refused source/runtime deletion: '+$taskFinal)}}
        Assert-NoReparseAncestor $taskFinal
        if(Test-Path -LiteralPath $taskFinal){
            $taskItem=Get-Item -LiteralPath $taskFinal -Force
            if($taskItem.Attributes -band [IO.FileAttributes]::ReparsePoint){throw ('Refused reparse point: '+$taskFinal)}
            if($taskItem.PSIsContainer){
                $taskLinks=@(Get-ChildItem -LiteralPath $taskFinal -Recurse -Force -Attributes ReparsePoint)
                if($taskLinks.Count){throw ('Refused target containing links/junctions: '+$taskFinal)}
            }
        }
        $null=$taskTargets.Add($taskFinal)
    }
    foreach($taskName in $taskKeepDist){
        $taskFile=Join-Path $taskRoot ('dist/'+$taskName)
        $taskExpected=@($taskPlan.keepFiles | Where-Object {$_.path -eq $taskFile})
        if($taskExpected.Count -ne 1 -or (Get-FileHash -LiteralPath $taskFile -Algorithm SHA256).Hash -ne $taskExpected[0].sha256){throw ('Latest test file changed or absent from plan: '+$taskFile)}
    }
    function Test-ReviewedSource {
        foreach($taskFile in $taskPlan.sourceFiles){
            $taskPath=[IO.Path]::GetFullPath((Join-Path $taskRoot $taskFile.path))
            if(-not $taskPath.StartsWith($taskRoot+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Source snapshot path escaped the project.'}
            if(-not(Test-Path -LiteralPath $taskPath) -or (Get-FileHash -LiteralPath $taskPath -Algorithm SHA256).Hash -ne $taskFile.sha256){throw ('Source changed since review; regenerate plan: '+$taskPath)}
        }
    }
    Test-ReviewedSource
    $taskJava=@(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'")
    foreach($taskProcess in $taskJava){foreach($taskTarget in $taskTargets){
        if($taskProcess.CommandLine -and $taskProcess.CommandLine.IndexOf($taskTarget,[StringComparison]::OrdinalIgnoreCase) -ge 0){throw ('A Java process still uses a cleanup target: PID '+$taskProcess.ProcessId)}
    }}
    $taskList=Join-Path $taskLogs ($taskStamp+'-targets.txt')
    $taskTargets | Sort-Object | Set-Content -LiteralPath $taskList -Encoding UTF8
    Write-Host ('Reviewed obsolete entries: '+$taskTargets.Count)
    Write-Host ('Complete deletion list: '+$taskList)
    Write-Host 'Keep: source, Git history, latest JAR/offline ZIP, current build and manual-latest client.'
    if(-not $Execute){Write-Host 'PREVIEW ONLY. No files deleted. Use -Execute for the approved cleanup.';return}
    $taskDeleted=[Collections.Generic.List[string]]::new()
    $taskBefore=(Get-PSDrive -Name D).Free
    $taskFailure=$null
    $taskSourceVerified=$false
    try {
        foreach($taskTarget in $taskTargets){
            if(-not(Test-Path -LiteralPath $taskTarget)){continue}
            Assert-NoReparseAncestor $taskTarget
            $taskCurrent=Get-Item -LiteralPath $taskTarget -Force
            if($taskCurrent.PSIsContainer -and @(Get-ChildItem -LiteralPath $taskTarget -Recurse -Force -Attributes ReparsePoint).Count){throw ('Target now contains a link/junction: '+$taskTarget)}
            if($PSCmdlet.ShouldProcess($taskTarget,'Permanently delete reviewed obsolete cache')){
                Remove-Item -LiteralPath $taskTarget -Recurse -Force -Confirm:$false -ErrorAction Stop
                $taskDeleted.Add($taskTarget)
                if($taskDeleted.Count%40 -eq 0){Write-Host ('Removed entries: '+$taskDeleted.Count)}
            }
        }
        Test-ReviewedSource
        $taskSourceVerified=$true
    } catch {$taskFailure=$_.Exception.Message}
    $taskResult=@{completedAt=(Get-Date -Format 'yyyy-MM-dd HH:mm:ss');targetListSHA256=$taskApproval;deletedEntries=$taskDeleted.Count;deletedPaths=$taskDeleted.ToArray();freedBytes=((Get-PSDrive -Name D).Free-$taskBefore);whatIf=[bool]$WhatIfPreference;sourceIntegrityChecked=$taskSourceVerified;error=$taskFailure}
    if(-not $WhatIfPreference){$taskResult | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $taskCache 'cleanup-result.json') -Encoding UTF8}
    Write-Host ('Cleanup finished. Removed entries: '+$taskDeleted.Count+'; source integrity checked: '+$taskSourceVerified)
    if($taskFailure){throw $taskFailure}
} catch {
    Write-Host ('STOPPED: '+$_.Exception.Message) -ForegroundColor Red
    Write-Host ('Details: '+$taskLog)
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
