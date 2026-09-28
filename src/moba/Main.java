package moba;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CraftArena: Block Legends (Minecraft MOBA)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1024, 720);
            frame.setMinimumSize(new Dimension(800, 600));
            frame.setLocationRelativeTo(null);

            GameEngine engine = new GameEngine(1024, 720);

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    engine.setDimensions(getWidth(), getHeight());
                    engine.render((Graphics2D) g);
                }
            };
            panel.setFocusable(true);
            panel.addKeyListener(engine);
            panel.addMouseListener(engine);
            panel.addMouseMotionListener(engine);

            frame.add(panel);
            frame.setVisible(true);
            panel.requestFocusInWindow();

            // High-Performance 60 FPS Game Loop Thread
            Thread gameLoop = new Thread(() -> {
                final long OPTIMAL_FRAME_TIME_NS = 1_000_000_000L / 60L; // 16.66ms target for 60 FPS
                long lastTime = System.nanoTime();

                while (frame.isDisplayable()) {
                    long now = System.nanoTime();
                    long elapsedNs = now - lastTime;
                    lastTime = now;

                    float dt = elapsedNs / 1_000_000_000.0f;
                    // Cap dt to prevent physics tunneling or large time jumps on stutter
                    if (dt > 0.05f) dt = 0.05f;

                    // Update game state
                    engine.update(dt);

                    // Repaint panel and synchronize native graphics buffer to display immediately
                    panel.repaint();
                    Toolkit.getDefaultToolkit().sync();

                    long renderDuration = System.nanoTime() - now;
                    long sleepTimeNs = OPTIMAL_FRAME_TIME_NS - renderDuration;
                    if (sleepTimeNs > 0) {
                        try {
                            Thread.sleep(sleepTimeNs / 1_000_000L, (int) (sleepTimeNs % 1_000_000L));
                        } catch (InterruptedException ignored) {}
                    }
                }
            }, "GameLoopThread");
            gameLoop.setDaemon(true);
            gameLoop.start();
        });
    }
}
