package com.employeeleave.gui;

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

/** Main window: title at the top, navigation buttons on the left, selected screen in the centre. */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final JPanel navigationPanel = new JPanel(new GridLayout(0, 1, 0, 10));

    public MainFrame() {
        setTitle("Employee Leave Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 680);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createTitleLabel(), BorderLayout.NORTH);

        navigationPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 5));
        JPanel navigationWrapper = new JPanel(new BorderLayout());   // keeps buttons at the top
        navigationWrapper.add(navigationPanel, BorderLayout.NORTH);
        add(navigationWrapper, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        EmployeePanel employeePanel = new EmployeePanel();
        ApplyLeavePanel applyLeavePanel = new ApplyLeavePanel();
        LeaveRecordsPanel leaveRecordsPanel = new LeaveRecordsPanel();
        SearchLeavePanel searchLeavePanel = new SearchLeavePanel();
        UpdateLeavePanel updateLeavePanel = new UpdateLeavePanel();

        // The Runnable (method reference) is the code that reloads that screen's data
        addModule("Employee Management", employeePanel, employeePanel::refresh);
        addModule("Apply Leave", applyLeavePanel, applyLeavePanel::refresh);
        addModule("Leave Records", leaveRecordsPanel, leaveRecordsPanel::refresh);
        addModule("Search Leave", searchLeavePanel, searchLeavePanel::refresh);
        addModule("Update Leave Status", updateLeavePanel, updateLeavePanel::refresh);

        cardLayout.show(contentPanel, "Employee Management");
        employeePanel.refresh();
    }

    private JLabel createTitleLabel() {
        JLabel title = new JLabel("Employee Leave Management System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(new Color(33, 64, 110));
        title.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        return title;
    }

    /** Registers a screen and creates its navigation button. */
    private void addModule(String name, JPanel panel, Runnable refreshAction) {
        contentPanel.add(panel, name);

        JButton button = new JButton(name);
        button.setPreferredSize(new Dimension(190, 40));
        // Event handling: clicking the button shows the screen and reloads its data
        button.addActionListener(e -> {
            cardLayout.show(contentPanel, name);
            refreshAction.run();
        });
        navigationPanel.add(button);
    }
}
