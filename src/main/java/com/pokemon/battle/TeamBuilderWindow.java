package com.pokemon.battle;

import com.pokemon.battle.team.PokemonTeam;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import com.pokemon.battle.service.*;
import com.pokemon.battle.ui.*;

/** Retro Kanto team selection screen, before entering a battle. */
public class TeamBuilderWindow extends JFrame {
    private final DefaultListModel<String> selected = new DefaultListModel<>();
    private final JList<String> party = new JList<>(selected);
    private final JPanel grid = new JPanel(new GridLayout(0, 6, 6, 6));
    private final JTextField search = new JTextField();
    private final JLabel info = new JLabel("Choose up to six Pokémon to battle!");
    private final java.util.List<Integer> ids = new ArrayList<>();
    private final Color ink = new Color(35, 46, 62), paper = new Color(242, 239, 211);
    private final Font pixel = new Font(Font.MONOSPACED, Font.BOLD, 15);
    private final Color selectedColor = new Color(100, 190, 110);
    private final Color normalColor = new Color(219, 223, 185);
    private boolean preparing;
    private JButton startButton;
    private String partyName = "Current party";
    public TeamBuilderWindow() {
        super("KANTO TEAM BUILDER - 001 to 151");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1090, 750);setMinimumSize(new Dimension(780, 560));setLocationRelativeTo(null);
        JPanel main=new JPanel(new BorderLayout(10,10));main.setBackground(new Color(85, 129, 116));
        main.setBorder(new EmptyBorder(12,12,12,12));setContentPane(main);
        JLabel heading=new JLabel("  KANTO POKéDEX  |  TEAM BUILDER  |  GEN I  ");
        heading.setFont(new Font(Font.MONOSPACED,Font.BOLD,25));heading.setForeground(paper);
        heading.setBorder(new EmptyBorder(8,6,10,0));main.add(heading,BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(8,8));center.setOpaque(false);main.add(center,BorderLayout.CENTER);
        JPanel top=new JPanel(new BorderLayout(8,0));top.setOpaque(false);
        JLabel prompt=new JLabel(" SEARCH  ");prompt.setFont(pixel);prompt.setForeground(paper);
        top.add(prompt,BorderLayout.WEST);search.setFont(pixel);top.add(search,BorderLayout.CENTER);
        center.add(top,BorderLayout.NORTH);
        grid.setBackground(paper);grid.setBorder(new EmptyBorder(9,9,9,9));
        JScrollPane scroll=new JScrollPane(grid);scroll.getVerticalScrollBar().setUnitIncrement(25);
        scroll.setBorder(BorderFactory.createLineBorder(ink,5));center.add(scroll,BorderLayout.CENTER);
        JPanel right=new JPanel(new BorderLayout(7,7));right.setBackground(paper);
        right.setPreferredSize(new Dimension(255,0));right.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ink,5),new EmptyBorder(10,10,10,10)));
        JLabel partyTitle=new JLabel("YOUR PARTY  (0/6)");partyTitle.setFont(pixel);right.add(partyTitle,BorderLayout.NORTH);
        party.setFont(pixel);party.setBackground(paper);party.setFixedCellHeight(35);
        right.add(new JScrollPane(party),BorderLayout.CENTER);
        JPanel rightButtons=new JPanel(new GridLayout(0,1,5,5));rightButtons.setOpaque(false);
        JButton remove=retroButton("REMOVE SELECTED");remove.addActionListener(e -> {
            int i=party.getSelectedIndex();if(i>=0){partyName="Current party";ids.remove(i);selected.remove(i);refreshTitle(partyTitle);}
        });rightButtons.add(remove);
        JButton clear=retroButton("CLEAR PARTY");clear.addActionListener(e -> {partyName="Current party";ids.clear();selected.clear();refreshTitle(partyTitle);});rightButtons.add(clear);
        JButton random=retroButton("RANDOM TEAM");random.addActionListener(e -> {
            ids.clear();selected.clear();java.util.List<Integer> pool=new ArrayList<>();
            for(int i=1;i<=151;i++)pool.add(i);Collections.shuffle(pool);
            for(int i=0;i<6;i++)addPokemon(pool.get(i),partyTitle);
        });rightButtons.add(random);
        JButton database=retroButton("DATABASE SETTINGS");
        database.addActionListener(e -> new DatabaseSettingsDialog(this).setVisible(true));rightButtons.add(database);
        JButton saved=retroButton("SAVED TEAMS");
        saved.addActionListener(e -> new TeamLibraryDialog(this, AppServices.TEAMS, () -> java.util.List.copyOf(ids), t -> {
            java.util.List<Integer> members=TeamService.validMembers(t.pokemonIds());
            ids.clear();selected.clear();ids.addAll(members);
            for(int id:ids)selected.addElement(String.format("%03d  %s",id,KantoDex.name(id)));
            partyName=t.name();refreshTitle(partyTitle);info.setText("Loaded saved team: "+partyName);
        }).setVisible(true));rightButtons.add(saved);
        JButton report=retroButton("BATTLE REPORT");
        report.addActionListener(e -> new BattleReportDialog(this,AppServices.HISTORY).setVisible(true));rightButtons.add(report);
        startButton=retroButton("START BATTLE >>");startButton.setBackground(new Color(100,166,109));
        startButton.addActionListener(e -> beginBattle());rightButtons.add(startButton);
        right.add(rightButtons,BorderLayout.SOUTH);center.add(right,BorderLayout.EAST);
        info.setForeground(paper);info.setFont(pixel);info.setBorder(new EmptyBorder(5,4,2,0));main.add(info,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e){populate(partyTitle);}
            public void removeUpdate(javax.swing.event.DocumentEvent e){populate(partyTitle);}
            public void changedUpdate(javax.swing.event.DocumentEvent e){populate(partyTitle);}
        });
        populate(partyTitle);setVisible(true);
    }
    private JButton retroButton(String text) {
        JButton b=new JButton(text);b.setFont(pixel);b.setFocusPainted(false);
        b.setForeground(ink);b.setBackground(new Color(219,223,185));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ink,3),new EmptyBorder(7,4,7,4)));
        return b;
    }
    private void refreshTitle(JLabel title) {
    title.setText("YOUR PARTY  (" + ids.size() + "/6)");
    populate(title);
}

private void addPokemon(int id, JLabel title) {
    partyName = "Current party";
    // Clicking a selected Pokemon removes it.
    if (ids.contains(id)) {
        int index = ids.indexOf(id);
        ids.remove(index);
        selected.remove(index);

        refreshTitle(title);
        info.setText(KantoDex.name(id) + " left your party.");
        return;
    }

    // Only add a new Pokemon if there is room.
    if (ids.size() >= 6) {
        info.setText("Party full! Remove a Pokémon first.");
        return;
    }

    ids.add(id);
    selected.addElement(
            String.format("%03d  %s", id, KantoDex.name(id))
    );

    refreshTitle(title);
    info.setText(KantoDex.name(id) + " joined your party!");
}
    private void populate(JLabel title) {
        grid.removeAll();String filter=search.getText().trim().toLowerCase(Locale.ROOT);
        for(int id=1;id<=151;id++) {
            String name=KantoDex.name(id);
            if(!name.toLowerCase(Locale.ROOT).contains(filter) && !String.valueOf(id).equals(filter))continue;
            final int number=id;
            JButton b=retroButton(String.format("<html><center>%03d<br>%s</center></html>",id,name));
            b.setFont(new Font(Font.MONOSPACED,Font.BOLD,11));
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBackground(ids.contains(id) ? selectedColor : normalColor);
            b.setToolTipText("#"+id+"  "+name+"  "+KantoDex.type(id));
            b.addActionListener(e -> addPokemon(number,title));grid.add(b);
        }
        grid.revalidate();grid.repaint();
    }
    private void beginBattle() {
        if(preparing)return;
        if(ids.isEmpty()){info.setText("Choose at least one Pokémon first!");return;}
        java.util.List<Integer> choice=new ArrayList<>(ids);
        String battleTeamName=partyName;
        preparing=true;startButton.setEnabled(false);
        info.setText("Preparing Pokémon stats and moves...");
        new SwingWorker<PokemonTeam[],Void>() {
            protected PokemonTeam[] doInBackground() {
                PokemonTeam player=new PokemonTeam(),enemy=new PokemonTeam();
                for(int id:choice)player.addPokemon(KantoDex.create(id));
                java.util.List<Integer> pool=new ArrayList<>();for(int i=1;i<=151;i++)if(!choice.contains(i))pool.add(i);
                Collections.shuffle(pool);
                for(int i=0;i<Math.min(choice.size(),pool.size());i++)enemy.addPokemon(KantoDex.create(pool.get(i)));
                return new PokemonTeam[]{player,enemy};
            }
            protected void done() {
                try {PokemonTeam[] teams=get();if(!isDisplayable())return;dispose();new BattleWindow(teams[0],teams[1],battleTeamName);}
                catch(InterruptedException ex){Thread.currentThread().interrupt();info.setText("Battle preparation interrupted. Try again.");}
                catch(java.util.concurrent.ExecutionException ex){info.setText("Could not prepare battle: "+UiTasks.message(ex.getCause()));}
                finally {preparing=false;startButton.setEnabled(true);}
            }
        }.execute();
    }
}
