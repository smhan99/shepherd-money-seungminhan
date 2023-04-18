package com.shepherdmoney.interviewproject.exception;

public class ApiRequestException extends RuntimeException {

    public ApiRequestException(String message) {
        super(message);
    }

    public ApiRequestException(String message, Throwable t) {
        super(message, t);
    }
}