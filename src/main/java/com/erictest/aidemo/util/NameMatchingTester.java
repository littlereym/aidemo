package com.erictest.aidemo.util;

import java.lang.reflect.Method;

import com.erictest.aidemo.service.ImageRecognitionService;

/**
 * 姓名匹配功能測試
 */
public class NameMatchingTester {

    public static void main(String[] args) {
        System.out.println("=== 姓名匹配功能測試 ===");

        try {
            ImageRecognitionService service = new ImageRecognitionService();

            // 使用反射訪問私有方法進行測試
            Method verifyNameMethod = ImageRecognitionService.class.getDeclaredMethod("verifyName", String.class, String.class);
            verifyNameMethod.setAccessible(true);

            // 測試案例
            String[][] testCases = {
                {"王小明", "王小明"}, // 完全匹配
                {"王 小 明", "王小明"}, // 空格問題
                {"玉小明", "王小明"}, // 王字被誤識為玉
                {"0小明", "王小明"}, // 王字被誤識為0
                {"李小明中華民國", "李小明"}, // 包含其他文字
                {"姓名李小明出生", "李小明"}, // 包含標籤文字
                {"季小明", "李小明"}, // 李字被誤識為季
                {"木小明", "李小明"}, // 李字被誤識為木
                {"陳大華", "陳大華"}, // 完全匹配
                {"陸大華", "陳大華"}, // 陳字被誤識為陸
                {"呂小美", "吳小美"}, // 吳字被誤識為呂
                {"朱 建 國", "朱建國"}, // 分散的姓名
                {"未建國", "朱建國"}, // 朱字被誤識為未
                {"崔偉明", "崔偉明"}, // 完全匹配
                {"催偉明", "崔偉明"}, // 崔字被誤識為催
            };

            int totalTests = testCases.length;
            int passedTests = 0;

            System.out.println("\n開始測試 " + totalTests + " 個案例:");
            System.out.println("=" + "=".repeat(50));

            for (int i = 0; i < testCases.length; i++) {
                String extractedText = testCases[i][0];
                String expectedName = testCases[i][1];

                Boolean result = (Boolean) verifyNameMethod.invoke(service, extractedText, expectedName);

                String status = result ? "✅ PASS" : "❌ FAIL";
                System.out.printf("測試 %2d: %s\n", i + 1, status);
                System.out.printf("         提取文字: '%s'\n", extractedText);
                System.out.printf("         預期姓名: '%s'\n", expectedName);

                if (result) {
                    passedTests++;
                }

                System.out.println();
            }

            System.out.println("=" + "=".repeat(50));
            System.out.printf("測試結果: %d/%d 通過 (%.1f%%)\n",
                    passedTests, totalTests, (double) passedTests / totalTests * 100);

            if (passedTests == totalTests) {
                System.out.println("🎉 所有測試都通過了！姓名匹配功能運作正常。");
            } else {
                System.out.println("⚠️ 還有一些測試未通過，需要進一步優化。");
            }

        } catch (Exception e) {
            System.err.println("測試過程發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
