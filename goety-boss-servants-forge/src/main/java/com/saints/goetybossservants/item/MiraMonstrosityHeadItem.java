package com.saints.goetybossservants.item;

import com.saints.goetybossservants.entity.MiraVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Mirrors Goety's RedstoneMonstrosityHeadItem: remembers the owner and name, plus the variant. */
public class MiraMonstrosityHeadItem extends BlockItem {
    public MiraMonstrosityHeadItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getOwnerID(stack) != null;
    }

    public static void setOwner(ItemStack stack, @Nullable UUID ownerId, @Nullable LivingEntity owner) {
        if (ownerId != null) {
            stack.getOrCreateTag().putUUID("owner", ownerId);
        }
        if (owner != null) {
            stack.getOrCreateTag().putString("owner_name", owner.getDisplayName().getString());
        }
    }

    @Nullable
    public static UUID getOwnerID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    public static void setCustomName(ItemStack stack, String name) {
        stack.getOrCreateTag().putString("mod_custom_name", name);
    }

    @Nullable
    public static String getCustomName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("mod_custom_name") ? tag.getString("mod_custom_name") : null;
    }

    public static void setVariant(ItemStack stack, MiraVariant variant) {
        stack.getOrCreateTag().putString("variant", variant.getId());
    }

    public static MiraVariant getVariant(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? MiraVariant.byId(tag.getString("variant")) : MiraVariant.MIRA;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (getVariant(stack) != MiraVariant.MIRA) {
                tooltip.add(getVariant(stack).getTypeName().copy().withStyle(ChatFormatting.GRAY));
            }
            // Same tooltip lines as Goety's head item.
            if (tag.contains("owner_name")) {
                tooltip.add(Component.translatable("tooltip.goety.arcaPlayer").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(tag.getString("owner_name")).withStyle(ChatFormatting.GRAY)));
            }
            if (tag.contains("mod_custom_name")) {
                tooltip.add(Component.translatable("tooltip.goety.customName").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(tag.getString("mod_custom_name")).withStyle(ChatFormatting.GRAY)));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.saints.goetybossservants.client.MiraHeadItemRenderer.getInstance();
            }
        });
    }
}
