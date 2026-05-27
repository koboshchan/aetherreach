package com.kobosh.aetherreach.level;

import com.kobosh.aetherreach.HitResult;
import com.kobosh.aetherreach.Player;
import com.kobosh.aetherreach.phys.AABB;
import org.lwjgl.opengl.GL11;

public class LevelRenderer implements LevelListener {
    private static final int CHUNK_SIZE = 16;

    private final Level world;
    private final Chunk[] chunks;
    private final int xChunks, yChunks, zChunks;
    private final Tesselator tess = new Tesselator();

    public LevelRenderer(Level world) {
        this.world = world;
        world.addListener(this);

        xChunks = world.width / CHUNK_SIZE;
        yChunks = world.depth / CHUNK_SIZE;
        zChunks = world.height / CHUNK_SIZE;
        chunks = new Chunk[xChunks * yChunks * zChunks];

        for (int cx = 0; cx < xChunks; cx++) {
            for (int cy = 0; cy < yChunks; cy++) {
                for (int cz = 0; cz < zChunks; cz++) {
                    int bx0 = cx * CHUNK_SIZE, bx1 = Math.min((cx + 1) * CHUNK_SIZE, world.width);
                    int by0 = cy * CHUNK_SIZE, by1 = Math.min((cy + 1) * CHUNK_SIZE, world.depth);
                    int bz0 = cz * CHUNK_SIZE, bz1 = Math.min((cz + 1) * CHUNK_SIZE, world.height);
                    chunks[(cx + cy * xChunks) * zChunks + cz] = new Chunk(world, bx0, by0, bz0, bx1, by1, bz1);
                }
            }
        }
    }

    public void render(Player player, int layer) {
        Chunk.rebuiltThisFrame = 0;
        Frustum frustum = Frustum.getFrustum();
        for (Chunk chunk : chunks) {
            if (frustum.cubeInFrustum(chunk.aabb)) {
                chunk.render(layer);
            }
        }
    }

    public void pick(Player player) {
        float reach = 3.0F;
        AABB searchBox = player.bb.grow(reach, reach, reach);
        int x0 = (int) searchBox.x0, x1 = (int) (searchBox.x1 + 1);
        int y0 = (int) searchBox.y0, y1 = (int) (searchBox.y1 + 1);
        int z0 = (int) searchBox.z0, z1 = (int) (searchBox.z1 + 1);

        GL11.glInitNames();
        for (int x = x0; x < x1; x++) {
            GL11.glPushName(x);
            for (int y = y0; y < y1; y++) {
                GL11.glPushName(y);
                for (int z = z0; z < z1; z++) {
                    GL11.glPushName(z);
                    if (world.isSolidTile(x, y, z)) {
                        GL11.glPushName(0);
                        for (int face = 0; face < 6; face++) {
                            GL11.glPushName(face);
                            tess.init();
                            Tile.rock.renderFace(tess, x, y, z, face);
                            tess.flush();
                            GL11.glPopName();
                        }
                        GL11.glPopName();
                    }
                    GL11.glPopName();
                }
                GL11.glPopName();
            }
            GL11.glPopName();
        }
    }

    public void renderHit(HitResult hit) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float pulse = (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.2F + 0.4F;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, pulse + 0.2F);
        GL11.glLineWidth(3.0F);
        renderFaceOutline(hit);
        GL11.glLineWidth(1.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void renderFaceOutline(HitResult hit) {
        float x0 = hit.x;
        float x1 = hit.x + 1.0F;
        float y0 = hit.y;
        float y1 = hit.y + 1.0F;
        float z0 = hit.z;
        float z1 = hit.z + 1.0F;

        float eps = 0.002F;
        float ox = 0.0F;
        float oy = 0.0F;
        float oz = 0.0F;

        switch (hit.f) {
            case 0:
                oy = -eps;
                break;
            case 1:
                oy = eps;
                break;
            case 2:
                oz = -eps;
                break;
            case 3:
                oz = eps;
                break;
            case 4:
                ox = -eps;
                break;
            case 5:
                ox = eps;
                break;
            default:
                return;
        }

        GL11.glBegin(GL11.GL_LINE_LOOP);
        switch (hit.f) {
            case 0:
                GL11.glVertex3f(x0 + ox, y0 + oy, z1 + oz);
                GL11.glVertex3f(x0 + ox, y0 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y0 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y0 + oy, z1 + oz);
                break;
            case 1:
                GL11.glVertex3f(x1 + ox, y1 + oy, z1 + oz);
                GL11.glVertex3f(x1 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x0 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x0 + ox, y1 + oy, z1 + oz);
                break;
            case 2:
                GL11.glVertex3f(x0 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y0 + oy, z0 + oz);
                GL11.glVertex3f(x0 + ox, y0 + oy, z0 + oz);
                break;
            case 3:
                GL11.glVertex3f(x0 + ox, y1 + oy, z1 + oz);
                GL11.glVertex3f(x0 + ox, y0 + oy, z1 + oz);
                GL11.glVertex3f(x1 + ox, y0 + oy, z1 + oz);
                GL11.glVertex3f(x1 + ox, y1 + oy, z1 + oz);
                break;
            case 4:
                GL11.glVertex3f(x0 + ox, y1 + oy, z1 + oz);
                GL11.glVertex3f(x0 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x0 + ox, y0 + oy, z0 + oz);
                GL11.glVertex3f(x0 + ox, y0 + oy, z1 + oz);
                break;
            case 5:
                GL11.glVertex3f(x1 + ox, y0 + oy, z1 + oz);
                GL11.glVertex3f(x1 + ox, y0 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y1 + oy, z0 + oz);
                GL11.glVertex3f(x1 + ox, y1 + oy, z1 + oz);
                break;
        }
        GL11.glEnd();
    }

    private void markDirty(int x0, int y0, int z0, int x1, int y1, int z1) {
        int cx0 = Math.max(0, x0 / CHUNK_SIZE);
        int cy0 = Math.max(0, y0 / CHUNK_SIZE);
        int cz0 = Math.max(0, z0 / CHUNK_SIZE);
        int cx1 = Math.min(xChunks - 1, x1 / CHUNK_SIZE);
        int cy1 = Math.min(yChunks - 1, y1 / CHUNK_SIZE);
        int cz1 = Math.min(zChunks - 1, z1 / CHUNK_SIZE);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                for (int cz = cz0; cz <= cz1; cz++) {
                    chunks[(cx + cy * xChunks) * zChunks + cz].setDirty();
                }
            }
        }
    }

    @Override
    public void tileChanged(int x, int y, int z) {
        markDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
    }

    @Override
    public void lightColumnChanged(int x, int z, int yMin, int yMax) {
        markDirty(x - 1, yMin - 1, z - 1, x + 1, yMax + 1, z + 1);
    }

    @Override
    public void allChanged() {
        markDirty(0, 0, 0, world.width, world.depth, world.height);
    }
}
