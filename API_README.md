# MyBatis CRUD API 使用說明

這個專案提供了完整的用戶管理 CRUD 操作 API。

## 環境設置

### 1. 資料庫設置
請確保您的 MySQL 資料庫正在運行，並執行以下步驟：

1. 執行 `src/main/resources/db/init.sql` 腳本來建立資料庫和表格
2. 在 `application.properties` 中修改資料庫連接配置

### 2. 啟動應用程式
```bash
mvn clean install
mvn spring-boot:run
```

應用程式將在 http://localhost:8080 啟動

## API 端點

### 1. 建立用戶
- **POST** `/api/users`
- **請求體：**
```json
{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "password123",
    "age": 25
}
```
- **響應：**
```json
{
    "success": true,
    "message": "用戶創建成功",
    "userId": 5
}
```

### 2. 查詢所有用戶
- **GET** `/api/users`
- **響應：**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "username": "admin",
            "email": "admin@example.com",
            "password": "admin123",
            "age": 25,
            "createTime": "2025-08-05T10:30:00",
            "updateTime": "2025-08-05T10:30:00"
        }
    ],
    "total": 1
}
```

### 3. 根據ID查詢用戶
- **GET** `/api/users/{id}`
- **響應：**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "password": "admin123",
        "age": 25,
        "createTime": "2025-08-05T10:30:00",
        "updateTime": "2025-08-05T10:30:00"
    }
}
```

### 4. 根據用戶名查詢用戶
- **GET** `/api/users/username/{username}`
- **響應：** 同上

### 5. 更新用戶
- **PUT** `/api/users/{id}`
- **請求體：**
```json
{
    "username": "updateduser",
    "email": "updated@example.com",
    "password": "newpassword",
    "age": 30
}
```
- **響應：**
```json
{
    "success": true,
    "message": "用戶更新成功"
}
```

### 6. 刪除用戶
- **DELETE** `/api/users/{id}`
- **響應：**
```json
{
    "success": true,
    "message": "用戶刪除成功"
}
```

### 7. 分頁查詢用戶
- **GET** `/api/users/page?page=1&size=10`
- **響應：**
```json
{
    "success": true,
    "data": [...],
    "total": 100,
    "page": 1,
    "size": 10,
    "totalPages": 10
}
```

## 測試方法

### 使用 cURL 測試

1. **建立用戶：**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123","age":25}'
```

2. **查詢所有用戶：**
```bash
curl -X GET http://localhost:8080/api/users
```

3. **查詢特定用戶：**
```bash
curl -X GET http://localhost:8080/api/users/1
```

4. **更新用戶：**
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"updateduser","email":"updated@example.com","password":"newpassword","age":30}'
```

5. **刪除用戶：**
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## 專案結構說明

```
src/main/java/com/erictest/aidemo/
├── AidemoApplication.java          # 主應用程式類
├── controller/
│   └── UserController.java        # 控制器層
├── service/
│   └── UserService.java          # 服務層
├── mapper/
│   └── UserMapper.java           # MyBatis Mapper 接口
└── model/
    └── User.java                  # 實體類

src/main/resources/
├── mappers/
│   └── UserMapper.xml             # MyBatis XML 映射檔案
├── db/
│   └── init.sql                   # 資料庫初始化腳本
└── application.properties         # 應用程式配置
```

## 注意事項

1. 請確保修改 `application.properties` 中的資料庫連接配置
2. 密碼字段在實際應用中應該進行加密處理
3. 建議在生產環境中添加更多的驗證和安全措施
4. 可以根據需要添加更多的查詢方法和業務邏輯
