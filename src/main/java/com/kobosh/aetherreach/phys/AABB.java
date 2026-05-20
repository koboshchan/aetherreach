package com.kobosh.aetherreach.phys;

public class AABB {
    public float x0, y0, z0;
    public float x1, y1, z1;
    private static final float EPSILON = 0.0F;

    public AABB(float x0, float y0, float z0, float x1, float y1, float z1) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
    }

    public AABB expand(float dx, float dy, float dz) {
        float nx0 = x0, ny0 = y0, nz0 = z0;
        float nx1 = x1, ny1 = y1, nz1 = z1;
        if (dx < 0) nx0 += dx; else nx1 += dx;
        if (dy < 0) ny0 += dy; else ny1 += dy;
        if (dz < 0) nz0 += dz; else nz1 += dz;
        return new AABB(nx0, ny0, nz0, nx1, ny1, nz1);
    }

    public AABB grow(float dx, float dy, float dz) {
        return new AABB(x0 - dx, y0 - dy, z0 - dz, x1 + dx, y1 + dy, z1 + dz);
    }

    public float clipXCollide(AABB other, float xa) {
        if (other.y1 <= y0 || other.y0 >= y1) return xa;
        if (other.z1 <= z0 || other.z0 >= z1) return xa;
        if (xa > 0 && other.x1 <= x0) {
            float gap = x0 - other.x1 - EPSILON;
            if (gap < xa) xa = gap;
        }
        if (xa < 0 && other.x0 >= x1) {
            float gap = x1 - other.x0 + EPSILON;
            if (gap > xa) xa = gap;
        }
        return xa;
    }

    public float clipYCollide(AABB other, float ya) {
        if (other.x1 <= x0 || other.x0 >= x1) return ya;
        if (other.z1 <= z0 || other.z0 >= z1) return ya;
        if (ya > 0 && other.y1 <= y0) {
            float gap = y0 - other.y1 - EPSILON;
            if (gap < ya) ya = gap;
        }
        if (ya < 0 && other.y0 >= y1) {
            float gap = y1 - other.y0 + EPSILON;
            if (gap > ya) ya = gap;
        }
        return ya;
    }

    public float clipZCollide(AABB other, float za) {
        if (other.x1 <= x0 || other.x0 >= x1) return za;
        if (other.y1 <= y0 || other.y0 >= y1) return za;
        if (za > 0 && other.z1 <= z0) {
            float gap = z0 - other.z1 - EPSILON;
            if (gap < za) za = gap;
        }
        if (za < 0 && other.z0 >= z1) {
            float gap = z1 - other.z0 + EPSILON;
            if (gap > za) za = gap;
        }
        return za;
    }

    public boolean intersects(AABB other) {
        if (other.x1 <= x0 || other.x0 >= x1) return false;
        if (other.y1 <= y0 || other.y0 >= y1) return false;
        return !(other.z1 <= z0) && !(other.z0 >= z1);
    }

    public void move(float dx, float dy, float dz) {
        x0 += dx; y0 += dy; z0 += dz;
        x1 += dx; y1 += dy; z1 += dz;
    }
}
