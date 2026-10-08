package com.employeeleave.gui;

import com.employeeleave.dao.LeaveDAO;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

/**
 * The only screen of the Employee login. Three tabs:
 *   Apply Leave  - submit a new request
 *   View Leave   - the employee's OWN leave requests (stored procedure GetEmployeeLeaves)
 *   Cancel Leave - cancel one of the employee's OWN Pending/Approved requests
 */
public class EmployeeLeavePanel extends JPanel {

    private final LeaveDAO leaveDAO = new LeaveDAO();
    private final int employeeId;                       // the logged-in employee

    private final ApplyLeavePanel applyLeavePanel;
    private final JTable viewTable = new JTable();
    private final JTable cancelTable = new JTable();
    private final JTabbedPane tabs = new JTabbedPane();

    public EmployeeLeavePanel(int employeeId) {
        this.employeeId = employeeId;
        this.applyLeavePanel = new ApplyLeavePanel(employeeId);

        setLayout(new BorderLayout());
        viewTable.setRowHeight(24);
        cancelTable.setRowHeight(24);

        tabs.addTab("Apply Leave", applyLeavePanel);
        tabs.addTab("View Leave", createViewTab());
        tabs.addTab("Cancel Leave", createCancelTab());
        add(tabs, BorderLayout.CENTER);

        // Reload the data of a tab whenever the user switches to it
        tabs.addChangeListener(e -> refresh());
    }

    private JPanel createViewTab() {
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadMyLeaves());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(refreshButton);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(viewTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCancelTab() {
        JButton cancelButton = new JButton("Cancel Selected Leave");
        JButton refreshButton = new JButton("Refresh");
        cancelButton.addActionListener(e -> cancelSelectedLeave());
        refreshButton.addActionListener(e -> loadMyLeaves());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.add(new JLabel("Select one of your Pending or Approved requests, then click Cancel."));
        top.add(cancelButton);
        top.add(refreshButton);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(cancelTable), BorderLayout.CENTER);
        return panel;
    }

    /** Reloads the tab that is currently shown. */
    public void refresh() {
        if (tabs.getSelectedComponent() == applyLeavePanel) {
            applyLeavePanel.refresh();
        } else {
            loadMyLeaves();
        }
    }

    /** Loads only this employee's leaves (CallableStatement -> stored procedure). */
    private void loadMyLeaves() {
        try {
            viewTable.setModel(LeaveRecordsPanel.buildTableModel(leaveDAO.getLeavesByEmployee(employeeId)));
            cancelTable.setModel(LeaveRecordsPanel.buildTableModel(leaveDAO.getLeavesByEmployee(employeeId)));
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void cancelSelectedLeave() {
        int row = cancelTable.getSelectedRow();
        if (row < 0) {
            showError("Please select a leave request to cancel.");
            return;
        }
        int leaveId = Integer.parseInt(String.valueOf(cancelTable.getValueAt(row, 0)));
        String status = String.valueOf(cancelTable.getValueAt(row, 8));
        if (!status.equals("Pending") && !status.equals("Approved")) {
            showError("Only Pending or Approved leave requests can be cancelled. This request is " + status + ".");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel leave request " + leaveId + "?\n(The record is kept with status Cancelled.)",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            // The SQL checks that the leave belongs to this employee
            if (leaveDAO.cancelOwnLeave(leaveId, employeeId)) {
                JOptionPane.showMessageDialog(this, "Leave request cancelled.");
            } else {
                showError("This leave request could not be cancelled.");
            }
            loadMyLeaves();
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
