package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.CustomerRequest;
import com.api.bee_smart_backend.helper.response.CustomerResponse;

import java.util.List;

public interface CustomerService {
    List<CustomerResponse> getListCustomer();

    CustomerResponse updateCustomerById(String customerId, CustomerRequest customerRequest);
}
