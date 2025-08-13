let mediaRecorder;
let audioChunks = [];
let isRecording = false;

// 當頁面載入完成後初始化
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
});

function initializeEventListeners() {
    // 檔案選擇處理
    document.getElementById('audioFile').addEventListener('change', handleFileSelect);

    // 文字計數
    document.getElementById('textInput').addEventListener('input', handleTextInput);

    // 清空文字按鈕
    document.getElementById('clearTextBtn').addEventListener('click', clearText);

    // 錄音功能
    document.getElementById('recordBtn').addEventListener('click', toggleRecording);

    // 測試按鈕
    document.getElementById('testBtn').addEventListener('click', testSpeechToText);

    // 表單提交
    document.getElementById('speechToTextForm').addEventListener('submit', handleSpeechToText);
    document.getElementById('textToSpeechForm').addEventListener('submit', handleTextToSpeech);

    // 拖拽上傳
    initializeDragAndDrop();
}

function handleFileSelect(e) {
    const label = document.getElementById('audioFileLabel');
    const file = e.target.files[0];
    if (file) {
        label.textContent = `📁 已選擇：${file.name}`;
        label.classList.add('active');
    } else {
        label.textContent = '🎵 點擊選擇音頻檔案或拖拽檔案到這裡';
        label.classList.remove('active');
    }
}

function handleTextInput(e) {
    const charCount = e.target.value.length;
    document.getElementById('charCount').textContent = charCount;

    if (charCount > 5000) {
        e.target.style.borderColor = '#dc3545';
        document.getElementById('charCount').style.color = '#dc3545';
    } else {
        e.target.style.borderColor = '#e1e5e9';
        document.getElementById('charCount').style.color = '#666';
    }
}

function clearText() {
    document.getElementById('textInput').value = '';
    document.getElementById('charCount').textContent = '0';
    hideResult('ttsResult');
}

async function toggleRecording() {
    const recordBtn = document.getElementById('recordBtn');

    if (!isRecording) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

            // 檢查支援的音頻格式，優先選擇相容性最好的
            let options = {};
            if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
                options.mimeType = 'audio/webm;codecs=opus';
            } else if (MediaRecorder.isTypeSupported('audio/webm')) {
                options.mimeType = 'audio/webm';
            } else if (MediaRecorder.isTypeSupported('audio/ogg;codecs=opus')) {
                options.mimeType = 'audio/ogg;codecs=opus';
            } else if (MediaRecorder.isTypeSupported('audio/mp4')) {
                options.mimeType = 'audio/mp4';
            }

            console.log('使用的錄音格式：', options.mimeType || 'browser default');

            mediaRecorder = new MediaRecorder(stream, options);
            audioChunks = [];

            mediaRecorder.ondataavailable = event => {
                audioChunks.push(event.data);
            };

            mediaRecorder.onstop = async () => {
                // 使用實際的 MediaRecorder 格式，而不是強制 WAV
                const mimeType = mediaRecorder.mimeType || 'audio/webm';
                let fileName = 'recording.webm';

                // 根據實際格式設定檔案名
                if (mimeType.includes('webm')) {
                    fileName = 'recording.webm';
                } else if (mimeType.includes('ogg')) {
                    fileName = 'recording.ogg';
                } else if (mimeType.includes('mp4')) {
                    fileName = 'recording.mp4';
                }

                console.log('錄音完成，格式：', mimeType, '檔案名：', fileName);

                const audioBlob = new Blob(audioChunks, { type: mimeType });
                const originalFile = new File([audioBlob], fileName, { type: mimeType });

                // 更新標籤顯示轉換中狀態
                const label = document.getElementById('audioFileLabel');
                label.textContent = `🔄 正在轉換為 MP3...`;
                label.classList.add('active');

                try {
                    // 將錄音檔案發送到後端轉換為 MP3
                    const mp3File = await convertToMp3(originalFile);

                    // 創建一個新的 FileList
                    const dataTransfer = new DataTransfer();
                    dataTransfer.items.add(mp3File);
                    document.getElementById('audioFile').files = dataTransfer.files;

                    // 更新標籤
                    label.textContent = `🎙️ 已錄製並轉換：${mp3File.name} (${formatFileSize(mp3File.size)})`;

                } catch (error) {
                    console.error('MP3 轉換失敗:', error);
                    alert('錄音轉換失敗: ' + error.message);

                    // 回退到原始檔案
                    const dataTransfer = new DataTransfer();
                    dataTransfer.items.add(originalFile);
                    document.getElementById('audioFile').files = dataTransfer.files;

                    label.textContent = `🎙️ 已錄製（原格式）：${originalFile.name} (${formatFileSize(originalFile.size)})`;
                }

                stream.getTracks().forEach(track => track.stop());
            }; mediaRecorder.start();
            isRecording = true;
            recordBtn.textContent = '⏹️ 停止錄音';
            recordBtn.classList.add('recording');
        } catch (error) {
            alert('無法存取麥克風：' + error.message);
        }
    } else {
        mediaRecorder.stop();
        isRecording = false;
        recordBtn.textContent = '🎙️ 開始錄音';
        recordBtn.classList.remove('recording');
    }
}

async function testSpeechToText() {
    console.log('🧪 測試按鈕被點擊');
    showLoading('sttLoading');
    hideResult('sttResult');

    try {
        const response = await fetch('/speech/api/test-speech-to-text', {
            method: 'POST'
        });

        console.log('測試API響應狀態：', response.status);
        const result = await response.json();
        console.log('測試API結果：', result);

        hideLoading('sttLoading');

        if (result.success) {
            console.log('準備顯示測試結果');
            const displayContent = createSpeechResultHTML(result.data, true);
            showResult('sttResult', 'success', displayContent);
        } else {
            showResult('sttResult', 'error', `<p>❌ ${result.message}</p>`);
        }
    } catch (error) {
        console.error('測試請求失敗：', error);
        hideLoading('sttLoading');
        showResult('sttResult', 'error', `<p>❌ 測試失敗：${error.message}</p>`);
    }
}

async function handleSpeechToText(e) {
    e.preventDefault();

    const audioFile = document.getElementById('audioFile').files[0];
    if (!audioFile) {
        alert('請選擇音頻檔案');
        return;
    }

    console.log('開始語音轉文字處理，檔案：', audioFile.name);

    const formData = new FormData();
    formData.append('audioFile', audioFile);

    showLoading('sttLoading');
    hideResult('sttResult');

    try {
        console.log('發送請求到 /speech/api/speech-to-text');
        const response = await fetch('/speech/api/speech-to-text', {
            method: 'POST',
            body: formData
        });

        console.log('收到響應，狀態：', response.status);

        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        const result = await response.json();
        console.log('API 響應結果：', result);

        hideLoading('sttLoading');

        if (result.success) {
            console.log('語音轉文字成功，顯示結果');
            console.log('識別文字：', result.data.recognizedText);

            const displayContent = createSpeechResultHTML(result.data, false);
            showResult('sttResult', 'success', displayContent);
        } else {
            console.log('語音轉文字失敗：', result.message);
            showResult('sttResult', 'error', `<p>❌ ${result.message}</p>`);
        }
    } catch (error) {
        console.error('請求失敗：', error);
        hideLoading('sttLoading');
        showResult('sttResult', 'error', `
            <p>❌ 請求失敗：${error.message}</p>
            <p style="color: #666; font-size: 0.9em; margin-top: 10px;">
                請檢查網路連線或聯繫系統管理員
            </p>
        `);
    }
}

async function handleTextToSpeech(e) {
    e.preventDefault();

    const text = document.getElementById('textInput').value;
    const language = document.getElementById('language').value;
    const voice = document.getElementById('voice').value;

    if (!text.trim()) {
        alert('請輸入要轉換的文字');
        return;
    }

    if (text.length > 5000) {
        alert('文字長度不能超過5000字元');
        return;
    }

    const formData = new FormData();
    formData.append('text', text);
    formData.append('language', language);
    formData.append('voice', voice);

    showLoading('ttsLoading');
    hideResult('ttsResult');

    try {
        const response = await fetch('/speech/api/text-to-speech', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        hideLoading('ttsLoading');

        if (result.success) {
            const displayContent = createTextToSpeechResultHTML(result.data);
            showResult('ttsResult', 'success', displayContent);

            // 添加播放按鈕事件監聽器
            setupPlayButton(result.data);
        } else {
            showResult('ttsResult', 'error', `<p>❌ ${result.message}</p>`);
        }
    } catch (error) {
        hideLoading('ttsLoading');
        showResult('ttsResult', 'error', `<p>❌ 請求失敗：${error.message}</p>`);
    }
}

function createSpeechResultHTML(data, isTest) {
    const title = isTest ? '🧪 測試結果' : '🎯 語音識別結果';
    return `
        <div style="background: #d4edda; padding: 20px; border-radius: 10px; margin: 15px 0; border: 2px solid #28a745;">
            <h4 style="color: #155724; margin-bottom: 15px;">${title}</h4>
            <div style="background: white; padding: 15px; border-radius: 8px; margin-bottom: 15px;">
                <p style="margin: 0; font-size: 1.2em; color: #333; line-height: 1.5;">
                    <strong>識別內容：</strong><br>
                    ${data.recognizedText}
                </p>
            </div>
            <div style="background: rgba(255,255,255,0.7); padding: 10px; border-radius: 5px;">
                <p style="margin: 5px 0; color: #155724;">
                    📁 檔案：${data.fileName}
                </p>
                <p style="margin: 5px 0; color: #155724;">
                    📊 大小：${(data.fileSize / 1024).toFixed(2)} KB
                </p>
                <p style="margin: 5px 0; color: #155724;">
                    🎯 信心度：${(data.confidence * 100).toFixed(1)}%
                </p>
            </div>
        </div>
    `;
}

function createTextToSpeechResultHTML(data) {
    const playButtonId = 'playBtn_' + Date.now();
    return `
        <div style="background: #e7e3ff; padding: 20px; border-radius: 10px; margin: 15px 0; border: 2px solid #764ba2;">
            <h4 style="color: #4a2c6a; margin-bottom: 15px;">🎵 語音生成成功！</h4>
            <div style="background: white; padding: 15px; border-radius: 8px; margin-bottom: 15px;">
                <p style="margin: 0 0 10px 0; font-size: 1.1em; color: #333;">
                    <strong>原文字：</strong>
                </p>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 5px; font-size: 1.1em; line-height: 1.6; color: #495057;">
                    ${data.text}
                </div>
            </div>
            <div style="background: rgba(255,255,255,0.7); padding: 15px; border-radius: 8px; margin-bottom: 15px;">
                <div style="display: flex; gap: 20px; flex-wrap: wrap;">
                    <p style="margin: 0; color: #4a2c6a;">📢 語言：${data.language}</p>
                    <p style="margin: 0; color: #4a2c6a;">🎭 語音：${data.voice}</p>
                    <p style="margin: 0; color: #4a2c6a;">⏱️ 預計：${data.duration.toFixed(1)}秒</p>
                </div>
            </div>
            <div style="text-align: center; margin-top: 20px;">
                <button id="${playButtonId}" style="background: linear-gradient(45deg, #667eea, #764ba2); color: white; border: none; padding: 15px 30px; border-radius: 50px; font-size: 1.1em; cursor: pointer; box-shadow: 0 4px 15px rgba(102,126,234,0.3); transition: all 0.3s ease;">
                    🔊 播放語音
                </button>
            </div>
            <p style="margin-top: 15px; padding: 10px; background: rgba(255,193,7,0.1); border-radius: 5px; color: #856404; font-size: 0.9em; text-align: center;">
                💡 演示版本使用瀏覽器內建語音合成，實際部署時會使用 Google Cloud Text-to-Speech API
            </p>
        </div>
    `;
}

function setupPlayButton(data) {
    setTimeout(() => {
        // 找到最新創建的播放按鈕
        const playBtns = document.querySelectorAll('[id^="playBtn_"]');
        const playBtn = playBtns[playBtns.length - 1]; // 獲取最後一個（最新的）按鈕
        if (playBtn) {
            playBtn.addEventListener('click', function () {
                playTextToSpeech(data.text, data.language, this);
            });
        }
    }, 100);
}

function playTextToSpeech(text, language, button) {
    if ('speechSynthesis' in window) {
        // 先停止任何正在播放的語音
        speechSynthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);

        // 設定語言
        if (language === 'zh-TW') {
            utterance.lang = 'zh-TW';
        } else if (language === 'en-US') {
            utterance.lang = 'en-US';
        }

        // 設定語音參數
        utterance.rate = 0.9; // 語速
        utterance.pitch = 1.0; // 音調
        utterance.volume = 0.8; // 音量

        // 改變按鈕狀態
        button.innerHTML = '🔄 播放中...';
        button.disabled = true;

        // 播放完成後重置按鈕
        utterance.onend = function () {
            button.innerHTML = '🔊 播放語音';
            button.disabled = false;
        };

        // 播放語音
        speechSynthesis.speak(utterance);

        console.log('🔊 開始播放語音：', text);
    } else {
        alert('您的瀏覽器不支援語音合成功能');
    }
}

function initializeDragAndDrop() {
    const fileLabel = document.getElementById('audioFileLabel');

    fileLabel.addEventListener('dragover', function (e) {
        e.preventDefault();
        this.style.borderColor = '#667eea';
        this.style.background = '#f0f4ff';
    });

    fileLabel.addEventListener('dragleave', function (e) {
        e.preventDefault();
        this.style.borderColor = '#dee2e6';
        this.style.background = '#f8f9fa';
    });

    fileLabel.addEventListener('drop', function (e) {
        e.preventDefault();
        this.style.borderColor = '#dee2e6';
        this.style.background = '#f8f9fa';

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            document.getElementById('audioFile').files = files;
            this.textContent = `📁 已選擇：${files[0].name}`;
            this.classList.add('active');
        }
    });
}

// 工具函數
function showLoading(id) {
    console.log('顯示載入動畫：', id);
    const loadingEl = document.getElementById(id);
    if (loadingEl) {
        loadingEl.style.display = 'block';
    } else {
        console.error('找不到載入元素：', id);
    }
}

function hideLoading(id) {
    console.log('隱藏載入動畫：', id);
    const loadingEl = document.getElementById(id);
    if (loadingEl) {
        loadingEl.style.display = 'none';
    } else {
        console.error('找不到載入元素：', id);
    }
}

function showResult(id, type, content) {
    console.log('顯示結果：', { id, type, contentLength: content.length });
    const resultEl = document.getElementById(id);
    const contentEl = document.getElementById(id.replace('Result', 'Content'));

    if (!resultEl) {
        console.error('找不到結果元素：', id);
        return;
    }

    if (!contentEl) {
        console.error('找不到內容元素：', id.replace('Result', 'Content'));
        return;
    }

    // 強制顯示結果區域
    resultEl.style.display = 'block';
    resultEl.style.visibility = 'visible';
    resultEl.style.opacity = '1';
    resultEl.className = `result-section result-${type} show`;

    // 設置內容
    contentEl.innerHTML = content;

    // 確保內容可見
    contentEl.style.display = 'block';
    contentEl.style.visibility = 'visible';
    contentEl.style.opacity = '1';

    // 滾動到結果區域
    resultEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

    console.log('結果已顯示，元素狀態：', {
        resultDisplay: resultEl.style.display,
        resultClass: resultEl.className,
        contentDisplay: contentEl.style.display,
        contentHTML: contentEl.innerHTML.substring(0, 100) + '...'
    });
}

function hideResult(id) {
    console.log('隱藏結果：', id);
    const resultEl = document.getElementById(id);
    if (resultEl) {
        resultEl.style.display = 'none';
    } else {
        console.error('找不到結果元素：', id);
    }
}

// 格式化檔案大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 將音頻檔案轉換為 MP3 格式
async function convertToMp3(audioFile) {
    return new Promise((resolve, reject) => {
        const formData = new FormData();
        formData.append('audioFile', audioFile);

        fetch('/speech/api/convert-to-mp3', {
            method: 'POST',
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`轉換失敗: HTTP ${response.status}`);
                }

                // 檢查是否返回檔案
                const contentType = response.headers.get('content-type');
                if (!contentType || !contentType.includes('audio/mpeg')) {
                    throw new Error('伺服器沒有返回正確的 MP3 檔案');
                }

                return response.blob();
            })
            .then(blob => {
                // 創建 MP3 檔案對象
                const timestamp = new Date().getTime();
                const mp3File = new File([blob], `recording_${timestamp}.mp3`, { type: 'audio/mpeg' });

                console.log('MP3 轉換成功:', mp3File.name, mp3File.size, 'bytes');
                resolve(mp3File);
            })
            .catch(error => {
                console.error('MP3 轉換失敗:', error);
                reject(error);
            });
    });
}
