package com.saints.goetybossservants.client;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.compat.PatchouliMolds;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.saints.goetybossservants.registry.ModRegistry;

/** Renderers for the servants of whichever boss mods are installed (see ModCompat). */
public class GoetyBossServantsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ModRegistry.registerAttributes();
            // The molds for this mod's pages of Goety's Black Book, which needs Patchouli (see PatchouliMolds). Only the
            // client's book uses them, and by now Goety has made its blocks (its own config must load first).
            if (ModCompat.isLoaded("patchouli")) {
                PatchouliMolds.register();
            }
        });
        ModRegistry.MONSTROSITIES.values().forEach(type -> EntityRendererRegistry.register(type, MaterialMonstrosityRenderer::new));
        EntityRendererRegistry.register(ModRegistry.MONSTROSITY_IRON_GOLEM, MonstrosityIronGolemRenderer::new);
        EntityRendererRegistry.register(ModRegistry.DIAMOND_SPIKE, DiamondSpikeRenderer::new);
        EntityRendererRegistry.register(ModRegistry.NETHERITE_RIFT, NetheriteRiftRenderer::new);
        // Sculk zombies' copies of the mobs they were are kept up on the client (SculkPuppets).
        SculkZombie.CLIENT = SculkPuppets.INSTANCE;
        EntityRendererRegistry.register(ModRegistry.SCULK_ZOMBIE, SculkZombieRenderer::new);
        EntityRendererRegistry.register(ModRegistry.SCULK_MIRE, SculkMireRenderer::new);
        EntityRendererRegistry.register(ModRegistry.SCULK_TENTACLE, SculkTentacleRenderer::new);
        EntityRendererRegistry.register(ModRegistry.SCULK_GLOB, SculkGlobRenderer::new);
        BlockEntityRenderers.register(ModRegistry.MATERIAL_MONSTROSITY_HEAD_BE, MaterialHeadRenderer.BlockRenderer::new);
        BlockEntityRenderers.register(ModRegistry.WARDEN_CORE_BE, WardenCoreRenderer::new);
        BuiltinItemRendererRegistry.INSTANCE.register(ModRegistry.MATERIAL_MONSTROSITY_HEAD_ITEM, new MaterialHeadRenderer.ItemRenderer());
        if (ModCompat.aquamirae()) {
            AquamiraeClient.registerRenderers();
        }
        if (ModCompat.bmd()) {
            BmdClient.registerRenderers();
        }
        if (ModCompat.soulslike()) {
            SoulslikeClient.registerRenderers();
        }
        if (ModCompat.knightquest()) {
            KnightQuestClient.registerRenderers();
        }
        GoetyBossServants.startSelfTest("com.saints.goetybossservants.debug.ClientSelfTest");
    }
}
