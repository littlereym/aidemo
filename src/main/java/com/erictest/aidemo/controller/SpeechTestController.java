package com.erictest.aidemo.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erictest.aidemo.service.WhisperSpeechRecognitionService;

/**
 * 語音識別功能測試控制器
 */
@RestController
@RequestMapping("/test")
public class SpeechTestController {

    @Autowired
    private WhisperSpeechRecognitionService whisperService;

    /**
     * 測試語音識別功能 使用項目中現有的測試音頻檔案
     */
    @GetMapping("/speech-recognition")
    public ResponseEntity<Map<String, Object>> testSpeechRecognition() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🎯 開始語音識別功能測試...");

            // 查找測試音頻檔案
            String testAudioPath = findTestAudioFile();
            if (testAudioPath == null) {
                result.put("success", false);
                result.put("message", "未找到測試音頻檔案");
                return ResponseEntity.ok(result);
            }

            System.out.println("📁 找到測試檔案: " + testAudioPath);

            // 記錄開始時間
            long startTime = System.currentTimeMillis();

            // 執行語音識別
            File audioFile = new File(testAudioPath);
            Map<String, Object> recognitionResult = whisperService.recognizeFromFile(audioFile);

            // 記錄結束時間
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 提取識別結果
            String transcription = (String) recognitionResult.get("transcription");
            Boolean success = (Boolean) recognitionResult.get("success");

            System.out.println("✅ 語音識別測試完成");
            System.out.println("⏱️ 處理時間: " + duration + "ms");
            System.out.println("📝 識別結果: " + transcription);
            System.out.println("🎯 識別狀態: " + (success ? "成功" : "失敗"));

            result.put("success", success);
            result.put("audioFile", testAudioPath);
            result.put("transcription", transcription);
            result.put("processingTime", duration + "ms");
            result.put("recognitionDetails", recognitionResult);
            result.put("message", success ? "語音識別測試成功" : "語音識別測試失敗");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ 語音識別測試失敗: " + e.getMessage());
            e.printStackTrace();

            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "語音識別測試失敗");

            return ResponseEntity.ok(result);
        }
    }

    /**
     * 查找可用的測試音頻檔案
     */
    private String findTestAudioFile() {
        // 優先順序: MP3 -> WAV -> 其他
        String[] possiblePaths = {
            "test_file.mp3",
            "uploads/audio",
            "test_file.wav"
        };

        for (String path : possiblePaths) {
            File file = new File(path);

            if (file.isFile() && file.exists()) {
                return file.getAbsolutePath();
            } else if (file.isDirectory() && file.exists()) {
                // 在目錄中查找音頻檔案
                File[] audioFiles = file.listFiles((dir, name)
                        -> name.toLowerCase().endsWith(".mp3")
                        || name.toLowerCase().endsWith(".wav")
                        || name.toLowerCase().endsWith(".m4a")
                );

                if (audioFiles != null && audioFiles.length > 0) {
                    return audioFiles[0].getAbsolutePath();
                }
            }
        }

        return null;
    }

    /**
     * 測試系統狀態
     */
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> testSystemStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🔍 檢查系統狀態...");

            // 檢查 FFmpeg
            String ffmpegPath = System.getProperty("ffmpeg.path");
            boolean ffmpegAvailable = ffmpegPath != null && new File(ffmpegPath, "ffmpeg.exe").exists();

            // 檢查上傳目錄
            File uploadsDir = new File("uploads/audio");
            boolean uploadsReady = uploadsDir.exists() && uploadsDir.isDirectory();

            // 檢查測試檔案
            String testFile = findTestAudioFile();
            boolean testFileAvailable = testFile != null;

            result.put("ffmpeg", Map.of(
                    "available", ffmpegAvailable,
                    "path", ffmpegPath != null ? ffmpegPath : "未設置"
            ));

            result.put("uploads", Map.of(
                    "ready", uploadsReady,
                    "path", uploadsDir.getAbsolutePath()
            ));

            result.put("testFile", Map.of(
                    "available", testFileAvailable,
                    "path", testFile != null ? testFile : "未找到"
            ));

            boolean allReady = ffmpegAvailable && uploadsReady && testFileAvailable;
            result.put("allSystemsReady", allReady);
            result.put("message", allReady ? "所有系統組件正常" : "部分系統組件需要檢查");

            System.out.println(allReady ? "✅ 系統狀態檢查完成 - 正常" : "⚠️ 系統狀態檢查完成 - 有問題");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ 系統狀態檢查失敗: " + e.getMessage());
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
