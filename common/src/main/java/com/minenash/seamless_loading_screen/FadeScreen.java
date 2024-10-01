package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class FadeScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final int fadeFrames;
    private int frames;
    private Consumer<Boolean> callback;
    private boolean done;

    public FadeScreen(int totalFrames, int fadeFrames) {
        super(Text.translatable("seamless_loading_screen.screen.loading_chunks"));
        this.fadeFrames = Math.min(totalFrames, fadeFrames);
        this.frames = totalFrames;
    }

    public FadeScreen then(Consumer<Boolean> callback) {
        this.callback = callback;

        return this;
    }

    @Override
    public void removed() {
        markDone(true);

        super.removed();
    }

    @Override
    protected void init() {
        if (SeamlessLoadingScreenConfig.get().playSoundEffect) {
            var id = Identifier.tryParse(SeamlessLoadingScreenConfig.get().soundEffect);

            if (id != null) {
                SoundEvent soundEvent = Registries.SOUND_EVENT.getOrEmpty(id).orElse(SoundEvents.ENTITY_ENDER_DRAGON_GROWL);

                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(soundEvent, SeamlessLoadingScreenConfig.get().soundPitch, SeamlessLoadingScreenConfig.get().soundVolume));
            } else {
                LOGGER.error("[SeamlessLoadingScreen]: Unable to parse the above SoundEffect due to it not being a valid Identifier. [Value: {}]", SeamlessLoadingScreenConfig.get().soundEffect);
            }
        }
    }

    private void markDone(boolean forceClosed) {
        if (this.done) return;

        this.done = true;

        this.frames = 0;

        if (this.callback != null) this.callback.accept(forceClosed);
        if (this.callback == null && !forceClosed && this.client != null && this.client.currentScreen == this)
            this.client.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (frames <= 0 || client == null) return;

        boolean doFade = frames <= fadeFrames;

        float alpha = doFade ? Math.min(frames / (float) fadeFrames, 1.0f) : 1.0f;

        Vector4f color = new Vector4f(1, 1, 1, alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        if (ScreenshotLoader.loaded) {
            RenderSystem.setShaderTexture(0, ScreenshotLoader.SCREENSHOT);
            int w = (int) (ScreenshotLoader.imageRatio * height);
            int h = height;
            int x = width / 2 - w / 2;
            int y = 0;

            loadQuad(context, color, x, y, x + w, y + h);

            if (w < width) {
                RenderSystem.setShaderTexture(0, 0);
                // 0.25f is from Screen.renderBackgroundTexture vertex colors
                color.set(0.25f, 0.25f, 0.25f, alpha);
                loadQuad(context, color, 0, 0, x, height, 0, 0, x / 32f, height / 32f);
                loadQuad(context, color, x + w, 0, width, height, (x + w) / 32f, 0, width / 32f, height / 32f);
            }
        } else {
            RenderSystem.setShaderTexture(0, 0);
            color.set(0.25f, 0.25f, 0.25f, alpha);
            loadQuad(context, color, 0, 0, width, height, 0, 0, width / 32f, height / 32f);
        }

        ScreenshotLoader.renderAfterEffects(this, context, alpha);

        if (!doFade) {
            context.drawCenteredTextWithShadow(client.textRenderer, title, width / 2, 70, 0xFFFFFF);
            String progress = String.valueOf(client.worldRenderer.getCompletedChunkCount());
            context.drawCenteredTextWithShadow(client.textRenderer, progress, width / 2, 90, 0xFFFFFF);
        }

        RenderSystem.disableBlend();

        frames--;

        if (frames == 0) markDone(false);
    }

    private void loadQuad(DrawContext context, Vector4f color, int x0, int y0, int x1, int y1) {
        loadQuad(context, color, x0, y0, x1, y1, 0, 0, 1, 1);
    }

    private void loadQuad(DrawContext context, Vector4f color, int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1) {
        MatrixStack stack = context.getMatrices();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f modelMat = stack.peek().getPositionMatrix();
        builder.vertex(modelMat, x0, y1, 0).texture(u0, v1).color(color.x(), color.y(), color.z(), color.w());
        builder.vertex(modelMat, x1, y1, 0).texture(u1, v1).color(color.x(), color.y(), color.z(), color.w());
        builder.vertex(modelMat, x1, y0, 0).texture(u1, v0).color(color.x(), color.y(), color.z(), color.w());
        builder.vertex(modelMat, x0, y0, 0).texture(u0, v0).color(color.x(), color.y(), color.z(), color.w());
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
}