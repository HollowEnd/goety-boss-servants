package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.effect.LapisBuffEffects;
import com.saints.goetybossservants.entity.monstrosity.DecayedBy;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.monstrosity.SculkDecay;
import com.saints.goetybossservants.event.MonstrosityEvents;
import com.saints.goetybossservants.event.ServantEvents;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric stand-ins for the Forge events the servants rely on (see ServantEvents), and where Forge
 * keeps a mod's own data on an entity (who decayed it, see DecayedBy).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements DecayedBy.Holder {
    @Shadow
    @Final
    private Map<MobEffect, MobEffectInstance> activeEffects;

    @Shadow
    protected abstract void onEffectRemoved(MobEffectInstance effect);

    @Unique
    private static final String goetybossservants$DECAYED_BY = "goetybossservants:SculkDecayedBy";
    @Unique
    @Nullable
    private UUID goetybossservants$decayedBy;

    @Override
    @Nullable
    public UUID goetybossservants$getDecayedBy() {
        return this.goetybossservants$decayedBy;
    }

    @Override
    public void goetybossservants$setDecayedBy(@Nullable UUID by) {
        this.goetybossservants$decayedBy = by;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void goetybossservants$saveDecayedBy(CompoundTag tag, CallbackInfo ci) {
        if (this.goetybossservants$decayedBy != null) {
            tag.putUUID(goetybossservants$DECAYED_BY, this.goetybossservants$decayedBy);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void goetybossservants$loadDecayedBy(CompoundTag tag, CallbackInfo ci) {
        this.goetybossservants$decayedBy = tag.hasUUID(goetybossservants$DECAYED_BY) ? tag.getUUID(goetybossservants$DECAYED_BY) : null;
    }

    /**
     * What dies decaying rises as a sculk zombie (Sculk Monstrosity, see SculkDecay): Forge's
     * LivingDeathEvent, as the death starts (after totems, before it drops anything, so the zombie's
     * look keeps the dead mob's gear).
     */
    @Inject(method = "die", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;dead:Z", opcode = Opcodes.PUTFIELD))
    private void goetybossservants$sculkRise(DamageSource source, CallbackInfo ci) {
        SculkDecay.onDeath((LivingEntity) (Object) this);
    }
    /** No new effects on someone a boss servant's attack was just stopped on. */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$spareEffects(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        // (And only this mod gives the risen effects: a risen creeper's lingering cloud would pass them on.)
        boolean risenEffect = effect.getEffect() == ModEffects.SCULK_RISEN || effect.getEffect() == ModEffects.SCULK_RISING;
        if (ServantEvents.blocksEffect(self) || risenEffect && !self.level().isClientSide && !RisenMobs.giving()) {
            cir.setReturnValue(false);
        }
    }

    /** Minions and risen mobs drop no loot or gear (no farming a servant's summons). */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$noMinionLoot(DamageSource source, CallbackInfo ci) {
        if (ServantMinions.isMinion((LivingEntity) (Object) this) || RisenMobs.isRisen((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    /** Gear a corpse that rose as itself dropped isn't kept on what rose (Forge: LivingDropsEvent). */
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void goetybossservants$afterCorpseDrops(DamageSource source, CallbackInfo ci) {
        RisenMobs.afterCorpseDrops((LivingEntity) (Object) this);
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$noMinionExperience(CallbackInfo ci) {
        if (ServantMinions.isMinion((LivingEntity) (Object) this) || RisenMobs.isRisen((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    /** Minions follow their boss, and go when it's gone (ServantMinions); risen mobs fight for their monstrosity, and crumble
     * when their time is up (RisenMobs); boss servants join their owner's fights. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void goetybossservants$minionTick(CallbackInfo ci) {
        if ((Object) this instanceof Mob mob && !mob.level().isClientSide && ServantMinions.isMinion(mob)) {
            ServantMinions.tick(mob);
        }
        if ((Object) this instanceof Mob mob && !mob.level().isClientSide && RisenMobs.isRisen(mob)) {
            RisenMobs.tick(mob);
        }
        if ((Object) this instanceof BossServant servant) {
            BossServant.followOwnersFight(servant);
            BossServant.logTargetingStatus(servant);
        }
    }

    /** The Lapis Monstrosity's buffs that change damage (Forge's LivingHurtEvent; see LapisBuffEffects). */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float goetybossservants$buffDamage(float amount, DamageSource source) {
        return LapisBuffEffects.modifyDamage((LivingEntity) (Object) this, source, amount);
    }

    /**
     * The Sculk Monstrosity's attacks decay what they hit (Forge's LivingHurtEvent: once a hit gets
     * past the time after the last one, just before it is dealt, with what is left of it after any
     * shield; see SculkDecay).
     */
    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void goetybossservants$sculkDecay(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        SculkDecay.onHit((LivingEntity) (Object) this, source, amount);
    }

    /** A golden statue (Gold Monstrosity) doesn't move or jump. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void goetybossservants$gildedTick(CallbackInfo ci) {
        MonstrosityEvents.tickGilded((LivingEntity) (Object) this);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$gildedJump(CallbackInfo ci) {
        if (MonstrosityEvents.isGilded(this)) {
            ci.cancel();
        }
    }

    // Everyone nearby needs to see who is gilded, risen or rising (Minecraft only tells players their own effects).
    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void goetybossservants$syncAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        MonstrosityEvents.syncShown((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void goetybossservants$syncUpdated(MobEffectInstance effect, boolean reapply, Entity source, CallbackInfo ci) {
        MonstrosityEvents.syncShown((LivingEntity) (Object) this, effect);
    }

    /** Nothing cuts a risen mob's time short (cleansing, /effect): it crumbles when it runs out (RisenMobs). */
    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$keepRisen(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if ((effect == ModEffects.SCULK_RISEN || effect == ModEffects.SCULK_RISING) && RisenMobs.isRisen((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void goetybossservants$syncRemoved(MobEffectInstance effect, CallbackInfo ci) {
        // (A risen mob whose time is up crumbles next tick: its sculk stays on until it's gone.)
        if (!(effect.getEffect() == ModEffects.SCULK_RISEN && RisenMobs.isRisen((LivingEntity) (Object) this))) {
            MonstrosityEvents.syncShownRemoved((LivingEntity) (Object) this, effect.getEffect());
        }
    }

    /** ...nor clearing all its effects (/effect clear, cleansing): the rest go, its time stays (Forge: MobEffectEvent.Remove). */
    @Inject(method = "removeAllEffects", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$keepRisenTime(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || !RisenMobs.isRisen(self)) {
            return;
        }
        boolean removed = false;
        Iterator<MobEffectInstance> effects = this.activeEffects.values().iterator();
        while (effects.hasNext()) {
            MobEffectInstance effect = effects.next();
            if (effect.getEffect() != ModEffects.SCULK_RISEN && effect.getEffect() != ModEffects.SCULK_RISING) {
                this.onEffectRemoved(effect);
                effects.remove();
                removed = true;
            }
        }
        cir.setReturnValue(removed);
    }
}
