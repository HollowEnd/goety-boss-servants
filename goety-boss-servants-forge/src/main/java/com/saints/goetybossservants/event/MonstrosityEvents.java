package com.saints.goetybossservants.event;

import com.Polarice3.Goety.common.items.ModItems;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.effect.LapisBuffEffects;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.monstrosity.SculkDecay;
import com.saints.goetybossservants.registry.ModEffects;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The material monstrosities' molds and the wardens' Warden Cores, the Gold one's stun, the Lapis one's custom enchantment buffs and the Sculk one's decay. */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID)
public class MonstrosityEvents {

    // ----- Molds ---------------------------------------------------------------------------------

    /**
     * The material molds share the Redstone Monstrosity's core block (all but the Sculk one, whose
     * core is the Warden Core), and Goety only lets one mold own a core block. So the Animation Core
     * is intercepted here first: if a complete material mold surrounds the core we handle it,
     * otherwise Goety's own Animation Core logic runs as normal. (Forge asks before vanilla's checks
     * for spectators and adventure mode, so those are made here, as they are before Goety's useOn.)
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.ANIMATION_CORE.get()) || event.getUseItem() == Event.Result.DENY
                || !MaterialMonstrosityMold.mayActivate(event.getEntity(), event.getItemStack(), event.getLevel(), event.getPos())) {
            return;
        }
        MonstrosityMaterial material = MaterialMonstrosityMold.findMaterial(event.getLevel(), event.getPos());
        if (material == null) {
            // Goety has no mold for the Warden Core, so it would fail without a word: say the mold is
            // wrong, as Goety's own molds do.
            if (event.getLevel().getBlockState(event.getPos()).is(ModRegistry.WARDEN_CORE.get())) {
                if (!event.getLevel().isClientSide) {
                    event.getEntity().displayClientMessage(Component.translatable("info.goety.block.fail"), true);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
            }
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MaterialMonstrosityMold.spawnServant(serverLevel, event.getPos(), material, event.getEntity(), event.getItemStack());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    /**
     * Every warden drops a Warden Core, the Sculk Monstrosity mold's core: a pool in vanilla's warden
     * table (so it also rolls where another table pulls the warden's in, as on Fabric), and a loot
     * modifier for a warden table a datapack replaces, which Forge skips this event for (see AddItemModifier).
     */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (event.getName().equals(EntityType.WARDEN.getDefaultLootTable())) {
            event.getTable().addPool(LootPool.lootPool().name(GoetyBossServants.MOD_ID + ":warden_core").setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(ModRegistry.WARDEN_CORE_ITEM.get())).build());
        }
    }

    // ----- Gilded (Gold Monstrosity) -------------------------------------------------------------

    private static boolean isGilded(Object entity) {
        return entity instanceof LivingEntity living && living.hasEffect(ModEffects.GILDED.get());
    }

    /**
     * A golden statue can't attack. And the monstrosities' area attacks (Goety's melee smash and
     * slam included) never land on their own side, players, pets or player-owned servants, unless
     * that is what they're fighting; the same rule as the boss servants (see ModEvents).
     */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (isGilded(event.getSource().getEntity()) && event.getSource().getEntity() == event.getSource().getDirectEntity()) {
            event.setCanceled(true);
        } else if (event.getSource().getEntity() instanceof MaterialMonstrosity monstrosity && monstrosity != event.getEntity()
                && !monstrosity.canHitWithArea(event.getEntity())) {
            event.setCanceled(true);
        } else if (RisenMobs.blocksDamage(event.getSource(), event.getEntity())) {
            // (The same for the mobs risen for them, see RisenMobs.)
            event.setCanceled(true);
        }
    }

    /** ...or shoot... */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && !event.loadedFromDisk() && event.getEntity() instanceof Projectile projectile && isGilded(projectile.getOwner())) {
            event.setCanceled(true);
        }
    }

    /** ...or move (its speed is zeroed by the effect; this also stops falls being cushioned, jumps and pathing). */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (isGilded(entity)) {
            entity.setDeltaMovement(0.0, Math.min(0.0, entity.getDeltaMovement().y), 0.0);
            if (entity instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setJumping(false);
            }
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (isGilded(event.getEntity())) {
            event.getEntity().setDeltaMovement(0.0, 0.0, 0.0);
        }
    }

    // Minecraft only tells a player about their own effects; everyone nearby needs to see the statue (and
    // which mobs are risen, and rising, to draw them so).
    private static boolean shown(MobEffect effect) {
        return effect == ModEffects.GILDED.get() || effect == ModEffects.SCULK_RISEN.get() || effect == ModEffects.SCULK_RISING.get();
    }

    private static void syncShown(LivingEntity entity, MobEffectInstance instance) {
        if (entity.level() instanceof ServerLevel level && shown(instance.getEffect())) {
            level.getChunkSource().broadcast(entity, new ClientboundUpdateMobEffectPacket(entity.getId(), instance));
        }
    }

    private static void syncShownRemoved(LivingEntity entity, MobEffect effect) {
        if (entity.level() instanceof ServerLevel level && shown(effect)) {
            level.getChunkSource().broadcast(entity, new ClientboundRemoveMobEffectPacket(entity.getId(), effect));
        }
    }

    /** Only this mod gives the risen effects (a risen creeper's lingering cloud would pass them on, /effect give...). */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if ((effect == ModEffects.SCULK_RISEN.get() || effect == ModEffects.SCULK_RISING.get()) && !event.getEntity().level().isClientSide
                && !RisenMobs.giving()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        syncShown(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        // Nothing cuts a risen mob's time short (milk, cleansing): it crumbles when it runs out.
        if ((event.getEffect() == ModEffects.SCULK_RISEN.get() || event.getEffect() == ModEffects.SCULK_RISING.get()) && RisenMobs.isRisen(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEffect() != null) {
            syncShownRemoved(event.getEntity(), event.getEffect());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        // (A risen mob whose time is up crumbles next tick: its sculk stays on until it's gone.)
        if (event.getEffectInstance() != null && !(event.getEffectInstance().getEffect() == ModEffects.SCULK_RISEN.get() && RisenMobs.isRisen(event.getEntity()))) {
            syncShownRemoved(event.getEntity(), event.getEffectInstance().getEffect());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living && event.getEntity() instanceof ServerPlayer player) {
            for (MobEffectInstance instance : living.getActiveEffects()) {
                if (shown(instance.getEffect())) {
                    player.connection.send(new ClientboundUpdateMobEffectPacket(living.getId(), instance));
                }
            }
        }
    }

    // ----- Lapis Monstrosity buffs ---------------------------------------------------------------

    /**
     * The buffs that change damage or act on hits (see LapisBuffEffects; the rest work through
     * mixins), and the Sculk one's attacks decaying what they hit.
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        event.setAmount(LapisBuffEffects.modifyDamage(event.getEntity(), event.getSource(), event.getAmount()));
        LapisBuffEffects.afterHit(event.getEntity(), event.getSource(), event.getAmount());
        SculkDecay.onHit(event.getEntity(), event.getSource(), event.getAmount());
    }

    // ----- Sculk Decay (Sculk Monstrosity) -------------------------------------------------------

    /** What dies decaying rises as a sculk zombie (last, so not when something else saved it). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        SculkDecay.onDeath(event.getEntity());
    }
}
