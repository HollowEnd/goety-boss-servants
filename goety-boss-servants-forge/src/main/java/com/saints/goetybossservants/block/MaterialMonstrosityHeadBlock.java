package com.saints.goetybossservants.block;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dropped when a material monstrosity dies (one block for all seven; the material is kept in its
 * block entity). Placing it on top of the revive shape brings the monstrosity back for its original
 * owner, like Goety's Redstone Monstrosity Head, but with the material's block in place of redstone
 * blocks:
 * <pre>
 *     ~~~^~~~      ^ = this head
 *     D#####D      # = the material's block
 *     ###H###      H = Reinforced Redstone Block
 *     ~#####~      D = Diamond Mold Block
 *     ~##D##~      ~ = air
 * </pre>
 * Like Goety's head it is two blocks tall (an invisible upper half above it), as its model is.
 */
public class MaterialMonstrosityHeadBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    @Nullable
    private final Map<MonstrosityMaterial, BlockPattern> revivePatterns = new EnumMap<>(MonstrosityMaterial.class);

    public MaterialMonstrosityHeadBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).instrument(NoteBlockInstrument.CUSTOM_HEAD).pushReaction(PushReaction.DESTROY).noLootTable());
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight() || !context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return this.defaultBlockState().setValue(ROTATION, Mth.floor(context.getRotation() * 16.0f / 360.0f + 0.5) & 15);
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, HALF);
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull PathComputationType type) {
        return false;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.canSurvive(state, level, pos) : level.getBlockState(pos.below()).is(this);
    }

    // The two halves go together, like Goety's head (and a door).
    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighbour, @NotNull LevelAccessor level,
                                           @NotNull BlockPos pos, @NotNull BlockPos neighbourPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighbour.is(this) && neighbour.getValue(HALF) != half ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new MaterialMonstrosityHeadBlockEntity(pos, state) : null;
    }

    // The lower half holds the head's data.
    private static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private static ItemStack toItem(@Nullable BlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModRegistry.MATERIAL_MONSTROSITY_HEAD_ITEM.get());
        if (blockEntity instanceof MaterialMonstrosityHeadBlockEntity head) {
            if (head.getOwnerId() != null) {
                stack.getOrCreateTag().putUUID("owner", head.getOwnerId());
            }
            if (head.getOwnerName() != null) {
                stack.getOrCreateTag().putString("owner_name", head.getOwnerName());
            }
            if (head.getCustomName() != null && !head.getCustomName().isEmpty()) {
                MaterialMonstrosityHeadItem.setCustomName(stack, head.getCustomName());
            }
            MaterialMonstrosityHeadItem.setMaterial(stack, head.getMaterial());
        }
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        // Always this head's material; crouching also copies its owner and name.
        BlockEntity blockEntity = level.getBlockEntity(lower(state, pos));
        if (player.isCrouching()) {
            return toItem(blockEntity);
        }
        return blockEntity instanceof MaterialMonstrosityHeadBlockEntity head ? MaterialMonstrosityHeadItem.create(head.getMaterial()) : new ItemStack(this);
    }

    // Breaking the upper half breaks the head (the lower half), dropping it.
    @Override
    public void playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState lower = level.getBlockState(below);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (!player.getAbilities().instabuild) {
                    popResource(level, below, toItem(level.getBlockEntity(below)));
                }
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, below, Block.getId(lower));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    // The head keeps its owner, name and material when broken, so it's dropped by hand (no loot table).
    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity blockEntity, @NotNull ItemStack tool) {
        if (!player.getAbilities().instabuild) {
            popResource(level, pos, toItem(blockEntity));
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MaterialMonstrosityHeadBlockEntity head) {
            head.setOwnerId(MaterialMonstrosityHeadItem.getOwnerID(stack));
            if (stack.getTag() != null && stack.getTag().contains("owner_name")) {
                head.setOwnerName(stack.getTag().getString("owner_name"));
            }
            head.setCustomName(MaterialMonstrosityHeadItem.getCustomName(stack));
            head.setMaterial(MaterialMonstrosityHeadItem.getMaterial(stack));
        }
        if (!level.isClientSide && !this.trySpawnServant(placer, level, pos, stack)) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    private BlockPattern revivePattern(MonstrosityMaterial material) {
        return this.revivePatterns.computeIfAbsent(material, key -> BlockPatternBuilder.start()
                .aisle("~~~^~~~", "D#####D", "###H###", "~#####~", "~##D##~")
                .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(this)))
                .where('#', BlockInWorld.hasState(key::isFill))
                .where('H', BlockInWorld.hasState(BlockStatePredicate.forBlock(ModBlocks.REINFORCED_REDSTONE_BLOCK.get())))
                .where('D', BlockInWorld.hasState(BlockStatePredicate.forBlock(ModBlocks.DIAMOND_MOLD_BLOCK.get())))
                .where('~', block -> block.getState().isAir())
                .build());
    }

    /** Revives the monstrosity if the head completes the shape; whether it did. */
    private boolean trySpawnServant(@Nullable LivingEntity placer, Level level, BlockPos pos, ItemStack stack) {
        BlockPattern pattern = this.revivePattern(MaterialMonstrosityHeadItem.getMaterial(stack));
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) {
            return false;
        }
        MaterialMonstrosity monstrosity = MaterialMonstrosity.create(level, MaterialMonstrosityHeadItem.getMaterial(stack));
        if (monstrosity == null) {
            return false;
        }
        for (int i = 0; i < pattern.getWidth(); ++i) {
            for (int j = 0; j < pattern.getHeight(); ++j) {
                BlockInWorld block = match.getBlock(i, j, 0);
                level.setBlock(block.getPos(), Blocks.AIR.defaultBlockState(), 2);
                level.levelEvent(2001, block.getPos(), Block.getId(block.getState()));
            }
        }
        BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
        // The head remembers its original master; only a head with no owner takes the placer.
        if (MaterialMonstrosityHeadItem.getOwnerID(stack) != null) {
            monstrosity.setOwnerId(MaterialMonstrosityHeadItem.getOwnerID(stack));
        } else if (placer != null) {
            monstrosity.setTrueOwner(placer);
        }
        String name = MaterialMonstrosityHeadItem.getCustomName(stack);
        if (name != null) {
            monstrosity.setCustomName(Component.literal(name));
        }
        monstrosity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, 0.0f, 0.0f);
        if (level instanceof ServerLevel serverLevel) {
            monstrosity.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        }
        monstrosity.setWandering(false);
        monstrosity.setStaying(false);
        level.addFreshEntity(monstrosity);
        for (int i = 0; i < pattern.getWidth(); ++i) {
            for (int j = 0; j < pattern.getHeight(); ++j) {
                level.blockUpdated(match.getBlock(i, j, 0).getPos(), Blocks.AIR);
            }
        }
        return true;
    }
}
