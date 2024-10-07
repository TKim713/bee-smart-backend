package com.api.bee_smart_backend.helper.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public class ResourceNotFound extends RuntimeException{

    private static final long serialVersionUID = 1L;
    private String status;
	public ResourceNotFound(String message, String status){
        super(message);
        this.status = status;
    }
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
}
