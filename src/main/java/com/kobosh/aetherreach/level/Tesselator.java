package com.kobosh.aetherreach.level;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Tesselator {
    private static final int BUFFER_VERTS = 100_000;

    private final FloatBuffer vertBuf  = BufferUtils.createFloatBuffer(BUFFER_VERTS * 3);
    private final FloatBuffer texBuf   = BufferUtils.createFloatBuffer(BUFFER_VERTS * 2);
    private final FloatBuffer colorBuf = BufferUtils.createFloatBuffer(BUFFER_VERTS * 3);

    private int vertCount;
    private float u, v;
    private float r, g, b;
    private boolean useTexture;
    private boolean useColor;

    public void init() {
        ((Buffer) vertBuf).clear();
        ((Buffer) texBuf).clear();
        ((Buffer) colorBuf).clear();
        vertCount = 0;
        useTexture = false;
        useColor = false;
    }

    public void tex(float u, float v) {
        this.u = u;
        this.v = v;
        useTexture = true;
    }

    public void color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        useColor = true;
    }

    public void vertex(float x, float y, float z) {
        int base3 = vertCount * 3;
        int base2 = vertCount * 2;
        vertBuf.put(base3, x).put(base3 + 1, y).put(base3 + 2, z);
        if (useTexture) texBuf.put(base2, u).put(base2 + 1, v);
        if (useColor)   colorBuf.put(base3, r).put(base3 + 1, g).put(base3 + 2, b);
        vertCount++;
        if (vertCount == BUFFER_VERTS) flush();
    }

    public void flush() {
        ((Buffer) vertBuf).flip();
        ((Buffer) texBuf).flip();
        ((Buffer) colorBuf).flip();

        GL11.glVertexPointer(3, 0, vertBuf);
        if (useTexture)  GL11.glTexCoordPointer(2, 0, texBuf);
        if (useColor)    GL11.glColorPointer(3, 0, colorBuf);

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        if (useTexture) GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        if (useColor)   GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

        GL11.glDrawArrays(GL11.GL_QUADS, 0, vertCount);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        if (useTexture) GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        if (useColor)   GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);

        vertCount = 0;
        ((Buffer) vertBuf).clear();
        ((Buffer) texBuf).clear();
        ((Buffer) colorBuf).clear();
    }
}
