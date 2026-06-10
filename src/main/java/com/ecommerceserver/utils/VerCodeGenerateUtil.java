package com.ecommerceserver.utils;

import java.security.SecureRandom;
import java.util.Random;
public class VerCodeGenerateUtil {
   private static final String SYMBOLS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
   private static final Random RANDOM = new SecureRandom();
   public static String getVerCode() {
       char[] nonceChars = new char[4];
       for (int index = 0; index < nonceChars.length; ++index) {
           nonceChars[index] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
       }
       return new String(nonceChars);
   }
}