package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

public class JungleCamp extends Entity {
    public enum BossType {
        TURTLE,
        LORD
    }

    public BossType type = BossType.TURTLE;
    public String bossName = "Dragon Turtle";
    public Entity targetEntity = null;
    public float initialX, initialY;
    public float respawnTimer = 0f;
    public float anim = 0f;

    // Timing and condition flags
    public boolean hasSpawnedMinute3 = false;
    public boolean hasSpawnedMinute6 = false;
    public boolean hasSpawnedMinute10Lord = false;
    public boolean turtleSlainAtLeastOnce = false;
    public float timeSinceLastDamage = 999.0f; // Time since last damage received
    public boolean wasAttackedAfterMin8 = false;
    public boolean pendingLordSummon = false;
    public float lordSummonCountdown = 0f;
    public int lordSummonRewardTeam = TEAM_NEUTRAL;

    public JungleCamp(float x, float y) {
        super(x, y, TEAM_NEUTRAL, 2800f);
        this.initialX = x;
        this.initialY = y;
        this.radius = 1.3f;
        this.moveSpeed = 2.0f;
        this.attackRange = 2.4f;
        this.attackDamage = 50f;
        this.attackSpeed = 0.8f;
        setBossType(BossType.TURTLE);
        // Boss starts unspawned at match start (0:00), awaits Minute 3 (180s)
        this.alive = false;
        this.respawnTimer = 180.0f;
    }

    public void setBossType(BossType newType) {
        this.type = newType;
        if (newType == BossType.TURTLE) {
            this.bossName = "Dragon Turtle";
            this.maxHp = 2800f;
            this.hp = maxHp;
            this.attackDamage = 50f;
        } else {
            this.bossName = "Nether Lord";
            this.maxHp = 5200f;
            this.hp = maxHp;
            this.attackDamage = 90f;
        }
    }

    public void spawnTurtleImmediately() {
        setBossType(BossType.TURTLE);
        alive = true;
        hp = maxHp;
        x = initialX;
        y = initialY;
        targetEntity = null;
        timeSinceLastDamage = 999.0f;
    }

    public void spawnLordImmediately() {
        setBossType(BossType.LORD);
        alive = true;
        hp = maxHp;
        x = initialX;
        y = initialY;
        targetEntity = null;
        timeSinceLastDamage = 999.0f;
    }

    public String getObjectiveStatus(float matchTime) {
        if (alive) {
            return (type == BossType.TURTLE ? "Turtle Active!" : "Lord Active!");
        }
        if (pendingLordSummon) {
            return String.format("Lord March: %02ds", (int) Math.ceil(lordSummonCountdown));
        }
        if (matchTime < 180f) {
            int rem = (int) Math.max(0, 180f - matchTime);
            return String.format("Turtle: %02d:%02d", rem / 60, rem % 60);
        }
        if (matchTime < 360f && !hasSpawnedMinute6) {
            int rem = (int) Math.max(0, 360f - matchTime);
            return String.format("Turtle: %02d:%02d", rem / 60, rem % 60);
        }
        if (matchTime < 600f) {
            int rem = (int) Math.max(0, 600f - matchTime);
            return String.format("Lord: %02d:%02d", rem / 60, rem % 60);
        }
        int rem = (int) Math.max(0, respawnTimer);
        return String.format("Lord: %02d:%02d", rem / 60, rem % 60);
    }

    public void transformIntoLord(GameEngine engine) {
        setBossType(BossType.LORD);
        alive = true;
        hp = maxHp;
        x = initialX;
        y = initialY;
        targetEntity = null;
        timeSinceLastDamage = 999.0f;
        engine.addKillEvent("System", "Nether Lord has awakened in the river pit!", true, false, false);
        SoundManager.playExplosion();
        for (int i = 0; i < 35; i++) {
            engine.addParticle(Particle.createExplosionDebris(x, y, 1.8f, Color.ORANGE));
        }
    }

    @Override
    public void update(float dt, GameEngine engine) {
        // Pending Summoned Lord 10-second march countdown
        if (pendingLordSummon) {
            lordSummonCountdown -= dt;
            if (lordSummonCountdown <= 0) {
                pendingLordSummon = false;
                spawnSummonedLord(engine);
            }
        }

        float matchTime = engine.getMatchTime();

        // Track time since last damage
        if (targetEntity != null && targetEntity.alive) {
            timeSinceLastDamage = 0f;
        } else {
            timeSinceLastDamage += dt;
        }

        // 1. Min 3 (180s): First Turtle spawn
        if (!hasSpawnedMinute3 && matchTime >= 180.0f) {
            hasSpawnedMinute3 = true;
            if (!alive && type == BossType.TURTLE) {
                spawnTurtleImmediately();
                engine.addKillEvent("System", "Dragon Turtle has spawned in the river pit!", true, false, false);
                SoundManager.playLevelUp();
            }
        }

        // 2. Min 6 (360s): Second Turtle spawn / respawn
        if (!hasSpawnedMinute6 && matchTime >= 360.0f) {
            hasSpawnedMinute6 = true;
            if (!alive && type == BossType.TURTLE) {
                spawnTurtleImmediately();
                engine.addKillEvent("System", "Dragon Turtle has respawned in the river pit!", true, false, false);
                SoundManager.playLevelUp();
            }
        }

        // 3. Min 8 (480s): Check for Turtle transformation into Lord
        if (type == BossType.TURTLE && alive && matchTime >= 480.0f) {
            boolean isBeingAttacked = (targetEntity != null && targetEntity.alive) || (timeSinceLastDamage < 30.0f);
            if (!isBeingAttacked) {
                // Untouched at min 8, or attacker disengaged for >= 30s
                transformIntoLord(engine);
            }
        }

        // 4. Min 10 (600s): Lord spawn if Turtle died earlier
        if (!hasSpawnedMinute10Lord && matchTime >= 600.0f) {
            hasSpawnedMinute10Lord = true;
            if (!alive || type == BossType.TURTLE) {
                spawnLordImmediately();
                engine.addKillEvent("System", "Nether Lord has descended into the river pit!", true, false, false);
                SoundManager.playExplosion();
            }
        }

        // Respawn timer tick for unspawned / dead state
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) {
                if (matchTime >= 600.0f) {
                    spawnLordImmediately();
                } else if (matchTime >= 360.0f) {
                    spawnTurtleImmediately();
                } else if (matchTime >= 180.0f) {
                    spawnTurtleImmediately();
                }
            }
            return;
        }

        anim += dt * 2.2f;
        if (attackCooldown > 0) attackCooldown -= dt;

        // Combat behavior
        if (targetEntity != null) {
            if (!targetEntity.alive || distanceTo(initialX, initialY) > 9.5f) {
                targetEntity = null; // Disengage / target died or fled
            } else {
                float dist = distanceTo(targetEntity);
                if (dist <= attackRange) {
                    isMoving = false;
                    if (attackCooldown <= 0) {
                        attackCooldown = 1.0f / attackSpeed;
                        SoundManager.playHit();
                        targetEntity.takeDamage(attackDamage, this, engine);
                        engine.addParticle(Particle.createDamagePopup(targetEntity.x, targetEntity.y, targetEntity.z, (int) attackDamage, true));

                        if (type == BossType.LORD) {
                            // Lord ground tremor particles
                            for (int i = 0; i < 8; i++) {
                                double a = (Math.PI * 2 / 8) * i;
                                engine.addParticle(new Particle(x + (float)Math.cos(a)*1.4f, y + (float)Math.sin(a)*1.4f, 0.2f, 0, 0, 1.2f, new Color(255, 120, 30), 6, 0.5f));
                            }
                        }
                    }
                } else {
                    isMoving = true;
                    float dx = targetEntity.x - x;
                    float dy = targetEntity.y - y;
                    x += (dx / dist) * moveSpeed * dt;
                    y += (dy / dist) * moveSpeed * dt;
                }
            }
        } else {
            // Leash back to camp and regenerate slowly
            float d = distanceTo(initialX, initialY);
            if (d > 0.2f) {
                isMoving = true;
                x += ((initialX - x) / d) * moveSpeed * dt;
                y += ((initialY - y) / d) * moveSpeed * dt;
                hp = Math.min(maxHp, hp + 45f * dt);
            } else {
                isMoving = false;
            }
        }
    }

    @Override
    public void takeDamage(float dmg, Entity source, GameEngine engine) {
        super.takeDamage(dmg, source, engine);
        timeSinceLastDamage = 0f;
        if (engine.getMatchTime() >= 480.0f) {
            wasAttackedAfterMin8 = true;
        }
        if (source != null && source.alive) {
            this.targetEntity = source;
        }
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        SoundManager.playLevelUp();
        boolean killerIsBlue = (killer != null && killer.team == TEAM_BLUE);
        int rewardTeam = killerIsBlue ? TEAM_BLUE : TEAM_RED;
        String killerName = killer instanceof Hero h ? h.heroName : (killerIsBlue ? "Blue Team" : "Red Team");

        if (type == BossType.TURTLE) {
            turtleSlainAtLeastOnce = true;

            // "kalau turtle mati maka lord muncul menit 10"
            float matchTime = engine.getMatchTime();
            if (matchTime < 360.0f) {
                // If killed in early game (< min 6), next spawn is Turtle at Min 6 (360s)
                respawnTimer = Math.max(0.1f, 360.0f - matchTime);
            } else {
                // If killed at or after min 6, next boss is Lord at Min 10 (600s)
                respawnTimer = Math.max(0.1f, 600.0f - matchTime);
            }

            // Grant entire team Gold + Shield buff
            for (Entity e : engine.getAllEntities()) {
                if (e.alive && e.team == rewardTeam && e instanceof Hero hero) {
                    hero.addGold(160);
                    hero.addXP(220, engine);
                    hero.shieldHp = 250f; // Turtle shield
                    engine.addParticle(Particle.createHealPopup(hero.x, hero.y, hero.z, 250));
                }
            }

            // Kill notification banner
            engine.addKillEvent(killerName, "Turtle", killerIsBlue, false, false);

            for (int i = 0; i < 25; i++) {
                engine.addParticle(Particle.createExplosionDebris(x, y, 1.2f, new Color(50, 180, 70)));
            }

        } else {
            // LORD SLAIN!
            respawnTimer = 180.0f; // Respawns in 3 mins
            for (Entity e : engine.getAllEntities()) {
                if (e.alive && e.team == rewardTeam && e instanceof Hero hero) {
                    hero.addGold(300);
                    hero.addXP(380, engine);
                    hero.bonusDamage += 25f;
                }
            }

            // Kill notification banner
            engine.addKillEvent(killerName, "Nether Lord", killerIsBlue, false, false);
            engine.addKillEvent("Lord Slain", "Summoned Lord will awaken and march in 10 seconds!", killerIsBlue, false, false);

            for (int i = 0; i < 40; i++) {
                engine.addParticle(Particle.createExplosionDebris(x, y, 1.8f, new Color(255, 100, 30)));
            }

            // Lord does not attack immediately; prepares for 10 seconds, then checks lane with fewest turrets
            pendingLordSummon = true;
            lordSummonCountdown = 10.0f;
            lordSummonRewardTeam = rewardTeam;
        }
    }

    public void spawnSummonedLord(GameEngine engine) {
        int enemyTeam = (lordSummonRewardTeam == TEAM_BLUE) ? TEAM_RED : TEAM_BLUE;

        // Check which enemy lane has the fewest surviving enemy turrets!
        int topTowers = 0;
        int midTowers = 0;
        int botTowers = 0;
        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team == enemyTeam && e instanceof Tower tower) {
                if (tower.towerName.contains("Top")) topTowers++;
                else if (tower.towerName.contains("Mid")) midTowers++;
                else if (tower.towerName.contains("Bot")) botTowers++;
            }
        }

        int minTowers = Math.min(topTowers, Math.min(midTowers, botTowers));
        Hero.Lane targetLane;
        if (midTowers == minTowers) {
            targetLane = Hero.Lane.MID;
        } else if (botTowers == minTowers) {
            targetLane = Hero.Lane.BOT;
        } else {
            targetLane = Hero.Lane.TOP;
        }

        List<Point> marchPath;
        if (lordSummonRewardTeam == TEAM_BLUE) {
            switch (targetLane) {
                case TOP -> marchPath = List.of(new Point(6, 15), new Point(6, 30), new Point(6, 43), new Point(25, 43), new Point(43, 43));
                case BOT -> marchPath = List.of(new Point(15, 6), new Point(30, 6), new Point(43, 6), new Point(43, 25), new Point(43, 43));
                default  -> marchPath = List.of(new Point(12, 12), new Point(20, 20), new Point(25, 25), new Point(33, 33), new Point(43, 43));
            }
        } else {
            switch (targetLane) {
                case TOP -> marchPath = List.of(new Point(30, 43), new Point(15, 43), new Point(6, 43), new Point(6, 25), new Point(6, 6));
                case BOT -> marchPath = List.of(new Point(43, 30), new Point(43, 15), new Point(43, 6), new Point(25, 6), new Point(6, 6));
                default  -> marchPath = List.of(new Point(36, 36), new Point(28, 28), new Point(25, 25), new Point(16, 16), new Point(6, 6));
            }
        }

        float spawnX = marchPath.get(0).x;
        float spawnY = marchPath.get(0).y;
        SummonedLord summonedLord = new SummonedLord(spawnX, spawnY, lordSummonRewardTeam, marchPath, targetLane);
        engine.getAllEntities().add(summonedLord);

        String laneStr = (targetLane == Hero.Lane.TOP ? "Top Lane" : (targetLane == Hero.Lane.BOT ? "Bot Lane" : "Mid Lane"));
        engine.addKillEvent("Lord Awakens", "Lord is charging down " + laneStr + " (" + minTowers + " enemy towers remain)!",
                lordSummonRewardTeam == TEAM_BLUE, false, false);
        SoundManager.playExplosion();
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        if (type == BossType.TURTLE) {
            VoxelRenderer.drawTurtleBoss(g, sp.x, sp.y, anim);
        } else {
            VoxelRenderer.drawLordBoss(g, sp.x, sp.y, anim, false);
        }

        // Name and Health bar positioned according to colossal boss scale
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(type == BossType.TURTLE ? new Color(80, 255, 120) : new Color(255, 180, 40));
        int tw = g.getFontMetrics().stringWidth(bossName);
        if (type == BossType.TURTLE) {
            g.drawString(bossName, sp.x - tw / 2, sp.y - 78);
            renderHealthBar(g, sp.x, sp.y, 80, 70);
        } else {
            g.drawString(bossName, sp.x - tw / 2, sp.y - 142);
            renderHealthBar(g, sp.x, sp.y, 110, 134);
        }
    }
}
