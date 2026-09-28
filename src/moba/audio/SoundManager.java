package moba.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * High-performance, zero-dropout procedural retro audio synthesizer for Minecraft MOBA.
 * Uses a single dedicated continuous software-mixed audio line, completely eliminating
 * line handle exhaustion, thread starvation, and audio cut-outs.
 */
public class SoundManager {
    private static volatile boolean soundEnabled = true;
    private static final float SAMPLE_RATE = 22050f;
    private static final int CHUNK_SIZE = 512;

    enum WaveType { SINE, SQUARE, TRIANGLE, SAWTOOTH }

    private static class PlayingSound {
        final byte[] data;
        int offset = 0;

        PlayingSound(byte[] data) {
            this.data = data;
        }
    }

    private static final Object lock = new Object();
    private static final List<PlayingSound> activeSounds = new ArrayList<>();
    private static SourceDataLine mixerLine = null;
    private static boolean mixerInitialized = false;

    // Pre-synthesized procedural waveforms (pure memory, zero hardware dependency)
    private static final byte[] HIT_DATA = generateToneSweep(180, 50, 0.12f, 0.6f, WaveType.SQUARE);
    private static final byte[] SWORD_DATA = generateNoise(0.08f, 0.35f);
    private static final byte[] SHOOT_DATA = generateToneSweep(600, 220, 0.10f, 0.45f, WaveType.TRIANGLE);
    private static final byte[] EXPLOSION_DATA = generateNoiseDecay(0.35f, 0.85f);
    private static final byte[] LEVEL_UP_DATA = generateLevelUpArpeggio();
    private static final byte[] COIN_DATA = generateCoinChime();
    private static final byte[] TELEPORT_DATA = generateToneSweep(200, 900, 0.18f, 0.55f, WaveType.SINE);
    private static final byte[] TOWER_SHOT_DATA = generateToneSweep(800, 300, 0.12f, 0.55f, WaveType.SAWTOOTH);

    static {
        // Start background mixer daemon thread
        Thread mixerThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    mixerLine = (SourceDataLine) AudioSystem.getLine(info);
                    mixerLine.open(format, 4096);
                    mixerLine.start();
                    mixerInitialized = true;
                }
            } catch (Throwable t) {
                mixerLine = null;
                mixerInitialized = false;
            }

            if (mixerLine == null) return;

            byte[] outBuffer = new byte[CHUNK_SIZE];
            int[] mixAccumulator = new int[CHUNK_SIZE];

            while (true) {
                boolean hasAudio = false;

                synchronized (lock) {
                    while (activeSounds.isEmpty() || !soundEnabled) {
                        try {
                            lock.wait(50);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    // Reset accumulator
                    for (int i = 0; i < CHUNK_SIZE; i++) {
                        mixAccumulator[i] = 0;
                    }

                    Iterator<PlayingSound> it = activeSounds.iterator();
                    while (it.hasNext()) {
                        PlayingSound ps = it.next();
                        int remaining = ps.data.length - ps.offset;
                        int toMix = Math.min(CHUNK_SIZE, remaining);

                        for (int i = 0; i < toMix; i++) {
                            mixAccumulator[i] += ps.data[ps.offset + i];
                        }

                        ps.offset += toMix;
                        if (ps.offset >= ps.data.length) {
                            it.remove();
                        }
                        hasAudio = true;
                    }
                }

                if (hasAudio) {
                    // Clamp and write to audio line
                    for (int i = 0; i < CHUNK_SIZE; i++) {
                        int v = mixAccumulator[i];
                        if (v > 127) v = 127;
                        else if (v < -128) v = -128;
                        outBuffer[i] = (byte) v;
                    }
                    try {
                        mixerLine.write(outBuffer, 0, CHUNK_SIZE);
                    } catch (Throwable ignored) {}
                }
            }
        }, "CraftArena-SoundMixer");

        mixerThread.setDaemon(true);
        mixerThread.start();
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    private static void play(byte[] data) {
        if (!soundEnabled || data == null) return;
        synchronized (lock) {
            // Cap concurrent sounds to 12 to maintain crisp mix
            if (activeSounds.size() < 12) {
                activeSounds.add(new PlayingSound(data));
                lock.notify();
            }
        }
    }

    public static void playHit() {
        play(HIT_DATA);
    }

    public static void playSwordSwing() {
        play(SWORD_DATA);
    }

    public static void playShoot() {
        play(SHOOT_DATA);
    }

    public static void playExplosion() {
        play(EXPLOSION_DATA);
    }

    public static void playLevelUp() {
        play(LEVEL_UP_DATA);
    }

    public static void playCoin() {
        play(COIN_DATA);
    }

    public static void playTeleport() {
        play(TELEPORT_DATA);
    }

    public static void playTowerShot() {
        play(TOWER_SHOT_DATA);
    }

    // --- Waveform Generators ---

    private static byte[] generateToneSweep(float startFreq, float endFreq, float durationSec, float volume, WaveType wave) {
        int numSamples = (int) (durationSec * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            float progress = (float) i / numSamples;
            float currentFreq = startFreq + (endFreq - startFreq) * progress;
            float t = (float) i / SAMPLE_RATE;
            float sample = 0;
            float phase = (t * currentFreq) % 1.0f;
            switch (wave) {
                case SINE -> sample = (float) Math.sin(2 * Math.PI * currentFreq * t);
                case SQUARE -> sample = phase < 0.5f ? 1.0f : -1.0f;
                case TRIANGLE -> sample = 2.0f * Math.abs(2.0f * (phase - 0.5f)) - 1.0f;
                case SAWTOOTH -> sample = 2.0f * phase - 1.0f;
            }
            float env = 1.0f - progress;
            buffer[i] = (byte) (sample * env * volume * 127f);
        }
        return buffer;
    }

    private static byte[] generateNoise(float durationSec, float volume) {
        int numSamples = (int) (durationSec * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples];
        Random r = new Random(42);
        for (int i = 0; i < numSamples; i++) {
            float env = 1.0f - ((float) i / numSamples);
            float sample = (r.nextFloat() * 2f - 1f) * env * volume;
            buffer[i] = (byte) (sample * 127f);
        }
        return buffer;
    }

    private static byte[] generateNoiseDecay(float durationSec, float volume) {
        int numSamples = (int) (durationSec * SAMPLE_RATE);
        byte[] buffer = new byte[numSamples];
        Random r = new Random(1337);
        float lastSample = 0;
        for (int i = 0; i < numSamples; i++) {
            float progress = (float) i / numSamples;
            float raw = r.nextFloat() * 2f - 1f;
            lastSample = lastSample * 0.7f + raw * 0.3f;
            float env = (1.0f - progress) * (1.0f - progress);
            buffer[i] = (byte) (lastSample * env * volume * 127f);
        }
        return buffer;
    }

    private static byte[] generateLevelUpArpeggio() {
        float[] freqs = { 523.25f, 659.25f, 783.99f, 1046.50f };
        float[] durs  = { 0.08f, 0.08f, 0.08f, 0.20f };
        float[] vols  = { 0.45f, 0.45f, 0.45f, 0.55f };

        int totalSamples = 0;
        for (float d : durs) totalSamples += (int)(d * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples];

        int offset = 0;
        for (int note = 0; note < freqs.length; note++) {
            int noteSamples = (int)(durs[note] * SAMPLE_RATE);
            float freq = freqs[note];
            float vol = vols[note];
            for (int i = 0; i < noteSamples; i++) {
                float t = (float) i / SAMPLE_RATE;
                float sample = (float) Math.sin(2 * Math.PI * freq * t);
                float env = 1.0f - ((float) i / noteSamples) * 0.3f;
                buffer[offset + i] = (byte) (sample * env * vol * 127f);
            }
            offset += noteSamples;
        }
        return buffer;
    }

    private static byte[] generateCoinChime() {
        float[] freqs = { 987.77f, 1318.51f };
        float[] durs  = { 0.05f, 0.12f };
        float[] vols  = { 0.4f, 0.4f };

        int totalSamples = 0;
        for (float d : durs) totalSamples += (int)(d * SAMPLE_RATE);
        byte[] buffer = new byte[totalSamples];

        int offset = 0;
        for (int note = 0; note < freqs.length; note++) {
            int noteSamples = (int)(durs[note] * SAMPLE_RATE);
            float freq = freqs[note];
            float vol = vols[note];
            for (int i = 0; i < noteSamples; i++) {
                float t = (float) i / SAMPLE_RATE;
                float sample = (float) Math.sin(2 * Math.PI * freq * t);
                float env = 1.0f - ((float) i / noteSamples);
                buffer[offset + i] = (byte) (sample * env * vol * 127f);
            }
            offset += noteSamples;
        }
        return buffer;
    }
}
