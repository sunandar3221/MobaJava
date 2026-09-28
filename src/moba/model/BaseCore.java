package moba.model;

import moba.GameEngine;
import moba.audio.SoundManager;
import moba.gfx.Particle;
import moba.gfx.VoxelRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;

public class BaseCore extends Entity {
    public float pulseAnim = 0f;

    public BaseCore(float x, float y, int team) {
        super(x, y, team, 4500f);
        this.radius = 1.8f;
        this.attackRange = 0f; // Core does not attack directly
    }

    @Override
    public void update(float dt, GameEngine engine) {
        if (!alive) return;
        pulseAnim += dt * 2.5f;
    }

    @Override
    public void onDeath(Entity killer, GameEngine engine) {
        SoundManager.playExplosion();
        // Massive fireworks/explosion
        for (int i = 0; i < 60; i++) {
            Color c = (team == TEAM_BLUE) ? new Color(50, 180, 255) : new Color(255, 60, 40);
            engine.addParticle(Particle.createExplosionDebris(x, y, 1.0f, c));
        }
        engine.endGame(team != TEAM_BLUE); // If Red core died, Blue wins!
    }

    @Override
    public void render(Graphics2D g, float camX, float camY) {
        if (!alive) return;
        Point sp = VoxelRenderer.worldToScreen(x, y, z, camX, camY);

        VoxelRenderer.drawBaseCore(g, sp.x, sp.y, team == TEAM_RED, pulseAnim);

        String label = (team == TEAM_BLUE ? "BLUE NEXUS" : "RED NEXUS");
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        int tw = g.getFontMetrics().stringWidth(label);
        g.setColor(team == TEAM_BLUE ? new Color(100, 200, 255) : new Color(255, 100, 100));
        g.drawString(label, sp.x - tw / 2, sp.y - 75);

        renderHealthBar(g, sp.x, sp.y, 64, 70);
    }
}
