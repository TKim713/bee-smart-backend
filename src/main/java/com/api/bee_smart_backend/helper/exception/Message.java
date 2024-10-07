package com.api.bee_smart_backend.helper.exception;

	public class Message{

	    private String status;
	    private String message;
	public Message(String message, String status){
        this.message = message;
        this.status = status;
    }
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
