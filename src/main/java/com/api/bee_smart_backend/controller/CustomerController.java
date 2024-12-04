package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CustomerRequest;
import com.api.bee_smart_backend.helper.response.CustomerResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<ResponseObject<List<CustomerResponse>>> getListCustomer() {
        try {
            List<CustomerResponse> responses = customerService.getListCustomer();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Danh sách khách hàng", responses));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi lấy danh sách khách hàng", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<ResponseObject<CustomerResponse>> updateCustomerById(
            @PathVariable("customerId") String customerId,
            @Valid @RequestBody CustomerRequest customerRequest) {

        try {
            CustomerResponse updatedCustomer = customerService.updateCustomerById(customerId, customerRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Cập nhật thành công", updatedCustomer));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }
}
