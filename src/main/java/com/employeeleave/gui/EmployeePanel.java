package com.employeeleave.gui;

import com.employeeleave.dao.DepartmentDAO;
import com.employeeleave.dao.EmployeeDAO;
import com.employeeleave.model.Department;
import com.employeeleave.model.Employee;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
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
import javax.swing.table.DefaultTableModel;

/** Employee Management screen: add, update, delete, clear and view employees. */
public class EmployeePanel extends JPanel {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^[0-9]{10}$";

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    private final JTextField idField = new JTextField(25);
    private final JTextField nameField = new JTextField(25);
    private final JComboBox<Department> departmentCombo = new JComboBox<>();
    private final JTextField emailField = new JTextField(25);
    private final JTextField phoneField = new JTextField(25);
    private final JTable table = new JTable();

    // The employees currently shown in the table (row i of the table = employeeList.get(i))
    private List<Employee> employeeList = new ArrayList<>();

    public EmployeePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createTopPanel(), BorderLayout.NORTH);
        table.setRowHeight(24);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Clicking a row copies that employee into the input fields
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFieldsFromSelectedRow();
            }
        });
    }

    private JPanel createTopPanel() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Employee Details"));
        form.add(new JLabel("Employee ID"));
        form.add(idField);
        form.add(new JLabel("Employee Name"));
        form.add(nameField);
        form.add(new JLabel("Department"));
        form.add(departmentCombo);
        form.add(new JLabel("Email"));
        form.add(emailField);
        form.add(new JLabel("Phone"));
        form.add(phoneField);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.add(form, BorderLayout.WEST);

        JButton addButton = new JButton("Add Employee");
        JButton updateButton = new JButton("Update Employee");
        JButton deleteButton = new JButton("Delete Employee");
        JButton clearButton = new JButton("Clear");
        JButton viewButton = new JButton("View Employees");

        // Event handling: one ActionListener per button
        addButton.addActionListener(e -> addEmployee());
        updateButton.addActionListener(e -> updateEmployee());
        deleteButton.addActionListener(e -> deleteEmployee());
        clearButton.addActionListener(e -> clearFields());
        viewButton.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(clearButton);
        buttons.add(viewButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(formWrapper, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        return top;
    }

    /** Reloads the department drop-down (from MySQL) and the employee table. */
    public void refresh() {
        try {
            Department selected = (Department) departmentCombo.getSelectedItem();
            departmentCombo.removeAllItems();
            for (Department d : departmentDAO.getAllDepartments()) {
                departmentCombo.addItem(d);
            }
            selectDepartment(selected == null ? -1 : selected.getDepartmentId());

            employeeList = employeeDAO.getEmployees();
            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Employee ID", "Name", "Department", "Email", "Phone"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;   // read-only table
                }
            };
            for (Employee e : employeeList) {
                model.addRow(new Object[]{e.getEmployeeId(), e.getName(), e.getDepartmentName(),
                    e.getEmail(), e.getPhone()});
            }
            table.setModel(model);
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- button actions ----------------

    private void addEmployee() {
        Employee employee = readAndValidate();
        if (employee == null) {
            return;
        }
        try {
            if (employeeDAO.getEmployeeById(employee.getEmployeeId()) != null) {
                showError("Employee ID " + employee.getEmployeeId() + " already exists.");
                return;
            }
            employeeDAO.addEmployee(employee);
            JOptionPane.showMessageDialog(this, "Employee added successfully.");
            clearFields();
            refresh();
        } catch (SQLIntegrityConstraintViolationException ex) {
            showError("This email address is already used by another employee.");
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void updateEmployee() {
        Employee employee = readAndValidate();
        if (employee == null) {
            return;
        }
        try {
            if (employeeDAO.updateEmployee(employee)) {
                JOptionPane.showMessageDialog(this, "Employee updated successfully.");
                refresh();
            } else {
                showError("Employee not found. Select an existing employee to update.");
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            showError("This email address is already used by another employee.");
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void deleteEmployee() {
        int employeeId;
        try {
            employeeId = Integer.parseInt(idField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Please select an employee or enter a valid numeric Employee ID to delete.");
            return;
        }
        try {
            // Rule: an employee with leave history must not be deleted
            if (employeeDAO.hasLeaveRequests(employeeId)) {
                showError("Cannot delete employee because leave records exist.");
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete employee " + employeeId + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            if (employeeDAO.deleteEmployee(employeeId)) {
                JOptionPane.showMessageDialog(this, "Employee deleted successfully.");
                clearFields();
                refresh();
            } else {
                showError("Employee not found.");
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        if (departmentCombo.getItemCount() > 0) {
            departmentCombo.setSelectedIndex(0);
        }
        table.clearSelection();
    }

    // ---------------- helpers ----------------

    /** Validates the form. Returns the Employee, or null after showing an error message. */
    private Employee readAndValidate() {
        String idText = idField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        Department department = (Department) departmentCombo.getSelectedItem();

        if (idText.isEmpty()) {
            showError("Employee ID cannot be empty.");
            return null;
        }
        int employeeId;
        try {
            employeeId = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            showError("Please enter a valid numeric Employee ID.");
            return null;
        }
        if (employeeId <= 0) {
            showError("Employee ID must be a positive number.");
            return null;
        }
        if (name.isEmpty()) {
            showError("Employee name cannot be empty.");
            return null;
        }
        if (department == null) {
            showError("Please select a department.");
            return null;
        }
        if (email.isEmpty()) {
            showError("Email cannot be empty.");
            return null;
        }
        if (!email.matches(EMAIL_REGEX)) {
            showError("Please enter a valid email address (example: name@example.com).");
            return null;
        }
        if (phone.isEmpty()) {
            showError("Phone number cannot be empty.");
            return null;
        }
        if (!phone.matches(PHONE_REGEX)) {
            showError("Phone number must contain exactly 10 digits.");
            return null;
        }
        return new Employee(employeeId, name, department.getDepartmentId(), email, phone);
    }

    private void fillFieldsFromSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= employeeList.size()) {
            return;
        }
        Employee e = employeeList.get(row);
        idField.setText(String.valueOf(e.getEmployeeId()));
        nameField.setText(e.getName());
        emailField.setText(e.getEmail());
        phoneField.setText(e.getPhone());
        selectDepartment(e.getDepartmentId());
    }

    private void selectDepartment(int departmentId) {
        for (int i = 0; i < departmentCombo.getItemCount(); i++) {
            if (departmentCombo.getItemAt(i).getDepartmentId() == departmentId) {
                departmentCombo.setSelectedIndex(i);
                return;
            }
        }
        if (departmentCombo.getItemCount() > 0) {
            departmentCombo.setSelectedIndex(0);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showDbError(SQLException ex) {
        showError("Database error: " + ex.getMessage()
                + "\nCheck that MySQL is running and the username/password in DatabaseConnection.java are correct.");
    }
}
