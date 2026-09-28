package moba;

import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;
import moba.model.*;
import moba.ui.HUD;
import moba.ui.ShopUI;
import moba.world.GameMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEngine implements KeyListener, MouseListener, MouseMotionListener {
    public static class KillEvent {
        public final String killer;
        public final String victim;
        public final String title;
        public final String subtitle;
        public final Color titleColor;
        public final Color bannerBgColor;
        public float timer;
        public final float maxTimer;
        public final boolean isPlayerInvolved;

        public KillEvent(String killer, String victim, String title, String subtitle, Color titleColor, Color bannerBgColor, float duration, boolean isPlayerInvolved) {
            this.killer = killer;
            this.victim = victim;
            this.title = title;
            this.subtitle = subtitle;
            this.titleColor = titleColor;
            this.bannerBgColor = bannerBgColor;
            this.timer = duration;
            this.maxTimer = duration;
            this.isPlayerInvolved = isPlayerInvolved;
        }
    }

    private static final int PERIMETER_SEGMENTS = 24;
    private static final int[] PERIM_REL_X = new int[PERIMETER_SEGMENTS];
    private static final int[] PERIM_REL_Y = new int[PERIMETER_SEGMENTS];
    private static final BasicStroke STROKE_DANGER = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_RING = new BasicStroke(1.5f);
    private static final BasicStroke STROKE_NORMAL = new BasicStroke(1.0f);
    private static final Font FONT_DANGER = new Font("Monospaced", Font.BOLD, 13);

    static {
        float r = 8.5f; // Tower attack range
        for (int i = 0; i < PERIMETER_SEGMENTS; i++) {
            double angle = (2 * Math.PI / PERIMETER_SEGMENTS) * i;
            float cosA = (float) Math.cos(angle);
            float sinA = (float) Math.sin(angle);
            PERIM_REL_X[i] = (int) (r * (cosA - sinA) * (VoxelRenderer.TILE_WIDTH / 2f));
            PERIM_REL_Y[i] = (int) (r * (cosA + sinA) * (VoxelRenderer.TILE_HEIGHT / 2f));
        }
    }

    private int width = 1024;
    private int height = 720;

    private GameMap map;
    private Hero playerHero;
    private Hero enemyHero;
    private BaseCore blueCore;
    private BaseCore redCore;
    private JungleCamp jungleBoss;

    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private final List<Particle> particles = new CopyOnWriteArrayList<>();
    private final List<KillEvent> killFeed = new CopyOnWriteArrayList<>();
    private KillEvent activeBanner = null;

    private final HUD hud = new HUD();
    private final ShopUI shop = new ShopUI();

    // Camera
    private float cameraX = 0;
    private float cameraY = 0;

    // Match timing
    private float matchTime = 0f;
    private float minionWaveTimer = 0f;
    private static final float WAVE_INTERVAL = 22.0f;

    public enum GameState {
        MAIN_MENU,
        LANE_SELECT,
        MATCHMAKING,
        DRAFT_PICK,
        PLAYING,
        GAME_OVER
    }

    // Game state
    private GameState state = GameState.MAIN_MENU;
    private final moba.ui.MenuUI menuUI = new moba.ui.MenuUI();
    private boolean gameOver = false;
    private boolean playerVictory = false;

    // Mouse coordinates
    private int mouseX = 0;
    private int mouseY = 0;

    // Move click marker
    private Point moveMarker = null;
    private float moveMarkerTimer = 0f;

    private final List<Hero> blueHeroes = new CopyOnWriteArrayList<>();
    private final List<Hero> redHeroes = new CopyOnWriteArrayList<>();
    private final List<BuffMonster> buffMonsters = new CopyOnWriteArrayList<>();

    public GameEngine(int w, int h) {
        this.width = w;
        this.height = h;
        initMatch();
    }

    public void initMatch() {
        startMatchWithDraft("Steve", Hero.Lane.EXP);
        state = GameState.MAIN_MENU;
    }

    public void startMatchDirectly() {
        startMatchWithDraft("Steve", Hero.Lane.EXP);
        state = GameState.PLAYING;
    }

    public void startMatchWithDraft(String heroName, Hero.Lane chosenLane) {
        entities.clear();
        projectiles.clear();
        particles.clear();
        blueHeroes.clear();
        redHeroes.clear();
        buffMonsters.clear();
        killFeed.clear();
        activeBanner = null;
        matchTime = 0f;
        minionWaveTimer = 1.0f; // First wave spawns after 1s
        gameOver = false;
        playerVictory = false;

        map = new GameMap();

        // 5 Standard Roles: EXP, MID, GOLD, ROAM, JUNGLE
        Hero.Lane[] lanes = { Hero.Lane.EXP, Hero.Lane.MID, Hero.Lane.GOLD, Hero.Lane.ROAM, Hero.Lane.JUNGLE };
        String[] defaultBlueNames = { "Steve", "Evoker", "Alex", "Iron Golem", "Nether Knight" };
        float[][] blueSpawns = {
                {6.0f, 6.0f},
                {8.0f, 6.0f},
                {6.0f, 8.0f},
                {7.5f, 7.5f},
                {8.0f, 8.0f}
        };

        // Populate Blue Team (Player + 4 Allies)
        for (int i = 0; i < 5; i++) {
            Hero.Lane lane = lanes[i];
            if (lane == chosenLane) {
                playerHero = new Hero(heroName, blueSpawns[i][0], blueSpawns[i][1], Entity.TEAM_BLUE, true, chosenLane);
                blueHeroes.add(playerHero);
            } else {
                String allyName = defaultBlueNames[i];
                if (allyName.equals(heroName)) {
                    // Swapped with the hero of chosenLane
                    for (int j = 0; j < 5; j++) {
                        if (lanes[j] == chosenLane) {
                            allyName = defaultBlueNames[j];
                            break;
                        }
                    }
                }
                Hero ally = new Hero(allyName, blueSpawns[i][0], blueSpawns[i][1], Entity.TEAM_BLUE, false, lane);
                blueHeroes.add(ally);
            }
        }
        entities.addAll(blueHeroes);

        // Populate Red Team (5 Heroes across 5 Roles)
        enemyHero = new Hero("Herobrine", 43.0f, 43.0f, Entity.TEAM_RED, false, Hero.Lane.MID);
        Hero piglin = new Hero("Piglin Brute", 41.0f, 43.0f, Entity.TEAM_RED, false, Hero.Lane.EXP);
        Hero wither = new Hero("Wither Skeletor", 43.0f, 41.0f, Entity.TEAM_RED, false, Hero.Lane.GOLD);
        Hero warden = new Hero("Warden", 41.5f, 41.5f, Entity.TEAM_RED, false, Hero.Lane.ROAM);
        Hero creeper = new Hero("Shadow Creeper", 42.0f, 42.0f, Entity.TEAM_RED, false, Hero.Lane.JUNGLE);

        redHeroes.add(enemyHero);
        redHeroes.add(piglin);
        redHeroes.add(wither);
        redHeroes.add(warden);
        redHeroes.add(creeper);
        entities.addAll(redHeroes);

        // Bases / Nexus
        blueCore = new BaseCore(5.0f, 5.0f, Entity.TEAM_BLUE);
        redCore = new BaseCore(44.0f, 44.0f, Entity.TEAM_RED);
        entities.add(blueCore);
        entities.add(redCore);

        // 3-Lane Defensive Towers:
        // Top Lane Towers
        entities.add(new Tower("Blue Top Inner", 6.0f, 15.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Blue Top Outer", 6.0f, 27.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Red Top Outer", 23.0f, 43.0f, Entity.TEAM_RED));
        entities.add(new Tower("Red Top Inner", 35.0f, 43.0f, Entity.TEAM_RED));

        // Mid Lane Towers
        entities.add(new Tower("Blue Mid Inner", 12.0f, 12.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Blue Mid Outer", 20.0f, 20.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Red Mid Outer", 30.0f, 30.0f, Entity.TEAM_RED));
        entities.add(new Tower("Red Mid Inner", 38.0f, 38.0f, Entity.TEAM_RED));

        // Bot Lane Towers
        entities.add(new Tower("Blue Bot Inner", 15.0f, 6.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Blue Bot Outer", 27.0f, 6.0f, Entity.TEAM_BLUE));
        entities.add(new Tower("Red Bot Outer", 43.0f, 23.0f, Entity.TEAM_RED));
        entities.add(new Tower("Red Bot Inner", 43.0f, 35.0f, Entity.TEAM_RED));

        // River Boss (Turtle early / Lord late) in River pit
        jungleBoss = new JungleCamp(18.0f, 31.0f);
        entities.add(jungleBoss);

        // 4 Jungle Buff Monsters (Blue & Red Buff for Blue team, Blue & Red Buff for Red team)
        BuffMonster blueBuff1 = new BuffMonster(11.5f, 19.5f, BuffMonster.BuffType.BLUE);
        BuffMonster redBuff1 = new BuffMonster(18.5f, 11.5f, BuffMonster.BuffType.RED);
        BuffMonster blueBuff2 = new BuffMonster(38.5f, 30.5f, BuffMonster.BuffType.BLUE);
        BuffMonster redBuff2 = new BuffMonster(31.5f, 38.5f, BuffMonster.BuffType.RED);

        buffMonsters.add(blueBuff1);
        buffMonsters.add(redBuff1);
        buffMonsters.add(blueBuff2);
        buffMonsters.add(redBuff2);
        entities.addAll(buffMonsters);

        state = GameState.PLAYING;
        if (playerHero != null) {
            centerCameraOn(playerHero.x, playerHero.y);
        }
    }

    public void centerCameraOn(float wx, float wy) {
        Point sp = VoxelRenderer.worldToScreen(wx, wy, 0, 0, 0);
        cameraX = sp.x - width / 2f;
        cameraY = sp.y - height / 2f;
    }

    public synchronized void update(float dt) {
        if (state != GameState.PLAYING && state != GameState.GAME_OVER) {
            menuUI.update(dt, this);
            return;
        }

        if (!gameOver) {
            matchTime += dt;

            // Minion wave spawner
            minionWaveTimer -= dt;
            if (minionWaveTimer <= 0) {
                spawnMinionWave();
                minionWaveTimer = WAVE_INTERVAL;
            }
        }

        // Camera smoothly follows player hero
        if (playerHero != null && playerHero.alive) {
            Point targetSp = VoxelRenderer.worldToScreen(playerHero.x, playerHero.y, 0, 0, 0);
            float targetCamX = targetSp.x - width / 2f;
            float targetCamY = targetSp.y - height / 2f;
            cameraX += (targetCamX - cameraX) * 8f * dt;
            cameraY += (targetCamY - cameraY) * 8f * dt;
        }

        // Update entities
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity e = entities.get(i);
            e.update(dt, this);
            if (!e.alive && !(e instanceof Hero) && !(e instanceof BaseCore) && !(e instanceof JungleCamp) && !(e instanceof BuffMonster)) {
                entities.remove(i);
            }
        }

        // Entity soft collision separation with fast squared distance pre-check
        int entCount = entities.size();
        for (int i = 0; i < entCount; i++) {
            Entity e1 = entities.get(i);
            if (!e1.alive || e1 instanceof BaseCore || e1 instanceof Tower) continue;
            for (int j = i + 1; j < entCount; j++) {
                Entity e2 = entities.get(j);
                if (!e2.alive) continue;
                float minDist = e1.radius + e2.radius;
                float dx = e1.x - e2.x;
                float dy = e1.y - e2.y;
                float distSq = dx * dx + dy * dy;
                float minDistSq = minDist * minDist;
                if (distSq < minDistSq && distSq > 0.0001f) {
                    float d = (float) Math.sqrt(distSq);
                    float overlap = 0.5f * (minDist - d);
                    float pushX = (dx / d) * overlap;
                    float pushY = (dy / d) * overlap;
                    if (!(e1 instanceof Tower || e1 instanceof BaseCore)) {
                        e1.x += pushX;
                        e1.y += pushY;
                    }
                    if (!(e2 instanceof Tower || e2 instanceof BaseCore || e2 instanceof JungleCamp)) {
                        e2.x -= pushX;
                        e2.y -= pushY;
                    }
                }
            }
        }

        // Update projectiles
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(dt, this);
            if (!p.active) {
                projectiles.remove(i);
            }
        }

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle pt = particles.get(i);
            if (!pt.update(dt)) {
                particles.remove(i);
            }
        }

        if (moveMarkerTimer > 0) {
            moveMarkerTimer -= dt;
        }

        // Update kill notification banner
        if (activeBanner != null) {
            activeBanner.timer -= dt;
            if (activeBanner.timer <= 0) {
                activeBanner = null;
            }
        }

        // Update killfeed items
        for (int i = killFeed.size() - 1; i >= 0; i--) {
            KillEvent ke = killFeed.get(i);
            ke.timer -= dt;
            if (ke.timer <= -4.0f) {
                killFeed.remove(i);
            }
        }
    }

    private void spawnMinionWave() {
        // --- 1. MID LANE WAVES ---
        List<Point> blueMidWp = List.of(new Point(12, 12), new Point(20, 20), new Point(25, 25), new Point(33, 33), new Point(43, 43));
        List<Point> redMidWp  = List.of(new Point(36, 36), new Point(28, 28), new Point(25, 25), new Point(16, 16), new Point(6, 6));

        entities.add(new Minion(7.0f, 6.0f, Entity.TEAM_BLUE, false, blueMidWp));
        entities.add(new Minion(6.0f, 7.0f, Entity.TEAM_BLUE, true, blueMidWp));

        entities.add(new Minion(42.0f, 43.0f, Entity.TEAM_RED, false, redMidWp));
        entities.add(new Minion(43.0f, 42.0f, Entity.TEAM_RED, true, redMidWp));

        // --- 2. TOP LANE WAVES ---
        List<Point> blueTopWp = List.of(new Point(6, 15), new Point(6, 30), new Point(6, 43), new Point(25, 43), new Point(43, 43));
        List<Point> redTopWp  = List.of(new Point(30, 43), new Point(15, 43), new Point(6, 43), new Point(6, 25), new Point(6, 6));

        entities.add(new Minion(6.0f, 9.0f, Entity.TEAM_BLUE, false, blueTopWp));
        entities.add(new Minion(6.0f, 8.0f, Entity.TEAM_BLUE, true, blueTopWp));

        entities.add(new Minion(39.0f, 43.0f, Entity.TEAM_RED, false, redTopWp));
        entities.add(new Minion(40.0f, 43.0f, Entity.TEAM_RED, true, redTopWp));

        // --- 3. BOT LANE WAVES ---
        List<Point> blueBotWp = List.of(new Point(15, 6), new Point(30, 6), new Point(43, 6), new Point(43, 25), new Point(43, 43));
        List<Point> redBotWp  = List.of(new Point(43, 30), new Point(43, 15), new Point(43, 6), new Point(25, 6), new Point(6, 6));

        entities.add(new Minion(9.0f, 6.0f, Entity.TEAM_BLUE, false, blueBotWp));
        entities.add(new Minion(8.0f, 6.0f, Entity.TEAM_BLUE, true, blueBotWp));

        entities.add(new Minion(43.0f, 39.0f, Entity.TEAM_RED, false, redBotWp));
        entities.add(new Minion(43.0f, 40.0f, Entity.TEAM_RED, true, redBotWp));
    }

    public synchronized void render(Graphics2D g) {
        if (state != GameState.PLAYING && state != GameState.GAME_OVER) {
            menuUI.render(g, width, height, this);
            return;
        }

        // Background void / sky
        g.setColor(new Color(18, 22, 28));
        g.fillRect(0, 0, width, height);

        // Calculate visible tile range
        VoxelRenderer.Point2D_Float topLeft = VoxelRenderer.screenToWorld(0, 0, cameraX, cameraY);
        VoxelRenderer.Point2D_Float bottomRight = VoxelRenderer.screenToWorld(width, height, cameraX, cameraY);

        int minX = (int) Math.floor(Math.min(topLeft.x, bottomRight.x)) - 8;
        int maxX = (int) Math.ceil(Math.max(topLeft.x, bottomRight.x)) + 8;
        int minY = (int) Math.floor(Math.min(topLeft.y, bottomRight.y)) - 8;
        int maxY = (int) Math.ceil(Math.max(topLeft.y, bottomRight.y)) + 8;

        // 1. Render Ground Tiles with screen frustum culling
        map.renderGround(g, cameraX, cameraY, minX, minY, maxX, maxY, width, height);

        // 2. Render Tall Grass / Bushes (Wind animation & stealth hiding spots)
        map.renderBushes(g, cameraX, cameraY, (float) matchTime, width, height);

        // 3. Render Turret Attack Range Areas & Danger Indicators
        renderTowerRanges(g);

        // 4. Render Move Click Indicator
        if (moveMarkerTimer > 0 && moveMarker != null) {
            g.setColor(new Color(40, 255, 120, (int) (moveMarkerTimer * 255)));
            g.drawOval(moveMarker.x - 12, moveMarker.y - 6, 24, 12);
            g.drawOval(moveMarker.x - 6, moveMarker.y - 3, 12, 6);
        }

        // 5. Render World Objects & Entities sorted by isometric depth (with Fog of War & Screen Culling)
        List<Entity> sortedEntities = new ArrayList<>(entities);
        sortedEntities.sort(Comparator.comparingDouble(e -> (e.x + e.y)));

        for (Entity e : sortedEntities) {
            // Screen frustum culling: don't render entities far outside viewport
            if (!isEntityOnScreen(e)) continue;

            // Fog of War vision culling: don't render enemy/neutral units unless allied vision reveals them!
            // (Teammates/Blue team are always visible)
            if (!isEntityVisibleToPlayer(e)) continue;

            e.render(g, cameraX, cameraY);
        }

        // 6. Render Projectiles (no anonymous class allocations)
        for (Projectile p : projectiles) {
            if (isPointOnScreen(p.x, p.y, 0)) {
                p.render(g, cameraX, cameraY);
            }
        }

        // 7. Render Tree Leaves/Decorations with screen frustum check
        int marginX = VoxelRenderer.TILE_WIDTH;
        int marginY = VoxelRenderer.TILE_HEIGHT * 2;
        for (GameMap.Point3D dec : map.getDecorations()) {
            if (dec.x >= minX && dec.x <= maxX && dec.y >= minY && dec.y <= maxY) {
                Point sp = VoxelRenderer.worldToScreen(dec.x, dec.y, dec.z, cameraX, cameraY);
                if (sp.x >= -marginX && sp.x <= width + marginX &&
                    sp.y >= -marginY && sp.y <= height + marginY) {
                    VoxelRenderer.drawBlock(g, dec.type, sp.x, sp.y);
                }
            }
        }

        // 8. Render Combat Particles & Popups
        for (Particle pt : particles) {
            Point psp = VoxelRenderer.worldToScreen(pt.x, pt.y, pt.z, cameraX, cameraY);
            pt.render(g, psp.x, psp.y);
        }

        // 9. Render HUD
        hud.render(g, width, height, playerHero, this);

        // 10. Render Shop GUI if open
        shop.render(g, width, height, playerHero);
    }

    public boolean hasVisionOf(float x, float y) {
        if (playerHero != null && playerHero.alive) {
            float dx = playerHero.x - x;
            float dy = playerHero.y - y;
            if (dx * dx + dy * dy <= 10.5f * 10.5f) return true;
        }

        for (Entity e : entities) {
            if (e.alive && e.team == Entity.TEAM_BLUE) {
                float vRadius = 9.0f;
                if (e instanceof Minion) vRadius = 6.5f;
                else if (e instanceof Tower || e instanceof BaseCore) vRadius = 10.0f;

                float dx = e.x - x;
                float dy = e.y - y;
                if (dx * dx + dy * dy <= vRadius * vRadius) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEntityVisibleToPlayer(Entity e) {
        // Allied units are always visible
        if (e.team == Entity.TEAM_BLUE) return true;

        // Bush stealth check for enemy heroes
        if (e instanceof Hero eh && eh.isInBush) {
            // Revealed if attacking or taking damage recently
            if (eh.revealTimer > 0) return true;

            // Revealed if any alive allied hero is in the bush with them, or point-blank close
            for (Hero ally : blueHeroes) {
                if (ally.alive) {
                    float dx = ally.x - eh.x;
                    float dy = ally.y - eh.y;
                    float distSq = dx * dx + dy * dy;
                    if (distSq <= 1.4f * 1.4f) return true; // Point blank proximity
                    if (ally.isInBush && distSq <= 3.2f * 3.2f) return true; // Same bush cluster
                }
            }
            return false; // Hidden in tall grass stealth!
        }

        // Enemy and neutral units require allied vision
        return hasVisionOf(e.x, e.y);
    }

    public boolean isPointOnScreen(float x, float y, float z) {
        Point sp = VoxelRenderer.worldToScreen(x, y, z, cameraX, cameraY);
        return sp.x >= -100 && sp.x <= width + 100 && sp.y >= -120 && sp.y <= height + 100;
    }

    public boolean isEntityOnScreen(Entity e) {
        return isPointOnScreen(e.x, e.y, e.z);
    }

    private void renderTowerRanges(Graphics2D g) {
        for (Entity e : entities) {
            if (e instanceof Tower tower && tower.alive) {
                Point centerSp = VoxelRenderer.worldToScreen(tower.x, tower.y, 0, cameraX, cameraY);
                if (centerSp.x < -220 || centerSp.x > width + 220 || centerSp.y < -220 || centerSp.y > height + 220) {
                    continue;
                }

                boolean isEnemy = (tower.team == Entity.TEAM_RED);
                // Enemy towers outside vision are not revealed
                if (isEnemy && !hasVisionOf(tower.x, tower.y)) continue;

                // Build isometric ground perimeter circle using pre-computed relative offsets
                int[] px = new int[PERIMETER_SEGMENTS];
                int[] py = new int[PERIMETER_SEGMENTS];
                for (int i = 0; i < PERIMETER_SEGMENTS; i++) {
                    px[i] = centerSp.x + PERIM_REL_X[i];
                    py[i] = centerSp.y + PERIM_REL_Y[i];
                }
                Polygon poly = new Polygon(px, py, PERIMETER_SEGMENTS);

                boolean targetingPlayer = isEnemy && (tower.targetEntity == playerHero);
                boolean targetingMinion = isEnemy && (tower.targetEntity instanceof Minion);
                boolean targetingOtherHero = isEnemy && (tower.targetEntity instanceof Hero && tower.targetEntity != playerHero);

                if (isEnemy) {
                    if (targetingPlayer) {
                        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.012) * 0.5 + 0.5);
                        g.setColor(new Color(255, 30, 30, (int) (40 + pulse * 60)));
                        g.fillPolygon(poly);
                        g.setColor(new Color(255, 30, 30, (int) (180 + pulse * 75)));
                        g.setStroke(STROKE_DANGER);
                        g.drawPolygon(poly);
                        g.setStroke(STROKE_NORMAL);

                        g.setFont(FONT_DANGER);
                        g.setColor(new Color(255, 40, 40, 250));
                        String warn = "!! DANGER: TURRET ATTACKING YOU !!";
                        int tw = g.getFontMetrics().stringWidth(warn);
                        g.drawString(warn, centerSp.x - tw / 2, centerSp.y - 92);
                    } else if (targetingMinion) {
                        g.setColor(new Color(255, 180, 50, 15));
                        g.fillPolygon(poly);
                        g.setColor(new Color(255, 190, 50, 130));
                        g.setStroke(STROKE_RING);
                        g.drawPolygon(poly);
                        g.setStroke(STROKE_NORMAL);
                    } else if (targetingOtherHero) {
                        g.setColor(new Color(255, 120, 50, 18));
                        g.fillPolygon(poly);
                        g.setColor(new Color(255, 130, 60, 130));
                        g.setStroke(STROKE_RING);
                        g.drawPolygon(poly);
                        g.setStroke(STROKE_NORMAL);
                    } else {
                        g.setColor(new Color(255, 50, 50, 15));
                        g.fillPolygon(poly);
                        g.setColor(new Color(255, 70, 70, 120));
                        g.setStroke(STROKE_RING);
                        g.drawPolygon(poly);
                        g.setStroke(STROKE_NORMAL);
                    }
                } else {
                    g.setColor(new Color(40, 180, 255, 15));
                    g.fillPolygon(poly);
                    g.setColor(new Color(60, 210, 255, 120));
                    g.setStroke(STROKE_RING);
                    g.drawPolygon(poly);
                    g.setStroke(STROKE_NORMAL);
                }
            }
        }
    }

    public void addParticle(Particle p) {
        particles.add(p);
    }

    public void addProjectile(Projectile p) {
        projectiles.add(p);
    }

    public void endGame(boolean playerWon) {
        gameOver = true;
        playerVictory = playerWon;
    }

    // Input Handling
    @Override
    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();

        if (state != GameState.PLAYING && state != GameState.GAME_OVER) {
            menuUI.handleMouseClick(mouseX, mouseY, width, height, this);
            return;
        }

        if (gameOver) {
            state = GameState.MAIN_MENU;
            initMatch();
            return;
        }

        // Check if Shop was clicked
        if (shop.isOpen) {
            shop.handleClick(mouseX, mouseY, playerHero, this);
            return;
        }

        VoxelRenderer.Point2D_Float worldPos = VoxelRenderer.screenToWorld(mouseX, mouseY, cameraX, cameraY);

        if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON1) {
            // Check if clicked an enemy entity (Attack-Move)
            Entity clickedEnemy = null;
            for (Entity ent : entities) {
                if (ent.alive && ent.team != playerHero.team) {
                    Point sp = VoxelRenderer.worldToScreen(ent.x, ent.y, ent.z, cameraX, cameraY);
                    if (Math.abs(sp.x - mouseX) < 26 && Math.abs(sp.y - mouseY) < 40) {
                        clickedEnemy = ent;
                        break;
                    }
                }
            }

            if (clickedEnemy != null) {
                playerHero.targetEntity = clickedEnemy;
                playerHero.cancelRecall();
            } else {
                // Move command
                playerHero.targetEntity = null;
                playerHero.destinationX = worldPos.x;
                playerHero.destinationY = worldPos.y;
                playerHero.cancelRecall();

                moveMarker = new Point(mouseX, mouseY);
                moveMarkerTimer = 0.5f;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (state != GameState.PLAYING && state != GameState.GAME_OVER) {
            return;
        }

        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            state = GameState.MAIN_MENU;
            initMatch();
            return;
        }

        VoxelRenderer.Point2D_Float mouseWorld = VoxelRenderer.screenToWorld(mouseX, mouseY, cameraX, cameraY);

        switch (e.getKeyCode()) {
            case KeyEvent.VK_Q -> {
                playerHero.skillQ.cast(playerHero, mouseWorld.x, mouseWorld.y, null, this);
                playerHero.cancelRecall();
            }
            case KeyEvent.VK_W -> {
                playerHero.skillW.cast(playerHero, mouseWorld.x, mouseWorld.y, null, this);
                playerHero.cancelRecall();
            }
            case KeyEvent.VK_E -> {
                playerHero.skillE.cast(playerHero, mouseWorld.x, mouseWorld.y, null, this);
                playerHero.cancelRecall();
            }
            case KeyEvent.VK_R -> {
                if (playerHero.level < 4) {
                    addKillEvent("Skill Locked", "Ultimate [R] unlocks at Level 4!", true, false, false);
                    return;
                }
                playerHero.skillR.cast(playerHero, mouseWorld.x, mouseWorld.y, null, this);
                playerHero.cancelRecall();
            }
            case KeyEvent.VK_B -> playerHero.startRecall();
            case KeyEvent.VK_P -> shop.toggle();
            case KeyEvent.VK_SPACE -> centerCameraOn(playerHero.x, playerHero.y);
            case KeyEvent.VK_F2 -> saveScreenshot("screenshot_" + System.currentTimeMillis() + ".png");
        }
    }

    public BufferedImage renderToImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        render(g2);
        g2.dispose();
        return img;
    }

    public boolean saveScreenshot(String filepath) {
        BufferedImage img = renderToImage();
        try {
            File outFile = new File(filepath);
            ImageIO.write(img, "png", outFile);
            System.out.println("Screenshot saved to: " + outFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addKillEvent(String killer, String victim, boolean isBlueKiller, boolean isBlueVictim, boolean isPlayerVictim) {
        boolean isPlayerKiller = (playerHero != null && playerHero.heroName.equals(killer));
        String title;
        String subtitle = killer + "  ⚔  " + victim;
        Color titleColor;
        Color bgColor;

        if (victim.contains("Tower") || victim.contains("Turret") || victim.contains("Outer") || victim.contains("Inner")) {
            if (isBlueKiller) {
                title = "TURRET DESTROYED!";
                subtitle = "Your team destroyed " + victim + "! (+250G)";
                titleColor = new Color(255, 220, 80);
                bgColor = new Color(30, 60, 95, 230);
            } else {
                title = "TURRET LOST!";
                subtitle = victim + " was destroyed!";
                titleColor = new Color(255, 90, 80);
                bgColor = new Color(85, 25, 25, 230);
            }
        } else if (isPlayerKiller) {
            title = "YOU HAVE SLAIN AN ENEMY!";
            subtitle = killer + "  ⚔  " + victim + " (+320 Gold)";
            titleColor = new Color(255, 215, 0); // MLBB Gold
            bgColor = new Color(20, 65, 115, 240);
            SoundManager.playLevelUp();
        } else if (isPlayerVictim) {
            title = "YOU HAVE BEEN SLAIN!";
            subtitle = "Slain by " + killer;
            titleColor = new Color(255, 70, 70); // Red
            bgColor = new Color(90, 20, 20, 245);
            SoundManager.playExplosion();
        } else if (isBlueKiller) {
            title = "AN ENEMY HAS BEEN SLAIN!";
            titleColor = new Color(70, 220, 255); // Cyan Blue
            bgColor = new Color(20, 50, 85, 230);
            SoundManager.playHit();
        } else {
            title = "AN ALLY HAS BEEN SLAIN!";
            titleColor = new Color(255, 110, 70); // Orange Red
            bgColor = new Color(80, 28, 28, 230);
            SoundManager.playHit();
        }

        KillEvent event = new KillEvent(killer, victim, title, subtitle, titleColor, bgColor, 3.5f, isPlayerKiller || isPlayerVictim);
        activeBanner = event;
        killFeed.add(0, event);
        while (killFeed.size() > 5) {
            killFeed.remove(killFeed.size() - 1);
        }
    }

    public KillEvent getActiveBanner() { return activeBanner; }
    public List<KillEvent> getKillFeed() { return killFeed; }

    // Getters and Setters
    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }
    public moba.ui.MenuUI getMenuUI() { return menuUI; }
    public JungleCamp getJungleBoss() { return jungleBoss; }
    public JungleCamp getJungleCamp() { return jungleBoss; }
    public List<BuffMonster> getBuffMonsters() { return buffMonsters; }
    public GameMap getMap() { return map; }
    public Hero getPlayerHero() { return playerHero; }
    public Hero getEnemyHero() { return enemyHero; }
    public List<Hero> getBlueHeroes() { return blueHeroes; }
    public List<Hero> getRedHeroes() { return redHeroes; }
    public List<Entity> getAllEntities() { return entities; }
    public float getMatchTime() { return matchTime; }
    public void setMatchTime(float time) { this.matchTime = time; }
    public boolean isGameOver() { return gameOver; }
    public boolean isPlayerVictory() { return playerVictory; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setDimensions(int w, int h) { this.width = w; this.height = h; }

    @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}
