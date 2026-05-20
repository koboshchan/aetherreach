package com.kobosh.aetherreach.level;

public class Tile {
    public static final Tile rock  = new Tile(1);
    public static final Tile grass = new Tile(0);
    public static final Tile wood  = new Tile(2);
    public static final Tile goal  = new Tile(3);

    private static final float TEX_SIZE = 1.0F / 16.0F;
    private static final float TEX_INSET = 0.0624375F;

    private final int texIndex;

    private Tile(int texIndex) {
        this.texIndex = texIndex;
    }

    public void render(Tesselator t, Level world, int layer, int bx, int by, int bz) {
        float u0 = texIndex * TEX_SIZE;
        float u1 = u0 + TEX_INSET;
        float v0 = 0.0F;
        float v1 = v0 + TEX_INSET;

        float topBright  = 1.0F;
        float sideBright = 0.8F;
        float botBright  = 0.6F;

        float fx0 = bx,     fx1 = bx + 1;
        float fy0 = by,     fy1 = by + 1;
        float fz0 = bz,     fz1 = bz + 1;

        // Bottom face
        if (!world.isSolidTile(bx, by - 1, bz)) {
            float br = world.getBrightness(bx, by - 1, bz) * topBright;
            if (br == topBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u0, v1); t.vertex(fx0, fy0, fz1);
                t.tex(u0, v0); t.vertex(fx0, fy0, fz0);
                t.tex(u1, v0); t.vertex(fx1, fy0, fz0);
                t.tex(u1, v1); t.vertex(fx1, fy0, fz1);
            }
        }

        // Top face
        if (!world.isSolidTile(bx, by + 1, bz)) {
            float br = world.getBrightness(bx, by, bz) * topBright;
            if (br == topBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u1, v1); t.vertex(fx1, fy1, fz1);
                t.tex(u1, v0); t.vertex(fx1, fy1, fz0);
                t.tex(u0, v0); t.vertex(fx0, fy1, fz0);
                t.tex(u0, v1); t.vertex(fx0, fy1, fz1);
            }
        }

        // North face (-Z)
        if (!world.isSolidTile(bx, by, bz - 1)) {
            float br = world.getBrightness(bx, by, bz - 1) * sideBright;
            if (br == sideBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u1, v0); t.vertex(fx0, fy1, fz0);
                t.tex(u0, v0); t.vertex(fx1, fy1, fz0);
                t.tex(u0, v1); t.vertex(fx1, fy0, fz0);
                t.tex(u1, v1); t.vertex(fx0, fy0, fz0);
            }
        }

        // South face (+Z)
        if (!world.isSolidTile(bx, by, bz + 1)) {
            float br = world.getBrightness(bx, by, bz + 1) * sideBright;
            if (br == sideBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u0, v0); t.vertex(fx0, fy1, fz1);
                t.tex(u0, v1); t.vertex(fx0, fy0, fz1);
                t.tex(u1, v1); t.vertex(fx1, fy0, fz1);
                t.tex(u1, v0); t.vertex(fx1, fy1, fz1);
            }
        }

        // West face (-X)
        if (!world.isSolidTile(bx - 1, by, bz)) {
            float br = world.getBrightness(bx - 1, by, bz) * botBright;
            if (br == botBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u1, v0); t.vertex(fx0, fy1, fz1);
                t.tex(u0, v0); t.vertex(fx0, fy1, fz0);
                t.tex(u0, v1); t.vertex(fx0, fy0, fz0);
                t.tex(u1, v1); t.vertex(fx0, fy0, fz1);
            }
        }

        // East face (+X)
        if (!world.isSolidTile(bx + 1, by, bz)) {
            float br = world.getBrightness(bx + 1, by, bz) * botBright;
            if (br == botBright ^ layer == 1) {
                t.color(br, br, br);
                t.tex(u0, v1); t.vertex(fx1, fy0, fz1);
                t.tex(u1, v1); t.vertex(fx1, fy0, fz0);
                t.tex(u1, v0); t.vertex(fx1, fy1, fz0);
                t.tex(u0, v0); t.vertex(fx1, fy1, fz1);
            }
        }
    }

    public void renderFace(Tesselator t, int bx, int by, int bz, int face) {
        float fx0 = bx,     fx1 = bx + 1;
        float fy0 = by,     fy1 = by + 1;
        float fz0 = bz,     fz1 = bz + 1;

        switch (face) {
            case 0: // bottom
                t.vertex(fx0, fy0, fz1);
                t.vertex(fx0, fy0, fz0);
                t.vertex(fx1, fy0, fz0);
                t.vertex(fx1, fy0, fz1);
                break;
            case 1: // top
                t.vertex(fx1, fy1, fz1);
                t.vertex(fx1, fy1, fz0);
                t.vertex(fx0, fy1, fz0);
                t.vertex(fx0, fy1, fz1);
                break;
            case 2: // north (-Z)
                t.vertex(fx0, fy1, fz0);
                t.vertex(fx1, fy1, fz0);
                t.vertex(fx1, fy0, fz0);
                t.vertex(fx0, fy0, fz0);
                break;
            case 3: // south (+Z)
                t.vertex(fx0, fy1, fz1);
                t.vertex(fx0, fy0, fz1);
                t.vertex(fx1, fy0, fz1);
                t.vertex(fx1, fy1, fz1);
                break;
            case 4: // west (-X)
                t.vertex(fx0, fy1, fz1);
                t.vertex(fx0, fy1, fz0);
                t.vertex(fx0, fy0, fz0);
                t.vertex(fx0, fy0, fz1);
                break;
            case 5: // east (+X)
                t.vertex(fx1, fy0, fz1);
                t.vertex(fx1, fy0, fz0);
                t.vertex(fx1, fy1, fz0);
                t.vertex(fx1, fy1, fz1);
                break;
        }
    }
}
