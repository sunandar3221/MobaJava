package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

public class BuffMonster extends Entity {
    public enum BuffType {
        BLUE,
        RED
    }

    public final BuffType buffType;
    public final String monsterName;
    public final float initialX, initialY;
    public Entity targetEntity = null;
    public float respawnTimer = 0f;
    public float anim = 0f;

    public BuffMonster(float x, float y, BuffType type) {
        super(x, y, TEAM_NEUTRAL, 1300f);
        this.buffType = type;
        this.monsterName = (type == BuffType.BLUE) ? "Lapis Golem (Blue Buff)" : "Magma Fiend (Red Buff)";
        this.initialX = x;
        this.initialY = y;
        this.radius = 1.0f;
        this.moveSpeed = 1.8f;
        this.attackRange = 2.0f;
        this.attackDamage = 28f;
        this.attackSpeed = 0.85f;
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) {
            respawnTimer -= dt;
            if (respawnTimer <= 0) {
                alive = true;
                hp = maxHp;
                x = initialX;
                y = initialY;
                targetEntity = null;
            }
            return;
        }

        anim += dt * 2.5f;
        if (attackCooldown > 0) attackCooldown -= dt;

        // Combat behavior
        if (targetEntity != null) {
            if (!targetEntity.alive || distanceTo(initialX, initialY) > 8.0f) {
                targetEntity = null;
            } else {
                float dist = distanceTo(targetEntity);
                if (dist <= attackRange) {
                    isMoving = false;
                    if (attackCooldown <= 0) {
                        attackCooldown = 1.0f / attackSpeed;
                        SoundManager.playHit();
                        targetEntity.takeDamage(attackDamage, this, engine);
                        engine.addParticle(Particle.createDamagePopup(targetEntity.x, targetEntity.y, targetEntity.z, (int) attackDamage, false));
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
                hp = Math.min(maxHp, hp + 50f * dt);
            } else {
                isMoving = false;
            }
        }
    }

    @Override
    public void takeDamage(float dmg, Entity source, GameEngine engine) {
        super.takeDamage(dmg, source, engine);
        if (source != null && source.alive) {
            this.targetEntity = source;
        }
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        SoundManager.playLevelUp();
        respawnTimer = 75.0f; // Respawns in 75 seconds

        if (killer instanceof Hero hero) {
            hero.addGold(120);
            hero.addXP(160, engine);

            if (buffType == BuffType.BLUE) {
                hero.blueBuffTimer = 75.0f;
                engine.addKillEvent(hero.heroName, "Blue Buff", hero.team == TEAM_BLUE, false, false);
                for (int i = 0; i < 20; i++) {
                    engine.addParticle(Particle.createCritStar(hero.x, hero.y, hero.z));
                }
            } else {
                hero.redBuffTimer = 75.0f;
                engine.addKillEvent(hero.heroName, "Red Buff", hero.team == TEAM_BLUE, false, false);
                for (int i = 0; i < 20; i++) {
                    engine.addParticle(Particle.createExplosionDebris(hero.x, hero.y, 1.2f, Color.ORANGE));
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            engine.addParticle(Particle.createExplosionDebris(x, y, 1.0f, buffType == BuffType.BLUE ? Color.CYAN : Color.RED));
        }
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        if (buffType == BuffType.BLUE) {
            VoxelRenderer.drawBlueBuffMonster(g, sp.x, sp.y, anim);
        } else {
            VoxelRenderer.drawRedBuffMonster(g, sp.x, sp.y, anim);
        }

        // Name and Health bar
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(buffType == BuffType.BLUE ? new Color(100, 210, 255) : new Color(255, 120, 80));
        int tw = g.getFontMetrics().stringWidth(monsterName);
        g.drawString(monsterName, sp.x - tw / 2, sp.y - 60);

        renderHealthBar(g, sp.x, sp.y, 50, 54);
    }
}
