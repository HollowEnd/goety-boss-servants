package com.saints.goetybossservants;

import com.saints.goetybossservants.compat.AquamiraeCompat;
import com.saints.goetybossservants.compat.BmdCompat;
import com.saints.goetybossservants.compat.KnightQuestCompat;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.compat.SoulslikeCompat;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.event.MonstrosityEvents;
import com.saints.goetybossservants.event.ServantEvents;
import com.saints.goetybossservants.registry.ModItems;
import com.saints.goetybossservants.registry.ModRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;

/** Goety is required; each boss mod is optional and only unlocks its own servants (see ModCompat). */
public class GoetyBossServants implements ModInitializer {
    public static final String MOD_ID = "goetybossservants";

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.COMMON, ServantConfig.SPEC, MOD_ID + "-common.toml");
        ModRegistry.init();
        ServantEvents.register();
        MonstrosityEvents.register();
        if (ModCompat.aquamirae()) {
            AquamiraeCompat.register();
        }
        if (ModCompat.bmd()) {
            BmdCompat.register();
        }
        if (ModCompat.soulslike()) {
            SoulslikeCompat.register();
        }
        if (ModCompat.knightquest()) {
            KnightQuestCompat.register();
        }
        ModItems.addToCreativeTab();
        startSelfTest("com.saints.goetybossservants.debug.SelfTest");
    }

    /** The dev self-test (debug package, never shipped) is only looked up when it's switched on. */
    public static void startSelfTest(String className) {
        if (!Boolean.getBoolean(MOD_ID + ".selftest")) {
            return;
        }
        try {
            Class.forName(className).getMethod("init").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Self-test requested but " + className + " is missing", e);
        }
    }
}
