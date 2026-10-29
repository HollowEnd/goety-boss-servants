package com.saints.goetybossservants.construct;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The shape of Goety's Redstone Monstrosity mold, shared by every monstrosity mold here (Mira and
 * the material ones). Offsets are relative to the core block and copied from Goety's
 * RedstoneMonstrosityMold: a brick base, diamond mold blocks, an outer ring (lava in Goety's), and
 * the fill on top (redstone blocks in Goety's) around a Reinforced Redstone Block core. The Sculk
 * Monstrosity's mold puts its own blocks in the four fill spots touching the core and its own core.
 */
public final class MonstrosityMoldShape {
    private static final List<BlockPos> BOTTOM_STONE_LOCATIONS = ImmutableList.of(
            new BlockPos(0, -1, 0), new BlockPos(0, -1, 2), new BlockPos(0, -1, 3), new BlockPos(0, -1, 4), new BlockPos(0, -1, -2), new BlockPos(0, -1, -3), new BlockPos(0, -1, -4),
            new BlockPos(1, -1, 1), new BlockPos(1, -1, -1), new BlockPos(1, -1, 2), new BlockPos(1, -1, -2), new BlockPos(1, -1, 3), new BlockPos(1, -1, -3), new BlockPos(1, -1, 5), new BlockPos(1, -1, -5),
            new BlockPos(-1, -1, 1), new BlockPos(-1, -1, -1), new BlockPos(-1, -1, 2), new BlockPos(-1, -1, -2), new BlockPos(-1, -1, 3), new BlockPos(-1, -1, -3), new BlockPos(-1, -1, 5), new BlockPos(-1, -1, -5),
            new BlockPos(2, -1, 0), new BlockPos(2, -1, 1), new BlockPos(2, -1, -1), new BlockPos(2, -1, 2), new BlockPos(2, -1, -2), new BlockPos(2, -1, 3), new BlockPos(2, -1, -3), new BlockPos(2, -1, 4), new BlockPos(2, -1, -4),
            new BlockPos(-2, -1, 0), new BlockPos(-2, -1, 1), new BlockPos(-2, -1, -1), new BlockPos(-2, -1, 2), new BlockPos(-2, -1, -2), new BlockPos(-2, -1, 3), new BlockPos(-2, -1, -3), new BlockPos(-2, -1, 4), new BlockPos(-2, -1, -4),
            new BlockPos(3, -1, 0), new BlockPos(3, -1, 1), new BlockPos(3, -1, -1), new BlockPos(3, -1, 2), new BlockPos(3, -1, -2), new BlockPos(3, -1, 3), new BlockPos(3, -1, -3), new BlockPos(3, -1, 4), new BlockPos(3, -1, -4),
            new BlockPos(-3, -1, 0), new BlockPos(-3, -1, 1), new BlockPos(-3, -1, -1), new BlockPos(-3, -1, 2), new BlockPos(-3, -1, -2), new BlockPos(-3, -1, 3), new BlockPos(-3, -1, -3), new BlockPos(-3, -1, 4), new BlockPos(-3, -1, -4),
            new BlockPos(4, -1, 0), new BlockPos(4, -1, 2), new BlockPos(4, -1, -2), new BlockPos(4, -1, 3), new BlockPos(4, -1, -3),
            new BlockPos(-4, -1, 0), new BlockPos(-4, -1, 2), new BlockPos(-4, -1, -2), new BlockPos(-4, -1, 3), new BlockPos(-4, -1, -3),
            new BlockPos(5, -1, 1), new BlockPos(5, -1, -1), new BlockPos(-5, -1, 1), new BlockPos(-5, -1, -1));
    private static final List<BlockPos> ABOVE_STONE_LOCATIONS = ImmutableList.of(
            new BlockPos(1, 0, 6), new BlockPos(1, 0, -6), new BlockPos(3, 0, 5), new BlockPos(3, 0, -5), new BlockPos(-3, 0, 5), new BlockPos(-3, 0, -5),
            new BlockPos(5, 0, 3), new BlockPos(5, 0, -3), new BlockPos(-5, 0, 3), new BlockPos(-5, 0, -3), new BlockPos(6, 0, 1), new BlockPos(6, 0, -1),
            new BlockPos(-6, 0, 1), new BlockPos(-6, 0, -1));
    public static final List<BlockPos> STONE_LOCATIONS = Stream.of(BOTTOM_STONE_LOCATIONS, ABOVE_STONE_LOCATIONS).flatMap(List::stream).toList();

    private static final List<BlockPos> BOTTOM_DIAMOND_LOCATIONS = ImmutableList.of(
            new BlockPos(0, -1, 1), new BlockPos(0, -1, -1), new BlockPos(0, -1, 5), new BlockPos(0, -1, -5), new BlockPos(1, -1, 0), new BlockPos(1, -1, 4), new BlockPos(1, -1, -4),
            new BlockPos(-1, -1, 0), new BlockPos(-1, -1, 4), new BlockPos(-1, -1, -4), new BlockPos(2, -1, 5), new BlockPos(2, -1, -5),
            new BlockPos(-2, -1, 5), new BlockPos(-2, -1, -5), new BlockPos(4, -1, 1), new BlockPos(4, -1, -1), new BlockPos(-4, -1, 1), new BlockPos(-4, -1, -1),
            new BlockPos(5, -1, 0), new BlockPos(5, -1, 2), new BlockPos(5, -1, -2), new BlockPos(-5, -1, 0), new BlockPos(-5, -1, 2), new BlockPos(-5, -1, -2));
    private static final List<BlockPos> ABOVE_DIAMOND_LOCATIONS = ImmutableList.of(
            new BlockPos(0, 0, 6), new BlockPos(0, 0, -6), new BlockPos(2, 0, 6), new BlockPos(2, 0, -6), new BlockPos(-2, 0, 6), new BlockPos(-2, 0, -6),
            new BlockPos(4, 0, 4), new BlockPos(4, 0, -4), new BlockPos(-4, 0, 4), new BlockPos(-4, 0, -4), new BlockPos(6, 0, 0), new BlockPos(6, 0, 2),
            new BlockPos(6, 0, -2), new BlockPos(-6, 0, 0), new BlockPos(-6, 0, 2), new BlockPos(-6, 0, -2));
    public static final List<BlockPos> DIAMOND_LOCATIONS = Stream.of(BOTTOM_DIAMOND_LOCATIONS, ABOVE_DIAMOND_LOCATIONS).flatMap(List::stream).toList();

    // Goety's lava positions.
    private static final List<BlockPos> BOTTOM_RING_LOCATIONS = ImmutableList.of(
            new BlockPos(0, -1, 6), new BlockPos(0, -1, -6), new BlockPos(1, -1, 6), new BlockPos(1, -1, -6), new BlockPos(-1, -1, 6), new BlockPos(-1, -1, -6),
            new BlockPos(2, -1, 6), new BlockPos(2, -1, -6), new BlockPos(-2, -1, 6), new BlockPos(-2, -1, -6), new BlockPos(3, -1, 5), new BlockPos(3, -1, -5),
            new BlockPos(-3, -1, 5), new BlockPos(-3, -1, -5), new BlockPos(4, -1, 4), new BlockPos(4, -1, -4), new BlockPos(-4, -1, 4), new BlockPos(-4, -1, -4),
            new BlockPos(5, -1, 3), new BlockPos(5, -1, -3), new BlockPos(-5, -1, 3), new BlockPos(-5, -1, -3), new BlockPos(6, -1, 0), new BlockPos(6, -1, 1),
            new BlockPos(6, -1, -1), new BlockPos(6, -1, 2), new BlockPos(6, -1, -2), new BlockPos(-6, -1, 0), new BlockPos(-6, -1, 1), new BlockPos(-6, -1, -1),
            new BlockPos(-6, -1, 2), new BlockPos(-6, -1, -2));
    private static final List<BlockPos> ABOVE_RING_LOCATIONS = ImmutableList.of(
            new BlockPos(0, 0, 7), new BlockPos(0, 0, -7), new BlockPos(1, 0, 7), new BlockPos(1, 0, -7), new BlockPos(-1, 0, 7), new BlockPos(-1, 0, -7),
            new BlockPos(2, 0, 7), new BlockPos(2, 0, -7), new BlockPos(-2, 0, 7), new BlockPos(-2, 0, -7), new BlockPos(3, 0, 6), new BlockPos(3, 0, -6),
            new BlockPos(-3, 0, 6), new BlockPos(-3, 0, -6), new BlockPos(4, 0, 5), new BlockPos(4, 0, -5), new BlockPos(-4, 0, 5), new BlockPos(-4, 0, -5),
            new BlockPos(5, 0, 4), new BlockPos(5, 0, -4), new BlockPos(-5, 0, 4), new BlockPos(-5, 0, -4), new BlockPos(6, 0, 3), new BlockPos(6, 0, -3),
            new BlockPos(-6, 0, 3), new BlockPos(-6, 0, -3), new BlockPos(7, 0, 0), new BlockPos(7, 0, 1), new BlockPos(7, 0, -1), new BlockPos(7, 0, 2),
            new BlockPos(7, 0, -2), new BlockPos(-7, 0, 0), new BlockPos(-7, 0, 1), new BlockPos(-7, 0, -1), new BlockPos(-7, 0, 2), new BlockPos(-7, 0, -2));
    public static final List<BlockPos> RING_LOCATIONS = Stream.of(BOTTOM_RING_LOCATIONS, ABOVE_RING_LOCATIONS).flatMap(List::stream).toList();

    // Goety's redstone block positions.
    public static final List<BlockPos> FILL_LOCATIONS = buildFillLocations();
    // The four fill spots touching the core (sculk shriekers in the Sculk Monstrosity's mold).
    public static final List<BlockPos> CENTER_LOCATIONS = ImmutableList.of(new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1));
    public static final BlockPos CORE = BlockPos.ZERO;

    private MonstrosityMoldShape() {
    }

    private static List<BlockPos> buildFillLocations() {
        ImmutableList.Builder<BlockPos> builder = ImmutableList.builder();
        // Every top-layer spot inside the ring except the core, matching Goety's REDSTONE_LOCATIONS:
        // |x| <= 2 reaches |z| = 5, |x| = 3 and 4 reach |z| = 4 and 3, |x| = 5 reaches |z| = 2.
        int[] reach = {5, 5, 5, 4, 3, 2};
        for (int x = -5; x <= 5; ++x) {
            int maxZ = reach[Math.abs(x)];
            for (int z = -maxZ; z <= maxZ; ++z) {
                if (x != 0 || z != 0) {
                    builder.add(new BlockPos(x, 0, z));
                }
            }
        }
        return builder.build();
    }

    public static boolean checkStones(Level level, BlockPos core) {
        for (BlockPos offset : STONE_LOCATIONS) {
            BlockPos pos = core.offset(offset);
            if (!isBrick(level, pos, level.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    /** Same rule as Goety: any full block whose id contains "bricks" (stone, deepslate, ...). */
    public static boolean isBrick(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getBlock().getDescriptionId().contains("bricks") && state.isSolidRender(level, pos);
    }

    public static boolean checkAll(Level level, BlockPos core, List<BlockPos> offsets, Predicate<BlockState> test) {
        for (BlockPos offset : offsets) {
            if (!test.test(level.getBlockState(core.offset(offset)))) {
                return false;
            }
        }
        return true;
    }

    /** Goety's core block, the Reinforced Redstone Block. */
    public static boolean isRedstoneCore(BlockState state) {
        return state.is(ModBlocks.REINFORCED_REDSTONE_BLOCK.get());
    }

    /** The core, diamond molds and fill (not the ring) are in place. */
    public static boolean checkInner(Level level, BlockPos core, Predicate<BlockState> fill) {
        return checkInner(level, core, MonstrosityMoldShape::isRedstoneCore, fill, fill);
    }

    /** Like the above, with its own core block, and {@code center} in the four fill spots touching the core. */
    public static boolean checkInner(Level level, BlockPos core, Predicate<BlockState> coreBlock, Predicate<BlockState> fill, Predicate<BlockState> center) {
        if (!coreBlock.test(level.getBlockState(core))) {
            return false;
        }
        for (BlockPos offset : FILL_LOCATIONS) {
            if (!(CENTER_LOCATIONS.contains(offset) ? center : fill).test(level.getBlockState(core.offset(offset)))) {
                return false;
            }
        }
        return checkAll(level, core, DIAMOND_LOCATIONS, state -> state.is(ModBlocks.DIAMOND_MOLD_BLOCK.get()))
                && checkStones(level, core);
    }

    /** Builds a complete mold around {@code core} (stone bricks for the base). Used by the dev self-test. */
    public static void place(Level level, BlockPos core, BlockState ring, BlockState fill) {
        place(level, core, ring, fill, fill, ModBlocks.REINFORCED_REDSTONE_BLOCK.get().defaultBlockState());
    }

    /** Like the above, with {@code center} in the four fill spots touching the core and its own core block. */
    public static void place(Level level, BlockPos core, BlockState ring, BlockState fill, BlockState center, BlockState coreBlock) {
        STONE_LOCATIONS.forEach(offset -> level.setBlockAndUpdate(core.offset(offset), Blocks.STONE_BRICKS.defaultBlockState()));
        DIAMOND_LOCATIONS.forEach(offset -> level.setBlockAndUpdate(core.offset(offset), ModBlocks.DIAMOND_MOLD_BLOCK.get().defaultBlockState()));
        RING_LOCATIONS.forEach(offset -> level.setBlockAndUpdate(core.offset(offset), ring));
        FILL_LOCATIONS.forEach(offset -> level.setBlockAndUpdate(core.offset(offset), CENTER_LOCATIONS.contains(offset) ? center : fill));
        level.setBlockAndUpdate(core, coreBlock);
    }

    /** Like Goety, the brick base and diamond molds stay; the fill, ring and core are used up. */
    public static void removeUsedBlocks(Level level, BlockPos core, Predicate<BlockState> ring, Predicate<BlockState> fill) {
        removeUsedBlocks(level, core, ring, fill, fill, MonstrosityMoldShape::isRedstoneCore);
    }

    /** Like the above, with {@code center} for the four fill spots touching the core and its own core block. */
    public static void removeUsedBlocks(Level level, BlockPos core, Predicate<BlockState> ring, Predicate<BlockState> fill, Predicate<BlockState> center,
                                        Predicate<BlockState> coreBlock) {
        for (BlockPos offset : RING_LOCATIONS) {
            BlockPos pos = core.offset(offset);
            if (ring.test(level.getBlockState(pos))) {
                breakBlock(level, pos);
            }
        }
        for (BlockPos offset : FILL_LOCATIONS) {
            BlockPos pos = core.offset(offset);
            if ((CENTER_LOCATIONS.contains(offset) ? center : fill).test(level.getBlockState(pos))) {
                breakBlock(level, pos);
            }
        }
        BlockPos corePos = core.offset(CORE);
        if (coreBlock.test(level.getBlockState(corePos))) {
            breakBlock(level, corePos);
        }
    }

    private static void breakBlock(Level level, BlockPos pos) {
        level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
