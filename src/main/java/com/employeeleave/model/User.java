package com.employeeleave.model;

/**
 * Maps to table: users(user_id, username, password_hash, role, employee_id).
 * The password hash is deliberately NOT stored in this object: it is only used
 * inside UserDAO while checking the login.
 */
public class User {
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_EMPLOYEE = "Employee";

    private int userId;
    private String username;
    private String role;
    private Integer employeeId;   // null for Admin

    public User() {
    }

    public User(int userId, String username, String role, Integer employeeId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
