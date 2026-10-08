package com.employeeleave.model;

import java.time.LocalDate;

/** Maps to table: leave_requests(leave_id, employee_id, leave_type_id, from_date, to_date, reason, status). */
public class LeaveRequest {
    private int leaveId;
    private int employeeId;
    private int leaveTypeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String status;

    // DISPLAY-ONLY derived fields (not columns of leave_requests): filled from the
    // employee_leave_details view / JOINs
    private String employeeName;
    private String departmentName;
    private String leaveTypeName;

    public LeaveRequest() {
    }

    /** Used when a NEW request is submitted. Status starts as Pending. */
    public LeaveRequest(int employeeId, int leaveTypeId, LocalDate fromDate, LocalDate toDate, String reason) {
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.reason = reason;
        this.status = "Pending";
    }

    public int getLeaveId() { return leaveId; }
    public void setLeaveId(int leaveId) { this.leaveId = leaveId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

    @Override
    public String toString() {
        return "Leave #" + leaveId + " [employee " + employeeId + ", " + fromDate + " to " + toDate
                + ", " + status + "]";
    }
}
