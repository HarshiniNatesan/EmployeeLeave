package com.employeeleave.dao;

import com.employeeleave.database.DatabaseConnection;
import com.employeeleave.model.LeaveRequest;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** DAO for the leave_requests table. READ operations use the employee_leave_details view. */
public class LeaveDAO {

    // Columns of the view employee_leave_details
    private static final String VIEW_SELECT =
            "SELECT leave_id, employee_id, employee_name, department_name, leave_type, "
            + "from_date, to_date, reason, status FROM employee_leave_details";

    /**
     * CREATE - runs as ONE TRANSACTION (all steps succeed, or nothing is saved):
     *   1. lock the employee row (SELECT ... FOR UPDATE)
     *   2. check that the dates do not overlap another Pending/Approved leave of the employee
     *   3. INSERT the new request with status Pending
     * If a rule is broken, the transaction is rolled back and IllegalArgumentException is thrown.
     *
     * @return the generated leave_id
     */
    public int addLeaveRequest(LeaveRequest leave) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            try {
                con.setAutoCommit(false);                       // start transaction

                if (!lockEmployee(con, leave.getEmployeeId())) {
                    throw new IllegalArgumentException("Employee not found.");
                }
                if (hasOverlappingLeave(con, leave)) {
                    throw new IllegalArgumentException(
                            "This employee already has a Pending or Approved leave overlapping these dates.");
                }
                int leaveId = insertLeave(con, leave);

                con.commit();                                   // all steps succeeded
                return leaveId;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();                                 // undo everything
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private boolean lockEmployee(Connection con, int employeeId) throws SQLException {
        String sql = "SELECT employee_id FROM employees WHERE employee_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasOverlappingLeave(Connection con, LeaveRequest leave) throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_requests "
                + "WHERE employee_id = ? AND status IN ('Pending', 'Approved') "
                + "AND from_date <= ? AND to_date >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, leave.getEmployeeId());
            ps.setDate(2, Date.valueOf(leave.getToDate()));
            ps.setDate(3, Date.valueOf(leave.getFromDate()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int insertLeave(Connection con, LeaveRequest leave) throws SQLException {
        String sql = "INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leave.getEmployeeId());
            ps.setInt(2, leave.getLeaveTypeId());
            ps.setDate(3, Date.valueOf(leave.getFromDate()));
            ps.setDate(4, Date.valueOf(leave.getToDate()));
            ps.setString(5, leave.getReason());
            ps.setString(6, leave.getStatus());                 // "Pending"
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /** READ (all) - from the view. */
    public List<LeaveRequest> getAllLeaveRequests() throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = VIEW_SELECT + " ORDER BY leave_id";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** READ (one) - from the view. Returns null if the leave request does not exist. */
    public LeaveRequest getLeaveById(int leaveId) throws SQLException {
        String sql = VIEW_SELECT + " WHERE leave_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * READ (search) - filtering is done by SQL (WHERE / LIKE / AND).
     * Pass null (or empty text) for any criterion that should be ignored.
     * Only fixed SQL fragments are appended; user values always go through '?' parameters.
     */
    public List<LeaveRequest> searchLeaveRequests(Integer leaveId, Integer employeeId,
                                                  String employeeName, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(VIEW_SELECT + " WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();

        if (leaveId != null) {
            sql.append(" AND leave_id = ?");
            parameters.add(leaveId);
        }
        if (employeeId != null) {
            sql.append(" AND employee_id = ?");
            parameters.add(employeeId);
        }
        if (employeeName != null && !employeeName.trim().isEmpty()) {
            sql.append(" AND employee_name LIKE ?");
            parameters.add("%" + employeeName.trim() + "%");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            parameters.add(status.trim());
        }
        sql.append(" ORDER BY leave_id");

        List<LeaveRequest> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** READ via the STORED PROCEDURE GetEmployeeLeaves(emp_id) using CallableStatement. */
    public List<LeaveRequest> getLeavesByEmployee(int employeeId) throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             CallableStatement cs = con.prepareCall("{CALL GetEmployeeLeaves(?)}")) {
            cs.setInt(1, employeeId);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** UPDATE status. Returns true if a row was changed. */
    public boolean updateLeaveStatus(int leaveId, String newStatus) throws SQLException {
        String sql = "UPDATE leave_requests SET status = ? WHERE leave_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, leaveId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Cancel = UPDATE status to 'Cancelled'. The row is kept so the history is preserved. */
    public boolean cancelLeave(int leaveId) throws SQLException {
        return updateLeaveStatus(leaveId, "Cancelled");
    }

    /** DELETE. Returns true if a row was deleted. */
    public boolean deleteLeaveRequest(int leaveId) throws SQLException {
        String sql = "DELETE FROM leave_requests WHERE leave_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Converts one row of employee_leave_details (or the stored procedure result) to an object. */
    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest leave = new LeaveRequest();
        leave.setLeaveId(rs.getInt("leave_id"));
        leave.setEmployeeId(rs.getInt("employee_id"));
        leave.setEmployeeName(rs.getString("employee_name"));
        leave.setDepartmentName(rs.getString("department_name"));
        leave.setLeaveTypeName(rs.getString("leave_type"));
        leave.setFromDate(rs.getDate("from_date").toLocalDate());
        leave.setToDate(rs.getDate("to_date").toLocalDate());
        leave.setReason(rs.getString("reason"));
        leave.setStatus(rs.getString("status"));
        return leave;
    }
}
