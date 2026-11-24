package com.pokemon.battle;

import com.pokemon.battle.ui.UiTasks;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiTasksTest {
    @Test void databaseWorkLeavesEdtAndRepeatedActionIsIgnored() throws Exception {
        JPanel panel=new JPanel();JButton button=new JButton("Save");JLabel status=new JLabel();
        CountDownLatch started=new CountDownLatch(1), release=new CountDownLatch(1), finished=new CountDownLatch(1);
        AtomicInteger executions=new AtomicInteger();AtomicBoolean background=new AtomicBoolean(),callbackOnEdt=new AtomicBoolean();
        java.util.function.Supplier<Integer> work=() -> {
            executions.incrementAndGet();background.set(!SwingUtilities.isEventDispatchThread());started.countDown();
            try { if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Timed out"); }
            catch(InterruptedException ex) { Thread.currentThread().interrupt();throw new IllegalStateException(ex); }
            return 1;
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                panel.add(button);panel.addNotify();
                UiTasks.run(panel,status,"Saving",work,result -> { callbackOnEdt.set(SwingUtilities.isEventDispatchThread());finished.countDown(); });
                UiTasks.run(panel,status,"Duplicate",work,result -> finished.countDown());
                assertFalse(button.isEnabled());
            });
            assertTrue(started.await(5,TimeUnit.SECONDS));release.countDown();assertTrue(finished.await(5,TimeUnit.SECONDS));
            assertEquals(1,executions.get());assertTrue(background.get());assertTrue(callbackOnEdt.get());
            SwingUtilities.invokeAndWait(() -> assertTrue(button.isEnabled()));
        } finally { release.countDown();SwingUtilities.invokeAndWait(panel::removeNotify); }
    }
}
