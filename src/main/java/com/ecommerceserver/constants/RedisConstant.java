package com.ecommerceserver.constants;

/**
 * @author dawn
 * @date 2024-09-09 10:26
 * Redis常量
 */
public class RedisConstant {

    //临时用户前缀
    public static final String TEMP_USER_SESSION = "temp:user:identifier:";


    //存储登录用户
    public static final String LOGIN_USER_TOKEN = "login:user:key:";

    public static final String STATS_PVTOTAL_KEY = "stats:pv:total";

    public static final String STATS_PVDAY_KEY = "stats:pv:";

    public static final String STATS_UVDAY_KEY = "stats:uv:";


    public static final String STATS_PAGE_KEY = "stats:page_rank";


    // 验证码存储key前缀（与SMSUtil中一致）
    public static final String CODE_MESSAGE_KEY = "message:code:";
    // 验证码发送限流key前缀
    public static final String SMS_LIMIT_KEY = "sms:limit:";
    //邮箱验证码发送限流key前缀
    public static final String EMAIL_LIMIT_KEY = "email:limit:";
    //邮箱验证码存储key前缀
    public static final String EMAIL_CODE_KEY = "email:code:";


}
