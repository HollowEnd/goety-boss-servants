package com.saints.goetybossservants.entity.monstrosity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Where the floor is, for what the monstrosities leave on (or raise from) the ground. */
public final class Ground {
    private Ground() {
    }

    /**
     * The top (as it looks) of the floor nearest {@code y} in the column at ({@code x}, {@code z}), from a
     * block above it down to five below: of a block that could be stood on (with room above it), so not a
     * roof, overhang or leaves over it, nor the side of a tree or wall, nor water. {@code y} itself if
     * there is none.
     */
    public static double floorAt(BlockGetter level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blockX = Mth.floor(x), blockY = Mth.floor(y), blockZ = Mth.floor(z);
        double floor = y;
        double nearest = Double.MAX_VALUE;
        for (int dy = 1; dy >= -5; --dy) {
            pos.set(blockX, blockY + dy, blockZ);
            VoxelShape shape = shape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            // How high it looks (soul sand, mud and honey look taller than they stand, fences and
            // walls shorter).
            VoxelShape looks = level.getBlockState(pos).getShape(level, pos);
            double top = pos.getY() + (looks.isEmpty() ? shape : looks).max(Direction.Axis.Y);
            pos.move(Direction.UP);
            boolean room = shape(level, pos).isEmpty();
            if (room && Math.abs(top - y) < nearest) {
                floor = top;
                nearest = Math.abs(top - y);
            }
        }
        return floor;
    }

    // A block's solid shape, but a snow layer's as it looks (one layer doesn't stop anything).
    private static VoxelShape shape(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof SnowLayerBlock ? state.getShape(level, pos) : state.getCollisionShape(level, pos);
    }
}
