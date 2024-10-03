package com.abdecd.moebackend.business.tokenLogin.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * 允许的权限值, 可以写多个
     */
    String[] value();

    /**
     * 抛出异常的类型
     */
    Class<? extends RuntimeException> exception() default RuntimeException.class;
}
