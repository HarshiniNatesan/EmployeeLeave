# Login and Role-Based Access

## 1. What was added

| Item | Detail |
|---|---|
| Table `users` | `user_id` PK, `username` UNIQUE, `password_hash` (SHA-256, 64 hex chars), `role` ('Admin' or 'Employee', CHECK), `employee_id` FK -> employees (UNIQUE, NULL for Admin) |
| `model/User.java` | `userId`, `username`, `role`, `employeeId` (the hash is never kept in memory after login) |
| `dao/UserDAO.java` | `authenticate(username, password, role)`, `hashPassword(password)` |
| `gui/LoginFrame.java` | Login page (role drop-down, username, password, Login / Clear / Exit; Enter key logs in) |
| `gui/MainFrame.java` | Now takes the logged-in `User` and builds the menu for the role; adds a Logout button |
| `gui/EmployeeLeavePanel.java` | The Employee screen with tabs Apply Leave / View Leave / Cancel Leave |
| `gui/ApplyLeavePanel.java` | Employee ID, name and department are now filled from the login (read-only) |
| `EmployeeDAO.addEmployee` | Now a transaction: inserts the employee **and** the login account |
| `LeaveDAO.cancelOwnLeave` | Employee cancel; the SQL itself restricts it to the employee's own Pending/Approved leave |
| `Main.java` | Starts the login page |

Flow: `Main -> LoginFrame -> UserDAO.authenticate -> MainFrame(role) -> panels -> DAOs -> MySQL`.

## 2. Access matrix

| Screen / action | Admin | Employee |
|---|---|---|
| Employee Management (add / update / delete / view employees) | Yes | No |
| Leave Records | Yes | No |
| Search Leave | Yes | No |
| Update Leave Status (approve / reject / cancel / delete) | Yes | No |
| Apply Leave | No | Yes (own) |
| View Leave | No (uses Leave Records) | Yes (own only) |
| Cancel Leave | Yes (any, in Update Leave Status) | Yes (own Pending/Approved only) |

Employees cannot approve or reject leave; only the Admin can.

## 3. Normalization of `users`

Functional dependencies: `user_id -> username, password_hash, role, employee_id`; `username -> user_id, password_hash, role, employee_id` (UNIQUE); `employee_id -> user_id, username, ...` (UNIQUE, so at most one login per employee; admins have NULL).
Candidate keys: `user_id`, `username`, `employee_id`. Every determinant is a candidate key, so `users` is in **3NF and BCNF**. Name, department, email and phone are not copied into `users`; they are reached through `employee_id` and a JOIN.

## 4. Design decisions

* **Password storage:** only the SHA-256 hash is stored. The Java code hashes the typed password and compares hashes in the SQL `WHERE`, so the plain password is never stored or sent to MySQL. This is suitable for a college project; real systems also add a random salt and use a slow algorithm such as bcrypt.
* **Same error for all failures:** "Invalid username, password or role." does not tell an attacker which part was wrong.
* **Role selection plus role check:** the role chosen on the login page must match the role stored for that user.
* **ON DELETE CASCADE only for `users.employee_id`:** deleting an employee removes only the login account. Leave history is still protected: `leave_requests` keeps `ON DELETE RESTRICT`, and Delete Employee is still blocked if leave records exist.
* **CHECK limitation:** MySQL does not allow a CHECK constraint on a column that has a CASCADE foreign key, so the rule "Admin has no employee_id, Employee has one" is enforced by the application and the sample data, not by a CHECK.
* **Transactions (now two):** Submit Leave (lock, overlap check, insert) and Add Employee (employee row + login row, all or nothing).
* **Security note:** security is enforced in the database queries (an employee's own leaves come from `GetEmployeeLeaves(employee_id)`, and cancel uses `WHERE leave_id = ? AND employee_id = ?`), not only by hiding menu buttons.

## 5. Test cases

Re-run `database/employee_leave_management.sql` first.

| # | Test | Steps | Expected result |
|---|---|---|---|
| L1 | Admin login | Role Admin, `admin` / `admin123` | Window with 4 modules + Logout; header shows "Logged in: admin (Admin)" |
| L2 | Employee login | Role Employee, `emp101` / `emp@101` | Window with only **Apply Leave**; tabs Apply / View / Cancel |
| L3 | Wrong password | `admin` / `wrong` | "Invalid username, password or role."; password box cleared |
| L4 | Wrong role | Role Admin with `emp101` / `emp@101` | "Invalid username, password or role." |
| L5 | Empty fields | Click Login with blanks | "Please enter your username." / "Please enter your password." |
| L6 | Enter key | Type credentials, press Enter | Logs in |
| L7 | Logout | Click Logout | Returns to the login page |
| L8 | Employee identity | Log in as `emp101`, open Apply Leave | Employee ID 101, name Arun Kumar, department shown and read-only |
| L9 | Apply leave | Casual Leave, 2026-11-20 to 2026-11-21, reason | Success, status Pending |
| L10 | View own leaves | View Leave tab | Only employee 101's requests (not other employees') |
| L11 | Cancel own leave | Cancel Leave tab, select a Pending/Approved row, confirm | Status becomes Cancelled; the row remains |
| L12 | Cancel not allowed | Select a Cancelled or Rejected row | "Only Pending or Approved leave requests can be cancelled." |
| L13 | Cancel nothing selected | Click Cancel without selecting | "Please select a leave request to cancel." |
| L14 | Admin sees all | Log in as admin, Leave Records | All employees' leaves |
| L15 | Admin approves | Update Leave Status, approve the leave from L9 | Status Approved; employee sees it in View Leave |
| L16 | New employee login | Admin adds employee 201 | Message shows username `emp201`, password `emp@201`; that login works |
| L17 | Add employee rollback | Add employee whose username `emp<ID>` already exists in `users` | Error; **no** employee row is saved (transaction rolled back) |
| L18 | Delete employee | Delete an employee without leaves (e.g. 106) | Deleted; the `emp106` login is removed too |
| L19 | Database down | Stop MySQL, click Login | "Database error..." dialog; the app does not crash |

Database checks:
```sql
SELECT user_id, username, role, employee_id, LEFT(password_hash, 12) AS hash_start FROM users;
-- passwords are not readable: only hashes are stored
```

## 6. Viva questions

1. **How does the login work?** The user enters username, password and role. `UserDAO.authenticate` hashes the password with SHA-256 and runs `SELECT ... FROM users WHERE username = ? AND password_hash = ? AND role = ?`. If a row is found, `MainFrame` opens with the menu for that role.
2. **Why store a hash, not the password?** If the database leaks, the passwords are not directly readable. A hash cannot be reversed easily.
3. **What is SHA-256?** A one-way hashing algorithm that turns any text into a fixed 64-character hexadecimal value.
4. **How are Admin and Employee different?** The `role` column. Admin sees the 4 management screens; Employee sees only Apply Leave (Apply / View / Cancel), limited to their own `employee_id`.
5. **Where is the Employee-to-account link?** `users.employee_id` is a foreign key to `employees.employee_id`, UNIQUE so one employee has one account.
6. **Why is `employee_id` NULL for the admin?** The admin is not an employee record.
7. **How do you stop an employee cancelling someone else's leave?** `cancelOwnLeave` uses `WHERE leave_id = ? AND employee_id = ? AND status IN ('Pending','Approved')`, so MySQL changes nothing otherwise.
8. **Why a transaction in Add Employee?** Two inserts (employee and login) must succeed together; if the second fails, the first is rolled back.
9. **Why CASCADE for users but RESTRICT for leave_requests?** A login account has no history value, so it can go with the employee; leave records are history and must be protected.
10. **Is `users` normalized?** Yes. All determinants (`user_id`, `username`, `employee_id`) are candidate keys, so it is in 3NF and BCNF.
11. **What would you improve?** Add a random salt and bcrypt, a change-password screen, and lock the account after repeated failures.
