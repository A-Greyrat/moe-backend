package com.abdecd.moebackend.business.exceptionhandler;

public class BaseException extends RuntimeException {
    public BaseException(String msg) {
        super(msg);
    }
}
