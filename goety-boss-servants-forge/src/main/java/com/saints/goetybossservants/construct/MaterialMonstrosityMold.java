package com.saints.goetybossservants.construct;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.research.ResearchList;
import com.Polarice3.Goety.utils.SEHelper;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.saints.goetybossservants.construct.MonstrosityMoldShape.CENTER_LOCATIONS;
import static com.saints.goetybossservants.construct.MonstrosityMoldShape.RING_LOCATIONS;

/**
 * The material monstrosity molds: Goety's Redstone Monstrosity mold exactly (lava ring included,
 * see MonstrosityMoldShape), with the redstone block fill swapped for the material's block. The
 * fill decides which monstrosity it builds. Activated with Goety's Animation Core on the core.
 *
 * The Sculk Monstrosity's mold is its own, on the same shape: a ring of sculk sensors (calibrated
 * ones too) for the lava, sculk catalysts for the fill, four sculk shriekers round the core, and a
 * Warden Core (dropped by wardens) for the core.
 */
public final class MaterialMonstrosityMold {
    private MaterialMonstrosityMold() {
    }

    /**
     * What goes where in a material's mold (the brick base and diamond molds are always Goety's), and
     * the block shown for each part (in the self-test's molds and the Black Book's, see PatchouliMolds).
     */
    public record Mold(Predicate<BlockState> ring, Predicate<BlockState> fill, Predicate<BlockState> center, Predicate<BlockState> core,
                       Supplier<Block> ringShown, Supplier<Block> fillShown, Supplier<Block> centerShown, Supplier<Block> coreShown) {
        boolean isComplete(Level level, BlockPos core) {
            return MonstrosityMoldShape.checkInner(level, core, this.core, this.fill, this.center)
                    && MonstrosityMoldShape.checkAll(level, core, RING_LOCATIONS, this.ring);
        }
    }

    private static final Mold SCULK_MOLD = new Mold(state -> state.is(Blocks.SCULK_SENSOR) || state.is(Blocks.CALIBRATED_SCULK_SENSOR),
            state -> state.is(Blocks.SCULK_CATALYST), state -> state.is(Blocks.SCULK_SHRIEKER), state -> state.is(ModRegistry.WARDEN_CORE.get()),
            () -> Blocks.SCULK_SENSOR, () -> Blocks.SCULK_CATALYST, () -> Blocks.SCULK_SHRIEKER, () -> ModRegistry.WARDEN_CORE.get());

    public static Mold mold(MonstrosityMaterial material) {
        if (material == MonstrosityMaterial.SCULK) {
            return SCULK_MOLD;
        }
        return new Mold(state -> state.is(Blocks.LAVA), material::isFill, material::isFill, MonstrosityMoldShape::isRedstoneCore,
                () -> Blocks.LAVA, material::fillBlock, material::fillBlock, () -> ModBlocks.REINFORCED_REDSTONE_BLOCK.get());
    }

    /**
     * Whether the player may use an Animation Core on this block at all, as vanilla decides before an
     * item's useOn (where Goety's own molds run): not a spectator, and allowed to build there
     * (in adventure mode only with the Animation Core's CanPlaceOn for the block).
     */
    public static boolean mayActivate(Player player, ItemStack stack, Level level, BlockPos pos) {
        return !player.isSpectator() && (player.mayBuild()
                || stack.hasAdventureModePlaceTagForBlock(level.registryAccess().registryOrThrow(Registries.BLOCK), new BlockInWorld(level, pos, false)));
    }

    /** Which material's mold is built around this core, or null if none is complete. */
    @Nullable
    public static MonstrosityMaterial findMaterial(Level level, BlockPos core) {
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            if (mold(material).isComplete(level, core)) {
                return material;
            }
        }
        return null;
    }

    /** Like Goety's limits, counted per material: each material has its own per-player and world limit. */
    public static boolean conditionsMet(ServerLevel level, Player player, MonstrosityMaterial material) {
        int count = 0;
        int global = 0;
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof MaterialMonstrosity monstrosity && monstrosity.isAlive() && monstrosity.material() == material) {
                    if (player.getUUID().equals(monstrosity.getOwnerId())) {
                        ++count;
                    }
                    ++global;
                }
            }
        }
        return count < ServantConfig.MaterialMonstrosityPlayerLimit.get() && global < ServantConfig.MaterialMonstrosityGlobalLimit.get();
    }

    /** Server side: tries to build the monstrosity for a complete mold. Mirrors Goety's RedstoneMonstrosityMold.spawnServant. */
    public static void spawnServant(ServerLevel level, BlockPos core, MonstrosityMaterial material, Player player, ItemStack stack) {
        if (ServantConfig.MaterialMonstrosityNeedsResearch.get() && !SEHelper.hasResearch(player, ResearchList.TERMINUS)) {
            player.displayClientMessage(Component.translatable("info.goety.research.fail"), true);
            return;
        }
        if (!conditionsMet(level, player, material)) {
            player.displayClientMessage(Component.translatable("info.goety.summon.limit"), true);
            return;
        }
        MaterialMonstrosity monstrosity = MaterialMonstrosity.create(level, material);
        if (monstrosity == null) {
            return;
        }
        monstrosity.setTrueOwner(player);
        monstrosity.moveTo(core.getX() + 0.5, core.getY() + 0.05, core.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        monstrosity.finalizeSpawn(level, level.getCurrentDifficultyAt(core), MobSpawnType.MOB_SUMMONED, null, null);
        monstrosity.setWandering(false);
        monstrosity.setStaying(false);
        monstrosity.setBoundPos(null);
        if (level.addFreshEntity(monstrosity)) {
            if (material == MonstrosityMaterial.SCULK) {
                // Its four shriekers shriek one last time as it wakes.
                for (BlockPos offset : CENTER_LOCATIONS) {
                    level.levelEvent(LevelEvent.PARTICLES_SCULK_SHRIEK, core.offset(offset), 0);
                }
            }
            Mold mold = mold(material);
            MonstrosityMoldShape.removeUsedBlocks(level, core, mold.ring(), mold.fill(), mold.center(), mold.core());
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
    public static void placeMold(Level level, BlockPos core, MonstrosityMaterial material) {
        Mold mold = mold(material);
        MonstrosityMoldShape.place(level, core, mold.ringShown().get().defaultBlockState(), mold.fillShown().get().defaultBlockState(),
                mold.centerShown().get().defaultBlockState(), mold.coreShown().get().defaultBlockState());
    }
}
