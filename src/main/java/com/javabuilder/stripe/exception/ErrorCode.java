package com.javabuilder.stripe.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    ;

    private final int code;
    private final String message;
    private HttpStatus httpStatus;
}
