package com.saints.goetybossservants.debug;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jetbrains.annotations.Nullable;

/**
 * The self-test runs in a dev client: on the title screen this creates a fresh flat world, and
 * {@link SelfTest} takes over once its integrated server has started.
 */
public class ClientSelfTest {
    private static boolean started;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (started || !(screen instanceof TitleScreen)) {
                return;
            }
            started = true;
            LevelSettings settings = new LevelSettings("selftest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), new WorldDataConfiguration(DataPackConfig.DEFAULT, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
            client.createWorldOpenFlows().createFreshLevel("selftest-" + System.currentTimeMillis(), settings, new WorldOptions(0L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        });
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

    /** The client's copy of an entity (read from the server thread; fine for a dev check). */
    @Nullable
    public static LivingEntity clientCopy(int entityId) {
        net.minecraft.client.multiplayer.ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.getEntity(entityId) instanceof LivingEntity living ? living : null;
    }
}
