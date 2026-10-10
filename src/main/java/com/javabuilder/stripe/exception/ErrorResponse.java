package com.javabuilder.stripe.exception;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse implements Serializable {
	private int code;
	private String error;
	private String message;
	private Date timestamp;
	private String path;
}
