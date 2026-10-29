package com.saints.goetybossservants.registry;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.effect.BuffEffect;
import com.saints.goetybossservants.effect.MendingEffect;
import com.saints.goetybossservants.effect.SculkDecayEffect;
import com.saints.goetybossservants.effect.SculkRisenEffect;
import com.saints.goetybossservants.effect.SharpnessEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Effects from the material monstrosities: the Gold one's stun, the Netherite one's armor break
 * and the Lapis one's enchantment buffs. Every buff is its own effect (never a vanilla one), so
 * it stacks with Strength, Speed, Resistance and the like and with the enchantments on your gear.
 * What each does is in LapisBuffEffects; the level is the book's enchantment level.
 */
public final class ModEffects {
    /** Turned to gold: can't move, attack or shoot (see MonstrosityEvents), and drawn as a gold statue. */
    public static final MobEffect GILDED = register("gilded", new BuffEffect(MobEffectCategory.HARMFUL, 0xF5D33A)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6f1d2b0e-5d5c-4a8e-9d43-2a7a3f0c9b11", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, "0b8f4f63-2b3e-4f86-8d0e-7d7e6c1f5a22", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "c2a3e8f1-7b4d-4c55-9f61-1e2d3c4b5a33", 1.0, AttributeModifier.Operation.ADDITION));
    /** Armor cracked by the Netherite Monstrosity landing on it: half armor and armor toughness. */
    public static final MobEffect SUNDERED = register("sundered", new BuffEffect(MobEffectCategory.HARMFUL, 0x4A2A28)
            .addAttributeModifier(Attributes.ARMOR, "5e7a9c1b-3d2f-4b8e-a6c4-9f1e2d3c4b44", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.ARMOR_TOUGHNESS, "7a1c3e5f-9b2d-4f6a-8c4e-1d3f5a7c9e55", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** The Sculk Monstrosity's stacking damage over time (see SculkDecay). */
    public static final MobEffect SCULK_DECAY = register("sculk_decay", new SculkDecayEffect());
    /** A mob that died with it, risen as itself for a while (see RisenMobs). */
    public static final MobEffect SCULK_RISEN = register("sculk_risen", new SculkRisenEffect());
    /** ...still climbing out of the ground: it can't move, hurt or be hurt. */
    public static final MobEffect SCULK_RISING = register("sculk_rising", new SculkRisenEffect()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "2d6b8f4e-1c3a-4e7b-9f2d-6a8c0e1b3d55", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, "8a1e3c5b-7d9f-4b2a-a6c8-0e2f4d6b8a66", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // Armour enchantments.
    public static final MobEffect PROTECTION = buff("protection", 0x9FA8C8);
    public static final MobEffect FIRE_PROTECTION = buff("fire_protection", 0xE8642A);
    public static final MobEffect BLAST_PROTECTION = buff("blast_protection", 0x6B6B6B);
    public static final MobEffect PROJECTILE_PROTECTION = buff("projectile_protection", 0xB8A27A);
    public static final MobEffect FEATHER_FALLING = buff("feather_falling", 0xF2F2F2);
    public static final MobEffect THORNS = buff("thorns", 0x6E8B3D);
    public static final MobEffect RESPIRATION = buff("respiration", 0x5FB8E8);
    public static final MobEffect AQUA_AFFINITY = buff("aqua_affinity", 0x3FD6C8);
    public static final MobEffect DEPTH_STRIDER = buff("depth_strider", 0x2A5FC8);
    public static final MobEffect FROST_WALKER = buff("frost_walker", 0xA8DCFF);
    public static final MobEffect SOUL_SPEED = buff("soul_speed", 0x4FD6D6);
    public static final MobEffect SWIFT_SNEAK = buff("swift_sneak", 0x1E6B6B);
    // Weapon enchantments.
    public static final MobEffect SHARPNESS = register("sharpness", new SharpnessEffect(MobEffectCategory.BENEFICIAL, 0xD8D8E8)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, "3c8e5a2d-41f6-4b7e-9a1c-5d2e7f8a9b01", 1.0, AttributeModifier.Operation.ADDITION));
    public static final MobEffect SMITE = buff("smite", 0xF0E6A0);
    public static final MobEffect BANE_OF_ARTHROPODS = buff("bane_of_arthropods", 0x8A3A5A);
    public static final MobEffect KNOCKBACK = buff("knockback", 0x8FC85A);
    public static final MobEffect FIRE_ASPECT = buff("fire_aspect", 0xFF7A1A);
    public static final MobEffect LOOTING = buff("looting", 0x3D6BE0);
    public static final MobEffect SWEEPING = buff("sweeping", 0x7FE8E0);
    public static final MobEffect POWER = buff("power", 0xC89A5A);
    public static final MobEffect PUNCH = buff("punch", 0xE8D85A);
    public static final MobEffect FLAME = buff("flame", 0xFFA830);
    public static final MobEffect IMPALING = buff("impaling", 0x5AC8B0);
    // Tool enchantments.
    public static final MobEffect EFFICIENCY = buff("efficiency", 0xFFD84A);
    public static final MobEffect FORTUNE = buff("fortune", 0x4AE8E8);
    public static final MobEffect LUCK_OF_THE_SEA = register("luck_of_the_sea", new BuffEffect(MobEffectCategory.BENEFICIAL, 0x2A8AC8)
            .addAttributeModifier(Attributes.LUCK, "8e1f4c7a-2b5d-4e9f-a3c6-7d1e2f3a4b02", 1.0, AttributeModifier.Operation.ADDITION));
    public static final MobEffect UNBREAKING = buff("unbreaking", 0xA8A8A8);
    public static final MobEffect MENDING = register("mending", new MendingEffect(MobEffectCategory.BENEFICIAL, 0x7FD66B));

    private ModEffects() {
    }

    private static MobEffect buff(String name, int color) {
        return register(name, new BuffEffect(MobEffectCategory.BENEFICIAL, color));
    }

    private static MobEffect register(String name, MobEffect effect) {
        return Registry.register(BuiltInRegistries.MOB_EFFECT, GoetyBossServants.id(name), effect);
    }

    /** Loads this class, registering the effects. */
    public static void init() {
    }
}
