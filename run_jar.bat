@echo off
chcp 65001 > nul
echo ================================
echo 🚀 語音識別系統 - JAR 執行版本
echo ================================

REM 檢查 JAR 檔案是否存在
echo 📁 檢查 JAR 檔案...
if not exist "target\aidemo-0.0.1-SNAPSHOT.jar" (
    echo ❌ 錯誤: 未找到 JAR 檔案！
    echo 💡 請先執行編譯: mvnw.cmd clean package -DskipTests
    pause
    exit /b 1
)
echo ✅ JAR 檔案存在

echo 📦 正在配置 FFmpeg 路徑...
set PATH=%PATH%;C:\project\aidemo\ffmpeg-7.1.1-essentials_build\bin
echo ✅ FFmpeg 路徑已設置: C:\project\aidemo\ffmpeg-7.1.1-essentials_build\bin

echo 📋 檢測 FFmpeg...
ffmpeg -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 錯誤: 找不到 FFmpeg！
    echo 💡 請確保 ffmpeg-7.1.1-essentials_build 資料夾存在
    pause
    exit /b 1
)
echo ✅ FFmpeg 檢測成功

echo 🐍 檢查 Python Whisper...
py -c "import whisper" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  警告: Whisper 不可用，語音識別功能將受限
    echo 💡 建議執行: pip install openai-whisper
) else (
    echo ✅ Python Whisper 正常
)

echo.
echo 🎯 正在啟動 Spring Boot JAR...
echo ⏰ 首次啟動需要預熱 Whisper 模型，請稍候...
echo 🌐 應用程序將在 http://localhost:8080 啟動
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
