package com.ecommerceserver.constants;

/**
 * @author dawn
 * @date 2024-09-10 21:48
 */
public class CommonConstant {
    /**
     * 加密盐值
     */
    public static final String SALT = "techPilotServer";

    /**
     * 当前登录人员id
     */
    public static final String LOGIN_USER_ID = "userId";

    /**
     * 当前用户的类型
     */
    public static final String LOGIN_USER_TYPE = "type";



    /**
     * 手机号正则
     */
    public static final String PHONE_REGEX = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";



    public static final long TEMP_SESSION_EXPIRE_SECONDS=24*60*60;


    public static final int ACCESSLIMIT_SECONG = 3;
    public static final int ACCESSLIMIT_MAX_COUNT = 10;
}
