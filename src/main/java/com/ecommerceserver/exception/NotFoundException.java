package com.ecommerceserver.exception;

/**
 * 资源未找到异常
 */
public class NotFoundException extends GlobalException {
  public NotFoundException(String message) {
    super(message);
  }
}