package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

public class Tower extends Entity {
    public String towerName;
    public Entity targetEntity = null;
    public float pulseAnim = 0f;

    public Tower(String name, float x, float y, int team) {
        super(x, y, team, 2400f);
        this.towerName = name;
        this.radius = 1.2f;
        this.attackRange = 7.0f;
        this.attackDamage = 85f;
        this.attackSpeed = 0.8f;
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) return;

        pulseAnim += dt * 3f;
        if (attackCooldown > 0) attackCooldown -= dt;

        // Target validation
        if (targetEntity != null) {
            if (!targetEntity.alive || distanceTo(targetEntity) > attackRange) {
                targetEntity = null;
            }
        }

        // Acquire new target if null
        if (targetEntity == null) {
            targetEntity = selectTarget(engine);
        }

        // Fire turret shot
        if (targetEntity != null && attackCooldown <= 0) {
            attackCooldown = 1.0f / attackSpeed;
            SoundManager.playTowerShot();
            Projectile shot = new Projectile(this, targetEntity, Projectile.Type.TURRET_ENERGY, attackDamage, 10f);
            engine.addProjectile(shot);
        }
    }

    private Entity selectTarget(GameEngine engine) {
        Entity targetMinion = null;
        Entity targetHero = null;
        float minMinionDist = Float.MAX_VALUE;
        float minHeroDist = Float.MAX_VALUE;

        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != this.team) {
                float d = distanceTo(e);
                if (d <= attackRange) {
                    if (e instanceof Minion && d < minMinionDist) {
                        minMinionDist = d;
                        targetMinion = e;
                    } else if (e instanceof Hero && d < minHeroDist) {
                        minHeroDist = d;
                        targetHero = e;
                    }
                }
            }
        }
        // Turrets prioritize minions first unless aggroed by hero attacking allied hero
        return targetMinion != null ? targetMinion : targetHero;
    }

    public void setAggroOnHero(Hero hero) {
        if (distanceTo(hero) <= attackRange) {
            this.targetEntity = hero;
        }
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        SoundManager.playExplosion();
        // Give entire team gold
        for (Entity e : engine.getAllEntities()) {
            if (e instanceof Hero hero && hero.team != this.team) {
                hero.addGold(250);
                hero.addXP(300, engine);
            }
        }

        // Broadcast turret destruction announcement
        boolean killerIsBlue = (killer != null && killer.team == TEAM_BLUE);
        engine.addKillEvent(killerIsBlue ? "Blue Team" : "Red Team", this.towerName, killerIsBlue, this.team == TEAM_BLUE, false);

        // Giant explosion particles
        for (int i = 0; i < 35; i++) {
            Color c = (i % 2 == 0) ? new Color(60, 40, 80) : new Color(255, 100, 30);
            engine.addParticle(Particle.createExplosionDebris(x, y, 1.5f, c));
        }
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        // Turret targeting laser line
        if (targetEntity != null && targetEntity.alive) {
            Point tsp = VoxelRenderer.worldToScreen(targetEntity.x, targetEntity.y, targetEntity.z, camX, camY);
            if (targetEntity instanceof Hero h && h.isPlayer) {
                // Intense pulsating red laser beam targeting player!
                g.setColor(new Color(255, 30, 30, 220));
                g.drawLine(sp.x, sp.y - 70, tsp.x, tsp.y - 20);
                g.drawLine(sp.x + 1, sp.y - 70, tsp.x + 1, tsp.y - 20);
            } else if (targetEntity instanceof Minion) {
                // Calm amber laser targeting minion
                g.setColor(new Color(255, 180, 50, 100));
                g.drawLine(sp.x, sp.y - 70, tsp.x, tsp.y - 20);
            } else {
                g.setColor(new Color(255, 80, 80, 110));
                g.drawLine(sp.x, sp.y - 70, tsp.x, tsp.y - 20);
            }
        }

        VoxelRenderer.drawTower(g, sp.x, sp.y, team == TEAM_RED, pulseAnim);

        // Name and Health bar
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        int tw = g.getFontMetrics().stringWidth(towerName);
        g.drawString(towerName, sp.x - tw / 2, sp.y - 82);

        renderHealthBar(g, sp.x, sp.y, 50, 78);
    }
}
