package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;

import java.awt.Color;

public class Skill {
    public enum Slot { Q, W, E, R }

    public Slot slot;
    public String name;
    public String description;
    public float baseCooldown;
    public float currentCooldown = 0f;
    public int manaCost;
    public float range;
    public boolean targetRequired;

    public Skill(Slot slot, String name, String description, float cooldown, int manaCost, float range, boolean targetRequired) {
        this.slot = slot;
        this.name = name;
        this.description = description;
        this.baseCooldown = cooldown;
        this.manaCost = manaCost;
        this.range = range;
        this.targetRequired = targetRequired;
    }

    public boolean isReady() {
        return currentCooldown <= 0f;
    }

    public boolean isUnlocked(Hero caster) {
        if (slot == Slot.R) return caster != null && caster.level >= 4;
        return true;
    }

    public void update(float dt) {
        if (currentCooldown > 0f) {
            currentCooldown -= dt;
            if (currentCooldown < 0f) currentCooldown = 0f;
        }
    }

    public boolean cast(Hero caster, float targetX, float targetY, Entity targetEntity, GameEngine engine) {
        if (!isReady()) return false;
        if (slot == Slot.R && (caster == null || caster.level < 4)) return false;
        if (caster.mana < manaCost) return false;

        caster.mana -= manaCost;
        currentCooldown = baseCooldown;

        switch (slot) {
            case Q -> {
                // Diamond Whirlwind (Instant AoE spin)
                SoundManager.playSwordSwing();
                float aoe = 2.8f;
                float dmg = 48f + caster.level * 14f + caster.bonusDamage * 0.8f;

                for (Entity e : engine.getAllEntities()) {
                    if (e.alive && e.team != caster.team && caster.distanceTo(e) <= aoe) {
                        e.takeDamage(dmg, caster, engine);
                        engine.addParticle(Particle.createDamagePopup(e.x, e.y, e.z, (int) dmg, true));
                        for (int i = 0; i < 5; i++) {
                            engine.addParticle(Particle.createCritStar(e.x, e.y, e.z));
                        }
                    }
                }
                // Whirlwind particle ring around caster
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    float px = caster.x + (float) Math.cos(angle) * 1.5f;
                    float py = caster.y + (float) Math.sin(angle) * 1.5f;
                    engine.addParticle(new Particle(px, py, 0.8f, (float)Math.cos(angle)*1.2f, (float)Math.sin(angle)*1.2f, 0.4f,
                            new Color(40, 240, 230), 8, 0.35f));
                }
                return true;
            }
            case W -> {
                // TNT Toss
                SoundManager.playShoot();
                float dmg = 75f + caster.level * 18f + caster.bonusDamage * 1.0f;
                Projectile tnt = new Projectile(caster, targetX, targetY, Projectile.Type.TNT_BOMB, dmg, 8.5f);
                engine.addProjectile(tnt);
                return true;
            }
            case E -> {
                // Ender Pearl (Blink)
                SoundManager.playTeleport();
                float dx = targetX - caster.x;
                float dy = targetY - caster.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float maxDist = range;
                if (dist > maxDist) {
                    dx = (dx / dist) * maxDist;
                    dy = (dy / dist) * maxDist;
                }
                float destX = caster.x + dx;
                float destY = caster.y + dy;

                if (engine.getMap().isWalkable(destX, destY)) {
                    // Spawn teleport particles at old pos
                    for (int i = 0; i < 12; i++) {
                        engine.addParticle(Particle.createExplosionDebris(caster.x, caster.y, 0.5f, new Color(130, 40, 220)));
                    }
                    caster.x = destX;
                    caster.y = destY;
                    caster.destinationX = destX;
                    caster.destinationY = destY;
                    caster.speedBuffTimer = 3.0f; // Speed boost after blink
                    // Spawn particles at new pos
                    for (int i = 0; i < 12; i++) {
                        engine.addParticle(Particle.createExplosionDebris(caster.x, caster.y, 0.5f, new Color(40, 240, 180)));
                    }
                    return true;
                }
                return false;
            }
            case R -> {
                // Redstone Titan Shockwave Ultimate
                SoundManager.playExplosion();
                float ultRadius = 4.8f;
                float dmg = 150f + caster.level * 32f + caster.bonusDamage * 1.4f;

                for (Entity e : engine.getAllEntities()) {
                    if (e.alive && e.team != caster.team && caster.distanceTo(e) <= ultRadius) {
                        e.takeDamage(dmg, caster, engine);
                        engine.addParticle(Particle.createDamagePopup(e.x, e.y, e.z, (int) dmg, true));
                        if (e instanceof Hero targetHero) {
                            targetHero.stunTimer = 1.8f; // Stun hero!
                        }
                    }
                }
                // Shockwave redstone particles
                for (int i = 0; i < 28; i++) {
                    double angle = (Math.PI * 2 / 28) * i;
                    float px = caster.x + (float) Math.cos(angle) * 3.0f;
                    float py = caster.y + (float) Math.sin(angle) * 3.0f;
                    engine.addParticle(new Particle(px, py, 0.2f, (float)Math.cos(angle)*2.5f, (float)Math.sin(angle)*2.5f, 0.8f,
                            new Color(255, 30, 30), 10, 0.6f));
                }
                return true;
            }
        }
        return false;
    }
}
