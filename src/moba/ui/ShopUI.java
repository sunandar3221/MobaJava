package moba.ui;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.model.Hero;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ShopUI {
    public static class ShopItem {
        public String name;
        public String description;
        public int cost;
        public float bonusDmg;
        public float bonusHp;
        public float bonusSpeed;
        public float bonusRegen;
        public float bonusArmor = 0f;
        public Color iconColor;

        public ShopItem(String name, String desc, int cost, float dmg, float hp, float speed, float regen, float armor, Color color) {
            this.name = name;
            this.description = desc;
            this.cost = cost;
            this.bonusDmg = dmg;
            this.bonusHp = hp;
            this.bonusSpeed = speed;
            this.bonusRegen = regen;
            this.bonusArmor = armor;
            this.iconColor = color;
        }

        public ShopItem(String name, String desc, int cost, float dmg, float hp, float speed, float regen, Color color) {
            this(name, desc, cost, dmg, hp, speed, regen, 0f, color);
        }
    }

    public static final List<ShopItem> ITEMS = new ArrayList<>();
    public boolean isOpen = false;

    static {
        ITEMS.add(new ShopItem("Diamond Sword", "+35 Attack Damage", 1100, 35, 0, 0, 0, 0, new Color(40, 240, 230)));
        ITEMS.add(new ShopItem("Netherite Armor", "+400 HP, +15 Armor", 1300, 0, 400, 0, 0, 15, new Color(70, 60, 65)));
        ITEMS.add(new ShopItem("Golden Apple", "+10 HP Regen/sec", 750, 0, 100, 0, 10, 0, new Color(255, 215, 0)));
        ITEMS.add(new ShopItem("Swift Boots", "+25% Move Speed", 600, 5, 0, 1.1f, 0, 0, new Color(139, 69, 19)));
        ITEMS.add(new ShopItem("Infinity Bow", "+25 Damage, +15% Speed", 1250, 25, 0, 0.6f, 0, 0, new Color(200, 160, 80)));
        ITEMS.add(new ShopItem("Totem of Undying", "+200 HP, +15 Damage", 1500, 15, 200, 0, 5, 10, new Color(120, 220, 80)));
    }

    public void toggle() {
        isOpen = !isOpen;
    }

    public void handleClick(int mx, int my, Hero player, GameEngine engine) {
        if (!isOpen) return;

        int panelW = 460;
        int panelH = 340;
        int panelX = (engine.getWidth() - panelW) / 2;
        int panelY = (engine.getHeight() - panelH) / 2;

        // Close button (X)
        if (mx >= panelX + panelW - 32 && mx <= panelX + panelW - 8 && my >= panelY + 8 && my <= panelY + 32) {
            isOpen = false;
            return;
        }

        // Check buy buttons
        int itemH = 46;
        for (int i = 0; i < ITEMS.size(); i++) {
            int itemY = panelY + 45 + i * (itemH + 4);
            int buyBtnX = panelX + panelW - 90;
            int buyBtnY = itemY + 8;
            int buyBtnW = 75;
            int buyBtnH = 28;

            if (mx >= buyBtnX && mx <= buyBtnX + buyBtnW && my >= buyBtnY && my <= buyBtnY + buyBtnH) {
                buyItem(i, player, engine);
                break;
            }
        }
    }

    public void buyItem(int index, Hero player, GameEngine engine) {
        if (index < 0 || index >= ITEMS.size()) return;
        ShopItem item = ITEMS.get(index);

        if (player.gold >= item.cost && player.inventory.size() < 6) {
            player.gold -= item.cost;
            player.inventory.add(item.name);
            player.bonusDamage += item.bonusDmg;
            player.bonusArmor += item.bonusArmor;
            player.bonusHp += item.bonusHp;
            player.moveSpeed += item.bonusSpeed;
            player.hpRegen += item.bonusRegen;
            player.hp += item.bonusHp; // Immediate HP boost
            SoundManager.playLevelUp();
        }
    }

    public void render(Graphics2D g, int screenW, int screenH, Hero player) {
        if (!isOpen) return;

        int panelW = 460;
        int panelH = 350;
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelH) / 2;

        // Dark modal overlay
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, screenW, screenH);

        // Minecraft GUI Frame
        g.setColor(new Color(198, 198, 198));
        g.fillRect(panelX, panelY, panelW, panelH);

        // 3D Bevel borders
        g.setColor(Color.WHITE);
        g.drawLine(panelX, panelY, panelX + panelW, panelY);
        g.drawLine(panelX, panelY, panelX, panelY + panelH);
        g.setColor(new Color(85, 85, 85));
        g.drawLine(panelX, panelY + panelH, panelX + panelW, panelY + panelH);
        g.drawLine(panelX + panelW, panelY, panelX + panelW, panelY + panelH);

        // Header Title
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(new Color(40, 40, 40));
        g.drawString("MINECRAFT ITEM SHOP", panelX + 16, panelY + 28);

        // Close button 'X'
        g.setColor(new Color(170, 50, 50));
        g.fillRect(panelX + panelW - 30, panelY + 10, 20, 20);
        g.setColor(Color.WHITE);
        g.drawString("X", panelX + panelW - 25, panelY + 26);

        // Player Emerald Gold count
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(0, 140, 40));
        g.drawString("Emeralds: " + player.gold, panelX + panelW - 170, panelY + 28);

        // Item Rows
        int itemH = 46;
        for (int i = 0; i < ITEMS.size(); i++) {
            ShopItem item = ITEMS.get(i);
            int rowY = panelY + 42 + i * (itemH + 4);

            // Row background slot
            g.setColor(new Color(140, 140, 140));
            g.fillRect(panelX + 12, rowY, panelW - 24, itemH);
            g.setColor(new Color(60, 60, 60));
            g.drawRect(panelX + 12, rowY, panelW - 24, itemH);

            // Item Icon Box
            g.setColor(new Color(50, 50, 50));
            g.fillRect(panelX + 16, rowY + 4, 38, 38);
            g.setColor(item.iconColor);
            g.fillRect(panelX + 21, rowY + 9, 28, 28);
            g.setColor(Color.WHITE);
            g.drawRect(panelX + 21, rowY + 9, 28, 28);

            // Item Name & Description
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(Color.BLACK);
            g.drawString(item.name, panelX + 62, rowY + 18);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(40, 40, 40));
            g.drawString(item.description, panelX + 62, rowY + 34);

            // Buy Button
            int buyBtnX = panelX + panelW - 95;
            int buyBtnY = rowY + 9;
            int buyBtnW = 75;
            int buyBtnH = 28;

            boolean canAfford = player.gold >= item.cost && player.inventory.size() < 6;
            g.setColor(canAfford ? new Color(60, 140, 60) : new Color(110, 110, 110));
            g.fillRect(buyBtnX, buyBtnY, buyBtnW, buyBtnH);
            g.setColor(Color.WHITE);
            g.drawRect(buyBtnX, buyBtnY, buyBtnW, buyBtnH);

            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            String costStr = item.cost + " Em";
            int cw = g.getFontMetrics().stringWidth(costStr);
            g.drawString(costStr, buyBtnX + (buyBtnW - cw) / 2, buyBtnY + 18);
        }
    }
}
