package com.employeeleave.gui;

import com.employeeleave.dao.UserDAO;
import com.employeeleave.model.User;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Login page. After a successful login the window for the user's role (MainFrame) opens. */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO = new UserDAO();

    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{User.ROLE_ADMIN, User.ROLE_EMPLOYEE});
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginFrame() {
        setTitle("Login - Employee Leave Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 380);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Employee Leave Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(new Color(33, 64, 110));
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 15));
        form.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 40));
        form.add(new JLabel("Login as"));
        form.add(roleCombo);
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        add(form, BorderLayout.CENTER);

        JButton loginButton = new JButton("Login");
        JButton clearButton = new JButton("Clear");
        JButton exitButton = new JButton("Exit");

        // Event handling
        loginButton.addActionListener(e -> login());
        clearButton.addActionListener(e -> clearFields());
        exitButton.addActionListener(e -> System.exit(0));
        getRootPane().setDefaultButton(loginButton);       // Enter key = Login

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttons.add(loginButton);
        buttons.add(clearButton);
        buttons.add(exitButton);
        add(buttons, BorderLayout.SOUTH);
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, ' ');                   // do not keep the password in memory

        if (username.isEmpty()) {
            showError("Please enter your username.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        try {
            User user = userDAO.authenticate(username, password, (String) roleCombo.getSelectedItem());
            if (user == null) {
                showError("Invalid username, password or role.");
                passwordField.setText("");
                return;
            }
            if (!user.isAdmin() && user.getEmployeeId() == null) {
                showError("This employee account is not linked to an employee record. Contact the administrator.");
                return;
            }
            dispose();                                      // close the login window
            new MainFrame(user).setVisible(true);           // open the window for this role
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage()
                    + "\nCheck that MySQL is running and the username/password in DatabaseConnection.java are correct.");
        }
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocus();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
    }
}
