package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseObject<C> {
    private int status;
    private String message;
    private Object data;
}
