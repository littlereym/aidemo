package com.erictest.aidemo.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erictest.aidemo.service.AudioConversionService;

/**
 * 音頻處理 API Controller
 */
@RestController
@RequestMapping("/speech/api")
public class AudioProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingController.class);

    @Autowired
    private AudioConversionService audioConversionService;

    /**
     * 將上傳的音頻檔案轉換為 MP3 格式
     */
    @PostMapping("/convert-to-mp3")
    public ResponseEntity<?> convertToMp3(@RequestParam("audioFile") MultipartFile audioFile) {
        Map<String, Object> response = new HashMap<>();
        File tempInputFile = null;
        File convertedFile = null;

        try {
            logger.info("🎵 收到音頻轉換請求: {} ({} bytes)",
                    audioFile.getOriginalFilename(), audioFile.getSize());

            // 驗證檔案
            if (audioFile.isEmpty()) {
                response.put("success", false);
                response.put("message", "檔案為空");
                return ResponseEntity.badRequest().body(response);
            }

            // 檢查檔案大小 (最大 50MB)
            if (audioFile.getSize() > 50 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "檔案大小超過 50MB 限制");
                return ResponseEntity.badRequest().body(response);
            }

            // 創建上傳目錄 - 使用絕對路徑避免 JAR 執行環境問題
            Path workingDir = Paths.get(System.getProperty("user.dir"));
            Path uploadDir = workingDir.resolve("uploads").resolve("audio");
            Files.createDirectories(uploadDir);

            // 保存上傳的檔案
            String originalFileName = audioFile.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String timestamp = String.valueOf(System.currentTimeMillis());

            tempInputFile = uploadDir.resolve("temp_" + timestamp + "." + fileExtension).toFile();
            audioFile.transferTo(tempInputFile);

            logger.info("📁 臨時檔案已保存: {}", tempInputFile.getName());

            // 轉換為 MP3
            String outputFileName = "converted_" + timestamp + ".mp3";
            convertedFile = audioConversionService.convertToMp3(tempInputFile, outputFileName);

            // 準備下載響應
            FileSystemResource resource = new FileSystemResource(convertedFile);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + outputFileName + "\"");
            headers.add("X-Converted-Size", String.valueOf(convertedFile.length()));
            headers.add("X-Original-Size", String.valueOf(audioFile.getSize()));

            logger.info("✅ 音頻轉換完成: {} -> {} ({} bytes)",
                    originalFileName, outputFileName, convertedFile.length());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.valueOf("audio/mpeg"))
                    .contentLength(convertedFile.length())
                    .body(resource);

        } catch (Exception e) {
            logger.error("❌ 音頻轉換失敗: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "音頻轉換失敗: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);

        } finally {
            // 清理臨時檔案
            if (tempInputFile != null) {
                audioConversionService.cleanupTempFile(tempInputFile);
            }

            // 注意：這裡不清理轉換後的檔案，因為正在被下載
            // 可以考慮使用定時任務清理舊檔案
        }
    }

    /**
     * 獲取檔案擴展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "tmp";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
