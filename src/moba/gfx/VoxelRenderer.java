package moba.gfx;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance isometric voxel renderer with Minecraft aesthetic.
 * Pre-bakes 16x16 pixel art textures into 2.5D isometric block sprites.
 */
public class VoxelRenderer {
    public static final int TILE_WIDTH = 44;
    public static final int TILE_HEIGHT = 22;
    public static final int BLOCK_HEIGHT = 18;

    private static final Map<String, BufferedImage> blockSprites = new HashMap<>();

    static {
        bakeBlockSprites();
    }

    public static Point worldToScreen(float wx, float wy, float wz, float camX, float camY) {
        int sx = (int) ((wx - wy) * (TILE_WIDTH / 2f) - camX);
        int sy = (int) ((wx + wy) * (TILE_HEIGHT / 2f) - (wz * BLOCK_HEIGHT) - camY);
        return new Point(sx, sy);
    }

    public static Point2D_Float screenToWorld(int sx, int sy, float camX, float camY) {
        float adjX = sx + camX;
        float adjY = sy + camY;
        float halfW = TILE_WIDTH / 2f;
        float halfH = TILE_HEIGHT / 2f;

        float wx = (adjX / halfW + adjY / halfH) / 2f;
        float wy = (adjY / halfH - adjX / halfW) / 2f;
        return new Point2D_Float(wx, wy);
    }

    public static class Point2D_Float {
        public float x, y;
        public Point2D_Float(float x, float y) { this.x = x; this.y = y; }
    }

    private static void bakeBlockSprites() {
        String[] types = {
            TextureAtlas.GRASS_TOP,
            TextureAtlas.DIRT,
            TextureAtlas.STONE_BRICK,
            TextureAtlas.COBBLESTONE,
            TextureAtlas.WATER,
            TextureAtlas.OAK_WOOD,
            TextureAtlas.OAK_LEAVES,
            TextureAtlas.OBSIDIAN,
            TextureAtlas.REDSTONE_BLOCK,
            TextureAtlas.BLUE_CRYSTAL,
            TextureAtlas.RED_CRYSTAL,
            TextureAtlas.NETHER_BRICK,
            TextureAtlas.SAND
        };

        for (String type : types) {
            blockSprites.put(type, renderBlockSprite(type));
        }
    }

    private static BufferedImage renderBlockSprite(String type) {
        int w = TILE_WIDTH;
        int h = TILE_HEIGHT + BLOCK_HEIGHT;
        BufferedImage sprite = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sprite.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        BufferedImage topTex = TextureAtlas.get(type);
        BufferedImage sideTex = type.equals(TextureAtlas.GRASS_TOP) ? TextureAtlas.get(TextureAtlas.GRASS_SIDE) : topTex;

        int halfW = TILE_WIDTH / 2;
        int halfH = TILE_HEIGHT / 2;

        // 1. Draw Top Face (Isometric diamond)
        int texSize = TextureAtlas.TILE_SIZE;
        for (int py = 0; py < TILE_HEIGHT; py++) {
            float normY = (float) py / TILE_HEIGHT; // 0 to 1
            // Diamond span at py:
            float span = normY <= 0.5f ? (normY * 2f) : ((1f - normY) * 2f);
            int startX = Math.max(0, (int) (halfW - span * halfW));
            int endX = Math.min(w - 1, (int) (halfW + span * halfW));

            for (int px = startX; px <= endX; px++) {
                if (px < 0 || px >= w || py < 0 || py >= h) continue;
                // UV mapping
                float u = (float) (px - halfW) / TILE_WIDTH + normY;
                float v = normY - (float) (px - halfW) / TILE_WIDTH;
                int tx = Math.max(0, Math.min(texSize - 1, (int) (u * texSize)));
                int ty = Math.max(0, Math.min(texSize - 1, (int) (v * texSize)));

                int rgb = topTex.getRGB(tx, ty);
                sprite.setRGB(px, py, rgb);
            }
        }

        // 2. Draw Left Face (Shaded 82% brightness)
        for (int dy = 0; dy < BLOCK_HEIGHT; dy++) {
            for (int px = 0; px < halfW; px++) {
                float normX = (float) px / halfW;
                int py = (int) (halfH + normX * halfH + dy);
                if (px >= 0 && px < w && py >= 0 && py < h) {
                    int tx = Math.max(0, Math.min(texSize - 1, (int) (normX * texSize)));
                    int ty = Math.max(0, Math.min(texSize - 1, (int) ((float) dy / BLOCK_HEIGHT * texSize)));
                    Color c = new Color(sideTex.getRGB(tx, ty), true);
                    int r = (int) (c.getRed() * 0.82f);
                    int gc = (int) (c.getGreen() * 0.82f);
                    int b = (int) (c.getBlue() * 0.82f);
                    sprite.setRGB(px, py, new Color(r, gc, b, c.getAlpha()).getRGB());
                }
            }
        }

        // 3. Draw Right Face (Shaded 65% brightness)
        for (int dy = 0; dy < BLOCK_HEIGHT; dy++) {
            for (int px = halfW; px < w; px++) {
                float normX = (float) (px - halfW) / halfW;
                int py = (int) (TILE_HEIGHT - normX * halfH + dy);
                if (px >= 0 && px < w && py >= 0 && py < h) {
                    int tx = Math.max(0, Math.min(texSize - 1, (int) (normX * texSize)));
                    int ty = Math.max(0, Math.min(texSize - 1, (int) ((float) dy / BLOCK_HEIGHT * texSize)));
                    Color c = new Color(sideTex.getRGB(tx, ty), true);
                    int r = (int) (c.getRed() * 0.65f);
                    int gc = (int) (c.getGreen() * 0.65f);
                    int b = (int) (c.getBlue() * 0.65f);
                    sprite.setRGB(px, py, new Color(r, gc, b, c.getAlpha()).getRGB());
                }
            }
        }

        // Outline borders for voxel definition
        g.setColor(new Color(0, 0, 0, 50));
        // Diamond border
        g.drawLine(halfW, 0, w - 1, halfH);
        g.drawLine(w - 1, halfH, halfW, TILE_HEIGHT - 1);
        g.drawLine(halfW, TILE_HEIGHT - 1, 0, halfH);
        g.drawLine(0, halfH, halfW, 0);
        // Vertical edges
        g.drawLine(0, halfH, 0, halfH + BLOCK_HEIGHT);
        g.drawLine(halfW, TILE_HEIGHT - 1, halfW, h - 1);
        g.drawLine(w - 1, halfH, w - 1, halfH + BLOCK_HEIGHT);
        // Bottom edges
        g.drawLine(0, halfH + BLOCK_HEIGHT, halfW, h - 1);
        g.drawLine(halfW, h - 1, w - 1, halfH + BLOCK_HEIGHT);

        g.dispose();
        return sprite;
    }

    public static void drawBlock(Graphics2D g, String type, int screenX, int screenY) {
        BufferedImage sprite = blockSprites.getOrDefault(type, blockSprites.get(TextureAtlas.DIRT));
        if (sprite != null) {
            g.drawImage(sprite, screenX - TILE_WIDTH / 2, screenY, null);
        }
    }

    /**
     * Draws Minecraft voxel hero model tailored to specific hero archetype.
     */
    public static void drawVoxelHero(Graphics2D g, int sx, int sy, String roleName, boolean isEnemy, float walkAnim, float facingAngle, int teamColor) {
        // Base shadow circle
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(sx - 14, sy - 6, 28, 12);

        float swing = (float) Math.sin(walkAnim) * 5f;

        if ("Alex".equalsIgnoreCase(roleName)) {
            // Alex: Green tunic, brown pants, orange hair, bow
            Color pants = new Color(75, 55, 35);
            g.setColor(pants);
            g.fillRect(sx - 6, sy - 14 + (int)swing, 5, 14);
            g.fillRect(sx + 1, sy - 14 - (int)swing, 5, 14);

            Color tunic = new Color(85, 130, 45);
            g.setColor(tunic);
            g.fillRect(sx - 7, sy - 28, 14, 15);
            g.setColor(tunic.darker());
            g.drawRect(sx - 7, sy - 28, 14, 15);

            Color skin = new Color(230, 185, 150);
            g.setColor(skin);
            g.fillRect(sx - 11, sy - 24 - (int)swing, 4, 10);
            g.fillRect(sx + 7, sy - 24 + (int)swing, 4, 10);

            // Bow
            drawBow(g, sx + 9, sy - 24 + (int)swing);

            // Head
            g.setColor(skin);
            g.fillRect(sx - 6, sy - 42, 12, 14);
            // Orange Hair with side braid
            Color orangeHair = new Color(215, 110, 30);
            g.setColor(orangeHair);
            g.fillRect(sx - 6, sy - 42, 12, 5);
            g.fillRect(sx - 8, sy - 38, 2, 8); // Hair braid
            // Green eyes
            g.setColor(new Color(50, 160, 70));
            g.fillRect(sx - 4, sy - 36, 2, 2);
            g.fillRect(sx + 2, sy - 36, 2, 2);

        } else if ("Evoker".equalsIgnoreCase(roleName)) {
            // Evoker: Dark robe, gold trim, pale head, magic staff
            Color robe = new Color(45, 48, 55);
            g.setColor(robe);
            g.fillRect(sx - 8, sy - 28, 16, 28);
            // Gold trim
            g.setColor(new Color(230, 190, 40));
            g.fillRect(sx - 2, sy - 28, 4, 28);
            g.drawRect(sx - 8, sy - 28, 16, 28);

            Color paleSkin = new Color(175, 175, 175);
            // Magic Staff
            drawStaff(g, sx + 8, sy - 30);

            // Head (Villager brow & big nose)
            g.setColor(paleSkin);
            g.fillRect(sx - 6, sy - 44, 12, 16);
            g.setColor(new Color(150, 150, 150));
            g.fillRect(sx - 2, sy - 36, 4, 7); // Nose
            // Green glowing eyes
            g.setColor(new Color(40, 220, 120));
            g.fillRect(sx - 4, sy - 38, 2, 2);
            g.fillRect(sx + 2, sy - 38, 2, 2);

        } else if ("Piglin Brute".equalsIgnoreCase(roleName)) {
            // Piglin Brute: Golden armor, tusks, battleaxe
            Color pigSkin = new Color(210, 150, 115);
            Color goldArmor = new Color(220, 175, 40);

            g.setColor(new Color(50, 30, 20));
            g.fillRect(sx - 8, sy - 14 + (int)swing, 6, 14);
            g.fillRect(sx + 2, sy - 14 - (int)swing, 6, 14);

            g.setColor(goldArmor);
            g.fillRect(sx - 9, sy - 29, 18, 16);
            g.setColor(Color.BLACK);
            g.drawRect(sx - 9, sy - 29, 18, 16);

            // Battleaxe
            drawBattleaxe(g, sx + 10, sy - 28 + (int)swing);

            // Head with tusks and ears
            g.setColor(pigSkin);
            g.fillRect(sx - 8, sy - 44, 16, 15);
            // Floppy ears
            g.fillRect(sx - 11, sy - 42, 3, 8);
            g.fillRect(sx + 8, sy - 42, 3, 8);
            // White Tusks
            g.setColor(Color.WHITE);
            g.fillRect(sx - 6, sy - 34, 2, 4);
            g.fillRect(sx + 4, sy - 34, 2, 4);
            // Red eyes
            g.setColor(new Color(220, 30, 30));
            g.fillRect(sx - 5, sy - 39, 2, 2);
            g.fillRect(sx + 3, sy - 39, 2, 2);

        } else if ("Wither Skeletor".equalsIgnoreCase(roleName)) {
            // Wither Skeleton: Obsidian black bones, purple aura, dark sword
            Color witherBone = new Color(30, 30, 35);
            g.setColor(witherBone);
            g.fillRect(sx - 6, sy - 16 + (int)swing, 4, 16);
            g.fillRect(sx + 2, sy - 16 - (int)swing, 4, 16);
            g.fillRect(sx - 7, sy - 32, 14, 16);

            drawSword(g, sx + 8, sy - 30 + (int)swing, true);

            // Skull
            g.setColor(witherBone);
            g.fillRect(sx - 7, sy - 48, 14, 16);
            // Purple flame eyes
            g.setColor(new Color(180, 50, 255));
            g.fillRect(sx - 5, sy - 42, 3, 3);
            g.fillRect(sx + 2, sy - 42, 3, 3);

        } else if ("Iron Golem".equalsIgnoreCase(roleName)) {
            // Iron Golem Tank: Massive iron body, vines, red poppy
            Color iron = new Color(210, 205, 195);
            Color vine = new Color(75, 145, 55);
            g.setColor(iron);
            g.fillRect(sx - 7, sy - 16 + (int)swing, 6, 16);
            g.fillRect(sx + 2, sy - 16 - (int)swing, 6, 16);
            g.fillRect(sx - 9, sy - 34, 18, 19);
            g.setColor(vine);
            g.fillRect(sx - 4, sy - 30, 3, 10);
            g.setColor(iron);
            g.fillRect(sx - 13, sy - 32 - (int)swing, 4, 20);
            g.fillRect(sx + 9, sy - 32 + (int)swing, 4, 20);
            // Red Poppy / Rose
            g.setColor(new Color(230, 35, 35));
            g.fillRect(sx + 10, sy - 36 + (int)swing, 4, 4);
            // Head
            g.setColor(iron);
            g.fillRect(sx - 7, sy - 48, 14, 14);
            g.setColor(new Color(180, 170, 160));
            g.fillRect(sx - 2, sy - 42, 4, 6);
            g.setColor(new Color(220, 40, 40));
            g.fillRect(sx - 5, sy - 44, 2, 2);
            g.fillRect(sx + 3, sy - 44, 2, 2);

        } else if ("Nether Knight".equalsIgnoreCase(roleName)) {
            // Nether Knight Assassin: Dark Netherite armor, red crimson cape, flame blade
            Color netherite = new Color(40, 36, 42);
            Color crimson = new Color(185, 30, 45);
            g.setColor(netherite);
            g.fillRect(sx - 6, sy - 14 + (int)swing, 5, 14);
            g.fillRect(sx + 1, sy - 14 - (int)swing, 5, 14);
            // Crimson Cape
            g.setColor(crimson);
            g.fillRect(sx - 8, sy - 28, 16, 18);
            // Chestplate
            g.setColor(netherite);
            g.fillRect(sx - 7, sy - 28, 14, 14);
            drawSword(g, sx + 9, sy - 26 + (int)swing, true);
            // Helmet
            g.setColor(netherite);
            g.fillRect(sx - 6, sy - 42, 12, 14);
            // Red Glowing Visor
            g.setColor(new Color(255, 60, 40));
            g.fillRect(sx - 4, sy - 35, 8, 3);

        } else if ("Warden".equalsIgnoreCase(roleName)) {
            // Warden Tank: Deep sculk teal, sculk horns, glowing ribcage
            Color sculk = new Color(15, 42, 52);
            Color sculkBright = new Color(30, 180, 195);
            g.setColor(sculk);
            g.fillRect(sx - 8, sy - 16 + (int)swing, 7, 16);
            g.fillRect(sx + 2, sy - 16 - (int)swing, 7, 16);
            g.fillRect(sx - 10, sy - 36, 20, 21);
            // Glowing sculk ribcage core
            g.setColor(sculkBright);
            g.fillRect(sx - 4, sy - 30, 8, 8);
            g.setColor(sculk);
            g.fillRect(sx - 14, sy - 34 - (int)swing, 5, 22);
            g.fillRect(sx + 9, sy - 34 + (int)swing, 5, 22);
            // Head with sculk horns
            g.fillRect(sx - 8, sy - 50, 16, 15);
            g.setColor(sculkBright);
            g.fillRect(sx - 12, sy - 56, 4, 10);
            g.fillRect(sx + 8, sy - 56, 4, 10);

        } else if ("Shadow Creeper".equalsIgnoreCase(roleName)) {
            // Shadow Creeper Assassin: Dark green camouflage, explosive daggers
            Color camo = new Color(25, 75, 35);
            Color dark = new Color(15, 45, 20);
            g.setColor(camo);
            g.fillRect(sx - 6, sy - 14 + (int)swing, 5, 14);
            g.fillRect(sx + 1, sy - 14 - (int)swing, 5, 14);
            g.fillRect(sx - 7, sy - 28, 14, 15);
            g.setColor(dark);
            g.fillRect(sx - 4, sy - 25, 8, 8);
            drawSword(g, sx + 8, sy - 26 + (int)swing, true);
            // Head
            g.setColor(camo);
            g.fillRect(sx - 6, sy - 42, 12, 14);
            // Creeper face
            g.setColor(Color.BLACK);
            g.fillRect(sx - 4, sy - 38, 2, 2);
            g.fillRect(sx + 2, sy - 38, 2, 2);
            g.fillRect(sx - 2, sy - 35, 4, 4);

        } else {
            // Default Steve / Herobrine model
            Color legColor = isEnemy ? new Color(50, 20, 20) : new Color(40, 45, 95);
            Color legShade = isEnemy ? new Color(30, 10, 10) : new Color(25, 30, 65);

            g.setColor(legColor);
            g.fillRect(sx - 7, sy - 14 + (int)swing, 5, 14);
            g.setColor(legShade);
            g.drawRect(sx - 7, sy - 14 + (int)swing, 5, 14);

            g.setColor(legColor);
            g.fillRect(sx + 2, sy - 14 - (int)swing, 5, 14);
            g.setColor(legShade);
            g.drawRect(sx + 2, sy - 14 - (int)swing, 5, 14);

            Color shirtColor = isEnemy ? new Color(180, 40, 40) : new Color(0, 150, 160);
            Color shirtShade = isEnemy ? new Color(120, 25, 25) : new Color(0, 105, 115);
            g.setColor(shirtColor);
            g.fillRect(sx - 8, sy - 28, 16, 15);
            g.setColor(shirtShade);
            g.drawRect(sx - 8, sy - 28, 16, 15);

            Color skinColor = isEnemy ? new Color(160, 120, 110) : new Color(220, 170, 140);
            g.setColor(shirtColor);
            g.fillRect(sx - 12, sy - 28 - (int)swing, 4, 6);
            g.setColor(skinColor);
            g.fillRect(sx - 12, sy - 22 - (int)swing, 4, 8);

            g.setColor(shirtColor);
            g.fillRect(sx + 8, sy - 28 + (int)swing, 4, 6);
            g.setColor(skinColor);
            g.fillRect(sx + 8, sy - 22 + (int)swing, 4, 8);

            drawSword(g, sx + 10, sy - 26 + (int)swing, isEnemy);

            g.setColor(skinColor);
            g.fillRect(sx - 7, sy - 42, 14, 14);
            g.setColor(skinColor.darker());
            g.drawRect(sx - 7, sy - 42, 14, 14);

            Color hairColor = isEnemy ? new Color(20, 15, 15) : new Color(75, 45, 20);
            g.setColor(hairColor);
            g.fillRect(sx - 7, sy - 42, 14, 4);
            g.fillRect(sx - 7, sy - 38, 2, 4);
            g.fillRect(sx + 5, sy - 38, 2, 4);

            if (isEnemy) {
                g.setColor(new Color(255, 255, 255, 240));
                g.fillRect(sx - 4, sy - 36, 3, 2);
                g.fillRect(sx + 2, sy - 36, 3, 2);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(sx - 5, sy - 36, 3, 2);
                g.fillRect(sx + 2, sy - 36, 3, 2);
                g.setColor(new Color(60, 80, 180));
                g.fillRect(sx - 4, sy - 36, 2, 2);
                g.fillRect(sx + 2, sy - 36, 2, 2);
            }
        }
    }

    public static void drawVoxelHero(Graphics2D g, int sx, int sy, boolean isEnemy, float walkAnim, float facingAngle, int teamColor) {
        drawVoxelHero(g, sx, sy, isEnemy ? "Herobrine" : "Steve", isEnemy, walkAnim, facingAngle, teamColor);
    }

    private static void drawBow(Graphics2D g, int x, int y) {
        g.setColor(new Color(139, 69, 19));
        g.drawLine(x, y - 12, x + 6, y);
        g.drawLine(x + 6, y, x, y + 12);
        g.setColor(Color.WHITE);
        g.drawLine(x, y - 12, x, y + 12);
    }

    private static void drawStaff(Graphics2D g, int x, int y) {
        g.setColor(new Color(110, 80, 50));
        g.fillRect(x + 2, y - 12, 3, 22);
        // Crystal orb on staff
        g.setColor(new Color(40, 220, 240));
        g.fillOval(x, y - 18, 7, 7);
        g.setColor(Color.WHITE);
        g.drawOval(x, y - 18, 7, 7);
    }

    private static void drawBattleaxe(Graphics2D g, int x, int y) {
        g.setColor(new Color(110, 80, 50));
        g.fillRect(x + 2, y - 14, 3, 24);
        // Golden axe head
        g.setColor(new Color(240, 200, 30));
        g.fillRect(x + 5, y - 14, 6, 8);
        g.fillRect(x - 3, y - 14, 5, 8);
        g.setColor(Color.WHITE);
        g.drawRect(x + 5, y - 14, 6, 8);
    }

    private static void drawSword(Graphics2D g, int x, int y, boolean isEnemy) {
        Color bladeColor = isEnemy ? new Color(255, 60, 60) : new Color(40, 240, 230);
        Color hiltColor = new Color(139, 69, 19);

        g.setColor(bladeColor);
        g.fillRect(x + 2, y - 14, 3, 14);
        g.setColor(bladeColor.brighter());
        g.fillRect(x + 3, y - 14, 1, 14);

        g.setColor(new Color(218, 165, 32));
        g.fillRect(x, y - 1, 7, 2);

        g.setColor(hiltColor);
        g.fillRect(x + 2, y + 1, 3, 4);
    }

    /**
     * Draws Minecraft Minion (Friendly Zombie/Golem or Enemy Skeleton)
     */
    public static void drawMinion(Graphics2D g, int sx, int sy, boolean isEnemy, boolean isRanged, float walkAnim) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(sx - 10, sy - 5, 20, 10);

        float swing = (float) Math.sin(walkAnim) * 4f;

        if (isEnemy) {
            // Skeleton Archer (Grey/White bone)
            Color bone = new Color(200, 200, 200);
            Color boneDark = new Color(140, 140, 140);

            // Legs
            g.setColor(bone);
            g.fillRect(sx - 5, sy - 10 + (int)swing, 3, 10);
            g.fillRect(sx + 2, sy - 10 - (int)swing, 3, 10);

            // Ribcage/Torso
            g.fillRect(sx - 5, sy - 20, 10, 10);
            g.setColor(boneDark);
            g.drawLine(sx - 5, sy - 17, sx + 4, sy - 17);
            g.drawLine(sx - 5, sy - 14, sx + 4, sy - 14);

            // Arms (Holding bow forward)
            g.setColor(bone);
            g.fillRect(sx + 4, sy - 19, 7, 3);
            // Bow
            g.setColor(new Color(139, 69, 19));
            g.drawLine(sx + 10, sy - 24, sx + 13, sy - 18);
            g.drawLine(sx + 13, sy - 18, sx + 10, sy - 12);
            g.setColor(Color.WHITE);
            g.drawLine(sx + 10, sy - 24, sx + 10, sy - 12);

            // Skull
            g.setColor(bone);
            g.fillRect(sx - 6, sy - 31, 12, 11);
            g.setColor(Color.BLACK);
            g.fillRect(sx - 4, sy - 27, 3, 3); // Eye socket
            g.fillRect(sx + 1, sy - 27, 3, 3);
            g.fillRect(sx - 2, sy - 23, 4, 2); // Mouth
        } else {
            // Friendly Zombie Swordsman (Green skin, cyan shirt, purple pants)
            Color skin = new Color(80, 130, 60);
            Color shirt = new Color(0, 150, 160);
            Color pants = new Color(60, 40, 90);

            // Legs
            g.setColor(pants);
            g.fillRect(sx - 6, sy - 11 + (int)swing, 4, 11);
            g.fillRect(sx + 2, sy - 11 - (int)swing, 4, 11);

            // Torso
            g.setColor(shirt);
            g.fillRect(sx - 6, sy - 22, 12, 11);

            // Arms raised forward (classic Minecraft Zombie pose)
            g.setColor(skin);
            g.fillRect(sx + 4, sy - 21, 8, 4);
            // Iron Sword
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(sx + 10, sy - 28, 3, 11);

            // Head
            g.setColor(skin);
            g.fillRect(sx - 5, sy - 32, 11, 10);
            g.setColor(Color.BLACK);
            g.fillRect(sx - 3, sy - 28, 2, 2);
            g.fillRect(sx + 1, sy - 28, 2, 2);
        }
    }

    /**
     * Draws Tower / Turret (Minecraft Obsidian/Stone obelisk with glowing crystal)
     */
    public static void drawTower(Graphics2D g, int sx, int sy, boolean isEnemy, float pulseAnim) {
        // Shadow
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(sx - 24, sy - 10, 48, 20);

        // Obsidian Base Platform
        g.setColor(new Color(30, 20, 45));
        g.fillRect(sx - 18, sy - 12, 36, 14);
        g.setColor(new Color(50, 35, 75));
        g.drawRect(sx - 18, sy - 12, 36, 14);

        // Stone Brick Tower Pillar
        g.setColor(new Color(110, 110, 110));
        g.fillRect(sx - 12, sy - 52, 24, 42);
        g.setColor(new Color(70, 70, 70));
        g.drawRect(sx - 12, sy - 52, 24, 42);
        // Mortar lines
        g.drawLine(sx - 12, sy - 42, sx + 11, sy - 42);
        g.drawLine(sx - 12, sy - 32, sx + 11, sy - 32);
        g.drawLine(sx - 12, sy - 22, sx + 11, sy - 22);

        // Battlements Top
        g.setColor(new Color(90, 90, 90));
        g.fillRect(sx - 15, sy - 58, 30, 7);
        g.fillRect(sx - 15, sy - 63, 6, 6);
        g.fillRect(sx + 9, sy - 63, 6, 6);

        // Floating glowing crystal at top
        float bob = (float) Math.sin(pulseAnim) * 4f;
        Color crystalColor = isEnemy ? new Color(255, 40, 40) : new Color(40, 190, 255);
        Color crystalGlow = isEnemy ? new Color(255, 60, 60, 90) : new Color(60, 210, 255, 90);

        int cy = (int) (sy - 72 + bob);
        // Glow halo
        g.setColor(crystalGlow);
        g.fillOval(sx - 16, cy - 16, 32, 32);

        // Crystal Diamond
        Polygon poly = new Polygon();
        poly.addPoint(sx, cy - 10);
        poly.addPoint(sx + 8, cy);
        poly.addPoint(sx, cy + 10);
        poly.addPoint(sx - 8, cy);
        g.setColor(crystalColor);
        g.fillPolygon(poly);
        g.setColor(Color.WHITE);
        g.drawPolygon(poly);
    }

    /**
     * Draws Base Core / Nexus (Ancient Nether/Ender Core with glowing rings)
     */
    public static void drawBaseCore(Graphics2D g, int sx, int sy, boolean isEnemy, float pulseAnim) {
        // Base altar
        g.setColor(new Color(0, 0, 0, 140));
        g.fillOval(sx - 36, sy - 14, 72, 28);

        // Large Obsidian Pedestal
        g.setColor(new Color(25, 18, 38));
        g.fillRect(sx - 28, sy - 20, 56, 22);
        g.setColor(new Color(60, 45, 90));
        g.drawRect(sx - 28, sy - 20, 56, 22);

        // Gold / Netherite trim
        g.setColor(isEnemy ? new Color(210, 60, 40) : new Color(255, 215, 0));
        g.fillRect(sx - 24, sy - 24, 48, 5);

        // Floating Massive Core Crystal
        float bob = (float) Math.sin(pulseAnim * 1.5f) * 6f;
        int cy = (int) (sy - 50 + bob);

        Color coreColor = isEnemy ? new Color(255, 30, 30) : new Color(30, 180, 255);
        Color aura = isEnemy ? new Color(255, 50, 50, 80) : new Color(50, 180, 255, 80);

        // Aura
        g.setColor(aura);
        g.fillOval(sx - 26, cy - 26, 52, 52);

        // Voxel Crystal Octahedron
        Polygon poly = new Polygon();
        poly.addPoint(sx, cy - 20);
        poly.addPoint(sx + 16, cy);
        poly.addPoint(sx, cy + 20);
        poly.addPoint(sx - 16, cy);
        g.setColor(coreColor);
        g.fillPolygon(poly);

        g.setColor(Color.WHITE);
        g.drawPolygon(poly);
        g.drawLine(sx, cy - 20, sx, cy + 20);
        g.drawLine(sx - 16, cy, sx + 16, cy);
    }

    /**
     * Draws Neutral Jungle Boss (Ender Dragon / Golem in pit)
     */
    public static void drawJungleBoss(Graphics2D g, int sx, int sy, float anim) {
        g.setColor(new Color(0, 0, 0, 130));
        g.fillOval(sx - 28, sy - 10, 56, 20);

        // Iron Golem / Titan
        Color iron = new Color(210, 205, 195);
        Color vine = new Color(70, 140, 50);

        // Thick legs
        g.setColor(iron);
        g.fillRect(sx - 14, sy - 16, 9, 16);
        g.fillRect(sx + 5, sy - 16, 9, 16);

        // Huge Body
        g.fillRect(sx - 20, sy - 44, 40, 28);
        g.setColor(vine);
        g.fillRect(sx - 10, sy - 38, 4, 12); // Vines on chest

        // Long Arms
        g.setColor(iron);
        g.fillRect(sx - 28, sy - 40, 8, 30);
        g.fillRect(sx + 20, sy - 40, 8, 30);

        // Head with long villager nose
        g.fillRect(sx - 9, sy - 60, 18, 16);
        g.setColor(new Color(190, 180, 170));
        g.fillRect(sx - 3, sy - 52, 6, 9); // Nose

        // Red glowing eyes
        g.setColor(new Color(255, 30, 30));
        g.fillRect(sx - 7, sy - 56, 3, 3);
        g.fillRect(sx + 4, sy - 56, 3, 3);
    }

    /**
     * Draws Minecraft 3D waving tall grass (Bush for stealth & ambushes)
     * Extra tall (40-56px high) and dense to fully conceal heroes
     */
    public static void drawTallGrass(Graphics2D g, int sx, int sy, float anim) {
        int[] bladeOffsets = {-16, -12, -7, -2, 3, 8, 12, 16};
        int[] heights = {42, 50, 56, 52, 48, 54, 46, 38};
        Color baseDark = new Color(24, 85, 20);
        Color midGreen = new Color(38, 135, 32);
        Color tipGreen = new Color(90, 215, 50);

        for (int i = 0; i < bladeOffsets.length; i++) {
            int bx = sx + bladeOffsets[i];
            int sway = (int) (Math.sin(anim * 2.2f + i * 1.2f) * 3.5f);
            int h = heights[i];

            // Base stalk
            g.setColor(baseDark);
            g.fillRect(bx, sy - h, 4, h);
            // Mid stalk highlight
            g.setColor(midGreen);
            g.fillRect(bx + 1, sy - h + 8, 2, h - 14);
            // Swaying bright foliage tip
            g.setColor(tipGreen);
            g.fillRect(bx + sway, sy - h, 4, 8);
        }
    }

    /**
     * Draws front layer of tall grass overlapping the lower body of heroes standing inside
     */
    public static void drawTallGrassFront(Graphics2D g, int sx, int sy, float anim) {
        int[] frontOffsets = {-12, -5, 2, 9};
        int[] frontHeights = {32, 38, 36, 30};
        Color midGreen = new Color(35, 125, 30);
        Color tipGreen = new Color(95, 225, 55);

        for (int i = 0; i < frontOffsets.length; i++) {
            int bx = sx + frontOffsets[i];
            int sway = (int) (Math.sin(anim * 2.4f + i * 1.5f) * 2.8f);
            int h = frontHeights[i];

            g.setColor(midGreen);
            g.fillRect(bx, sy - h, 4, h);
            g.setColor(tipGreen);
            g.fillRect(bx + sway, sy - h, 4, 7);
        }
    }

    /**
     * Draws River Turtle Boss (Giant Ancient Minecraft Dragon Turtle with massive rune shell)
     * Colossally sized (80px wide shell, 114px flipper spread)
     */
    public static void drawTurtleBoss(Graphics2D g, int sx, int sy, float anim) {
        // Deep monster shadow
        g.setColor(new Color(0, 0, 0, 140));
        g.fillOval(sx - 44, sy - 14, 88, 28);

        float flipperSwing = (float) Math.sin(anim * 2.5f) * 6f;
        Color flipperDark = new Color(30, 100, 50);
        Color flipperBright = new Color(55, 165, 80);

        // 4 Massive Dragon Flippers
        // Front flippers
        g.setColor(flipperDark);
        g.fillRect(sx - 46, (int)(sy - 20 + flipperSwing), 18, 10);
        g.fillRect(sx + 28, (int)(sy - 20 - flipperSwing), 18, 10);
        g.setColor(flipperBright);
        g.fillRect(sx - 44, (int)(sy - 18 + flipperSwing), 14, 6);
        g.fillRect(sx + 30, (int)(sy - 18 - flipperSwing), 14, 6);

        // Rear flippers
        g.setColor(flipperDark);
        g.fillRect(sx - 38, (int)(sy + 2 - flipperSwing), 14, 8);
        g.fillRect(sx + 24, (int)(sy + 2 + flipperSwing), 14, 8);

        // Colossal Dragon Turtle Carapace
        Color shellBase = new Color(24, 75, 36);
        Color shellPlate = new Color(55, 145, 65);
        Color shellHighlight = new Color(85, 195, 95);
        Color shellGold = new Color(240, 205, 50);
        Color cyanRune = new Color(60, 230, 255);

        // Base shell
        g.setColor(shellBase);
        g.fillRoundRect(sx - 38, sy - 46, 76, 46, 18, 18);
        // Elevated center plates
        g.setColor(shellPlate);
        g.fillRoundRect(sx - 28, sy - 42, 56, 38, 12, 12);
        g.setColor(shellHighlight);
        g.fillRect(sx - 18, sy - 36, 36, 26);

        // Golden rune trim
        g.setColor(shellGold);
        g.drawRoundRect(sx - 38, sy - 46, 76, 46, 18, 18);
        g.drawRoundRect(sx - 28, sy - 42, 56, 38, 12, 12);

        // Glowing Ancient Aqua Runes
        g.setColor(cyanRune);
        g.fillRect(sx - 10, sy - 32, 20, 4);
        g.fillRect(sx - 2, sy - 32, 4, 18);
        g.fillRect(sx - 12, sy - 20, 24, 4);

        // Spiked Dragon Head
        Color headColor = new Color(45, 150, 70);
        g.setColor(headColor);
        g.fillRect(sx - 12, sy - 60, 24, 20);
        // Dragon Snout
        g.fillRect(sx - 8, sy - 66, 16, 8);

        // Dragon Horns
        g.setColor(shellGold);
        g.fillRect(sx - 16, sy - 68, 5, 12);
        g.fillRect(sx + 11, sy - 68, 5, 12);

        // Glowing Aqua Dragon Eyes
        g.setColor(cyanRune);
        g.fillRect(sx - 8, sy - 56, 4, 4);
        g.fillRect(sx + 4, sy - 56, 4, 4);
        g.setColor(Color.WHITE);
        g.fillRect(sx - 7, sy - 55, 2, 2);
        g.fillRect(sx + 5, sy - 55, 2, 2);
    }

    /**
     * Draws Colossal Lord Boss (Towering Nether Titan with horn crown, 135px height)
     */
    public static void drawLordBoss(Graphics2D g, int sx, int sy, float anim, boolean isBlueTeam) {
        // Massive titan shadow
        g.setColor(new Color(0, 0, 0, 160));
        g.fillOval(sx - 52, sy - 16, 104, 32);

        Color armor = isBlueTeam ? new Color(25, 40, 70) : new Color(38, 18, 22);
        Color armorHighlight = isBlueTeam ? new Color(45, 75, 125) : new Color(68, 30, 38);
        Color trim = isBlueTeam ? new Color(60, 210, 255) : new Color(255, 120, 30);
        Color aura = isBlueTeam ? new Color(40, 180, 255, 70) : new Color(255, 60, 30, 70);

        // Colossal radiant energy aura
        g.setColor(aura);
        g.fillOval(sx - 55, sy - 120, 110, 120);

        float stomp = (float) Math.sin(anim * 2.2f) * 5f;

        // Massive Titan Legs (sy - 36 to sy)
        g.setColor(armor);
        g.fillRect(sx - 26, (int)(sy - 36 + stomp), 18, 36);
        g.fillRect(sx + 8, (int)(sy - 36 - stomp), 18, 36);
        g.setColor(armorHighlight);
        g.fillRect(sx - 24, (int)(sy - 34 + stomp), 14, 16);
        g.fillRect(sx + 10, (int)(sy - 34 - stomp), 14, 16);
        g.setColor(trim);
        g.drawRect(sx - 26, (int)(sy - 36 + stomp), 18, 36);
        g.drawRect(sx + 8, (int)(sy - 36 - stomp), 18, 36);

        // Titanic Netherite Chest & Shoulders (sy - 86 to sy - 36, width 76px)
        g.setColor(armor);
        g.fillRect(sx - 38, sy - 86, 76, 52);
        g.setColor(armorHighlight);
        g.fillRect(sx - 26, sy - 80, 52, 40);
        // Glowing Chestplate Runes
        g.setColor(trim);
        g.fillRect(sx - 18, sy - 72, 36, 14);
        g.drawRect(sx - 38, sy - 86, 76, 52);

        // Massive Left Arm
        g.setColor(armor);
        g.fillRect(sx - 52, (int)(sy - 82 - stomp), 15, 52);
        g.setColor(trim);
        g.fillRect(sx - 54, (int)(sy - 86 - stomp), 18, 14); // Spiked pauldron

        // Massive Right Arm with Colossal War Club
        g.setColor(armor);
        g.fillRect(sx + 37, (int)(sy - 82 + stomp), 15, 52);
        g.setColor(trim);
        g.fillRect(sx + 36, (int)(sy - 86 + stomp), 18, 14); // Spiked pauldron

        // Gigantic Nether War Club (65px long)
        g.setColor(new Color(30, 20, 25));
        g.fillRect(sx + 48, (int)(sy - 98 + stomp), 14, 66);
        g.setColor(trim);
        g.fillRect(sx + 44, (int)(sy - 108 + stomp), 22, 22); // Fiery Spiked Hammerhead
        g.setColor(Color.WHITE);
        g.fillRect(sx + 50, (int)(sy - 102 + stomp), 10, 10);

        // Towering Nether Crown Helmet (sy - 120 to sy - 86)
        g.setColor(armor);
        g.fillRect(sx - 18, sy - 114, 36, 30);
        g.setColor(armorHighlight);
        g.fillRect(sx - 14, sy - 110, 28, 22);

        // Giant Curved Nether Horns
        g.setColor(trim);
        g.fillRect(sx - 28, sy - 128, 10, 24);
        g.fillRect(sx - 24, sy - 134, 8, 10);
        g.fillRect(sx + 18, sy - 128, 10, 24);
        g.fillRect(sx + 16, sy - 134, 8, 10);

        // Colossal Blazing Eyes
        Color eyeColor = isBlueTeam ? new Color(120, 245, 255) : new Color(255, 45, 45);
        g.setColor(eyeColor);
        g.fillRect(sx - 12, sy - 104, 7, 6);
        g.fillRect(sx + 5, sy - 104, 7, 6);
        g.setColor(Color.WHITE);
        g.fillRect(sx - 10, sy - 102, 3, 3);
        g.fillRect(sx + 7, sy - 102, 3, 3);
    }

    /**
     * Draws Blue Buff Monster (Lapis Golem with glowing cyan runes and crystal fists)
     */
    public static void drawBlueBuffMonster(Graphics2D g, int sx, int sy, float anim) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(sx - 20, sy - 8, 40, 16);

        float bounce = (float) Math.sin(anim * 3f) * 3f;
        Color stone = new Color(60, 70, 90);
        Color lapis = new Color(30, 100, 220);
        Color rune = new Color(80, 220, 255);

        // Legs
        g.setColor(stone);
        g.fillRect(sx - 12, (int)(sy - 16 + bounce), 8, 16);
        g.fillRect(sx + 4, (int)(sy - 16 - bounce), 8, 16);

        // Body & Lapis Core
        g.setColor(stone);
        g.fillRect(sx - 16, sy - 40, 32, 26);
        g.setColor(lapis);
        g.fillRect(sx - 10, sy - 36, 20, 18);
        g.setColor(rune);
        g.fillRect(sx - 6, sy - 32, 12, 10);

        // Heavy crystal fists
        g.setColor(stone);
        g.fillRect(sx - 24, (int)(sy - 34 - bounce), 8, 22);
        g.fillRect(sx + 16, (int)(sy - 34 + bounce), 8, 22);
        g.setColor(rune);
        g.fillRect(sx - 25, (int)(sy - 18 - bounce), 10, 10);
        g.fillRect(sx + 15, (int)(sy - 18 + bounce), 10, 10);

        // Head with glowing eyes
        g.setColor(stone);
        g.fillRect(sx - 8, sy - 52, 16, 13);
        g.setColor(rune);
        g.fillRect(sx - 6, sy - 48, 3, 3);
        g.fillRect(sx + 3, sy - 48, 3, 3);
    }

    /**
     * Draws Red Buff Monster (Crimson Beast / Magma Fiend with magma core & horns)
     */
    public static void drawRedBuffMonster(Graphics2D g, int sx, int sy, float anim) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(sx - 20, sy - 8, 40, 16);

        float bounce = (float) Math.sin(anim * 3.5f) * 3f;
        Color nether = new Color(70, 20, 25);
        Color magma = new Color(220, 70, 20);
        Color flame = new Color(255, 200, 40);

        // Clawed legs
        g.setColor(nether);
        g.fillRect(sx - 13, (int)(sy - 16 + bounce), 8, 16);
        g.fillRect(sx + 5, (int)(sy - 16 - bounce), 8, 16);

        // Fiery Torso
        g.setColor(nether);
        g.fillRect(sx - 16, sy - 40, 32, 26);
        g.setColor(magma);
        g.fillRect(sx - 10, sy - 36, 20, 18);
        g.setColor(flame);
        g.fillRect(sx - 5, sy - 31, 10, 8);

        // Blazing arms
        g.setColor(nether);
        g.fillRect(sx - 24, (int)(sy - 35 - bounce), 8, 22);
        g.fillRect(sx + 16, (int)(sy - 35 + bounce), 8, 22);
        g.setColor(flame);
        g.fillRect(sx - 25, (int)(sy - 19 - bounce), 10, 10);
        g.fillRect(sx + 15, (int)(sy - 19 + bounce), 10, 10);

        // Horned Beast Head
        g.setColor(nether);
        g.fillRect(sx - 9, sy - 53, 18, 14);
        // Lava Horns
        g.setColor(magma);
        g.fillRect(sx - 14, sy - 60, 6, 9);
        g.fillRect(sx + 8, sy - 60, 6, 9);
        // Blazing flame eyes
        g.setColor(flame);
        g.fillRect(sx - 6, sy - 48, 4, 3);
        g.fillRect(sx + 2, sy - 48, 4, 3);
    }

    /**
     * Draws magical rotating rune circles beneath hero feet when carrying Blue / Red buffs
     */
    public static void drawHeroBuffRings(Graphics2D g, int sx, int sy, float anim, boolean hasBlueBuff, boolean hasRedBuff) {
        if (!hasBlueBuff && !hasRedBuff) return;

        if (hasBlueBuff) {
            g.setColor(new Color(60, 180, 255, 120));
            g.drawOval(sx - 18, sy - 6, 36, 12);
            // 3 rotating orbs
            for (int i = 0; i < 3; i++) {
                double a = anim * 2.8 + (Math.PI * 2 / 3) * i;
                int ox = sx + (int)(Math.cos(a) * 18);
                int oy = sy + (int)(Math.sin(a) * 6);
                g.setColor(new Color(100, 220, 255, 230));
                g.fillRect(ox - 2, oy - 2, 4, 4);
            }
        }

        if (hasRedBuff) {
            g.setColor(new Color(255, 80, 30, 120));
            g.drawOval(sx - 22, sy - 8, 44, 15);
            // 4 rotating magma embers
            for (int i = 0; i < 4; i++) {
                double a = -anim * 3.2 + (Math.PI * 2 / 4) * i;
                int ox = sx + (int)(Math.cos(a) * 22);
                int oy = sy + (int)(Math.sin(a) * 7.5);
                g.setColor(new Color(255, 180, 40, 240));
                g.fillRect(ox - 2, oy - 2, 5, 5);
            }
        }
    }
}
