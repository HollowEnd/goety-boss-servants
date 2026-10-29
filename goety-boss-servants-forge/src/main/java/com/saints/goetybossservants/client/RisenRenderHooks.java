package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Risen mobs (see RisenMobs) drawn as sculk zombies are: climbing up out of the ground while they
 * rise, and drawn a second time in only the sculk grown over them (SculkedTextures). Done around the
 * call that draws each entity (EntityRenderDispatcherMixin), and for mobs a renderer draws some
 * other way (as AstemirLib does), around the living-renderer events instead. Render thread only.
 */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID, value = Dist.CLIENT)
public final class RisenRenderHooks {
    // Mobs being drawn through the dispatcher now, and through the events.
    private static final Set<Entity> WRAPPED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Entity> FALLBACK = Collections.newSetFromMap(new IdentityHashMap<>());
    // While drawing a sculk overlay (so it isn't drawn over again).
    private static boolean overlaying;

    private RisenRenderHooks() {
    }

    /** Just before {@code entity}'s renderer draws it (its pose stack where it stands). */
    public static void before(Entity entity, float partialTicks, PoseStack poseStack) {
        if (overlaying || !RisenMobs.isRisen(entity) || entity instanceof SculkZombie) {
            return;
        }
        WRAPPED.add(entity);
        poseStack.translate(0.0f, -sink(entity, partialTicks), 0.0f);
    }

    /** Just after: its sculk, and back up. */
    public static void after(EntityRenderDispatcher dispatcher, Entity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (!WRAPPED.remove(entity)) {
            return;
        }
        overlay(dispatcher.getRenderer(entity), entity, yaw, partialTicks, poseStack, buffers, light);
        poseStack.translate(0.0f, sink(entity, partialTicks), 0.0f);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (overlaying || WRAPPED.contains(entity) || !RisenMobs.isRisen(entity) || entity instanceof SculkZombie) {
            return;
        }
        FALLBACK.add(entity);
        event.getPoseStack().translate(0.0f, -sink(entity, event.getPartialTick()), 0.0f);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!FALLBACK.remove(entity)) {
            return;
        }
        float partialTicks = event.getPartialTick();
        overlay(event.getRenderer(), entity, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()), partialTicks, event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
        event.getPoseStack().translate(0.0f, sink(entity, partialTicks), 0.0f);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void overlay(EntityRenderer renderer, Entity entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        overlaying = true;
        // Its own pose stack, so a renderer that fails part way can't leave the world's unbalanced.
        PoseStack own = new PoseStack();
        own.last().pose().set(poseStack.last().pose());
        own.last().normal().set(poseStack.last().normal());
        try {
            renderer.render(entity, yaw, partialTicks, own, SculkedTextures.overlay(buffers), light);
        } catch (RuntimeException ignored) {
            // No sculk on it this frame.
        } finally {
            overlaying = false;
        }
    }

    // How far below the ground it still is.
    private static float sink(Entity entity, float partialTicks) {
        MobEffectInstance rising = entity instanceof LivingEntity living ? living.getEffect(ModEffects.SCULK_RISING.get()) : null;
        if (rising == null) {
            return 0.0f;
        }
        float left = Mth.clamp(rising.getDuration() - partialTicks, 0.0f, SculkZombie.RISE_TICKS) / SculkZombie.RISE_TICKS;
        return left * entity.getBbHeight();
    }
}
