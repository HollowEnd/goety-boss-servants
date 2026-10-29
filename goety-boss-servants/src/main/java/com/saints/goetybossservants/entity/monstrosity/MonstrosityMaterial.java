package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.GoetyBossServants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The seven material monstrosities. Each is Goety's Redstone Monstrosity with its own stats, skin,
 * mold fill and special attack in place of the Redstone Cube summon (see MonstrositySpecials).
 * Default stats scale around the Redstone Monstrosity's 600 health / 21 damage / 0 armor.
 */
public enum MonstrosityMaterial {
    COPPER("copper", 450.0, 16.0, 2.0, 0xB4684D, 0x4FA58A),
    IRON("iron", 650.0, 22.0, 6.0, 0xD8D8D8, 0x5AA8FF),
    GOLD("gold", 380.0, 14.0, 4.0, 0xF5D33A, 0xFFAA1A),
    LAPIS("lapis", 420.0, 14.0, 2.0, 0x2A4FA8, 0xB45CFF),
    DIAMOND("diamond", 800.0, 26.0, 10.0, 0x6FE8DA, 0xCFFFFF),
    NETHERITE("netherite", 1000.0, 30.0, 15.0, 0x3B3335, 0xFF5A14),
    SCULK("sculk", 900.0, 28.0, 8.0, 0x0B2A33, 0x19E6E6);

    private final String id;
    public final double defaultHealth;
    public final double defaultDamage;
    public final double defaultArmor;
    public final int eggColor;
    public final int accentColor;

    MonstrosityMaterial(String id, double health, double damage, double armor, int eggColor, int accentColor) {
        this.id = id;
        this.defaultHealth = health;
        this.defaultDamage = damage;
        this.defaultArmor = armor;
        this.eggColor = eggColor;
        this.accentColor = accentColor;
    }

    public String getId() {
        return this.id;
    }

    /** How much bigger than Goety's Redstone Monstrosity it is (model and hitbox): the Netherite one, the strongest, towers over the rest. */
    public float scale() {
        return this == NETHERITE ? 1.2f : 1.0f;
    }

    /** Registry name of this material's entity, e.g. "copper_monstrosity". */
    public String entityName() {
        return this.id + "_monstrosity";
    }

    public Component typeName() {
        return Component.translatable("entity." + GoetyBossServants.MOD_ID + "." + this.entityName());
    }

    /**
     * The blocks that fill this material's mold and revive shape and repair it (any copper block
     * counts for copper). The Sculk one's mold is its own, of sculk catalysts (see MaterialMonstrosityMold).
     */
    public List<Block> fillBlocks() {
        return switch (this) {
            case COPPER -> List.of(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER,
                    Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_EXPOSED_COPPER, Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER);
            case IRON -> List.of(Blocks.IRON_BLOCK);
            case GOLD -> List.of(Blocks.GOLD_BLOCK);
            case LAPIS -> List.of(Blocks.LAPIS_BLOCK);
            case DIAMOND -> List.of(Blocks.DIAMOND_BLOCK);
            case NETHERITE -> List.of(Blocks.NETHERITE_BLOCK);
            case SCULK -> List.of(Blocks.SCULK);
        };
    }

    public Block fillBlock() {
        return this.fillBlocks().get(0);
    }

    public boolean isFill(BlockState state) {
        for (Block block : this.fillBlocks()) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }

    public static MonstrosityMaterial byId(String id) {
        for (MonstrosityMaterial material : values()) {
            if (material.id.equals(id)) {
                return material;
            }
        }
        return COPPER;
    }
}
