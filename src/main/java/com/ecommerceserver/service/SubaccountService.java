package com.ecommerceserver.service;

import com.ecommerceserver.model.dto.SubaccountCreateDTO;
import com.ecommerceserver.model.dto.SubaccountQueryDTO;
import com.ecommerceserver.model.dto.SubaccountUpdateDTO;
import com.ecommerceserver.model.vo.SubaccountVO;
import com.ecommerceserver.result.PageResult;
import com.ecommerceserver.result.Result;

public interface SubaccountService {

    // 创建子账号
    Result<?> createSubaccount(SubaccountCreateDTO createDTO);

    // 查询子账号列表
    PageResult<SubaccountVO> querySubaccounts(Integer pageNum, Integer pageSize, SubaccountQueryDTO queryDTO);

    // 修改子账号
    SubaccountVO updateSubaccount(Integer subaccountId, SubaccountUpdateDTO updateDTO);

    // 删除子账号
    Integer deleteSubaccount(Integer subaccountId);

}
