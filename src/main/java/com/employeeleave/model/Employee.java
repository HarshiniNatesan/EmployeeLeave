package com.employeeleave.model;

/** Maps to table: employees(employee_id, name, department_id, email, phone). */
public class Employee {
    private int employeeId;
    private String name;
    private int departmentId;
    private String email;
    private String phone;

    // DISPLAY-ONLY derived field (not a column of employees): filled by a JOIN with departments
    private String departmentName;

    public Employee() {
    }

    public Employee(int employeeId, String name, int departmentId, String email, String phone) {
        this.employeeId = employeeId;
        this.name = name;
        this.departmentId = departmentId;
        this.email = email;
        this.phone = phone;
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    @Override
    public String toString() {
        return employeeId + " - " + name;
    }
}
