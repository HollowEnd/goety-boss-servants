package com.saints.goetybossservants.event;

import com.Polarice3.Goety.common.items.ModItems;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.effect.LapisBuffEffects;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.registry.ModEffects;
import com.saints.goetybossservants.registry.ModRegistry;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * The material monstrosities' molds and the wardens' Warden Cores, the Gold one's stun, the Lapis
 * one's custom enchantment buffs and the Sculk one's decay, on Fabric events and our own mixins
 * (LivingEntityMixin, EnchantmentHelperMixin, ...).
 */
public final class MonstrosityEvents {
    private MonstrosityEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            // The material molds share the Redstone Monstrosity's core block (all but the Sculk one,
            // whose core is the Warden Core), and Goety only lets one mold own a core block. So the
            // Animation Core is intercepted here first: if a complete material mold surrounds the core
            // we handle it, otherwise Goety's own Animation Core logic runs as normal. (This runs before
            // vanilla's checks for spectators and adventure mode, so those are made here, as they are
            // before Goety's useOn.)
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(ModItems.ANIMATION_CORE) || !MaterialMonstrosityMold.mayActivate(player, stack, level, hit.getBlockPos())) {
                return InteractionResult.PASS;
            }
            MonstrosityMaterial material = MaterialMonstrosityMold.findMaterial(level, hit.getBlockPos());
            if (material == null) {
                // Goety has no mold for the Warden Core, so it would fail without a word: say the mold
                // is wrong, as Goety's own molds do. Only once the server has the click (a FAIL on the
                // client would keep it from the server, which may refuse it anyway, e.g. spawn protection).
                if (!level.isClientSide && level.getBlockState(hit.getBlockPos()).is(ModRegistry.WARDEN_CORE)) {
                    player.displayClientMessage(Component.translatable("info.goety.block.fail"), true);
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel serverLevel) {
                MaterialMonstrosityMold.spawnServant(serverLevel, hit.getBlockPos(), material, player, player.getItemInHand(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        });
        // Every warden drops a Warden Core, the Sculk Monstrosity mold's core.
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (id.equals(EntityType.WARDEN.getDefaultLootTable())) {
                tableBuilder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f)).add(LootItem.lootTableItem(ModRegistry.WARDEN_CORE_ITEM)));
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MonstrosityEvents::allowDamage);
        // A golden statue can't shoot.
        ServantEvents.SPAWN_FILTERS.add(entity -> !(entity instanceof Projectile projectile && isGilded(projectile.getOwner())));
        // Risen mobs (see RisenMobs): counted and given their goals as they load, and nobody tames, feeds,
        // rides, trades with, leashes or breeds one.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> RisenMobs.joined(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> RisenMobs.left(entity));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> RisenMobs.isRisen(entity) ? InteractionResult.FAIL : InteractionResult.PASS);
        // Fabric calls this before the entity's spawn packet goes out, so send it on the next tick.
        EntityTrackingEvents.START_TRACKING.register((target, player) -> {
            if (target instanceof LivingEntity living && living.getActiveEffects().stream().anyMatch(instance -> shown(instance.getEffect()))) {
                player.server.tell(new TickTask(player.server.getTickCount() + 1, () -> {
                    for (MobEffectInstance instance : living.getActiveEffects()) {
                        if (shown(instance.getEffect()) && !player.hasDisconnected()) {
                            player.connection.send(new ClientboundUpdateMobEffectPacket(living.getId(), instance));
                        }
                    }
                }));
            }
        });
    }

    // ----- Gilded (Gold Monstrosity) -------------------------------------------------------------

    public static boolean isGilded(Object entity) {
        return entity instanceof LivingEntity living && living.hasEffect(ModEffects.GILDED);
    }

    /**
     * A golden statue can't attack. The monstrosities' area attacks (Goety's melee smash and slam
     * included) never land on their own side, players, pets or player-owned servants, unless that
     * is what they're fighting; the same rule as the boss servants (see ServantEvents). And the
     * Lapis Monstrosity's buffs that act on hits do so here (LapisBuffEffects.afterHit).
     */
    private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (isGilded(source.getEntity()) && source.getEntity() == source.getDirectEntity()) {
            return false;
        }
        if (source.getEntity() instanceof MaterialMonstrosity monstrosity && monstrosity != victim && !monstrosity.canHitWithArea(victim)) {
            return false;
        }
        // (The same for the mobs risen for them, see RisenMobs.)
        if (RisenMobs.blocksDamage(source, victim)) {
            return false;
        }
        LapisBuffEffects.afterHit(victim, source, amount);
        return true;
    }

    /** Called every tick for every living entity (LivingEntityMixin): a golden statue doesn't move. */
    public static void tickGilded(LivingEntity entity) {
        if (isGilded(entity)) {
            entity.setDeltaMovement(0.0, Math.min(0.0, entity.getDeltaMovement().y), 0.0);
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setJumping(false);
            }
        }
    }

    // Minecraft only tells a player about their own effects; everyone nearby needs to see the statue (and
    // which mobs are risen, and rising, to draw them so).
    private static boolean shown(MobEffect effect) {
        return effect == ModEffects.GILDED || effect == ModEffects.SCULK_RISEN || effect == ModEffects.SCULK_RISING;
    }

    public static void syncShown(LivingEntity entity, MobEffectInstance instance) {
        if (entity.level() instanceof ServerLevel level && shown(instance.getEffect())) {
            level.getChunkSource().broadcast(entity, new ClientboundUpdateMobEffectPacket(entity.getId(), instance));
        }
    }

    public static void syncShownRemoved(LivingEntity entity, MobEffect effect) {
        if (entity.level() instanceof ServerLevel level && shown(effect)) {
            level.getChunkSource().broadcast(entity, new ClientboundRemoveMobEffectPacket(entity.getId(), effect));
        }
    }
}
