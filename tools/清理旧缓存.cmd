@echo off
setlocal
set "CLEANUP_PS=%~dp0cleanup-local.ps1"
set "CLEANUP_PWSH=%ProgramFiles%\PowerShell\7\pwsh.exe"
if exist "%CLEANUP_PWSH%" goto launch
where pwsh.exe >nul 2>&1
if errorlevel 1 goto missing
set "CLEANUP_PWSH=pwsh.exe"
:launch
echo PREVIEW ONLY. This launcher does not delete files.
"%CLEANUP_PWSH%" -NoLogo -NoProfile -NoExit -File "%CLEANUP_PS%"
set "CLEANUP_EXIT=%errorlevel%"
echo PowerShell exited with code %CLEANUP_EXIT%.
pause
exit /b %CLEANUP_EXIT%
:missing
echo PowerShell 7 was not found. No files deleted.
pause
exit /b 1
