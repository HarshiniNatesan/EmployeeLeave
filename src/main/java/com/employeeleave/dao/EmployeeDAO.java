package com.employeeleave.dao;

import com.employeeleave.database.DatabaseConnection;
import com.employeeleave.model.Employee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO for the employees table. Department name is obtained with a JOIN (not stored in employees). */
public class EmployeeDAO {

    private static final String SELECT_WITH_DEPARTMENT =
            "SELECT e.employee_id, e.name, e.department_id, d.department_name, e.email, e.phone "
            + "FROM employees e INNER JOIN departments d ON e.department_id = d.department_id";

    /** CREATE */
    public void addEmployee(Employee employee) throws SQLException {
        String sql = "INSERT INTO employees (employee_id, name, department_id, email, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employee.getEmployeeId());
            ps.setString(2, employee.getName());
            ps.setInt(3, employee.getDepartmentId());
            ps.setString(4, employee.getEmail());
            ps.setString(5, employee.getPhone());
            ps.executeUpdate();
        }
    }

    /** READ (all) - INNER JOIN with departments */
    public List<Employee> getEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = SELECT_WITH_DEPARTMENT + " ORDER BY e.employee_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** READ (one). Returns null if the employee does not exist. */
    public Employee getEmployeeById(int employeeId) throws SQLException {
        String sql = SELECT_WITH_DEPARTMENT + " WHERE e.employee_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** UPDATE. Returns true if a row was changed. */
    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = "UPDATE employees SET name = ?, department_id = ?, email = ?, phone = ? WHERE employee_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setInt(2, employee.getDepartmentId());
            ps.setString(3, employee.getEmail());
            ps.setString(4, employee.getPhone());
            ps.setInt(5, employee.getEmployeeId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * DELETE. Returns true if a row was deleted.
     * The GUI calls hasLeaveRequests() first; the foreign key (ON DELETE RESTRICT)
     * is the second line of defence and throws SQLException if leave records exist.
     */
    public boolean deleteEmployee(int employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /** True if the employee has at least one row in leave_requests. */
    public boolean hasLeaveRequests(int employeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_requests WHERE employee_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee employee = new Employee(
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getInt("department_id"),
                rs.getString("email"),
                rs.getString("phone"));
        employee.setDepartmentName(rs.getString("department_name"));   // display-only
        return employee;
    }
}
