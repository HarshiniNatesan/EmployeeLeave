# Testing, Screenshots and Viva

Reset the data before testing: re-run `database/employee_leave_management.sql`.

## A. Database tests (run in MySQL Workbench / mysql client)

| # | Test | Statement | Expected result |
|---|---|---|---|
| D1 | Valid insert | `INSERT INTO employees VALUES (107,'Test User',1,'test.user@example.com','9000000001');` | 1 row inserted |
| D2 | Invalid foreign key | `INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason) VALUES (999,1,'2026-12-01','2026-12-02','x');` | Error 1452: foreign key constraint fails |
| D3 | Duplicate email | `INSERT INTO employees VALUES (110,'Dup',1,'arun.kumar@example.com','9000000002');` | Error 1062: duplicate entry |
| D4 | Invalid status | `INSERT INTO leave_requests (employee_id, leave_type_id, from_date, to_date, reason, status) VALUES (101,1,'2026-12-01','2026-12-02','x','Done');` | Error 3819: check constraint `chk_leave_status` violated |
| D5 | Invalid date range | same insert with from_date `2026-12-05`, to_date `2026-12-01` | Error 3819: check constraint `chk_leave_dates` violated |
| D6 | NULL in required field | `INSERT INTO employees (employee_id,name,department_id,email) VALUES (111,'No Phone',1,'np@example.com');` | Error 1364: field `phone` has no default value |
| D7 | Employee WITH leave history | `DELETE FROM employees WHERE employee_id = 101;` | Error 1451: cannot delete parent row (RESTRICT) |
| D8 | Employee WITHOUT leave history | `DELETE FROM employees WHERE employee_id = 106;` | 1 row deleted |
| D9 | Default status | insert a leave without `status`, then select it | status = Pending |
| D10 | Stored procedure | `CALL GetEmployeeLeaves(101);` | 3 rows |
| D11 | View | `SELECT * FROM employee_leave_details;` | 9 rows with names |

## B. Application tests

| # | Test | Steps | Expected result |
|---|---|---|---|
| A1 | Add employee | Employee Management: ID 201, Meena Iyer, Finance, meena@example.com, 9000011111 -> Add Employee | "Employee added successfully"; row in table; present in MySQL |
| A2 | Duplicate ID | Add ID 201 again | "Employee ID 201 already exists." |
| A3 | Duplicate email | New ID 202 with meena@example.com | "This email address is already used by another employee." |
| A4 | Update employee | Click row 201, change department to Administration -> Update Employee | "Employee updated successfully"; table shows Administration |
| A5 | Delete employee (no leaves) | Select 201 -> Delete -> Yes | confirmation, then deleted |
| A6 | Delete employee (with leaves) | Select 101 -> Delete | "Cannot delete employee because leave records exist." |
| A7 | Apply leave | Apply Leave: ID 106 (name and department appear), Casual Leave, 2026-11-10 to 2026-11-12, "Family event" -> Submit | success with new Leave ID, status Pending |
| A8 | View leave | Leave Records -> Refresh | new request visible with department and leave type name |
| A9 | Search leave | Search: Employee Name "Arun"; then Status = Pending; then Leave ID 3; then Employee ID 102 + Status Approved | only matching rows each time (combined with AND); View All shows 9+ |
| A10 | Stored procedure screen | Leave Records: Employee ID 101 -> Show Employee Leaves | 3 rows |
| A11 | Approve leave | Update Leave Status: Leave ID 3 -> Load -> Approved -> Update Status | confirmation; table shows Approved |
| A12 | Reject leave | Leave ID 5 -> Rejected -> Update Status | status Rejected |
| A13 | Cancel leave | Leave ID 8 -> Cancel Leave -> Yes | status Cancelled; record still exists |
| A14 | Delete leave | Leave ID 7 -> Delete Leave -> Yes | "permanently delete" confirmation; row removed |
| A15 | Invalid employee ID | Apply Leave: ID 9999 | name box shows "Employee not found"; submit shows an error; nothing inserted |
| A16 | Non-numeric ID | Apply Leave or Employee ID "abc" | "Employee ID must be a number" / "valid numeric" message |
| A17 | Empty fields | Add Employee / Submit Leave with blank boxes | clear message for the first missing field |
| A18 | Invalid email | email `arun@` | "Please enter a valid email address" |
| A19 | Invalid phone | phone `12345` or `98765abc10` | "Phone number must contain exactly 10 digits." |
| A20 | Invalid date | From Date `2026-02-30` or `15/10/2026` | "From Date is invalid. Use the format yyyy-MM-dd" |
| A21 | From after To | From 2026-12-10, To 2026-12-05 | "From Date cannot be after To Date." |
| A22 | Exceeds maximum days | Emergency Leave, 2026-12-01 to 2026-12-10 | "Emergency Leave allows a maximum of 5 days. You requested 10 days." |
| A23 | Overlapping leave (transaction rollback) | Employee 101, Casual Leave, 2026-10-21 to 2026-10-21 (overlaps Pending leave 3) | "already has a Pending or Approved leave overlapping"; nothing inserted |
| A24 | Leave not found | Update Leave Status: Leave ID 9999 -> Load | "Leave request not found" |
| A25 | Database connection failure | Stop MySQL or put a wrong password, start the app | error dialog "Database error: ..." with a hint; the app does not crash |

## C. Screenshots to capture

1. MySQL Workbench: the `employee_leave_management` database with its 4 tables, view and procedure visible
2. `SELECT * FROM departments;`
3. `SELECT * FROM employees;`
4. `SELECT * FROM leave_types;`
5. `SELECT * FROM leave_requests;`
6. ER diagram (Workbench: Database -> Reverse Engineer, or draw by hand/draw.io)
7. Employee Management screen with the employee table
8. Add Employee success message (and one validation error)
9. Apply Leave screen with name, department and maximum days displayed
10. Leave submitted message (status Pending)
11. Leave Records screen
12. Search Leave results
13. Update Leave Status screen with loaded details
14. A leave after Approved
15. A leave after Cancelled
16. Delete employee blocked message ("leave records exist")
17. MySQL multi-table JOIN output (Q6)
18. Aggregate output: GROUP BY / HAVING / subquery (Q8, Q9, Q12)
19. `SELECT * FROM employee_leave_details;` (view)
20. `CALL GetEmployeeLeaves(101);` (stored procedure)
21. `SHOW INDEX FROM leave_requests;`
22. Constraint failures from tests D2, D3, D4, D7
23. Final application overview (main window)

## D. Viva questions and answers

### DBMS
1. **Why MySQL?** It is free, popular, runs on any computer, supports foreign keys, views, procedures and transactions, and has a JDBC driver.
2. **Why is MySQL an RDBMS?** It stores data in related tables (rows and columns) and links them with keys, and it is managed with SQL.
3. **What is a primary key?** A column that uniquely identifies each row and cannot be NULL, e.g. `employee_id`.
4. **What is a candidate key?** Any column (or set) that could serve as the primary key. In `employees`, `employee_id` and `email` are candidate keys; we chose `employee_id` as primary.
5. **What is a foreign key?** A column that refers to the primary key of another table, e.g. `leave_requests.employee_id` refers to `employees.employee_id`.
6. **What is referential integrity?** A rule that a foreign key value must exist in the parent table, so there are no leave requests for non-existent employees.
7. **Why four tables?** To avoid repeating data: department and leave-type details are stored once, which removes redundancy and anomalies.
8. **Why not store the department name in employees?** It would be repeated for every employee in that department; renaming a department would need many updates. We store `department_id` and use a JOIN for the name.
9. **Why not store the leave type in leave_requests?** The same reason: leave type name and max days would repeat on every request. We store `leave_type_id`.
10. **What is normalization and why?** Organizing tables to reduce redundancy and avoid anomalies, by splitting tables according to functional dependencies.
11. **What is 1NF?** Every column holds a single atomic value, with no repeating groups.
12. **What is 2NF?** 1NF plus no partial dependency (no non-key attribute depends on only part of a composite key). Our tables have single-column keys, so it holds.
13. **What is 3NF?** 2NF plus no transitive dependency (non-key attributes depend only on the key). Example: department name depends on department_id, so it is in its own table.
14. **What is BCNF?** A stricter 3NF: for every functional dependency X -> Y, X must be a superkey. All four of our tables satisfy it.
15. **What is a functional dependency?** `A -> B` means that each value of A determines exactly one value of B, e.g. `employee_id -> name`.
16. **Insertion, deletion and update anomalies?** Insertion: cannot add an employee without a leave row. Deletion: deleting the only leave row loses the employee's details. Update: changing a phone number needs many row changes and may leave inconsistent data.
17. **What is a JOIN?** It combines rows of two or more tables based on a related column.
18. **Why LEFT JOIN?** To keep all rows of the left table even without a match, e.g. to list employees who never applied for leave.
19. **What is GROUP BY?** It groups rows with the same value so aggregate functions like COUNT can be applied per group.
20. **What is HAVING?** A filter on groups after GROUP BY (WHERE filters individual rows before grouping).
21. **What is a subquery?** A query inside another query, e.g. the average number of leaves used inside HAVING.
22. **What is a view?** A saved SELECT that acts like a virtual table. `employee_leave_details` hides the 4-table JOIN.
23. **What is a stored procedure?** A named group of SQL statements stored in the database and run with CALL, e.g. `GetEmployeeLeaves`.
24. **What is a transaction?** A group of operations that succeed or fail together. Submit Leave locks the employee row, checks for overlap, and inserts.
25. **COMMIT and ROLLBACK?** COMMIT saves the transaction permanently; ROLLBACK cancels all its changes.
26. **What is an index?** A sorted structure that lets MySQL find rows quickly without scanning the whole table.
27. **Why ON DELETE RESTRICT, not CASCADE?** CASCADE would silently delete an employee's leave history. RESTRICT blocks the delete and protects the data.
28. **Why is Cancel an UPDATE?** It keeps the history; the status becomes Cancelled.
29. **What does the CHECK constraint do?** It rejects invalid values, e.g. a status other than the four allowed ones or `to_date` before `from_date`.

### JDBC
30. **What is JDBC?** A Java API for connecting to databases and running SQL.
31. **Why do we need it?** Java cannot talk to MySQL directly; JDBC plus the MySQL driver does it.
32. **What is Connection?** An open session between Java and the database.
33. **What is PreparedStatement and why use it?** A precompiled SQL statement with `?` parameters. It prevents SQL injection and handles data types safely.
34. **What is ResultSet?** The table of rows returned by a SELECT; `rs.next()` moves through the rows.
35. **What is CallableStatement?** The JDBC object used to call stored procedures: `{CALL GetEmployeeLeaves(?)}`.
36. **How does Java communicate with MySQL?** Application -> JDBC API -> Connector/J driver -> MySQL server.
37. **Why try-with-resources?** It closes connections and statements automatically, even if an error occurs.

### Java
38. **Encapsulation:** private fields with getters and setters in the model classes.
39. **Abstraction:** the GUI calls DAO methods without knowing the SQL.
40. **Inheritance:** `MainFrame extends JFrame`, panels `extend JPanel`.
41. **Polymorphism:** overriding methods such as `isCellEditable`, `toString`, `focusLost`, and passing screens' refresh code as a `Runnable`.
42. **DAO pattern:** one class per table holding all its database code.
43. **Why separate GUI and database code?** Cleaner code, easier changes and testing, and each class has one job.
44. **Event handling:** the program reacts to user actions (clicks) through listeners such as `ActionListener`.
45. **Why Swing?** It is part of standard Java, needs no extra libraries, and is suitable for desktop applications.
