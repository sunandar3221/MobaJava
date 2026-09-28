package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

public class Projectile {
    public enum Type { ARROW, TURRET_ENERGY, TNT_BOMB, MAGIC_ORB }

    public float x, y, z;
    public float targetX, targetY;
    public Entity targetEntity;
    public Entity owner;
    public Type type;
    public float damage;
    public float speed;
    public boolean active = true;
    public float lifeTime = 3.0f;

    // Ballistics for TNT
    public float arcProgress = 0f;
    public float startX, startY;
    public float peakHeight = 2.0f;

    public Projectile(Entity owner, Entity target, Type type, float damage, float speed) {
        this.owner = owner;
        this.targetEntity = target;
        this.x = owner.x;
        this.y = owner.y;
        this.z = 1.0f;
        this.startX = x;
        this.startY = y;
        this.targetX = target.x;
        this.targetY = target.y;
        this.type = type;
        this.damage = damage;
        this.speed = speed;
    }

    public Projectile(Entity owner, float tx, float ty, Type type, float damage, float speed) {
        this.owner = owner;
        this.x = owner.x;
        this.y = owner.y;
        this.z = 1.0f;
        this.startX = x;
        this.startY = y;
        this.targetX = tx;
        this.targetY = ty;
        this.type = type;
        this.damage = damage;
        this.speed = speed;
    }

    public void update(float dt, GameEngine engine) {
        lifeTime -= dt;
        if (lifeTime <= 0) {
            active = false;
            return;
        }

        if (targetEntity != null && targetEntity.alive) {
            targetX = targetEntity.x;
            targetY = targetEntity.y;
        }

        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (type == Type.TNT_BOMB) {
            // Parabolic arc throw
            float totalDist = (float) Math.sqrt((targetX - startX) * (targetX - startX) + (targetY - startY) * (targetY - startY));
            if (totalDist > 0.1f) {
                arcProgress += (speed * dt) / totalDist;
                x = startX + (targetX - startX) * arcProgress;
                y = startY + (targetY - startY) * arcProgress;
                z = (float) Math.sin(arcProgress * Math.PI) * peakHeight;
            }
            if (arcProgress >= 1.0f) {
                detonateTNT(engine);
                active = false;
            }
        } else {
            // Direct homing or linear travel
            float step = speed * dt;
            if (dist <= step || (targetEntity != null && dist < targetEntity.radius + 0.3f)) {
                hitTarget(engine);
                active = false;
            } else {
                x += (dx / dist) * step;
                y += (dy / dist) * step;
            }
        }
    }

    private void hitTarget(GameEngine engine) {
        if (targetEntity != null && targetEntity.alive) {
            targetEntity.takeDamage(damage, owner, engine);
            engine.addParticle(Particle.createDamagePopup(targetEntity.x, targetEntity.y, targetEntity.z, (int) damage, false));
            for (int i = 0; i < 4; i++) {
                engine.addParticle(Particle.createCritStar(targetEntity.x, targetEntity.y, targetEntity.z));
            }
            SoundManager.playHit();
        }
    }

    private void detonateTNT(GameEngine engine) {
        SoundManager.playExplosion();
        // AOE damage in 3.5 block radius
        float aoeRadius = 3.5f;
        for (Entity e : engine.getAllEntities()) {
            if (e.alive && e.team != owner.team) {
                float d = (float) Math.sqrt((e.x - x) * (e.x - x) + (e.y - y) * (e.y - y));
                if (d <= aoeRadius) {
                    float factor = 1.0f - (d / aoeRadius) * 0.4f;
                    float dmg = damage * factor;
                    e.takeDamage(dmg, owner, engine);
                    engine.addParticle(Particle.createDamagePopup(e.x, e.y, e.z, (int) dmg, true));
                }
            }
        }
        // Explosion particles
        for (int i = 0; i < 20; i++) {
            Color c = (i % 2 == 0) ? new Color(255, 120, 20) : new Color(220, 50, 30);
            engine.addParticle(Particle.createExplosionDebris(x, y, 0.5f, c));
        }
    }

    public void render(Graphics2D g, float camX, float camY) {
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        switch (type) {
            case ARROW -> {
                g.setColor(new Color(139, 69, 19));
                g.fillRect(sp.x - 2, sp.y - 2, 4, 4);
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(sp.x - 1, sp.y - 4, 2, 3);
            }
            case TURRET_ENERGY -> {
                // Glowing plasma bolt
                g.setColor(new Color(255, 80, 80, 150));
                g.fillOval(sp.x - 8, sp.y - 8, 16, 16);
                g.setColor(Color.WHITE);
                g.fillOval(sp.x - 4, sp.y - 4, 8, 8);
            }
            case TNT_BOMB -> {
                // Red TNT cube with white band
                g.setColor(new Color(200, 30, 30));
                g.fillRect(sp.x - 7, sp.y - 7, 14, 14);
                g.setColor(Color.WHITE);
                g.fillRect(sp.x - 7, sp.y - 2, 14, 4);
                g.setColor(Color.BLACK);
                g.fillRect(sp.x - 2, sp.y - 1, 4, 2);
            }
            case MAGIC_ORB -> {
                g.setColor(new Color(140, 40, 255, 180));
                g.fillOval(sp.x - 7, sp.y - 7, 14, 14);
                g.setColor(Color.CYAN);
                g.fillOval(sp.x - 3, sp.y - 3, 6, 6);
            }
        }
    }
}
