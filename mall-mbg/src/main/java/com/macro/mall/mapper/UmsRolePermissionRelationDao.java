package com.macro.mall.mapper;

import com.macro.mall.model.UmsPermission;
import com.macro.mall.model.UmsRolePermissionRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 后台用户角色管理自定义Dao
 * https://gitee.com/zscat-platform/mall on 2018/9/30.
 */
public interface UmsRolePermissionRelationDao {
    /**
     * 批量插入角色和权限关系
     */
    int insertList(@Param("list") List<UmsRolePermissionRelation> list);

    /**
     * 根据角色获取权限
     */
    List<UmsPermission> getPermissionList(@Param("roleId") Long roleId);

    List<UmsRolePermissionRelation> getRolePermissionList(@Param("roleId") Long roleId);
}
