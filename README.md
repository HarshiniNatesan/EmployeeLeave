# Employee Leave Management System (DBMS Mini-Project)

Java Swing + JDBC + MySQL 8 desktop application. Database-first design with four normalized tables:
`departments`, `employees`, `leave_types`, `leave_requests`.

Documents:
* `docs/DBMS_REPORT.md` - report content: ER model, functional dependencies, 1NF/2NF/3NF/BCNF, constraints, queries, view, procedure, transaction, indexes, JDBC, DAO, CRUD
* `docs/TESTING_AND_VIVA.md` - test cases, screenshot list, viva questions and answers
* `database/employee_leave_management.sql` - full database script
* `database/sample_queries.sql` - demonstration queries (JOIN, GROUP BY, HAVING, subquery, view, procedure)

## Login and roles (added)

The application now starts with a **login page**. Choose the role, then enter username and password.

| Role | What the user sees |
|---|---|
| **Admin** | Employee Management, Leave Records, Search Leave, Update Leave Status |
| **Employee** | Only **Apply Leave**, which has three tabs: Apply Leave, View Leave, Cancel Leave (own records only) |

Sample accounts (created by the SQL script; change them before real use):

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Employee (ID 101) | `emp101` | `emp@101` |
| Employee (ID 102 ... 106) | `emp102` ... `emp106` | `emp@102` ... `emp@106` |

When the admin adds a new employee, a login is created automatically in the same transaction: username `emp<ID>`, password `emp@<ID>` (shown in the success message).

**Important:** because the database script changed (new `users` table), re-run `database/employee_leave_management.sql` once before starting. Details, tests and viva questions: `docs/LOGIN_AND_ROLES.md`.

## 1. Setup (about 5 minutes)

**Requirements:** JDK 11+, MySQL 8.0.16+ (the CHECK constraints need it), Maven 3.6+.

1. **Create the database.** From the project folder:
   ```
   mysql -u root -p < database/employee_leave_management.sql
   ```
   (or open the file in MySQL Workbench and run all of it). It drops and rebuilds the tables, so it can be re-run any time to reset the data.
2. **Enter your MySQL username and password** in ONE place: `src/main/java/com/employeeleave/database/DatabaseConnection.java`
   ```java
   private static final String MYSQL_USERNAME = "root";
   private static final String MYSQL_PASSWORD = "your_password_here";
   ```
   The URL is `jdbc:mysql://localhost:3306/employee_leave_management` (plus three harmless connection options).
3. **Compile and run:**
   ```
   mvn clean compile
   mvn exec:java
   ```
   or import the folder as a Maven project in IntelliJ / Eclipse / NetBeans / VS Code and run `com.employeeleave.Main`.
   Maven downloads the JDBC driver (`mysql-connector-j`) automatically.

| Error message | Cause / fix |
|---|---|
| Access denied for user | Wrong username/password in `DatabaseConnection.java` |
| Unknown database | The SQL script has not been run |
| Communications link failure | MySQL not running, or different port |
| No suitable driver | Not run through Maven; add Connector/J to the classpath |
| `GetEmployeeLeaves` does not exist | Script was not run completely (the procedure is at the end) |

## 2. Frozen schema

```
departments (department_id PK AI, department_name UNIQUE NOT NULL)
employees (employee_id PK, name, department_id FK, email UNIQUE, phone)
leave_types (leave_type_id PK AI, leave_type_name UNIQUE, max_days)
leave_requests (leave_id PK AI, employee_id FK, leave_type_id FK, from_date, to_date, reason, status DEFAULT 'Pending')
```

Cardinalities: Department 1:N Employee, Employee 1:N Leave Request, Leave Type 1:N Leave Request (so Employee and Leave Type are in an M:N relationship, resolved by `leave_requests`).
All foreign keys use `ON DELETE RESTRICT`: nothing is silently cascaded.

Two notes about names:
* `employees.employee_id` is an `INT` (typed in by the user, not auto-generated) so that it matches the `GetEmployeeLeaves(IN emp_id INT)` procedure.
* Status values (Pending, Approved, Rejected, Cancelled) are enforced by a `CHECK` constraint, so the Update/Search screens list those four fixed values. Leave types and departments are always loaded from MySQL.

## 3. Class map (schema -> Java)

| SQL | Java model | DAO | GUI |
|---|---|---|---|
| `departments` | `Department` (`departmentId`, `departmentName`) | `DepartmentDAO` | drop-down in `EmployeePanel` |
| `employees` | `Employee` (`employeeId`, `name`, `departmentId`, `email`, `phone`; display-only `departmentName`) | `EmployeeDAO` | `EmployeePanel`, `ApplyLeavePanel` |
| `leave_types` | `LeaveType` (`leaveTypeId`, `leaveTypeName`, `maxDays`) | `LeaveTypeDAO` | drop-down in `ApplyLeavePanel` |
| `leave_requests` + view `employee_leave_details` | `LeaveRequest` (`leaveId`, `employeeId`, `leaveTypeId`, `fromDate`, `toDate`, `reason`, `status`; display-only `employeeName`, `departmentName`, `leaveTypeName`) | `LeaveDAO` | `ApplyLeavePanel`, `LeaveRecordsPanel`, `SearchLeavePanel`, `UpdateLeavePanel` |

`LeaveRequest.leaveTypeId` is filled only when creating a request. Rows read from the view carry the leave type *name* (the view has no id column, exactly as specified).

Flow: `Swing panel -> DAO -> DatabaseConnection -> JDBC -> MySQL -> ResultSet -> model objects -> JTable`.
`MainFrame` creates the five panels and shows one at a time (`CardLayout`); `Main` starts the program.

## 4. Where each DBMS feature appears in the code

| Feature | Location |
|---|---|
| JOIN (employee + department) | `EmployeeDAO.SELECT_WITH_DEPARTMENT` |
| 4-table JOIN | view `employee_leave_details` (used by all `LeaveDAO` reads) |
| WHERE / LIKE / AND | `LeaveDAO.searchLeaveRequests` |
| Stored procedure | `GetEmployeeLeaves` called with `CallableStatement` in `LeaveDAO.getLeavesByEmployee`; button on the Leave Records screen |
| Transaction (commit / rollback) | `LeaveDAO.addLeaveRequest` |
| Referential integrity | foreign keys with `ON DELETE RESTRICT`; `EmployeeDAO.hasLeaveRequests` checked before deleting |
| Constraints | PK, UNIQUE, NOT NULL, DEFAULT, CHECK in the SQL script |
| Indexes | `idx_employees_department`, `idx_leave_employee`, `idx_leave_type`, `idx_leave_status` |

## 5. Consistency checklist (performed on this code)

* All Java source files compile together (19 classes, `javac` via the compiler API, no errors); packages match folders.
* Every table and column in SQL strings exists in the script; the view's columns match `LeaveDAO.mapRow` exactly.
* Every DAO method called from a panel exists with matching parameters (`searchLeaveRequests(Integer, Integer, String, String)` etc.).
* Every `?` placeholder has a matching `setXxx` call, in order.
* No old two-table schema names remain (no `department` text column, no `leave_type` string in `leave_requests`).
* Departments and leave types shown in drop-downs come from MySQL.
* All SQL statements in the script parse; the sample data and every demo query were also executed against the same data in SQLite as a stand-in and give the expected rows.

Honest limitation: I could not run MySQL itself or open the Swing window where this project was generated, so please run the SQL script and the app once before your demo. The script is written for MySQL 8.0.16+.
