@echo off
setlocal
set "CLEANUP_PS=%~dp0cleanup-local.ps1"
set "CLEANUP_PWSH=%ProgramFiles%\PowerShell\7\pwsh.exe"
if exist "%CLEANUP_PWSH%" goto launch
where pwsh.exe >nul 2>&1
if errorlevel 1 goto missing
set "CLEANUP_PWSH=pwsh.exe"
:launch
echo REAL CLEANUP. Only the 704 reviewed entries inside this project are eligible.
echo The external release cache is excluded. Source and current release files are protected.
"%CLEANUP_PWSH%" -NoLogo -NoProfile -NoExit -File "%CLEANUP_PS%" -Execute
set "CLEANUP_EXIT=%errorlevel%"
echo PowerShell exited with code %CLEANUP_EXIT%.
pause
exit /b %CLEANUP_EXIT%
:missing
echo PowerShell 7 was not found. No files deleted.
pause
exit /b 1
