package com.pokemon.battle.ui;

import com.pokemon.battle.KantoDex;
import com.pokemon.battle.exception.ValidationException;
import com.pokemon.battle.model.SavedTeam;
import com.pokemon.battle.service.TeamService;
import java.awt.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class TeamLibraryDialog extends JDialog {
    private final TeamService service;
    private final Supplier<List<Integer>> draft;
    private final Consumer<SavedTeam> load;
    private final JPanel panel = new JPanel(new BorderLayout(8,8));
    private final JLabel status = new JLabel("Ready.");
    private final JTextField search = new JTextField(16), name = new JTextField(20);
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Team name", "Size", "Pokemon in order"},0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<SavedTeam> rows = List.of();

    public TeamLibraryDialog(Window owner, TeamService service, Supplier<List<Integer>> draft, Consumer<SavedTeam> load) {
        super(owner, "Saved teams", ModalityType.APPLICATION_MODAL);
        this.service=service; this.draft=draft; this.load=load;
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel top = new JPanel(new GridLayout(0,1,4,4));
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton find = new JButton("Search"), refresh = new JButton("Refresh");
        filter.add(new JLabel("Name contains")); filter.add(search); filter.add(find); filter.add(refresh);
        JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton create = new JButton("Save current party as new"), update = new JButton("Replace selected with current party");
        edit.add(new JLabel("Team name")); edit.add(name);
        JPanel editButtons=new JPanel(new FlowLayout(FlowLayout.LEFT));editButtons.add(create);editButtons.add(update);
        top.add(filter); top.add(edit); top.add(editButtons); top.add(new JLabel("Build your party in the team builder first. Save and Replace use that current party."));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoCreateRowSorter(true); table.setRowHeight(26);
        table.getColumnModel().getColumn(2).setPreferredWidth(500);
        table.getSelectionModel().addListSelectionListener(e -> { SavedTeam t=selected(); if(t!=null) name.setText(t.name()); });
        JPanel bottom = new JPanel(new BorderLayout()); JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton use = new JButton("Load into party"), delete = new JButton("Delete selected"), close = new JButton("Close");
        buttons.add(use); buttons.add(delete); buttons.add(close); bottom.add(buttons,BorderLayout.NORTH); bottom.add(status,BorderLayout.SOUTH);
        panel.add(top,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER); panel.add(bottom,BorderLayout.SOUTH);
        find.addActionListener(e -> refresh()); refresh.addActionListener(e -> refresh()); search.addActionListener(e -> refresh());
        create.addActionListener(e -> {
            String label=name.getText(); List<Integer> ids=List.copyOf(draft.get());
            UiTasks.run(panel,status,"Saving team...",() -> service.create(label,ids), t -> { search.setText(""); refresh(); });
        });
        update.addActionListener(e -> {
            SavedTeam old=selected(); if(old==null) { needSelection(); return; }
            String label=name.getText(); List<Integer> ids=List.copyOf(draft.get());
            if(JOptionPane.showConfirmDialog(this,"Replace " + old.name() + " with your current party?","Replace saved team",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            UiTasks.run(panel,status,"Updating team...",() -> service.update(old,label,ids), t -> { search.setText(""); refresh(); });
        });
        use.addActionListener(e -> {
            SavedTeam t=selected(); if(t==null) { needSelection(); return; }
            UiTasks.run(panel,status,"Loading selected team...",() -> service.load(t.id()),fresh -> { load.accept(fresh);dispose(); });
        });
        delete.addActionListener(e -> {
            SavedTeam t=selected(); if(t==null) { needSelection(); return; }
            if(JOptionPane.showConfirmDialog(this,"Delete saved team " + t.name() + "? Battle history will be kept.","Delete team",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
            UiTasks.run(panel,status,"Deleting team...",() -> { service.delete(t); return true; }, unused -> refresh());
        });
        close.addActionListener(e -> dispose());
        setContentPane(panel); setSize(1000,540); setMinimumSize(new Dimension(850,420)); setLocationRelativeTo(owner);
        refresh();
    }
    private SavedTeam selected() { int row=table.getSelectedRow(); return row<0 ? null : rows.get(table.convertRowIndexToModel(row)); }
    private void needSelection() { UiTasks.showFailure(panel,status,new ValidationException("Select a saved team first.")); }
    private void refresh() {
        String term=search.getText();
        UiTasks.run(panel,status,"Loading saved teams...",() -> service.search(term), result -> {
            rows=result; model.setRowCount(0);
            for(SavedTeam t:rows) model.addRow(new Object[]{t.name(), t.pokemonIds().size(), t.pokemonIds().stream().map(KantoDex::name).collect(Collectors.joining(", "))});
            status.setText(rows.size()+" saved team(s). All displayed teams were read from the database.");
        });
    }
}
