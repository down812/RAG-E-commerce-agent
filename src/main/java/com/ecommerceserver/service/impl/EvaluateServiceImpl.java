package com.ecommerceserver.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.Enum.EvaluateEnum;
import com.ecommerceserver.constants.EvaluateConstant;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.mapper.EvaluateMapper;
import com.ecommerceserver.model.dto.EvaluateDTO;
import com.ecommerceserver.model.dto.EvaluatePageDTO;
import com.ecommerceserver.model.entity.Evaluate;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.EvaluateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

@RequiredArgsConstructor
@Service
@Slf4j
public class EvaluateServiceImpl extends ServiceImpl<EvaluateMapper, Evaluate> implements EvaluateService {
    
    @Override
    public Evaluate addEvaluate(EvaluateDTO evaluateDTO) {
        Long userId = LoginContext.getUserId();

        if (StringUtils.isEmpty(evaluateDTO.getMessageId()) || StringUtils.isEmpty(evaluateDTO.getSessionId())) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_SESSION_ID_NOT_NULL + "和" + EvaluateConstant.EVALUATE_MESSAGE_ID_NOT_NULL));
        }

        if (evaluateDTO.getRating() != EvaluateEnum.LIKE.getCode() && evaluateDTO.getRating() != EvaluateEnum.DISLIKE.getCode()) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_INVALID_RATING));
        }
        
        Evaluate evaluate = Evaluate.builder()
                .userId(userId)
                .sessionId(evaluateDTO.getSessionId())
                .messageId(evaluateDTO.getMessageId())
                .rating(evaluateDTO.getRating())
                .comment(evaluateDTO.getComment())
                .createdAt(DateUtil.formatDateTime(new Date()))
                .updatedAt(DateUtil.formatDateTime(new Date()))
                .build();
        
        this.save(evaluate);
        log.info("用户{}对消息{}添加评价：{}", userId, evaluateDTO.getMessageId(), evaluateDTO.getRating());
        return evaluate;
    }
    
    @Override
    public Evaluate getEvaluateByMessageId(String messageId) {
        LambdaQueryWrapper<Evaluate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluate::getMessageId, messageId);
        Evaluate one = this.getOne(wrapper);
        if (one == null) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_NOT_FOUND));
        }
        return one;
    }
    
    @Override
    public Evaluate getEvaluateBySessionId(String sessionId) {
        LambdaQueryWrapper<Evaluate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluate::getSessionId, sessionId);
        wrapper.orderByDesc(Evaluate::getCreatedAt);
        wrapper.last("LIMIT 1");
        Evaluate one = this.getOne(wrapper);
        if (one == null) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_NOT_FOUND));
        }
        return one;
    }
    
    @Override
    public Evaluate updateEvaluate(EvaluateDTO evaluateDTO) {
        Evaluate existingEvaluate = getEvaluateByMessageId(evaluateDTO.getMessageId());
        if (existingEvaluate == null) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_NOT_FOUND));
        }

        if (evaluateDTO.getRating() != EvaluateEnum.LIKE.getCode() && evaluateDTO.getRating() != EvaluateEnum.DISLIKE.getCode()) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_INVALID_RATING));
        }

        existingEvaluate.setRating(evaluateDTO.getRating());
        existingEvaluate.setComment(evaluateDTO.getComment());
        existingEvaluate.setUpdatedAt(DateUtil.formatDateTime(new Date()));
        
        this.updateById(existingEvaluate);
        log.info("更新消息{}的评价为：{}", evaluateDTO.getMessageId(), evaluateDTO.getRating());
        return existingEvaluate;
    }
    
    @Override
    public boolean deleteEvaluate(Long id) {
        Evaluate evaluate = this.getById(id);
        if (evaluate == null) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_NOT_FOUND));
        }
        boolean result = this.removeById(id);
        if (result) {
            log.info("删除评价ID：{}", id);
        }
        return result;
    }
    
    @Override
    public boolean deleteEvaluateByMessageId(String messageId) {
        LambdaQueryWrapper<Evaluate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluate::getMessageId, messageId);

        Evaluate evaluate = this.getOne(wrapper);
        if (evaluate == null) {
            throw new GlobalException(Result.error(EvaluateConstant.EVALUATE_NOT_FOUND));
        }

        boolean result = this.remove(wrapper);
        if (result) {
            log.info("删除消息{}的评价", messageId);
        }
        return result;
    }

    @Override
    public Page<Evaluate> getEvaluatePage(EvaluatePageDTO evaluatePageDTO) {
        Integer current = evaluatePageDTO.getCurrent() != null ? evaluatePageDTO.getCurrent() : 1;
        Integer size = evaluatePageDTO.getSize() != null ? evaluatePageDTO.getSize() : 10;
        
        Page<Evaluate> page = new Page<>(current, size);
        LambdaQueryWrapper<Evaluate> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(evaluatePageDTO.getUserId() != null, Evaluate::getUserId, evaluatePageDTO.getUserId())
                .eq(StringUtils.isNotEmpty(evaluatePageDTO.getMessageId()), Evaluate::getMessageId, evaluatePageDTO.getMessageId())
                .eq(evaluatePageDTO.getRating() != null, Evaluate::getRating, evaluatePageDTO.getRating())
                .eq(StringUtils.isNotEmpty(evaluatePageDTO.getSessionId()), Evaluate::getSessionId, evaluatePageDTO.getSessionId());
        
        wrapper.orderByDesc(Evaluate::getCreatedAt);
        
        return this.page(page, wrapper);
    }
}