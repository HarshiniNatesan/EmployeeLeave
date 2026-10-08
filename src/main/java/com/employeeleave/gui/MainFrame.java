package com.employeeleave.gui;

import com.employeeleave.model.User;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Main window shown after login.
 *   Admin    -> Employee Management, Leave Records, Search Leave, Update Leave Status
 *   Employee -> Apply Leave (with Apply / View / Cancel tabs)
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel navigationPanel = new JPanel(new GridLayout(0, 1, 0, 10));

    public MainFrame(User user) {
        setTitle("Employee Leave Management System - " + user.getRole());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 680);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createHeader(user), BorderLayout.NORTH);

        navigationPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 5));
        JPanel navigationWrapper = new JPanel(new BorderLayout());
        navigationWrapper.add(navigationPanel, BorderLayout.NORTH);
        add(navigationWrapper, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // The menu depends on the role of the logged-in user
        if (user.isAdmin()) {
            buildAdminMenu();
        } else {
            buildEmployeeMenu(user);
        }
        addLogoutButton();
    }

    private void buildAdminMenu() {
        EmployeePanel employeePanel = new EmployeePanel();
        LeaveRecordsPanel leaveRecordsPanel = new LeaveRecordsPanel();
        SearchLeavePanel searchLeavePanel = new SearchLeavePanel();
        UpdateLeavePanel updateLeavePanel = new UpdateLeavePanel();

        addModule("Employee Management", employeePanel, employeePanel::refresh);
        addModule("Leave Records", leaveRecordsPanel, leaveRecordsPanel::refresh);
        addModule("Search Leave", searchLeavePanel, searchLeavePanel::refresh);
        addModule("Update Leave Status", updateLeavePanel, updateLeavePanel::refresh);

        cardLayout.show(contentPanel, "Employee Management");
        employeePanel.refresh();
    }

    private void buildEmployeeMenu(User user) {
        EmployeeLeavePanel leavePanel = new EmployeeLeavePanel(user.getEmployeeId());

        addModule("Apply Leave", leavePanel, leavePanel::refresh);

        cardLayout.show(contentPanel, "Apply Leave");
        leavePanel.refresh();
    }

    private JPanel createHeader(User user) {
        JLabel title = new JLabel("Employee Leave Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel loggedIn = new JLabel("Logged in: " + user.getUsername() + " (" + user.getRole() + ")");
        loggedIn.setForeground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(33, 64, 110));
        header.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        header.add(title, BorderLayout.CENTER);
        header.add(loggedIn, BorderLayout.EAST);
        return header;
    }

    /** Registers a screen and creates its navigation button. */
    private void addModule(String name, JPanel panel, Runnable refreshAction) {
        contentPanel.add(panel, name);

        JButton button = new JButton(name);
        button.setPreferredSize(new Dimension(190, 40));
        button.addActionListener(e -> {
            cardLayout.show(contentPanel, name);
            refreshAction.run();
        });
        navigationPanel.add(button);
    }

    private void addLogoutButton() {
        JButton logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(190, 40));
        logoutButton.addActionListener(e -> {
            dispose();                              // close this window
            new LoginFrame().setVisible(true);      // back to the login page
        });
        navigationPanel.add(logoutButton);
    }
}
