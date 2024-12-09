package com.api.bee_smart_backend.config;

public class RolePermissions {

    // Roles that can access all APIs
    public static final String[] ALL_API_ROLES = {
            "SYSTEM_ADMIN",
            "STUDENT",
            "PARENT"
    };

    // Roles for customer APIs
    public static final String[] CUSTOMER_ROLES = {
            "STUDENT",
            "PARENT"
    };

    public static final String[] PARENT_ROLES = {
            "PARENT"
    };

    public static final String[] ADMIN_ROLES = {
            "SYSTEM_ADMIN"
    };
}
