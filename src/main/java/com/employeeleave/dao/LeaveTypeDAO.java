package com.employeeleave.dao;

import com.employeeleave.database.DatabaseConnection;
import com.employeeleave.model.LeaveType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO for the leave_types table (read-only in this project). */
public class LeaveTypeDAO {

    public List<LeaveType> getAllLeaveTypes() throws SQLException {
        List<LeaveType> list = new ArrayList<>();
        String sql = "SELECT leave_type_id, leave_type_name, max_days FROM leave_types ORDER BY leave_type_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns null if the leave type does not exist. */
    public LeaveType getLeaveTypeById(int leaveTypeId) throws SQLException {
        String sql = "SELECT leave_type_id, leave_type_name, max_days FROM leave_types WHERE leave_type_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private LeaveType mapRow(ResultSet rs) throws SQLException {
        return new LeaveType(rs.getInt("leave_type_id"), rs.getString("leave_type_name"), rs.getInt("max_days"));
    }
}
