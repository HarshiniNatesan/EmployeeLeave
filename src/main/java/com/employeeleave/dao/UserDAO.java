package com.employeeleave.dao;

import com.employeeleave.database.DatabaseConnection;
import com.employeeleave.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** DAO for the users table (login). */
public class UserDAO {

    /**
     * Checks username + password + role. The password is hashed with SHA-256 in Java and
     * compared with the stored hash, so the plain password never goes to the database.
     * Returns the User, or null if the login details are wrong.
     */
    public User authenticate(String username, String password, String role) throws SQLException {
        String sql = "SELECT user_id, username, role, employee_id FROM users "
                + "WHERE username = ? AND password_hash = ? AND role = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int employeeIdValue = rs.getInt("employee_id");
                    Integer employeeId = rs.wasNull() ? null : Integer.valueOf(employeeIdValue);
                    return new User(rs.getInt("user_id"), rs.getString("username"),
                            rs.getString("role"), employeeId);
                }
            }
        }
        return null;
    }

    /** SHA-256 as 64 lowercase hex characters (same result as MySQL SHA2(text, 256)). */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
