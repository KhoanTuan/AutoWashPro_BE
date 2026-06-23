package com.autowashpro.autowashpro_be.common.openapi;

import io.swagger.v3.oas.annotations.Hidden;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ẩn endpoint khỏi Swagger UI — API vẫn hoạt động bình thường.
 * Dùng cho API legacy hoặc chưa tích hợp FE.
 */
@Hidden
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiHidden {
}
