@echo off
chcp 65001 > nul
echo ================================
echo 🔨 AI Demo 專案編譯工具
echo ================================
echo.

REM 檢查 Java 環境
echo 🔍 檢查 Java 環境...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 錯誤: 找不到 Java！
    echo 💡 請安裝 Java 17 或更高版本
    pause
    exit /b 1
)
echo ✅ Java 環境正常

REM 顯示編譯選項
echo 📋 編譯選項:
echo [1] 快速編譯 (跳過測試)
echo [2] 完整編譯 (包含測試)
echo [3] 清理重新編譯
echo [4] 只清理不編譯
echo.
set /p choice=請選擇編譯方式 (1-4): 

if "%choice%"=="1" goto :quick_compile
if "%choice%"=="2" goto :full_compile
if "%choice%"=="3" goto :clean_compile
if "%choice%"=="4" goto :clean_only
echo ❌ 無效選擇，使用預設快速編譯
goto :quick_compile

:quick_compile
echo 🚀 執行快速編譯...
call .\mvnw.cmd clean package -DskipTests
goto :compile_done

:full_compile
echo 🧪 執行完整編譯（包含測試）...
call .\mvnw.cmd clean package
goto :compile_done

:clean_compile
echo 🧹 清理並重新編譯...
call .\mvnw.cmd clean
call .\mvnw.cmd package -DskipTests
goto :compile_done

:clean_only
echo 🧹 只清理不編譯...
call .\mvnw.cmd clean
echo ✅ 清理完成
goto :end

:compile_done
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 編譯失敗！
    echo 💡 請檢查錯誤訊息並修正代碼
    pause
    exit /b 1
)

echo.
echo ✅ 編譯成功！
echo 📦 JAR 檔案位置: target\aidemo-0.0.1-SNAPSHOT.jar
echo.
echo 🎯 現在可以執行:
echo    .\run_jar.bat    - 直接運行 JAR
echo    .\run_app.bat    - 智能啟動器
echo.

:end
pause
