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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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

/** Apply Leave screen: submit a new leave request (status = Pending). */
public class ApplyLeavePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final LeaveDAO leaveDAO = new LeaveDAO();
    private final LeaveTypeDAO leaveTypeDAO = new LeaveTypeDAO();

    private final JTextField employeeIdField = new JTextField(25);
    private final JTextField employeeNameField = new JTextField(25);
    private final JTextField departmentField = new JTextField(25);
    private final JComboBox<LeaveType> leaveTypeCombo = new JComboBox<>();
    private final JLabel maxDaysLabel = new JLabel(" ");
    private final JTextField fromDateField = new JTextField(25);
    private final JTextField toDateField = new JTextField(25);
    private final JTextField reasonField = new JTextField(25);

    public ApplyLeavePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        employeeNameField.setEditable(false);
        departmentField.setEditable(false);

        add(createFormPanel(), BorderLayout.NORTH);

        // Show the employee's name and department when the user leaves the ID field or presses Enter
        employeeIdField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                showEmployeeDetails();
            }
        });
        employeeIdField.addActionListener(e -> showEmployeeDetails());
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

    /** Loads the leave types from MySQL into the drop-down. */
    public void refresh() {
        try {
            leaveTypeCombo.removeAllItems();
            for (LeaveType type : leaveTypeDAO.getAllLeaveTypes()) {
                leaveTypeCombo.addItem(type);
            }
            leaveTypeCombo.setSelectedIndex(-1);   // nothing chosen until the user selects
            showMaxDays();
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showMaxDays() {
        LeaveType type = (LeaveType) leaveTypeCombo.getSelectedItem();
        maxDaysLabel.setText(type == null ? " " : String.valueOf(type.getMaxDays()));
    }

    /** Looks up the employee and shows name and department. Returns the employee, or null. */
    private Employee showEmployeeDetails() {
        String text = employeeIdField.getText().trim();
        employeeNameField.setText("");
        departmentField.setText("");
        if (text.isEmpty()) {
            return null;
        }
        try {
            Employee employee = employeeDAO.getEmployeeById(Integer.parseInt(text));
            if (employee == null) {
                employeeNameField.setText("Employee not found");
                return null;
            }
            employeeNameField.setText(employee.getName());
            departmentField.setText(employee.getDepartmentName());
            return employee;
        } catch (NumberFormatException ex) {
            employeeNameField.setText("Invalid Employee ID");
            return null;
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
            return null;
        }
    }

    private void submitLeave() {
        // ---- validation ----
        String idText = employeeIdField.getText().trim();
        if (idText.isEmpty()) {
            showError("Please enter a valid employee ID.");
            return;
        }
        try {
            Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            showError("Employee ID must be a number.");
            return;
        }
        Employee employee = showEmployeeDetails();
        if (employee == null) {
            showError("Employee not found. Please enter a valid employee ID.");
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

        // ---- save through the DAO (the INSERT runs inside a transaction) ----
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

    private void clearFields() {
        employeeIdField.setText("");
        employeeNameField.setText("");
        departmentField.setText("");
        leaveTypeCombo.setSelectedIndex(-1);
        fromDateField.setText("");
        toDateField.setText("");
        reasonField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
