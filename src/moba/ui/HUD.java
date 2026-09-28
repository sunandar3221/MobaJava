package moba.ui;

import moba.GameEngine;
import moba.model.Entity;
import moba.model.Hero;
import moba.model.Skill;
import moba.world.GameMap;

import java.awt.*;
import java.util.List;

public class HUD {

    public void render(Graphics2D g, int screenW, int screenH, Hero player, GameEngine engine) {
        // 1. Top Bar: Match Timer, Scoreboard, and 3v3 Hero Status Badges
        renderTopScoreboard(g, screenW, player, engine);

        // 2. Bottom Minecraft MOBA Console Dashboard Panel
        int consoleW = 430;
        int consoleH = 120;
        int consoleX = (screenW - consoleW) / 2;
        int consoleY = screenH - consoleH - 6;

        g.setColor(new Color(18, 22, 28, 245));
        g.fillRoundRect(consoleX, consoleY, consoleW, consoleH, 12, 12);
        g.setColor(new Color(75, 80, 95));
        g.drawRoundRect(consoleX, consoleY, consoleW, consoleH, 12, 12);

        // Minecraft Hearts & Mana Bar
        renderHealthAndMana(g, screenW, screenH, player);

        // Bottom Minecraft Hotbar & Skills
        renderHotbar(g, screenW, screenH, player);

        // 3. Minimap at Bottom-Right
        renderMinimap(g, screenW, screenH, engine);

        // 4. MLBB-Style Kill Notification Banner and Killfeed
        renderKillFeed(g, screenW, engine);
        renderKillBanner(g, screenW, screenH, engine);

        // 5. Player Death Screen with Live Respawn Countdown
        if (!player.alive) {
            renderPlayerDeathScreen(g, screenW, screenH, player);
        }

        // 6. Victory / Defeat Overlay
        if (engine.isGameOver()) {
            renderGameOver(g, screenW, screenH, engine);
        }
    }

    private void renderTopScoreboard(Graphics2D g, int screenW, Hero player, GameEngine engine) {
        int panelW = 420;
        int panelH = 44;
        int panelX = (screenW - panelW) / 2;
        int panelY = 8;

        // Dark banner
        g.setColor(new Color(20, 20, 25, 225));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);
        g.setColor(new Color(60, 60, 70));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        // Match Time
        int totalSec = (int) engine.getMatchTime();
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        String timeStr = String.format("%02d:%02d", mins, secs);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        int tw = g.getFontMetrics().stringWidth(timeStr);
        g.drawString(timeStr, panelX + (panelW - tw) / 2, panelY + 18);

        // Boss Objective Status
        if (engine.getJungleCamp() != null) {
            String objStatus = engine.getJungleCamp().getObjectiveStatus(engine.getMatchTime());
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            if (objStatus.contains("Lord")) {
                g.setColor(new Color(255, 170, 50));
            } else {
                g.setColor(new Color(80, 240, 140));
            }
            int ow = g.getFontMetrics().stringWidth(objStatus);
            g.drawString(objStatus, panelX + (panelW - ow) / 2, panelY + 34);
        }

        // Calculate Team Kills across all 5 heroes
        int blueTeamKills = 0;
        for (Hero h : engine.getBlueHeroes()) blueTeamKills += h.kills;
        int redTeamKills = 0;
        for (Hero h : engine.getRedHeroes()) redTeamKills += h.kills;

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(new Color(60, 180, 255));
        g.drawString("BLUE " + blueTeamKills, panelX + 25, panelY + 27);

        g.setColor(new Color(255, 60, 60));
        g.drawString(redTeamKills + " RED", panelX + panelW - 95, panelY + 27);

        // Render 5v5 Hero Status Badges (Left for Blue, Right for Red)
        renderHeroTeamStatus(g, 15, 12, engine.getBlueHeroes(), true);
        renderHeroTeamStatus(g, screenW - 220, 12, engine.getRedHeroes(), false);

        // Player KDA and CS
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        String kdaStr = String.format("K/D/A: %d/%d/%d  CS: %d", player.kills, player.deaths, player.assists, player.creepScore);
        g.drawString(kdaStr, panelX + 30, panelY + 60);

        // Emeralds / Gold
        g.setColor(new Color(60, 255, 100));
        g.drawString("Emeralds: " + player.gold + "  [P] Shop", panelX + panelW - 190, panelY + 60);
    }

    private void renderHeroTeamStatus(Graphics2D g, int startX, int startY, List<Hero> heroes, boolean isBlue) {
        int cardW = 205;
        int cardH = 24;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));

        for (int i = 0; i < heroes.size(); i++) {
            Hero h = heroes.get(i);
            int cy = startY + i * (cardH + 3);

            // Background card
            g.setColor(new Color(20, 24, 30, 210));
            g.fillRoundRect(startX, cy, cardW, cardH, 6, 6);
            g.setColor(isBlue ? new Color(50, 100, 160) : new Color(160, 50, 50));
            g.drawRoundRect(startX, cy, cardW, cardH, 6, 6);

            // Hero Name & Level
            g.setColor(Color.WHITE);
            String nameText = h.heroName + " (L" + h.level + ")";
            g.drawString(nameText, startX + 6, cy + 11);

            if (h.alive) {
                // Mini HP bar
                int hpBarW = 70;
                int hpBarH = 5;
                int hpBarX = startX + cardW - hpBarW - 6;
                int hpBarY = cy + 5;

                g.setColor(new Color(40, 10, 10));
                g.fillRect(hpBarX, hpBarY, hpBarW, hpBarH);

                float pct = Math.max(0f, Math.min(1f, h.hp / (h.maxHp + h.bonusHp)));
                g.setColor(isBlue ? new Color(50, 220, 80) : new Color(240, 60, 60));
                g.fillRect(hpBarX, hpBarY, (int) (hpBarW * pct), hpBarH);
                g.setColor(Color.BLACK);
                g.drawRect(hpBarX, hpBarY, hpBarW, hpBarH);

                // Lane badge
                g.setColor(new Color(200, 200, 150));
                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g.drawString(h.assignedLane.name(), startX + 6, cy + 21);
                g.setFont(new Font("Monospaced", Font.BOLD, 10));
            } else {
                // Dead Hero: Display Live Snappy Respawn Countdown!
                int sec = Math.max(1, (int) Math.ceil(h.respawnTimer));
                g.setColor(new Color(255, 70, 70));
                String respawnStr = "Respawn: " + sec + "s";
                g.drawString(respawnStr, startX + cardW - 95, cy + 16);
            }
        }
    }

    private void renderPlayerDeathScreen(Graphics2D g, int screenW, int screenH, Hero player) {
        // Red translucent death vignette
        g.setColor(new Color(130, 0, 0, 150));
        g.fillRect(0, 0, screenW, screenH);

        int boxW = 440;
        int boxH = 160;
        int boxX = (screenW - boxW) / 2;
        int boxY = (screenH - boxH) / 2 - 40;

        // Dark Minecraft death dialog frame
        g.setColor(new Color(20, 15, 18, 235));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g.setColor(new Color(200, 40, 40));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // "YOU DIED!" Title
        g.setFont(new Font("Monospaced", Font.BOLD, 38));
        g.setColor(new Color(240, 30, 30));
        String title = "YOU DIED!";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (screenW - tw) / 2, boxY + 48);

        // Killer info
        g.setFont(new Font("Monospaced", Font.PLAIN, 15));
        g.setColor(Color.LIGHT_GRAY);
        String killerText = "Killed by: " + player.lastKilledByName;
        int kw = g.getFontMetrics().stringWidth(killerText);
        g.drawString(killerText, (screenW - kw) / 2, boxY + 75);

        // Prominent Snappy Respawn Countdown (MLBB style)
        int remainingSec = Math.max(1, (int) Math.ceil(player.respawnTimer));
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(new Color(255, 215, 0));
        String respawnText = "RESPAWNING IN: " + remainingSec + "s";
        int rw = g.getFontMetrics().stringWidth(respawnText);
        g.drawString(respawnText, (screenW - rw) / 2, boxY + 110);

        // Respawn Progress Bar
        int barW = 320;
        int barH = 10;
        int barX = (screenW - barW) / 2;
        int barY = boxY + 128;

        g.setColor(new Color(40, 40, 40));
        g.fillRect(barX, barY, barW, barH);

        float progress = 1.0f - (player.respawnTimer / Math.max(1.0f, player.maxRespawnTime));
        progress = Math.max(0f, Math.min(1f, progress));

        g.setColor(new Color(80, 220, 50));
        g.fillRect(barX, barY, (int) (barW * progress), barH);
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barW, barH);
    }

    private void renderHealthAndMana(Graphics2D g, int screenW, int screenH, Hero player) {
        int centerX = screenW / 2;
        int bottomY = screenH - 74;

        float maxHp = player.maxHp + player.bonusHp;
        int totalHearts = 10;
        int heartW = 14;
        int heartSpacing = 16;
        int startX = centerX - 165;

        float hpPerHeart = maxHp / totalHearts;

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        String hpStr = (int) player.hp + " / " + (int) maxHp + " HP";
        g.drawString(hpStr, startX, bottomY - 26);

        if (player.shieldHp > 0) {
            int hpw = g.getFontMetrics().stringWidth(hpStr);
            g.setColor(new Color(60, 220, 255));
            g.drawString(" [+" + (int) player.shieldHp + " Shield]", startX + hpw + 6, bottomY - 26);
        }

        String manaStr = (int) player.mana + " / " + (int) player.maxMana + " MP";
        g.setColor(new Color(120, 190, 255));
        int mw = g.getFontMetrics().stringWidth(manaStr);
        g.drawString(manaStr, centerX + 165 - mw, bottomY - 26);

        // Active Buff Indicators
        int buffX = startX;
        int buffY = bottomY - 38;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        if (player.blueBuffTimer > 0) {
            String bStr = String.format("[Blue Buff: %ds]", (int) Math.ceil(player.blueBuffTimer));
            g.setColor(new Color(60, 200, 255));
            g.drawString(bStr, buffX, buffY);
            buffX += g.getFontMetrics().stringWidth(bStr) + 8;
        }
        if (player.redBuffTimer > 0) {
            String rStr = String.format("[Red Buff: %ds]", (int) Math.ceil(player.redBuffTimer));
            g.setColor(new Color(255, 110, 40));
            g.drawString(rStr, buffX, buffY);
        }

        for (int i = 0; i < totalHearts; i++) {
            int hx = startX + i * heartSpacing;
            int hy = bottomY - 20;

            g.setColor(new Color(40, 0, 0));
            g.fillRect(hx, hy + 2, heartW, heartW - 2);

            float heartThreshold = i * hpPerHeart;
            if (player.hp >= heartThreshold + hpPerHeart) {
                g.setColor(new Color(220, 20, 20));
                g.fillRect(hx + 1, hy + 3, heartW - 2, heartW - 4);
                g.setColor(new Color(255, 100, 100));
                g.fillRect(hx + 2, hy + 4, 3, 3);
            } else if (player.hp > heartThreshold) {
                g.setColor(new Color(220, 20, 20));
                g.fillRect(hx + 1, hy + 3, (heartW - 2) / 2, heartW - 4);
            }
        }

        int manaStartX = centerX + 15;
        int totalBubbles = 10;
        float manaPerBubble = player.maxMana / totalBubbles;

        for (int i = 0; i < totalBubbles; i++) {
            int bx = manaStartX + i * heartSpacing;
            int hy = bottomY - 20;

            g.setColor(new Color(0, 20, 50));
            g.fillRect(bx, hy + 2, heartW, heartW - 2);

            if (player.mana >= (i + 1) * manaPerBubble) {
                g.setColor(new Color(30, 140, 255));
                g.fillRect(bx + 1, hy + 3, heartW - 2, heartW - 4);
                g.setColor(new Color(150, 210, 255));
                g.fillRect(bx + 2, hy + 4, 3, 3);
            }
        }

        int xpBarW = 340;
        int xpBarH = 5;
        int xpX = centerX - xpBarW / 2;
        int xpY = bottomY - 3;

        g.setColor(new Color(20, 20, 20));
        g.fillRect(xpX - 1, xpY - 1, xpBarW + 2, xpBarH + 2);
        g.setColor(new Color(30, 80, 20));
        g.fillRect(xpX, xpY, xpBarW, xpBarH);

        float xpPct = (float) player.xp / player.xpToNext;
        g.setColor(new Color(110, 230, 30));
        g.fillRect(xpX, xpY, (int) (xpBarW * xpPct), xpBarH);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        String lvlStr = String.valueOf(player.level);
        g.setColor(new Color(0, 0, 0));
        g.drawString(lvlStr, centerX - 4, xpY + 1);
        g.setColor(new Color(120, 255, 40));
        g.drawString(lvlStr, centerX - 5, xpY);
    }

    private void renderHotbar(Graphics2D g, int screenW, int screenH, Hero player) {
        int slotSize = 44;
        int slotCount = 6;
        int hotbarW = slotCount * (slotSize + 4);
        int startX = (screenW - hotbarW) / 2;
        int startY = screenH - 58;

        Skill[] skills = { player.skillQ, player.skillW, player.skillE, player.skillR };
        String[] keys = { "Q", "W", "E", "R", "B", "P" };

        for (int i = 0; i < slotCount; i++) {
            int sx = startX + i * (slotSize + 4);

            g.setColor(new Color(140, 140, 140));
            g.fillRect(sx, startY, slotSize, slotSize);
            g.setColor(new Color(50, 50, 50));
            g.drawRect(sx, startY, slotSize, slotSize);
            g.setColor(Color.WHITE);
            g.drawLine(sx, startY, sx + slotSize, startY);
            g.drawLine(sx, startY, sx, startY + slotSize);

            if (i < 4 && skills[i] != null) {
                Skill s = skills[i];
                drawSkillIcon(g, sx + 5, startY + 5, i);

                // Ultimate [R] Lock until Level 4
                if (i == 3 && player.level < 4) {
                    // Dark padlock overlay
                    g.setColor(new Color(0, 0, 0, 205));
                    g.fillRect(sx, startY, slotSize, slotSize);

                    // Golden Padlock Icon
                    g.setColor(new Color(255, 215, 0));
                    g.drawArc(sx + 15, startY + 8, 14, 14, 0, 180);
                    g.fillRect(sx + 13, startY + 16, 18, 11);
                    g.setColor(new Color(60, 40, 0));
                    g.fillRect(sx + 20, startY + 19, 4, 4);

                    // Requirement Tag "Lv.4"
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(255, 230, 80));
                    String reqStr = "Lv.4";
                    int rtw = g.getFontMetrics().stringWidth(reqStr);
                    g.drawString(reqStr, sx + (slotSize - rtw) / 2, startY + 39);
                } else if (!s.isReady()) {
                    g.setColor(new Color(0, 0, 0, 175));
                    g.fillRect(sx, startY, slotSize, slotSize);

                    g.setFont(new Font("Monospaced", Font.BOLD, 13));
                    g.setColor(Color.WHITE);
                    String cdStr = String.format("%.1f", s.currentCooldown);
                    int tw = g.getFontMetrics().stringWidth(cdStr);
                    g.drawString(cdStr, sx + (slotSize - tw) / 2, startY + 28);
                }
            } else if (i == 4) {
                g.setColor(new Color(80, 200, 255));
                g.fillRect(sx + 12, startY + 8, 20, 26);
                if (player.isRecalling) {
                    g.setColor(new Color(0, 0, 0, 160));
                    g.fillRect(sx, startY, slotSize, slotSize);
                }
            } else {
                g.setColor(new Color(50, 220, 70));
                Polygon em = new Polygon();
                em.addPoint(sx + 22, startY + 8);
                em.addPoint(sx + 34, startY + 22);
                em.addPoint(sx + 22, startY + 36);
                em.addPoint(sx + 10, startY + 22);
                g.fillPolygon(em);
            }

            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(new Color(255, 230, 80));
            g.drawString("[" + keys[i] + "]", sx + 3, startY + 13);
        }
    }

    private void drawSkillIcon(Graphics2D g, int x, int y, int idx) {
        switch (idx) {
            case 0 -> {
                g.setColor(new Color(40, 240, 230));
                g.fillRect(x + 14, y + 4, 6, 24);
                g.setColor(new Color(180, 140, 40));
                g.fillRect(x + 8, y + 24, 18, 4);
            }
            case 1 -> {
                g.setColor(new Color(220, 40, 40));
                g.fillRect(x + 5, y + 5, 22, 22);
                g.setColor(Color.WHITE);
                g.fillRect(x + 5, y + 12, 22, 7);
                g.setColor(Color.BLACK);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString("TNT", x + 7, y + 18);
            }
            case 2 -> {
                g.setColor(new Color(20, 80, 70));
                g.fillOval(x + 5, y + 5, 22, 22);
                g.setColor(new Color(40, 180, 140));
                g.fillOval(x + 9, y + 9, 14, 14);
            }
            case 3 -> {
                g.setColor(new Color(240, 30, 30));
                g.fillOval(x + 4, y + 4, 26, 26);
                g.setColor(Color.YELLOW);
                g.fillOval(x + 9, y + 9, 15, 15);
            }
        }
    }

    private void renderMinimap(Graphics2D g, int screenW, int screenH, GameEngine engine) {
        int mapSize = 140;
        int mapX = screenW - mapSize - 16;
        int mapY = screenH - mapSize - 16;

        g.setColor(new Color(15, 18, 22, 230));
        g.fillRect(mapX, mapY, mapSize, mapSize);
        g.setColor(new Color(80, 85, 95));
        g.drawRect(mapX, mapY, mapSize, mapSize);

        // 3-Lane guidelines on minimap:
        // Mid Lane (Diagonal)
        g.setColor(new Color(75, 75, 75));
        g.drawLine(mapX + 12, mapY + mapSize - 12, mapX + mapSize - 12, mapY + 12);
        // Top Lane (Left edge and Top edge)
        g.drawLine(mapX + 12, mapY + mapSize - 12, mapX + 12, mapY + 12);
        g.drawLine(mapX + 12, mapY + 12, mapX + mapSize - 12, mapY + 12);
        // Bot Lane (Bottom edge and Right edge)
        g.drawLine(mapX + 12, mapY + mapSize - 12, mapX + mapSize - 12, mapY + mapSize - 12);
        g.drawLine(mapX + mapSize - 12, mapY + mapSize - 12, mapX + mapSize - 12, mapY + 12);

        // River crossing
        g.setColor(new Color(35, 75, 150, 180));
        g.drawLine(mapX + 12, mapY + 12, mapX + mapSize - 12, mapY + mapSize - 12);

        // Draw entities on minimap (filtered by Fog of War for enemies)
        for (Entity e : engine.getAllEntities()) {
            if (!engine.isEntityVisibleToPlayer(e)) continue;

            float normX = e.x / GameMap.MAP_WIDTH;
            float normY = e.y / GameMap.MAP_HEIGHT;
            int ex = mapX + (int) (normX * mapSize);
            int ey = mapY + mapSize - (int) (normY * mapSize);

            if (e instanceof Hero h) {
                if (h.alive) {
                    g.setColor(h.isPlayer ? new Color(50, 255, 255) : (h.team == Entity.TEAM_BLUE ? new Color(80, 200, 255) : new Color(255, 70, 70)));
                    g.fillOval(ex - 4, ey - 4, 8, 8);
                    g.setColor(Color.WHITE);
                    g.drawOval(ex - 4, ey - 4, 8, 8);
                } else {
                    // Dead hero red X marker
                    g.setColor(Color.RED);
                    g.drawLine(ex - 3, ey - 3, ex + 3, ey + 3);
                    g.drawLine(ex - 3, ey + 3, ex + 3, ey - 3);
                }
            } else if (e.alive) {
                if (e instanceof moba.model.Tower) {
                    g.setColor(e.team == Entity.TEAM_BLUE ? new Color(60, 160, 255) : new Color(255, 80, 80));
                    g.fillRect(ex - 2, ey - 2, 5, 5);
                } else if (e instanceof moba.model.BaseCore) {
                    g.setColor(e.team == Entity.TEAM_BLUE ? new Color(40, 220, 255) : new Color(255, 40, 40));
                    g.fillRect(ex - 4, ey - 4, 8, 8);
                } else if (e instanceof moba.model.JungleCamp) {
                    g.setColor(new Color(255, 215, 0));
                    g.fillOval(ex - 3, ey - 3, 6, 6);
                } else if (e instanceof moba.model.BuffMonster bm) {
                    g.setColor(bm.buffType == moba.model.BuffMonster.BuffType.BLUE ? new Color(60, 190, 255) : new Color(255, 90, 30));
                    g.fillRect(ex - 2, ey - 2, 4, 4);
                } else if (e instanceof moba.model.Minion) {
                    g.setColor(e.team == Entity.TEAM_BLUE ? new Color(100, 220, 100) : new Color(220, 100, 100));
                    g.fillRect(ex - 1, ey - 1, 3, 3);
                }
            }
        }
    }

    private void renderGameOver(Graphics2D g, int screenW, int screenH, GameEngine engine) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, screenW, screenH);

        boolean victory = engine.isPlayerVictory();
        String title = victory ? "VICTORY!" : "DEFEAT";
        Color titleColor = victory ? new Color(60, 240, 100) : new Color(240, 50, 50);

        g.setFont(new Font("Monospaced", Font.BOLD, 48));
        int tw = g.getFontMetrics().stringWidth(title);
        g.setColor(titleColor);
        g.drawString(title, (screenW - tw) / 2, screenH / 2 - 30);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        String sub = victory ? "Enemy Nexus Destroyed!" : "Your Nexus has Fallen!";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (screenW - sw) / 2, screenH / 2 + 15);

        g.setColor(new Color(255, 215, 0));
        String restartStr = "Press [SPACE] or Click to Play Again";
        int rw = g.getFontMetrics().stringWidth(restartStr);
        g.drawString(restartStr, (screenW - rw) / 2, screenH / 2 + 65);
    }

    private void renderKillBanner(Graphics2D g, int screenW, int screenH, GameEngine engine) {
        GameEngine.KillEvent banner = engine.getActiveBanner();
        if (banner == null || banner.timer <= 0) return;

        // Banner appears immediately at full opacity, then fades out smoothly in the last 0.4s
        float alpha = 1.0f;
        if (banner.timer < 0.4f) {
            alpha = banner.timer / 0.4f;
        }
        alpha = Math.max(0.1f, Math.min(1.0f, alpha));

        int bannerW = 460;
        int bannerH = 52;
        int bannerX = (screenW - bannerW) / 2;
        int bannerY = 82; // Positioned cleanly below scoreboard and K/D/A stats

        Composite origComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Dark banner shadow
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(bannerX + 3, bannerY + 3, bannerW, bannerH, 12, 12);

        // Dark banner background
        Color bg = banner.bannerBgColor;
        g.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 245));
        g.fillRoundRect(bannerX, bannerY, bannerW, bannerH, 12, 12);

        // Glowing colored outline
        g.setColor(banner.titleColor);
        g.setStroke(new BasicStroke(2.2f));
        g.drawRoundRect(bannerX, bannerY, bannerW, bannerH, 12, 12);
        g.setStroke(new BasicStroke(1.0f));

        // Header Title (e.g. "YOU HAVE SLAIN AN ENEMY!" or "YOU HAVE BEEN SLAIN!")
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(banner.titleColor);
        int tw = g.getFontMetrics().stringWidth(banner.title);
        g.drawString(banner.title, (screenW - tw) / 2, bannerY + 23);

        // Subtitle (Killer ⚔ Victim)
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        int sw = g.getFontMetrics().stringWidth(banner.subtitle);
        g.drawString(banner.subtitle, (screenW - sw) / 2, bannerY + 42);

        g.setComposite(origComp);
    }

    private void renderKillFeed(Graphics2D g, int screenW, GameEngine engine) {
        List<GameEngine.KillEvent> feed = engine.getKillFeed();
        if (feed == null || feed.isEmpty()) return;

        int feedX = screenW - 225;
        int feedY = 110;
        int itemH = 22;
        int itemW = 210;

        Composite origComp = g.getComposite();

        for (int i = 0; i < Math.min(4, feed.size()); i++) {
            GameEngine.KillEvent ke = feed.get(i);
            int cy = feedY + i * (itemH + 4);

            float alpha = 1.0f;
            if (ke.timer < 0) {
                alpha = Math.max(0.1f, 1.0f + (ke.timer / 4.0f));
            }
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // Background pill
            g.setColor(new Color(15, 18, 24, 215));
            g.fillRoundRect(feedX, cy, itemW, itemH, 6, 6);
            g.setColor(new Color(65, 70, 85));
            g.drawRoundRect(feedX, cy, itemW, itemH, 6, 6);

            // Draw killer, sword icon, victim
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            boolean killerIsBlue = ke.killer.equals("Steve") || ke.killer.equals("Alex") || ke.killer.equals("Evoker") || ke.killer.equals("Blue Team");
            g.setColor(killerIsBlue ? new Color(60, 190, 255) : new Color(255, 75, 75));
            g.drawString(ke.killer, feedX + 8, cy + 15);

            int kw = g.getFontMetrics().stringWidth(ke.killer);
            g.setColor(new Color(255, 215, 0));
            g.drawString(" ⚔ ", feedX + 8 + kw, cy + 15);
            int sw = g.getFontMetrics().stringWidth(" ⚔ ");

            boolean victimIsBlue = ke.victim.equals("Steve") || ke.victim.equals("Alex") || ke.victim.equals("Evoker") || ke.victim.contains("Blue");
            g.setColor(victimIsBlue ? new Color(60, 190, 255) : new Color(255, 75, 75));
            g.drawString(ke.victim, feedX + 8 + kw + sw, cy + 15);
        }

        g.setComposite(origComp);
    }
}
