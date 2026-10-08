-- =====================================================================
-- Employee Leave Management System  |  MySQL 8.0.16 or newer
-- Run:  mysql -u root -p < database/employee_leave_management.sql
-- (or open in MySQL Workbench and run the whole script)
-- Safe to re-run: it rebuilds everything from scratch.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS employee_leave_management;
USE employee_leave_management;

-- Drop in reverse dependency order (child tables first)
DROP PROCEDURE IF EXISTS GetEmployeeLeaves;
DROP VIEW      IF EXISTS employee_leave_details;
DROP TABLE     IF EXISTS leave_requests;
DROP TABLE     IF EXISTS users;
DROP TABLE     IF EXISTS employees;
DROP TABLE     IF EXISTS leave_types;
DROP TABLE     IF EXISTS departments;

-- ---------------------------------------------------------------------
-- 1. departments  (parent of employees)
-- ---------------------------------------------------------------------
CREATE TABLE departments (
    department_id   INT          NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (department_id),
    CONSTRAINT uq_department_name UNIQUE (department_name)
);

-- ---------------------------------------------------------------------
-- 2. leave_types  (parent of leave_requests)
--    max_days values are SAMPLE project values, not real company policy.
-- ---------------------------------------------------------------------
CREATE TABLE leave_types (
    leave_type_id   INT         NOT NULL AUTO_INCREMENT,
    leave_type_name VARCHAR(50) NOT NULL,
    max_days        INT         NOT NULL,
    CONSTRAINT pk_leave_types PRIMARY KEY (leave_type_id),
    CONSTRAINT uq_leave_type_name UNIQUE (leave_type_name),
    CONSTRAINT chk_max_days CHECK (max_days > 0)
);

-- ---------------------------------------------------------------------
-- 3. employees  (child of departments)
--    employee_id is entered by the user, so it is not AUTO_INCREMENT.
-- ---------------------------------------------------------------------
CREATE TABLE employees (
    employee_id   INT          NOT NULL,
    name          VARCHAR(100) NOT NULL,
    department_id INT          NOT NULL,
    email         VARCHAR(100) NOT NULL,
    phone         VARCHAR(15)  NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (employee_id),
    CONSTRAINT uq_employee_email UNIQUE (email),
    INDEX idx_employees_department (department_id),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id)
        REFERENCES departments (department_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- 3b. users  (login accounts; child of employees)
--     role 'Admin'    -> employee_id is NULL
--     role 'Employee' -> employee_id points to the employee who owns the account
--     password_hash   -> SHA-256 hex (64 chars); plain passwords are never stored.
--     ON DELETE CASCADE here only removes the login account of a deleted employee;
--     leave history is still protected (leave_requests uses RESTRICT).
--     (MySQL does not allow CHECK constraints on a column that has a CASCADE
--      foreign key, so the role/employee_id pairing is enforced by the application.)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash CHAR(64)     NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    employee_id   INT          NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_employee UNIQUE (employee_id),
    CONSTRAINT chk_user_role CHECK (role IN ('Admin', 'Employee')),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id)
        REFERENCES employees (employee_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- 4. leave_requests  (child of employees and leave_types)
--    No ON DELETE CASCADE: an employee with leave history cannot be deleted.
-- ---------------------------------------------------------------------
CREATE TABLE leave_requests (
    leave_id      INT          NOT NULL AUTO_INCREMENT,
    employee_id   INT          NOT NULL,
    leave_type_id INT          NOT NULL,
    from_date     DATE         NOT NULL,
    to_date       DATE         NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    CONSTRAINT pk_leave_requests PRIMARY KEY (leave_id),
    CONSTRAINT chk_leave_status CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Cancelled')),
    CONSTRAINT chk_leave_dates  CHECK (to_date >= from_date),
    INDEX idx_leave_employee (employee_id),
    INDEX idx_leave_type     (leave_type_id),
    INDEX idx_leave_status   (status),
    CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id)
        REFERENCES employees (employee_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_leave_type FOREIGN KEY (leave_type_id)
        REFERENCES leave_types (leave_type_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- Sample data (parents first, then children)
-- ---------------------------------------------------------------------
INSERT INTO departments (department_name) VALUES
    ('Information Technology'),
    ('Human Resources'),
    ('Finance'),
    ('Administration');

INSERT INTO leave_types (leave_type_name, max_days) VALUES
    ('Casual Leave',    12),
    ('Sick Leave',      15),
    ('Earned Leave',    20),
    ('Emergency Leave',  5);

INSERT INTO employees (employee_id, name, department_id, email, phone) VALUES
    (101, 'Arun Kumar',   1, 'arun.kumar@example.com',   '9876543210'),
    (102, 'Priya Sharma', 2, 'priya.sharma@example.com', '9123456780'),
    (103, 'Rahul Verma',  3, 'rahul.verma@example.com',  '9988776655'),
    (104, 'Sneha Reddy',  1, 'sneha.reddy@example.com',  '9871234560'),
    (105, 'Karthik Raj',  4, 'karthik.raj@example.com',  '9765432108'),
    (106, 'Divya Menon',  2, 'divya.menon@example.com',  '9654321870');   -- no leave requests

INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason, status) VALUES
    (101, 1, '2026-08-10', '2026-08-12', 'Family function',        'Approved'),
    (101, 2, '2026-09-03', '2026-09-04', 'Fever and cold',         'Approved'),
    (101, 1, '2026-10-20', '2026-10-22', 'Personal work',          'Pending'),
    (102, 2, '2026-09-15', '2026-09-16', 'Medical checkup',        'Approved'),
    (102, 3, '2026-11-02', '2026-11-06', 'Vacation',               'Pending'),
    (103, 4, '2026-09-28', '2026-09-28', 'Family emergency',       'Rejected'),
    (104, 1, '2026-10-12', '2026-10-13', 'Sibling wedding',        'Cancelled'),
    (104, 3, '2026-12-21', '2026-12-24', 'Year-end trip',          'Pending'),
    (105, 2, '2026-10-01', '2026-10-02', 'Dental treatment',       'Approved');

-- Login accounts (SAMPLE passwords, for demonstration only)
--   admin  / admin123          (Admin)
--   emp101 / emp@101  ...  emp106 / emp@106   (Employee, linked to employee 101..106)
INSERT INTO users (username, password_hash, role, employee_id) VALUES
    ('admin',  SHA2('admin123', 256), 'Admin',    NULL),
    ('emp101', SHA2('emp@101',  256), 'Employee', 101),
    ('emp102', SHA2('emp@102',  256), 'Employee', 102),
    ('emp103', SHA2('emp@103',  256), 'Employee', 103),
    ('emp104', SHA2('emp@104',  256), 'Employee', 104),
    ('emp105', SHA2('emp@105',  256), 'Employee', 105),
    ('emp106', SHA2('emp@106',  256), 'Employee', 106);

-- ---------------------------------------------------------------------
-- View: joins the 4 tables once, so queries do not repeat the JOINs
-- ---------------------------------------------------------------------
CREATE VIEW employee_leave_details AS
SELECT e.employee_id,
       e.name            AS employee_name,
       d.department_name,
       l.leave_id,
       lt.leave_type_name AS leave_type,
       l.from_date,
       l.to_date,
       l.reason,
       l.status
FROM leave_requests l
INNER JOIN employees   e  ON l.employee_id   = e.employee_id
INNER JOIN departments d  ON e.department_id = d.department_id
INNER JOIN leave_types lt ON l.leave_type_id = lt.leave_type_id;

-- ---------------------------------------------------------------------
-- Stored procedure: all leave requests of one employee
-- ---------------------------------------------------------------------
DELIMITER $$
CREATE PROCEDURE GetEmployeeLeaves(IN emp_id INT)
BEGIN
    SELECT leave_id, employee_id, employee_name, department_name,
           leave_type, from_date, to_date, reason, status
    FROM employee_leave_details
    WHERE employee_id = emp_id
    ORDER BY from_date;
END$$
DELIMITER ;
