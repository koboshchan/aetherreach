package com.kobosh.aetherreach.level;

import com.kobosh.aetherreach.phys.AABB;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class Frustum {
    private static final int PLANE_COUNT = 6;
    private static final int RIGHT  = 0;
    private static final int LEFT   = 1;
    private static final int BOTTOM = 2;
    private static final int TOP    = 3;
    private static final int BACK   = 4;
    private static final int FRONT  = 5;

    private final float[][] planes = new float[PLANE_COUNT][4];
    private final FloatBuffer projBuf  = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modlBuf  = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer clipBuf  = BufferUtils.createFloatBuffer(16);
    private final float[] proj = new float[16];
    private final float[] modl = new float[16];
    private final float[] clip = new float[16];

    private static final Frustum INSTANCE = new Frustum();

    private Frustum() {}

    public static Frustum getFrustum() {
        INSTANCE.update();
        return INSTANCE;
    }

    private void normalize(int side) {
        float mag = (float) Math.sqrt(
            planes[side][0] * planes[side][0] +
            planes[side][1] * planes[side][1] +
            planes[side][2] * planes[side][2]);
        planes[side][0] /= mag;
        planes[side][1] /= mag;
        planes[side][2] /= mag;
        planes[side][3] /= mag;
    }

    private void update() {
        ((Buffer) projBuf).clear();
        ((Buffer) modlBuf).clear();
        ((Buffer) clipBuf).clear();

        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuf);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,  modlBuf);
        ((Buffer) projBuf).flip().limit(16);
        projBuf.get(proj);
        ((Buffer) modlBuf).flip().limit(16);
        modlBuf.get(modl);

        // multiply projection * modelview into clip
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += modl[row * 4 + k] * proj[k * 4 + col];
                }
                clip[row * 4 + col] = sum;
            }
        }

        // extract frustum planes from the clip matrix
        planes[RIGHT][0]  = clip[3]  - clip[0];
        planes[RIGHT][1]  = clip[7]  - clip[4];
        planes[RIGHT][2]  = clip[11] - clip[8];
        planes[RIGHT][3]  = clip[15] - clip[12];
        normalize(RIGHT);

        planes[LEFT][0]   = clip[3]  + clip[0];
        planes[LEFT][1]   = clip[7]  + clip[4];
        planes[LEFT][2]   = clip[11] + clip[8];
        planes[LEFT][3]   = clip[15] + clip[12];
        normalize(LEFT);

        planes[BOTTOM][0] = clip[3]  + clip[1];
        planes[BOTTOM][1] = clip[7]  + clip[5];
        planes[BOTTOM][2] = clip[11] + clip[9];
        planes[BOTTOM][3] = clip[15] + clip[13];
        normalize(BOTTOM);

        planes[TOP][0]    = clip[3]  - clip[1];
        planes[TOP][1]    = clip[7]  - clip[5];
        planes[TOP][2]    = clip[11] - clip[9];
        planes[TOP][3]    = clip[15] - clip[13];
        normalize(TOP);

        planes[BACK][0]   = clip[3]  - clip[2];
        planes[BACK][1]   = clip[7]  - clip[6];
        planes[BACK][2]   = clip[11] - clip[10];
        planes[BACK][3]   = clip[15] - clip[14];
        normalize(BACK);

        planes[FRONT][0]  = clip[3]  + clip[2];
        planes[FRONT][1]  = clip[7]  + clip[6];
        planes[FRONT][2]  = clip[11] + clip[10];
        planes[FRONT][3]  = clip[15] + clip[14];
        normalize(FRONT);
    }

    public boolean pointInFrustum(float x, float y, float z) {
        for (int i = 0; i < PLANE_COUNT; i++) {
            if (planes[i][0]*x + planes[i][1]*y + planes[i][2]*z + planes[i][3] <= 0) return false;
        }
        return true;
    }

    public boolean sphereInFrustum(float x, float y, float z, float radius) {
        for (int i = 0; i < PLANE_COUNT; i++) {
            if (planes[i][0]*x + planes[i][1]*y + planes[i][2]*z + planes[i][3] <= -radius) return false;
        }
        return true;
    }

    public boolean cubeInFrustum(AABB box) {
        return cubeInFrustum(box.x0, box.y0, box.z0, box.x1, box.y1, box.z1);
    }

    public boolean cubeInFrustum(float x0, float y0, float z0, float x1, float y1, float z1) {
        for (int i = 0; i < PLANE_COUNT; i++) {
            float[] p = planes[i];
            if (p[0]*x0 + p[1]*y0 + p[2]*z0 + p[3] > 0) continue;
            if (p[0]*x1 + p[1]*y0 + p[2]*z0 + p[3] > 0) continue;
            if (p[0]*x0 + p[1]*y1 + p[2]*z0 + p[3] > 0) continue;
            if (p[0]*x1 + p[1]*y1 + p[2]*z0 + p[3] > 0) continue;
            if (p[0]*x0 + p[1]*y0 + p[2]*z1 + p[3] > 0) continue;
            if (p[0]*x1 + p[1]*y0 + p[2]*z1 + p[3] > 0) continue;
            if (p[0]*x0 + p[1]*y1 + p[2]*z1 + p[3] > 0) continue;
            if (p[0]*x1 + p[1]*y1 + p[2]*z1 + p[3] > 0) continue;
            return false;
        }
        return true;
    }

    public boolean cubeFullyInFrustum(float x0, float y0, float z0, float x1, float y1, float z1) {
        for (int i = 0; i < PLANE_COUNT; i++) {
            float[] p = planes[i];
            if (p[0]*x0 + p[1]*y0 + p[2]*z0 + p[3] <= 0) return false;
            if (p[0]*x1 + p[1]*y0 + p[2]*z0 + p[3] <= 0) return false;
            if (p[0]*x0 + p[1]*y1 + p[2]*z0 + p[3] <= 0) return false;
            if (p[0]*x1 + p[1]*y1 + p[2]*z0 + p[3] <= 0) return false;
            if (p[0]*x0 + p[1]*y0 + p[2]*z1 + p[3] <= 0) return false;
            if (p[0]*x1 + p[1]*y0 + p[2]*z1 + p[3] <= 0) return false;
            if (p[0]*x0 + p[1]*y1 + p[2]*z1 + p[3] <= 0) return false;
            if (p[0]*x1 + p[1]*y1 + p[2]*z1 + p[3] <= 0) return false;
        }
        return true;
    }
}
