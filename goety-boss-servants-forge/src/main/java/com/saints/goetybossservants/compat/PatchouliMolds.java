package com.saints.goetybossservants.compat;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.construct.MiraMonstrosityMold;
import com.saints.goetybossservants.construct.MonstrosityMoldShape;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.patchouli.api.IStateMatcher;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.api.TriPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * The monstrosity molds as Patchouli multiblocks, for this mod's pages of Goety's Black Book (only
 * with Patchouli installed, which the book needs). Registered here, as Goety registers its own, so
 * the book's "Visualize" accepts what the molds really accept (any bricks, any copper block,
 * calibrated sensors...), by the molds' own tests; the pages keep an inline copy as a fallback.
 * Anchored, like Goety's Redstone Monstrosity mold, on the brick under the core.
 */
public final class PatchouliMolds {
    private PatchouliMolds() {
    }

    public static ResourceLocation id(MonstrosityMaterial material) {
        return new ResourceLocation(GoetyBossServants.MOD_ID, material.entityName() + "_mold");
    }

    public static final ResourceLocation MIRA = new ResourceLocation(GoetyBossServants.MOD_ID, "mira_monstrosity_mold");

    public static void register() {
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            MaterialMonstrosityMold.Mold mold = MaterialMonstrosityMold.mold(material);
            PatchouliAPI.get().registerMultiblock(id(material), PatchouliAPI.get().makeMultiblock(pattern(),
                    'R', matcher(mold.ringShown().get(), mold.ring()),
                    'F', matcher(mold.fillShown().get(), mold.fill()),
                    'H', matcher(mold.centerShown().get(), mold.center()),
                    'C', matcher(mold.coreShown().get(), mold.core()),
                    'S', BRICK, '0', BRICK,
                    'D', matcher(ModBlocks.DIAMOND_MOLD_BLOCK.get(), state -> state.is(ModBlocks.DIAMOND_MOLD_BLOCK.get()))));
        }
        if (ModCompat.meowhem()) {
            registerMira();
        }
    }

    // The Mira Monstrosity's own lava ring, as on its page (the page names the other two rings; a matcher
    // tests each block alone, so "any of the three" would pass a mixed ring the mold refuses).
    private static void registerMira() {
        Predicate<BlockState> fill = MiraMonstrosityMold::isFill;
        PatchouliAPI.get().registerMultiblock(MIRA, PatchouliAPI.get().makeMultiblock(pattern(),
                'R', matcher(Blocks.LAVA, state -> MiraMonstrosityMold.isRingBlock(state, MiraVariant.MIRA)),
                'F', matcher(Blocks.ORANGE_TERRACOTTA, fill),
                'H', matcher(Blocks.ORANGE_TERRACOTTA, fill),
                'C', matcher(ModBlocks.REINFORCED_REDSTONE_BLOCK.get(), MonstrosityMoldShape::isRedstoneCore),
                'S', BRICK, '0', BRICK,
                'D', matcher(ModBlocks.DIAMOND_MOLD_BLOCK.get(), state -> state.is(ModBlocks.DIAMOND_MOLD_BLOCK.get()))));
    }

    // The mold's own brick test (any full block whose id contains "bricks"), shown as stone bricks.
    private static final IStateMatcher BRICK = matcher(Blocks.STONE_BRICKS, MonstrosityMoldShape::isBrick);

    private static IStateMatcher matcher(Block shown, Predicate<BlockState> test) {
        return matcher(shown, (level, pos, state) -> test.test(state));
    }

    private static IStateMatcher matcher(Block shown, TriPredicate<BlockGetter, BlockPos, BlockState> test) {
        return new IStateMatcher() {
            @Override
            public BlockState getDisplayedState(long ticks) {
                return shown.defaultBlockState();
            }

            @Override
            public TriPredicate<BlockGetter, BlockPos, BlockState> getStatePredicate() {
                return test;
            }
        };
    }

    /**
     * MonstrosityMoldShape's 15x15 mold in Patchouli's form: layers from the top, each layer's strings
     * along x and their characters along z. R ring, F fill, H the four fill spots touching the core,
     * C core, S bricks (0 the one under the core), D diamond molds. Bricks too at the two rim spots the
     * mold never checks, as in Goety's: the pattern is then the same every way round, which it has to be,
     * since Visualize turns it to the player's facing while the mold itself never turns.
     */
    private static String[][] pattern() {
        char[][][] grid = new char[2][15][15];
        for (char[][] layer : grid) {
            for (char[] row : layer) {
                Arrays.fill(row, '_');
            }
        }
        for (BlockPos offset : MonstrosityMoldShape.FILL_LOCATIONS) {
            set(grid, offset, MonstrosityMoldShape.CENTER_LOCATIONS.contains(offset) ? 'H' : 'F');
        }
        MonstrosityMoldShape.STONE_LOCATIONS.forEach(offset -> set(grid, offset, 'S'));
        MonstrosityMoldShape.DIAMOND_LOCATIONS.forEach(offset -> set(grid, offset, 'D'));
        MonstrosityMoldShape.RING_LOCATIONS.forEach(offset -> set(grid, offset, 'R'));
        List.of(new BlockPos(-1, 0, 6), new BlockPos(-1, 0, -6)).forEach(offset -> set(grid, offset, 'S'));
        set(grid, MonstrosityMoldShape.CORE, 'C');
        set(grid, MonstrosityMoldShape.CORE.below(), '0');
        String[][] pattern = new String[2][15];
        for (int layer = 0; layer < 2; ++layer) {
            for (int x = 0; x < 15; ++x) {
                pattern[layer][x] = new String(grid[layer][x]);
            }
        }
        return pattern;
    }

    private static void set(char[][][] grid, BlockPos offset, char c) {
        grid[offset.getY() == 0 ? 0 : 1][offset.getX() + 7][offset.getZ() + 7] = c;
    }
}
