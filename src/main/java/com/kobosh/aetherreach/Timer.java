package com.kobosh.aetherreach;

public class Timer {
    private static final long NS_PER_SEC = 1_000_000_000L;
    private static final int MAX_TICKS = 100;

    private final float targetTps;
    private long prevTime;

    public int ticks;
    public float a;
    public float fps;
    public float passedTime;
    public float timeScale = 1.0F;

    public Timer(float ticksPerSecond) {
        this.targetTps = ticksPerSecond;
        this.prevTime = System.nanoTime();
    }

    public void advanceTime() {
        long now = System.nanoTime();
        long elapsed = now - prevTime;
        prevTime = now;

        if (elapsed < 0) elapsed = 0;
        if (elapsed > NS_PER_SEC) elapsed = NS_PER_SEC;

        fps = (float) NS_PER_SEC / elapsed;
        passedTime += (float) elapsed * timeScale * targetTps / (float) NS_PER_SEC;

        ticks = (int) passedTime;
        if (ticks > MAX_TICKS) ticks = MAX_TICKS;

        passedTime -= ticks;
        a = passedTime;
    }
}
