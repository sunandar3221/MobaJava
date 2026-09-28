package moba.ui;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.model.Hero;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MenuUI {
    public static class HeroDraftOption {
        public final String name;
        public final String role;
        public final String description;
        public final Color accentColor;

        public HeroDraftOption(String name, String role, String description, Color accentColor) {
            this.name = name;
            this.role = role;
            this.description = description;
            this.accentColor = accentColor;
        }
    }

    public static final List<HeroDraftOption> DRAFT_HEROES = List.of(
            new HeroDraftOption("Steve", "Fighter / EXP", "Diamond sword whirlwind, TNT tosses & ground stun", new Color(0, 180, 200)),
            new HeroDraftOption("Alex", "Marksman / Gold", "Rapid arrow volleys, explosive traps & mobility", new Color(220, 130, 40)),
            new HeroDraftOption("Evoker", "Mage / Mid", "Chomping ground fangs, healing aura & Vex tempest", new Color(120, 100, 220)),
            new HeroDraftOption("Iron Golem", "Tank / Roam", "Massive iron health, uppercut toss & seismic shock", new Color(180, 180, 170)),
            new HeroDraftOption("Nether Knight", "Assassin / Jungle", "Twin blade crimson slashes, smoke & abyssal execute", new Color(220, 40, 50))
    );

    private int selectedLaneIndex = 1; // Default MID
    private int selectedHeroIndex = 0; // Default Steve
    private float matchmakingTimer = 0f;
    private float draftTimer = 15.0f;
    private boolean matchAccepted = false;
    private float animTicker = 0f;

    public void update(float dt, GameEngine engine) {
        animTicker += dt * 3.0f;

        if (engine.getState() == GameEngine.GameState.MATCHMAKING) {
            matchmakingTimer += dt;
            if (matchmakingTimer >= 2.4f) {
                matchAccepted = true;
            }
            if (matchmakingTimer >= 3.2f) {
                engine.setState(GameEngine.GameState.DRAFT_PICK);
                draftTimer = 15.0f;
                matchmakingTimer = 0f;
                matchAccepted = false;
                SoundManager.playLevelUp();
            }
        } else if (engine.getState() == GameEngine.GameState.DRAFT_PICK) {
            draftTimer -= dt;
            if (draftTimer <= 0) {
                // Auto lock-in on timeout
                lockInDraft(engine);
            }
        }
    }

    public void render(Graphics2D g, int screenW, int screenH, GameEngine engine) {
        switch (engine.getState()) {
            case MAIN_MENU -> renderMainMenu(g, screenW, screenH);
            case LANE_SELECT -> renderLaneSelect(g, screenW, screenH);
            case MATCHMAKING -> renderMatchmaking(g, screenW, screenH);
            case DRAFT_PICK -> renderDraftPick(g, screenW, screenH);
            default -> {}
        }
    }

    // --- 1. MAIN MENU ---
    private void renderMainMenu(Graphics2D g, int screenW, int screenH) {
        // Dark voxel void background
        g.setColor(new Color(15, 18, 24));
        g.fillRect(0, 0, screenW, screenH);

        // Grid lines pattern
        g.setColor(new Color(25, 30, 40));
        for (int x = 0; x < screenW; x += 40) g.drawLine(x, 0, x, screenH);
        for (int y = 0; y < screenH; y += 40) g.drawLine(0, y, screenW, y);

        int cx = screenW / 2;

        // Title Banner
        g.setFont(new Font("Monospaced", Font.BOLD, 46));
        g.setColor(new Color(255, 215, 0));
        String title = "CRAFTARENA: BLOCK LEGENDS";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, cx - tw / 2, 170);

        // Subtitle
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(new Color(80, 220, 255));
        String sub = "5v5 MINECRAFT MOBA - EXP, MID, GOLD, ROAM & JUNGLE";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, cx - sw / 2, 210);

        // Buttons
        int btnW = 320;
        int btnH = 50;

        // Button 1: Start 5v5 Game
        int btn1Y = 280;
        g.setColor(new Color(30, 140, 70));
        g.fillRoundRect(cx - btnW / 2, btn1Y, btnW, btnH, 10, 10);
        g.setColor(new Color(80, 255, 140));
        g.drawRoundRect(cx - btnW / 2, btn1Y, btnW, btnH, 10, 10);

        g.setFont(new Font("Monospaced", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        String playText = "[ START 5v5 MATCH ]";
        int pw = g.getFontMetrics().stringWidth(playText);
        g.drawString(playText, cx - pw / 2, btn1Y + 32);

        // Features list
        int infoY = 380;
        g.setColor(new Color(20, 25, 35, 220));
        g.fillRoundRect(cx - 260, infoY, 520, 180, 12, 12);
        g.setColor(new Color(60, 70, 90));
        g.drawRoundRect(cx - 260, infoY, 520, 180, 12, 12);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(255, 215, 0));
        g.drawString("⭐ 5v5 MOBA GAMEPLAY FEATURES:", cx - 240, infoY + 30);

        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("• 5 Roles: EXP Lane, Mid Lane, Gold Lane, Roam, and Jungle", cx - 240, infoY + 60);
        g.drawString("• River Bosses: Dragon Turtle (Shield & Gold) & Nether Lord (Siege Titan)", cx - 240, infoY + 85);
        g.drawString("• Tall Grass Stealth: Hide in bushes & ambush/culik unsuspecting foes", cx - 240, infoY + 110);
        g.drawString("• Smart AI: Restrained turret dive (tidak nafsu kill) & grass ganks", cx - 240, infoY + 135);
        g.drawString("• Draft Pick: Select your hero & role before battle begins", cx - 240, infoY + 160);
    }

    // --- 2. LANE / ROLE SELECT ---
    private void renderLaneSelect(Graphics2D g, int screenW, int screenH) {
        g.setColor(new Color(15, 18, 24));
        g.fillRect(0, 0, screenW, screenH);

        int cx = screenW / 2;

        g.setFont(new Font("Monospaced", Font.BOLD, 32));
        g.setColor(new Color(255, 215, 0));
        String title = "CHOOSE YOUR PREFERRED LANE & ROLE";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, cx - tw / 2, 75);

        String[] roles = { "EXP LANE", "MID LANE", "GOLD LANE", "ROAM", "JUNGLE" };
        String[] icons = { "⚔", "🧙", "🏹", "🛡", "🗡" };
        String[] descs = {
                "Solo brawler, duels & lane holding",
                "Burst mage, fast waveclear & rotation",
                "Ranged marksman carry, high scaling",
                "Tank & vision control, bush ganker",
                "Buff hunter, Turtle/Lord killer & ambush"
        };
        Color[] colors = {
                new Color(220, 110, 40),
                new Color(120, 100, 240),
                new Color(240, 200, 50),
                new Color(60, 200, 240),
                new Color(220, 40, 60)
        };

        int cardW = 165;
        int cardH = 260;
        int spacing = 16;
        int totalW = 5 * cardW + 4 * spacing;
        int startX = (screenW - totalW) / 2;
        int startY = 130;

        for (int i = 0; i < 5; i++) {
            int x = startX + i * (cardW + spacing);
            boolean isSel = (i == selectedLaneIndex);

            // Card background
            g.setColor(isSel ? new Color(30, 45, 65) : new Color(20, 24, 32));
            g.fillRoundRect(x, startY, cardW, cardH, 10, 10);
            g.setColor(isSel ? colors[i] : new Color(60, 65, 80));
            g.setStroke(new BasicStroke(isSel ? 2.5f : 1.0f));
            g.drawRoundRect(x, startY, cardW, cardH, 10, 10);
            g.setStroke(new BasicStroke(1.0f));

            // Icon
            g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
            g.drawString(icons[i], x + cardW / 2 - 20, startY + 60);

            // Title
            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            g.setColor(colors[i]);
            int rw = g.getFontMetrics().stringWidth(roles[i]);
            g.drawString(roles[i], x + (cardW - rw) / 2, startY + 105);

            // Description
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(Color.LIGHT_GRAY);
            String[] words = descs[i].split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = startY + 135;
            for (String w : words) {
                if (line.length() + w.length() > 18) {
                    g.drawString(line.toString(), x + 10, lineY);
                    line = new StringBuilder(w + " ");
                    lineY += 16;
                } else {
                    line.append(w).append(" ");
                }
            }
            if (line.length() > 0) g.drawString(line.toString(), x + 10, lineY);

            // Select tag
            if (isSel) {
                g.setColor(colors[i]);
                g.fillRoundRect(x + 20, startY + cardH - 35, cardW - 40, 24, 6, 6);
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 11));
                g.drawString("SELECTED", x + cardW / 2 - 28, startY + cardH - 19);
            }
        }

        // Confirm Button
        int btnW = 280;
        int btnH = 48;
        int btnY = 440;
        g.setColor(new Color(30, 120, 220));
        g.fillRoundRect(cx - btnW / 2, btnY, btnW, btnH, 8, 8);
        g.setColor(Color.WHITE);
        g.drawRoundRect(cx - btnW / 2, btnY, btnW, btnH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        String confirmText = "[ FIND MATCH 5v5 ]";
        int cw = g.getFontMetrics().stringWidth(confirmText);
        g.drawString(confirmText, cx - cw / 2, btnY + 31);
    }

    // --- 3. MATCHMAKING SCREEN ---
    private void renderMatchmaking(Graphics2D g, int screenW, int screenH) {
        g.setColor(new Color(12, 15, 20));
        g.fillRect(0, 0, screenW, screenH);

        int cx = screenW / 2;
        int cy = screenH / 2;

        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        g.setColor(new Color(255, 215, 0));
        String title = matchAccepted ? "MATCH FOUND! ACCEPTING..." : "SEARCHING FOR 5v5 MATCH...";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, cx - tw / 2, cy - 80);

        // Animated Rotating Voxel Compass
        int spinnerR = 36;
        for (int i = 0; i < 8; i++) {
            double angle = animTicker + (Math.PI * 2 / 8) * i;
            int px = cx + (int) (Math.cos(angle) * spinnerR);
            int py = cy + (int) (Math.sin(angle) * spinnerR);
            int size = 6 + (int)(Math.sin(animTicker + i) * 3);
            g.setColor(matchAccepted ? new Color(60, 255, 120) : new Color(60, 180, 255));
            g.fillRect(px - size/2, py - size/2, size, size);
        }

        // Player count status
        int foundCount = Math.min(10, 2 + (int)(matchmakingTimer * 3.5f));
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String countStr = "Players Found: " + foundCount + " / 10";
        int cw = g.getFontMetrics().stringWidth(countStr);
        g.drawString(countStr, cx - cw / 2, cy + 90);

        // Selected lane reminder
        String[] roleNames = { "EXP Lane", "Mid Lane", "Gold Lane", "Roam", "Jungle" };
        g.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g.setColor(new Color(180, 220, 255));
        String laneReminder = "Assigned Role: " + roleNames[selectedLaneIndex];
        int lw = g.getFontMetrics().stringWidth(laneReminder);
        g.drawString(laneReminder, cx - lw / 2, cy + 120);
    }

    // --- 4. DRAFT PICK SCREEN ---
    private void renderDraftPick(Graphics2D g, int screenW, int screenH) {
        g.setColor(new Color(15, 18, 24));
        g.fillRect(0, 0, screenW, screenH);

        // Top bar: Draft Pick Timer
        int barH = 50;
        g.setColor(new Color(22, 26, 34));
        g.fillRect(0, 0, screenW, barH);
        g.setColor(new Color(60, 65, 80));
        g.drawLine(0, barH, screenW, barH);

        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(new Color(255, 215, 0));
        g.drawString("DRAFT PICK", 25, 34);

        // Snappy countdown timer in center
        int sec = Math.max(1, (int) Math.ceil(draftTimer));
        g.setColor(sec <= 5 ? new Color(255, 60, 60) : Color.WHITE);
        String timerStr = "Time Remaining: " + sec + "s";
        int tw = g.getFontMetrics().stringWidth(timerStr);
        g.drawString(timerStr, (screenW - tw) / 2, 34);

        // LEFT COLUMN: Blue Team (Player + 4 Allies)
        renderTeamDraftColumn(g, 15, 65, true, selectedHeroIndex, selectedLaneIndex);

        // RIGHT COLUMN: Red Team (5 Enemies)
        renderTeamDraftColumn(g, screenW - 225, 65, false, -1, -1);

        // CENTER: Hero Grid & Skill Details
        int centerW = screenW - 480;
        int centerX = 240;

        // Hero Selection Cards (5 Heroes)
        int heroCardW = (centerW - 4 * 10) / 5;
        int heroCardH = 110;
        int heroCardY = 70;

        for (int i = 0; i < DRAFT_HEROES.size(); i++) {
            HeroDraftOption opt = DRAFT_HEROES.get(i);
            int hx = centerX + i * (heroCardW + 10);
            boolean isSel = (i == selectedHeroIndex);

            g.setColor(isSel ? new Color(30, 48, 70) : new Color(22, 26, 34));
            g.fillRoundRect(hx, heroCardY, heroCardW, heroCardH, 8, 8);
            g.setColor(isSel ? opt.accentColor : new Color(60, 65, 75));
            g.setStroke(new BasicStroke(isSel ? 2.5f : 1.0f));
            g.drawRoundRect(hx, heroCardY, heroCardW, heroCardH, 8, 8);
            g.setStroke(new BasicStroke(1.0f));

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(Color.WHITE);
            int nw = g.getFontMetrics().stringWidth(opt.name);
            g.drawString(opt.name, hx + (heroCardW - nw) / 2, heroCardY + 30);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(opt.accentColor);
            int rw = g.getFontMetrics().stringWidth(opt.role);
            g.drawString(opt.role, hx + (heroCardW - rw) / 2, heroCardY + 52);

            if (isSel) {
                g.setColor(new Color(60, 255, 120));
                g.fillRect(hx + 8, heroCardY + heroCardH - 12, heroCardW - 16, 4);
            }
        }

        // Selected Hero Detailed Description & Skills Panel
        HeroDraftOption sel = DRAFT_HEROES.get(selectedHeroIndex);
        int detailY = 200;
        int detailH = 160;
        g.setColor(new Color(22, 26, 35));
        g.fillRoundRect(centerX, detailY, centerW, detailH, 10, 10);
        g.setColor(sel.accentColor);
        g.drawRoundRect(centerX, detailY, centerW, detailH, 10, 10);

        g.setFont(new Font("Monospaced", Font.BOLD, 20));
        g.setColor(sel.accentColor);
        g.drawString(sel.name + " - " + sel.role, centerX + 18, detailY + 34);

        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString(sel.description, centerX + 18, detailY + 62);

        // Skill slots preview
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(255, 215, 0));
        g.drawString("SKILL LOADOUT: [Q] Primary  [W] Secondary  [E] Dash/Blink  [R] Ultimate", centerX + 18, detailY + 105);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(180, 220, 255));
        g.drawString("Passive: Minecraft Voxel Resilience • Turtle Shield Ready • Bush Stealth Active", centerX + 18, detailY + 130);

        // LOCK IN BUTTON
        int btnW = 260;
        int btnH = 46;
        int btnX = centerX + (centerW - btnW) / 2;
        int btnY = 380;

        g.setColor(new Color(30, 140, 70));
        g.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
        g.setColor(new Color(80, 255, 140));
        g.drawRoundRect(btnX, btnY, btnW, btnH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String lockStr = "[ LOCK IN HERO ]";
        int lw = g.getFontMetrics().stringWidth(lockStr);
        g.drawString(lockStr, btnX + (btnW - lw) / 2, btnY + 29);
    }

    private void renderTeamDraftColumn(Graphics2D g, int startX, int startY, boolean isBlue, int selHeroIdx, int selLaneIdx) {
        int cardW = 210;
        int cardH = 68;
        int gap = 8;

        String[] blueNames = { "Steve", "Alex", "Evoker", "Iron Golem", "Nether Knight" };
        String[] redNames  = { "Herobrine", "Piglin Brute", "Wither Skeletor", "Warden", "Shadow Creeper" };
        String[] blueRoles = { "EXP Lane", "Gold Lane", "Mid Lane", "Roam", "Jungle" };
        String[] redRoles  = { "Mid Lane", "EXP Lane", "Gold Lane", "Roam", "Jungle" };

        String title = isBlue ? "BLUE ALLIANCE" : "RED HORDE";
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(isBlue ? new Color(60, 190, 255) : new Color(255, 70, 70));
        g.drawString(title, startX + 4, startY - 8);

        for (int i = 0; i < 5; i++) {
            int y = startY + i * (cardH + gap);

            g.setColor(new Color(20, 24, 32, 230));
            g.fillRoundRect(startX, y, cardW, cardH, 6, 6);
            g.setColor(isBlue ? new Color(50, 100, 160) : new Color(160, 50, 50));
            g.drawRoundRect(startX, y, cardW, cardH, 6, 6);

            String name = isBlue ? blueNames[i] : redNames[i];
            String role = isBlue ? blueRoles[i] : redRoles[i];

            if (isBlue && i == 0) {
                // Player slot
                name = DRAFT_HEROES.get(selHeroIdx).name + " (YOU)";
                g.setColor(new Color(255, 215, 0));
            } else {
                g.setColor(Color.WHITE);
            }

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.drawString(name, startX + 10, y + 26);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(isBlue ? new Color(120, 210, 255) : new Color(255, 130, 130));
            g.drawString(role, startX + 10, y + 48);

            // Locked badge
            g.setColor(new Color(80, 255, 100));
            g.fillRect(startX + cardW - 14, y + cardH / 2 - 5, 6, 10);
        }
    }

    public void handleMouseClick(int mx, int my, int screenW, int screenH, GameEngine engine) {
        int cx = screenW / 2;

        if (engine.getState() == GameEngine.GameState.MAIN_MENU) {
            int btnW = 320;
            int btnH = 50;
            int btn1Y = 280;
            if (mx >= cx - btnW / 2 && mx <= cx + btnW / 2 && my >= btn1Y && my <= btn1Y + btnH) {
                SoundManager.playLevelUp();
                engine.setState(GameEngine.GameState.LANE_SELECT);
            }
        } else if (engine.getState() == GameEngine.GameState.LANE_SELECT) {
            int cardW = 165;
            int cardH = 260;
            int spacing = 16;
            int totalW = 5 * cardW + 4 * spacing;
            int startX = (screenW - totalW) / 2;
            int startY = 130;

            for (int i = 0; i < 5; i++) {
                int x = startX + i * (cardW + spacing);
                if (mx >= x && mx <= x + cardW && my >= startY && my <= startY + cardH) {
                    selectedLaneIndex = i;
                    SoundManager.playCoin();
                    break;
                }
            }

            // Confirm Button
            int btnW = 280;
            int btnH = 48;
            int btnY = 440;
            if (mx >= cx - btnW / 2 && mx <= cx + btnW / 2 && my >= btnY && my <= btnY + btnH) {
                SoundManager.playLevelUp();
                engine.setState(GameEngine.GameState.MATCHMAKING);
                matchmakingTimer = 0f;
                matchAccepted = false;
            }
        } else if (engine.getState() == GameEngine.GameState.DRAFT_PICK) {
            int centerW = screenW - 480;
            int centerX = 240;
            int heroCardW = (centerW - 4 * 10) / 5;
            int heroCardH = 110;
            int heroCardY = 70;

            for (int i = 0; i < DRAFT_HEROES.size(); i++) {
                int hx = centerX + i * (heroCardW + 10);
                if (mx >= hx && mx <= hx + heroCardW && my >= heroCardY && my <= heroCardY + heroCardH) {
                    selectedHeroIndex = i;
                    SoundManager.playCoin();
                    break;
                }
            }

            // Lock In Button
            int btnW = 260;
            int btnH = 46;
            int btnX = centerX + (centerW - btnW) / 2;
            int btnY = 380;
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                lockInDraft(engine);
            }
        }
    }

    private void lockInDraft(GameEngine engine) {
        SoundManager.playLevelUp();
        Hero.Lane chosenLane;
        switch (selectedLaneIndex) {
            case 0 -> chosenLane = Hero.Lane.EXP;
            case 2 -> chosenLane = Hero.Lane.GOLD;
            case 3 -> chosenLane = Hero.Lane.ROAM;
            case 4 -> chosenLane = Hero.Lane.JUNGLE;
            default -> chosenLane = Hero.Lane.MID;
        }

        String chosenHero = DRAFT_HEROES.get(selectedHeroIndex).name;
        engine.startMatchWithDraft(chosenHero, chosenLane);
    }

    public int getSelectedLaneIndex() { return selectedLaneIndex; }
    public int getSelectedHeroIndex() { return selectedHeroIndex; }
}
