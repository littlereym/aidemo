package com.erictest.aidemo.debug;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IDE 啟動模式調試工具
 */
@RestController
@RequestMapping("/debug")
public class IDEDebugController {

    @GetMapping("/environment")
    public Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> info = new HashMap<>();

        // 基本環境資訊
        info.put("currentDirectory", System.getProperty("user.dir"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion", System.getProperty("os.version"));

        // FFmpeg 路徑資訊
        String ffmpegPath = System.getProperty("ffmpeg.path");
        String ffmpegFullPath = System.getProperty("ffmpeg.full.path");
        info.put("ffmpegPath", ffmpegPath);
        info.put("ffmpegFullPath", ffmpegFullPath != null
                ? ffmpegFullPath.substring(0, Math.min(200, ffmpegFullPath.length())) + "..." : null);

        // 檢查關鍵目錄和檔案
        String currentDir = System.getProperty("user.dir");
        File ffmpegDir = new File(currentDir, "ffmpeg-7.1.1-essentials_build/bin");
        File ffmpegExe = new File(ffmpegDir, "ffmpeg.exe");
        File uploadsDir = new File(currentDir, "uploads/audio");

        info.put("ffmpegDirExists", ffmpegDir.exists());
        info.put("ffmpegExeExists", ffmpegExe.exists());
        info.put("uploadsDirExists", uploadsDir.exists());

        // PATH 環境變數
        String systemPath = System.getenv("PATH");
        info.put("systemPathContainsFFmpeg", systemPath != null && systemPath.contains("ffmpeg"));

        // 類路徑資訊
        info.put("classPath", System.getProperty("java.class.path").split(File.pathSeparator).length + " entries");

        return info;
    }

    @GetMapping("/whisper-check")
    public Map<String, Object> checkWhisperEnvironment() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 檢查 Python
            ProcessBuilder pb = new ProcessBuilder("py", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            result.put("pythonAvailable", finished && process.exitValue() == 0);

            // 檢查 Whisper
            pb = new ProcessBuilder("py", "-c", "import whisper; print(whisper.__version__)");
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            // 設定 FFmpeg 路徑
            String ffmpegFullPath = System.getProperty("ffmpeg.full.path");
            if (ffmpegFullPath != null) {
                pb.environment().put("PATH", ffmpegFullPath);
            }

            process = pb.start();
            finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            result.put("whisperAvailable", finished && process.exitValue() == 0);

            if (finished && process.exitValue() == 0) {
                // 讀取 Whisper 版本
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String version = reader.readLine();
                    result.put("whisperVersion", version);
                }
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    @GetMapping("/test-ffmpeg")
    public Map<String, Object> testFFmpeg() {
        Map<String, Object> result = new HashMap<>();

        try {
            String ffmpegPath = System.getProperty("ffmpeg.path");
            String ffmpegCommand;

            if (ffmpegPath != null) {
                ffmpegCommand = new File(ffmpegPath, "ffmpeg.exe").getAbsolutePath();
            } else {
                ffmpegCommand = "ffmpeg";
            }

            ProcessBuilder pb = new ProcessBuilder(ffmpegCommand, "-version");

            // 設定環境變數
            String ffmpegFullPath = System.getProperty("ffmpeg.full.path");
            if (ffmpegFullPath != null) {
                pb.environment().put("PATH", ffmpegFullPath);
            }

            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            result.put("ffmpegAvailable", finished && process.exitValue() == 0);
            result.put("ffmpegCommand", ffmpegCommand);

            if (finished && process.exitValue() == 0) {
                // 讀取 FFmpeg 版本資訊
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null && lineCount < 5) {
                        output.append(line).append("\n");
                        lineCount++;
                    }
                    result.put("ffmpegVersion", output.toString());
                }
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}
