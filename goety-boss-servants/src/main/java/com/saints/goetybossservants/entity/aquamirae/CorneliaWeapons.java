package com.saints.goetybossservants.entity.aquamirae;

import dev.obscuria.aquamirae.registry.AquamiraeEffects;
import dev.obscuria.aquamirae.registry.AquamiraeItems;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Helpers for the servant's weapon choice (CaptainCorneliaServant.chooseWeapon), using Aquamirae 7's weapons. */
public final class CorneliaWeapons {
    /** Health Shatter stacks (5% max health each) go up to 10: 5 of her Shatterblade hits (see applyShatter). */
    public static final int SHATTER_MAX_STACKS = 10;
    private static final float SHATTER_REDUCTION = 0.05f;
    private static final int STACKS_PER_HIT = 2;
    private static final int TICKS_PER_HIT = 60 * 20;

    private CorneliaWeapons() {
    }

    /** Health Shatter stacks on the target (the effect's level is stacks - 1). */
    public static int shatterStacks(LivingEntity target) {
        MobEffectInstance effect = target.getEffect(AquamiraeEffects.HEALTH_SHATTER.get());
        return effect == null ? 0 : Math.min(SHATTER_MAX_STACKS, effect.getAmplifier() + 1);
    }

    /**
     * Her Shatterblade hits, reworked like our old Divider: Aquamirae adds one Health Shatter stack (5%)
     * for 12 seconds; hers add two stacks (10% max health) per hit, lasting a minute per hit she has
     * landed: up to 50% for 5 minutes after 5 hits. Replaces the stack Aquamirae just added.
     */
    public static void applyShatter(LivingEntity target, int stacksBefore) {
        int stacks = Math.min(SHATTER_MAX_STACKS, stacksBefore + STACKS_PER_HIT);
        int hits = (stacks + STACKS_PER_HIT - 1) / STACKS_PER_HIT;
        target.removeEffect(AquamiraeEffects.HEALTH_SHATTER.get());
        target.addEffect(new MobEffectInstance(AquamiraeEffects.HEALTH_SHATTER.get(), hits * TICKS_PER_HIT, stacks - 1));
    }

    /** The target's max health before any Health Shatter, so a cut-down boss still counts as big. */
    public static float originalMaxHealth(LivingEntity target) {
        return target.getMaxHealth() / (1.0f - SHATTER_REDUCTION * shatterStacks(target));
    }

    /** What one hit with this weapon from her would do to the target after its armor. */
    public static float weaponDamage(LivingEntity cornelia, Item weapon, LivingEntity target) {
        double damage = cornelia.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        for (AttributeModifier modifier : new ItemStack(weapon).getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                damage += modifier.getAmount();
            }
        }
        return CombatRules.getDamageAfterAbsorb((float) damage, (float) target.getArmorValue(), (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    public static boolean canBePoisoned(LivingEntity target) {
        return target.canBeAffected(new MobEffectInstance(MobEffects.POISON, 80, 2));
    }

    /** Aquamirae dual-wields these, with a copy in the off hand; her main weapon and the Shatterspike are one-handed. */
    public static boolean dualWielded(Item item) {
        return item == AquamiraeItems.REMNANT_SABER.get() || item == AquamiraeItems.SHATTERBLADE.get() || item == AquamiraeItems.TERRIBLE_BLADE.get();
    }
}
