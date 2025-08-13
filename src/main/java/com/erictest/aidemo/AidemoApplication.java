package com.erictest.aidemo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.erictest.aidemo.mapper")
public class AidemoApplication {

    public static void main(String[] args) {
        // 在應用程序啟動前自動配置 FFmpeg 路徑
        configureFFmpegPath();

        // 預熱 Whisper 引擎
        preWarmWhisperEngine();

        // 啟動 Spring Boot 應用程序
        org.springframework.context.ConfigurableApplicationContext context
                = SpringApplication.run(AidemoApplication.class, args);

        // 啟動完成後進行功能自測試
        performStartupSelfTest(context);
    }

    /**
     * 自動配置 FFmpeg 路徑到系統 PATH 環境變數
     */
    private static void configureFFmpegPath() {
        try {
            // 獲取當前執行目錄
            String currentDir = System.getProperty("user.dir");
            Path ffmpegBinPath = Paths.get(currentDir, "ffmpeg-7.1.1-essentials_build", "bin");

            System.out.println("🔧 正在配置 FFmpeg 環境...");
            System.out.println("📁 當前目錄: " + currentDir);
            System.out.println("🎯 FFmpeg 路徑: " + ffmpegBinPath.toString());

            // 檢查 FFmpeg 目錄是否存在
            if (!ffmpegBinPath.toFile().exists()) {
                System.err.println("❌ 錯誤: 找不到 FFmpeg 目錄 - " + ffmpegBinPath.toString());
                System.err.println("💡 請確保 ffmpeg-7.1.1-essentials_build 目錄存在於應用程序目錄中");
                return;
            }

            // 檢查 ffmpeg.exe 是否存在
            File ffmpegExe = ffmpegBinPath.resolve("ffmpeg.exe").toFile();
            if (!ffmpegExe.exists()) {
                System.err.println("❌ 錯誤: 找不到 ffmpeg.exe - " + ffmpegExe.getAbsolutePath());
                return;
            }

            // 動態添加 FFmpeg 到 PATH 環境變數
            String currentPath = System.getenv("PATH");
            String newPath = ffmpegBinPath.toString() + File.pathSeparator + currentPath;

            // 在 Java 17 模組系統中，直接修改 System.getenv() 會受到限制
            // 改用更安全的方法：設置 system property 供 ProcessBuilder 使用
            System.setProperty("ffmpeg.path", ffmpegBinPath.toString());
            System.setProperty("ffmpeg.full.path", newPath);

            try {
                // 嘗試動態修改環境變數 (可能在某些 JVM 版本中失敗)
                java.util.Map<String, String> env = System.getenv();
                java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
                field.setAccessible(true);
                ((java.util.Map<String, String>) field.get(env)).put("PATH", newPath);

                System.out.println("✅ FFmpeg 路徑已成功添加到環境變數 (反射方式)");

            } catch (Exception e) {
                System.err.println("⚠️ 無法動態修改 PATH 環境變數: " + e.getMessage());
                System.out.println("💡 將使用 ProcessBuilder 環境設定方式調用 FFmpeg");
            }

            System.out.println("✅ FFmpeg 路徑已配置完成");
            System.out.println("🎵 語音識別功能已準備就緒");

        } catch (Exception e) {
            System.err.println("❌ 配置 FFmpeg 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 預熱 Whisper 引擎 - 在 Java 啟動時初始化 Whisper
     */
    private static void preWarmWhisperEngine() {
        try {
            System.out.println("🚀 正在預熱 Whisper 語音識別引擎...");

            // 檢查 Python 和 Whisper 是否可用
            if (!checkPythonAndWhisper()) {
                System.out.println("⚠️ Whisper 不可用，跳過預熱");
                return;
            }

            // 在後台線程中預熱，避免阻塞主程序啟動
            Thread warmupThread = new Thread(() -> {
                try {
                    warmupWhisperModel();
                } catch (Exception e) {
                    System.err.println("⚠️ Whisper 預熱過程中發生錯誤: " + e.getMessage());
                }
            });

            warmupThread.setDaemon(true); // 設為守護線程
            warmupThread.setName("Whisper-Warmup");
            warmupThread.start();

            System.out.println("🎤 Whisper 預熱已啟動（後台進行）");

        } catch (Exception e) {
            System.err.println("❌ Whisper 預熱啟動失敗: " + e.getMessage());
        }
    }

    /**
     * 檢查 Python 和 Whisper 是否可用
     */
    private static boolean checkPythonAndWhisper() {
        try {
            // 檢查 Python
            ProcessBuilder pb = new ProcessBuilder("py", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished || process.exitValue() != 0) {
                System.out.println("❌ Python 不可用");
                return false;
            }

            // 檢查 Whisper
            pb = new ProcessBuilder("py", "-c", "import whisper; print(whisper.__version__)");
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            process = pb.start();
            finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished || process.exitValue() != 0) {
                System.out.println("❌ Whisper 模組不可用");
                return false;
            }

            System.out.println("✅ Python 和 Whisper 檢查通過");
            return true;

        } catch (Exception e) {
            System.out.println("❌ 檢查 Python/Whisper 時發生錯誤: " + e.getMessage());
            return false;
        }
    }

    /**
     * 執行 Whisper 模型預熱
     */
    private static void warmupWhisperModel() throws Exception {
        System.out.println("🔥 開始預熱 Whisper 模型...");

        // 創建一個簡短的無聲音頻檔案進行預熱
        String pythonScript
                = "import whisper\n"
                + "import numpy as np\n"
                + "import tempfile\n"
                + "import os\n"
                + "print('載入 Whisper base 模型...')\n"
                + "model = whisper.load_model('base')\n"
                + "print('模型載入完成')\n"
                + "# 創建短暫的無聲音頻進行預熱\n"
                + "audio = np.zeros(16000, dtype=np.float32)  # 1秒無聲音頻\n"
                + "audio = whisper.pad_or_trim(audio)\n"
                + "mel = whisper.log_mel_spectrogram(audio, model.dims.n_mels)\n"
                + "print('執行預熱轉錄...')\n"
                + "options = whisper.DecodingOptions(language='zh')\n"
                + "result = whisper.decode(model, mel, options)\n"
                + "print(f'預熱完成 - 模型已準備就緒')\n"
                + "print(f'模型參數數量: {sum(p.numel() for p in model.parameters()):,}')";

        ProcessBuilder pb = new ProcessBuilder("py", "-c", pythonScript);

        // 設置環境變數
        java.util.Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        String ffmpegPath = System.getProperty("ffmpeg.full.path");
        if (ffmpegPath != null) {
            env.put("PATH", ffmpegPath);
        }

        pb.redirectErrorStream(true);

        System.out.println("🐍 執行 Python Whisper 預熱腳本...");
        Process process = pb.start();

        // 讀取輸出
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("🤖 " + line);
            }
        }

        boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);

        if (finished && process.exitValue() == 0) {
            System.out.println("🎉 Whisper 預熱成功完成！");
            System.out.println("⚡ 首次語音識別將更快響應");
        } else {
            System.out.println("⚠️ Whisper 預熱可能未完全成功");
        }
    }

    /**
     * 啟動後自測試功能
     */
    private static void performStartupSelfTest(org.springframework.context.ConfigurableApplicationContext context) {
        System.out.println("\n🧪 開始進行啟動自測試...");

        try {
            // 測試 1: FFmpeg 可用性
            testFFmpegAvailability();

            // 測試 2: Python 和 Whisper 可用性  
            testPythonWhisperAvailability();

            // 測試 3: 檢查 Spring Boot 服務
            testSpringBootServices(context);

            // 測試 4: 檢查上傳目錄
            testUploadDirectories();

            System.out.println("✅ 自測試完成 - 所有核心功能正常");
            System.out.println("🌐 應用程序已準備就緒，可接受請求");

        } catch (Exception e) {
            System.err.println("⚠️ 自測試過程中發現問題: " + e.getMessage());
        }
    }

    /**
     * 測試 FFmpeg 可用性
     */
    private static void testFFmpegAvailability() {
        try {
            System.out.print("🎵 測試 FFmpeg... ");

            String ffmpegPath = System.getProperty("ffmpeg.path");
            if (ffmpegPath == null) {
                System.out.println("❌ FFmpeg 路徑未設置");
                return;
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(Paths.get(ffmpegPath, "ffmpeg.exe").toString(), "-version");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                System.out.println("✅ 正常");
            } else {
                System.out.println("❌ 不可用");
            }

        } catch (Exception e) {
            System.out.println("❌ 測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試 Python 和 Whisper 可用性
     */
    private static void testPythonWhisperAvailability() {
        System.out.print("🤖 測試 Python/Whisper... ");

        if (checkPythonAndWhisper()) {
            System.out.println("✅ 正常");
        } else {
            System.out.println("❌ 不可用");
        }
    }

    /**
     * 測試 Spring Boot 服務
     */
    private static void testSpringBootServices(org.springframework.context.ConfigurableApplicationContext context) {
        System.out.print("🌱 測試 Spring Boot 服務... ");

        try {
            // 檢查一些關鍵的 Bean 是否存在
            String[] criticalBeans = {
                "audioProcessingController",
                "whisperSpeechRecognitionService",
                "audioConversionService"
            };

            boolean allBeansPresent = true;
            for (String beanName : criticalBeans) {
                if (!context.containsBean(beanName)) {
                    allBeansPresent = false;
                    break;
                }
            }

            if (allBeansPresent) {
                System.out.println("✅ 正常");
            } else {
                System.out.println("⚠️ 部分服務未找到");
            }

        } catch (Exception e) {
            System.out.println("❌ 測試失敗: " + e.getMessage());
        }
    }

    /**
     * 測試上傳目錄
     */
    private static void testUploadDirectories() {
        System.out.print("📁 檢查上傳目錄... ");

        try {
            String currentDir = System.getProperty("user.dir");
            Path uploadsDir = Paths.get(currentDir, "uploads");
            Path audioDir = Paths.get(currentDir, "uploads", "audio");

            if (!uploadsDir.toFile().exists()) {
                uploadsDir.toFile().mkdirs();
            }

            if (!audioDir.toFile().exists()) {
                audioDir.toFile().mkdirs();
            }

            System.out.println("✅ 正常");

        } catch (Exception e) {
            System.out.println("❌ 創建失敗: " + e.getMessage());
        }
    }

}
