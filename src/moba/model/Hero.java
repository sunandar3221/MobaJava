package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;
import moba.ui.ShopUI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class Hero extends Entity {
    public enum Lane {
        EXP,
        MID,
        GOLD,
        ROAM,
        JUNGLE,
        TOP, // alias for EXP
        BOT  // alias for GOLD
    }

    public String heroName;
    public boolean isPlayer;
    public Lane assignedLane = Lane.MID;

    // Progression
    public int level = 1;
    public int xp = 0;
    public int xpToNext = 100;
    public int gold = 500;
    public int kills = 0;
    public int deaths = 0;
    public int assists = 0;
    public int creepScore = 0;

    // Mana & Regeneration
    public float mana = 200f;
    public float maxMana = 200f;
    public float hpRegen = 3.0f;
    public float manaRegen = 4.5f;

    // Combat Stats & Modifiers
    public float baseArmor = 22f; // Base physical damage mitigation armor
    public float bonusDamage = 0f;
    public float bonusArmor = 0f;
    public float bonusHp = 0f;
    public float shieldHp = 0f; // Turtle shield or defensive ability
    public float aiSkillCooldown = 0f; // Humanized pacing between AI skill casts
    public float blueBuffTimer = 0f; // Blue Buff (Mana regen & Cooldown reduction)
    public float redBuffTimer = 0f;  // Red Buff (True damage & Slow burn)

    // Skills
    public Skill skillQ;
    public Skill skillW;
    public Skill skillE;
    public Skill skillR;

    // Navigation & Actions
    public float destinationX;
    public float destinationY;
    public Entity targetEntity = null;

    // Status effects & Stealth
    public float stunTimer = 0f;
    public float speedBuffTimer = 0f;
    public float respawnTimer = 0f;
    public float maxRespawnTime = 0f;
    public float recallTimer = 0f;
    public boolean isRecalling = false;
    public String lastKilledByName = "Minion";

    // Bush & Stealth
    public boolean isInBush = false;
    public float revealTimer = 0f; // Revealed for 2.5s on attack/damage
    public float bushAmbushTimer = 0f;

    // Items
    public final List<String> inventory = new ArrayList<>();

    // Spawn point
    public float spawnX, spawnY;

    // Lane Waypoints for AI navigation
    private final List<Point> laneWaypoints = new ArrayList<>();
    private int currentWpIndex = 0;

    public Hero(String name, float x, float y, int team, boolean isPlayer, Lane lane) {
        super(x, y, team, 550f);
        this.heroName = name;
        this.isPlayer = isPlayer;
        this.spawnX = x;
        this.spawnY = y;
        this.destinationX = x;
        this.destinationY = y;
        this.assignedLane = lane;

        setupHeroArchetype(name);
        setupLaneWaypoints(team, lane);
    }

    private void setupHeroArchetype(String name) {
        switch (name) {
            case "Alex" -> {
                this.maxHp = 480f;
                this.hp = maxHp;
                this.baseArmor = 16f; // Marksman
                this.moveSpeed = 4.4f;
                this.attackRange = 5.2f; // Ranged archer
                this.attackDamage = 38f;
                this.attackSpeed = 1.25f;
                this.skillQ = new Skill(Skill.Slot.Q, "Arrow Volley", "Fire rapid arrows at target", 3.8f, 25, 6.0f, true);
                this.skillW = new Skill(Skill.Slot.W, "Explosive Arrow", "Shoot arrow that explodes on impact", 5.5f, 40, 7.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Acrobatic Leap", "Dash swiftly away with speed burst", 7.0f, 30, 5.0f, false);
                this.skillR = new Skill(Skill.Slot.R, "Arrow Storm", "Rain down arrows in target area", 24.0f, 75, 7.0f, false);
            }
            case "Evoker" -> {
                this.maxHp = 460f;
                this.hp = maxHp;
                this.baseArmor = 16f; // Mage
                this.moveSpeed = 4.0f;
                this.attackRange = 4.8f; // Magic ranged
                this.attackDamage = 35f;
                this.attackSpeed = 1.0f;
                this.skillQ = new Skill(Skill.Slot.Q, "Fang Strike", "Summon ground fangs to chomp enemies", 4.2f, 35, 5.5f, false);
                this.skillW = new Skill(Skill.Slot.W, "Healing Aura", "Restore 140 HP to self and nearby allies", 8.0f, 50, 4.0f, false);
                this.skillE = new Skill(Skill.Slot.E, "Mist Fade", "Teleport forward leaving smoke behind", 7.5f, 35, 5.2f, false);
                this.skillR = new Skill(Skill.Slot.R, "Vex Tempest", "Unleash swirling magic shockwave", 26.0f, 85, 6.0f, false);
            }
            case "Iron Golem" -> {
                this.maxHp = 760f; // Tank / Roam
                this.hp = maxHp;
                this.baseArmor = 36f; // Heavy Tank armor
                this.moveSpeed = 3.8f;
                this.attackRange = 1.8f;
                this.attackDamage = 44f;
                this.attackSpeed = 0.9f;
                this.skillQ = new Skill(Skill.Slot.Q, "Iron Uppercut", "Heavy punch tossing target up in the air", 4.5f, 30, 2.5f, false);
                this.skillW = new Skill(Skill.Slot.W, "Rose Shield", "Gain 220 HP shield and increase armor", 8.0f, 40, 3.0f, false);
                this.skillE = new Skill(Skill.Slot.E, "Heavy Charge", "Dash forward slamming into foes", 7.0f, 35, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Seismic Quake", "Slam ground with massive AoE stun", 25.0f, 75, 4.8f, false);
            }
            case "Nether Knight" -> {
                this.maxHp = 520f; // Assassin / Jungle
                this.hp = maxHp;
                this.baseArmor = 20f; // Assassin
                this.moveSpeed = 4.5f;
                this.attackRange = 2.2f;
                this.attackDamage = 52f;
                this.attackSpeed = 1.2f;
                this.skillQ = new Skill(Skill.Slot.Q, "Crimson Slash", "High damage twin blade strike", 3.5f, 25, 3.0f, false);
                this.skillW = new Skill(Skill.Slot.W, "Flame Dagger", "Throw piercing dagger slowing target", 6.0f, 35, 6.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Nether Shadow", "Dash swiftly into target or bush", 6.5f, 30, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Abyssal Execute", "Leap upon low HP foe dealing lethal burst", 22.0f, 70, 5.5f, false);
            }
            case "Piglin Brute" -> {
                this.maxHp = 700f; // Fighter / EXP
                this.hp = maxHp;
                this.baseArmor = 28f; // Bruiser armor
                this.moveSpeed = 3.9f;
                this.attackRange = 1.8f;
                this.attackDamage = 48f;
                this.attackSpeed = 0.95f;
                this.skillQ = new Skill(Skill.Slot.Q, "Axe Cleave", "Heavy battleaxe swing that cleaves foes", 4.0f, 25, 2.5f, false);
                this.skillW = new Skill(Skill.Slot.W, "War Cry", "Gain 200 HP shield and speed burst", 9.0f, 40, 3.0f, false);
                this.skillE = new Skill(Skill.Slot.E, "Bull Charge", "Dash violently forward ramming enemies", 7.0f, 35, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Golden Berserk", "Empower attack damage and speed", 28.0f, 70, 4.0f, false);
            }
            case "Wither Skeletor" -> {
                this.maxHp = 520f; // Marksman / Gold
                this.hp = maxHp;
                this.baseArmor = 18f; // Marksman
                this.moveSpeed = 4.2f;
                this.attackRange = 5.0f;
                this.attackDamage = 42f;
                this.attackSpeed = 1.15f;
                this.skillQ = new Skill(Skill.Slot.Q, "Wither Skull", "Launch explosive wither skull projectile", 4.0f, 30, 6.5f, false);
                this.skillW = new Skill(Skill.Slot.W, "Curse Mist", "Area wither cloud causing damage over time", 6.5f, 45, 5.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Shadow Step", "Fade into shadow and blink forward", 8.0f, 35, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Nether Cataclysm", "Erupt dark explosion dealing heavy damage", 25.0f, 85, 5.5f, false);
            }
            case "Warden" -> {
                this.maxHp = 820f; // Tank / Roam (Red)
                this.hp = maxHp;
                this.baseArmor = 38f; // Heavy Tank armor
                this.moveSpeed = 3.8f;
                this.attackRange = 2.0f;
                this.attackDamage = 48f;
                this.attackSpeed = 0.85f;
                this.skillQ = new Skill(Skill.Slot.Q, "Sculk Grasp", "Root nearby foes in dark tendrils", 4.5f, 35, 3.2f, false);
                this.skillW = new Skill(Skill.Slot.W, "Sonic Boom", "Fire concentrated sonic shockwave beam", 7.5f, 50, 6.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Dark Dash", "Charge violently forward knocking foes", 7.0f, 35, 5.0f, false);
                this.skillR = new Skill(Skill.Slot.R, "Echoing Roar", "Terrifying roar stunning all nearby enemies", 26.0f, 80, 5.0f, false);
            }
            case "Shadow Creeper" -> {
                this.maxHp = 490f; // Assassin / Jungle (Red)
                this.hp = maxHp;
                this.baseArmor = 18f; // Assassin
                this.moveSpeed = 4.6f;
                this.attackRange = 2.0f;
                this.attackDamage = 54f;
                this.attackSpeed = 1.25f;
                this.skillQ = new Skill(Skill.Slot.Q, "Ambush Strike", "Sudden burst blade strike from grass", 3.5f, 25, 3.0f, false);
                this.skillW = new Skill(Skill.Slot.W, "Sulfur Smoke", "Blinding smoke that slows enemies", 6.0f, 40, 5.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Shadow Stalk", "Swift dash with temporary speed burst", 6.5f, 30, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Nitro Blast", "Massive targeted explosion on single target", 22.0f, 75, 4.5f, false);
            }
            default -> { // Steve & Herobrine
                this.maxHp = 560f;
                this.hp = maxHp;
                this.baseArmor = 24f; // Balanced Fighter armor
                this.moveSpeed = 4.2f;
                this.attackRange = 2.0f;
                this.attackDamage = 46f;
                this.attackSpeed = 1.1f;
                this.skillQ = new Skill(Skill.Slot.Q, "Whirlwind", "Spin diamond blade hitting nearby foes", 4.0f, 30, 2.8f, false);
                this.skillW = new Skill(Skill.Slot.W, "TNT Toss", "Throw explosive TNT causing AoE blast", 6.5f, 45, 7.5f, false);
                this.skillE = new Skill(Skill.Slot.E, "Ender Pearl", "Teleport forward with speed burst", 8.0f, 40, 5.5f, false);
                this.skillR = new Skill(Skill.Slot.R, "Titan Slam", "Slam ground with redstone shockwave stun", 25.0f, 80, 4.5f, false);
            }
        }
    }

    private void setupLaneWaypoints(int team, Lane lane) {
        laneWaypoints.clear();
        if (team == TEAM_BLUE) {
            switch (lane) {
                case EXP, TOP -> {
                    laneWaypoints.add(new Point(6, 15));
                    laneWaypoints.add(new Point(6, 30));
                    laneWaypoints.add(new Point(6, 43));
                    laneWaypoints.add(new Point(25, 43));
                    laneWaypoints.add(new Point(43, 43));
                }
                case GOLD, BOT -> {
                    laneWaypoints.add(new Point(15, 6));
                    laneWaypoints.add(new Point(30, 6));
                    laneWaypoints.add(new Point(43, 6));
                    laneWaypoints.add(new Point(43, 25));
                    laneWaypoints.add(new Point(43, 43));
                }
                case ROAM -> {
                    // Roamer patrols river bushes and helps lanes
                    laneWaypoints.add(new Point(12, 19));
                    laneWaypoints.add(new Point(23, 26)); // Mid river bush
                    laneWaypoints.add(new Point(17, 28)); // Boss pit bush
                    laneWaypoints.add(new Point(27, 23));
                    laneWaypoints.add(new Point(35, 8));  // Bot river bush
                }
                case JUNGLE -> {
                    // Jungler patrols camps and gank bushes
                    laneWaypoints.add(new Point(11, 19));
                    laneWaypoints.add(new Point(18, 12));
                    laneWaypoints.add(new Point(17, 28)); // Boss pit bush
                    laneWaypoints.add(new Point(23, 26));
                    laneWaypoints.add(new Point(32, 22));
                }
                default -> { // MID
                    laneWaypoints.add(new Point(12, 12));
                    laneWaypoints.add(new Point(20, 20));
                    laneWaypoints.add(new Point(25, 25));
                    laneWaypoints.add(new Point(33, 33));
                    laneWaypoints.add(new Point(43, 43));
                }
            }
        } else {
            // RED TEAM
            switch (lane) {
                case EXP, TOP -> {
                    laneWaypoints.add(new Point(30, 43));
                    laneWaypoints.add(new Point(15, 43));
                    laneWaypoints.add(new Point(6, 43));
                    laneWaypoints.add(new Point(6, 25));
                    laneWaypoints.add(new Point(6, 6));
                }
                case GOLD, BOT -> {
                    laneWaypoints.add(new Point(43, 30));
                    laneWaypoints.add(new Point(43, 15));
                    laneWaypoints.add(new Point(43, 6));
                    laneWaypoints.add(new Point(25, 6));
                    laneWaypoints.add(new Point(6, 6));
                }
                case ROAM -> {
                    laneWaypoints.add(new Point(38, 31));
                    laneWaypoints.add(new Point(27, 23)); // Mid river bush
                    laneWaypoints.add(new Point(32, 22)); // Boss pit bush
                    laneWaypoints.add(new Point(23, 26));
                    laneWaypoints.add(new Point(8, 35));  // Top river bush
                }
                case JUNGLE -> {
                    laneWaypoints.add(new Point(39, 31));
                    laneWaypoints.add(new Point(32, 38));
                    laneWaypoints.add(new Point(32, 22)); // Boss pit bush
                    laneWaypoints.add(new Point(27, 23));
                    laneWaypoints.add(new Point(17, 28));
                }
                default -> { // MID
                    laneWaypoints.add(new Point(36, 36));
                    laneWaypoints.add(new Point(28, 28));
                    laneWaypoints.add(new Point(25, 25));
                    laneWaypoints.add(new Point(16, 16));
                    laneWaypoints.add(new Point(6, 6));
                }
            }
        }
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) {
            respawnTimer -= dt;
            if (!isPlayer && respawnTimer > 0) {
                tryBuyItemsAI(engine);
            }
            if (respawnTimer <= 0) {
                respawn(engine);
            }
            return;
        }

        skillQ.update(dt);
        skillW.update(dt);
        skillE.update(dt);
        skillR.update(dt);
        if (attackCooldown > 0) attackCooldown -= dt;
        if (aiSkillCooldown > 0) aiSkillCooldown -= dt;

        // Bush & Stealth check
        isInBush = engine.getMap().isBush(x, y);
        if (revealTimer > 0) {
            revealTimer -= dt;
        }

        if (stunTimer > 0) {
            stunTimer -= dt;
            return;
        }

        if (speedBuffTimer > 0) {
            speedBuffTimer -= dt;
        }

        // Blue & Red Buff Timers
        if (blueBuffTimer > 0) {
            blueBuffTimer -= dt;
            mana = Math.min(maxMana, mana + 18.0f * dt); // Rapid mana recovery
            // Extra CDR tick
            skillQ.update(dt * 0.20f);
            skillW.update(dt * 0.20f);
            skillE.update(dt * 0.20f);
            skillR.update(dt * 0.20f);
        }

        if (redBuffTimer > 0) {
            redBuffTimer -= dt;
        }

        if (isRecalling) {
            recallTimer -= dt;
            if (Math.random() < 0.3) {
                engine.addParticle(new Particle(x, y, 0.2f, 0, 0, 1.2f, new Color(100, 220, 255), 6, 0.4f));
            }
            if (recallTimer <= 0) {
                isRecalling = false;
                x = spawnX;
                y = spawnY;
                destinationX = spawnX;
                destinationY = spawnY;
                hp = maxHp + bonusHp;
                mana = maxMana;
                SoundManager.playTeleport();
            }
            return;
        }

        float actualMaxHp = maxHp + bonusHp;
        if (hp < actualMaxHp) {
            hp = Math.min(actualMaxHp, hp + hpRegen * dt);
        }
        if (mana < maxMana) {
            mana = Math.min(maxMana, mana + manaRegen * dt);
        }

        // Base Fountain Heal
        if (distanceTo(spawnX, spawnY) < 5.0f) {
            hp = Math.min(actualMaxHp, hp + 85f * dt);
            mana = Math.min(maxMana, mana + 65f * dt);
            if (!isPlayer) {
                tryBuyItemsAI(engine);
            }
        }

        if (!isPlayer) {
            updateSmartAI(dt, engine);
        }

        updateMovementAndCombat(dt, engine);
    }

    private void tryBuyItemsAI(GameEngine engine) {
        if (inventory.size() >= 6) return;
        for (int i = 0; i < ShopUI.ITEMS.size(); i++) {
            ShopUI.ShopItem item = ShopUI.ITEMS.get(i);
            if (gold >= item.cost && !inventory.contains(item.name)) {
                gold -= item.cost;
                inventory.add(item.name);
                bonusDamage += item.bonusDmg;
                bonusArmor += item.bonusArmor;
                bonusHp += item.bonusHp;
                moveSpeed += item.bonusSpeed;
                hpRegen += item.bonusRegen;
                hp += item.bonusHp;
                break;
            }
        }
    }

    private void updateMovementAndCombat(float dt, GameEngine engine) {
        if (targetEntity != null) {
            if (!targetEntity.alive) {
                targetEntity = null;
            } else {
                float dist = distanceTo(targetEntity);
                if (dist <= attackRange) {
                    isMoving = false;
                    if (attackCooldown <= 0) {
                        performBasicAttack(targetEntity, engine);
                    }
                } else {
                    destinationX = targetEntity.x;
                    destinationY = targetEntity.y;
                }
            }
        }

        float dx = destinationX - x;
        float dy = destinationY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.25f) {
            facingAngle = (float) Math.atan2(dy, dx);

            float speed = moveSpeed * (speedBuffTimer > 0 ? 1.35f : 1.0f);
            float step = speed * dt;
            float nx = x + (dx / dist) * Math.min(step, dist);
            float ny = y + (dy / dist) * Math.min(step, dist);

            float oldX = x;
            float oldY = y;
            if (engine.getMap().isWalkable(nx, ny)) {
                x = nx;
                y = ny;
            } else if (engine.getMap().isWalkable(nx, y)) {
                x = nx;
            } else if (engine.getMap().isWalkable(x, ny)) {
                y = ny;
            }

            if (x != oldX || y != oldY) {
                isMoving = true;
                walkAnim += dt * 10f;
            } else {
                isMoving = false;
                // Bot obstacle recovery: advance waypoint if blocked
                if (!isPlayer && currentWpIndex < laneWaypoints.size() - 1) {
                    currentWpIndex++;
                }
            }
        } else {
            isMoving = false;
        }
    }

    public void performBasicAttack(Entity target, GameEngine engine) {
        attackCooldown = 1.0f / attackSpeed;
        revealTimer = 2.5f; // Revealed when attacking!

        if (attackRange > 3.0f) {
            // Ranged Attack (Alex, Evoker, Wither)
            SoundManager.playShoot();
            Projectile.Type pType = "Evoker".equals(heroName) ? Projectile.Type.MAGIC_ORB : Projectile.Type.ARROW;
            float dmg = attackDamage + bonusDamage;
            Projectile proj = new Projectile(this, target, pType, dmg, 10.5f);
            engine.addProjectile(proj);
        } else {
            // Melee Attack (Steve, Herobrine, Piglin, Iron Golem, Nether Knight, etc.)
            SoundManager.playSwordSwing();
            float finalDmg = attackDamage + bonusDamage;
            boolean crit = Math.random() < 0.20;
            if (crit) finalDmg *= 1.75f;

            target.takeDamage(finalDmg, this, engine);
            engine.addParticle(Particle.createDamagePopup(target.x, target.y, target.z, (int) finalDmg, crit));
            for (int i = 0; i < (crit ? 8 : 4); i++) {
                engine.addParticle(Particle.createCritStar(target.x, target.y, target.z));
            }
            SoundManager.playHit();
        }

        // Red Buff On-Hit Burn & Slow
        if (redBuffTimer > 0) {
            float redBurn = 25.0f;
            target.takeDamage(redBurn, this, engine);
            engine.addParticle(Particle.createDamagePopup(target.x, target.y, target.z, (int) redBurn, false));
            for (int i = 0; i < 4; i++) {
                engine.addParticle(Particle.createExplosionDebris(target.x, target.y, 0.7f, Color.ORANGE));
            }
        }

        // Tower aggro switch: if damaging an enemy hero near their tower, tower protects ally
        if (target instanceof Hero targetHero && targetHero.team != this.team) {
            for (Entity e : engine.getAllEntities()) {
                if (e.alive && e.team == targetHero.team && e instanceof Tower tower) {
                    if (tower.distanceTo(targetHero) <= tower.attackRange || tower.distanceTo(this) <= tower.attackRange) {
                        tower.setAggroOnHero(this);
                    }
                }
            }
        }
    }

    /**
     * Enhanced Smart AI:
     * - Restrained turret diving (tidak nafsu kill blindly under turret)
     * - Strategic Bush Ambush & Ganks (pinter culik di rumput)
     * - Retreat on low HP
     */
    private void updateSmartAI(float dt, GameEngine engine) {
        float actualMaxHp = maxHp + bonusHp;

        // 1. SMART RETREAT: If low HP (< 30%), disengage and retreat
        if (hp < actualMaxHp * 0.30f) {
            destinationX = spawnX;
            destinationY = spawnY;
            targetEntity = null;

            if (skillE.isReady() && distanceTo(spawnX, spawnY) > 6.0f) {
                skillE.cast(this, spawnX, spawnY, null, engine);
            }
            return;
        }

        // If healing at base, wait until healthy (> 80%) before leaving
        if (distanceTo(spawnX, spawnY) < 6.0f && hp < actualMaxHp * 0.80f) {
            isMoving = false;
            return;
        }

        // 2. CHECK TURRET DANGER & STANDOFF PERIMETER: Find nearest enemy turret
        Tower nearestEnemyTower = null;
        float minTowerDist = Float.MAX_VALUE;
        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != this.team && e instanceof Tower t) {
                float d = distanceTo(t);
                if (d < minTowerDist) {
                    minTowerDist = d;
                    nearestEnemyTower = t;
                }
            }
        }

        int alliedMinionsNear = 0;
        float safeTowerDist = 11.0f;
        float safeX = spawnX;
        float safeY = spawnY;

        if (nearestEnemyTower != null) {
            safeTowerDist = nearestEnemyTower.attackRange + 4.0f;
            for (Entity e : engine.getAllEntities()) {
                if (e.alive && e.team == this.team && e instanceof Minion m && nearestEnemyTower.distanceTo(m) <= nearestEnemyTower.attackRange) {
                    alliedMinionsNear++;
                }
            }

            // Calculate safe position outside tower along the axis from tower to hero
            float tdx = this.x - nearestEnemyTower.x;
            float tdy = this.y - nearestEnemyTower.y;
            float td = (float) Math.hypot(tdx, tdy);
            if (td < 0.1f) {
                tdx = spawnX - nearestEnemyTower.x;
                tdy = spawnY - nearestEnemyTower.y;
                td = (float) Math.hypot(tdx, tdy);
                if (td < 0.1f) { tdx = 1.0f; tdy = 0.0f; td = 1.0f; }
            }
            safeX = nearestEnemyTower.x + (tdx / td) * safeTowerDist;
            safeY = nearestEnemyTower.y + (tdy / td) * safeTowerDist;

            // Turret danger logic when NO allied minions are tanking:
            if (alliedMinionsNear == 0) {
                // If previous target is under enemy turret, cancel target immediately!
                if (targetEntity != null && nearestEnemyTower.distanceTo(targetEntity) <= nearestEnemyTower.attackRange) {
                    targetEntity = null;
                }

                // If hero accidentally stepped inside turret attack range (or within buffer zone 0.6f), retreat immediately to safe distance!
                if (minTowerDist < nearestEnemyTower.attackRange + 0.6f) {
                    destinationX = safeX;
                    destinationY = safeY;
                    targetEntity = null;
                    return;
                }
            }
        }

        // 3. TARGETING HIERARCHY: Find priority enemy hero
        Hero priorityEnemyHero = null;
        float minTargetScore = Float.MAX_VALUE;

        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != this.team && e instanceof Hero enemy) {
                // Bush stealth culling: enemies in bushes cannot be targeted unless point blank (<= 1.4) or in same bush (<= 3.2)!
                if (enemy.isInBush && enemy.revealTimer <= 0) {
                    boolean sameBush = this.isInBush && distanceTo(enemy) <= 3.2f;
                    boolean pointBlank = distanceTo(enemy) <= 1.4f;
                    if (!sameBush && !pointBlank) {
                        continue;
                    }
                }

                float d = distanceTo(enemy);
                if (d < 11.5f) {
                    // Smart balanced threat scoring: Frontline heroes/tanks in close range naturally draw aggro
                    // so bots do not tunnel-vision onto the player across the screen!
                    float hpPercent = enemy.hp / (enemy.maxHp + enemy.bonusHp);
                    float targetScore = d * 1.6f + hpPercent * 4.0f;
                    if (targetScore < minTargetScore) {
                        minTargetScore = targetScore;
                        priorityEnemyHero = enemy;
                    }
                }
            }
        }

        // RESTRAIN TURRET DIVE: If target is under enemy turret, do NOT chase unless safe!
        if (priorityEnemyHero != null && nearestEnemyTower != null) {
            boolean enemyUnderTower = (nearestEnemyTower.distanceTo(priorityEnemyHero) <= nearestEnemyTower.attackRange);
            if (enemyUnderTower) {
                boolean safeDive = (alliedMinionsNear >= 2) && (priorityEnemyHero.hp < priorityEnemyHero.maxHp * 0.18f) && (hp > actualMaxHp * 0.55f);
                if (!safeDive) {
                    priorityEnemyHero = null; // Do NOT dive!
                }
            }
        }

        // Clear targetEntity if it was an enemy hero who retreated under turret without minions
        if (priorityEnemyHero == null && targetEntity instanceof Hero th && th.team != this.team) {
            if (nearestEnemyTower != null && alliedMinionsNear == 0 && nearestEnemyTower.distanceTo(th) <= nearestEnemyTower.attackRange) {
                targetEntity = null;
            }
        }

        // 4. COMBAT & SKILL ROTATION:
        if (priorityEnemyHero != null) {
            targetEntity = priorityEnemyHero;

            // Humanized skill rotation cadence (no instant 4-skill machine gun dump in 1 frame)
            if (aiSkillCooldown <= 0) {
                float dist = distanceTo(priorityEnemyHero);

                // Step 1: Gap close with Skill E if out of basic attack range
                if (skillE.isReady() && dist > attackRange + 0.8f && dist <= skillE.range + 1.5f) {
                    if (skillE.cast(this, priorityEnemyHero.x, priorityEnemyHero.y, priorityEnemyHero, engine)) {
                        aiSkillCooldown = 0.55f;
                        return;
                    }
                }

                // Step 2: Ultimate (R) - only if Level >= 4 and foe is low HP (<65%) or close
                if (level >= 4 && skillR.isReady() && dist <= skillR.range) {
                    if (priorityEnemyHero.hp <= (priorityEnemyHero.maxHp + priorityEnemyHero.bonusHp) * 0.65f || dist <= 3.2f) {
                        if (skillR.cast(this, priorityEnemyHero.x, priorityEnemyHero.y, priorityEnemyHero, engine)) {
                            aiSkillCooldown = 0.65f;
                            return;
                        }
                    }
                }

                // Step 3: Burst damage W & Q with human reaction spacing
                if (skillW.isReady() && dist <= skillW.range) {
                    if (skillW.cast(this, priorityEnemyHero.x, priorityEnemyHero.y, priorityEnemyHero, engine)) {
                        aiSkillCooldown = 0.50f;
                        return;
                    }
                }
                if (skillQ.isReady() && dist <= skillQ.range) {
                    if (skillQ.cast(this, priorityEnemyHero.x, priorityEnemyHero.y, priorityEnemyHero, engine)) {
                        aiSkillCooldown = 0.50f;
                        return;
                    }
                }
            }
            return;
        }

        // 5. OBJECTIVE BEHAVIOR FOR JUNGLE & ROAM AT TURTLE / LORD:
        JungleCamp boss = engine.getJungleBoss();
        if (boss != null && boss.alive) {
            float distToBoss = distanceTo(boss.x, boss.y);
            if (assignedLane == Lane.JUNGLE) {
                // Jungler acts solo & panicky ("goblok"):
                // If low HP (< 60%) near boss, panics and runs away abandoning the objective
                if (hp < actualMaxHp * 0.60f && distToBoss < 7.5f) {
                    destinationX = spawnX;
                    destinationY = spawnY;
                    targetEntity = null;
                    return;
                }
                // When healthy, actively seeks and attacks Turtle or Lord!
                if (hp >= actualMaxHp * 0.60f) {
                    targetEntity = boss;
                    if (distToBoss <= attackRange) {
                        destinationX = x;
                        destinationY = y;
                        isMoving = false;
                        if (attackCooldown <= 0) performBasicAttack(boss, engine);
                        if (skillQ.isReady() && distToBoss <= skillQ.range) skillQ.cast(this, boss.x, boss.y, boss, engine);
                        if (skillW.isReady() && distToBoss <= skillW.range) skillW.cast(this, boss.x, boss.y, boss, engine);
                    } else {
                        // Actively navigate straight into boss pit from anywhere on map!
                        destinationX = boss.x;
                        destinationY = boss.y;
                    }
                    return;
                }
            } else if (assignedLane == Lane.ROAM) {
                // Roamer is completely "ga kompak" at boss:
                // Roamer avoids helping Jungler at Turtle/Lord, wandering away to farm lane creeps or rotate!
                if (distToBoss < 9.0f) {
                    if (!laneWaypoints.isEmpty()) {
                        Point wp = laneWaypoints.get((currentWpIndex + 2) % laneWaypoints.size());
                        destinationX = wp.x;
                        destinationY = wp.y;
                        targetEntity = null;
                        return;
                    }
                }
            }
        }

        // 6. JUNGLER AI: FARM BUFF MONSTERS (BLUE BUFF & RED BUFF)
        if (assignedLane == Lane.JUNGLE) {
            BuffMonster closestBuff = null;
            float minBuffDist = Float.MAX_VALUE;
            for (BuffMonster bm : engine.getBuffMonsters()) {
                if (bm.alive) {
                    float d = distanceTo(bm);
                    boolean isOwnJungle = (this.team == TEAM_BLUE && bm.initialX < 26.0f) || (this.team == TEAM_RED && bm.initialX > 24.0f);
                    float effectiveDist = isOwnJungle ? d : d + 12.0f;
                    if (effectiveDist < minBuffDist) {
                        minBuffDist = effectiveDist;
                        closestBuff = bm;
                    }
                }
            }

            if (closestBuff != null && (boss == null || !boss.alive || hp < actualMaxHp * 0.60f || minBuffDist < 9.0f)) {
                float d = distanceTo(closestBuff);
                if (d <= attackRange) {
                    targetEntity = closestBuff;
                    if (attackCooldown <= 0) performBasicAttack(closestBuff, engine);
                    if (skillQ.isReady() && d <= skillQ.range) skillQ.cast(this, closestBuff.x, closestBuff.y, closestBuff, engine);
                    if (skillW.isReady() && d <= skillW.range) skillW.cast(this, closestBuff.x, closestBuff.y, closestBuff, engine);
                } else {
                    destinationX = closestBuff.x;
                    destinationY = closestBuff.y;
                    targetEntity = null;
                }
                return;
            }
        }

        // 7. ROAM AI: ACCOMPANY GOLD LANE (MARKSMAN) & ROTATE TO OTHER LANES
        if (assignedLane == Lane.ROAM) {
            Hero alliedGoldLaner = null;
            List<Hero> teamHeroes = (this.team == TEAM_BLUE) ? engine.getBlueHeroes() : engine.getRedHeroes();
            for (Hero h : teamHeroes) {
                if (h.alive && h != this && h.assignedLane == Lane.GOLD) {
                    alliedGoldLaner = h;
                    break;
                }
            }

            // Roam rotation timer: rotates between Gold Lane (50s), Mid Lane (20s), and EXP/River (20s)
            float cycleTime = (engine.getMatchTime() + (this.team == TEAM_RED ? 12f : 0f)) % 90.0f;
            boolean isMidRotation = (cycleTime >= 50.0f && cycleTime < 70.0f);
            boolean isExpRotation = (cycleTime >= 70.0f && cycleTime < 90.0f);

            if (isMidRotation) {
                // Rotate to Mid Lane (open stone road)
                Point midWp = (this.team == TEAM_BLUE) ? new Point(20, 20) : new Point(30, 30);
                float distToMid = distanceTo(midWp.x, midWp.y);
                if (distToMid > 1.8f) {
                    destinationX = midWp.x;
                    destinationY = midWp.y;
                } else {
                    destinationX = x;
                    destinationY = y;
                    isMoving = false;
                }
                targetEntity = null;
            } else if (isExpRotation) {
                // Rotate to EXP Lane (top lane open road)
                Point expWp = (this.team == TEAM_BLUE) ? new Point(6, 25) : new Point(25, 43);
                float distToExp = distanceTo(expWp.x, expWp.y);
                if (distToExp > 1.8f) {
                    destinationX = expWp.x;
                    destinationY = expWp.y;
                } else {
                    destinationX = x;
                    destinationY = y;
                    isMoving = false;
                }
                targetEntity = null;
            } else if (alliedGoldLaner != null && alliedGoldLaner.alive) {
                // Accompany Gold Laner (Marksman / Alex / Wither Skeletor)
                float distToMarksman = distanceTo(alliedGoldLaner);

                // If marksman has an active target, protect them!
                if (alliedGoldLaner.targetEntity != null && alliedGoldLaner.targetEntity.alive) {
                    targetEntity = alliedGoldLaner.targetEntity;
                    float td = distanceTo(targetEntity);
                    if (td <= attackRange) {
                        isMoving = false;
                        destinationX = x;
                        destinationY = y;
                        if (attackCooldown <= 0) performBasicAttack(targetEntity, engine);
                        if (skillQ.isReady() && td <= skillQ.range) skillQ.cast(this, targetEntity.x, targetEntity.y, targetEntity, engine);
                    } else {
                        destinationX = targetEntity.x;
                        destinationY = targetEntity.y;
                    }
                    return;
                }

                // Smooth escort without jitter (hysteresis: catch up when > 3.8f, rest when <= 3.8f)
                if (distToMarksman > 3.8f) {
                    destinationX = alliedGoldLaner.x;
                    destinationY = alliedGoldLaner.y;
                    targetEntity = null;
                    return;
                } else {
                    destinationX = x;
                    destinationY = y;
                    isMoving = false;
                    targetEntity = null;
                    return;
                }
            }
        }

        // 6. LANE CREEP LAST-HITTING & FARMING (Avoid diving turret without minions)
        Entity bestCreep = null;
        float minCreepDist = Float.MAX_VALUE;
        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != this.team && e instanceof Minion minion) {
                if (nearestEnemyTower != null && alliedMinionsNear == 0 && nearestEnemyTower.distanceTo(minion) <= nearestEnemyTower.attackRange) {
                    continue;
                }
                float d = distanceTo(minion);
                if (d < 8.5f && d < minCreepDist) {
                    minCreepDist = d;
                    bestCreep = minion;
                }
            }
        }

        if (bestCreep != null) {
            targetEntity = bestCreep;
            if (skillQ.isReady() && distanceTo(bestCreep) <= skillQ.range) {
                skillQ.cast(this, bestCreep.x, bestCreep.y, bestCreep, engine);
            }
            return;
        }

        // 7. ATTACK ENEMY TOWERS IN RANGE (Only when safe with minions or outranging tower)
        if (alliedMinionsNear > 0 || (nearestEnemyTower != null && minTowerDist > nearestEnemyTower.attackRange && attackRange > nearestEnemyTower.attackRange)) {
            for (Entity e : engine.getAllEntities()) {
                if (e.alive && e.team != this.team && (e instanceof Tower || e instanceof BaseCore)) {
                    if (distanceTo(e) <= 7.5f) {
                        targetEntity = e;
                        return;
                    }
                }
            }
        }

        // 7b. REAL MLBB BOT MACRO TACTICS WHEN WAITING AT ENEMY TURRET (NO ALLIED MINIONS)
        if (nearestEnemyTower != null && alliedMinionsNear == 0 && minTowerDist <= safeTowerDist + 1.5f) {
            // A. Low/Moderate HP Recall: Return to base to heal & shop instead of idling in lane!
            if (hp < actualMaxHp * 0.60f) {
                destinationX = spawnX;
                destinationY = spawnY;
                targetEntity = null;
                return;
            }

            // B. Farm nearby neutral jungle buff monster if safely outside enemy turret
            for (BuffMonster bm : engine.getBuffMonsters()) {
                if (bm.alive && nearestEnemyTower.distanceTo(bm) > nearestEnemyTower.attackRange + 1.5f && distanceTo(bm) < 10.0f) {
                    targetEntity = bm;
                    float bd = distanceTo(bm);
                    if (bd <= attackRange) {
                        destinationX = x;
                        destinationY = y;
                        isMoving = false;
                        if (attackCooldown <= 0) performBasicAttack(bm, engine);
                        if (skillQ.isReady() && bd <= skillQ.range) skillQ.cast(this, bm.x, bm.y, bm, engine);
                    } else {
                        destinationX = bm.x;
                        destinationY = bm.y;
                    }
                    return;
                }
            }

            // C. Take Cover in Nearby Safe Bush (Ambush / Vision / Stealth):
            Point bestSafeBush = null;
            float minBushDist = 8.5f;
            for (Point bp : engine.getMap().getBushPositions()) {
                // Bush must be comfortably outside enemy turret attack range!
                if (nearestEnemyTower.distanceTo(bp.x, bp.y) >= nearestEnemyTower.attackRange + 1.0f) {
                    float bd = distanceTo(bp.x, bp.y);
                    if (bd < minBushDist) {
                        minBushDist = bd;
                        bestSafeBush = bp;
                    }
                }
            }

            if (bestSafeBush != null) {
                float distToBush = distanceTo(bestSafeBush.x, bestSafeBush.y);
                if (distToBush > 0.25f) {
                    // Keep walking smoothly all the way into the safe bush center
                    destinationX = bestSafeBush.x;
                    destinationY = bestSafeBush.y;
                    targetEntity = null;
                    return;
                } else {
                    // Deep inside bush center: snap cleanly to center and hold stealth ambush position
                    destinationX = bestSafeBush.x;
                    destinationY = bestSafeBush.y;
                    x = bestSafeBush.x;
                    y = bestSafeBush.y;
                    isInBush = true;
                    isMoving = false;
                    targetEntity = null;
                    return;
                }
            }

            // D. Mid & Roam Lane Rotation (Scout river / Gank adjacent lanes):
            if ((assignedLane == Lane.MID && Math.abs(x - y) < 5.0f) || assignedLane == Lane.ROAM) {
                Point rotateTarget = (this.team == TEAM_BLUE) ? new Point(18, 31) : new Point(31, 18);
                float rd = distanceTo(rotateTarget.x, rotateTarget.y);
                if (rd > 2.5f) {
                    destinationX = rotateTarget.x;
                    destinationY = rotateTarget.y;
                    targetEntity = null;
                    return;
                }
            }

            // E. Safe Standoff & Minion Waiting: If no nearby bush or rotation, retreat outside turret danger!
            if (minTowerDist < safeTowerDist - 0.5f) {
                // Too close to turret border! Retreat further back to safely wait for oncoming minion wave!
                destinationX = safeX;
                destinationY = safeY;
                targetEntity = null;
                return;
            } else {
                // Safely outside turret danger: hold ground waiting for next minion wave
                destinationX = x;
                destinationY = y;
                isMoving = false;
                targetEntity = null;
                return;
            }
        }

        // 8. MARCH DOWN WAYPOINTS (Respect safe turret perimeter when minions absent)
        targetEntity = null;
        if (!laneWaypoints.isEmpty()) {
            if (currentWpIndex >= laneWaypoints.size()) {
                currentWpIndex = laneWaypoints.size() - 1;
            }
            Point wp = laneWaypoints.get(currentWpIndex);
            float distToWp = distanceTo(wp.x, wp.y);
            if (distToWp < 2.0f && currentWpIndex < laneWaypoints.size() - 1) {
                currentWpIndex++;
                wp = laneWaypoints.get(currentWpIndex);
            }

            // If marching down lane brings hero into turret danger without minions, hold ground at safe standoff!
            if (nearestEnemyTower != null && alliedMinionsNear == 0) {
                float wpTowerDist = (float) Math.hypot(wp.x - nearestEnemyTower.x, wp.y - nearestEnemyTower.y);
                if (wpTowerDist < safeTowerDist || minTowerDist <= safeTowerDist + 0.6f) {
                    destinationX = x;
                    destinationY = y;
                    isMoving = false;
                    targetEntity = null;
                    return;
                }
            }

            destinationX = wp.x;
            destinationY = wp.y;
        }
    }

    private Point findNearestStrategicBush(GameEngine engine) {
        Point best = null;
        float minDist = 14.0f;
        for (Point bp : engine.getMap().getBushPositions()) {
            float d = distanceTo(bp.x, bp.y);
            if (d < minDist) {
                minDist = d;
                best = bp;
            }
        }
        return best;
    }

    @Override
    public void takeDamage(float dmg, Entity source, GameEngine engine) {
        if (source instanceof Hero h) {
            revealTimer = 2.5f; // Reveal stealth when damaged by enemy hero
            h.revealTimer = 2.5f;
        }

        // Apply Armor Damage Reduction (Physical mitigation formula: Armor / (Armor + 100))
        float totalArmor = Math.max(0f, baseArmor + bonusArmor);
        float damageReduction = totalArmor / (totalArmor + 100.0f);
        float reducedDmg = dmg * (1.0f - damageReduction);

        // Absorb with Shield if active
        if (shieldHp > 0) {
            float absorbed = Math.min(shieldHp, reducedDmg);
            shieldHp -= absorbed;
            reducedDmg -= absorbed;
            engine.addParticle(Particle.createHealPopup(x, y, z + 0.3f, (int) absorbed));
            if (reducedDmg <= 0) return;
        }

        super.takeDamage(reducedDmg, source, engine);
    }

    public void addXP(int amount, GameEngine engine) {
        xp += amount;
        while (xp >= xpToNext && level < 18) {
            xp -= xpToNext;
            level++;
            xpToNext = (int) (xpToNext * 1.35f);
            onLevelUp(engine);
        }
    }

    private void onLevelUp(GameEngine engine) {
        maxHp += 45f;
        hp = Math.min(hp + 50f, maxHp + bonusHp);
        maxMana += 25f;
        mana = Math.min(mana + 35f, maxMana);
        attackDamage += 4.5f;

        if (isPlayer) {
            SoundManager.playLevelUp();
            if (level == 4) {
                engine.addKillEvent("Skill Unlocked!", "Ultimate [R] is now available!", true, false, false);
            }
        }
        engine.addParticle(Particle.createHealPopup(x, y, z, 50));
        for (int i = 0; i < 18; i++) {
            double angle = (Math.PI * 2 / 18) * i;
            engine.addParticle(new Particle(x + (float)Math.cos(angle)*1.2f, y + (float)Math.sin(angle)*1.2f, 0.5f,
                    0, 0, 1.8f, new Color(255, 215, 0), 8, 0.8f));
        }
    }

    public void addGold(int amount) {
        gold += amount;
        if (isPlayer) SoundManager.playCoin();
    }

    public void startRecall() {
        if (!alive || isRecalling) return;
        isRecalling = true;
        recallTimer = 4.0f;
    }

    public void cancelRecall() {
        isRecalling = false;
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        deaths++;
        maxRespawnTime = 3.0f + level * 0.8f;
        respawnTimer = maxRespawnTime;
        isRecalling = false;
        targetEntity = null;
        shieldHp = 0f;
        isMoving = false;

        // 20-second damage attribution rule:
        // If killer is not an enemy Hero, check if an enemy Hero damaged this victim in the last 20 seconds!
        Hero effectiveHeroKiller = null;
        if (killer instanceof Hero heroKiller && heroKiller.team != this.team) {
            effectiveHeroKiller = heroKiller;
        } else {
            float matchTime = engine.getMatchTime();
            if (lastHeroDamager != null && lastHeroDamager.alive && lastHeroDamager.team != this.team) {
                if (matchTime - lastHeroDamageTime <= 20.0f) {
                    effectiveHeroKiller = lastHeroDamager;
                }
            }
        }

        String killerName;
        boolean killerIsBlue;
        boolean isPlayerKiller = false;

        if (effectiveHeroKiller != null) {
            killerName = effectiveHeroKiller.heroName;
            killerIsBlue = (effectiveHeroKiller.team == TEAM_BLUE);
            isPlayerKiller = effectiveHeroKiller.isPlayer;

            effectiveHeroKiller.kills++;
            effectiveHeroKiller.addGold(320);
            effectiveHeroKiller.addXP(260, engine);
        } else {
            // Executed by environment / neutral (no enemy hero in last 20 seconds)
            if (killer instanceof Tower towerKiller) {
                killerName = towerKiller.towerName;
                killerIsBlue = (towerKiller.team == TEAM_BLUE);
            } else if (killer instanceof Minion minionKiller) {
                killerName = (minionKiller.team == TEAM_BLUE ? "Blue Minion" : "Red Minion");
                killerIsBlue = (minionKiller.team == TEAM_BLUE);
            } else if (killer instanceof BuffMonster buffKiller) {
                killerName = buffKiller.monsterName;
                killerIsBlue = false;
            } else if (killer instanceof JungleCamp bossKiller) {
                killerName = bossKiller.bossName;
                killerIsBlue = false;
            } else if (killer instanceof SummonedLord lordKiller) {
                killerName = "Summoned Lord";
                killerIsBlue = (lordKiller.team == TEAM_BLUE);
            } else {
                killerName = "Turret";
                killerIsBlue = (this.team != TEAM_BLUE);
            }
        }
        lastKilledByName = killerName;

        // Determine assists: All other enemy heroes of this victim who dealt damage in the last 20 seconds
        float matchTime = engine.getMatchTime();
        for (java.util.Map.Entry<Hero, Float> entry : recentHeroDamagers.entrySet()) {
            Hero assister = entry.getKey();
            float dmgTime = entry.getValue();
            if (assister != effectiveHeroKiller && assister.team != this.team && (matchTime - dmgTime <= 20.0f)) {
                assister.assists++;
                assister.addGold(150);
                assister.addXP(120, engine);
                if (assister.isPlayer) {
                    SoundManager.playCoin();
                    engine.addParticle(Particle.createGoldPopup(x, y, z + 0.3f, 150));
                }
            }
        }
        recentHeroDamagers.clear();

        // Broadcast Kill Banner & Killfeed Notification
        engine.addKillEvent(killerName, this.heroName, killerIsBlue, this.team == TEAM_BLUE, this.isPlayer);

        for (int i = 0; i < 25; i++) {
            engine.addParticle(Particle.createExplosionDebris(x, y, 0.5f, team == TEAM_BLUE ? Color.CYAN : Color.RED));
        }
    }

    public void respawn(GameEngine engine) {
        alive = true;
        x = spawnX;
        y = spawnY;
        destinationX = spawnX;
        destinationY = spawnY;
        hp = maxHp + bonusHp;
        mana = maxMana;
        targetEntity = null;
        currentWpIndex = 0;
        shieldHp = 0f;
        isMoving = false;
        lastHeroDamager = null;
        lastHeroDamageTime = -999.0f;
        recentHeroDamagers.clear();
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;

        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        if (isRecalling) {
            g.setColor(new Color(60, 200, 255, 120));
            int rw = (int) (32 * (1.0f - recallTimer / 4.0f));
            g.drawOval(sp.x - rw, sp.y - rw / 2, rw * 2, rw);
        }

        // Bush Stealth Camouflage Aura (for player / allies in grass)
        if (isInBush) {
            g.setColor(new Color(40, 220, 80, 70));
            g.fillOval(sp.x - 20, sp.y - 10, 40, 20);
            g.setColor(new Color(80, 255, 120, 180));
            g.drawOval(sp.x - 20, sp.y - 10, 40, 20);
        }

        // Turtle Shield Aura
        if (shieldHp > 0) {
            g.setColor(new Color(80, 220, 255, 60));
            g.fillOval(sp.x - 18, sp.y - 28, 36, 42);
            g.setColor(new Color(120, 240, 255, 180));
            g.drawOval(sp.x - 18, sp.y - 28, 36, 42);
        }

        // Blue & Red Magical Buff Rings at feet
        VoxelRenderer.drawHeroBuffRings(g, sp.x, sp.y, walkAnim, blueBuffTimer > 0, redBuffTimer > 0);

        // Draw Voxel Hero (with translucent camouflage in bush)
        Composite origComp = g.getComposite();
        if (isInBush) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }

        VoxelRenderer.drawVoxelHero(g, sp.x, sp.y, heroName, team == TEAM_RED, isMoving ? walkAnim : 0f, facingAngle, team);

        if (isInBush) {
            g.setComposite(origComp);
            // Overlapping front grass layer so hero is visibly nestled deep inside tall grass
            VoxelRenderer.drawTallGrassFront(g, sp.x, sp.y + 4, walkAnim);
        }

        // Name and Level Tag
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        String tag = heroName + " (Lv." + level + ") [" + assignedLane.name() + "]";
        int tw = g.getFontMetrics().stringWidth(tag);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(sp.x - tw / 2 - 2, sp.y - 58, tw + 4, 12);
        g.setColor(isPlayer ? Color.CYAN : (team == TEAM_BLUE ? new Color(100, 220, 255) : new Color(255, 100, 100)));
        g.drawString(tag, sp.x - tw / 2, sp.y - 48);

        // Stealth status indicator when hiding in bush
        if (isInBush) {
            String stealthTag = "👁 HIDDEN IN BUSH";
            int stw = g.getFontMetrics().stringWidth(stealthTag);
            g.setColor(new Color(0, 0, 0, 190));
            g.fillRect(sp.x - stw / 2 - 2, sp.y - 72, stw + 4, 12);
            g.setColor(new Color(60, 255, 120));
            g.drawString(stealthTag, sp.x - stw / 2, sp.y - 62);
        }

        // Health Bar & Shield Bar
        renderHealthAndShieldBar(g, sp.x, sp.y);
    }

    private void renderHealthAndShieldBar(Graphics2D g, int sx, int sy) {
        int barW = 34;
        int barH = 5;
        int bx = sx - barW / 2;
        int by = sy - 44;

        g.setColor(Color.BLACK);
        g.fillRect(bx - 1, by - 1, barW + 2, barH + 2);

        float actualMaxHp = maxHp + bonusHp;
        float pct = Math.max(0f, Math.min(1f, hp / actualMaxHp));

        g.setColor(team == TEAM_BLUE ? new Color(50, 220, 80) : new Color(240, 50, 50));
        g.fillRect(bx, by, (int) (barW * pct), barH);

        // Shield bar overlay (Yellow/Cyan)
        if (shieldHp > 0) {
            float shieldPct = Math.min(1f, shieldHp / actualMaxHp);
            g.setColor(new Color(255, 225, 70));
            g.fillRect(bx, by, (int) (barW * shieldPct), 2);
        }

        g.setColor(new Color(150, 150, 150));
        g.drawRect(bx, by, barW, barH);
    }
}
