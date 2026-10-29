package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saints.goetybossservants.block.MiraMonstrosityHeadBlock;
import com.saints.goetybossservants.block.MiraMonstrosityHeadBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class MiraHeadBlockRenderer implements BlockEntityRenderer<MiraMonstrosityHeadBlockEntity> {
    private final MiraHeadModel model;

    public MiraHeadBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new MiraHeadModel(context.getModelSet());
    }

    @Override
    public void render(MiraMonstrosityHeadBlockEntity head, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, int overlay) {
        float yRot = head.getBlockState().getValue(MiraMonstrosityHeadBlock.ROTATION) * 22.5f;
        this.model.render(poseStack, buffers, light, yRot, head.getVariant());
    }
}
