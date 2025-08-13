@echo off
REM 重新載入環境變數
REM 這個檔案用於在安裝軟體後重新載入系統環境變數

REM 重新讀取註冊表中的環境變數
for /f "skip=2 tokens=3*" %%a in ('reg query HKLM\SYSTEM\CurrentControlSet\Control\Session" "Manager\Environment /v PATH') do set SysPath=%%a %%b
for /f "skip=2 tokens=3*" %%a in ('reg query HKCU\Environment /v PATH') do set UserPath=%%a %%b

REM 設置新的 PATH
if defined UserPath (
    set "PATH=%SysPath%;%UserPath%"
) else (
    set "PATH=%SysPath%"
)

REM 重新載入其他重要的環境變數
for /f "skip=2 tokens=3*" %%a in ('reg query HKLM\SYSTEM\CurrentControlSet\Control\Session" "Manager\Environment /v JAVA_HOME 2^>nul') do set JAVA_HOME=%%a %%b
for /f "skip=2 tokens=3*" %%a in ('reg query HKCU\Environment /v JAVA_HOME 2^>nul') do set JAVA_HOME=%%a %%b

echo 環境變數已重新載入
