package com.kobosh.aetherreach;

import com.kobosh.aetherreach.level.Chunk;
import com.kobosh.aetherreach.level.Level;
import com.kobosh.aetherreach.level.LevelRenderer;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.swing.JOptionPane;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class AetherReach implements Runnable {
    private static final int WINDOW_W = 1024;
    private static final int WINDOW_H = 768;

    private int screenW, screenH;
    private final Timer timer = new Timer(60.0F);
    private final FloatBuffer fogColorBuf = BufferUtils.createFloatBuffer(4);
    private final IntBuffer viewportBuf = BufferUtils.createIntBuffer(16);
    private final IntBuffer selectBuf = BufferUtils.createIntBuffer(2000);

    private Level world;
    private LevelRenderer renderer;
    private Player player;
    private HitResult hitResult;
    private final FontRenderer font = new FontRenderer();
    private int fps;

    private void init() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(WINDOW_W, WINDOW_H));
        Display.setTitle("Aether Reach");
        Display.create();
        Keyboard.create();
        Mouse.create();

        screenW = Display.getDisplayMode().getWidth();
        screenH = Display.getDisplayMode().getHeight();

        // light blue sky
        int skyRgb = 0x87CEEB;
        float skyR = ((skyRgb >> 16) & 0xFF) / 255.0F;
        float skyG = ((skyRgb >> 8) & 0xFF) / 255.0F;
        float skyB = (skyRgb & 0xFF) / 255.0F;
        fogColorBuf.put(new float[] { 0.25F, 0.25F, 0.25F, 1.0F });
        ((Buffer) fogColorBuf).flip();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glClearColor(skyR, skyG, skyB, 0.0F);
        GL11.glClearDepth(1.0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        world = new Level(256, 256, 64);
        world.generateParkour(40);
        renderer = new LevelRenderer(world);
        player = new Player(world);

        Mouse.setGrabbed(true);
    }

    private void destroy() {
        world.save();
        Mouse.destroy();
        Keyboard.destroy();
        Display.destroy();
    }

    @Override
    public void run() {
        try {
            init();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.toString(), "Failed to start Aether Reach", 0);
            System.exit(0);
        }

        long lastSecond = System.currentTimeMillis();
        int frameCount = 0;

        try {
            while (!Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && !Display.isCloseRequested()) {
                timer.advanceTime();
                for (int i = 0; i < timer.ticks; i++)
                    tick();
                render(timer.a);

                frameCount++;
                while (System.currentTimeMillis() >= lastSecond + 1000L) {
                    fps = frameCount;
                    frameCount = 0;
                    Chunk.updates = 0;
                    lastSecond += 1000L;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            destroy();
        }
    }

    private void tick() {
        player.tick();
    }

    private void positionCamera(float alpha) {
        GL11.glTranslatef(0, 0, -0.3F);
        GL11.glRotatef(player.xRot, 1, 0, 0);
        GL11.glRotatef(player.yRot, 0, 1, 0);

        float ix = player.xo + (player.x - player.xo) * alpha;
        float iy = player.yo + (player.y - player.yo) * alpha;
        float iz = player.zo + (player.z - player.zo) * alpha;
        GL11.glTranslatef(-ix, -iy, -iz);
    }

    private void setupPerspective(float alpha) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(70.0F, (float) screenW / screenH, 0.05F, 1000.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        positionCamera(alpha);
    }

    private void setupPickPerspective(float alpha, int cx, int cy) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        ((Buffer) viewportBuf).clear();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
        ((Buffer) viewportBuf).flip().limit(16);
        GLU.gluPickMatrix(cx, cy, 5.0F, 5.0F, viewportBuf);
        GLU.gluPerspective(70.0F, (float) screenW / screenH, 0.05F, 1000.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        positionCamera(alpha);
    }

    private void performPick(float alpha) {
        ((Buffer) selectBuf).clear();
        GL11.glSelectBuffer(selectBuf);
        GL11.glRenderMode(GL11.GL_SELECT);
        setupPickPerspective(alpha, screenW / 2, screenH / 2);
        renderer.pick(player);

        int hits = GL11.glRenderMode(GL11.GL_RENDER);
        ((Buffer) selectBuf).flip().limit(selectBuf.capacity());

        long closestZ = 0;
        int[] bestNames = new int[10];
        int bestCount = 0;

        for (int i = 0; i < hits; i++) {
            int nameCount = selectBuf.get();
            long minZ = (long) selectBuf.get();
            selectBuf.get(); // maxZ – unused

            if (minZ >= closestZ && i != 0) {
                for (int n = 0; n < nameCount; n++)
                    selectBuf.get();
            } else {
                closestZ = minZ;
                bestCount = nameCount;
                for (int n = 0; n < nameCount; n++)
                    bestNames[n] = selectBuf.get();
            }
        }

        hitResult = (bestCount > 0)
                ? new HitResult(bestNames[0], bestNames[1], bestNames[2], bestNames[3], bestNames[4])
                : null;
    }

    private void render(float alpha) {
        player.turn(Mouse.getDX(), Mouse.getDY());
        performPick(alpha);

        while (Mouse.next()) {
        }
        while (Keyboard.next()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && Keyboard.getEventKeyState()) {
                world.save();
            }
        }

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        setupPerspective(alpha);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.025F);
        GL11.glFog(GL11.GL_FOG_COLOR, fogColorBuf);
        renderer.render(player, 0);
        GL11.glDisable(GL11.GL_FOG);
        renderer.render(player, 1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (hitResult != null)
            renderer.renderHit(hitResult);

        font.drawDynamic("fps", "FPS: " + fps, 4, 4, screenW, screenH, false);
        font.drawDynamicRight("xyz",
                String.format("X: %.1f  Y: %.1f  Z: %.1f", player.x, player.y, player.z),
                screenW - 4, 4, screenW, screenH);

        Display.update();
    }

    public static void main(String[] args) {
        new Thread(new AetherReach()).start();
    }
}
