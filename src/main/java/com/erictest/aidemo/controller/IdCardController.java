package com.erictest.aidemo.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.erictest.aidemo.service.ImageRecognitionService;
import com.erictest.aidemo.service.ImageRecognitionService.ImageValidationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 身分證上傳控制器 - 整合圖像識別功能
 */
@Controller
@RequestMapping("/id-card")
@Tag(name = "📄 身分證上傳", description = "處理身分證正反面圖片上傳功能，包含圖像識別驗證")
public class IdCardController {

    // 設定檔案上傳目錄
    private static final String UPLOAD_DIR = "uploads/id-cards/";

    @Autowired
    private ImageRecognitionService imageRecognitionService;

    /**
     * 顯示身分證上傳頁面
     */
    @GetMapping("/upload")
    @Operation(summary = "📄 顯示身分證上傳頁面", description = "返回身分證上傳的 HTML 頁面")
    public String uploadPage() {
        return "id-card-upload";
    }

    /**
     * 處理身分證檔案上傳 - 整合圖像識別
     */
    @PostMapping("/upload")
    @Operation(summary = "📤 上傳身分證圖片", description = "處理身分證正反面圖片上傳並使用AI進行驗證")
    public String uploadIdCard(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("userName") String userName,
            RedirectAttributes redirectAttributes) {

        try {
            // 建立上傳目錄
            createUploadDirectoryIfNotExists();

            // 基本驗證檔案
            String validationResult = validateFiles(frontImage, backImage, userName);
            if (validationResult != null) {
                redirectAttributes.addFlashAttribute("error", validationResult);
                return "redirect:/id-card/upload";
            }

            // 🔍 只對正面圖片的姓名區域進行驗證，提高準確率
            StringBuilder aiResults = new StringBuilder();

            // 只驗證正面姓名區域
            ImageValidationResult frontValidation = imageRecognitionService.validateNameRegionOnly(
                    frontImage.getBytes(), userName);
            aiResults.append("🔍 正面姓名驗證：").append(frontValidation.getMessage()).append("\n");

            // 反面圖片不進行AI驗證
            aiResults.append("🔍 反面圖片：✅ 已上傳（跳過AI驗證以提高準確率）").append("\n");

            // 產生檔案名稱
            String frontFileName = generateFileName(userName, "front", frontImage.getOriginalFilename());
            String backFileName = generateFileName(userName, "back", backImage.getOriginalFilename());

            // 保存檔案
            saveFile(frontImage, frontFileName);
            saveFile(backImage, backFileName);

            // 根據AI驗證結果設定訊息（現在只檢查正面姓名區域）
            if (frontValidation.isValid()) {
                redirectAttributes.addFlashAttribute("success",
                        String.format("🎉 %s 的身分證上傳成功！\n\n%s\n正面檔案：%s\n反面檔案：%s",
                                userName, aiResults.toString(), frontFileName, backFileName));
            } else {
                redirectAttributes.addFlashAttribute("error",
                        String.format("⚠️ %s 的身分證已上傳，但姓名驗證未通過：\n\n%s\n\n檔案已保存：\n正面：%s\n反面：%s",
                                userName, aiResults.toString(), frontFileName, backFileName));
            }

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "❌ 檔案上傳失敗：" + e.getMessage());
        }

        return "redirect:/id-card/upload";
    }

    /**
     * API 端點：處理 AJAX 上傳請求 - 整合圖像識別
     */
    @PostMapping("/api/upload")
    @ResponseBody
    @Operation(summary = "📤 API上傳身分證", description = "透過 API 上傳身分證圖片並進行AI驗證（返回 JSON）")
    public Map<String, Object> uploadIdCardApi(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("userName") String userName) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 建立上傳目錄
            createUploadDirectoryIfNotExists();

            // 基本驗證檔案
            String validationResult = validateFiles(frontImage, backImage, userName);
            if (validationResult != null) {
                response.put("success", false);
                response.put("message", validationResult);
                return response;
            }

            // 🔍 只對正面圖片的姓名區域進行驗證，提高準確率
            ImageValidationResult frontValidation = imageRecognitionService.validateNameRegionOnly(
                    frontImage.getBytes(), userName);

            // 反面圖片不進行AI驗證，只檢查基本格式
            ImageValidationResult backValidation = new ImageRecognitionService.ImageValidationResult(
                    true, true, true, "", "✅ 反面圖片已上傳");

            // 產生檔案名稱
            String frontFileName = generateFileName(userName, "front", frontImage.getOriginalFilename());
            String backFileName = generateFileName(userName, "back", backImage.getOriginalFilename());

            // 保存檔案
            saveFile(frontImage, frontFileName);
            saveFile(backImage, backFileName);

            // 設定回應資料（現在只檢查正面姓名區域）
            boolean aiValidationPassed = frontValidation.isValid();

            response.put("success", true);
            response.put("message", aiValidationPassed ? "身分證上傳並驗證成功！" : "身分證已上傳，但姓名驗證未通過");
            response.put("aiValidation", Map.of(
                    "passed", aiValidationPassed,
                    "frontResult", Map.of(
                            "valid", frontValidation.isValid(),
                            "message", frontValidation.getMessage(),
                            "extractedText", frontValidation.getExtractedText()
                    ),
                    "backResult", Map.of(
                            "valid", backValidation.isValid(),
                            "message", backValidation.getMessage(),
                            "extractedText", "跳過反面驗證以提高準確率"
                    )
            ));
            response.put("data", Map.of(
                    "userName", userName,
                    "frontImage", frontFileName,
                    "backImage", backFileName,
                    "uploadTime", java.time.LocalDateTime.now().toString()
            ));

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "檔案上傳失敗：" + e.getMessage());
        }

        return response;
    }

    /**
     * 建立上傳目錄
     */
    private void createUploadDirectoryIfNotExists() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    /**
     * 驗證上傳檔案
     */
    private String validateFiles(MultipartFile frontImage, MultipartFile backImage, String userName) {
        // 檢查用戶名
        if (userName == null || userName.trim().isEmpty()) {
            return "❌ 請輸入姓名";
        }

        // 檢查檔案是否為空
        if (frontImage.isEmpty()) {
            return "❌ 請選擇身分證正面圖片";
        }
        if (backImage.isEmpty()) {
            return "❌ 請選擇身分證反面圖片";
        }

        // 檢查檔案大小（限制 5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (frontImage.getSize() > maxSize) {
            return "❌ 身分證正面圖片檔案過大（超過 5MB）";
        }
        if (backImage.getSize() > maxSize) {
            return "❌ 身分證反面圖片檔案過大（超過 5MB）";
        }

        // 檢查檔案類型
        if (!isValidImageFile(frontImage)) {
            return "❌ 身分證正面必須是圖片檔案（JPG、PNG、JPEG）";
        }
        if (!isValidImageFile(backImage)) {
            return "❌ 身分證反面必須是圖片檔案（JPG、PNG、JPEG）";
        }

        return null; // 驗證通過
    }

    /**
     * 檢查是否為有效的圖片檔案
     */
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg")
                || contentType.equals("image/jpg")
                || contentType.equals("image/png"));
    }

    /**
     * 產生檔案名稱
     */
    private String generateFileName(String userName, String side, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFilename);
        return String.format("%s_%s_%s%s", userName, side, timestamp, extension);
    }

    /**
     * 區域 OCR 識別 API
     */
    @PostMapping("/api/ocr-region")
    @ResponseBody
    @Operation(summary = "🎯 區域 OCR 識別", description = "對圖片中的特定區域進行 OCR 文字識別")
    public Map<String, Object> ocrRegion(
            @RequestParam("image") MultipartFile image,
            @RequestParam("regionType") String regionType,
            @RequestParam("side") String side) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 檢查檔案是否存在
            if (image.isEmpty()) {
                response.put("success", false);
                response.put("message", "沒有接收到圖片檔案");
                return response;
            }

            // 檢查檔案類型
            String contentType = image.getContentType();
            if (contentType == null
                    || (!contentType.equals("image/jpeg")
                    && !contentType.equals("image/jpg")
                    && !contentType.equals("image/png"))) {
                response.put("success", false);
                response.put("message", "不支援的檔案格式，請使用 JPG 或 PNG");
                return response;
            }

            // 檢查檔案大小 (5MB 限制)
            if (image.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "檔案大小不能超過 5MB");
                return response;
            }

            // 執行 OCR 識別
            ImageRecognitionService.OCRResult ocrResult = imageRecognitionService.performOCR(image);

            if (ocrResult != null && ocrResult.getExtractedText() != null && !ocrResult.getExtractedText().trim().isEmpty()) {
                // 根據區域類型進行特定的文字處理和驗證
                String processedText = processRegionText(ocrResult.getExtractedText(), regionType, side);

                Map<String, Object> result = new HashMap<>();
                result.put("extractedText", processedText);
                result.put("originalText", ocrResult.getExtractedText());
                result.put("confidence", ocrResult.getConfidence());
                result.put("regionType", regionType);
                result.put("side", side);

                response.put("success", true);
                response.put("message", "OCR 識別成功");
                response.put("result", result);

                // 記錄識別結果
                System.out.println("🎯 區域 OCR 識別成功 - " + side + " " + regionType + ": " + processedText);

            } else {
                response.put("success", false);
                response.put("message", "未能從圖片中識別出文字，請檢查圖片品質");
            }

        } catch (Exception e) {
            System.err.println("❌ 區域 OCR 識別失敗: " + e.getMessage());

            response.put("success", false);
            response.put("message", "OCR 識別過程中發生錯誤: " + e.getMessage());
        }

        return response;
    }

    /**
     * 根據區域類型處理識別出的文字
     */
    private String processRegionText(String rawText, String regionType, @SuppressWarnings("unused") String side) {
        if (rawText == null) {
            return "";
        }

        String cleanText = rawText.replaceAll("\\s+", " ").trim();

        return switch (regionType) {
            case "name" ->
                // 姓名區域：移除非中文字符，保留中文姓名
                cleanText.replaceAll("[^\\u4e00-\\u9fa5]", "");

            case "id" -> {
                // 身分證號：提取數字和字母組合，並進行格式驗證
                String idText = cleanText.replaceAll("[^A-Z0-9a-z]", "").toUpperCase();

                // 嘗試匹配台灣身分證號碼格式 (1個英文字母 + 9個數字)
                java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("[A-Z][0-9]{9}");
                java.util.regex.Matcher matcher = idPattern.matcher(idText);

                if (matcher.find()) {
                    yield matcher.group(); // 返回找到的完整身分證號
                } else {
                    // 如果沒找到完整格式，返回所有英數字元
                    yield idText;
                }
            }

            case "address" ->
                // 地址區域：保留中文和數字
                cleanText.replaceAll("[^\\u4e00-\\u9fa50-9]", "");

            case "office" ->
                // 發證機關：保留中文
                cleanText.replaceAll("[^\\u4e00-\\u9fa5]", "");

            case "date" ->
                // 日期：提取數字和斜線
                cleanText.replaceAll("[^0-9/年月日]", "");

            case "spouse" ->
                // 配偶：保留中文
                cleanText.replaceAll("[^\\u4e00-\\u9fa5]", "");

            default ->
                // 其他情況：返回清理後的原文
                cleanText;
        };
    }

    /**
     * 獲取檔案副檔名
     */
    private String getFileExtension(String filename) {
        if (filename != null && filename.lastIndexOf(".") > 0) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg"; // 預設副檔名
    }

    /**
     * 保存檔案到指定目錄
     */
    private void saveFile(MultipartFile file, String fileName) throws IOException {
        Path targetLocation = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
    }
}
