package com.saints.goetybossservants.client;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.client.compat.AstemirPuppets;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Renderers for the servants of whichever boss mods are installed (see ModCompat), and sculk zombies' copies (SculkPuppets). */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        SculkZombie.CLIENT = SculkPuppets.INSTANCE;
        if (ModCompat.isLoaded(ModCompat.ASTEMIRLIB)) {
            MinecraftForge.EVENT_BUS.addListener(AstemirPuppets::onRenderTick);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModRegistry.MONSTROSITIES.values().forEach(type -> event.registerEntityRenderer(type.get(), MaterialMonstrosityRenderer::new));
        event.registerEntityRenderer(ModRegistry.MONSTROSITY_IRON_GOLEM.get(), MonstrosityIronGolemRenderer::new);
        event.registerEntityRenderer(ModRegistry.DIAMOND_SPIKE.get(), DiamondSpikeRenderer::new);
        event.registerEntityRenderer(ModRegistry.NETHERITE_RIFT.get(), NetheriteRiftRenderer::new);
        event.registerEntityRenderer(ModRegistry.SCULK_ZOMBIE.get(), SculkZombieRenderer::new);
        event.registerEntityRenderer(ModRegistry.SCULK_MIRE.get(), SculkMireRenderer::new);
        event.registerEntityRenderer(ModRegistry.SCULK_TENTACLE.get(), SculkTentacleRenderer::new);
        event.registerEntityRenderer(ModRegistry.SCULK_GLOB.get(), SculkGlobRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.MATERIAL_MONSTROSITY_HEAD_BE.get(), MaterialHeadRenderer.BlockRenderer::new);
        event.registerBlockEntityRenderer(ModRegistry.WARDEN_CORE_BE.get(), WardenCoreRenderer::new);
        if (ModCompat.meowhem()) {
            MeowhemClient.registerRenderers(event);
        }
        if (ModCompat.aquamirae()) {
            AquamiraeClient.registerRenderers(event);
        }
        if (ModCompat.bmd()) {
            BmdClient.registerRenderers(event);
        }
        if (ModCompat.soulslike()) {
            SoulslikeClient.registerRenderers(event);
        }
        if (ModCompat.knightquest()) {
            KnightQuestClient.registerRenderers(event);
        }
    }
}
