package com.api.bee_smart_backend.helper.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandle {
	@ExceptionHandler(ResourceNotFound.class)
	public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFound exception, WebRequest request) {
		ErrorDetails errorDetails = new ErrorDetails(exception.getMessage(), exception.getStatus());
		return new ResponseEntity<Object>(errorDetails, HttpStatus.NOT_FOUND);
		
	}
}
