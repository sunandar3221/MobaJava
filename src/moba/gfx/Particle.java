package moba.gfx;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Particle {
    public float x, y, z;
    public float vx, vy, vz;
    public Color color;
    public float size;
    public float life;
    public float maxLife;
    public String text = null;
    public boolean isCritStar = false;

    public Particle(float x, float y, float z, float vx, float vy, float vz, Color color, float size, float life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.color = color;
        this.size = size;
        this.life = life;
        this.maxLife = life;
    }

    public static Particle createDamagePopup(float x, float y, float z, int damage, boolean isCrit) {
        Particle p = new Particle(x, y, z + 24, (float)(Math.random() - 0.5) * 0.5f, (float)(Math.random() - 0.5) * 0.5f, 1.2f,
                isCrit ? new Color(255, 60, 60) : Color.WHITE, isCrit ? 16 : 13, 0.8f);
        p.text = (isCrit ? "CRIT! -" : "-") + damage;
        return p;
    }

    public static Particle createHealPopup(float x, float y, float z, int heal) {
        Particle p = new Particle(x, y, z + 24, 0, 0, 1.0f, new Color(80, 255, 80), 14, 0.8f);
        p.text = "+" + heal;
        return p;
    }

    public static Particle createGoldPopup(float x, float y, float z, int gold) {
        Particle p = new Particle(x, y, z + 24, 0, 0, 1.1f, new Color(255, 215, 0), 14, 0.9f);
        p.text = "+$" + gold;
        return p;
    }

    public static Particle createCritStar(float x, float y, float z) {
        float angle = (float) (Math.random() * Math.PI * 2);
        float speed = 1.0f + (float) Math.random() * 2.0f;
        Particle p = new Particle(x, y, z + 12, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, 0.8f + (float)Math.random(),
                new Color(255, 215, 0), 6, 0.6f);
        p.isCritStar = true;
        return p;
    }

    public static Particle createExplosionDebris(float x, float y, float z, Color color) {
        float angle = (float) (Math.random() * Math.PI * 2);
        float speed = 1.5f + (float) Math.random() * 3.5f;
        return new Particle(x, y, z + 8, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed, 2.0f + (float)Math.random() * 2f,
                color, 4 + (float)Math.random() * 4, 0.7f + (float)Math.random() * 0.4f);
    }

    public boolean update(float dt) {
        life -= dt;
        x += vx * dt * 60;
        y += vy * dt * 60;
        z += vz * dt * 60;
        vz -= 2.0f * dt; // Gravity
        return life > 0;
    }

    public void render(Graphics2D g, int screenX, int screenY) {
        float alpha = Math.max(0f, Math.min(1f, life / maxLife));
        if (text != null) {
            g.setFont(new Font("Monospaced", Font.BOLD, (int) size));
            // Shadow
            g.setColor(new Color(0, 0, 0, (int)(alpha * 200)));
            g.drawString(text, screenX + 1, screenY + 1);
            // Main text
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g.drawString(text, screenX, screenY);
        } else if (isCritStar) {
            // Star/Cross shape (Minecraft critical hit effect)
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            int s = (int) size;
            g.fillRect(screenX - s / 2, screenY - s / 2, s, s);
            g.setColor(new Color(255, 255, 255, (int)(alpha * 255)));
            g.fillRect(screenX - 1, screenY - s, 2, s * 2);
            g.fillRect(screenX - s, screenY - 1, s * 2, 2);
        } else {
            // Voxel cube debris
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            int s = (int) size;
            g.fillRect(screenX - s / 2, screenY - s / 2, s, s);
            g.setColor(new Color(0, 0, 0, (int)(alpha * 120)));
            g.drawRect(screenX - s / 2, screenY - s / 2, s, s);
        }
    }
}
