package com.abdecd.tokenlogin.aspect;

import com.abdecd.tokenlogin.common.context.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
@Aspect
public class RequirePermissionAspect {
    @Around("@annotation(RequirePermission) || @within(RequirePermission) && execution(public * *(..))")
    public Object requirePermission(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature))
            throw new IllegalStateException("不支持非方法切入点");

        // get annotation
        RequirePermission requirePermission;
        Method method = methodSignature.getMethod();
        requirePermission = method.getAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = method.getDeclaringClass().getAnnotation(RequirePermission.class);
        }
        if (requirePermission == null) {
            throwException(RuntimeException.class, "Permission check failed");
            return null; // never reach
        }

        // check permission
        var permissions = requirePermission.value();
        var userPermissions = List.of(UserContext.getPermission().split(","));
        for (var permission : permissions) {
            if (userPermissions.contains(permission)) return joinPoint.proceed();
        }
        throwException(requirePermission.exception(), "Permission denied");
        return null; // never reach
    }

    private void throwException(Class<? extends RuntimeException> exceptionClass, String errMessage) {
        RuntimeException exception;
        try {
            exception = exceptionClass.getConstructor(String.class).newInstance(errMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw exception;
    }
}
