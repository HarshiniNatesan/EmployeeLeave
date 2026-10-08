package com.employeeleave.gui;

import com.employeeleave.dao.LeaveDAO;
import com.employeeleave.model.LeaveRequest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/** Search Leave screen. Filled-in criteria are combined with AND in the SQL WHERE clause. */
public class SearchLeavePanel extends JPanel {

    private final LeaveDAO leaveDAO = new LeaveDAO();

    private final JTextField leaveIdField = new JTextField(20);
    private final JTextField employeeIdField = new JTextField(20);
    private final JTextField employeeNameField = new JTextField(20);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
        "All", "Pending", "Approved", "Rejected", "Cancelled"});
    private final JTable table = new JTable();

    public SearchLeavePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        table.setRowHeight(24);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Search Leave Requests (leave a box empty to ignore it)"));
        form.add(new JLabel("Leave ID"));
        form.add(leaveIdField);
        form.add(new JLabel("Employee ID"));
        form.add(employeeIdField);
        form.add(new JLabel("Employee Name"));
        form.add(employeeNameField);
        form.add(new JLabel("Status"));
        form.add(statusCombo);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.add(form, BorderLayout.WEST);

        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear Search");
        JButton viewAllButton = new JButton("View All");
        searchButton.addActionListener(e -> search());
        clearButton.addActionListener(e -> clearSearch());
        viewAllButton.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttons.add(searchButton);
        buttons.add(clearButton);
        buttons.add(viewAllButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formWrapper, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /** View All: shows every leave request. */
    public void refresh() {
        try {
            table.setModel(LeaveRecordsPanel.buildTableModel(leaveDAO.getAllLeaveRequests()));
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void search() {
        Integer leaveId;
        Integer employeeId;
        try {
            leaveId = parseOptionalNumber(leaveIdField.getText());
            employeeId = parseOptionalNumber(employeeIdField.getText());
        } catch (NumberFormatException ex) {
            showError("Leave ID and Employee ID must be numbers.");
            return;
        }
        String name = employeeNameField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        if ("All".equals(status)) {
            status = null;
        }

        if (leaveId == null && employeeId == null && name.isEmpty() && status == null) {
            showError("Please enter at least one search value, or click View All.");
            return;
        }
        try {
            List<LeaveRequest> results = leaveDAO.searchLeaveRequests(leaveId, employeeId, name, status);
            table.setModel(LeaveRecordsPanel.buildTableModel(results));
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No matching leave requests found.");
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void clearSearch() {
        leaveIdField.setText("");
        employeeIdField.setText("");
        employeeNameField.setText("");
        statusCombo.setSelectedIndex(0);
        refresh();
    }

    /** Empty text means "not used" (null). Non-numbers throw NumberFormatException. */
    private Integer parseOptionalNumber(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : Integer.valueOf(trimmed);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
