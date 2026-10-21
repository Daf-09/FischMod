package com.godze.fischfishing;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class FishingScreen extends Screen {

    public static void open(float speed, float bar) {
        Minecraft.getInstance().setScreen(new FishingScreen(speed, bar));
    }

    // ---- Tuning ----
    private static final float PUSH = 3.0f;      // speed gain while holding (moves bar right)
    private static final float DRIFT = 2.0f;     // pull toward the left while not holding
    private static final float MAX_VEL = 1.2f;
    private static final float FILL_RATE = 0.25f;   // progress gained while fish is inside the bar
    private static final float DRAIN_RATE = 0.10f;  // progress lost while outside

    private final float fishSpeed;
    private final float barSize;

    // All positions are 0..1 along the track (0 = left end, 1 = right end)
    private float barPos;
    private float barVel = 0f;
    private float fishPos = 0.5f;
    private float fishTarget = 0.5f;
    private float retargetTimer = 0f;
    private float progress = 0.35f;
    private long lastNanos = -1;
    private boolean done = false;

    private FishingScreen(float fishSpeed, float barSize) {
        super(Component.literal("Fishing"));
        this.fishSpeed = fishSpeed;
        this.barSize = barSize;
        this.barPos = (1f - barSize) / 2f; // bar starts in the middle
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        finish(false); // Esc = give up
    }

    private void finish(boolean won) {
        if (done) return;
        done = true;
        Network.CHANNEL.send(new Network.ResultPacket(won), PacketDistributor.SERVER.noArg());
        Minecraft.getInstance().setScreen(null);
    }

    private boolean isHolding() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_SPACE);
    }

    private void update(float dt) {
        // Holding pushes the bar right, otherwise it drifts toward the left corner
        barVel += (isHolding() ? PUSH : -DRIFT) * dt;
        barVel = Math.max(-MAX_VEL, Math.min(MAX_VEL, barVel));
        barPos += barVel * dt;
        float max = 1f - barSize;
        if (barPos < 0f) { barPos = 0f; barVel = 0f; }
        if (barPos > max) { barPos = max; barVel = 0f; }

        // Fish wanders toward random targets
        retargetTimer -= dt;
        if (retargetTimer <= 0f) {
            fishTarget = (float) Math.random();
            retargetTimer = 0.5f + (float) Math.random();
        }
        float step = fishSpeed * 0.6f * dt;
        fishPos += Math.max(-step, Math.min(step, fishTarget - fishPos));

        // Progress fills while the fish is inside the bar
        boolean inside = fishPos >= barPos && fishPos <= barPos + barSize;
        progress += (inside ? FILL_RATE : -DRAIN_RATE) * dt;

        if (progress >= 1f) finish(true);
        else if (progress <= 0f) finish(false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = lastNanos < 0 ? 0f : Math.min((now - lastNanos) / 1_000_000_000f, 0.05f);
        lastNanos = now;
        if (!done) update(dt);

        int w = 220, h = 18;
        int x = width / 2 - w / 2;
        int y = height - 90;

        // Background track: semi-transparent, fading out toward both ends
        for (int i = 0; i < w; i++) {
            float t = i / (float) (w - 1);
            float edge = Math.min(1f, Math.min(t, 1f - t) / 0.15f);
            int alpha = (int) (0x90 * (0.25f + 0.75f * edge));
            g.fill(x + i, y, x + i + 1, y + h, (alpha << 24) | 0x101820);
        }

        // Catch bar (white)
        int barLeft = x + (int) (barPos * w);
        int barRight = x + (int) ((barPos + barSize) * w);
        g.fill(barLeft, y, barRight, y + h, 0xFFFFFFFF);

        // Fish (red)
        int fx = x + (int) (fishPos * w);
        g.fill(fx - 3, y - 2, fx + 3, y + h + 2, 0xFFE03030);

        // Progress meter under the track
        int py = y + h + 8;
        g.fill(x, py, x + w, py + 4, 0x66000000);
        g.fill(x, py, x + (int) (progress * w), py + 4, 0xFFFFFFFF);

        g.drawCenteredString(font, "Hold click or SPACE to reel", width / 2, py + 10, 0xFFFFFF);
    }
}