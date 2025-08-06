package com.erictest.aidemo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.erictest.aidemo.mapper.UserMapper;
import com.erictest.aidemo.model.User;

/**
 * 用戶服務類
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 新增用戶
     *
     * @param user 用戶對象
     * @return 新增成功返回用戶ID，失敗返回null
     */
    public Long createUser(User user) {
        if (user == null || user.getUsername() == null || user.getEmail() == null) {
            throw new IllegalArgumentException("用戶信息不能為空");
        }

        // 檢查用戶名是否已存在
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("用戶名已存在");
        }

        int result = userMapper.insert(user);
        return result > 0 ? user.getId() : null;
    }

    /**
     * 根據ID刪除用戶
     *
     * @param id 用戶ID
     * @return 刪除成功返回true，失敗返回false
     */
    public boolean deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用戶ID不能為空或小於等於0");
        }

        int result = userMapper.deleteById(id);
        return result > 0;
    }

    /**
     * 更新用戶信息
     *
     * @param user 用戶對象
     * @return 更新成功返回true，失敗返回false
     */
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用戶信息或用戶ID不能為空");
        }

        // 檢查用戶是否存在
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("用戶不存在");
        }

        int result = userMapper.update(user);
        return result > 0;
    }

    /**
     * 根據ID查詢用戶
     *
     * @param id 用戶ID
     * @return 用戶對象，不存在返回null
     */
    public User getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("用戶ID不能為空或小於等於0");
        }

        return userMapper.selectById(id);
    }

    /**
     * 查詢所有用戶
     *
     * @return 用戶列表
     */
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    /**
     * 根據用戶名查詢用戶
     *
     * @param username 用戶名
     * @return 用戶對象，不存在返回null
     */
    public User getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用戶名不能為空");
        }

        return userMapper.selectByUsername(username);
    }

    /**
     * 分頁查詢用戶
     *
     * @param page 頁碼（從1開始）
     * @param size 每頁數量
     * @return 用戶列表
     */
    public List<User> getUsersByPage(int page, int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("頁碼和每頁數量必須大於0");
        }

        int offset = (page - 1) * size;
        return userMapper.selectByPage(offset, size);
    }

    /**
     * 獲取用戶總數
     *
     * @return 用戶總數
     */
    public long getUserCount() {
        return userMapper.count();
    }
}
