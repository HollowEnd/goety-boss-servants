package com.saints.goetybossservants.client.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import ru.astemir.astemirlib.client.bedrock.animation.Animated;
import ru.astemir.astemirlib.client.bedrock.animation.data.Animator;
import ru.astemir.astemirlib.client.bedrock.model.BedrockEntityModel;
import ru.astemir.astemirlib.client.bedrock.renderer.BaseEntityRenderer;
import ru.astemir.astemirlib.client.bedrock.renderer.EntityRenderData;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * AstemirLib (The Dawn Era's library) poses its mobs' models only from its own loop over the
 * entities in the world, which never reaches a sculk zombie's copy of one (see SculkPuppets), so a
 * copy is posed here instead, just before it's drawn. Posing one moves its animations on by the time
 * since the last frame, so a copy is posed in full once a frame; any other time (drawn again, or the
 * game paused) its pose is just put back on the model, which every mob of its kind shares. Only
 * loaded with AstemirLib; if posing ever fails, copies are drawn as they are from then on.
 */
public final class AstemirPuppets {
    private static final Logger LOGGER = LogUtils.getLogger();
    // The frame each copy was last posed in full.
    private static final Map<Entity, Long> POSED = new WeakHashMap<>();
    private static long frame;
    private static boolean failed;

    private AstemirPuppets() {
    }

    /** Whether posing a copy ever failed (checked by the self-test). */
    public static boolean failed() {
        return failed;
    }

    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ++frame;
        }
    }

    /** Poses {@code puppet} if {@code renderer} draws it with AstemirLib; whether it does. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean pose(EntityRenderer<?> renderer, LivingEntity puppet, float partialTicks) {
        if (!(renderer instanceof BaseEntityRenderer bedrock) || !(puppet instanceof Animated animated)) {
            return false;
        }
        if (failed) {
            return true;
        }
        try {
            BedrockEntityModel model = bedrock.getModel(puppet);
            if (model == null) {
                return true;
            }
            Long posed = POSED.put(puppet, frame);
            if (!Minecraft.getInstance().isPaused() && (posed == null || posed != frame)) {
                model.updateAnimations(puppet, EntityRenderData.prepare(puppet, partialTicks));
            } else {
                Animator animator = animated.getOrCreateAnimator(model);
                model.resetAll();
                animator.check(model);
                animator.applyTransform(puppet);
                model.animate(animator, puppet, EntityRenderData.prepare(puppet, partialTicks));
            }
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            LOGGER.warn("Sculk zombies' copies of AstemirLib mobs are drawn unposed from now on: posing one failed", e);
        }
        return true;
    }
}
