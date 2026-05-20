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
        goalY = 42 + (int) (length * 0.5f);

        int cx = width  / 2;
        int cz = height / 2;

        // random initial direction: +x, -x, +z, -z
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] dir = dirs[rand.nextInt(4)];
        int fdx = dir[0], fdz = dir[1];

        int[] px = new int[length];
        int[] pz = new int[length];
        px[0] = cx;
        pz[0] = cz;

        int prevTurn = 0; // 0=forward, 1=left, 2=right

        for (int i = 1; i < length; i++) {
            int turn;
            if      (prevTurn == 1) turn = rand.nextBoolean() ? 0 : 2;
            else if (prevTurn == 2) turn = rand.nextBoolean() ? 0 : 1;
            else                   turn = rand.nextInt(3);

            if (turn == 1) { // rotate CCW
                int nd = -fdz; fdz = fdx; fdx = nd;
            } else if (turn == 2) { // rotate CW
                int nd = fdz; fdz = -fdx; fdx = nd;
            }
            prevTurn = turn;

            cx = Math.max(1, Math.min(width  - 2, cx + fdx * 2));
            cz = Math.max(1, Math.min(height - 2, cz + fdz * 2));
            px[i] = cx;
            pz[i] = cz;
        }

        int startY = depth * 2 / 3;
        for (int i = 0; i < length; i++) {
            int y = (length == 1) ? startY : startY + ((goalY - startY) * i) / (length - 1);
            setTile(px[i], y, pz[i], 1);
        }
    }
}
