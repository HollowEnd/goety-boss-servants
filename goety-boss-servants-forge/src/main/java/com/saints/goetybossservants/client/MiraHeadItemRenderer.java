package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saints.goetybossservants.item.MiraMonstrosityHeadItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Renders the head item with the same model as the placed block (the item model is builtin/entity). */
public class MiraHeadItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static MiraHeadItemRenderer instance;
    private MiraHeadModel model;

    private MiraHeadItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static MiraHeadItemRenderer getInstance() {
        if (instance == null) {
            instance = new MiraHeadItemRenderer();
        }
        return instance;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        this.model = new MiraHeadModel(Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext context, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, int overlay) {
        if (this.model == null) {
            this.model = new MiraHeadModel(Minecraft.getInstance().getEntityModels());
        }
        // Face forward in the item transforms of minecraft:item/template_skull (inventory, hand, head slot).
        this.model.render(poseStack, buffers, light, 0.0f, MiraMonstrosityHeadItem.getVariant(stack));
    }
}
