package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;

public class Minion extends Entity {
    public boolean isRanged;
    public List<Point> waypoints;
    public int currentWaypointIdx = 0;
    public Entity targetEntity = null;

    public Minion(float x, float y, int team, boolean isRanged, List<Point> waypoints) {
        super(x, y, team, isRanged ? 180f : 280f);
        this.isRanged = isRanged;
        this.waypoints = waypoints;
        this.moveSpeed = 2.8f;
        this.attackRange = isRanged ? 5.2f : 1.5f;
        this.attackDamage = isRanged ? 18f : 24f;
        this.attackSpeed = isRanged ? 0.85f : 1.0f;
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) return;

        if (attackCooldown > 0) attackCooldown -= dt;

        // If current target is a hero who entered a bush and is unrevealed, lose vision immediately!
        if (targetEntity != null && targetEntity instanceof Hero h && h.isInBush && h.revealTimer <= 0) {
            targetEntity = null;
        }

        // 1. Look for enemy target in aggro range (6.0 blocks)
        if (targetEntity == null || !targetEntity.alive || distanceTo(targetEntity) > 7.5f) {
            targetEntity = findNearestEnemy(engine);
        }

        // 2. If target found, engage
        if (targetEntity != null) {
            float dist = distanceTo(targetEntity);
            if (dist <= attackRange) {
                // In attack range
                isMoving = false;
                if (attackCooldown <= 0) {
                    performAttack(targetEntity, engine);
                }
            } else {
                // Move towards target
                moveTowards(targetEntity.x, targetEntity.y, dt, engine);
            }
            return;
        }

        // 3. Otherwise follow lane waypoints
        if (waypoints != null && currentWaypointIdx < waypoints.size()) {
            Point wp = waypoints.get(currentWaypointIdx);
            float distToWp = distanceTo(wp.x, wp.y);
            if (distToWp < 1.2f) {
                currentWaypointIdx++;
            } else {
                moveTowards(wp.x, wp.y, dt, engine);
            }
        } else {
            isMoving = false;
        }
    }

    private void moveTowards(float tx, float ty, float dt, GameEngine engine) {
        float dx = tx - x;
        float dy = ty - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.1f) {
            isMoving = true;
            walkAnim += dt * 8f;
            float step = moveSpeed * dt;
            float nx = x + (dx / dist) * Math.min(step, dist);
            float ny = y + (dy / dist) * Math.min(step, dist);

            if (engine.getMap().isWalkable(nx, ny)) {
                x = nx;
                y = ny;
            } else {
                x = nx;
                y = ny;
            }
        }
    }

    private void performAttack(Entity target, GameEngine engine) {
        attackCooldown = 1.0f / attackSpeed;

        if (isRanged) {
            SoundManager.playShoot();
            Projectile arrow = new Projectile(this, target, Projectile.Type.ARROW, attackDamage, 9.0f);
            engine.addProjectile(arrow);
        } else {
            SoundManager.playSwordSwing();
            target.takeDamage(attackDamage, this, engine);
            engine.addParticle(Particle.createDamagePopup(target.x, target.y, target.z, (int) attackDamage, false));
            SoundManager.playHit();
        }
    }

    private Entity findNearestEnemy(GameEngine engine) {
        Entity closest = null;
        float minDist = 6.5f;

        for (Entity e : engine.getAllEntities()) {
            // Minions only attack opposing team (not neutral jungle buffs or camps)
            if (e.alive && e.team != this.team && e.team != TEAM_NEUTRAL && !(e instanceof JungleCamp) && !(e instanceof BuffMonster)) {
                if (e instanceof Hero h && h.isInBush && h.revealTimer <= 0) {
                    continue; // Minions cannot see or target heroes in bush
                }
                float d = distanceTo(e);
                if (d < minDist) {
                    minDist = d;
                    closest = e;
                }
            }
        }
        return closest;
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        Hero effectiveHeroKiller = null;
        if (killer instanceof Hero hero && hero.team != this.team) {
            effectiveHeroKiller = hero;
        } else {
            float matchTime = engine.getMatchTime();
            if (lastHeroDamager != null && lastHeroDamager.alive && lastHeroDamager.team != this.team) {
                if (matchTime - lastHeroDamageTime <= 20.0f) {
                    effectiveHeroKiller = lastHeroDamager;
                }
            }
        }

        if (effectiveHeroKiller != null) {
            effectiveHeroKiller.creepScore++;
            effectiveHeroKiller.addGold(isRanged ? 25 : 20);
            effectiveHeroKiller.addXP(40, engine);
        }

        // Particle splash
        for (int i = 0; i < 8; i++) {
            engine.addParticle(Particle.createExplosionDebris(x, y, 0.4f, team == TEAM_BLUE ? Color.GREEN : Color.GRAY));
        }
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        VoxelRenderer.drawMinion(g, sp.x, sp.y, team == TEAM_RED, isRanged, isMoving ? walkAnim : 0f);
        renderHealthBar(g, sp.x, sp.y, 28, 36);
    }
}
