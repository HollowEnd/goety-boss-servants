package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.GoetyBossServants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * meowhem can't start on a dedicated server (its mod class loads client rendering classes), so the
 * self-test runs in a dev client: on the title screen this creates a fresh flat world, and
 * {@link SelfTest} takes over once its integrated server has started.
 */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID, value = Dist.CLIENT)
public class ClientSelfTest {
    private static boolean started;

    /** The client's copy of an entity (read from the server thread; fine for a dev check). */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.entity.LivingEntity clientCopy(int entityId) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.getEntity(entityId) instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
    }

    /** Saves a screenshot (screenshots/<name>.png in the run folder) on the next rendered frame, without the HUD. */
    public static void screenshot(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            minecraft.options.hideGui = true;
            net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.getMainRenderTarget(), message -> {
            });
        });
    }

    /** For the {@link Showcase} shots: no HUD, no view bobbing or speed FOV, bright. */
    public static void prepareShowcase() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            minecraft.options.hideGui = true;
            minecraft.options.bobView().set(false);
            minecraft.options.fovEffectScale().set(0.0);
            minecraft.options.gamma().set(1.0);
        });
    }

    /**
     * Renders the world once at width x height (off the window's size, as vanilla's panorama shots
     * do) with the given field of view, and saves it as screenshots/<name>.png in the run folder.
     */
    public static void capture(String name, int width, int height, int fov) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            minecraft.options.hideGui = true;
            minecraft.options.fov().set(fov);
            com.mojang.blaze3d.platform.Window window = minecraft.getWindow();
            com.mojang.blaze3d.pipeline.RenderTarget target = minecraft.getMainRenderTarget();
            int oldWidth = window.getWidth(), oldHeight = window.getHeight();
            resize(minecraft, target, width, height);
            try {
                target.bindWrite(true);
                minecraft.gameRenderer.renderLevel(1.0f, net.minecraft.Util.getNanos(), new com.mojang.blaze3d.vertex.PoseStack());
                net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, name + ".png", target, message -> {
                });
            } finally {
                resize(minecraft, target, oldWidth, oldHeight);
                target.bindWrite(true);
            }
        });
    }

    private static void resize(Minecraft minecraft, com.mojang.blaze3d.pipeline.RenderTarget target, int width, int height) {
        minecraft.getWindow().setWidth(width);
        minecraft.getWindow().setHeight(height);
        target.resize(width, height, Minecraft.ON_OSX);
        minecraft.gameRenderer.resize(width, height);
        minecraft.levelRenderer.resize(width, height);
    }

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (started || !(SelfTest.ENABLED || Showcase.ENABLED || Gallery.ENABLED) || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        started = true;
        Minecraft minecraft = Minecraft.getInstance();
        LevelSettings settings = new LevelSettings("selftest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                new GameRules(), new WorldDataConfiguration(DataPackConfig.DEFAULT, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
        minecraft.createWorldOpenFlows().createFreshLevel("selftest-" + System.currentTimeMillis(), settings, new WorldOptions(0L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }
}
