package com.pokemon.battle.ui;

import com.pokemon.battle.database.*;
import com.pokemon.battle.exception.DataAccessException;
import com.pokemon.battle.service.AppServices;
import java.awt.*;
import java.util.Properties;
import javax.swing.*;

public final class DatabaseSettingsDialog extends JDialog {
    public DatabaseSettingsDialog(Window owner) {
        super(owner, "Oracle database settings", ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(10, 10)); panel.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JPanel form = new JPanel(new GridLayout(0,1,4,4));
        Properties p;
        String initialStatus="Use the connection details from SQL Developer.";
        try { p = DatabaseConfig.readSettings(); }
        catch (DataAccessException ex) { p = new Properties(); initialStatus=ex.getMessage(); }
        JTextField url = new JTextField(p.getProperty("db.url", "jdbc:oracle:thin:@localhost:1521:xe"), 48);
        JTextField user = new JTextField(p.getProperty("db.user", ""));
        JPasswordField password = new JPasswordField(p.getProperty("db.password", ""));
        form.add(new JLabel("Oracle JDBC URL (SID or service name)")); form.add(url);
        form.add(new JLabel("Database username")); form.add(user);
        form.add(new JLabel("Database password")); form.add(password);
        form.add(new JLabel("Use a dedicated game user. SYSTEM is an Oracle administrator."));
        form.add(new JLabel("Settings are saved locally in " + DatabaseConfig.path() + "; do not share this file."));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton test = new JButton("Test connection"), save = new JButton("Save settings"), setup = new JButton("Create game tables"), close = new JButton("Close");
        buttons.add(test); buttons.add(save); buttons.add(setup); buttons.add(close);
        JLabel status = new JLabel(initialStatus);
        panel.add(form, BorderLayout.CENTER); panel.add(buttons, BorderLayout.NORTH); panel.add(status, BorderLayout.SOUTH);
        test.addActionListener(e -> {
            try {
                DatabaseConfig config = capture(url, user, password);
                UiTasks.run(panel, status, "Connecting to Oracle...", () -> {
                    AppServices.DATABASE.testConnection(config);
                    return "Connected to Oracle. Use Create game tables once, then Save settings.";
                }, status::setText);
            } catch (RuntimeException ex) { UiTasks.showFailure(panel, status, ex); }
        });
        save.addActionListener(e -> {
            try {
                DatabaseConfig config = capture(url, user, password);
                UiTasks.run(panel, status, "Saving local settings...", () -> { config.save(); return true; }, unused -> status.setText("Settings saved. You can now test the connection or close this window."));
            } catch (RuntimeException ex) { UiTasks.showFailure(panel, status, ex); }
        });
        setup.addActionListener(e -> {
            try {
                DatabaseConfig config=capture(url,user,password);
                if(JOptionPane.showConfirmDialog(this,"Create missing PS_ game tables for user "+config.user()+"? Existing game records will be kept.","Set up Oracle game tables",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                UiTasks.run(panel,status,"Creating and checking game tables...",() -> { AppServices.DATABASE.initialize(config);return true; },unused -> status.setText("Game tables are ready. Save settings before opening Saved Teams."));
            } catch(RuntimeException ex) { UiTasks.showFailure(panel,status,ex); }
        });
        close.addActionListener(e -> dispose());
        setContentPane(panel); pack(); setMinimumSize(getSize()); setLocationRelativeTo(owner);
    }
    private static DatabaseConfig capture(JTextField url, JTextField user, JPasswordField password) {
        char[] chars = password.getPassword();
        try { return new DatabaseConfig(url.getText(), user.getText(), new String(chars)); }
        finally { java.util.Arrays.fill(chars, '\0'); }
    }
}
