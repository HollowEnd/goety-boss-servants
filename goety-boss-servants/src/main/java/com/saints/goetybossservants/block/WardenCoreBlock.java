package com.saints.goetybossservants.block;

import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Warden Core: every warden drops one, and it's the core of the Sculk Monstrosity's mold (see
 * MaterialMonstrosityMold). Half a block tall, like the sculk shriekers round it there. Its top is
 * the warden's chest with its ribcage over the souls of its heart, which pulse with a calm warden's
 * heartbeat (WardenCoreRenderer) while the heartbeat thuds quietly. Crafting one gives 4 echo shards.
 */
public class WardenCoreBlock extends BaseEntityBlock {
    // Which way the souls' heads point (the way the placer was looking).
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    // A calm warden's heartbeat (Warden.getHeartBeatDelay at no anger): a beat every two seconds,
    // the souls' glow fading over half a second after each.
    public static final int BEAT = 40;
    private static final int FADE = 10;

    public WardenCoreBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0f).sound(SoundType.SCULK_CATALYST).lightLevel(state -> 6));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * How brightly the souls glow partway into game tick {@code gameTime}, 0 to 1, like
     * Warden.getHeartAnimation. (The beat is found from the exact tick count: a float of an old
     * world's game time loses its fractions and then whole ticks.)
     */
    public static float heartbeat(BlockPos pos, long gameTime, float partialTick) {
        float sinceBeat = Math.floorMod(gameTime + offset(pos), BEAT) + partialTick;
        return Math.max(0.0f, 1.0f - sinceBeat / FADE);
    }

    // Cores side by side don't beat in step.
    private static int offset(BlockPos pos) {
        return Math.floorMod(pos.hashCode(), BEAT);
    }

    // The heartbeat itself, quiet beside a warden's. (Block entities tick before the client's game time
    // moves on, so this is the beat the frames after this tick show.)
    private static void clientTick(Level level, BlockPos pos, BlockState state, WardenCoreBlockEntity core) {
        if (Math.floorMod(level.getGameTime() + 1 + offset(pos), BEAT) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.6f, 1.0f, false);
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModRegistry.WARDEN_CORE_BE, WardenCoreBlock::clientTick) : null;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WardenCoreBlockEntity(pos, state);
    }

    // BaseEntityBlock draws nothing by default; the block model is the slab, the renderer only the souls' glow.
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("block.goetybossservants.warden_core.info", MonstrosityMaterial.SCULK.typeName()).withStyle(ChatFormatting.GRAY));
    }
}
