package moba.model;

import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

public abstract class Entity {
    public static final int TEAM_BLUE = 0;
    public static final int TEAM_RED = 1;
    public static final int TEAM_NEUTRAL = 2;

    public float x, y, z;
    public int team;
    public float hp;
    public float maxHp;
    public float moveSpeed = 3.5f;
    public float attackRange = 1.8f;
    public float attackDamage = 25f;
    public float attackCooldown = 0f;
    public float attackSpeed = 1.0f; // attacks per second
    public float radius = 0.6f;
    public boolean alive = true;

    // Animation states
    public float walkAnim = 0f;
    public boolean isMoving = false;
    public float facingAngle = 0f;
    public Hero lastHeroDamager = null;
    public float lastHeroDamageTime = -999.0f;
    public final Map<Hero, Float> recentHeroDamagers = new HashMap<>();

    public Entity(float x, float y, int team, float maxHp) {
        this.x = x;
        this.y = y;
        this.z = 0;
        this.team = team;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public abstract void update(float dt, moba.GameEngine engine);
    public abstract void render(Graphics2D g, float camX, float camY);

    public void takeDamage(float dmg, Entity source, moba.GameEngine engine) {
        if (!alive) return;
        if (source instanceof Hero hero && hero.team != this.team) {
            this.lastHeroDamager = hero;
            this.lastHeroDamageTime = engine.getMatchTime();
            this.recentHeroDamagers.put(hero, engine.getMatchTime());
        }
        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            alive = false;
            onDeath(source, engine);
        }
    }

    public void heal(float amount) {
        if (!alive) return;
        hp = Math.min(maxHp, hp + amount);
    }

    public void onDeath(Entity killer, moba.GameEngine engine) {
        // Overridden by subclasses
    }

    public float distanceTo(Entity other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float distanceTo(float ox, float oy) {
        float dx = ox - this.x;
        float dy = oy - this.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void renderHealthBar(Graphics2D g, int sx, int sy, int barWidth, int yOffset) {
        int barHeight = 5;
        int barX = sx - barWidth / 2;
        int barY = sy - yOffset;

        // Background / Border (Minecraft dark stone border)
        g.setColor(new Color(20, 20, 20, 200));
        g.fillRect(barX - 1, barY - 1, barWidth + 2, barHeight + 2);

        // Red missing health
        g.setColor(new Color(150, 20, 20));
        g.fillRect(barX, barY, barWidth, barHeight);

        // Green current health
        float pct = Math.max(0f, Math.min(1f, hp / maxHp));
        Color hpColor = (team == TEAM_BLUE) ? new Color(50, 220, 50) : (team == TEAM_RED ? new Color(240, 50, 50) : new Color(230, 200, 40));
        g.setColor(hpColor);
        g.fillRect(barX, barY, (int) (barWidth * pct), barHeight);

        // Highlight shine
        g.setColor(new Color(255, 255, 255, 90));
        g.fillRect(barX, barY, (int) (barWidth * pct), 1);
    }
}
