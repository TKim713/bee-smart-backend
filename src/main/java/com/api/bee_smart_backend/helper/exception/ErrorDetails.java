package com.api.bee_smart_backend.helper.exception;


public class ErrorDetails {
	private String message;
	private String status;
	public ErrorDetails() {
	
	}
	
	public ErrorDetails(String message,String status) {
		this.message = message;
		this.status = status;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
