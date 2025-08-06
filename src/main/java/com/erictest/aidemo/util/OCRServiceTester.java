package com.erictest.aidemo.util;

import com.erictest.aidemo.service.ImageRecognitionService;

/**
 * 測試更新後的 OCR 服務
 */
public class OCRServiceTester {

    public static void main(String[] args) {
        System.out.println("=== OCR 服務測試 ===");

        try {
            // 初始化 OCR 服務
            ImageRecognitionService ocrService = new ImageRecognitionService();
            System.out.println("✅ OCR 服務初始化成功！");

            // 創建一個小的測試圖片資料 (1x1 像素白色圖片)
            byte[] testImageData = createSimpleTestImage();

            // 測試 OCR 功能
            String testName = "測試";
            var result = ocrService.validateIdCard(testImageData, testName);

            System.out.println("📊 測試結果:");
            System.out.println("   - 提取文字長度: " + result.getExtractedText().length());
            System.out.println("   - 訊息: " + result.getMessage());
            System.out.println("✅ OCR 服務運作正常，新版本檔案載入成功！");

        } catch (Exception e) {
            System.err.println("❌ OCR 服務測試失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] createSimpleTestImage() {
        // 返回一個簡單的 1x1 像素白色 PNG 圖片的 byte array
        return new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0xDA, 0x63, (byte) 0xF8, 0x0F, 0x00,
            0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
