package com.pokemon.battle.ui;

import com.pokemon.battle.exception.AppException;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.logging.*;
import javax.swing.*;

/** Every database action runs away from the event dispatch thread. */
public final class UiTasks {
    private UiTasks() {}
    public static <T> void run(JComponent controls, JLabel status, String progress, Supplier<T> work, Consumer<T> success) {
        if (Boolean.TRUE.equals(controls.getClientProperty("working"))) return;
        controls.putClientProperty("working", true); enable(controls, false); status.setText(progress);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() { return work.get(); }
            @Override protected void done() {
                controls.putClientProperty("working", false); enable(controls, true);
                if (!controls.isDisplayable()) return;
                try { T result = get(); status.setText("Ready."); success.accept(result); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); status.setText("Interrupted. Refresh to check the saved state before retrying."); }
                catch (ExecutionException ex) { showFailure(controls, status, ex.getCause()); }
                catch (RuntimeException ex) { showFailure(controls, status, ex); }
            }
        }.execute();
    }
    public static String message(Throwable ex) {
        if (ex instanceof AppException) return ex.getMessage();
        Logger.getLogger(UiTasks.class.getName()).log(Level.SEVERE, "Unexpected application error", ex);
        return "The operation could not finish. Refresh and try again.";
    }
    public static void showFailure(Component parent, JLabel status, Throwable ex) {
        String message = message(ex);
        status.setText("Could not confirm completion. Current selection kept; refresh before retrying.");
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }
    private static void enable(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) for (Component child : container.getComponents()) enable(child, enabled);
    }
}
