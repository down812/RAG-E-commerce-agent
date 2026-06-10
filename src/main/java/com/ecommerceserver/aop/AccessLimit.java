package com.ecommerceserver.aop;



import com.ecommerceserver.constants.CommonConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/** * @author dawn
 * @date 2024-10-09 16:27
 * 接口限流注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AccessLimit {
    int seconds() default CommonConstant.ACCESSLIMIT_SECONG;
    int maxCount() default CommonConstant.ACCESSLIMIT_MAX_COUNT;
}
