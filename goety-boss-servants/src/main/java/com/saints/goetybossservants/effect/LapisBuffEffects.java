package com.saints.goetybossservants.effect;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;


/**
 * What the Lapis Monstrosity's enchantment buffs do. Each is its own effect and adds on top of
 * everything else: vanilla effects, the enchantments on your gear and each other. L is the buff's
 * level (the book's enchantment level).
 *
 * <ul>
 * <li>Movement and tool buffs (Respiration, Aqua Affinity, Depth Strider, Frost Walker, Soul Speed,
 * Swift Sneak, Efficiency, Looting) add L to the enchantment's level on your gear
 * ({@link #enchantmentBonus}), so they work exactly like the enchantment and stack with it.
 * Fortune does the same for block drops ({@link #fortuneBonus}).</li>
 * <li>Fire Aspect, Knockback and Sweeping do too for players; mobs, which mostly don't use those
 * enchantments, get them on their melee hits instead ({@link #afterHit}).</li>
 * <li>Protection buffs, Smite, Bane of Arthropods, Impaling and Power change the damage
 * ({@link #modifyDamage}); Thorns, Punch and Flame act on hits ({@link #afterHit}); Sharpness,
 * Luck of the Sea and Mending are effects on their own (see ModEffects); Unbreaking saves
 * durability ({@link #savesDurability}).</li>
 * </ul>
 */
public final class LapisBuffEffects {
    // No sweeping or thorns off the damage they cause themselves.
    private static boolean reacting;

    private LapisBuffEffects() {
    }

    private static int level(LivingEntity entity, MobEffect effect) {
        MobEffectInstance instance = entity.getEffect(effect);
        return instance != null ? instance.getAmplifier() + 1 : 0;
    }

    /** Levels the buffs add to an enchantment on this entity's gear (EnchantmentHelperMixin). */
    public static int enchantmentBonus(Enchantment enchantment, LivingEntity entity) {
        if (entity == null || entity.getActiveEffectsMap().isEmpty()) {
            return 0;
        }
        if (enchantment == Enchantments.RESPIRATION) {
            return level(entity, ModEffects.RESPIRATION);
        } else if (enchantment == Enchantments.AQUA_AFFINITY) {
            return level(entity, ModEffects.AQUA_AFFINITY);
        } else if (enchantment == Enchantments.DEPTH_STRIDER) {
            return level(entity, ModEffects.DEPTH_STRIDER);
        } else if (enchantment == Enchantments.FROST_WALKER) {
            return level(entity, ModEffects.FROST_WALKER);
        } else if (enchantment == Enchantments.SOUL_SPEED) {
            return level(entity, ModEffects.SOUL_SPEED);
        } else if (enchantment == Enchantments.SWIFT_SNEAK) {
            return level(entity, ModEffects.SWIFT_SNEAK);
        } else if (enchantment == Enchantments.BLOCK_EFFICIENCY) {
            return level(entity, ModEffects.EFFICIENCY);
        } else if (enchantment == Enchantments.MOB_LOOTING) {
            return level(entity, ModEffects.LOOTING);
        } else if (entity instanceof Player) {
            if (enchantment == Enchantments.FIRE_ASPECT) {
                return level(entity, ModEffects.FIRE_ASPECT);
            } else if (enchantment == Enchantments.KNOCKBACK) {
                return level(entity, ModEffects.KNOCKBACK);
            } else if (enchantment == Enchantments.SWEEPING_EDGE) {
                return level(entity, ModEffects.SWEEPING);
            }
        }
        return 0;
    }

    /** Extra Fortune levels for block drops, from whoever broke the block (ApplyBonusCountMixin). */
    public static int fortuneBonus(Enchantment enchantment, LootContext context) {
        if (enchantment == Enchantments.BLOCK_FORTUNE && context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity entity) {
            return level(entity, ModEffects.FORTUNE);
        }
        return 0;
    }

    /** Unbreaking: like the enchantment, each point of wear has an L in L+1 chance to be ignored (ItemStackMixin). */
    public static boolean savesDurability(LivingEntity entity) {
        int unbreaking = entity != null ? level(entity, ModEffects.UNBREAKING) : 0;
        return unbreaking > 0 && entity.getRandom().nextInt(unbreaking + 1) > 0;
    }

    /** The damage after the victim's and attacker's buffs, before armour. */
    public static float modifyDamage(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0.0f) {
            return amount;
        }
        Entity cause = source.getEntity();
        if (cause instanceof LivingEntity attacker && attacker != victim) {
            boolean melee = source.getDirectEntity() == attacker;
            boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
            if (melee) {
                // Smite and Bane of Arthropods: +2.5 per level against their kind.
                if (victim.getMobType() == MobType.UNDEAD) {
                    amount += 2.5f * level(attacker, ModEffects.SMITE);
                }
                if (victim.getMobType() == MobType.ARTHROPOD) {
                    amount += 2.5f * level(attacker, ModEffects.BANE_OF_ARTHROPODS);
                }
            }
            if ((melee || projectile) && (victim.isInWaterOrRain() || victim.getMobType() == MobType.WATER)) {
                // Impaling: +2.5 per level against anything wet.
                amount += 2.5f * level(attacker, ModEffects.IMPALING);
            }
            if (projectile) {
                // Power: arrows and other shots hit 25% harder per level, plus 25%.
                int power = level(attacker, ModEffects.POWER);
                if (power > 0) {
                    amount *= 1.25f + 0.25f * power;
                }
            }
        }
        if (!source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) && !victim.getActiveEffectsMap().isEmpty()) {
            // Protection: 6% less of all damage per level; the specific ones 15% less of theirs per level.
            float reduction = 0.06f * level(victim, ModEffects.PROTECTION);
            if (source.is(DamageTypeTags.IS_FIRE)) {
                reduction += 0.15f * level(victim, ModEffects.FIRE_PROTECTION);
            }
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                reduction += 0.15f * level(victim, ModEffects.BLAST_PROTECTION);
            }
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                reduction += 0.15f * level(victim, ModEffects.PROJECTILE_PROTECTION);
            }
            if (source.is(DamageTypeTags.IS_FALL)) {
                reduction += 0.15f * level(victim, ModEffects.FEATHER_FALLING);
            }
            amount *= 1.0f - Math.min(0.8f, reduction);
        }
        return amount;
    }

    /** Side effects of a hit that is landing: Thorns, Fire Aspect, Knockback, Sweeping, Bane's slowness, Punch and Flame. */
    public static void afterHit(LivingEntity victim, DamageSource source, float amount) {
        if (reacting || !(source.getEntity() instanceof LivingEntity attacker) || attacker == victim || source.is(DamageTypes.THORNS)) {
            return;
        }
        reacting = true;
        try {
            if (source.getDirectEntity() == attacker && !source.is(DamageTypeTags.IS_PROJECTILE)) {
                melee(victim, attacker, source, amount);
            } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                int punch = level(attacker, ModEffects.PUNCH);
                if (punch > 0) {
                    Entity shot = source.getDirectEntity() != null ? source.getDirectEntity() : attacker;
                    victim.knockback(0.6 * punch, shot.getX() - victim.getX(), shot.getZ() - victim.getZ());
                }
                if (level(attacker, ModEffects.FLAME) > 0) {
                    victim.setSecondsOnFire(5);
                }
            }
        } finally {
            reacting = false;
        }
    }

    private static void melee(LivingEntity victim, LivingEntity attacker, DamageSource source, float amount) {
        int thorns = level(victim, ModEffects.THORNS);
        if (thorns > 0 && victim.getRandom().nextFloat() < 0.15f * thorns) {
            attacker.hurt(victim.damageSources().thorns(victim), 1 + victim.getRandom().nextInt(4));
        }
        int bane = level(attacker, ModEffects.BANE_OF_ARTHROPODS);
        if (bane > 0 && victim.getMobType() == MobType.ARTHROPOD) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + victim.getRandom().nextInt(10 * bane), 3));
        }
        if (attacker instanceof Player) {
            return; // Players get Fire Aspect, Knockback and Sweeping through their attack (enchantmentBonus).
        }
        int fire = level(attacker, ModEffects.FIRE_ASPECT);
        if (fire > 0) {
            victim.setSecondsOnFire(4 * fire);
        }
        int knockback = level(attacker, ModEffects.KNOCKBACK);
        if (knockback > 0) {
            victim.knockback(0.5 * knockback, attacker.getX() - victim.getX(), attacker.getZ() - victim.getZ());
        }
        int sweeping = level(attacker, ModEffects.SWEEPING);
        if (sweeping > 0) {
            float splash = amount * sweeping / (sweeping + 1.0f);
            for (LivingEntity other : victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(1.0, 0.25, 1.0),
                    other -> other != victim && other != attacker && sweepable(attacker, other))) {
                other.knockback(0.4, attacker.getX() - other.getX(), attacker.getZ() - other.getZ());
                other.hurt(source, splash);
            }
        }
    }

    // A sweep only catches monsters that aren't on the attacker's side.
    private static boolean sweepable(LivingEntity attacker, LivingEntity other) {
        return other.isAlive() && other instanceof Enemy && !(other instanceof Player) && !MobUtil.areAllies(attacker, other)
                && !(other instanceof OwnableEntity pet && pet.getOwnerUUID() != null)
                && !(other instanceof IOwned owned && owned.getOwnerId() != null);
    }
}
