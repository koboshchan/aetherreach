package com.kobosh.aetherreach;

import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

public class FontRenderer {
    private final HashMap<String, int[]> textureCache = new HashMap<>();

    public void drawString(String text, float x, float y, int screenW, int screenH) {
        drawString(text, x, y, screenW, screenH, true);
    }

    public void drawString(String text, float x, float y, int screenW, int screenH, boolean centered) {
        int[] entry = textureCache.computeIfAbsent(text, this::bakeTexture);
        renderQuad(entry, x, y, screenW, screenH, centered);
    }

    public void drawDynamic(String key, String text, float x, float y, int screenW, int screenH, boolean centered) {
        int[] entry = bakeTexture(text);
        textureCache.put(key, entry);
        renderQuad(entry, x, y, screenW, screenH, centered);
    }

    public void drawDynamicRight(String key, String text, float rightX, float y, int screenW, int screenH) {
        int[] entry = bakeTexture(text);
        textureCache.put(key, entry);
        renderQuad(entry, rightX - entry[1], y, screenW, screenH, false);
    }

    private void renderQuad(int[] entry, float sx, float sy, int sw, int sh, boolean centered) {
        int texId = entry[0], tw = entry[1], th = entry[2];
        float qx = centered ? sx - tw * 0.5F : sx;
        float qy = centered ? sy - th * 0.5F : sy;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, sw, sh, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glColor4f(1, 1, 1, 1);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(qx,      qy);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(qx + tw, qy);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(qx + tw, qy + th);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(qx,      qy + th);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    private int[] bakeTexture(String text) {
        Font font = new Font("Arial", Font.BOLD, 20);

        // measure
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        pg.setFont(font);
        FontMetrics fm = pg.getFontMetrics();
        int tw = fm.stringWidth(text) + 8;
        int th = fm.getHeight() + 8;
        pg.dispose();

        // render
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(0, 0, tw, th, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(text, 4, fm.getAscent() + 4);
        g.dispose();

        int[] pixels = img.getRGB(0, 0, tw, th, null, 0, tw);
        ByteBuffer buf = ByteBuffer.allocateDirect(tw * th * 4).order(ByteOrder.nativeOrder());
        for (int pixel : pixels) {
            buf.put((byte) ((pixel >> 16) & 0xFF));
            buf.put((byte) ((pixel >>  8) & 0xFF));
            buf.put((byte) ( pixel        & 0xFF));
            buf.put((byte) ((pixel >> 24) & 0xFF));
        }
        ((Buffer) buf).flip();

        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, tw, th, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        return new int[]{id, tw, th};
    }
}
