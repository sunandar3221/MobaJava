package moba.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates and stores 16x16 authentic pixel-art Minecraft-style textures.
 */
public class TextureAtlas {
    public static final int TILE_SIZE = 16;
    private static final Map<String, BufferedImage> textures = new HashMap<>();

    public static final String GRASS_TOP = "grass_top";
    public static final String GRASS_SIDE = "grass_side";
    public static final String DIRT = "dirt";
    public static final String STONE_BRICK = "stone_brick";
    public static final String COBBLESTONE = "cobblestone";
    public static final String WATER = "water";
    public static final String OAK_WOOD = "oak_wood";
    public static final String OAK_LEAVES = "oak_leaves";
    public static final String OBSIDIAN = "obsidian";
    public static final String REDSTONE_BLOCK = "redstone_block";
    public static final String BLUE_CRYSTAL = "blue_crystal";
    public static final String RED_CRYSTAL = "red_crystal";
    public static final String NETHER_BRICK = "nether_brick";
    public static final String SAND = "sand";

    static {
        generateTextures();
    }

    public static BufferedImage get(String name) {
        return textures.getOrDefault(name, textures.get(DIRT));
    }

    private static void generateTextures() {
        Random rand = new Random(42);

        // 1. Dirt Texture (16x16)
        BufferedImage dirt = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 120 + rand.nextInt(25);
                int g = 80 + rand.nextInt(20);
                int b = 50 + rand.nextInt(15);
                dirt.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(DIRT, dirt);

        // 2. Grass Top Texture
        BufferedImage grassTop = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 70 + rand.nextInt(35);
                int g = 145 + rand.nextInt(45);
                int b = 45 + rand.nextInt(25);
                grassTop.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(GRASS_TOP, grassTop);

        // 3. Grass Side Texture (Top 4px green grass fringe, bottom dirt)
        BufferedImage grassSide = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int fringeDepth = 3 + (x % 3 == 0 ? 1 : 0) + (x % 5 == 0 ? 2 : 0);
                if (y <= fringeDepth) {
                    int r = 70 + rand.nextInt(35);
                    int g = 140 + rand.nextInt(45);
                    int b = 45 + rand.nextInt(25);
                    grassSide.setRGB(x, y, new Color(r, g, b).getRGB());
                } else {
                    grassSide.setRGB(x, y, dirt.getRGB(x, y));
                }
            }
        }
        textures.put(GRASS_SIDE, grassSide);

        // 4. Stone Brick Texture
        BufferedImage stoneBrick = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int val = 120 + rand.nextInt(20);
                // Mortar lines every 4 pixels vertically, and staggered horizontally
                boolean isMortar = (y % 4 == 0);
                if ((y / 4) % 2 == 0) {
                    if (x % 8 == 0) isMortar = true;
                } else {
                    if ((x + 4) % 8 == 0) isMortar = true;
                }
                if (isMortar) {
                    val = 65 + rand.nextInt(15);
                }
                stoneBrick.setRGB(x, y, new Color(val, val, val).getRGB());
            }
        }
        textures.put(STONE_BRICK, stoneBrick);

        // 5. Cobblestone Texture
        BufferedImage cobblestone = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int base = 100 + rand.nextInt(40);
                if ((x + y * 3) % 5 == 0 || (x * 2 + y) % 7 == 0) {
                    base -= 35; // Crevice
                }
                base = Math.max(30, Math.min(220, base));
                cobblestone.setRGB(x, y, new Color(base, base, base + 2).getRGB());
            }
        }
        textures.put(COBBLESTONE, cobblestone);

        // 6. Water Texture (Translucent blue ripples)
        BufferedImage water = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int b = 180 + rand.nextInt(40);
                int g = 90 + rand.nextInt(30);
                int r = 35 + rand.nextInt(20);
                water.setRGB(x, y, new Color(r, g, b, 210).getRGB());
            }
        }
        textures.put(WATER, water);

        // 7. Oak Wood Log
        BufferedImage oakWood = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 100 + (x % 3 == 0 ? 15 : 0) + rand.nextInt(15);
                int g = 70 + rand.nextInt(10);
                int b = 40 + rand.nextInt(10);
                oakWood.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(OAK_WOOD, oakWood);

        // 8. Oak Leaves
        BufferedImage oakLeaves = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                if (rand.nextInt(10) == 0) {
                    oakLeaves.setRGB(x, y, new Color(0, 0, 0, 0).getRGB()); // Pixel gap
                } else {
                    int r = 30 + rand.nextInt(20);
                    int g = 100 + rand.nextInt(45);
                    int b = 25 + rand.nextInt(20);
                    oakLeaves.setRGB(x, y, new Color(r, g, b, 240).getRGB());
                }
            }
        }
        textures.put(OAK_LEAVES, oakLeaves);

        // 9. Obsidian
        BufferedImage obsidian = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 25 + rand.nextInt(15);
                int g = 15 + rand.nextInt(12);
                int b = 40 + rand.nextInt(25);
                if (rand.nextInt(8) == 0) { // Purple flecks
                    r += 30;
                    b += 45;
                }
                obsidian.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(OBSIDIAN, obsidian);

        // 10. Redstone / Red Crystal
        BufferedImage redCrystal = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 190 + rand.nextInt(60);
                int g = 20 + rand.nextInt(25);
                int b = 20 + rand.nextInt(25);
                redCrystal.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(RED_CRYSTAL, redCrystal);
        textures.put(REDSTONE_BLOCK, redCrystal);

        // 11. Blue Crystal / Diamond Block
        BufferedImage blueCrystal = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 40 + rand.nextInt(30);
                int g = 180 + rand.nextInt(55);
                int b = 220 + rand.nextInt(35);
                blueCrystal.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(BLUE_CRYSTAL, blueCrystal);

        // 12. Nether Brick (for Red team side)
        BufferedImage netherBrick = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 85 + rand.nextInt(30);
                int g = 25 + rand.nextInt(15);
                int b = 30 + rand.nextInt(15);
                if (y % 4 == 0 || (y / 4 % 2 == 0 ? x % 8 == 0 : (x + 4) % 8 == 0)) {
                    r = 45; g = 12; b = 15;
                }
                netherBrick.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(NETHER_BRICK, netherBrick);

        // 13. Sand
        BufferedImage sand = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < TILE_SIZE; y++) {
            for (int x = 0; x < TILE_SIZE; x++) {
                int r = 215 + rand.nextInt(25);
                int g = 195 + rand.nextInt(20);
                int b = 140 + rand.nextInt(20);
                sand.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        textures.put(SAND, sand);
    }
}
