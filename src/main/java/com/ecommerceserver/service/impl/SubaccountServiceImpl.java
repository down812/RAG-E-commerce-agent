package com.ecommerceserver.service.impl;


import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerceserver.exception.BusinessException;
import com.ecommerceserver.exception.BusinessNotFoundException;
import com.ecommerceserver.mapper.UserMapper;
import com.ecommerceserver.model.dto.SubaccountCreateDTO;
import com.ecommerceserver.model.dto.SubaccountQueryDTO;
import com.ecommerceserver.model.dto.SubaccountUpdateDTO;
import com.ecommerceserver.model.entity.User;
import com.ecommerceserver.model.vo.SubaccountVO;
import com.ecommerceserver.result.PageResult;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.SubaccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.ecommerceserver.constants.CommonConstant.*;
import static com.ecommerceserver.constants.MessageConstant.*;


@Service
public class SubaccountServiceImpl implements SubaccountService {


    @Autowired
    private UserMapper userMapper;



    /**
     * 创建子账号
     * @param createDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createSubaccount(SubaccountCreateDTO createDTO) {
        // 1. 校验账号是否已存在
        LambdaQueryWrapper<User> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(User::getIdentifier, createDTO.getIdentifier());
        User existingUser = userMapper.selectOne(existsWrapper);
        if (existingUser != null) {
            throw new BusinessException(ACCOUNT_ALREADY_EXIST);
        }

        // 2. 密码加密
        String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + createDTO.getPassword()).getBytes());

        // 3. 构建用户对象并插入数据库
        User subaccount = User.builder()
                .identifier(createDTO.getIdentifier())
                .password(encryptedPwd)
                .type(createDTO.getType())
                .createdAt(DateTime.now())
                .updatedAt(DateTime.now())
                .build();
        userMapper.insert(subaccount);

        // 4. 返回子账号ID
        return Result.success(SUBACCOUNT_CREATE_SUCCESS, subaccount.getId());
    }

    /**
     * 查询子账号
     * @param pageNum
     * @param pageSize
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<SubaccountVO> querySubaccounts(Integer pageNum, Integer pageSize, SubaccountQueryDTO queryDTO) {
        // 1. 构建 MyBatis-Plus 分页对象
        Page<User> page = new Page<>(pageNum, pageSize);

        // 2. 构建查询条件（支持账号模糊查询、类型筛选）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            // 账号模糊查询
            if (queryDTO.getIdentifier() != null && !queryDTO.getIdentifier().trim().isEmpty()) {
                queryWrapper.like(User::getIdentifier, queryDTO.getIdentifier().trim());
            }
            // 类型精确筛选（仅当 type 不为 null 时添加条件）
            if (queryDTO.getType() != null) {
                queryWrapper.eq(User::getType, queryDTO.getType());
            }
        }
        // 按创建时间倒序（最新的子账号在前）
        queryWrapper.orderByDesc(User::getCreatedAt);

        // 3. 执行分页查询
        Page<User> userPage = userMapper.selectPage(page, queryWrapper);

        // 4. 实体类（User）转 VO（SubaccountVO）
        List<SubaccountVO> subaccountVOList = userPage.getRecords().stream()
                .map(user -> SubaccountVO.builder()
                        .subaccountId(user.getId())       // 子账号ID（对应 User 的 id）
                        .identifier(user.getIdentifier()) // 子账号账号
                        .type(user.getType())             // 子账号类型
                        .createdAt(user.getCreatedAt())   // 创建时间
                        .build())
                .collect(Collectors.toList());

        // 5. 按 PageResult 构造方法封装结果
        return new PageResult<>(
                userPage.getTotal(),    // 总记录数
                userPage.getPages(),    // 总页数
                pageNum,                // 当前页码
                pageSize,               // 每页大小
                subaccountVOList        // 当前页数据列表（赋值给 rows 字段）
        );
    }

    /**
     * 修改子账号
     * @param subaccountId
     * @param updateDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubaccountVO updateSubaccount(Integer subaccountId, SubaccountUpdateDTO updateDTO) {
        // 1. 校验子账号是否存在
        User subaccount = userMapper.selectById(subaccountId);
        if (subaccount == null) {
            throw new BusinessNotFoundException(ACCOUNT_NOT_EXIST);
        }

        // 2. 校验类型修改规则（仅支持 1↔2 互转；禁止修改为0）
        if (updateDTO.getType() != null) {
            Integer oldType = subaccount.getType();
            Integer newType = updateDTO.getType();
            // 禁止修改为0（超级管理员）
            if (newType == 0) {
                throw new BusinessException(NOT_ALLOW_CHANGE);
            }
            // 允许的修改规则：1→2、2→1（普通管理员与普通用户互转）
            boolean isAllowed = (oldType == 1 && newType == 2)
                    || (oldType == 2 && newType == 1);
            if (!isAllowed) {
                throw new BusinessException(NOT_ALLOW_CHANGE);
            }
            // 允许修改则更新类型
            subaccount.setType(newType);
        }

        // 3. 若修改密码，加密后更新
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            // 密码加密
            String encryptedPwd = DigestUtils.md5DigestAsHex((SALT + updateDTO.getPassword()).getBytes());
            subaccount.setPassword(encryptedPwd);
        }

        // 4. 更新时间戳（统一维护更新时间）
        subaccount.setUpdatedAt(DateTime.now());

        // 5. 执行数据库更新操作
        int rows = userMapper.updateById(subaccount);
        if (rows <= 0) {
            throw new BusinessException(SUBACCOUNT_CHANGE_ERROR);
        }

        // 6. 转换为VO并返回（包含修改后的最新信息）
        return SubaccountVO.builder()
                .subaccountId(subaccount.getId())
                .identifier(subaccount.getIdentifier())
                .type(subaccount.getType())
                .createdAt(subaccount.getCreatedAt()) // 保留创建时间
                .build();
    }

    /**
     * 删除子账号
     * @param subaccountId
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer deleteSubaccount(Integer subaccountId) {
        // 1. 校验子账号是否存在
        User subaccount = userMapper.selectById(subaccountId);
        if (subaccount == null) {
            throw new BusinessException(ACCOUNT_NOT_EXIST);
        }

        // 2. 校验是否为超管（禁止删除超管账号，即使调用者是超管）
        if (subaccount.getType() == 0) {
            throw new BusinessException(NOT_ALLOW_CHANGE);
        }

        // 3. 执行删除操作（物理删除，若需要逻辑删除可改为 update 操作）
        int rows = userMapper.deleteById(subaccountId);
        if (rows <= 0) {
            throw new BusinessException(SUBACCOUNT_DELETE_ERROR);
        }

        // 4. 返回删除的子账号ID（用于前端确认）
        return subaccountId;
    }
}
