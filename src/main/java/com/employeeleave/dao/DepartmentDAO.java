package com.employeeleave.dao;

import com.employeeleave.database.DatabaseConnection;
import com.employeeleave.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO for the departments table (read-only in this project). */
public class DepartmentDAO {

    public List<Department> getAllDepartments() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Department(rs.getInt("department_id"), rs.getString("department_name")));
            }
        }
        return list;
    }

    /** Returns null if the department does not exist. */
    public Department getDepartmentById(int departmentId) throws SQLException {
        String sql = "SELECT department_id, department_name FROM departments WHERE department_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Department(rs.getInt("department_id"), rs.getString("department_name"));
                }
            }
        }
        return null;
    }
}
