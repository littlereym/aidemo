package com.erictest.aidemo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.erictest.aidemo.model.User;

/**
 * 用戶 Mapper 接口
 */
@Mapper
public interface UserMapper {

    /**
     * 新增用戶
     *
     * @param user 用戶對象
     * @return 影響的行數
     */
    int insert(User user);

    /**
     * 根據 ID 刪除用戶
     *
     * @param id 用戶 ID
     * @return 影響的行數
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新用戶信息
     *
     * @param user 用戶對象
     * @return 影響的行數
     */
    int update(User user);

    /**
     * 根據 ID 查詢用戶
     *
     * @param id 用戶 ID
     * @return 用戶對象
     */
    User selectById(@Param("id") Long id);

    /**
     * 查詢所有用戶
     *
     * @return 用戶列表
     */
    List<User> selectAll();

    /**
     * 根據用戶名查詢用戶
     *
     * @param username 用戶名
     * @return 用戶對象
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根據條件分頁查詢用戶
     *
     * @param offset 偏移量
     * @param limit 每頁數量
     * @return 用戶列表
     */
    List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 計算用戶總數
     *
     * @return 總數
     */
    long count();
}
