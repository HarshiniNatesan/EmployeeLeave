package com.employeeleave.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The ONLY place where the MySQL connection settings are written.
 * Every DAO gets its connection from DatabaseConnection.getConnection().
 */
public class DatabaseConnection {

    // ==================  ENTER YOUR MYSQL DETAILS HERE  ==================
    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "Vet@1234";
    // =====================================================================

    // Database URL (database name: employee_leave_management)
    private static final String URL =
            "jdbc:mysql://localhost:3306/employee_leave_management"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // Utility class: no objects needed
    private DatabaseConnection() {
    }

    /** Returns a NEW connection. The caller must close it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, MYSQL_USERNAME, MYSQL_PASSWORD);
    }
}
