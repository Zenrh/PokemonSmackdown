package com.pokemon.battle;

import com.pokemon.battle.engine.BattleEngine;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.team.PokemonTeam;
import com.pokemon.battle.team.TeamFactory;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import com.pokemon.battle.model.BattleRecord;
import com.pokemon.battle.model.BattleOutcome;
import com.pokemon.battle.service.AppServices;
import com.pokemon.battle.ui.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Responsive, silent, classic-inspired battle screen. */
public class BattleWindow extends JFrame {
    // Edit these four coordinates to reposition sprites and their hit effects together.
    private static final int ENEMY_X = 600, ENEMY_FEET = 280, ENEMY_SIZE = 162;
    private static final int PLAYER_X = 200, PLAYER_FEET = 480, PLAYER_SIZE = 250;
    private final BattleEngine battle;
    private final BattleCanvas canvas = new BattleCanvas();
    private final JTextArea dialog = new JTextArea();
    private final JPanel actions = new JPanel(new GridLayout(2, 2, 8, 8));
    private final JPanel bottom = new JPanel(new BorderLayout(8, 0));
    private final JPanel actionArea = new JPanel(new BorderLayout(0, 6));
    private JButton backButton;
    private boolean busy = false;
    private boolean showingMoves = false;
    private double playerShownHP = 1, enemyShownHP = 1;
    private int tick = 0, hitSide = 0, effectTime = 0, ballTime = 65;
    private String effectType = "Normal";
    private BufferedImage playerImage, enemyImage;
    private Pokemon loadedPlayer, loadedEnemy;
    private final java.util.List<Timer> delayedActions = new java.util.ArrayList<>();
    private final String battleKey = java.util.UUID.randomUUID().toString();
    private final LocalDateTime startedAt = LocalDateTime.now(ZoneOffset.UTC);
    private final java.util.List<Integer> playerIds, enemyIds;
    private final String teamLabel;
    private int playerTurns;
    private boolean ended, savingResult, resultSaved;
    private BattleRecord completedResult;
    private JButton saveResultButton;
    private String finalMessage;
    private final Timer animation = new Timer(33, e -> animateFrame());

    public BattleWindow() { this(TeamFactory.createPlayerTeam(), TeamFactory.createEnemyTeam()); }

    public BattleWindow(PokemonTeam playerTeam, PokemonTeam enemyTeam) {
        this(playerTeam, enemyTeam, "Current party");
    }

    public BattleWindow(PokemonTeam playerTeam, PokemonTeam enemyTeam, String teamLabel) {
        super("Pokemon Battle Simulator");
        battle = new BattleEngine(playerTeam, enemyTeam);
        this.teamLabel = com.pokemon.battle.service.TeamService.validName(teamLabel);
        playerIds = playerTeam.getTeam().stream().map(p -> KantoDex.idFor(p.getName())).toList();
        enemyIds = enemyTeam.getTeam().stream().map(p -> KantoDex.idFor(p.getName())).toList();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if(savingResult) { JOptionPane.showMessageDialog(BattleWindow.this,"Please wait for the result save to finish."); return; }
                if(!ended) { if(!busy && JOptionPane.showConfirmDialog(BattleWindow.this,"End this battle as FLED?","Leave battle",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) finishBattle(BattleOutcome.FLED); }
                else returnToBuilder();
            }
        });
        setMinimumSize(new Dimension(720, 510));
        setSize(980, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(27, 42, 50));
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        bottom.setBackground(new Color(53, 65, 60));
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dialog.setEditable(false);
        dialog.setFocusable(false);
        dialog.setLineWrap(true);
        dialog.setWrapStyleWord(true);
        dialog.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        dialog.setBackground(new Color(246, 245, 215));
        dialog.setForeground(new Color(40, 51, 55));
        dialog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(79, 98, 104), 4),
            BorderFactory.createEmptyBorder(13, 15, 8, 12)));
        JScrollPane dialogScroll=new JScrollPane(dialog);
        dialogScroll.setBorder(null);
        bottom.add(dialogScroll, BorderLayout.CENTER);
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(450, 168));
        bottom.setPreferredSize(new Dimension(980, 182));
        actionArea.setOpaque(false);
        actionArea.setPreferredSize(new Dimension(450, 168));
        actionArea.add(actions, BorderLayout.CENTER);

        backButton = button(
                "BACK",
                new Color(180, 180, 180),
                this::showMainMenu
        );
        backButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        backButton.setPreferredSize(new Dimension(0, 34));

        actionArea.add(backButton, BorderLayout.SOUTH);
        bottom.add(actionArea, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        showMessage("A wild " + battle.getEnemyPokemon().getName() + " appeared!\nWhat will " + battle.getPlayerPokemon().getName() + " do?");
        showMainMenu();
        playerShownHP = hpRatio(battle.getPlayerPokemon());
        enemyShownHP = hpRatio(battle.getEnemyPokemon());
        loadSpritesAsync();
        animation.start();
        setVisible(true);
    }

    private static double hpRatio(Pokemon p) {
        return p == null ? 0 : (double) p.getCurrentHP() / Math.max(1, p.getMaxHP());
    }
    private void showMessage(String message) { dialog.setText(message); }
    private JButton button(String text, Color color, Runnable action) {
    JButton b = new JButton(text) {
        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            int w = getWidth();
            int h = getHeight();

            Color fill = new Color(246, 245, 215);

            if (!isEnabled()) {
                fill = new Color(205, 205, 195);
            } else if (getModel().isPressed()) {
                fill = fill.darker();
            } else if (getModel().isRollover()) {
                fill = fill.brighter();
            }

            // Main button background.
            g.setColor(fill);
            g.fillRect(0, 0, w, h);

            // Outer border.
            g.setColor(new Color(40, 51, 55));
            g.setStroke(new BasicStroke(3));
            g.drawRect(2, 2, w - 5, h - 5);

            // Inner border uses the button's supplied color.
            g.setColor(color);
            g.setStroke(new BasicStroke(2));
            g.drawRect(5, 5, w - 11, h - 11);

            g.dispose();

            // Paint the label over the background.
            super.paintComponent(graphics);
        }
    };

    b.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
    b.setForeground(new Color(32, 43, 37));
    b.setHorizontalAlignment(SwingConstants.CENTER);
    b.setVerticalAlignment(SwingConstants.CENTER);
    b.setFocusPainted(false);
    b.setContentAreaFilled(false);
    b.setOpaque(false);
    b.setRolloverEnabled(true);
    b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

    b.addActionListener(e -> {
        if (!busy) {
            action.run();
        }
    });

    return b;
}
    private void showMainMenu() {
        showingMoves = false;
        backButton.setVisible(false);
        actions.removeAll();
        actions.add(button("FIGHT", new Color(204, 79, 94), this::showMoves));
        actions.add(button("BAG", new Color(176, 128, 54), () -> showMessage("The bag is empty for now!")));
        actions.add(button("POKEMON", new Color(75, 142, 91), this::showSwitchMenu));
        actions.add(button("RUN", new Color(66, 118, 160), () -> {
            if (JOptionPane.showConfirmDialog(this, "Leave this battle and record FLED?", "Run", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) finishBattle(BattleOutcome.FLED);
        }));
        actions.revalidate(); actions.repaint();
    }
    private void showMoves() {
        showingMoves = true;
        backButton.setVisible(true);
        actions.removeAll();
        Pokemon p = battle.getPlayerPokemon();
        if(p.getMoves().stream().noneMatch(m -> m.getCurrentPP()>0)) {
            actions.add(button("STRUGGLE",moveColor("Normal"),() -> attack(-1)));
            actions.revalidate();actions.repaint();return;
        }
        for (int i = 0; i < p.getMoves().size(); i++) {
            final int index = i;
            Move m = p.getMoves().get(i);
            JButton b = button("<html><center>" + m.getName() + "<br><small>" + m.getType() + " | PP " + m.getCurrentPP() + "</small></center></html>", moveColor(m.getType()), () -> attack(index));
            b.setEnabled(m.getCurrentPP() > 0);
            actions.add(b);
        }
        actions.revalidate(); actions.repaint();
    }
    private static Color moveColor(String type) {
        switch (type) {
            case "Fire": return new Color(194, 91, 61);
            case "Water": return new Color(66, 123, 192);
            case "Grass": return new Color(69, 151, 89);
            case "Electric": return new Color(175, 141, 39);
            case "Ice": return new Color(57, 154, 166);
            default: return new Color(106, 112, 132);
        }
    }
    private void showSwitchMenu() {
        PokemonTeam team = battle.getPlayerTeam();
        String[] options = new String[team.getTeamSize()];
        for (int i = 0; i < options.length; i++) {
            Pokemon p = team.getPokemon(i);
            options[i] = p.getName() + "  " + p.getCurrentHP() + "/" + p.getMaxHP() + " HP" + (p.isFainted() ? " (fainted)" : "");
        }
        int index = JOptionPane.showOptionDialog(this, "Choose a Pokemon", "Your team",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (index < 0) return;
        if (team.getPokemon(index) == battle.getPlayerPokemon()) { showMessage("That Pokemon is already battling!"); return; }
        if (!battle.switchPlayerPokemon(index)) { showMessage("That Pokemon cannot battle!"); return; }
        showMessage("Go, " + battle.getPlayerPokemon().getName() + "!");
        playerShownHP = hpRatio(battle.getPlayerPokemon());
        ballTime = 0;
        loadSpritesAsync();
        showMainMenu();
        busy = true;
        later(1100, () -> { busy = false; showMessage("What will " + battle.getPlayerPokemon().getName() + " do?"); });
    }
    private void attack(int index) {
        if(ended || busy)return;
        Pokemon attacker = battle.getPlayerPokemon();
        if(attacker.isFainted()) { showMessage("Choose a healthy Pokemon using POKEMON."); showMainMenu(); return; }
        Move move = index == -1 ? new Move("Struggle","Normal",50,100,1) : attacker.getMoves().get(index);
        if (move.getCurrentPP() <= 0) return;
        busy = true;
        playerTurns++;
        effectType = move.getType();
        hitSide = 2;
        effectTime = 18;
        showMessage(battle.playerAttack(index));
        canvas.repaint();
        later(1050, () -> {
            if (battle.isBattleOver()) { finishBattle(); return; }
            ensureEnemyActive();
            enemyTurn();
        });
    }
    private void enemyTurn() {
        if(ended)return;
        Pokemon enemy = battle.getEnemyPokemon();
        if (enemy == null || enemy.isFainted()) { finishBattle(); return; }
        java.util.List<Integer> usable = new java.util.ArrayList<>();
        for(int i=0;i<enemy.getMoves().size();i++) if(enemy.getMoves().get(i).getCurrentPP()>0) usable.add(i);
        int index=usable.isEmpty() ? -1 : usable.get((int)(Math.random()*usable.size()));
        Move move = index == -1 ? new Move("Struggle","Normal",50,100,1) : enemy.getMoves().get(index);
        effectType = move.getType();
        hitSide = 1;
        effectTime = 18;
        showMessage(battle.enemyAttack(index));
        later(1100, () -> {
            if (battle.isBattleOver()) { finishBattle(); return; }
            if (battle.getPlayerPokemon().isFainted()) {
                showMessage("Your Pokemon fainted! Choose another Pokemon.");
                busy = false;
                showSwitchMenu();
                if (battle.getPlayerPokemon().isFainted()) {
                    showMessage("Choose a healthy Pokemon using POKEMON.");
                    showMainMenu();
                }
            } else {
                busy = false;
                if (showingMoves) showMoves(); else showMainMenu();
                showMessage("What will " + battle.getPlayerPokemon().getName() + " do?");
            }
        });
    }
    private void ensureEnemyActive() {
        if (!battle.getEnemyPokemon().isFainted()) return;
        PokemonTeam team = battle.getEnemyTeam();
        for (int i = 0; i < team.getTeamSize(); i++) {
            if (!team.getPokemon(i).isFainted()) {
                battle.switchEnemyPokemon(i);
                enemyShownHP = hpRatio(battle.getEnemyPokemon());
                loadSpritesAsync();
                showMessage("Opponent sent out " + battle.getEnemyPokemon().getName() + "!");
                break;
            }
        }
    }
    private void finishBattle() {
        finishBattle(battle.getPlayerTeam().hasAvailablePokemon() ? BattleOutcome.WIN : BattleOutcome.LOSS);
    }
    private void finishBattle(BattleOutcome outcome) {
        if(ended)return;
        ended=true;busy=false;showingMoves=false;
        for(Timer timer:delayedActions)timer.stop();delayedActions.clear();
        completedResult=new BattleRecord(battleKey,teamLabel,outcome,playerTurns,startedAt,
                LocalDateTime.now(ZoneOffset.UTC),playerIds,enemyIds);
        finalMessage=switch(outcome) { case WIN -> "You won the battle!"; case LOSS -> "You lost the battle!"; case FLED -> "You left the battle."; };
        actions.removeAll();backButton.setVisible(false);
        saveResultButton=button("SAVE RESULT",new Color(75,142,91),this::saveBattleResult);
        actions.add(saveResultButton);
        actions.add(button("DATABASE",new Color(180,180,180),() -> new DatabaseSettingsDialog(this).setVisible(true)));
        actions.add(button("REPORT",new Color(66,118,160),() -> new BattleReportDialog(this,AppServices.HISTORY).setVisible(true)));
        actions.add(button("TEAM BUILDER",new Color(176,128,54),this::returnToBuilder));
        actions.revalidate();actions.repaint();saveBattleResult();
    }
    private void saveBattleResult() {
        if(savingResult || resultSaved || completedResult==null)return;
        savingResult=true;saveResultButton.setEnabled(false);
        showMessage(finalMessage+"\nSaving result to Oracle...");
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() { AppServices.HISTORY.save(completedResult);return null; }
            @Override protected void done() {
                savingResult=false;
                try { get();resultSaved=true;showMessage(finalMessage+"\nResult saved. Open REPORT to view it.");saveResultButton.setText("RESULT SAVED"); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt();showMessage(finalMessage+"\nSave interrupted. Refresh the report before retrying."); }
                catch(java.util.concurrent.ExecutionException ex) { showMessage(finalMessage+"\nNot saved or not confirmed. "+UiTasks.message(ex.getCause())+" Use SAVE RESULT to retry."); }
                finally { saveResultButton.setEnabled(!resultSaved); }
            }
        }.execute();
    }
    private void returnToBuilder() {
        if(savingResult) { JOptionPane.showMessageDialog(this,"Please wait for the result save to finish.");return; }
        if(!resultSaved && JOptionPane.showConfirmDialog(this,"This result has not been confirmed as saved. Leave anyway?","Unsaved result",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        dispose();new TeamBuilderWindow();
    }
    @Override public void dispose() {
        if(animation!=null)animation.stop();
        if(delayedActions!=null) { for(Timer timer:delayedActions)timer.stop();delayedActions.clear(); }
        super.dispose();
    }
    private void later(int ms, Runnable action) {
        Timer t = new Timer(ms, e -> {
            Timer source=(Timer)e.getSource();delayedActions.remove(source);
            if(isDisplayable() && !ended)action.run();
        });
        delayedActions.add(t);
        t.setRepeats(false); t.start();
    }
    private void loadSpritesAsync() {
        Pokemon p = battle.getPlayerPokemon(), e = battle.getEnemyPokemon();
        loadedPlayer = p; loadedEnemy = e;
        playerImage = null; enemyImage = null;
        new SwingWorker<BufferedImage[], Void>() {
            protected BufferedImage[] doInBackground() {
                return new BufferedImage[]{SpriteLoader.loadOriginal(p, true), SpriteLoader.loadOriginal(e, false)};
            }
            protected void done() {
                if(!isDisplayable())return;
                try {
                    BufferedImage[] images = get();
                    if (loadedPlayer == p && loadedEnemy == e) {
                        playerImage = images[0]; enemyImage = images[1]; canvas.repaint();
                    }
                } catch (Exception ex) { System.err.println("Sprite download failed: " + ex.getMessage()); }
            }
        }.execute();
    }
    private void animateFrame() {
        tick++;
        if (effectTime > 0) effectTime--;
        if (ballTime < 65) ballTime++;
        playerShownHP += (hpRatio(battle.getPlayerPokemon()) - playerShownHP) * .13;
        enemyShownHP += (hpRatio(battle.getEnemyPokemon()) - enemyShownHP) * .13;
        canvas.repaint();
    }

    private class BattleCanvas extends JPanel {
        private final Image background;
        private final Image enemyFrame = emeraldImage("healthbox_singles_opponent.png");
        private final Image playerFrame = emeraldImage("healthbox_singles_player.png");
        private static Image emeraldImage(String name) {
            java.net.URL url = BattleWindow.class.getResource("/assets/emerald/" + name);
            return url == null ? null : new ImageIcon(url).getImage();
        }
        BattleCanvas() {
            setBackground(new Color(91, 188, 109));
            java.net.URL url = getClass().getResource("/assets/backgrounds/grass.png");
            background = url == null ? null : new ImageIcon(url).getImage();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // Fill the available battle area in both windowed and fullscreen modes.
            // Scale every element together with the background, rather than
            // applying a different transform to the Pokemon and HP panels.
            double scaleX = w / 800.0;
            double scaleY = h / 470.0;
            g.scale(scaleX, scaleY);
            if (background != null) g.drawImage(background, 0, 0, 800, 470, this);
            else { g.setColor(new Color(126, 207, 114)); g.fillRect(0, 0, 800, 470); }
            drawSprite(g, enemyImage, ENEMY_X, ENEMY_FEET, ENEMY_SIZE, false, 2);
            drawSprite(g, playerImage, PLAYER_X, PLAYER_FEET, PLAYER_SIZE, true, 1);
            if (ballTime < 35) drawBall(g);
            drawHud(g, battle.getEnemyPokemon(), 24, 22, enemyShownHP, false);
            drawHud(g, battle.getPlayerPokemon(), 425, 313, playerShownHP, true);
            if (effectTime > 0) drawEffect(g);
            g.dispose();
        }
        private void drawSprite(Graphics2D g, BufferedImage image, int cx, int feet, int max, boolean player, int side) {
            if ((player && battle.getPlayerPokemon().isFainted()) || (!player && battle.getEnemyPokemon().isFainted())) return;
            int bob = (int) Math.round(Math.sin(tick * .085 + (player ? 0 : 2)) * 3);
            int knock = effectTime > 0 && hitSide == side ? (effectTime % 4 < 2 ? 7 : -7) : 0;
            if (image != null) {
                // Crop transparent padding so the visible feet align with the ground.
                int minX=image.getWidth(), minY=image.getHeight(), maxX=0, maxY=0;
                for (int y=0; y<image.getHeight(); y++) for (int x=0; x<image.getWidth(); x++) {
                    if ((image.getRGB(x,y) >>> 24) > 25) { minX=Math.min(minX,x); minY=Math.min(minY,y); maxX=Math.max(maxX,x); maxY=Math.max(maxY,y); }
                }
                if (minX <= maxX) {
                    int sw=maxX-minX+1, sh=maxY-minY+1;
                    double factor=Math.min(max/(double)sw, max/(double)sh);
                    int dw=(int)(sw*factor), dh=(int)(sh*factor);
                    g.drawImage(image, cx-dw/2+knock, feet-dh+bob, cx+dw/2+knock, feet+bob,
                        minX,minY,maxX+1,maxY+1,this);
                    return;
                }
            }
            g.setColor(new Color(255, 250, 235, 160));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g.drawString("Loading sprite...", cx-65, feet-70);
        }
        private void drawHud(Graphics2D g, Pokemon p, int x, int y, double fraction, boolean player) {
            // All labels and bars are positioned relative to the SAME large HUD frame.
            // Reference canvas: 800 x 470; fullscreen scales this entire canvas.
            final int fw = 350;
            final int fh = player ? 132 : 98;
            Image frame = player ? playerFrame : enemyFrame;
            if (frame != null) {
    // Crop the artwork to its main box, removing transparent space
    // and the decorative strip underneath.
    int left = player ? 9 : 1;
    int top = 2;
    int right = player ? 103 : 95;
    int bottom = player ? 34 : 26;

    g.drawImage(
            frame,
            x, y, x + fw, y + fh,
            left, top, right, bottom,
            this
    );
}
            else {
                g.setColor(new Color(245, 244, 218));
                g.fillRoundRect(x, y, fw, fh, 12, 12);
            }
            g.setColor(new Color(38, 45, 40));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
            String name = p.getName().toUpperCase();
            // Keep longer names from colliding with the level label.
            if (name.length() > 11) g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g.drawString(name, x + 22, y + 35);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            g.drawString("Lv.50", x + 265, y + 35);

            double ratio = Math.max(0, Math.min(1, fraction));
            int bx = x + 112, by = y + 53, bw = 206, bh = 15;
            g.setColor(new Color(34, 43, 38));
            g.fillRoundRect(bx - 3, by - 3, bw + 6, bh + 6, 5, 5);
            g.setColor(new Color(234, 197, 62));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g.drawString("HP", x + 80, by + 12);
            g.setColor(new Color(87, 92, 80));
            g.fillRect(bx, by, bw, bh);
            g.setColor(ratio > .5 ? new Color(76, 208, 76) : ratio > .2 ?
                new Color(242, 197, 55) : new Color(224, 69, 62));
            g.fillRect(bx, by, (int)Math.round(bw * ratio), bh);
            if (player) {
                g.setColor(new Color(39, 46, 42));
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
                String hp = p.getCurrentHP() + " / " + p.getMaxHP();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(hp, x + fw - 28 - fm.stringWidth(hp), y + 100);
            }
        }
        private void drawBall(Graphics2D g) {
            double t=ballTime/35.0;
            int x=(int)(-20+240*t), y=(int)(340-140*Math.sin(Math.PI*t));
            // 8-bit pixel Poké Ball, drawn on an 8x8 grid.
            String[] pixels={"..KKKK..",".KRRRRK.","KRRRRRRK","KRRKKRRK","KWWKKWWK","KWWWWWWK",".KWWWWK.","..KKKK.."};
            for(int row=0;row<8;row++) for(int col=0;col<8;col++) {
                char c=pixels[row].charAt(col);
                if(c=='.')continue;
                g.setColor(c=='K'?new Color(32,37,49):c=='R'?new Color(226,67,65):Color.WHITE);
                g.fillRect(x-16+col*4,y-16+row*4,4,4);
            }
        }
        private void drawEffect(Graphics2D g) {
            // These anchors match the drawSprite centers/feet above.
            // Keep them tied to the sprite geometry, not unrelated hardcoded hit coordinates.
            int x = hitSide == 1 ? PLAYER_X : ENEMY_X;
            int y = hitSide == 1 ? PLAYER_FEET - PLAYER_SIZE / 2 : ENEMY_FEET - ENEMY_SIZE / 2;
            Color color=moveColor(effectType);
            g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),105));
            int r=(19-effectTime)*9;
            g.setStroke(new BasicStroke(5));
            for(int i=0;i<3;i++) g.drawOval(x-r/2-i*11,y-r/2-i*11,r+i*22,r+i*22);
            g.setColor(new Color(255,255,255,140));
            for(int i=0;i<6;i++) {
                double a=i*Math.PI/3+tick*.2;
                int px=x+(int)(Math.cos(a)*r*.7), py=y+(int)(Math.sin(a)*r*.7);
                g.fillOval(px-5,py-5,10,10);
            }
        }
    }
}
