package com.erictest.aidemo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.erictest.aidemo.service.SphinxSpeechRecognitionService;
import com.erictest.aidemo.service.WhisperSpeechRecognitionService;

/**
 * 智能語音處理控制器 - 整合 OpenAI Whisper
 */
@Controller
@RequestMapping("/speech")
public class SimpleSpeechController {

    @Autowired
    private SphinxSpeechRecognitionService sphinxSpeechService;

    @Autowired
    private WhisperSpeechRecognitionService whisperSpeechService;

    // 設定音頻檔案上傳目錄
    private static final String UPLOAD_DIR = "uploads/audio/";

    /**
     * 顯示語音處理頁面
     */
    @GetMapping("/demo")
    public String speechDemo() {
        return "speech-demo";
    }

    /**
     * 顯示 Sphinx 引擎資訊
     */
    @GetMapping("/engine-info")
    @ResponseBody
    public Map<String, Object> getEngineInfo() {
        Map<String, Object> response = new HashMap<>();

        // 初始化 Sphinx 服務（如果尚未初始化）
        if (!sphinxSpeechService.isReady()) {
            sphinxSpeechService.initialize();
        }

        response.put("success", true);
        response.put("engineInfo", sphinxSpeechService.getEngineInfo());
        response.put("recommendations", sphinxSpeechService.getAudioRecommendations());
        response.put("isReady", sphinxSpeechService.isReady());

        return response;
    }

    /**
     * 測試API - 返回固定的測試數據
     */
    @PostMapping("/api/test-speech-to-text")
    @ResponseBody
    public Map<String, Object> testSpeechToText() {
        Map<String, Object> response = new HashMap<>();
        System.out.println("📝 收到測試語音轉文字請求");

        response.put("success", true);
        response.put("message", "語音轉文字完成");
        response.put("data", Map.of(
                "recognizedText", "這是測試識別結果：Hello World! 這是語音轉文字的測試內容。",
                "fileName", "test_audio.mp3",
                "fileSize", 123456L,
                "confidence", 0.98
        ));

        System.out.println("🎯 返回測試數據：" + response);
        return response;
    }

    /**
     * 語音轉文字 API - 使用 OpenAI Whisper 引擎
     */
    @PostMapping("/api/speech-to-text")
    @ResponseBody
    public Map<String, Object> speechToText(@RequestParam("audioFile") MultipartFile audioFile) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("🎤 收到語音轉文字請求，檔案名稱: " + audioFile.getOriginalFilename());
        System.out.println("📊 檔案大小: " + audioFile.getSize() + " bytes");
        System.out.println("📋 檔案類型: " + audioFile.getContentType());

        try {
            // 建立上傳目錄
            createUploadDirectoryIfNotExists();

            // 驗證音頻檔案
            String validationResult = validateAudioFile(audioFile);
            if (validationResult != null) {
                System.out.println("❌ 檔案驗證失敗: " + validationResult);
                response.put("success", false);
                response.put("message", validationResult);
                return response;
            }
            System.out.println("✅ 檔案驗證通過");

            // 保存音頻檔案
            String fileName = generateFileName("audio", audioFile.getOriginalFilename());
            File savedFile = saveFileAndGetFile(audioFile, fileName);
            System.out.println("💾 檔案已保存: " + fileName);

            // 優先使用 OpenAI Whisper 進行語音識別
            Map<String, Object> recognitionResult;
            try {
                recognitionResult = whisperSpeechService.recognizeFromFile(savedFile);
                System.out.println("🤖 Whisper 識別結果: " + recognitionResult);
            } catch (Exception whisperError) {
                System.out.println("⚠️ Whisper 服務不可用，回退到 Sphinx: " + whisperError.getMessage());
                recognitionResult = sphinxSpeechService.recognizeFromFile(savedFile);
                System.out.println("🔤 Sphinx 識別結果: " + recognitionResult);
            }

            if ((Boolean) recognitionResult.get("success")) {
                String engineName = (String) recognitionResult.getOrDefault("engine", "語音識別引擎");

                response.put("success", true);
                response.put("message", "語音轉文字完成 (" + engineName + ")");
                response.put("data", Map.of(
                        "recognizedText", recognitionResult.get("recognizedText"),
                        "fileName", fileName,
                        "fileSize", audioFile.getSize(),
                        "confidence", recognitionResult.get("confidence"),
                        "engine", engineName,
                        "audioInfo", recognitionResult.getOrDefault("audioInfo", new HashMap<>()),
                        "processingTime", recognitionResult.getOrDefault("processingTime", "N/A"),
                        "whisperDetails", recognitionResult.getOrDefault("whisperDetails", new HashMap<>()),
                        "note", recognitionResult.getOrDefault("note", "")
                ));
            } else {
                // 如果所有服務都失敗，回退到基礎模擬
                String recognizedText = simulateSpeechToText(audioFile);
                response.put("success", true);
                response.put("message", "語音轉文字完成 (基礎模式)");
                response.put("data", Map.of(
                        "recognizedText", recognizedText,
                        "fileName", fileName,
                        "fileSize", audioFile.getSize(),
                        "confidence", 0.75,
                        "engine", "基礎模擬引擎"
                ));
            }

            System.out.println("✅ 語音轉文字處理完成，準備返回結果");

        } catch (Exception e) {
            System.err.println("❌ 語音轉文字處理失敗: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "語音轉文字失敗：" + e.getMessage());
        }

        return response;
    }

    /**
     * 文字轉語音 API
     */
    @PostMapping("/api/text-to-speech")
    @ResponseBody
    public Map<String, Object> textToSpeech(@RequestParam("text") String text,
            @RequestParam(value = "language", defaultValue = "zh-TW") String language,
            @RequestParam(value = "voice", defaultValue = "female") String voice) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 驗證文字輸入
            if (text == null || text.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "請輸入要轉換的文字");
                return response;
            }

            if (text.length() > 5000) {
                response.put("success", false);
                response.put("message", "文字長度不能超過5000字元");
                return response;
            }

            // 建立上傳目錄
            createUploadDirectoryIfNotExists();

            // 模擬文字轉語音
            String audioFileName = simulateTextToSpeech(text, language, voice);

            response.put("success", true);
            response.put("message", "文字轉語音完成");
            response.put("data", Map.of(
                    "audioUrl", "/speech/audio/" + audioFileName,
                    "fileName", audioFileName,
                    "text", text,
                    "language", language,
                    "voice", voice,
                    "duration", Math.min(text.length() * 0.1, 60) // 模擬音頻長度
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "文字轉語音失敗：" + e.getMessage());
        }

        return response;
    }

    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    private String validateAudioFile(MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return "❌ 請選擇音頻檔案";
        }

        long maxSize = 10 * 1024 * 1024; // 10MB
        if (audioFile.getSize() > maxSize) {
            return "❌ 音頻檔案過大（超過 10MB）";
        }

        String contentType = audioFile.getContentType();
        if (contentType == null
                || (!contentType.equals("audio/wav")
                && !contentType.equals("audio/mp3")
                && !contentType.equals("audio/mpeg")
                && !contentType.equals("audio/ogg")
                && !contentType.equals("audio/webm")
                && !contentType.startsWith("audio/"))) {
            return "❌ 請上傳有效的音頻檔案（WAV、MP3、OGG 等格式）";
        }

        return null;
    }

    private String generateFileName(String prefix, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFilename);
        return String.format("%s_%s%s", prefix, timestamp, extension);
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.lastIndexOf(".") > 0) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".wav";
    }

    private void saveFile(MultipartFile file, String fileName) throws IOException {
        Path targetLocation = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 保存檔案並返回 File 對象供 Sphinx 使用
     */
    private File saveFileAndGetFile(MultipartFile file, String fileName) throws IOException {
        Path targetLocation = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return targetLocation.toFile();
    }

    private String simulateSpeechToText(MultipartFile audioFile) {
        String fileName = audioFile.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().contains("hello")) {
            return "您好，歡迎使用語音識別功能！";
        } else if (fileName != null && fileName.toLowerCase().contains("test")) {
            return "這是一個語音轉文字的測試。";
        } else {
            return "這是一段示例語音內容，實際使用時會調用 Google Cloud Speech-to-Text API 進行識別。您可以說任何內容，系統會將其轉換為文字。";
        }
    }

    private String simulateTextToSpeech(String text, String language, String voice) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String audioFileName = String.format("tts_%s_%s_%s.mp3", language, voice, timestamp);

        // 創建一個簡單的HTML音頻提示檔案
        String htmlContent = String.format(
                """
            <!DOCTYPE html>
            <html>
            <head><title>文字轉語音模擬</title></head>
            <body>
                <h3>模擬的文字轉語音結果</h3>
                <p><strong>原始文字：</strong> %s</p>
                <p><strong>語言：</strong> %s</p>
                <p><strong>語音類型：</strong> %s</p>
                <p style="color: #666;">實際部署時，這裡會是 Google Cloud 生成的真實音頻檔案</p>
                <script>
                    // 使用瀏覽器的語音合成API作為演示
                    const utterance = new SpeechSynthesisUtterance('%s');
                    utterance.lang = '%s';
                    speechSynthesis.speak(utterance);
                </script>
            </body>
            </html>
            """,
                text.replace("'", "\\'"), language, voice, text.replace("'", "\\'"),
                language.equals("zh-TW") ? "zh-CN" : "en-US"
        );

        // 保存HTML檔案到音頻目錄
        Path audioPath = Paths.get(UPLOAD_DIR + audioFileName + ".html");
        Files.write(audioPath, htmlContent.getBytes());

        return audioFileName;
    }
}
