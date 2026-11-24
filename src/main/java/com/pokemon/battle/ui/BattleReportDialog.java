package com.pokemon.battle.ui;

import com.pokemon.battle.KantoDex;
import com.pokemon.battle.exception.ValidationException;
import com.pokemon.battle.model.*;
import com.pokemon.battle.service.BattleHistoryService;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class BattleReportDialog extends JDialog {
    public BattleReportDialog(Window owner, BattleHistoryService service) {
        super(owner, "Battle history and win loss report", ModalityType.APPLICATION_MODAL);
        JPanel panel=new JPanel(new BorderLayout(8,8)); panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        LocalDate today=LocalDate.now(ZoneOffset.UTC);
        JTextField from=new JTextField(today.minusMonths(1).toString(),10), to=new JTextField(today.toString(),10);
        JComboBox<String> outcome=new JComboBox<>(new String[]{"ALL","WIN","LOSS","FLED"});
        JButton refresh=new JButton("Refresh report");
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("UTC dates From")); filters.add(from); filters.add(new JLabel("To")); filters.add(to); filters.add(outcome); filters.add(refresh);
        DefaultTableModel model=new DefaultTableModel(new String[]{"Finished UTC","Team","Outcome","Player turns","Player Pokemon","Enemy Pokemon"},0) {
            @Override public boolean isCellEditable(int r,int c) { return false; }
        };
        JTable table=new JTable(model); table.setAutoCreateRowSorter(true); table.setRowHeight(26);
        JLabel summary=new JLabel("No report loaded."), status=new JLabel("Ready.");
        JPanel bottom=new JPanel(new GridLayout(0,1,4,4)); bottom.add(summary); bottom.add(status);
        panel.add(filters,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER); panel.add(bottom,BorderLayout.SOUTH);
        Runnable reload=() -> {
            try {
                LocalDate start=LocalDate.parse(from.getText().strip()), end=LocalDate.parse(to.getText().strip());
                BattleOutcome selected="ALL".equals(outcome.getSelectedItem())?null:BattleOutcome.valueOf((String)outcome.getSelectedItem());
                UiTasks.run(panel,status,"Loading report...",() -> service.report(start,end,selected), report -> {
                    model.setRowCount(0);
                    for(BattleRecord b:report.battles()) model.addRow(new Object[]{b.endedAt().toString().replace('T',' '),b.teamLabel(),b.outcome(),b.playerTurns(),names(b.playerIds()),names(b.enemyIds())});
                    summary.setText(String.format("Battles: %d   Wins: %d   Losses: %d   Fled: %d   Win rate: %.1f%% (fled excluded)",report.battles().size(),report.wins(),report.losses(),report.fled(),report.winPercent()));
                    status.setText("Showing committed battle results for the selected UTC dates and outcome.");
                });
            } catch(DateTimeParseException ex) { UiTasks.showFailure(panel,status,new ValidationException("Use dates in YYYY-MM-DD format.")); }
        };
        refresh.addActionListener(e -> reload.run());
        setContentPane(panel); setSize(1100,500); setMinimumSize(new Dimension(850,350)); setLocationRelativeTo(owner);
        reload.run();
    }
    private static String names(java.util.List<Integer> ids) { return ids.stream().map(KantoDex::name).collect(Collectors.joining(", ")); }
}
