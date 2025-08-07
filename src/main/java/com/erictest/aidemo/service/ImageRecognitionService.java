package com.erictest.aidemo.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * 圖像識別服務 - 身分證檢測與驗證
 */
@Service
public class ImageRecognitionService {

    private final Tesseract tesseract;

    public ImageRecognitionService() {
        // 初始化 Tesseract OCR
        tesseract = new Tesseract();

        // 設定 tessdata 路徑
        tesseract.setDatapath("./tessdata");

        // 優化 OCR 設定
        try {
            // 設定語言 (先嘗試繁體中文，失敗則用英文)
            tesseract.setLanguage("chi_tra+eng");
            tesseract.setOcrEngineMode(1); // 使用 LSTM OCR 引擎
            tesseract.setPageSegMode(3);   // 自動頁面分割，但不使用 OSD

            // 針對身分證和 UTF-8 編碼優化的設定
            tesseract.setVariable("tessedit_char_blacklist", "");
            tesseract.setVariable("preserve_interword_spaces", "1");
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("textord_min_linesize", "2.5");

            // 確保 UTF-8 輸出
            tesseract.setVariable("tessedit_write_unlv", "0");
            tesseract.setVariable("tessedit_create_boxfile", "0");

            // 改善中文辨識
            tesseract.setVariable("chop_enable", "1");
            tesseract.setVariable("use_new_state_cost", "1");
            tesseract.setVariable("segment_segcost_rating", "1");
            tesseract.setVariable("enable_new_segsearch", "1");

            System.out.println("✅ Tesseract OCR 初始化成功 - 語言: chi_tra+eng");
        } catch (Exception e) {
            System.err.println("⚠️ 中文語言檔案載入失敗，改用英文: " + e.getMessage());
            try {
                tesseract.setLanguage("eng");
                System.out.println("✅ Tesseract OCR 初始化成功 - 語言: eng");
            } catch (Exception e2) {
                System.err.println("❌ OCR 初始化完全失敗: " + e2.getMessage());
            }
        }
    }

    /**
     * 檢測身分證圖片並驗證
     */
    public ImageValidationResult validateIdCard(byte[] imageData, String expectedName) {
        try {
            // 1. 檢測圖片基本屬性
            boolean isValidImage = checkImageProperties(imageData);

            // 2. OCR 文字識別
            String extractedText = extractTextFromImage(imageData);

            // 3. 驗證姓名
            boolean nameMatches = verifyName(extractedText, expectedName);

            // 4. 檢測是否為身分證格式
            boolean isIdCardFormat = detectIdCardFormat(extractedText);

            return new ImageValidationResult(
                    isValidImage,
                    nameMatches,
                    isIdCardFormat,
                    extractedText,
                    generateValidationMessage(isValidImage, nameMatches, isIdCardFormat)
            );

        } catch (Exception e) {
            return new ImageValidationResult(
                    false, false, false, "",
                    "圖像處理失敗: " + e.getMessage()
            );
        }
    }

    /**
     * 只驗證身分證正面的姓名區域 - 提高準確率
     */
    public ImageValidationResult validateNameRegionOnly(byte[] imageData, String expectedName) {
        try {
            // 1. 檢測圖片基本屬性
            boolean isValidImage = checkImageProperties(imageData);

            // 2. 從圖片中提取姓名區域並進行 OCR
            String extractedNameText = extractNameRegion(imageData);

            // 3. 驗證姓名
            boolean nameMatches = verifyName(extractedNameText, expectedName);

            System.out.println("=== 姓名區域驗證 ===");
            System.out.println("預期姓名: " + expectedName);
            System.out.println("提取的姓名區域文字: " + extractedNameText);
            System.out.println("姓名匹配結果: " + nameMatches);
            System.out.println("==================");

            return new ImageValidationResult(
                    isValidImage,
                    nameMatches,
                    true, // 假設圖片格式正確（因為只檢查姓名區域）
                    extractedNameText,
                    generateNameValidationMessage(isValidImage, nameMatches, extractedNameText)
            );

        } catch (Exception e) {
            System.err.println("❌ 姓名區域驗證失敗: " + e.getMessage());
            return new ImageValidationResult(
                    false, false, false, "",
                    "姓名區域驗證失敗: " + e.getMessage()
            );
        }
    }

    /**
     * 從身分證正面圖片中提取姓名區域文字
     */
    private String extractNameRegion(byte[] imageData) {
        try {
            // 將 byte[] 轉換為 BufferedImage
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                throw new IOException("無法讀取圖像");
            }

            // 計算姓名區域的座標 (基於台灣身分證標準位置)
            // 姓名區域通常位於左上部分：X約18%, Y約45%, 寬度約35%, 高度約13%
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            int nameX = (int) (imageWidth * 0.18);
            int nameY = (int) (imageHeight * 0.45);
            int nameWidth = (int) (imageWidth * 0.35);
            int nameHeight = (int) (imageHeight * 0.13);

            // 確保座標不超出圖片邊界
            nameX = Math.max(0, Math.min(nameX, imageWidth - 1));
            nameY = Math.max(0, Math.min(nameY, imageHeight - 1));
            nameWidth = Math.max(1, Math.min(nameWidth, imageWidth - nameX));
            nameHeight = Math.max(1, Math.min(nameHeight, imageHeight - nameY));

            // 提取姓名區域
            BufferedImage nameRegion = image.getSubimage(nameX, nameY, nameWidth, nameHeight);

            // 對姓名區域進行預處理以提高 OCR 準確度
            BufferedImage processedNameRegion = preprocessImage(nameRegion);

            // 使用 Tesseract 對姓名區域進行 OCR
            String nameText = tesseract.doOCR(processedNameRegion);

            // 清理提取的文字
            String cleanedNameText = cleanupEncodingIssues(nameText).trim();

            System.out.println("=== 姓名區域提取 ===");
            System.out.println("圖片尺寸: " + imageWidth + "x" + imageHeight);
            System.out.println("姓名區域: (" + nameX + "," + nameY + ") " + nameWidth + "x" + nameHeight);
            System.out.println("提取的原始文字: " + nameText);
            System.out.println("清理後的文字: " + cleanedNameText);
            System.out.println("==================");

            return cleanedNameText;

        } catch (TesseractException | IOException e) {
            System.err.println("❌ 姓名區域 OCR 處理失敗: " + e.getMessage());
            return "";
        }
    }

    /**
     * 為姓名區域驗證生成驗證訊息
     */
    private String generateNameValidationMessage(boolean isValidImage, boolean nameMatches, String extractedText) {
        if (!isValidImage) {
            return "圖片格式或比例不符合身分證標準";
        }

        if (nameMatches) {
            return "✅ 姓名驗證成功";
        } else {
            if (extractedText.isEmpty()) {
                return "❌ 無法從姓名區域識別出文字，請確保圖片清晰";
            } else {
                return "❌ 姓名不符，識別到：" + extractedText;
            }
        }
    }

    /**
     * 檢測圖片基本屬性 (台灣身分證比例檢查)
     */
    private boolean checkImageProperties(byte[] imageData) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                return false;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // 移除最小尺寸限制，只檢查比例
            // 檢查寬高比例 (台灣身分證標準比例為 85.6mm x 54mm ≈ 1.585:1)
            double ratio = (double) width / height;
            boolean validRatio = Math.abs(ratio - 1.585) <= 0.5; // 放寬比例容忍度

            return validRatio;

        } catch (IOException e) {
            System.err.println("圖片屬性檢測失敗: " + e.getMessage());
            return false;
        }
    }

    /**
     * 從圖像中提取文字
     */
    private String extractTextFromImage(byte[] imageData) {
        try {
            // 將 byte[] 轉換為 BufferedImage
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                throw new IOException("無法讀取圖像");
            }

            // 圖片預處理 - 提高 OCR 準確度
            BufferedImage processedImage = preprocessImage(image);

            // 使用 Tesseract 進行 OCR
            String text = tesseract.doOCR(processedImage);

            // UTF-8 編碼清理處理
            String cleanedText = cleanupEncodingIssues(text);
            String trimmedText = cleanedText.trim();

            // 如果結果太差，嘗試用不同設定再試一次
            if (trimmedText.length() < 5 || isLikelyGarbage(trimmedText)) {
                System.out.println("⚠️ 第一次 OCR 結果不佳，嘗試備用設定...");
                String backupText = performBackupOCR(processedImage);
                String cleanedBackupText = cleanupEncodingIssues(backupText);
                if (cleanedBackupText.trim().length() > trimmedText.length()) {
                    text = backupText;
                    trimmedText = cleanedBackupText.trim();
                }

                // 如果備用設定也不好，嘗試第三種設定
                if (trimmedText.length() < 5 || isLikelyGarbage(trimmedText)) {
                    System.out.println("⚠️ 備用設定也不佳，嘗試第三種設定...");
                    String thirdText = performThirdOCR(processedImage);
                    String cleanedThirdText = cleanupEncodingIssues(thirdText);
                    if (cleanedThirdText.trim().length() > trimmedText.length()) {
                        text = thirdText;
                        trimmedText = cleanedThirdText.trim();
                    }
                }
            }

            // 調試輸出：顯示提取到的文字
            System.out.println("=== OCR 提取結果 ===");
            System.out.println("圖片尺寸: " + image.getWidth() + "x" + image.getHeight());
            System.out.println("原始文字長度: " + text.length());
            System.out.println("原始文字 (前200字): " + (text.length() > 200 ? text.substring(0, 200) + "..." : text));
            System.out.println("清理後文字: " + trimmedText);
            System.out.println("清理後文字長度: " + trimmedText.length());

            // 檢查是否有亂碼 (大量非中文英文數字字符)
            long validChars = trimmedText.chars()
                    .filter(c -> Character.isLetterOrDigit(c)
                    || (c >= 0x4E00 && c <= 0x9FFF)
                    || // 中文字符範圍
                    Character.isWhitespace(c))
                    .count();
            double validRatio = trimmedText.length() > 0 ? (double) validChars / trimmedText.length() : 0;
            System.out.println("有效字符比例: " + String.format("%.2f", validRatio * 100) + "%");

            if (validRatio < 0.3 && trimmedText.length() > 10) {
                System.out.println("⚠️ 檢測到可能的亂碼，有效字符比例過低");
            }

            System.out.println("==================");

            return trimmedText;

        } catch (TesseractException | IOException e) {
            System.err.println("OCR 處理失敗: " + e.getMessage());
            return "";
        }
    }

    /**
     * 圖片預處理 - 提高 OCR 準確度
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();

            // 如果圖片太小，先放大到合適尺寸 (OCR 在較大圖片上效果更好)
            if (width < 800 || height < 500) {
                double scaleFactor = Math.max(800.0 / width, 500.0 / height);
                int newWidth = (int) (width * scaleFactor);
                int newHeight = (int) (height * scaleFactor);

                BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                scaledImage.getGraphics().drawImage(original.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH), 0, 0, null);
                original = scaledImage;
                width = newWidth;
                height = newHeight;
                System.out.println("🔍 圖片放大至: " + width + "x" + height);
            }

            // 建立新的灰階圖片
            BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            // 先計算平均亮度，動態調整閾值
            int totalBrightness = 0;
            int pixelCount = width * height;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = original.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    totalBrightness += gray;
                }
            }

            int avgBrightness = totalBrightness / pixelCount;
            int threshold = Math.max(100, Math.min(180, avgBrightness - 20)); // 動態閾值

            // 轉換為灰階並增強對比度
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = original.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    // 轉換為灰階
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                    // 使用動態閾值進行二值化
                    gray = gray > threshold ? 255 : 0;

                    int grayRGB = (gray << 16) | (gray << 8) | gray;
                    processed.setRGB(x, y, grayRGB);
                }
            }

            System.out.println("✅ 圖片預處理完成 - 動態閾值(" + threshold + ")二值化");
            return processed;

        } catch (Exception e) {
            System.err.println("⚠️ 圖片預處理失敗，使用原圖: " + e.getMessage());
            return original;
        }
    }

    /**
     * 檢查文字是否可能是亂碼
     */
    private boolean isLikelyGarbage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }

        // 計算有效字符比例
        long validChars = text.chars()
                .filter(c -> Character.isLetterOrDigit(c)
                || (c >= 0x4E00 && c <= 0x9FFF)
                || // 中文字符範圍
                Character.isWhitespace(c)
                || ".,!?()[]{}:-".indexOf(c) >= 0) // 常見標點
                .count();

        double validRatio = text.length() > 0 ? (double) validChars / text.length() : 0;
        return validRatio < 0.4; // 有效字符少於40%視為亂碼
    }

    /**
     * 清理編碼問題和常見的 OCR 錯誤
     */
    private String cleanupEncodingIssues(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 移除控制字符和不可見字符，但保留空格、換行和制表符
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // 修正常見的 OCR 錯誤（特別是中文字符的常見誤識）
        cleaned = cleaned
                // 修正常見的標點符號錯誤
                .replace("丨", "1")
                .replace("○", "0")
                .replace("◯", "0")
                .replace("〇", "0")
                .replace("．", ".")
                .replace("，", ",")
                .replace("：", ":")
                .replace("；", ";")
                // 修正常見的英文字母錯誤
                .replace("ー", "1")
                .replace("│", "1")
                .replace("０", "0")
                .replace("１", "1")
                .replace("２", "2")
                .replace("３", "3")
                .replace("４", "4")
                .replace("５", "5")
                .replace("６", "6")
                .replace("７", "7")
                .replace("８", "8")
                .replace("９", "9")
                // 修正常見的中文字符OCR錯誤
                .replace("玉", "王") // 王字常被誤識為玉
                .replace("0", "王") // 王字有時被誤識為0
                .replace("曰", "日") // 日字常被誤識為曰
                .replace("目", "日") // 日字常被誤識為目
                .replace("木", "林") // 某些情況下林被誤識為木
                .replace("才", "材") // 材字的常見誤識
                .replace("乂", "文") // 文字的筆畫誤識
                .replace("又", "文") // 文字的常見誤識
                .replace("未", "朱") // 朱字的常見誤識
                .replace("失", "朱") // 朱字的另一種誤識
                .replace("季", "李") // 李字的常見誤識
                .replace("木", "李") // 李字有時被誤識為木
                .replace("呂", "吳") // 吳字的常見誤識
                .replace("昊", "吳") // 吳字的常見誤識
                .replace("橫", "黃") // 黃字的常見誤識
                .replace("貴", "黃") // 黃字的常見誤識
                .replace("司", "周") // 周字的常見誤識
                .replace("同", "周") // 周字的常見誤識
                .replace("余", "徐") // 徐字的常見誤識
                .replace("俆", "徐") // 徐字的常見誤識
                .replace("亮", "高") // 高字的常見誤識
                .replace("商", "高") // 高字的常見誤識
                .replace("河", "何") // 何字的常見誤識
                .replace("可", "何") // 何字的常見誤識
                .replace("部", "郭") // 郭字的常見誤識
                .replace("都", "郭") // 郭字的常見誤識
                .replace("維", "羅") // 羅字的常見誤識
                .replace("網", "羅") // 羅字的常見誤識
                .replace("射", "謝") // 謝字的常見誤識
                .replace("榭", "謝") // 謝字的常見誤識
                .replace("音", "韓") // 韓字的常見誤識
                .replace("章", "韓") // 韓字的常見誤識
                .replace("嗎", "馬") // 馬字的常見誤識
                .replace("媽", "馬") // 馬字的常見誤識
                .replace("甲", "田") // 田字的常見誤識
                .replace("申", "田") // 田字的常見誤識
                .replace("苑", "範") // 範字的常見誤識
                .replace("笵", "範") // 範字的常見誤識
                .replace("万", "方") // 方字的常見誤識
                .replace("刀", "方") // 方字的常見誤識
                .replace("右", "石") // 石字的常見誤識
                .replace("古", "石") // 石字的常見誤識
                .replace("美", "姜") // 姜字的常見誤識
                .replace("薑", "姜") // 姜字的常見誤識
                .replace("曰", "白") // 白字的常見誤識
                .replace("自", "白") // 白字的常見誤識
                .replace("奏", "秦") // 秦字的常見誤識
                .replace("春", "秦") // 秦字的常見誤識
                .replace("吏", "史") // 史字的常見誤識
                .replace("更", "史") // 史字的常見誤識
                .replace("催", "崔") // 崔字的常見誤識
                .replace("摧", "崔"); // 崔字的常見誤識

        // 移除過多的空格，但保留單個空格
        cleaned = cleaned.replaceAll("\\s+", " ");

        System.out.println("🔧 編碼清理: 原長度=" + text.length() + ", 清理後=" + cleaned.length());

        return cleaned;
    }

    /**
     * 備用 OCR 處理 - 使用不同的設定
     */
    private String performBackupOCR(BufferedImage image) {
        try {
            // 暫時改變設定，專門針對中文姓名優化
            int originalPSM = 3;
            tesseract.setPageSegMode(8); // 單一單詞模式，適合姓名

            // 限制字符集，提高中文識別準確度
            tesseract.setVariable("tessedit_char_whitelist",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                    + "中華民國身分證統一編號姓名出生年月日性別男女發證日期"
                    + "王李陳林張吳趙黃周徐朱高何郭羅謝韓馬田範方石姜白秦史崔"
                    + "劉楊蔡許鄧蘇盧蔣蕭曾魏金唐詹董葉溫劉江余莊廖熊顏嚴"
                    + "俊偉明志豪傑宇軒昊翔勇強智敏慧美秀芳儀雅娟玲英華麗"
                    + "建國民族文化教育學習工作生活");

            // 設定更高的DPI和更嚴格的設定
            tesseract.setVariable("user_defined_dpi", "600");
            tesseract.setVariable("textord_min_linesize", "1.0");

            String result = tesseract.doOCR(image);

            // 恢復原設定
            tesseract.setPageSegMode(originalPSM);
            tesseract.setVariable("tessedit_char_whitelist", "");
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("textord_min_linesize", "2.5");

            return result;
        } catch (TesseractException e) {
            System.err.println("備用 OCR 也失敗: " + e.getMessage());
            return "";
        }
    }

    /**
     * 第三種 OCR 處理 - 專門針對身分證姓名區域
     */
    private String performThirdOCR(BufferedImage image) {
        try {
            // 保存原設定
            int originalPSM = 3;

            // 使用最適合身分證的設定
            tesseract.setPageSegMode(7); // 單一文字行，適合身分證上的單行文字

            // 只允許常見的中文姓名字符和必要符號
            tesseract.setVariable("tessedit_char_whitelist",
                    "0123456789"
                    + "中華民國身分證統一編號姓名出生年月日性別男女發證日期"
                    + "王李陳林張吳趙黃周徐朱高何郭羅謝韓馬田範方石姜白秦史崔"
                    + "劉楊蔡許鄧蘇盧蔣蕭曾魏金唐詹董葉溫江余莊廖熊顏嚴黃"
                    + "陸孫胡朱任呂施張康賈易鄒伍茅潘葛滕奚柯尹班車成廷歐"
                    + "景項祝董樑杜阮藍戴應霍常萬莫習管燕米施歷連仲喬任榮"
                    + "俊偉明志豪傑宇軒昊翔勇強智敏慧美秀芳儀雅娟玲英華麗"
                    + "建國民族文化教育學習工作生活和平正義民主自由平等博愛"
                    + "健康快樂幸福安全環保永續發展創新科技資訊網路通訊傳播");

            // 提高識別精度的設定
            tesseract.setVariable("user_defined_dpi", "400");
            tesseract.setVariable("textord_min_linesize", "0.8");
            tesseract.setVariable("enable_new_segsearch", "0"); // 停用新的搜尋演算法
            tesseract.setVariable("language_model_penalty_non_freq_dict_word", "0.5");
            tesseract.setVariable("language_model_penalty_non_dict_word", "0.8");

            String result = tesseract.doOCR(image);

            // 恢復原設定
            tesseract.setPageSegMode(originalPSM);
            tesseract.setVariable("tessedit_char_whitelist", "");
            tesseract.setVariable("user_defined_dpi", "300");
            tesseract.setVariable("textord_min_linesize", "2.5");
            tesseract.setVariable("enable_new_segsearch", "1");
            tesseract.setVariable("language_model_penalty_non_freq_dict_word", "0.1");
            tesseract.setVariable("language_model_penalty_non_dict_word", "0.15");

            return result;
        } catch (TesseractException e) {
            System.err.println("第三種 OCR 設定也失敗: " + e.getMessage());
            return "";
        }
    }

    /**
     * 驗證台灣身分證姓名是否匹配
     */
    private boolean verifyName(String extractedText, String expectedName) {
        if (extractedText == null || expectedName == null || expectedName.trim().isEmpty()) {
            return false;
        }

        // 調試輸出
        System.out.println("=== 姓名驗證調試 ===");
        System.out.println("預期姓名: " + expectedName);
        System.out.println("提取文字: " + extractedText);

        // 清理提取的文字
        String cleanedText = cleanTextForNameMatching(extractedText);
        String cleanedExpected = cleanTextForNameMatching(expectedName);

        System.out.println("清理後提取文字: " + cleanedText);
        System.out.println("清理後預期姓名: " + cleanedExpected);

        // 1. 直接完全匹配
        boolean directMatch = cleanedText.contains(cleanedExpected);
        System.out.println("直接匹配結果: " + directMatch);

        if (directMatch) {
            System.out.println("==================");
            return true;
        }

        // 2. 不區分大小寫匹配
        boolean caseInsensitiveMatch = cleanedText.toLowerCase().contains(cleanedExpected.toLowerCase());
        System.out.println("不區分大小寫匹配結果: " + caseInsensitiveMatch);

        if (caseInsensitiveMatch) {
            System.out.println("==================");
            return true;
        }

        // 3. 姓名分割匹配 (處理中文姓名可能被拆開的情況)
        boolean splitNameMatch = matchSplitName(cleanedText, cleanedExpected);
        System.out.println("分割姓名匹配結果: " + splitNameMatch);

        if (splitNameMatch) {
            System.out.println("==================");
            return true;
        }

        // 4. 模糊匹配 (允許一定的字符差異)
        boolean fuzzyMatch = fuzzyNameMatch(cleanedText, cleanedExpected);
        System.out.println("模糊匹配結果: " + fuzzyMatch);

        System.out.println("==================");
        return fuzzyMatch;
    }

    /**
     * 清理文字用於姓名匹配
     */
    private String cleanTextForNameMatching(String text) {
        if (text == null) {
            return "";
        }

        return text
                // 移除所有空格和標點符號
                .replaceAll("[\\s\\p{Punct}]", "")
                // 修正常見的 OCR 錯誤對應中文字符（注意順序很重要）
                .replace("0", "王") // 王字有時被誤識為0
                .replace("玉", "王") // 王字常被誤識為玉
                .replace("曰", "日") // 日字常被誤識為曰
                .replace("目", "日") // 日字常被誤識為目
                .replace("乂", "文") // 文字的筆畫誤識
                .replace("又", "文") // 文字的常見誤識
                .replace("丶", "").replace("丿", "") // 移除多餘的筆畫符號
                .replace("丨", "").replace("│", "") // 移除單獨的線條
                .trim();
    }

    /**
     * 處理姓名可能被分割的情況
     */
    private boolean matchSplitName(String text, String expectedName) {
        if (expectedName.length() < 2) {
            return false;
        }

        // 檢查姓名的每個字符是否都出現在文字中 (可以不連續)
        int foundChars = 0;
        for (int i = 0; i < expectedName.length(); i++) {
            char nameChar = expectedName.charAt(i);
            if (text.indexOf(nameChar) >= 0) {
                foundChars++;
            }
        }

        // 如果找到的字符數佔姓名長度的70%以上，認為匹配
        double matchRatio = (double) foundChars / expectedName.length();
        return matchRatio >= 0.7;
    }

    /**
     * 模糊姓名匹配 (處理相似字符)
     */
    private boolean fuzzyNameMatch(String text, String expectedName) {
        if (expectedName.length() < 2) {
            return false;
        }

        // 常見的相似字符映射
        String[][] similarChars = {
            {"王", "玉", "主", "0", "○"},
            {"李", "季", "木", "材", "村"},
            {"陳", "陸", "隆"},
            {"林", "木", "材", "村"},
            {"張", "弦", "強"},
            {"吳", "呂", "昊"},
            {"趙", "越", "起"},
            {"黃", "橫", "貴"},
            {"周", "司", "同"},
            {"徐", "余", "俆"},
            {"朱", "未", "失"},
            {"高", "亮", "商"},
            {"何", "河", "可"},
            {"郭", "部", "都"},
            {"羅", "維", "網"},
            {"謝", "射", "榭"},
            {"韓", "音", "章"},
            {"馬", "嗎", "媽"},
            {"田", "甲", "申"},
            {"範", "苑", "笵"},
            {"方", "万", "刀"},
            {"石", "右", "古"},
            {"姜", "美", "薑"},
            {"白", "曰", "自"},
            {"秦", "奏", "春"},
            {"史", "吏", "更"},
            {"崔", "催", "摧"}
        };

        String fuzzyExpected = expectedName;

        // 將預期姓名中的字符替換為可能的相似字符進行匹配
        for (String[] group : similarChars) {
            for (int i = 1; i < group.length; i++) {
                if (text.contains(group[i])) {
                    fuzzyExpected = fuzzyExpected.replace(group[0], group[i]);
                    if (text.contains(fuzzyExpected)) {
                        return true;
                    }
                    fuzzyExpected = expectedName; // 重置
                }
            }
        }

        return false;
    }

    /**
     * 檢測是否為台灣身分證格式
     */
    private boolean detectIdCardFormat(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int score = 0;

        // 檢查台灣身分證關鍵字 (每個匹配+1分)
        String[] taiwanKeywords = {
            "中華民國", "身分證", "身份證", "國民身分證",
            "REPUBLIC", "CHINA", "TAIWAN", "IDENTITY", "CARD"
        };

        String upperText = text.toUpperCase();
        for (String keyword : taiwanKeywords) {
            if (upperText.contains(keyword.toUpperCase())) {
                score += 1;
            }
        }

        // 檢查台灣身分證號碼格式 (1個英文字母 + 9個數字) (+2分)
        Pattern taiwanIdPattern = Pattern.compile("[A-Z][0-9]{9}");
        if (taiwanIdPattern.matcher(text.replaceAll("\\s+", "").toUpperCase()).find()) {
            score += 2;
        }

        // 檢查是否包含"姓名"、"出生"、"性別"等欄位 (每個+1分)
        String[] fieldKeywords = {"姓名", "出生", "性別", "發證", "統一編號"};
        for (String field : fieldKeywords) {
            if (text.contains(field)) {
                score += 1;
            }
        }

        // 檢查是否包含民國年份 (19-113年) (+1分)
        Pattern rocYearPattern = Pattern.compile("民國.{0,3}[1-9][0-9]{1,2}年");
        if (rocYearPattern.matcher(text).find()) {
            score += 1;
        }

        // 檢查性別 (男/女) (+1分)
        if (text.contains("男") || text.contains("女")) {
            score += 1;
        }

        // 分數 >= 3 視為台灣身分證
        return score >= 3;
    }

    /**
     * 生成台灣身分證驗證結果訊息
     */
    private String generateValidationMessage(boolean validImage, boolean nameMatches, boolean isIdCard) {
        StringBuilder message = new StringBuilder();

        if (validImage && nameMatches && isIdCard) {
            message.append("✅ 台灣身分證驗證成功！");
        } else {
            message.append("❌ 台灣身分證驗證失敗：");
            if (!validImage) {
                message.append("\n• 圖片比例不符合台灣身分證標準 (85.6mm x 54mm)");
            }
            if (!nameMatches) {
                message.append("\n• 無法在身分證上找到指定姓名");
            }
            if (!isIdCard) {
                message.append("\n• 未檢測到台灣身分證特徵 (缺少關鍵字、身分證號等)");
            }
        }

        return message.toString();
    }

    /**
     * 圖像驗證結果類
     */
    public static class ImageValidationResult {

        private final boolean validImage;
        private final boolean nameMatches;
        private final boolean isIdCardFormat;
        private final String extractedText;
        private final String message;

        public ImageValidationResult(boolean validImage, boolean nameMatches,
                boolean isIdCardFormat, String extractedText, String message) {
            this.validImage = validImage;
            this.nameMatches = nameMatches;
            this.isIdCardFormat = isIdCardFormat;
            this.extractedText = extractedText;
            this.message = message;
        }

        // Getters
        public boolean isValidImage() {
            return validImage;
        }

        public boolean isNameMatches() {
            return nameMatches;
        }

        public boolean isIdCardFormat() {
            return isIdCardFormat;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValid() {
            return validImage && nameMatches && isIdCardFormat;
        }
    }

    /**
     * OCR 識別結果類
     */
    public static class OCRResult {

        private final String extractedText;
        private final double confidence;
        private final String message;

        public OCRResult(String extractedText, double confidence, String message) {
            this.extractedText = extractedText;
            this.confidence = confidence;
            this.message = message;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 對 MultipartFile 圖片執行 OCR 識別
     */
    public OCRResult performOCR(org.springframework.web.multipart.MultipartFile imageFile) {
        try {
            // 轉換 MultipartFile 為 byte array
            byte[] imageData = imageFile.getBytes();
            return performOCR(imageData);

        } catch (IOException e) {
            System.err.println("❌ 讀取圖片檔案失敗: " + e.getMessage());
            return new OCRResult("", 0.0, "讀取圖片檔案失敗: " + e.getMessage());
        }
    }

    /**
     * 對 byte array 圖片執行 OCR 識別
     */
    public OCRResult performOCR(byte[] imageData) {
        return performOCR(imageData, null);
    }

    /**
     * 對 byte array 圖片執行 OCR 識別 (支援特定區域類型優化)
     */
    public OCRResult performOCR(byte[] imageData, String regionType) {
        try {
            // 將 byte array 轉換為 BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                return new OCRResult("", 0.0, "無法解析圖片格式");
            }

            // 根據區域類型進行特殊處理
            if ("id".equals(regionType)) {
                // 身分證號碼的特殊處理
                return performIdNumberOCR(image);
            } else {
                // 一般 OCR 處理
                return performGeneralOCR(image);
            }

        } catch (IOException e) {
            System.err.println("❌ 圖片讀取失敗: " + e.getMessage());
            return new OCRResult("", 0.0, "圖片讀取失敗: " + e.getMessage());
        } catch (Exception e) {
            return new OCRResult("", 0.0, "OCR 過程發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 專門針對身分證號碼的 OCR 處理
     */
    private OCRResult performIdNumberOCR(BufferedImage image) {
        try {
            // 為身分證號碼優化的 Tesseract 設定
            Tesseract idTesseract = new Tesseract();
            idTesseract.setDatapath("./tessdata");

            // 優先使用英文模式來識別身分證號碼
            idTesseract.setLanguage("eng");
            idTesseract.setOcrEngineMode(1);
            idTesseract.setPageSegMode(8); // 單個文字塊

            // 針對身分證號碼的特殊設定
            idTesseract.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
            idTesseract.setVariable("classify_bln_numeric_mode", "1");
            idTesseract.setVariable("tessedit_write_unlv", "0");
            idTesseract.setVariable("user_defined_dpi", "300");

            String ocrText = idTesseract.doOCR(image);
            if (ocrText == null || ocrText.trim().isEmpty()) {
                return new OCRResult("", 0.0, "未能識別出身分證號碼");
            }

            String cleanText = ocrText.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");

            // 驗證身分證號碼格式
            if (isValidTaiwanIdFormat(cleanText)) {
                System.out.println("🎯 身分證號碼識別成功: " + cleanText);
                return new OCRResult(cleanText, 0.95, "身分證號碼識別成功");
            } else {
                System.out.println("⚠️ 身分證號碼格式不完整: " + cleanText);
                return new OCRResult(cleanText, 0.6, "身分證號碼識別部分成功，格式需要檢查");
            }

        } catch (TesseractException e) {
            System.err.println("❌ 身分證號碼 OCR 處理失敗: " + e.getMessage());
            return new OCRResult("", 0.0, "身分證號碼 OCR 處理失敗: " + e.getMessage());
        }
    }

    /**
     * 一般的 OCR 處理
     */
    private OCRResult performGeneralOCR(BufferedImage image) {
        try {
            // 執行 OCR
            String ocrText = tesseract.doOCR(image);
            if (ocrText == null || ocrText.trim().isEmpty()) {
                return new OCRResult("", 0.0, "未能識別出任何文字");
            }

            // 清理 OCR 結果
            String cleanText = ocrText.trim();

            // 計算信心度 (簡單的啟發式方法)
            double confidence = calculateOCRConfidence(cleanText);

            System.out.println("🎯 OCR 識別成功，文字內容: " + cleanText);

            return new OCRResult(cleanText, confidence, "OCR 識別成功");

        } catch (TesseractException e) {
            System.err.println("❌ OCR 處理失敗: " + e.getMessage());
            return new OCRResult("", 0.0, "OCR 處理失敗: " + e.getMessage());
        }
    }

    /**
     * 驗證台灣身分證號碼格式
     */
    private boolean isValidTaiwanIdFormat(String idNumber) {
        if (idNumber == null || idNumber.length() != 10) {
            return false;
        }

        // 第一個字符必須是 A-Z
        char firstChar = idNumber.charAt(0);
        if (firstChar < 'A' || firstChar > 'Z') {
            return false;
        }

        // 後面 9 個字符必須是數字
        for (int i = 1; i < 10; i++) {
            char c = idNumber.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * 計算 OCR 識別的信心度
     */
    private double calculateOCRConfidence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        double confidence = 0.5; // 基礎信心度

        // 有中文字符加分
        if (text.matches(".*[\\u4e00-\\u9fa5].*")) {
            confidence += 0.2;
        }

        // 有數字加分
        if (text.matches(".*\\d.*")) {
            confidence += 0.1;
        }

        // 文字長度適中加分
        int length = text.length();
        if (length >= 2 && length <= 50) {
            confidence += 0.1;
        }

        // 沒有過多特殊字符加分
        long specialCharCount = text.chars().filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch)).count();
        if (specialCharCount <= text.length() * 0.2) {
            confidence += 0.1;
        }

        return Math.min(1.0, confidence);
    }
}
