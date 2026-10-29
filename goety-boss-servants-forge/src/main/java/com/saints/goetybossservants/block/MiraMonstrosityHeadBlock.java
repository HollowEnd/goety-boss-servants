package com.saints.goetybossservants.block;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.item.MiraMonstrosityHeadItem;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

/**
 * Dropped when a Mira Monstrosity servant dies. Placing it on top of the revive shape brings the
 * servant back for its original owner, like Goety's Redstone Monstrosity Head, but with orange
 * terracotta in place of redstone blocks:
 * <pre>
 *     ~~~^~~~      ^ = this head
 *     D#####D      # = orange terracotta
 *     ###H###      H = Reinforced Redstone Block
 *     ~#####~      D = Diamond Mold Block
 *     ~##D##~      ~ = air
 * </pre>
 */
public class MiraMonstrosityHeadBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    @Nullable
    private BlockPattern revivePattern;

    public MiraMonstrosityHeadBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0f).instrument(NoteBlockInstrument.CUSTOM_HEAD).pushReaction(PushReaction.DESTROY).noOcclusion().noLootTable());
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
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
        builder.add(ROTATION);
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new MiraMonstrosityHeadBlockEntity(pos, state);
    }

    private static ItemStack toItem(@Nullable BlockEntity blockEntity) {
        ItemStack stack = new ItemStack(ModRegistry.MIRA_MONSTROSITY_HEAD_ITEM.get());
        if (blockEntity instanceof MiraMonstrosityHeadBlockEntity head) {
            if (head.getOwnerId() != null) {
                stack.getOrCreateTag().putUUID("owner", head.getOwnerId());
            }
            if (head.getOwnerName() != null) {
                stack.getOrCreateTag().putString("owner_name", head.getOwnerName());
            }
            if (head.getCustomName() != null && !head.getCustomName().isEmpty()) {
                MiraMonstrosityHeadItem.setCustomName(stack, head.getCustomName());
            }
            MiraMonstrosityHeadItem.setVariant(stack, head.getVariant());
        }
        return stack;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return player.isCrouching() ? toItem(level.getBlockEntity(pos)) : new ItemStack(this);
    }

    // The head keeps its owner, name and variant when broken, so it's dropped by hand (no loot table).
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
        if (level.getBlockEntity(pos) instanceof MiraMonstrosityHeadBlockEntity head) {
            head.setOwnerId(MiraMonstrosityHeadItem.getOwnerID(stack));
            if (stack.getTag() != null && stack.getTag().contains("owner_name")) {
                head.setOwnerName(stack.getTag().getString("owner_name"));
            }
            head.setCustomName(MiraMonstrosityHeadItem.getCustomName(stack));
            head.setVariant(MiraMonstrosityHeadItem.getVariant(stack));
        }
        if (!level.isClientSide) {
            this.trySpawnServant(placer, level, pos, stack);
        }
    }

    private BlockPattern getOrCreateRevivePattern() {
        if (this.revivePattern == null) {
            this.revivePattern = BlockPatternBuilder.start()
                    .aisle("~~~^~~~", "D#####D", "###H###", "~#####~", "~##D##~")
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(this)))
                    .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.ORANGE_TERRACOTTA)))
                    .where('H', BlockInWorld.hasState(BlockStatePredicate.forBlock(ModBlocks.REINFORCED_REDSTONE_BLOCK.get())))
                    .where('D', BlockInWorld.hasState(BlockStatePredicate.forBlock(ModBlocks.DIAMOND_MOLD_BLOCK.get())))
                    .where('~', block -> block.getState().isAir())
                    .build();
        }
        return this.revivePattern;
    }

    private void trySpawnServant(@Nullable LivingEntity placer, Level level, BlockPos pos, ItemStack stack) {
        BlockPattern pattern = this.getOrCreateRevivePattern();
        BlockPattern.BlockPatternMatch match = pattern.find(level, pos);
        if (match == null) {
            return;
        }
        MiraMonstrosityServant monstrosity = ModRegistry.MIRA_MONSTROSITY.get().create(level);
        if (monstrosity == null) {
            return;
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
        if (MiraMonstrosityHeadItem.getOwnerID(stack) != null) {
            monstrosity.setOwnerId(MiraMonstrosityHeadItem.getOwnerID(stack));
        } else if (placer != null) {
            monstrosity.servant().setTrueOwner(placer);
        }
        monstrosity.setVariant(MiraMonstrosityHeadItem.getVariant(stack));
        String name = MiraMonstrosityHeadItem.getCustomName(stack);
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
    }
}
