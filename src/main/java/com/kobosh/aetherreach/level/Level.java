package com.kobosh.aetherreach.level;

import com.kobosh.aetherreach.phys.AABB;
import java.util.ArrayList;
import java.util.Random;

public class Level {
    public final int width;
    public final int height;
    public final int depth;
    public int goalY = 52;

    private final byte[] voxels;
    private final int[] lightColumn;
    private final ArrayList<LevelListener> listeners = new ArrayList<>();

    public Level(int width, int height, int depth) {
        this.width  = width;
        this.height = height;
        this.depth  = depth;
        this.voxels = new byte[width * height * depth];
        this.lightColumn = new int[width * height];

        int solidBelow = depth * 2 / 3;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < depth; y++) {
                for (int z = 0; z < height; z++) {
                    int idx = (y * height + z) * width + x;
                    voxels[idx] = (byte) (y <= solidBelow ? 1 : 0);
                }
            }
        }
        calcLightDepths(0, 0, width, height);
    }

    public void save() {}
    public void load() {}

    public void calcLightDepths(int x0, int z0, int spanX, int spanZ) {
        for (int x = x0; x < x0 + spanX; x++) {
            for (int z = z0; z < z0 + spanZ; z++) {
                int prev = lightColumn[x + z * width];
                int y = depth - 1;
                while (y > 0 && !isLightBlocker(x, y, z)) y--;
                lightColumn[x + z * width] = y;

                if (prev != y) {
                    int lo = Math.min(prev, y);
                    int hi = Math.max(prev, y);
                    for (LevelListener l : listeners) {
                        l.lightColumnChanged(x, z, lo, hi);
                    }
                }
            }
        }
    }

    public void addListener(LevelListener l)    { listeners.add(l); }
    public void removeListener(LevelListener l) { listeners.remove(l); }

    public boolean isTile(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= depth || z >= height) return false;
        return voxels[(y * height + z) * width + x] == 1;
    }

    public boolean isSolidTile(int x, int y, int z)   { return isTile(x, y, z); }
    public boolean isLightBlocker(int x, int y, int z) { return isSolidTile(x, y, z); }

    public ArrayList<AABB> getCubes(AABB query) {
        ArrayList<AABB> result = new ArrayList<>();
        int x0 = Math.max(0, (int) query.x0);
        int y0 = Math.max(0, (int) query.y0);
        int z0 = Math.max(0, (int) query.z0);
        int x1 = Math.min(width,  (int) (query.x1 + 1));
        int y1 = Math.min(depth,  (int) (query.y1 + 1));
        int z1 = Math.min(height, (int) (query.z1 + 1));

        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (isSolidTile(x, y, z)) {
                        result.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return result;
    }

    public float getBrightness(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= depth || z >= height) return 1.0F;
        return y < lightColumn[x + z * width] ? 0.8F : 1.0F;
    }

    public void setTile(int x, int y, int z, int type) {
        if (x < 0 || y < 0 || z < 0 || x >= width || y >= depth || z >= height) return;
        voxels[(y * height + z) * width + x] = (byte) type;
        calcLightDepths(x, z, 1, 1);
        for (LevelListener l : listeners) {
            l.tileChanged(x, y, z);
        }
    }

    public void generateParkour(int length) {
        if (length <= 0) return;
        Random rand = new Random();

        int cy = depth  * 2 / 3 + 1;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] startDir = dirs[rand.nextInt(4)];
        int fdx = startDir[0], fdz = startDir[1];

        int cx = width  / 2;
        int cz = height / 2;

        // Collect all block positions before placing them so we can shift if needed
        ArrayList<int[]> blocks = new ArrayList<>();
        blocks.add(new int[]{cx, cy, cz});

        int lastDir = 0; // 0=forward, 1=left, 2=right

        for (int i = 0; i < length; i++) {
            int jumpType = rand.nextInt(4);

            int dx = 0, dz = 0, dy = 0;
            int newDir;
            int newFdx = fdx, newFdz = fdz;

            switch (jumpType) {
                case 0: { // forward 2: forward, left, or right
                    newDir = pickDir(rand, lastDir, true, true, true);
                    if      (newDir == 1) { newFdx = -fdz; newFdz =  fdx; }
                    else if (newDir == 2) { newFdx =  fdz; newFdz = -fdx; }
                    dx = newFdx * 3; dz = newFdz * 3;
                    break;
                }
                case 1: { // diagonal 1 flat: left or right only (gap at (1,1))
                    newDir = pickDir(rand, lastDir, false, true, true);
                    if (newDir == 2) { dx = (fdx + fdz) * 2; dz = (fdz - fdx) * 2; }
                    else             { dx = (fdx - fdz) * 2; dz = (fdz + fdx) * 2; }
                    break;
                }
                case 2: { // diagonal 1 up: left or right only (gap at (1,1))
                    newDir = pickDir(rand, lastDir, false, true, true);
                    if (newDir == 2) { dx = (fdx + fdz) * 2; dz = (fdz - fdx) * 2; }
                    else             { dx = (fdx - fdz) * 2; dz = (fdz + fdx) * 2; }
                    dy = 1;
                    break;
                }
                default: { // forward 1 up: forward, left, or right
                    newDir = pickDir(rand, lastDir, true, true, true);
                    if      (newDir == 1) { newFdx = -fdz; newFdz =  fdx; }
                    else if (newDir == 2) { newFdx =  fdz; newFdz = -fdx; }
                    dx = newFdx * 2; dz = newFdz * 2;
                    dy = 1;
                    break;
                }
            }

            cx = Math.max(1, Math.min(width  - 2, cx + dx));
            cy = Math.max(depth * 2 / 3 + 1, Math.min(depth - 2, cy + dy));
            cz = Math.max(1, Math.min(height - 2, cz + dz));
            fdx = newFdx;
            fdz = newFdz;
            if (newDir != 0) lastDir = newDir;

            blocks.add(new int[]{cx, cy, cz});
        }

        // Final goal block: one step forward and up
        goalY = Math.min(cy + 1, depth - 2);
        int gcx = Math.max(1, Math.min(width  - 2, cx + fdx));
        int gcz = Math.max(1, Math.min(height - 2, cz + fdz));
        blocks.add(new int[]{gcx, goalY, gcz});

        // Shift all blocks in the initial forward direction if any overlap the
        // player's spawn AABB at (128.5, spawnY, 128.5).
        float spawnX = 128.5f, spawnZ = 128.5f;
        float playerHalfW = 0.3f;
        float px0 = spawnX - playerHalfW, px1 = spawnX + playerHalfW;
        float pz0 = spawnZ - playerHalfW, pz1 = spawnZ + playerHalfW;

        boolean overlaps = false;
        for (int[] b : blocks) {
            if (b[0] < px1 && b[0] + 1 > px0 && b[2] < pz1 && b[2] + 1 > pz0) {
                overlaps = true;
                break;
            }
        }
        int shiftX = overlaps ? startDir[0] : 0;
        int shiftZ = overlaps ? startDir[1] : 0;

        for (int[] b : blocks) {
            int bx = Math.max(1, Math.min(width  - 2, b[0] + shiftX));
            int by = b[1];
            int bz = Math.max(1, Math.min(height - 2, b[2] + shiftZ));
            setTile(bx, by, bz, 1);
        }
    }

    // Returns a direction (0=forward, 1=left, 2=right) respecting the
    // "no same lateral direction twice in a row" constraint.
    private int pickDir(Random rand, int lastDir, boolean canFwd, boolean canLeft, boolean canRight) {
        ArrayList<Integer> avail = new ArrayList<>();
        if (canFwd)                   avail.add(0);
        if (canLeft  && lastDir != 1) avail.add(1);
        if (canRight && lastDir != 2) avail.add(2);
        if (avail.isEmpty())          avail.add(0); // safety fallback
        return avail.get(rand.nextInt(avail.size()));
    }
}
