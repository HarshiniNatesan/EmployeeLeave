package com.employeeleave.model;

/** Maps to table: leave_types(leave_type_id, leave_type_name, max_days). */
public class LeaveType {
    private int leaveTypeId;
    private String leaveTypeName;
    private int maxDays;

    public LeaveType() {
    }

    public LeaveType(int leaveTypeId, String leaveTypeName, int maxDays) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.maxDays = maxDays;
    }

    public int getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(int leaveTypeId) { this.leaveTypeId = leaveTypeId; }

    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

    public int getMaxDays() { return maxDays; }
    public void setMaxDays(int maxDays) { this.maxDays = maxDays; }

    @Override
    public String toString() {
        return leaveTypeName;
    }
}
