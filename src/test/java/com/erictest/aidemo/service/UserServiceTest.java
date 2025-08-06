package com.erictest.aidemo.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.erictest.aidemo.model.User;

/**
 * 用戶服務測試類
 */
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testCreateUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword("password123");
        user.setAge(25);

        Long userId = userService.createUser(user);
        assertNotNull(userId);
        assertTrue(userId > 0);
    }

    @Test
    public void testGetUserById() {
        // 假設資料庫中已有ID為1的用戶
        User user = userService.getUserById(1L);
        assertNotNull(user);
        assertEquals(1L, user.getId());
    }

    @Test
    public void testGetAllUsers() {
        List<User> users = userService.getAllUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    public void testUpdateUser() {
        // 假設資料庫中已有ID為1的用戶
        User user = userService.getUserById(1L);
        if (user != null) {
            user.setAge(30);
            boolean result = userService.updateUser(user);
            assertTrue(result);
        }
    }

    @Test
    public void testDeleteUser() {
        // 先創建一個用戶用於測試刪除
        User user = new User();
        user.setUsername("deletetest");
        user.setEmail("deletetest@example.com");
        user.setPassword("password123");
        user.setAge(25);

        Long userId = userService.createUser(user);
        assertNotNull(userId);

        // 刪除用戶
        boolean result = userService.deleteUser(userId);
        assertTrue(result);

        // 驗證用戶已被刪除
        User deletedUser = userService.getUserById(userId);
        assertNull(deletedUser);
    }
}
