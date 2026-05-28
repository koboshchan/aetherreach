package com.kobosh.aetherreach;

import com.kobosh.aetherreach.level.Level;
import com.kobosh.aetherreach.phys.AABB;
import java.util.List;
import org.lwjgl.input.Keyboard;
import java.util.ArrayList;

public class Player {
    private final Level world;

    public float xo, yo, zo;
    public float x, y, z;
    public float xd, yd, zd;
    public float yRot, xRot;
    public AABB bb;
    public boolean onGround = false;
    public boolean died = false;
    public Runnable onDeath = null;

    private static final float WIDTH  = 0.3F;
    private static final float HEIGHT = 0.9F;
    public static final float EYE_HEIGHT = 1.62F;

    public Player(Level world) {
        this.world = world;
        resetPos();
    }

    private void resetPos() {
        float spawnX = 128.5F;
        float spawnY = world.depth * 2 / 3 + 1 + EYE_HEIGHT;
        float spawnZ = 128.5F;
        teleport(spawnX, spawnY, spawnZ);
    }

    private void teleport(float nx, float ny, float nz) {
        x = nx; y = ny; z = nz;
        bb = new AABB(nx - WIDTH, ny - HEIGHT, nz - WIDTH,
                      nx + WIDTH, ny + HEIGHT, nz + WIDTH);
        resolveBlockIntersection();
    }

    public void turn(float dx, float dy) {
        yRot += dx * 0.15F;
        xRot -= dy * 0.15F;
        xRot = Math.max(-90.0F, Math.min(90.0F, xRot));
    }

    public void tick() {
        xo = x; yo = y; zo = z;

        if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
            resetPos();
        }

        float moveX = 0, moveZ = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)    || Keyboard.isKeyDown(Keyboard.KEY_W)) moveZ--;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)  || Keyboard.isKeyDown(Keyboard.KEY_S)) moveZ++;
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)  || Keyboard.isKeyDown(Keyboard.KEY_A)) moveX--;
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D)) moveX++;

        if ((Keyboard.isKeyDown(Keyboard.KEY_SPACE) || Keyboard.isKeyDown(Keyboard.KEY_LBRACKET)) && onGround) {
            yd = 0.12F;
        }

        float accel = onGround ? 0.02F : 0.005F;
        applyRelativeVelocity(moveX, moveZ, accel);

        yd -= 0.005F;
        move(xd, yd, zd);

        xd *= 0.91F;
        yd *= 0.98F;
        zd *= 0.91F;

        if (onGround) {
            xd *= 0.8F;
            zd *= 0.8F;
        }

        resolveBlockIntersection();

        // Fell off course — reset
        if (onGround && y < 45.0F) {
            boolean onStartPlatform = x > 124.0F && x < 132.0F && z > 124.0F && z < 132.0F;
            if (!onStartPlatform) {
                died = true;
                if (onDeath != null) onDeath.run();
                resetPos();
            }
        }
    }

    public void move(float xa, float ya, float za) {
        float origXa = xa, origYa = ya, origZa = za;
        List<AABB> nearby = world.getCubes(bb.expand(xa, ya, za));

        for (AABB obstacle : nearby) ya = obstacle.clipYCollide(bb, ya);
        bb.move(0, ya, 0);

        for (AABB obstacle : nearby) xa = obstacle.clipXCollide(bb, xa);
        bb.move(xa, 0, 0);

        for (AABB obstacle : nearby) za = obstacle.clipZCollide(bb, za);
        bb.move(0, 0, za);

        onGround = origYa != ya && origYa < 0;
        if (origXa != xa) xd = 0;
        if (origYa != ya) yd = 0;
        if (origZa != za) zd = 0;

        x = (bb.x0 + bb.x1) / 2.0F;
        y = bb.y0 + EYE_HEIGHT;
        z = (bb.z0 + bb.z1) / 2.0F;
    }

    public void applyRelativeVelocity(float xa, float za, float speed) {
        float lenSq = xa * xa + za * za;
        if (lenSq < 0.01F) return;

        float scale = speed / (float) Math.sqrt(lenSq);
        xa *= scale;
        za *= scale;

        float sinY = (float) Math.sin(Math.toRadians(yRot));
        float cosY = (float) Math.cos(Math.toRadians(yRot));

        xd += xa * cosY - za * sinY;
        zd += za * cosY + xa * sinY;
    }

    /**
     * Pushes the player out of any block they are intersecting.
     *
     * - Feet stuck only  → push up.
     * - Head stuck (or entire body) → push horizontally toward the nearest clear space.
     */
    private void resolveBlockIntersection() {
        if (!isInBlock(bb)) return;

        final int   MAX  = 80;
        final float STEP = 0.05F;

        float midY = (bb.y0 + bb.y1) / 2.0F;

        boolean feetStuck = isInBlock(new AABB(bb.x0, bb.y0, bb.z0, bb.x1, midY, bb.z1));
        boolean headStuck = isInBlock(new AABB(bb.x0, midY,  bb.z0, bb.x1, bb.y1, bb.z1));

        if (feetStuck && !headStuck) {
            // Only feet in a block — push up
            for (int i = 0; i < MAX && isInBlock(bb); i++) {
                bb.move(0, STEP, 0);
            }
        } else {
            // Head stuck or entire body — push sideways toward nearest free space
            int[] dxDir = { 1, -1,  0,  0 };
            int[] dzDir = { 0,  0,  1, -1 };

            int bestDir   = 0;
            int bestSteps = Integer.MAX_VALUE;

            for (int d = 0; d < 4; d++) {
                AABB test = new AABB(bb.x0, bb.y0, bb.z0, bb.x1, bb.y1, bb.z1);
                for (int i = 0; i <= MAX; i++) {
                    if (!isInBlock(test)) {
                        if (i < bestSteps) { bestSteps = i; bestDir = d; }
                        break;
                    }
                    test.move(dxDir[d] * STEP, 0, dzDir[d] * STEP);
                }
            }

            for (int i = 0; i < bestSteps && isInBlock(bb); i++) {
                bb.move(dxDir[bestDir] * STEP, 0, dzDir[bestDir] * STEP);
            }
        }

        // Sync logical position from resolved AABB
        x = (bb.x0 + bb.x1) / 2.0F;
        y = bb.y0 + EYE_HEIGHT;
        z = (bb.z0 + bb.z1) / 2.0F;
    }

    private boolean isInBlock(AABB box) {
        for (AABB c : world.getCubes(box)) {
            if (c.intersects(box)) return true;
        }
        return false;
    }
}
