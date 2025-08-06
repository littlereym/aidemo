package com.erictest.aidemo.model;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用戶實體類
 */
@Schema(description = "👤 用戶資料模型 - 包含用戶的基本資訊")
public class User {

    @Schema(description = "🆔 用戶唯一標識 ID（由系統自動生成，新增時不需要填寫）", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "👤 用戶名（必填，需要唯一不重複）", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "📧 電子郵件地址（必填，用於聯繫和登入）", example = "admin@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "🔒 用戶密碼（必填，建議使用複雜密碼）", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "🎂 用戶年齡（選填，請填入真實年齡）", example = "25", minimum = "1", maximum = "150")
    private Integer age;

    @Schema(description = "📅 帳號創建時間（系統自動設置）", example = "2023-08-05T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    @Schema(description = "🔄 最後更新時間（系統自動維護）", example = "2023-08-05T15:20:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateTime;

    // 無參構造函數
    public User() {
    }

    // 有參構造函數
    public User(String username, String email, String password, Integer age) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.age = age;
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", password='" + password + '\''
                + ", age=" + age
                + ", createTime=" + createTime
                + ", updateTime=" + updateTime
                + '}';
    }
}
