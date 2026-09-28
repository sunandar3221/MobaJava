package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class SummonedLord extends Entity {
    public final Hero.Lane assignedLane;
    public Entity targetEntity = null;
    private final List<Point> waypoints = new ArrayList<>();
    private int currentWpIndex = 0;
    public float anim = 0f;
    public Entity lastDisturber = null;
    public float lastDisturberTime = -999.0f;

    public SummonedLord(float x, float y, int team, List<Point> path, Hero.Lane lane) {
        super(x, y, team, 4600f);
        this.assignedLane = (lane != null) ? lane : Hero.Lane.MID;
        this.radius = 1.3f;
        this.moveSpeed = 2.4f;
        this.attackRange = 2.4f;
        this.attackDamage = 135f;
        this.attackSpeed = 0.7f;
        if (path != null) {
            this.waypoints.addAll(path);
        }
    }

    public SummonedLord(float x, float y, int team, List<Point> path) {
        this(x, y, team, path, Hero.Lane.MID);
    }

    public List<Point> getWaypoints() {
        return waypoints;
    }

    public int getCurrentWpIndex() {
        return currentWpIndex;
    }

    @Override
    public void takeDamage(float dmg, Entity source, GameEngine engine) {
        super.takeDamage(dmg, source, engine);
        if (source != null && source.alive && source.team != this.team) {
            this.lastDisturber = source;
            this.lastDisturberTime = engine.getMatchTime();
        }
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) return;

        anim += dt * 2.5f;
        if (attackCooldown > 0) attackCooldown -= dt;

        // 1. Target acquisition:
        // Priority A: Enemy Turrets or Base Core in range (<= 8.0f) - ALWAYS highest priority!
        Entity priorityTarget = null;
        float minTowerDist = 8.0f;
        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != this.team && (e instanceof Tower || e instanceof BaseCore)) {
                float d = distanceTo(e);
                if (d < minTowerDist) {
                    minTowerDist = d;
                    priorityTarget = e;
                }
            }
        }

        // Priority B: If no turret in range, fight back only if disturbed ("bunuh musuh kalau digangguin")
        if (priorityTarget == null) {
            float matchTime = engine.getMatchTime();
            // Check if recently damaged by an enemy (within last 3.5s) who is still close by (<= 3.8f)
            if (lastDisturber != null && lastDisturber.alive && lastDisturber.team != this.team && (matchTime - lastDisturberTime <= 3.5f)) {
                if (distanceTo(lastDisturber) <= 3.8f) {
                    priorityTarget = lastDisturber;
                } else {
                    lastDisturber = null; // Disturber ran away, refuse to chase off lane!
                }
            }

            // Check if an enemy hero or minion is in direct melee contact blocking path (dist <= attackRange + 0.6f)
            if (priorityTarget == null) {
                float minEnemyDist = attackRange + 0.6f;
                for (Entity e : engine.getAllEntities()) {
                    if (e.alive && e.team != this.team && (e instanceof Hero || e instanceof Minion)) {
                        float d = distanceTo(e);
                        if (d < minEnemyDist) {
                            minEnemyDist = d;
                            priorityTarget = e;
                        }
                    }
                }
            }
        }

        targetEntity = priorityTarget;

        // 2. Attack or March strictly along assigned lane
        if (targetEntity != null && targetEntity.alive) {
            float dist = distanceTo(targetEntity);
            if (dist <= attackRange) {
                isMoving = false;
                if (attackCooldown <= 0) {
                    performLordAttack(targetEntity, engine);
                }
            } else {
                // Step towards target ONLY if it's a turret OR within close tether (<= 3.8f)
                // Refuse to chase kiting enemies off lane!
                if (dist <= 3.8f || targetEntity instanceof Tower || targetEntity instanceof BaseCore) {
                    isMoving = true;
                    float dx = targetEntity.x - x;
                    float dy = targetEntity.y - y;
                    facingAngle = (float) Math.atan2(dy, dx);
                    x += (dx / dist) * moveSpeed * dt;
                    y += (dy / dist) * moveSpeed * dt;
                } else {
                    targetEntity = null; // Break aggro immediately
                }
            }
        } else {
            // March strictly along assigned lane waypoints towards enemy base!
            if (currentWpIndex < waypoints.size()) {
                Point wp = waypoints.get(currentWpIndex);
                float dx = wp.x - x;
                float dy = wp.y - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < 1.2f) {
                    currentWpIndex++;
                } else {
                    isMoving = true;
                    facingAngle = (float) Math.atan2(dy, dx);
                    x += (dx / dist) * moveSpeed * dt;
                    y += (dy / dist) * moveSpeed * dt;
                }
            } else {
                isMoving = false;
            }
        }
    }

    private void performLordAttack(Entity target, GameEngine engine) {
        attackCooldown = 1.0f / attackSpeed;
        SoundManager.playExplosion();

        // Heavy ground shockwave damage
        target.takeDamage(attackDamage, this, engine);
        engine.addParticle(Particle.createDamagePopup(target.x, target.y, target.z, (int) attackDamage, true));

        // Area shockwave particles
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI / 16) * i;
            engine.addParticle(new Particle(
                    x + (float) Math.cos(angle) * 1.5f,
                    y + (float) Math.sin(angle) * 1.5f,
                    0.2f, 0, 0, 1.2f,
                    team == TEAM_BLUE ? new Color(60, 220, 255) : new Color(255, 60, 60),
                    8, 0.6f
            ));
        }
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        SoundManager.playExplosion();
        for (int i = 0; i < 35; i++) {
            engine.addParticle(Particle.createExplosionDebris(x, y, 1.5f, team == TEAM_BLUE ? Color.CYAN : Color.ORANGE));
        }
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        VoxelRenderer.drawLordBoss(g, sp.x, sp.y, anim, team == TEAM_BLUE);

        // Name, Lane and Health bar positioned for colossal Lord scale
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(team == TEAM_BLUE ? new Color(80, 220, 255) : new Color(255, 80, 80));
        String title = (team == TEAM_BLUE ? "Allied Lord" : "Enemy Lord") + " [" + assignedLane.name() + " LANE]";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, sp.x - tw / 2, sp.y - 142);

        renderHealthBar(g, sp.x, sp.y, 110, 134);
    }
}
