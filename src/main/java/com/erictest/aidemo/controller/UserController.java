package com.erictest.aidemo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.erictest.aidemo.model.User;
import com.erictest.aidemo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用戶控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "👥 用戶管理", description = "管理用戶的所有操作，包括新增、查詢、修改、刪除等功能")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 🆕 新增用戶
     *
     * @param user 用戶對象
     * @return 響應結果
     */
    @PostMapping
    @Operation(
            summary = "🆕 新增用戶",
            description = "創建一個全新的用戶帳號！\n\n"
            + "💡 **使用說明：**\n"
            + "- 用戶名和 Email 是必填項目，不能為空\n"
            + "- 用戶名必須是唯一的，不能重複\n"
            + "- 系統會自動生成創建時間和更新時間\n"
            + "- 年齡可以選填，建議填入真實年齡\n\n"
            + "✅ **成功時會回傳新用戶的 ID**\n"
            + "❌ **失敗時會告訴您具體的錯誤原因**"
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "🎉 用戶創建成功！",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Map.class),
                        examples = @ExampleObject(
                                name = "成功範例",
                                value = """
                    {
                        "success": true,
                        "message": "用戶創建成功",
                        "userId": 5
                    }
                    """
                        )
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "❌ 創建失敗（可能是用戶名重複或資料不完整）",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = Map.class),
                        examples = @ExampleObject(
                                name = "失敗範例",
                                value = """
                    {
                        "success": false,
                        "message": "用戶創建失敗：用戶名已存在"
                    }
                    """
                        )
                )
        )
    })
    public ResponseEntity<Map<String, Object>> createUser(
            @Parameter(
                    description = "📝 要新增的用戶資料（包含用戶名、Email、密碼等資訊）",
                    required = true,
                    schema = @Schema(implementation = User.class)
            )
            @RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = userService.createUser(user);
            response.put("success", true);
            response.put("message", "用戶創建成功");
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "用戶創建失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 🗑️ 刪除用戶
     *
     * @param id 用戶ID
     * @return 響應結果
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "🗑️ 刪除用戶",
            description = "根據用戶 ID 永久刪除指定的用戶！\n\n"
            + "⚠️ **重要提醒：**\n"
            + "- 這個操作是不可逆的，刪除後無法恢復\n"
            + "- 請確認您真的要刪除這個用戶\n"
            + "- 用戶 ID 必須是有效的數字\n\n"
            + "🎯 **使用方法：** 在 URL 路徑中提供要刪除的用戶 ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "✅ 刪除操作完成（成功或失敗都會返回 200）",
                content = @Content(
                        mediaType = "application/json",
                        examples = {
                            @ExampleObject(
                                    name = "刪除成功",
                                    value = """
                        {
                            "success": true,
                            "message": "用戶刪除成功"
                        }
                        """
                            ),
                            @ExampleObject(
                                    name = "刪除失敗",
                                    value = """
                        {
                            "success": false,
                            "message": "用戶刪除失敗"
                        }
                        """
                            )
                        }
                )
        ),
        @ApiResponse(
                responseCode = "400",
                description = "❌ 請求參數錯誤（如 ID 格式不正確）"
        )
    })
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(
                    description = "🆔 要刪除的用戶 ID（必須是正整數）",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1")
            )
            @PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = userService.deleteUser(id);
            response.put("success", success);
            response.put("message", success ? "用戶刪除成功" : "用戶刪除失敗");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "用戶刪除失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新用戶信息
     *
     * @param id 用戶ID
     * @param user 用戶對象
     * @return 響應結果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        try {
            user.setId(id);
            boolean success = userService.updateUser(user);
            response.put("success", success);
            response.put("message", success ? "用戶更新成功" : "用戶更新失敗");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "用戶更新失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據ID查詢用戶
     *
     * @param id 用戶ID
     * @return 用戶信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                response.put("success", true);
                response.put("data", user);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用戶不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查詢失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 📋 查詢所有用戶
     *
     * @return 用戶列表
     */
    @GetMapping
    @Operation(
            summary = "📋 查詢所有用戶",
            description = "一次性獲取系統中所有用戶的資料！\n\n"
            + "📊 **返回內容：**\n"
            + "- 完整的用戶列表（包含所有用戶資訊）\n"
            + "- 用戶總數統計\n"
            + "- 按創建時間降序排列（最新的在前面）\n\n"
            + "💡 **使用建議：**\n"
            + "- 如果用戶數量很多，建議使用分頁查詢\n"
            + "- 可以用來快速檢視系統中的所有用戶"
    )
    @ApiResponse(
            responseCode = "200",
            description = "🎉 成功獲取用戶列表",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "成功範例",
                            value = """
                {
                    "success": true,
                    "data": [
                        {
                            "id": 1,
                            "username": "admin",
                            "email": "admin@example.com",
                            "age": 25,
                            "createTime": "2023-08-05T10:30:00"
                        }
                    ],
                    "total": 1
                }
                """
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.getAllUsers();
            response.put("success", true);
            response.put("data", users);
            response.put("total", users.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查詢失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據用戶名查詢用戶
     *
     * @param username 用戶名
     * @return 用戶信息
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.getUserByUsername(username);
            if (user != null) {
                response.put("success", true);
                response.put("data", user);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用戶不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查詢失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 分頁查詢用戶
     *
     * @param page 頁碼（從1開始）
     * @param size 每頁數量
     * @return 用戶列表
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getUsersByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.getUsersByPage(page, size);
            long total = userService.getUserCount();

            response.put("success", true);
            response.put("data", users);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) total / size));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查詢失敗：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
