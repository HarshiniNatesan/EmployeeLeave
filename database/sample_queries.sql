-- Demonstration queries for the DBMS report (run after employee_leave_management.sql)
USE employee_leave_management;

-- Q1 Basic SELECT
SELECT * FROM employees;

-- Q2 WHERE: pending requests
SELECT * FROM leave_requests WHERE status = 'Pending';

-- Q3 ORDER BY: employees sorted by name
SELECT * FROM employees ORDER BY name;

-- Q4 LIKE: employee names containing 'ar'
SELECT employee_id, name FROM employees WHERE name LIKE '%ar%';

-- Q5 INNER JOIN: employee with department
SELECT e.employee_id, e.name, d.department_name
FROM employees e
INNER JOIN departments d ON e.department_id = d.department_id;

-- Q6 Multi-table JOIN (four tables)
SELECT e.name AS employee_name, d.department_name, lt.leave_type_name,
       l.from_date, l.to_date, l.status
FROM leave_requests l
INNER JOIN employees   e  ON l.employee_id   = e.employee_id
INNER JOIN departments d  ON e.department_id = d.department_id
INNER JOIN leave_types lt ON l.leave_type_id = lt.leave_type_id
ORDER BY l.leave_id;

-- Q7 LEFT JOIN: employees who have never applied for leave
SELECT e.employee_id, e.name
FROM employees e
LEFT JOIN leave_requests l ON e.employee_id = l.employee_id
WHERE l.leave_id IS NULL;

-- Q8 GROUP BY + COUNT: leave requests per employee
SELECT e.employee_id, e.name, COUNT(l.leave_id) AS total_leaves
FROM employees e
LEFT JOIN leave_requests l ON e.employee_id = l.employee_id
GROUP BY e.employee_id, e.name
ORDER BY total_leaves DESC;

-- Q9 HAVING: employees with more than 1 leave request
SELECT e.employee_id, e.name, COUNT(*) AS total_leaves
FROM employees e
INNER JOIN leave_requests l ON e.employee_id = l.employee_id
GROUP BY e.employee_id, e.name
HAVING COUNT(*) > 1;

-- Q10 COUNT by status
SELECT status, COUNT(*) AS total FROM leave_requests GROUP BY status;

-- Q11 MIN / MAX: earliest and latest leave dates per employee
SELECT employee_id, MIN(from_date) AS earliest_leave, MAX(to_date) AS latest_leave
FROM leave_requests
GROUP BY employee_id;

-- Q12 Subquery: employees with more leave requests than the average per employee
SELECT e.employee_id, e.name, COUNT(*) AS total_leaves
FROM employees e
INNER JOIN leave_requests l ON e.employee_id = l.employee_id
GROUP BY e.employee_id, e.name
HAVING COUNT(*) > (SELECT AVG(cnt)
                   FROM (SELECT COUNT(*) AS cnt
                         FROM leave_requests
                         GROUP BY employee_id) AS per_employee);

-- Q13 Date filtering: leaves that fall between two dates
SELECT * FROM leave_requests
WHERE from_date >= '2026-10-01' AND to_date <= '2026-10-31';

-- Q14 Status filtering
SELECT * FROM leave_requests WHERE status = 'Approved';

-- Q15 Using the view
SELECT * FROM employee_leave_details ORDER BY leave_id;

-- Q16 Calling the stored procedure
CALL GetEmployeeLeaves(101);

-- Q17 Show indexes
SHOW INDEX FROM leave_requests;

-- Constraint tests (each one SHOULD fail with an error)
-- INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason) VALUES (999, 1, '2026-12-01', '2026-12-02', 'x');   -- invalid foreign key
-- INSERT INTO employees VALUES (110, 'Test', 1, 'arun.kumar@example.com', '9000000000');                                                -- duplicate email
-- INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason, status) VALUES (101, 1, '2026-12-01', '2026-12-02', 'x', 'Done'); -- invalid status
-- INSERT INTO employees (employee_id, name, department_id, email) VALUES (111, 'No Phone', 1, 'np@example.com');                       -- NOT NULL (phone)
-- DELETE FROM employees WHERE employee_id = 101;                                                                                        -- blocked: has leave history
-- DELETE FROM employees WHERE employee_id = 106;                                                                                        -- allowed: no leave history
