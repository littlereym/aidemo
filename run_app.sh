#!/bin/bash
# Ubuntu 版本的 AI Demo 自動安裝腳本

set -e  # 遇到錯誤立即退出

echo "================================"
echo "🚀 AI Demo 語音識別專案啟動器 (Ubuntu 版)"
echo "================================"
echo ""

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 檢查 sudo 權限
echo "🔍 檢查權限..."
if [ "$(id -u)" = "0" ]; then
    echo -e "${GREEN}✅ 以 root 身分運行，跳過 sudo 檢查${NC}"
    SUDO_CMD=""
elif sudo -n true 2>/dev/null; then
    echo -e "${GREEN}✅ sudo 權限正常${NC}"
    SUDO_CMD="sudo"
else
    echo -e "${YELLOW}🔐 需要 sudo 權限進行安裝...${NC}"
    echo "請輸入您的密碼以繼續安裝"
    sudo -v
    SUDO_CMD="sudo"
fi
echo -e "${GREEN}✅ 權限檢查完成${NC}"

# 更新系統套件
echo "📦 更新系統套件..."
${SUDO_CMD} apt-get update -y

# 檢查並安裝 Java
install_java() {
    echo -e "${BLUE}🔽 正在安裝 Java 17...${NC}"
    ${SUDO_CMD} apt-get install -y openjdk-17-jdk
    echo -e "${GREEN}✅ Java 17 安裝完成！${NC}"
    
    # 設置 JAVA_HOME
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
    echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
}

# 檢查並安裝 Python
install_python() {
    echo -e "${BLUE}🔽 正在安裝 Python 3.11...${NC}"
    ${SUDO_CMD} apt-get install -y software-properties-common
    ${SUDO_CMD} add-apt-repository -y ppa:deadsnakes/ppa
    ${SUDO_CMD} apt-get update -y
    ${SUDO_CMD} apt-get install -y python3.11 python3.11-pip python3.11-venv python3.11-dev
    
    # 創建 python 和 pip 的符號連結
    ${SUDO_CMD} update-alternatives --install /usr/bin/python python /usr/bin/python3.11 1
    ${SUDO_CMD} update-alternatives --install /usr/bin/pip pip /usr/bin/pip3.11 1
    
    echo -e "${GREEN}✅ Python 3.11 安裝完成！${NC}"
}

# 檢查並安裝 Whisper
install_whisper() {
    echo -e "${BLUE}🔽 正在安裝 OpenAI Whisper...${NC}"
    
    # 安裝系統依賴
    ${SUDO_CMD} apt-get install -y ffmpeg git
    
    # 升級 pip
    pip install --upgrade pip
    
    # 安裝 PyTorch (CPU 版本)
    pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
    
    # 安裝 Whisper
    pip install openai-whisper
    
    echo -e "${GREEN}✅ OpenAI Whisper 安裝完成！${NC}"
}

# 安裝 Maven
install_maven() {
    echo -e "${BLUE}🔽 正在安裝 Maven...${NC}"
    ${SUDO_CMD} apt-get install -y maven
    echo -e "${GREEN}✅ Maven 安裝完成！${NC}"
}

# 檢查 Java 環境
echo "🔍 檢查 Java 環境..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ 找不到 Java！正在自動安裝...${NC}"
    install_java
else
    echo -e "${GREEN}✅ Java 環境正常${NC}"
    java -version
fi

# 檢查 Python 環境
echo "🐍 檢查 Python 環境..."
if ! command -v python &> /dev/null && ! command -v python3 &> /dev/null; then
    echo -e "${RED}❌ 找不到 Python！正在自動安裝...${NC}"
    install_python
else
    echo -e "${GREEN}✅ Python 環境正常${NC}"
    python --version || python3 --version
fi

# 檢查 Whisper
echo "🎤 檢查 Python Whisper..."
if ! python -c "import whisper" 2>/dev/null && ! python3 -c "import whisper" 2>/dev/null; then
    echo -e "${RED}❌ Whisper 不可用！正在自動安裝...${NC}"
    install_whisper
else
    echo -e "${GREEN}✅ Python Whisper 正常${NC}"
fi

# 檢查 Maven
echo "🔧 檢查 Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ 找不到 Maven！正在自動安裝...${NC}"
    install_maven
else
    echo -e "${GREEN}✅ Maven 環境正常${NC}"
    mvn -version
fi

# 設置 FFmpeg 路徑（如果存在）
if [ -d "ffmpeg-7.1.1-essentials_build/bin" ]; then
    echo "📦 配置 FFmpeg..."
    export PATH="$PATH:$(pwd)/ffmpeg-7.1.1-essentials_build/bin"
    echo -e "${GREEN}✅ FFmpeg 路徑已設置${NC}"
else
    echo "📦 使用系統 FFmpeg..."
fi

# 檢查項目狀態
echo "📁 檢查項目狀態..."
if [ -f "target/aidemo-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}✅ 發現編譯好的 JAR 檔案${NC}"
    run_jar=true
else
    echo -e "${YELLOW}⚠️  未找到 JAR 檔案，將進行編譯...${NC}"
    run_jar=false
fi

# 編譯項目
if [ "$run_jar" = false ]; then
    echo "🔨 正在編譯項目..."
    echo "⏰ 這可能需要 30-60 秒..."
    
    # 使用 Maven Wrapper 或系統 Maven
    if [ -f "./mvnw" ]; then
        chmod +x ./mvnw
        ./mvnw clean package -DskipTests
    else
        mvn clean package -DskipTests
    fi
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ 編譯失敗！${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ 編譯完成${NC}"
fi

echo ""
echo "🎯 啟動應用程序..."
echo "⏰ 首次啟動需要下載 Whisper 模型，請耐心等待..."
echo "🌐 應用程序將在 http://localhost:8080 啟動"
echo ""
echo "🛑 如何停止應用程序:"
echo "   1. 按 Ctrl+C (推薦方式)"
echo "   2. 關閉此終端窗口"
echo "   3. 新開終端執行: pkill -f 'java.*aidemo'"
echo ""
echo "按 Ctrl+C 停止應用程序"
echo "================================"
echo ""

# 啟動應用
java -jar target/aidemo-0.0.1-SNAPSHOT.jar

echo ""
echo "================================"
echo "📊 應用程序已結束"
echo "================================"
