package com.erictest.aidemo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 音頻格式轉換服務 - 使用 FFmpeg
 */
@Service
public class AudioConversionService {

    private static final Logger logger = LoggerFactory.getLogger(AudioConversionService.class);

    /**
     * 將音頻檔案轉換為 MP3 格式
     */
    public File convertToMp3(File inputFile, String outputFileName) throws IOException, InterruptedException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("輸入檔案不存在: " + (inputFile != null ? inputFile.getPath() : "null"));
        }

        logger.info("🎵 開始音頻格式轉換: {} -> MP3", inputFile.getName());

        // 準備輸出檔案路徑
        File outputFile = new File(inputFile.getParent(), outputFileName);

        try {
            // 構建 FFmpeg 命令
            ProcessBuilder pb = buildFFmpegCommand(inputFile, outputFile);

            logger.debug("執行 FFmpeg 命令: {}", String.join(" ", pb.command()));

            // 執行轉換
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg 轉換超時");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("FFmpeg 轉換失敗，退出碼: " + exitCode);
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("轉換後的檔案不存在或為空");
            }

            logger.info("✅ 音頻轉換成功: {} ({} bytes)", outputFile.getName(), outputFile.length());
            return outputFile;

        } catch (Exception e) {
            logger.error("❌ 音頻轉換失敗: {}", e.getMessage());

            // 清理可能的部分輸出檔案
            if (outputFile.exists()) {
                try {
                    Files.delete(outputFile.toPath());
                } catch (Exception cleanupEx) {
                    logger.warn("清理失敗的輸出檔案時發生錯誤: {}", cleanupEx.getMessage());
                }
            }

            throw e;
        }
    }

    /**
     * 構建 FFmpeg 命令
     */
    private ProcessBuilder buildFFmpegCommand(File inputFile, File outputFile) {
        ProcessBuilder pb;

        // 檢查是否有自定義 FFmpeg 路徑
        String ffmpegPath = System.getProperty("ffmpeg.path");
        String ffmpegCommand;

        if (ffmpegPath != null) {
            // 使用絕對路徑
            ffmpegCommand = Paths.get(ffmpegPath, "ffmpeg.exe").toString();
            logger.debug("使用自定義 FFmpeg 路徑: {}", ffmpegCommand);
        } else {
            // 使用系統 PATH
            ffmpegCommand = "ffmpeg";
            logger.debug("使用系統 PATH 中的 FFmpeg");
        }

        pb = new ProcessBuilder(
                ffmpegCommand,
                "-i", inputFile.getAbsolutePath(), // 輸入檔案
                "-acodec", "mp3", // 音頻編碼器
                "-ar", "44100", // 取樣率 44.1kHz
                "-ac", "1", // 單聲道 (語音識別通常使用單聲道)
                "-b:a", "128k", // 位元率 128kbps
                "-f", "mp3", // 輸出格式
                "-y", // 覆蓋現有檔案
                outputFile.getAbsolutePath() // 輸出檔案
        );

        // 設置環境變數
        if (ffmpegPath != null) {
            String currentPath = pb.environment().get("PATH");
            pb.environment().put("PATH", ffmpegPath + File.pathSeparator + (currentPath != null ? currentPath : ""));
        }

        // 重定向錯誤流以便於調試
        pb.redirectErrorStream(true);

        return pb;
    }

    /**
     * 清理臨時檔案
     */
    public boolean cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
                logger.debug("🗑️ 已清理臨時檔案: {}", file.getName());
                return true;
            } catch (Exception e) {
                logger.warn("清理臨時檔案失敗: {} - {}", file.getName(), e.getMessage());
                return false;
            }
        }
        return true;
    }
}
