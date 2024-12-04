package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CustomerRequest;
import com.api.bee_smart_backend.helper.response.CustomerResponse;
import com.api.bee_smart_backend.model.Customer;
import com.api.bee_smart_backend.model.Student;
import com.api.bee_smart_backend.repository.CustomerRepository;
import com.api.bee_smart_backend.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private final CustomerRepository customerRepository;

    private final MapData mapData;

    @Override
    public List<CustomerResponse> getListCustomer() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(this::mapCustomerToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse updateCustomerById(String customerId, CustomerRequest customerRequest) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));

        customer.setFullName(customerRequest.getFullName());
        customer.setDistrict(customerRequest.getDistrict());
        customer.setCity(customerRequest.getCity());
        customer.setDateOfBirth(customerRequest.getDateOfBirth());
        customer.setPhone(customerRequest.getPhone());
        customer.setAddress(customerRequest.getAddress());

        if (customer instanceof Student student) {
            student.setGrade(customerRequest.getGrade());
            student.setClassName(customerRequest.getClassName());
            student.setSchool(customerRequest.getSchool());
        }

        Customer updatedCustomer = customerRepository.save(customer);

        return mapCustomerToResponse(updatedCustomer);
    }

    private CustomerResponse mapCustomerToResponse(Customer customer) {
        CustomerResponse response = mapData.mapOne(customer, CustomerResponse.class);

        if (customer instanceof Student student) {
            response.setGrade(student.getGrade());
            response.setClassName(student.getClassName());
            response.setSchool(student.getSchool());
        }

        return response;
    }

}
