@echo off
chcp 65001 > nul
echo ================================
echo 🚀 AI Demo 語音識別專案啟動器 (自動安裝版)
echo ================================
echo.

REM 檢查管理員權限
>nul 2>&1 "%SYSTEMROOT%\system32\cacls.exe" "%SYSTEMROOT%\system32\config\system"
if '%errorlevel%' NEQ '0' (
    echo 🔐 需要管理員權限進行安裝...
    echo 請右鍵點擊此檔案並選擇 "以系統管理員身分執行"
    pause
    exit /b 1
)

REM 檢查並安裝 Java
echo 🔍 檢查 Java 環境...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 找不到 Java！正在自動安裝...
    call :install_java
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ Java 安裝失敗！
        pause
        exit /b 1
    )
) else (
    echo ✅ Java 環境正常
)

REM 檢查並安裝 Python
echo 🐍 檢查 Python 環境...
py --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    python --version >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ 找不到 Python！正在自動安裝...
        call :install_python
        if %ERRORLEVEL% NEQ 0 (
            echo ❌ Python 安裝失敗！
            pause
            exit /b 1
        )
    )
) else (
    echo ✅ Python 環境正常
)

REM 檢查並安裝 Whisper
echo 🎤 檢查 Python Whisper...
py -c "import whisper; print('Whisper available')" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    python -c "import whisper; print('Whisper available')" >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ Whisper 不可用！正在自動安裝...
        call :install_whisper
        if %ERRORLEVEL% NEQ 0 (
            echo ❌ Whisper 安裝失敗！
            pause
            exit /b 1
        )
    ) else (
        echo ✅ Python Whisper 正常
    )
) else (
    echo ✅ Python Whisper 正常
)

REM 設置 FFmpeg 路徑
echo 📦 配置 FFmpeg...
set PATH=%PATH%;C:\project\aidemo\ffmpeg-7.1.1-essentials_build\bin
echo ✅ FFmpeg 路徑已設置

REM 檢查是否有編譯好的 JAR
echo 📁 檢查項目狀態...
if exist "target\aidemo-0.0.1-SNAPSHOT.jar" (
    echo ✅ 發現編譯好的 JAR 檔案
    goto :run_jar
) else (
    echo ⚠️  未找到 JAR 檔案，將進行編譯...
    goto :compile_and_run
)

:compile_and_run
echo 🔨 正在編譯項目...
echo ⏰ 這可能需要 30-60 秒...
call .\mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 編譯失敗！
    pause
    exit /b 1
)
echo ✅ 編譯完成

:run_jar
echo.
echo 🎯 啟動應用程序...
echo ⏰ 首次啟動需要下載 Whisper 模型，請耐心等待...
echo 🌐 應用程序將在 http://localhost:8080 啟動
echo.
echo 🛑 如何停止應用程序:
echo    1. 按 Ctrl+C (推薦方式)
echo    2. 關閉此終端窗口
echo    3. 新開終端執行: taskkill /F /IM java.exe
echo.
echo 按 Ctrl+C 停止應用程序
echo ================================
echo.

java -jar target\aidemo-0.0.1-SNAPSHOT.jar

echo.
echo ================================
echo 📊 應用程序已結束
echo ================================
pause
goto :eof

REM ==================== 安裝函式 ====================

:install_java
echo.
echo 🔽 正在下載並安裝 Java 17...
echo ⏰ 這可能需要幾分鐘，請耐心等待...
echo.

REM 創建臨時目錄
if not exist "%TEMP%\aidemo_install" mkdir "%TEMP%\aidemo_install"
cd /d "%TEMP%\aidemo_install"

REM 下載 Java 17 (Eclipse Temurin)
echo 📥 下載 Java 17 安裝檔...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi' -OutFile 'java17.msi' -UseBasicParsing}" >nul 2>&1

if not exist "java17.msi" (
    echo ❌ Java 下載失敗！請檢查網路連線
    cd /d "%~dp0"
    exit /b 1
)

echo 🔧 安裝 Java 17...
msiexec /i java17.msi /quiet /norestart
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java 安裝失敗！
    cd /d "%~dp0"
    exit /b 1
)

REM 清理臨時檔案
del /f /q java17.msi >nul 2>&1
cd /d "%~dp0"

echo ✅ Java 17 安裝完成！
echo 🔄 正在重新載入環境變數...
call refreshenv >nul 2>&1
timeout /t 3 >nul
exit /b 0

:install_python
echo.
echo 🔽 正在下載並安裝 Python...
echo ⏰ 這可能需要幾分鐘，請耐心等待...
echo.

REM 創建臨時目錄
if not exist "%TEMP%\aidemo_install" mkdir "%TEMP%\aidemo_install"
cd /d "%TEMP%\aidemo_install"

REM 下載 Python 3.11
echo 📥 下載 Python 3.11 安裝檔...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://www.python.org/ftp/python/3.11.7/python-3.11.7-amd64.exe' -OutFile 'python-installer.exe' -UseBasicParsing}" >nul 2>&1

if not exist "python-installer.exe" (
    echo ❌ Python 下載失敗！請檢查網路連線
    cd /d "%~dp0"
    exit /b 1
)

echo 🔧 安裝 Python 3.11...
python-installer.exe /quiet InstallAllUsers=1 PrependPath=1 Include_test=0
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Python 安裝失敗！
    cd /d "%~dp0"
    exit /b 1
)

REM 清理臨時檔案
del /f /q python-installer.exe >nul 2>&1
cd /d "%~dp0"

echo ✅ Python 3.11 安裝完成！
echo 🔄 正在重新載入環境變數...
call refreshenv >nul 2>&1
timeout /t 3 >nul
exit /b 0

:install_whisper
echo.
echo 🔽 正在安裝 OpenAI Whisper...
echo ⏰ 這可能需要幾分鐘，請耐心等待...
echo.

REM 升級 pip
echo 📦 升級 pip...
py -m pip install --upgrade pip >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    python -m pip install --upgrade pip >nul 2>&1
)

REM 安裝必要的依賴
echo 🔧 安裝相依套件...
py -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    python -m pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ PyTorch 安裝失敗！
        exit /b 1
    )
)

REM 安裝 Whisper
echo 🎤 安裝 OpenAI Whisper...
py -m pip install openai-whisper >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    python -m pip install openai-whisper >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        echo ❌ Whisper 安裝失敗！
        exit /b 1
    )
)

echo ✅ OpenAI Whisper 安裝完成！
exit /b 0
