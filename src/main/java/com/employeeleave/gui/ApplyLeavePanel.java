package com.employeeleave.gui;

import com.employeeleave.dao.EmployeeDAO;
import com.employeeleave.dao.LeaveDAO;
import com.employeeleave.dao.LeaveTypeDAO;
import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.LeaveType;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Apply Leave tab (Employee login). The employee is the logged-in user, so the
 * Employee ID, name and department are filled in automatically and cannot be changed.
 */
public class ApplyLeavePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final LeaveDAO leaveDAO = new LeaveDAO();
    private final LeaveTypeDAO leaveTypeDAO = new LeaveTypeDAO();

    private final int employeeId;                       // the logged-in employee
    private Employee employee;                          // loaded from MySQL in refresh()

    private final JTextField employeeIdField = new JTextField(25);
    private final JTextField employeeNameField = new JTextField(25);
    private final JTextField departmentField = new JTextField(25);
    private final JComboBox<LeaveType> leaveTypeCombo = new JComboBox<>();
    private final JLabel maxDaysLabel = new JLabel(" ");
    private final JTextField fromDateField = new JTextField(25);
    private final JTextField toDateField = new JTextField(25);
    private final JTextField reasonField = new JTextField(25);

    public ApplyLeavePanel(int employeeId) {
        this.employeeId = employeeId;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        employeeIdField.setText(String.valueOf(employeeId));
        employeeIdField.setEditable(false);
        employeeNameField.setEditable(false);
        departmentField.setEditable(false);

        add(createFormPanel(), BorderLayout.NORTH);
        leaveTypeCombo.addActionListener(e -> showMaxDays());
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Leave Application"));
        form.add(new JLabel("Employee ID"));
        form.add(employeeIdField);
        form.add(new JLabel("Employee Name"));
        form.add(employeeNameField);
        form.add(new JLabel("Department"));
        form.add(departmentField);
        form.add(new JLabel("Leave Type"));
        form.add(leaveTypeCombo);
        form.add(new JLabel("Maximum Days Allowed"));
        form.add(maxDaysLabel);
        form.add(new JLabel("From Date (yyyy-MM-dd)"));
        form.add(fromDateField);
        form.add(new JLabel("To Date (yyyy-MM-dd)"));
        form.add(toDateField);
        form.add(new JLabel("Reason"));
        form.add(reasonField);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.add(form, BorderLayout.WEST);

        JButton submitButton = new JButton("Submit Leave");
        JButton clearButton = new JButton("Clear");
        submitButton.addActionListener(e -> submitLeave());
        clearButton.addActionListener(e -> clearFields());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttons.add(submitButton);
        buttons.add(clearButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formWrapper, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    /** Loads the leave types and the employee's name/department from MySQL. */
    public void refresh() {
        try {
            leaveTypeCombo.removeAllItems();
            for (LeaveType type : leaveTypeDAO.getAllLeaveTypes()) {
                leaveTypeCombo.addItem(type);
            }
            leaveTypeCombo.setSelectedIndex(-1);
            showMaxDays();
            loadEmployeeDetails();
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void loadEmployeeDetails() throws SQLException {
        employee = employeeDAO.getEmployeeById(employeeId);
        if (employee == null) {
            employeeNameField.setText("Employee not found");
            departmentField.setText("");
        } else {
            employeeNameField.setText(employee.getName());
            departmentField.setText(employee.getDepartmentName());
        }
    }

    private void showMaxDays() {
        LeaveType type = (LeaveType) leaveTypeCombo.getSelectedItem();
        maxDaysLabel.setText(type == null ? " " : String.valueOf(type.getMaxDays()));
    }

    private void submitLeave() {
        try {
            loadEmployeeDetails();
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
            return;
        }
        if (employee == null) {
            showError("Your employee record was not found. Please contact the administrator.");
            return;
        }
        LeaveType leaveType = (LeaveType) leaveTypeCombo.getSelectedItem();
        if (leaveType == null) {
            showError("Please select a leave type.");
            return;
        }

        LocalDate from;
        LocalDate to;
        try {
            if (fromDateField.getText().trim().isEmpty()) {
                showError("From Date cannot be empty (format: yyyy-MM-dd).");
                return;
            }
            from = LocalDate.parse(fromDateField.getText().trim());
        } catch (DateTimeParseException ex) {
            showError("From Date is invalid. Use the format yyyy-MM-dd (example: 2026-10-25).");
            return;
        }
        try {
            if (toDateField.getText().trim().isEmpty()) {
                showError("To Date cannot be empty (format: yyyy-MM-dd).");
                return;
            }
            to = LocalDate.parse(toDateField.getText().trim());
        } catch (DateTimeParseException ex) {
            showError("To Date is invalid. Use the format yyyy-MM-dd (example: 2026-10-27).");
            return;
        }
        if (from.isAfter(to)) {
            showError("From Date cannot be after To Date.");
            return;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > leaveType.getMaxDays()) {
            showError(leaveType.getLeaveTypeName() + " allows a maximum of " + leaveType.getMaxDays()
                    + " days. You requested " + days + " days.");
            return;
        }
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            showError("Reason cannot be empty.");
            return;
        }

        // Save through the DAO (the INSERT runs inside a transaction)
        LeaveRequest leave = new LeaveRequest(employee.getEmployeeId(), leaveType.getLeaveTypeId(),
                from, to, reason);
        try {
            int leaveId = leaveDAO.addLeaveRequest(leave);
            JOptionPane.showMessageDialog(this,
                    "Leave request submitted successfully.\nLeave ID: " + leaveId + "\nStatus: Pending");
            clearFields();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());           // rule broken (e.g. overlapping dates); transaction rolled back
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    /** Clears only the leave details; the logged-in employee's own details stay. */
    private void clearFields() {
        leaveTypeCombo.setSelectedIndex(-1);
        fromDateField.setText("");
        toDateField.setText("");
        reasonField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
