package com.kobosh.aetherreach.level;

import com.kobosh.aetherreach.Textures;
import com.kobosh.aetherreach.phys.AABB;
import org.lwjgl.opengl.GL11;

public class Chunk {
    public final AABB aabb;
    public final int x0, y0, z0, x1, y1, z1;

    private final Level world;
    private boolean dirty = true;
    private int displayLists = -1;

    private static final int texture = Textures.loadTexture("/terrain.png", GL11.GL_NEAREST);
    private static final Tesselator tess = new Tesselator();

    public static int rebuiltThisFrame = 0;
    public static int updates = 0;

    public Chunk(Level world, int x0, int y0, int z0, int x1, int y1, int z1) {
        this.world = world;
        this.x0 = x0; this.y0 = y0; this.z0 = z0;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.aabb = new AABB(x0, y0, z0, x1, y1, z1);
        this.displayLists = GL11.glGenLists(2);
    }

    private void rebuild(int layer) {
        if (rebuiltThisFrame >= 2) return;

        dirty = false;
        updates++;
        rebuiltThisFrame++;

        int grassY = world.depth * 2 / 3;
        int topY   = world.goalY;

        GL11.glNewList(displayLists + layer, GL11.GL_COMPILE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        tess.init();

        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    if (!world.isTile(x, y, z)) continue;
                    if (y == grassY)      Tile.grass.render(tess, world, layer, x, y, z);
                    else if (y == topY)   Tile.goal.render(tess, world, layer, x, y, z);
                    else if (y > grassY)  Tile.wood.render(tess, world, layer, x, y, z);
                    else                  Tile.rock.render(tess, world, layer, x, y, z);
                }
            }
        }

        tess.flush();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEndList();
    }

    public void render(int layer) {
        if (dirty) {
            rebuild(0);
            rebuild(1);
        }
        GL11.glCallList(displayLists + layer);
    }

    public void setDirty() {
        dirty = true;
    }
}
