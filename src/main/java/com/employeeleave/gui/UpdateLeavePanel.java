package com.employeeleave.gui;

import com.employeeleave.dao.LeaveDAO;
import com.employeeleave.model.LeaveRequest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Update Leave Status screen: load a leave request, change its status, cancel it or delete it. */
public class UpdateLeavePanel extends JPanel {

    private final LeaveDAO leaveDAO = new LeaveDAO();

    private final JTextField leaveIdField = new JTextField(10);
    private final JTextArea detailsArea = new JTextArea(7, 35);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
        "Pending", "Approved", "Rejected", "Cancelled"});
    private final JTable table = new JTable();

    public UpdateLeavePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        table.setRowHeight(24);
        detailsArea.setEditable(false);

        add(createTopPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Clicking a row in the table loads that leave request
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                leaveIdField.setText(String.valueOf(table.getValueAt(row, 0)));
                loadLeave();
            }
        });
    }

    private JPanel createTopPanel() {
        JButton loadButton = new JButton("Load Leave");

        JPanel idRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        idRow.add(leaveIdField);
        idRow.add(javax.swing.Box.createHorizontalStrut(10));
        idRow.add(loadButton);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Update Leave Status"));
        form.add(new JLabel("Leave ID"));
        form.add(idRow);
        form.add(new JLabel("Current Details"));
        form.add(new JScrollPane(detailsArea));
        form.add(new JLabel("New Status"));
        form.add(statusCombo);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.add(form, BorderLayout.WEST);

        JButton updateButton = new JButton("Update Status");
        JButton cancelButton = new JButton("Cancel Leave");
        JButton deleteButton = new JButton("Delete Leave");
        JButton refreshButton = new JButton("Refresh");

        loadButton.addActionListener(e -> loadLeave());
        leaveIdField.addActionListener(e -> loadLeave());
        updateButton.addActionListener(e -> updateStatus());
        cancelButton.addActionListener(e -> cancelLeave());
        deleteButton.addActionListener(e -> deleteLeave());
        refreshButton.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttons.add(updateButton);
        buttons.add(cancelButton);
        buttons.add(deleteButton);
        buttons.add(refreshButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formWrapper, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    /** Reloads the table of leave requests. */
    public void refresh() {
        try {
            table.setModel(LeaveRecordsPanel.buildTableModel(leaveDAO.getAllLeaveRequests()));
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    // ---------------- button actions ----------------

    private void loadLeave() {
        Integer leaveId = readLeaveId();
        if (leaveId == null) {
            return;
        }
        try {
            LeaveRequest leave = leaveDAO.getLeaveById(leaveId);
            if (leave == null) {
                detailsArea.setText("");
                showError("Leave request not found for Leave ID " + leaveId + ".");
                return;
            }
            detailsArea.setText("Employee ID: " + leave.getEmployeeId()
                    + "\nEmployee Name: " + leave.getEmployeeName()
                    + "\nDepartment: " + leave.getDepartmentName()
                    + "\nLeave Type: " + leave.getLeaveTypeName()
                    + "\nFrom Date: " + leave.getFromDate() + "    To Date: " + leave.getToDate()
                    + "\nReason: " + leave.getReason()
                    + "\nCurrent Status: " + leave.getStatus());
            statusCombo.setSelectedItem(leave.getStatus());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void updateStatus() {
        Integer leaveId = readLeaveId();
        if (leaveId == null) {
            return;
        }
        String newStatus = (String) statusCombo.getSelectedItem();
        try {
            if (leaveDAO.updateLeaveStatus(leaveId, newStatus)) {
                JOptionPane.showMessageDialog(this, "Leave status updated to " + newStatus + ".");
                loadLeave();
                refresh();
            } else {
                showError("Leave request not found for Leave ID " + leaveId + ".");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void cancelLeave() {
        Integer leaveId = readLeaveId();
        if (leaveId == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel this leave request?\n(The record is kept with status Cancelled.)",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (leaveDAO.cancelLeave(leaveId)) {
                JOptionPane.showMessageDialog(this, "Leave request cancelled.");
                loadLeave();
                refresh();
            } else {
                showError("Leave request not found for Leave ID " + leaveId + ".");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void deleteLeave() {
        Integer leaveId = readLeaveId();
        if (leaveId == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete this leave request?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (leaveDAO.deleteLeaveRequest(leaveId)) {
                JOptionPane.showMessageDialog(this, "Leave request deleted.");
                leaveIdField.setText("");
                detailsArea.setText("");
                refresh();
            } else {
                showError("Leave request not found for Leave ID " + leaveId + ".");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    /** Reads the Leave ID box. Shows an error and returns null if it is empty or not a number. */
    private Integer readLeaveId() {
        String text = leaveIdField.getText().trim();
        if (text.isEmpty()) {
            showError("Please enter or select a Leave ID.");
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            showError("Leave ID must be a number.");
            return null;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
