package com.employeeleave.gui;

import com.employeeleave.dao.LeaveDAO;
import com.employeeleave.model.LeaveRequest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Leave Records screen: all leave requests (from the employee_leave_details view). */
public class LeaveRecordsPanel extends JPanel {

    private static final String[] COLUMNS = {"Leave ID", "Employee ID", "Employee Name", "Department",
        "Leave Type", "From Date", "To Date", "Reason", "Status"};

    private final LeaveDAO leaveDAO = new LeaveDAO();
    private final JTextField employeeIdField = new JTextField(8);
    private final JTable table = new JTable();

    public LeaveRecordsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        table.setRowHeight(24);

        JButton refreshButton = new JButton("Refresh");
        JButton employeeLeavesButton = new JButton("Show Employee Leaves (Stored Procedure)");
        refreshButton.addActionListener(e -> refresh());
        employeeLeavesButton.addActionListener(e -> showEmployeeLeaves());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(refreshButton);
        top.add(new JLabel("   Employee ID:"));
        top.add(employeeIdField);
        top.add(employeeLeavesButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** Reloads ALL leave requests. */
    public void refresh() {
        try {
            table.setModel(buildTableModel(leaveDAO.getAllLeaveRequests()));
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    /** Calls the stored procedure GetEmployeeLeaves through LeaveDAO (CallableStatement). */
    private void showEmployeeLeaves() {
        int employeeId;
        try {
            employeeId = Integer.parseInt(employeeIdField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Please enter a valid numeric Employee ID.");
            return;
        }
        try {
            List<LeaveRequest> leaves = leaveDAO.getLeavesByEmployee(employeeId);
            table.setModel(buildTableModel(leaves));
            if (leaves.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No leave requests found for employee " + employeeId + ".");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    /** Shared by Leave Records, Search Leave and Update Leave Status screens. */
    public static DefaultTableModel buildTableModel(List<LeaveRequest> leaves) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;   // read-only table
            }
        };
        for (LeaveRequest l : leaves) {
            model.addRow(new Object[]{l.getLeaveId(), l.getEmployeeId(), l.getEmployeeName(),
                l.getDepartmentName(), l.getLeaveTypeName(), l.getFromDate(), l.getToDate(),
                l.getReason(), l.getStatus()});
        }
        return model;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
