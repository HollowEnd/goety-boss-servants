package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Lapis Monstrosity's "enchantments": each one it can grant is its own effect (see ModEffects)
 * that does what the enchantment does, for whoever has it, at the book's level. Enchantments with
 * no sensible buff (Silk Touch, Infinity, Multishot, curses, modded ones, ...) aren't listed, so a
 * book holding only those is refused.
 */
public final class LapisBuffs {
    /** Book levels above this (from commands or mods) are treated as this. */
    public static final int MAX_LEVEL = 10;

    /** The effect an enchantment becomes, and its amplifier (the level minus one). */
    public record Buff(Supplier<MobEffect> effect, int amplifier) {
    }

    private static Map<Enchantment, Supplier<MobEffect>> buffs;

    private LapisBuffs() {
    }

    // Built on first use: the effects only exist once registries are filled.
    private static Map<Enchantment, Supplier<MobEffect>> buffs() {
        if (buffs == null) {
            Map<Enchantment, Supplier<MobEffect>> map = new LinkedHashMap<>();
            map.put(Enchantments.ALL_DAMAGE_PROTECTION, ModEffects.PROTECTION);
            map.put(Enchantments.FIRE_PROTECTION, ModEffects.FIRE_PROTECTION);
            map.put(Enchantments.BLAST_PROTECTION, ModEffects.BLAST_PROTECTION);
            map.put(Enchantments.PROJECTILE_PROTECTION, ModEffects.PROJECTILE_PROTECTION);
            map.put(Enchantments.FALL_PROTECTION, ModEffects.FEATHER_FALLING);
            map.put(Enchantments.THORNS, ModEffects.THORNS);
            map.put(Enchantments.RESPIRATION, ModEffects.RESPIRATION);
            map.put(Enchantments.AQUA_AFFINITY, ModEffects.AQUA_AFFINITY);
            map.put(Enchantments.DEPTH_STRIDER, ModEffects.DEPTH_STRIDER);
            map.put(Enchantments.FROST_WALKER, ModEffects.FROST_WALKER);
            map.put(Enchantments.SOUL_SPEED, ModEffects.SOUL_SPEED);
            map.put(Enchantments.SWIFT_SNEAK, ModEffects.SWIFT_SNEAK);
            map.put(Enchantments.SHARPNESS, ModEffects.SHARPNESS);
            map.put(Enchantments.SMITE, ModEffects.SMITE);
            map.put(Enchantments.BANE_OF_ARTHROPODS, ModEffects.BANE_OF_ARTHROPODS);
            map.put(Enchantments.KNOCKBACK, ModEffects.KNOCKBACK);
            map.put(Enchantments.FIRE_ASPECT, ModEffects.FIRE_ASPECT);
            map.put(Enchantments.MOB_LOOTING, ModEffects.LOOTING);
            map.put(Enchantments.SWEEPING_EDGE, ModEffects.SWEEPING);
            map.put(Enchantments.POWER_ARROWS, ModEffects.POWER);
            map.put(Enchantments.PUNCH_ARROWS, ModEffects.PUNCH);
            map.put(Enchantments.FLAMING_ARROWS, ModEffects.FLAME);
            map.put(Enchantments.IMPALING, ModEffects.IMPALING);
            map.put(Enchantments.BLOCK_EFFICIENCY, ModEffects.EFFICIENCY);
            map.put(Enchantments.BLOCK_FORTUNE, ModEffects.FORTUNE);
            map.put(Enchantments.FISHING_LUCK, ModEffects.LUCK_OF_THE_SEA);
            map.put(Enchantments.UNBREAKING, ModEffects.UNBREAKING);
            map.put(Enchantments.MENDING, ModEffects.MENDING);
            buffs = map;
        }
        return buffs;
    }

    /** The effect an enchantment becomes, or null if it has none. */
    @Nullable
    public static MobEffect effect(Enchantment enchantment) {
        Supplier<MobEffect> effect = buffs().get(enchantment);
        return effect != null ? effect.get() : null;
    }

    /** The buff's level on this entity (0 without it): what it adds to the enchantment's level. */
    public static int level(LivingEntity entity, Supplier<MobEffect> effect) {
        var instance = entity.getEffect(effect.get());
        return instance != null ? instance.getAmplifier() + 1 : 0;
    }

    /** A random buff at level I (used when no book is held). */
    public static Buff random(RandomSource random) {
        List<Supplier<MobEffect>> all = new ArrayList<>(buffs().values());
        return new Buff(all.get(random.nextInt(all.size())), 0);
    }

    /** The book's enchantments that can become buffs, at the book's levels, in the book's order. */
    public static List<Buff> fromBook(ItemStack book) {
        List<Buff> result = new ArrayList<>();
        if (book.getItem() instanceof EnchantedBookItem) {
            for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(book)).entrySet()) {
                Supplier<MobEffect> effect = buffs().get(entry.getKey());
                if (effect != null) {
                    result.add(new Buff(effect, Math.max(1, Math.min(MAX_LEVEL, entry.getValue())) - 1));
                }
            }
        }
        return result;
    }

    /** The book's enchantments that can become buffs, with their levels, in the book's order. */
    public static Map<Enchantment, Integer> usableEnchantments(ItemStack book) {
        Map<Enchantment, Integer> usable = new LinkedHashMap<>();
        if (book.getItem() instanceof EnchantedBookItem) {
            EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(book)).forEach((enchantment, level) -> {
                if (buffs().containsKey(enchantment)) {
                    usable.put(enchantment, level);
                }
            });
        }
        return usable;
    }
}
