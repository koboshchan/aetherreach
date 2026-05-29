package com.kobosh.aetherreach;

import com.kobosh.aetherreach.level.Chunk;
import com.kobosh.aetherreach.level.Level;
import com.kobosh.aetherreach.level.LevelRenderer;
import com.kobosh.aetherreach.story.StoryEvent;
import com.kobosh.aetherreach.story.StorySequence;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private boolean mouseGrabbed;

    // --- story sequence ---
    private StorySequence sequence;
    private int seqIdx = 0;
    private boolean advanceAfterFrame = false; // set by render_empty; fires after first rendered frame

    // --- dialog state ---
    private boolean storyDialogOpen;
    private boolean isDieDialog = false;
    private int storyImageTex = -1;
    private String storyTitle = "";
    private String storyText = "";
    private float[] dialogBgColor = { 0.35F, 0.35F, 0.35F };

    // --- win screen state ---
    private boolean winScreenOpen = false;
    private String winTitle = "";
    private String winText = "";

    // --- parkour state ---
    private boolean won = false;

    private void init() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(WINDOW_W, WINDOW_H));
        Display.setTitle("Aether Reach"); // overwritten after sequence loads
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
        renderer = new LevelRenderer(world);
        player = new Player(world);

        sequence = StorySequence.load("/story.json");
        Display.setTitle(sequence.t("window.title", "Aether Reach"));
        advanceSequence();

        Mouse.setGrabbed(true);
        mouseGrabbed = true;
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
            while (!Display.isCloseRequested()) {
                timer.advanceTime();
                for (int i = 0; i < timer.ticks; i++) {
                    tick();
                }
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

    private void advanceSequence() {
        if (seqIdx >= sequence.events.size()) return;
        StoryEvent event = sequence.events.get(seqIdx++);

        if ("dialog".equals(event.type)) {
            isDieDialog = false;
            showStoryDialog(event.texture, event.title, event.text, hexToRgb(event.bgColor));
        } else if ("parkour".equals(event.type)) {
            startParkour(event.length, new HashSet<>(event.jumps));
        } else if ("render_empty".equals(event.type)) {
            startEmptyWorld();
            advanceAfterFrame = true; // advance after the first frame is rendered
        } else if ("win_screen".equals(event.type)) {
            winTitle = event.title != null ? event.title : "You Win!";
            winText  = event.text  != null ? event.text  : "Congratulations!";
            winScreenOpen = true;
            Mouse.setGrabbed(false);
            mouseGrabbed = false;
        }
    }

    private void startEmptyWorld() {
        if (renderer != null) renderer.destroy();
        world = new Level(256, 256, 64);
        renderer = new LevelRenderer(world);
        player = new Player(world);
        won = false;
    }

    private void startParkour(int length, Set<String> jumps) {
        if (renderer != null) renderer.destroy();
        world = new Level(256, 256, 64);
        world.generateParkour(length, jumps);
        renderer = new LevelRenderer(world);
        player = new Player(world);
        won = false;
        player.onDeath = () -> showDieDialog();
    }

    private void showDieDialog() {
        if (sequence.dieDialogs == null || sequence.dieDialogs.isEmpty()) return;
        int idx = (int) (Math.random() * sequence.dieDialogs.size());
        StoryEvent d = sequence.dieDialogs.get(idx);
        isDieDialog = true;
        showStoryDialog(d.texture, d.title, d.text, hexToRgb(d.bgColor));
    }

    private float[] hexToRgb(String hex) {
        if (hex == null || hex.isEmpty()) return new float[]{ 0.35F, 0.35F, 0.35F };
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new float[]{ r / 255F, g / 255F, b / 255F };
    }

    private void tick() {
        player.tick();
        if (!won) checkWin();
    }

    private void checkWin() {
        if (!player.onGround) return;
        float feet = player.y - Player.EYE_HEIGHT;
        int gpx = world.goalPlatformX, gpz = world.goalPlatformZ;
        if (feet >= world.goalY + 0.9f && feet <= world.goalY + 1.1f
                && player.x >= gpx - 1 && player.x <= gpx + 2
                && player.z >= gpz - 1 && player.z <= gpz + 2) {
            won = true;
            win();
        }
    }

    private void win() {
        System.out.println("win");
        advanceSequence();
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
        if (mouseGrabbed && !storyDialogOpen && !winScreenOpen) {
            player.turn(Mouse.getDX(), Mouse.getDY());
        }
        if (!storyDialogOpen && !winScreenOpen) {
            performPick(alpha);
        }

        while (Mouse.next()) {
            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                if (winScreenOpen) {
                    winScreenOpen = false;
                    Mouse.setGrabbed(true);
                    mouseGrabbed = true;
                } else if (storyDialogOpen) {
                    closeStoryDialog();
                } else if (!mouseGrabbed) {
                    Mouse.setGrabbed(true);
                    mouseGrabbed = true;
                }
            }
        }
        while (Keyboard.next()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE && Keyboard.getEventKeyState()) {
                Mouse.setGrabbed(false);
                mouseGrabbed = false;
            }
            if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && Keyboard.getEventKeyState()) {
                world.save();
            }
        }

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        setupPerspective(alpha);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.05625F);
        GL11.glFog(GL11.GL_FOG_COLOR, fogColorBuf);
        renderer.render(player, 0);
        GL11.glDisable(GL11.GL_FOG);
        renderer.render(player, 1);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (hitResult != null)
            renderer.renderHit(hitResult);

        if (storyDialogOpen) {
            renderStoryDialog();
        }

        if (winScreenOpen) {
            renderWinScreen();
        }

        if (!storyDialogOpen && !winScreenOpen) {
            renderCrosshair();
        }

        font.drawDynamic("fps", sequence.t("hud.fps", "FPS") + ": " + fps, 4, 4, screenW, screenH, false);
        font.drawDynamicRight("xyz",
                String.format(sequence.t("hud.coords", "X: %.1f  Y: %.1f  Z: %.1f"),
                        player.x, player.y, player.z),
                screenW - 4, 4, screenW, screenH);

        if (advanceAfterFrame) {
            advanceAfterFrame = false;
            advanceSequence();
        }

        Display.update();
    }

    public static void main(String[] args) {
        new AetherReach().run();
    }

    private void showStoryDialog(String imageResource, String title, String text, float[] bgColor) {
        storyTitle = title != null ? title : "";
        storyText  = text  != null ? text  : "";
        dialogBgColor = bgColor;
        storyImageTex = (imageResource != null && !imageResource.isEmpty())
                ? Textures.loadTexture(imageResource, GL11.GL_NEAREST)
                : -1;
        storyDialogOpen = true;
        Mouse.setGrabbed(false);
        mouseGrabbed = false;
    }

    private void closeStoryDialog() {
        storyDialogOpen = false;
        Mouse.setGrabbed(true);
        mouseGrabbed = true;
        boolean wasDie = isDieDialog;
        isDieDialog = false;
        if (!wasDie) {
            advanceSequence();
        }
    }

    private void renderStoryDialog() {
        float panelW = Math.min(900.0F, screenW * 0.82F);
        float panelH = Math.min(360.0F, screenH * 0.62F);
        float panelX = (screenW - panelW) * 0.5F;
        float panelY = (screenH - panelH) * 0.5F;

        float imageSize = 256.0F;
        float imageX = panelX + 28.0F;
        float imageY = panelY + (panelH - imageSize) * 0.5F;

        float titleY = panelY + 38.0F;
        float bodyStartY = titleY + 44.0F;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenW, screenH, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.45F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(screenW, 0);
        GL11.glVertex2f(screenW, screenH);
        GL11.glVertex2f(0, screenH);
        GL11.glEnd();

        GL11.glColor4f(dialogBgColor[0], dialogBgColor[1], dialogBgColor[2], 0.92F);
        drawRoundedRect(panelX, panelY, panelW, panelH, 18.0F, 8);

        if (storyImageTex >= 0) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, storyImageTex);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(imageX, imageY);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(imageX + imageSize, imageY);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(imageX + imageSize, imageY + imageSize);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(imageX, imageY + imageSize);
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        float textStartX = (storyImageTex >= 0) ? imageX + imageSize + 28.0F : panelX + 28.0F;

        font.drawDynamic("story-title", storyTitle, textStartX, titleY, screenW, screenH, false);

        int maxCharsPerLine = Math.max(20, (int) ((panelX + panelW - textStartX - 28.0F) / 10.0F));
        List<String> lines = wrapText(storyText, maxCharsPerLine, 4);
        for (int i = 0; i < lines.size(); i++) {
            font.drawDynamic("story-body-" + i, lines.get(i), textStartX, bodyStartY + i * 34.0F, screenW, screenH,
                    false);
        }

        font.drawDynamic(
                "story-continue",
                sequence.t("dialog.click_to_continue", "Click to Continue"),
                screenW * 0.5F,
                panelY + panelH - 30.0F,
                screenW,
                screenH,
                true);
    }

    private void renderWinScreen() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenW, screenH, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // Dark green overlay
        GL11.glColor4f(0.0F, 0.08F, 0.0F, 0.88F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(screenW, 0);
        GL11.glVertex2f(screenW, screenH);
        GL11.glVertex2f(0, screenH);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        font.drawDynamic("win-title", winTitle, screenW * 0.5F, screenH * 0.35F, screenW, screenH, true);
        List<String> lines = wrapText(winText, 60, 4);
        for (int i = 0; i < lines.size(); i++) {
            font.drawDynamic("win-body-" + i, lines.get(i),
                    screenW * 0.5F, screenH * 0.48F + i * 34.0F, screenW, screenH, true);
        }
        font.drawDynamic("win-continue", sequence.t("dialog.click_to_continue", "Click to Continue"),
                screenW * 0.5F, screenH * 0.75F, screenW, screenH, true);
    }

    private List<String> wrapText(String text, int maxCharsPerLine, int maxLines) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            if (current.length() + 1 + word.length() > maxCharsPerLine) {
                lines.add(current.toString());
                if (lines.size() == maxLines - 1) {
                    break;
                }
                current.setLength(0);
                current.append(word);
            } else {
                current.append(' ').append(word);
            }
        }

        if (lines.size() < maxLines && current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void drawRoundedRect(float x, float y, float w, float h, float radius, int arcSteps) {
        float r = Math.max(0.0F, Math.min(radius, Math.min(w, h) * 0.5F));

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x + r, y);
        GL11.glVertex2f(x + w - r, y);
        GL11.glVertex2f(x + w - r, y + h);
        GL11.glVertex2f(x + r, y + h);

        GL11.glVertex2f(x, y + r);
        GL11.glVertex2f(x + r, y + r);
        GL11.glVertex2f(x + r, y + h - r);
        GL11.glVertex2f(x, y + h - r);

        GL11.glVertex2f(x + w - r, y + r);
        GL11.glVertex2f(x + w, y + r);
        GL11.glVertex2f(x + w, y + h - r);
        GL11.glVertex2f(x + w - r, y + h - r);
        GL11.glEnd();

        drawCornerFan(x + r, y + r, r, (float) Math.PI, (float) (Math.PI * 1.5), arcSteps);
        drawCornerFan(x + w - r, y + r, r, (float) (Math.PI * 1.5), (float) (Math.PI * 2.0), arcSteps);
        drawCornerFan(x + w - r, y + h - r, r, 0.0F, (float) (Math.PI * 0.5), arcSteps);
        drawCornerFan(x + r, y + h - r, r, (float) (Math.PI * 0.5), (float) Math.PI, arcSteps);
    }

    private void drawCornerFan(float cx, float cy, float radius, float angleStart, float angleEnd, int steps) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float a = angleStart + (angleEnd - angleStart) * t;
            float px = cx + (float) Math.cos(a) * radius;
            float py = cy + (float) Math.sin(a) * radius;
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();
    }

    private void renderCrosshair() {
        float cx = screenW / 2.0F;
        float cy = screenH / 2.0F;
        float armLen = 6.0F;
        float halfThickness = 1.0F; // total thickness = 2px

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, screenW, screenH, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.95F);
        GL11.glBegin(GL11.GL_QUADS);
        // Horizontal arm
        GL11.glVertex2f(cx - armLen, cy - halfThickness);
        GL11.glVertex2f(cx + armLen, cy - halfThickness);
        GL11.glVertex2f(cx + armLen, cy + halfThickness);
        GL11.glVertex2f(cx - armLen, cy + halfThickness);
        // Vertical arm
        GL11.glVertex2f(cx - halfThickness, cy - armLen);
        GL11.glVertex2f(cx + halfThickness, cy - armLen);
        GL11.glVertex2f(cx + halfThickness, cy + armLen);
        GL11.glVertex2f(cx - halfThickness, cy + armLen);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }
}
