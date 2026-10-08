# DBMS Report Content - Employee Leave Management System

## 1. Title
Employee Leave Management System using Java Swing, JDBC and MySQL

## 2. Abstract
This project stores employees, departments, leave types and leave requests in a normalized MySQL database and provides a Java Swing desktop application to manage them. The database was designed first (ER model, functional dependencies, normalization to 3NF/BCNF), then accessed through JDBC using the DAO pattern. It demonstrates constraints, joins, aggregation, a view, a stored procedure, a transaction and indexes.

## 3. Problem Statement
Organizations often record leave in spreadsheets, where employee and department details are repeated on every row, which causes inconsistency and makes searching and reporting hard. A relational database with enforced rules is needed to record employees and leave requests reliably.

## 4. Objectives
* Design a normalized relational database for leave management.
* Enforce data integrity with keys and constraints.
* Provide CRUD operations for employees and leave requests through a GUI.
* Demonstrate joins, aggregates, views, stored procedures, transactions and indexes.

## 5. Proposed System
A four-table MySQL database, accessed through a Swing application: GUI -> DAO -> JDBC -> MySQL.

## 6. DBMS Requirements
* An employee belongs to exactly one department; a department has many employees.
* An employee can submit many leave requests; each request belongs to one employee.
* Each request has exactly one leave type; a leave type has many requests.
* A request has dates, a reason and a status (Pending, Approved, Rejected, Cancelled; default Pending).
* Employees with leave history must not be deleted by accident.

## 7. ER Diagram
```
DEPARTMENTS  1 ----- N  EMPLOYEES  1 ----- N  LEAVE_REQUESTS  N ----- 1  LEAVE_TYPES
```
Draw it in your report as (Chen or crow's-foot notation):
* Entities as rectangles: DEPARTMENT, EMPLOYEE, LEAVE_TYPE, LEAVE_REQUEST.
* Relationships as diamonds: WORKS_IN (Department-Employee), APPLIES (Employee-LeaveRequest), HAS_TYPE (LeaveRequest-LeaveType).
* Attributes as ovals; underline primary keys.

## 8. Entities and Attributes

| Entity | Attributes (PK underlined in the diagram) |
|---|---|
| DEPARTMENT | department_id, department_name |
| EMPLOYEE | employee_id, name, email, phone |
| LEAVE_TYPE | leave_type_id, leave_type_name, max_days |
| LEAVE_REQUEST | leave_id, from_date, to_date, reason, status |

## 9-10. Relationships and Cardinality

| Relationship | Cardinality | Participation | Implemented by |
|---|---|---|---|
| Department WORKS_IN Employee | 1 : N | Employee total (every employee has a department); Department partial | `employees.department_id` FK |
| Employee APPLIES Leave Request | 1 : N | Leave request total; Employee partial (employee 106 has none) | `leave_requests.employee_id` FK |
| Leave Request HAS_TYPE Leave Type | N : 1 | Leave request total; Leave type partial | `leave_requests.leave_type_id` FK |
| Employee - Leave Type (derived) | M : N | - | resolved by the `leave_requests` table |

## 11. Relational Schema (final, frozen)
```
DEPARTMENTS(department_id PK, department_name UNIQUE NOT NULL)
EMPLOYEES(employee_id PK, name NOT NULL, department_id FK -> DEPARTMENTS, email UNIQUE NOT NULL, phone NOT NULL)
LEAVE_TYPES(leave_type_id PK, leave_type_name UNIQUE NOT NULL, max_days NOT NULL)
LEAVE_REQUESTS(leave_id PK, employee_id FK -> EMPLOYEES, leave_type_id FK -> LEAVE_TYPES,
               from_date, to_date, reason, status DEFAULT 'Pending')
```

## 12. Keys

| Relation | Primary key | Candidate keys | Foreign keys |
|---|---|---|---|
| departments | department_id | department_id, department_name | - |
| employees | employee_id | employee_id, email | department_id -> departments |
| leave_types | leave_type_id | leave_type_id, leave_type_name | - |
| leave_requests | leave_id | leave_id | employee_id -> employees; leave_type_id -> leave_types |

Prime attributes = attributes that belong to some candidate key: department_id, department_name; employee_id, email; leave_type_id, leave_type_name; leave_id. All other attributes are non-prime (for example `name`, `phone`, `max_days`, `from_date`, `to_date`, `reason`, `status`).

## 13. Anomalies in the Unnormalized Design

Hypothetical single table `LEAVE_ALL(Employee_ID, Employee_Name, Department, Email, Phone, Leave_Type, Max_Days, From_Date, To_Date, Reason, Status)`.
Assumption: an employee does not start two leaves on the same day, so the key is (Employee_ID, From_Date).

| Employee_ID | Employee_Name | Department | Email | Phone | Leave_Type | From_Date | Status |
|---|---|---|---|---|---|---|---|
| 101 | Arun Kumar | Information Technology | arun.kumar@example.com | 9876543210 | Casual Leave | 2026-08-10 | Approved |
| 101 | Arun Kumar | Information Technology | arun.kumar@example.com | 9876543210 | Sick Leave | 2026-09-03 | Approved |
| 101 | Arun Kumar | Information Technology | arun.kumar@example.com | 9876543210 | Casual Leave | 2026-10-20 | Pending |

If employee 101 applies for five leaves, name, department, email and phone are stored five times (redundancy), wasting space and risking inconsistency.

* **Update anomaly:** changing Arun's phone number must change every one of his rows. If one row is missed, the database holds two different phone numbers for one person.
* **Insertion anomaly:** a new employee, a new department or a new leave type cannot be stored until some leave request exists (or NULL leave columns would be needed, and the key `From_Date` cannot be NULL).
* **Deletion anomaly:** deleting an employee's only leave request also erases the employee's name, email and phone, and possibly the only record that the department or leave type exists.

## 14. Functional Dependencies

```
employee_id   -> name, department_id, email, phone
email         -> employee_id, name, department_id, phone       (email is UNIQUE)
department_id -> department_name
department_name -> department_id                               (UNIQUE)
leave_type_id -> leave_type_name, max_days
leave_type_name -> leave_type_id, max_days                     (UNIQUE)
leave_id      -> employee_id, leave_type_id, from_date, to_date, reason, status
```
Why they hold: an employee ID identifies exactly one person with one department, e-mail and phone; a department ID has one name; each leave type has one name and one maximum-days value; each leave ID is one request with exactly one employee, type, dates, reason and status.

In the unnormalized table: `Employee_ID -> Name, Department, Email, Phone`, `Leave_Type -> Max_Days`, and `(Employee_ID, From_Date) -> Leave_Type, To_Date, Reason, Status`.

## 15. Normalization

### 1NF - atomic values, no repeating groups
A first-draft record such as
`101 | Arun Kumar | Casual Leave, Sick Leave | 2026-08-10, 2026-09-03` stores several values in one cell, which violates 1NF. Splitting it into one row per leave request gives atomic values in every column (the table `LEAVE_ALL` above). In the final design every column holds one value: dates are DATE columns, status is a single word, and each leave is its own row.

### 2NF - no partial dependency on part of a composite key
`LEAVE_ALL` has the composite key (Employee_ID, From_Date). The dependency `Employee_ID -> Employee_Name, Department, Email, Phone` depends on only part of the key, so it is a **partial dependency** and `LEAVE_ALL` is not in 2NF. Decomposition: move employee data to its own relation `EMPLOYEES(employee_id, ...)`, keeping the leave data separate. A surrogate key `leave_id` identifies each request.

In the final relations every primary key is a single attribute, and a partial dependency requires a composite key. Therefore partial dependencies cannot occur, and all four relations are in 2NF. (No composite key was invented to demonstrate this.)

### 3NF - no transitive dependency of a non-prime attribute on the key
Before decomposition: `employee_id -> department_id` and `department_id -> department_name`, so `department_name` depends on the key only through `department_id` (a **transitive dependency**). Similarly `leave_id -> leave_type_id -> max_days`. Decomposition: `departments(department_id, department_name)` and `leave_types(leave_type_id, leave_type_name, max_days)`. Employees and leave requests store only the foreign keys.

After decomposition, in every relation each non-prime attribute depends only on the key. All four relations are in 3NF.

### BCNF analysis
A relation is in BCNF if the left side (determinant) of every non-trivial functional dependency is a superkey.

| Relation | Non-trivial FDs and their determinants | Are determinants superkeys? | BCNF? |
|---|---|---|---|
| departments | department_id -> department_name; department_name -> department_id | Both are candidate keys | Yes |
| employees | employee_id -> others; email -> others | Both are candidate keys | Yes |
| leave_types | leave_type_id -> others; leave_type_name -> others | Both are candidate keys | Yes |
| leave_requests | leave_id -> all other attributes | leave_id is the key | Yes |

All four relations satisfy BCNF, and BCNF is stricter than 3NF: 3NF allows a dependency whose determinant is not a superkey if the dependent attribute is prime; BCNF does not. Here no such dependency exists. The rule "an employee cannot have overlapping active leaves" is a business rule between rows, not a functional dependency, so it does not affect BCNF (the application checks it in a transaction). `phone` is not unique (two employees may share a phone), so no FD `phone -> ...` is claimed.

## 16. Final Normalized Schema
See section 11.

## 17. Constraints

| Constraint | Where | Purpose |
|---|---|---|
| PRIMARY KEY | every table | unique row identity |
| AUTO_INCREMENT | department_id, leave_type_id, leave_id | automatic IDs |
| FOREIGN KEY ... ON DELETE RESTRICT | employees.department_id, leave_requests.employee_id, leave_requests.leave_type_id | referential integrity, no accidental cascade |
| UNIQUE | department_name, leave_type_name, employees.email | no duplicates |
| NOT NULL | all columns | required data |
| DEFAULT 'Pending' | leave_requests.status | new requests start as Pending |
| CHECK status IN (...) | leave_requests | only the four valid statuses |
| CHECK to_date >= from_date | leave_requests | valid date range |
| CHECK max_days > 0 | leave_types | sensible limit |

## 18. SQL Table Creation, Sample Data
See `database/employee_leave_management.sql`. Creation order: departments, leave_types, employees, leave_requests. Sample data: 4 departments, 4 leave types, 6 employees, 9 leave requests (4 Approved, 3 Pending, 1 Rejected, 1 Cancelled). Employee 106 has no leave (used for LEFT JOIN and deletion demos). The `max_days` values are sample project values, not real company policy.

## 19. SQL Queries
All queries are in `database/sample_queries.sql`; results below are for the sample data.

| Concept | Query idea | Result on sample data |
|---|---|---|
| Basic SELECT | `SELECT * FROM employees` | 6 rows |
| WHERE | `status = 'Pending'` | leaves 3, 5, 8 |
| ORDER BY | employees by name | alphabetical list |
| LIKE | `name LIKE '%ar%'` | Arun Kumar, Priya Sharma, Karthik Raj |
| INNER JOIN | employees + departments | 6 rows, department names via JOIN |
| Multi-table JOIN | leave_requests + employees + departments + leave_types | 9 rows with name, department, leave type, dates, status |
| LEFT JOIN | employees with `leave_id IS NULL` | Divya Menon (106). LEFT JOIN keeps employees that have no matching leave; INNER JOIN would drop them |
| GROUP BY + COUNT | leaves per employee | 101: 3, 102: 2, 104: 2, 103: 1, 105: 1, 106: 0 |
| HAVING | `HAVING COUNT(*) > 1` | 101, 102, 104. HAVING filters groups; WHERE filters rows |
| MIN / MAX | earliest from_date, latest to_date per employee | one row per employee with leaves |
| Subquery | HAVING count > average count per employee (1.8) | 101, 102, 104 |
| Date filtering | leaves within October 2026 | leaves 3, 7, 9 |
| Status filtering | `status = 'Approved'` | 4 rows |

## 20. Views
A **view** is a stored SELECT that behaves like a virtual table. `employee_leave_details` joins all four tables and exposes: employee_id, employee_name, department_name, leave_id, leave_type, from_date, to_date, reason, status.
Benefits: the 4-table JOIN is written once; the Java code uses a simple `SELECT ... FROM employee_leave_details WHERE ...`; the stored procedure reuses it; if the table layout changes, only the view changes. It stores no data itself, so it is always current.

## 21. Stored Procedure
`GetEmployeeLeaves(IN emp_id INT)` returns every leave request of one employee (read from the view, ordered by date). Java calls it using `CallableStatement` with `{CALL GetEmployeeLeaves(?)}` in `LeaveDAO.getLeavesByEmployee`, triggered by the button on the Leave Records screen. A stored procedure is a named, precompiled group of SQL statements stored in the database. It is used for this one operation only, where it is a good fit; all other operations use PreparedStatement.

## 22. Transactions
`LeaveDAO.addLeaveRequest` runs as one transaction:
1. `setAutoCommit(false)`
2. `SELECT ... FOR UPDATE` on the employee row (locks the row; also checks the employee exists)
3. check that no Pending/Approved leave of that employee overlaps the new dates
4. `INSERT` the request with status Pending
5. `commit()`; on any error or broken rule, `rollback()`

Why it is meaningful: the overlap check and the insert must behave as one unit. Without the lock, two users could submit overlapping leaves for the same employee at the same moment, both pass the check, and both be inserted. Locking the employee row makes the second user wait until the first commits, so the second check sees the first insert.

* **Atomicity:** all steps succeed together, or none takes effect.
* **COMMIT** makes changes permanent; **ROLLBACK** undoes everything since the transaction started.
* Autocommit is switched back on in `finally`.

## 23. Indexes
| Index | Column | Why |
|---|---|---|
| `idx_employees_department` | employees.department_id | join employees with departments |
| `idx_leave_employee` | leave_requests.employee_id | per-employee leave lookup, joins, procedure |
| `idx_leave_type` | leave_requests.leave_type_id | join with leave_types |
| `idx_leave_status` | leave_requests.status | filtering by status |

An index is a sorted lookup structure (a B-tree in InnoDB), so MySQL can find matching rows without scanning the whole table. Primary keys and UNIQUE columns are indexed automatically. The cost is extra storage and slightly slower inserts, so only commonly searched columns are indexed. (InnoDB also needs an index on each foreign key column, so these four serve both purposes.) Use `SHOW INDEX FROM leave_requests;` and `EXPLAIN` in your report.

## 24. JDBC Architecture
Java application -> JDBC API (`java.sql`) -> MySQL Connector/J driver -> MySQL server.
* `DriverManager.getConnection(URL, user, password)` in `DatabaseConnection` (the one place with settings)
* `PreparedStatement` for all queries with user input (stops SQL injection)
* `CallableStatement` for the stored procedure
* `ResultSet` to read rows, `SQLException` for database errors
* try-with-resources closes every Connection, Statement and ResultSet
* `setAutoCommit / commit / rollback` for the transaction

## 25. DAO Pattern
Each table has a DAO class holding all its SQL: `DepartmentDAO`, `EmployeeDAO`, `LeaveTypeDAO`, `LeaveDAO`. Panels call methods such as `leaveDAO.updateLeaveStatus(5, "Approved")`, never SQL. Result: GUI and database code can be changed independently, SQL is in one place, and the design is easy to explain.

## 26. GUI
`MainFrame` (extends `JFrame`) with a left navigation menu and a `CardLayout`. Five panels (extend `JPanel`): Employee Management, Apply Leave, Leave Records, Search Leave, Update Leave Status. Drop-downs for departments and leave types are filled from MySQL. Event handling uses `ActionListener` on buttons (written as lambdas), `ListSelectionListener` on tables, and a `FocusListener` on the Employee ID field in Apply Leave.

## 27. CRUD Operations

| CRUD | Operation | DAO method | SQL |
|---|---|---|---|
| Create | Add Employee | `EmployeeDAO.addEmployee` | INSERT |
| Create | Submit Leave | `LeaveDAO.addLeaveRequest` | SELECT FOR UPDATE + INSERT (transaction) |
| Read | View Employees | `EmployeeDAO.getEmployees` | SELECT + INNER JOIN |
| Read | Leave Records | `LeaveDAO.getAllLeaveRequests` | SELECT from view |
| Read | Search Leave | `LeaveDAO.searchLeaveRequests` | SELECT ... WHERE / LIKE / AND |
| Read | Employee leaves | `LeaveDAO.getLeavesByEmployee` | CALL stored procedure |
| Update | Update Employee | `EmployeeDAO.updateEmployee` | UPDATE |
| Update | Update Leave Status | `LeaveDAO.updateLeaveStatus` | UPDATE |
| Update | Cancel Leave | `LeaveDAO.cancelLeave` | UPDATE status = 'Cancelled' |
| Delete | Delete Employee | `EmployeeDAO.deleteEmployee` (after `hasLeaveRequests`) | DELETE |
| Delete | Delete Leave | `LeaveDAO.deleteLeaveRequest` | DELETE |

**Why Cancel is an UPDATE, not a DELETE:** leave history is business data. Keeping the row with status "Cancelled" preserves who applied, when, and what happened, which supports audits and counting. Delete exists separately for removing mistakes.

**Why employees with leave history cannot be deleted:** the GUI checks `hasLeaveRequests()` and shows "Cannot delete employee because leave records exist." The foreign key with `ON DELETE RESTRICT` enforces the same rule inside MySQL, even if someone bypasses the application.

## 28. Additional business rules in the application
* Leave days (inclusive) cannot exceed the leave type's `max_days`.
* A new request cannot overlap a Pending/Approved request of the same employee.
* New requests always start as Pending.

## 29-31. Test Cases, Results, see `docs/TESTING_AND_VIVA.md`.

## 32. Conclusion
The project builds a normalized (3NF, also BCNF) relational database and a JDBC-based Swing application on top of it, demonstrating keys, constraints, joins, aggregation, a view, a stored procedure, a transaction and indexes, with CRUD operations and input validation.

## 33. Future Enhancements
Leave balance tracking per employee per year, report screens for the aggregate queries, department and leave-type maintenance screens, holiday calendars excluded from leave-day counts, and user login with roles.
