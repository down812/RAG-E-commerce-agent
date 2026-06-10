package com.ecommerceserver.controller.user;

import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.exception.PermissionDeniedException;
import com.ecommerceserver.model.dto.*;
import com.ecommerceserver.model.vo.*;
import com.ecommerceserver.result.PageResult;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.SubaccountService;
import com.ecommerceserver.service.UserService;
import com.ecommerceserver.utils.MailUtils;
import com.ecommerceserver.utils.VerCodeGenerateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static com.ecommerceserver.constants.MessageConstant.*;

/**
 * 用户相关接口控制器
 */
@Tag(name = "用户管理")
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SubaccountService subaccountService;

    @Autowired
    private MailUtils mailUtils;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email/code")
    public Result<String> sendEmailCode(
            @NotBlank(message = "邮箱不能为空")
            @RequestParam String email) {
        boolean success = mailUtils.sendEmailCode(email,VerCodeGenerateUtil.getVerCode());
        if (success) {
            return Result.success("验证码已发送，请注意查收（5分钟内有效）");
        } else {
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    @PostMapping("/subaccount")
    @Operation(summary = "创建子账号", description = "仅超级管理员（type=0）、普通管理员（type=1）可调用")
    public Result<?> createSubaccount(
            @Validated @RequestBody SubaccountCreateDTO createDTO
    ) {
        Integer currentUserType = LoginContext.getUserType();
        if (currentUserType == null || (currentUserType != 0 && currentUserType != 1)) {
            throw new PermissionDeniedException(NO_PERMISSION_ADMIN_REQUIRED);
        }
        return subaccountService.createSubaccount(createDTO);
    }


    @PostMapping("/subaccountList")
    @Operation(summary = "查询子账号列表", description = "分页查询，支持按账号、类型筛选；仅type=0/1可调用")
    public Result<PageResult<SubaccountVO>> querySubaccounts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestBody(required = false) SubaccountQueryDTO queryDTO
    ) {
        Integer currentUserType = LoginContext.getUserType();
        if (currentUserType == null || (currentUserType != 0 && currentUserType != 1)) {
            throw new PermissionDeniedException(NO_PERMISSION_ADMIN_REQUIRED);
        }
        PageResult<SubaccountVO> pageResult = subaccountService.querySubaccounts(pageNum, pageSize, queryDTO);
        return Result.success(pageResult);
    }

    @PutMapping("/subaccount/{subaccountId}")
    @Operation(summary = "修改子账号", description = "支持修改密码、类型；仅type=0/1可调用")
    public Result<SubaccountVO> updateSubaccount(
            @PathVariable Integer subaccountId,
            @Validated @RequestBody SubaccountUpdateDTO updateDTO
    ) {
        Integer currentUserType = LoginContext.getUserType();
        if (currentUserType == null || (currentUserType != 0 && currentUserType != 1)) {
            throw new PermissionDeniedException(NO_PERMISSION_ADMIN_REQUIRED);
        }
        SubaccountVO subaccountVO = subaccountService.updateSubaccount(subaccountId, updateDTO);
        return Result.success(subaccountVO);
    }


    @DeleteMapping("/subaccount/{subaccountId}")
    @Operation(summary = "删除子账号", description = "仅超级管理员（type=0）可调用")
    public Result<?> deleteSubaccount(
            @PathVariable Integer subaccountId
    ) {
        Integer currentUserType = LoginContext.getUserType();
        if (currentUserType == null || currentUserType != 0) {
            throw new PermissionDeniedException(NO_PERMISSION_SUPER_ADMIN_REQUIRED);
        }
        Integer deletedId = subaccountService.deleteSubaccount(subaccountId);
        return Result.success(DELETE_SUBACCOUNT_ERROR, deletedId);
    }
}