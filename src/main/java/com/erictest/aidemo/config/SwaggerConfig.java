package com.erictest.aidemo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger/OpenAPI 配置類
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("📱 用戶管理系統 API")
                        .description("🚀 這是一個基於 Spring Boot + MyBatis 開發的用戶管理系統\n\n"
                                + "💡 **系統功能說明：**\n"
                                + "- 👥 **用戶管理**：輕鬆新增、查詢、修改、刪除用戶資料\n"
                                + "- 🔍 **多種查詢方式**：支援根據 ID、用戶名等條件查詢\n"
                                + "- 📖 **分頁查詢**：處理大量數據時的分頁顯示\n"
                                + "- 🛡️ **數據驗證**：確保輸入數據的完整性和正確性\n\n"
                                + "🎯 **如何使用：**\n"
                                + "1. 點擊下方的 API 端點展開詳細說明\n"
                                + "2. 點擊「Try it out」按鈕開始測試\n"
                                + "3. 填入測試數據後點擊「Execute」執行\n"
                                + "4. 查看執行結果和響應數據\n\n"
                                + "📝 **開發團隊提醒：** 請先確保資料庫連接正常，並已執行初始化 SQL 腳本！")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("👨‍💻 開發團隊")
                                .email("dev@example.com")
                                .url("https://github.com/your-repo"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("🏠 本地開發環境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("🌐 正式環境服務器")
                ));
    }
}
