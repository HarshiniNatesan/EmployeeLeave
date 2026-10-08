package com.employeeleave;

import com.employeeleave.gui.LoginFrame;

import java.awt.Font;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point of the application. The first screen is the login page. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set the system look and feel: " + e.getMessage());
        }
        setDefaultFonts();

        // Swing screens must be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }

    /** One consistent font for the whole application. */
    private static void setDefaultFonts() {
        Font normal = new Font("SansSerif", Font.PLAIN, 14);
        Font bold = new Font("SansSerif", Font.BOLD, 14);
        String[] keys = {"Label.font", "Button.font", "TextField.font", "PasswordField.font", "TextArea.font",
            "ComboBox.font", "Table.font", "TabbedPane.font", "OptionPane.messageFont", "OptionPane.buttonFont"};
        for (String key : keys) {
            UIManager.put(key, normal);
        }
        UIManager.put("TableHeader.font", bold);
        UIManager.put("TitledBorder.font", bold);
    }
}
