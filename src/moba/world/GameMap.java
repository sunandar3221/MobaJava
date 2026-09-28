package moba.world;

import moba.gfx.TextureAtlas;
import moba.gfx.VoxelRenderer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class GameMap {
    public static final int MAP_WIDTH = 50;
    public static final int MAP_HEIGHT = 50;

    // Tile layers
    private final String[][] groundTiles = new String[MAP_WIDTH][MAP_HEIGHT];
    private final int[][] elevation = new int[MAP_WIDTH][MAP_HEIGHT];
    private final boolean[][] solid = new boolean[MAP_WIDTH][MAP_HEIGHT];
    private final boolean[][] bush = new boolean[MAP_WIDTH][MAP_HEIGHT];
    private final List<Point> bushPositions = new ArrayList<>();
    private final List<Point3D> decorations = new ArrayList<>();

    public static class Point3D {
        public int x, y, z;
        public String type;
        public Point3D(int x, int y, int z, String type) {
            this.x = x; this.y = y; this.z = z; this.type = type;
        }
    }

    public GameMap() {
        generateMap();
    }

    private void generateMap() {
        int centerAxis = (MAP_WIDTH + MAP_HEIGHT) / 2; // 50

        // 1. Base terrain (Grass for Blue side, Nether Brick for Red side)
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (x + y > 64) {
                    groundTiles[x][y] = TextureAtlas.NETHER_BRICK;
                } else {
                    groundTiles[x][y] = TextureAtlas.GRASS_TOP;
                }
                elevation[x][y] = 0;
                solid[x][y] = false;
            }
        }

        // 2. River crossing perpendicular to Mid Lane (x + y ≈ centerAxis)
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                int distToRiverAxis = Math.abs((x + y) - centerAxis);
                if (distToRiverAxis <= 2) {
                    groundTiles[x][y] = TextureAtlas.WATER;
                    elevation[x][y] = -1; // Sunken riverbed
                } else if (distToRiverAxis == 3) {
                    groundTiles[x][y] = TextureAtlas.SAND; // Sandy river bank
                }
            }
        }

        // 3. Top Lane: (6, 6) -> (6, 43) -> (43, 43)
        // Segment A: along x = 6, y from 6 to 43
        for (int y = 5; y <= 43; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                setLaneTile(6 + dx, y);
            }
        }
        // Segment B: along y = 43, x from 6 to 43
        for (int x = 5; x <= 43; x++) {
            for (int dy = -1; dy <= 1; dy++) {
                setLaneTile(x, 43 + dy);
            }
        }

        // 4. Mid Lane: Diagonal (5, 5) to (44, 44)
        for (int i = 4; i <= 45; i++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    setLaneTile(i + dx, i + dy);
                }
            }
        }

        // 5. Bottom Lane: (6, 6) -> (43, 6) -> (43, 43)
        // Segment A: along y = 6, x from 6 to 43
        for (int x = 5; x <= 43; x++) {
            for (int dy = -1; dy <= 1; dy++) {
                setLaneTile(x, 6 + dy);
            }
        }
        // Segment B: along x = 43, y from 6 to 43
        for (int y = 5; y <= 43; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                setLaneTile(43 + dx, y);
            }
        }

        // 6. Base Plazas
        // Blue Base Plaza around (6, 6)
        for (int x = 3; x <= 9; x++) {
            for (int y = 3; y <= 9; y++) {
                groundTiles[x][y] = TextureAtlas.COBBLESTONE;
                elevation[x][y] = 0;
            }
        }
        // Red Base Plaza around (42, 42)
        for (int x = 39; x <= 45; x++) {
            for (int y = 39; y <= 45; y++) {
                groundTiles[x][y] = TextureAtlas.NETHER_BRICK;
                elevation[x][y] = 0;
            }
        }

        // 7. Outer Border Walls
        for (int x = 0; x < MAP_WIDTH; x++) {
            setWall(x, 0);
            setWall(x, 1);
            setWall(x, MAP_HEIGHT - 2);
            setWall(x, MAP_HEIGHT - 1);
        }
        for (int y = 0; y < MAP_HEIGHT; y++) {
            setWall(0, y);
            setWall(1, y);
            setWall(MAP_WIDTH - 2, y);
            setWall(MAP_WIDTH - 1, y);
        }

        // 8. Jungle Trees (placed between lanes, away from boss pits)
        int[][] treeClusters = {
            // Upper Jungle (between Top & Mid Lane)
            {12, 28}, {15, 34}, {20, 38}, {14, 22}, {18, 25}, {26, 38}, {30, 36},
            // Lower Jungle (between Mid & Bot Lane)
            {28, 12}, {34, 15}, {38, 20}, {22, 14}, {25, 18}, {38, 26}, {36, 30},
            // Corner Woods
            {10, 42}, {42, 10}
        };

        for (int[] pos : treeClusters) {
            placeTree(pos[0], pos[1]);
        }

        // 9. Strategic Bushes (Rumput panjang untuk sembunyi & culik di sungai & lane)
        int[][] bushClusters = {
            // Mid Lane river entrances (long continuous river bushes)
            {21, 26}, {22, 26}, {23, 26}, {23, 27}, {24, 27}, {24, 28},
            {26, 22}, {26, 23}, {27, 23}, {27, 24}, {28, 24}, {29, 24},
            // Top / EXP Lane river & turret approaches (long lane edge bushes)
            {6, 33}, {7, 33}, {7, 34}, {8, 34}, {8, 35}, {9, 35},
            {16, 42}, {17, 42}, {18, 42}, {19, 42}, {20, 42},
            // Bot / Gold Lane river & turret approaches (long lane edge bushes)
            {33, 6}, {33, 7}, {34, 7}, {34, 8}, {35, 8}, {35, 9},
            {42, 16}, {42, 17}, {42, 18}, {42, 19}, {42, 20},
            // River Boss Pit entrances & crescent thicket (Turtle / Lord)
            {15, 28}, {16, 28}, {17, 28}, {18, 28}, {19, 28},
            {30, 22}, {31, 22}, {32, 22}, {33, 22}, {34, 22},
            // River center ambush corridors
            {20, 30}, {21, 30}, {29, 20}, {30, 20},
            // Blue & Red Jungle Buff bushes
            {10, 19}, {11, 19}, {12, 19}, {13, 19},
            {37, 31}, {38, 31}, {39, 31}, {40, 31}
        };

        for (int[] bp : bushClusters) {
            placeBush(bp[0], bp[1]);
        }
    }

    private void setLaneTile(int x, int y) {
        if (!isInBounds(x, y)) return;
        groundTiles[x][y] = TextureAtlas.STONE_BRICK;
        elevation[x][y] = 0; // Stone bridge over riverbed
    }

    private void setWall(int x, int y) {
        if (!isInBounds(x, y)) return;
        groundTiles[x][y] = TextureAtlas.COBBLESTONE;
        elevation[x][y] = 1;
        solid[x][y] = true;
    }

    private void placeTree(int x, int y) {
        if (!isInBounds(x, y) || isSolid(x, y) || isLaneTile(x, y)) return;
        solid[x][y] = true;
        decorations.add(new Point3D(x, y, 0, TextureAtlas.OAK_WOOD));
        decorations.add(new Point3D(x, y, 1, TextureAtlas.OAK_WOOD));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int tx = x + dx;
                int ty = y + dy;
                if (isInBounds(tx, ty) && !isLaneTile(tx, ty)) {
                    decorations.add(new Point3D(tx, ty, 2, TextureAtlas.OAK_LEAVES));
                }
            }
        }
        decorations.add(new Point3D(x, y, 3, TextureAtlas.OAK_LEAVES));
    }

    public boolean isLaneTile(int x, int y) {
        if (!isInBounds(x, y)) return false;
        return TextureAtlas.STONE_BRICK.equals(groundTiles[x][y]) || TextureAtlas.COBBLESTONE.equals(groundTiles[x][y]);
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT;
    }

    public boolean isSolid(int x, int y) {
        if (!isInBounds(x, y)) return true;
        return solid[x][y];
    }

    public boolean isWalkable(float x, float y) {
        int gx = Math.round(x);
        int gy = Math.round(y);
        return isInBounds(gx, gy) && !solid[gx][gy];
    }

    public String getTile(int x, int y) {
        if (!isInBounds(x, y)) return TextureAtlas.COBBLESTONE;
        return groundTiles[x][y];
    }

    public int getElevation(int x, int y) {
        if (!isInBounds(x, y)) return 0;
        return elevation[x][y];
    }

    public List<Point3D> getDecorations() {
        return decorations;
    }

    public void renderGround(Graphics2D g, float camX, float camY, int viewMinX, int viewMinY, int viewMaxX, int viewMaxY) {
        renderGround(g, camX, camY, viewMinX, viewMinY, viewMaxX, viewMaxY, 1024, 720);
    }

    public void renderGround(Graphics2D g, float camX, float camY, int viewMinX, int viewMinY, int viewMaxX, int viewMaxY, int screenW, int screenH) {
        int minGX = Math.max(0, viewMinX);
        int maxGX = Math.min(MAP_WIDTH - 1, viewMaxX);
        int minGY = Math.max(0, viewMinY);
        int maxGY = Math.min(MAP_HEIGHT - 1, viewMaxY);

        int marginX = VoxelRenderer.TILE_WIDTH;
        int marginY = VoxelRenderer.TILE_HEIGHT * 2;

        for (int sum = minGX + minGY; sum <= maxGX + maxGY; sum++) {
            for (int x = minGX; x <= maxGX; x++) {
                int y = sum - x;
                if (y >= minGY && y <= maxGY) {
                    Point sp = VoxelRenderer.worldToScreen(x, y, elevation[x][y], camX, camY);
                    // Fast screen frustum culling: skip drawing blocks outside screen boundaries
                    if (sp.x >= -marginX && sp.x <= screenW + marginX &&
                        sp.y >= -marginY && sp.y <= screenH + marginY) {
                        VoxelRenderer.drawBlock(g, groundTiles[x][y], sp.x, sp.y);
                    }
                }
            }
        }
    }

    private void placeBush(int x, int y) {
        if (!isInBounds(x, y) || isSolid(x, y)) return;
        bush[x][y] = true;
        bushPositions.add(new Point(x, y));
    }

    public boolean isBush(float x, float y) {
        int gx = Math.round(x);
        int gy = Math.round(y);
        if (isInBounds(gx, gy) && bush[gx][gy]) return true;
        for (Point bp : bushPositions) {
            float dx = bp.x - x;
            float dy = bp.y - y;
            if (dx * dx + dy * dy <= 0.70f * 0.70f) return true;
        }
        return false;
    }

    public List<Point> getBushPositions() {
        return bushPositions;
    }

    public void renderBushes(Graphics2D g, float camX, float camY, float anim, int screenW, int screenH) {
        int marginX = VoxelRenderer.TILE_WIDTH;
        int marginY = VoxelRenderer.TILE_HEIGHT * 2;
        for (Point bp : bushPositions) {
            Point sp = VoxelRenderer.worldToScreen(bp.x, bp.y, elevation[bp.x][bp.y], camX, camY);
            if (sp.x >= -marginX && sp.x <= screenW + marginX &&
                sp.y >= -marginY && sp.y <= screenH + marginY) {
                VoxelRenderer.drawTallGrass(g, sp.x, sp.y, anim);
            }
        }
    }
}
