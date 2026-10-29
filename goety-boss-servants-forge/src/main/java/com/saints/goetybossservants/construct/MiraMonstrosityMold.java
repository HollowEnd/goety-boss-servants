package com.saints.goetybossservants.construct;

import com.Polarice3.Goety.common.research.ResearchList;
import com.Polarice3.Goety.utils.SEHelper;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import static com.saints.goetybossservants.construct.MonstrosityMoldShape.RING_LOCATIONS;

/**
 * The Mira Monstrosity mold: Goety's Redstone Monstrosity mold block for block (see
 * MonstrosityMoldShape), with the redstone fill swapped for orange terracotta. The variant comes
 * from the outer ring that Goety fills with lava: lava for the Mira Monstrosity, obsidian for the
 * Obsidian one, sculk for the Warden one. The core is the same Reinforced Redstone Block,
 * activated with Goety's Animation Core.
 */
public class MiraMonstrosityMold {
    public static boolean isRingBlock(BlockState state, MiraVariant variant) {
        return switch (variant) {
            case OBSIDIAN -> state.is(Blocks.OBSIDIAN);
            case WARDEN -> state.is(Blocks.SCULK);
            default -> state.is(Blocks.LAVA);
        };
    }

    public static boolean isFill(BlockState state) {
        return state.is(Blocks.ORANGE_TERRACOTTA);
    }

    /** Which variant's mold is built around this core, or null if none is complete. */
    @Nullable
    public static MiraVariant findVariant(Level level, BlockPos core) {
        if (!MonstrosityMoldShape.checkInner(level, core, MiraMonstrosityMold::isFill)) {
            return null;
        }
        for (MiraVariant variant : MiraVariant.values()) {
            if (MonstrosityMoldShape.checkAll(level, core, RING_LOCATIONS, state -> isRingBlock(state, variant))) {
                return variant;
            }
        }
        return null;
    }

    /** Counts every Mira Monstrosity servant, all variants together, against the configured limits. */
    public static boolean conditionsMet(ServerLevel level, Player player) {
        int count = 0;
        int global = 0;
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof MiraMonstrosityServant servant && servant.isAlive()) {
                    if (player.getUUID().equals(servant.getGoetyOwnerId())) {
                        ++count;
                    }
                    ++global;
                }
            }
        }
        return count < ServantConfig.MiraMonstrosityPlayerLimit.get() && global < ServantConfig.MiraMonstrosityGlobalLimit.get();
    }

    /** Server side: tries to summon the servant for a complete mold. Mirrors RedstoneMonstrosityMold.spawnServant. */
    public static void spawnServant(ServerLevel level, BlockPos core, MiraVariant variant, Player player, ItemStack stack) {
        if (ServantConfig.MiraMonstrosityNeedsResearch.get() && !SEHelper.hasResearch(player, ResearchList.TERMINUS)) {
            player.displayClientMessage(Component.translatable("info.goety.research.fail"), true);
            return;
        }
        if (!conditionsMet(level, player)) {
            player.displayClientMessage(Component.translatable("info.goety.summon.limit"), true);
            return;
        }
        MiraMonstrosityServant monstrosity = ModRegistry.MIRA_MONSTROSITY.get().create(level);
        if (monstrosity == null) {
            return;
        }
        monstrosity.setVariant(variant);
        monstrosity.servant().setTrueOwner(player);
        monstrosity.moveTo(core.getX() + 0.5, core.getY() + 0.05, core.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        monstrosity.finalizeSpawn(level, level.getCurrentDifficultyAt(core), MobSpawnType.MOB_SUMMONED, null, null);
        monstrosity.setWandering(false);
        monstrosity.setStaying(false);
        monstrosity.setBoundPos(null);
        if (level.addFreshEntity(monstrosity)) {
            removeBlocks(level, core, variant);
            // Like Goety's molds (through Item.useOn), the Animation Core is kept in creative.
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, monstrosity);
            }
        }
    }

    /** Builds a complete mold around {@code core} (stone bricks for the base). Used by the dev self-test. */
    public static void placeMold(Level level, BlockPos core, MiraVariant variant) {
        BlockState ring = switch (variant) {
            case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
            case WARDEN -> Blocks.SCULK.defaultBlockState();
            default -> Blocks.LAVA.defaultBlockState();
        };
        MonstrosityMoldShape.place(level, core, ring, Blocks.ORANGE_TERRACOTTA.defaultBlockState());
    }

    private static void removeBlocks(Level level, BlockPos core, MiraVariant variant) {
        MonstrosityMoldShape.removeUsedBlocks(level, core, state -> isRingBlock(state, variant), MiraMonstrosityMold::isFill);
    }
}
