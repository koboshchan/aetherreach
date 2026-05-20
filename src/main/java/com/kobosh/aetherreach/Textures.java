package com.kobosh.aetherreach;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class Textures {
    private static final HashMap<String, Integer> cache = new HashMap<>();
    private static int lastBound = Integer.MIN_VALUE;

    public static int loadTexture(String resource, int filter) {
        if (cache.containsKey(resource)) {
            return cache.get(resource);
        }
        try {
            int id = GL11.glGenTextures();
            bind(id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

            BufferedImage img = ImageIO.read(Textures.class.getResourceAsStream(resource));
            int w = img.getWidth();
            int h = img.getHeight();
            int[] raw = img.getRGB(0, 0, w, h, null, 0, w);

            ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
            for (int pixel : raw) {
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >>  8) & 0xFF;
                int b =  pixel        & 0xFF;
                raw[pixels.position() / 4] = (a << 24) | (b << 16) | (g << 8) | r;
            }
            // Re-pack ABGR → RGBA for OpenGL
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int pixel : raw) {
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >>  8) & 0xFF;
                int b =  pixel        & 0xFF;
                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) a);
            }
            buf.flip();
            GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D, GL11.GL_RGBA, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

            cache.put(resource, id);
            return id;
        } catch (IOException e) {
            throw new RuntimeException("Could not load texture: " + resource, e);
        }
    }

    public static void bind(int id) {
        if (id != lastBound) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            lastBound = id;
        }
    }
}
