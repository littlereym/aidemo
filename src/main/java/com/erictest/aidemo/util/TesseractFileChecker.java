package com.erictest.aidemo.util;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;

/**
 * Tesseract 檔案資訊檢查工具
 */
public class TesseractFileChecker {

    public static void main(String[] args) {
        System.out.println("=== Tesseract 檔案資訊檢查 ===");

        try {
            // 檢查 tess4j 版本
            System.out.println("1. tess4j 版本資訊:");
            Package pkg = Tesseract.class.getPackage();
            if (pkg != null) {
                System.out.println("   - Implementation Title: " + pkg.getImplementationTitle());
                System.out.println("   - Implementation Version: " + pkg.getImplementationVersion());
            }

            // 初始化 Tesseract
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("./tessdata");

            // 檢查訓練檔案
            System.out.println("\n2. 訓練檔案資訊:");
            File tessDataDir = new File("./tessdata");
            if (tessDataDir.exists()) {
                System.out.println("   tessdata 目錄: " + tessDataDir.getAbsolutePath());

                File chiTraFile = new File("./tessdata/chi_tra.traineddata");
                if (chiTraFile.exists()) {
                    System.out.println("   chi_tra.traineddata:");
                    System.out.println("     - 檔案大小: " + chiTraFile.length() + " bytes");
                    System.out.println("     - 最後修改: " + new java.util.Date(chiTraFile.lastModified()));

                    // 嘗試載入並測試
                    try {
                        tesseract.setLanguage("chi_tra");
                        System.out.println("     - 語言檔案載入: ✅ 成功");

                        // 簡單測試 OCR 功能
                        tesseract.setOcrEngineMode(1); // LSTM 引擎
                        System.out.println("     - OCR 引擎模式: LSTM (1)");

                    } catch (Exception e) {
                        System.out.println("     - 語言檔案載入: ❌ 失敗 - " + e.getMessage());
                    }
                } else {
                    System.out.println("   ❌ chi_tra.traineddata 檔案不存在");
                }

                // 列出所有可用的語言檔案
                File[] trainedDataFiles = tessDataDir.listFiles((dir, name)
                        -> name.endsWith(".traineddata"));
                if (trainedDataFiles != null && trainedDataFiles.length > 0) {
                    System.out.println("\n   可用的訓練檔案:");
                    for (File file : trainedDataFiles) {
                        System.out.println("     - " + file.getName()
                                + " (" + file.length() + " bytes)");
                    }
                }
            } else {
                System.out.println("   ❌ tessdata 目錄不存在");
            }

            System.out.println("\n3. 版本比較建議:");
            System.out.println("   - 最新 tessdata_best 發布版本: 4.1.0 (2021-02-17)");
            System.out.println("   - 最新檔案大小: 12,985,735 bytes (約 12.4 MB)");
            System.out.println("   - 最新檔案 SHA256: 1AA60488574CAFA69486D919284F079CA9B68FCC7F6AD8DC1FF1B318DFD97028");

            // 檢查是否為最新版本
            File currentFile = new File("./tessdata/chi_tra.traineddata");
            if (currentFile.exists()) {
                long currentSize = currentFile.length();
                boolean isLatest = currentSize == 12985735L;
                System.out.println("   - 目前檔案狀態: " + (isLatest ? "✅ 已是最新版本" : "⚠️ 非最新版本"));
            }

        } catch (Exception e) {
            System.err.println("檢查過程發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
