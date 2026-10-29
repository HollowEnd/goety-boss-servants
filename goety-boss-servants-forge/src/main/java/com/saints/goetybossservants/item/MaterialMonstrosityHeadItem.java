package com.saints.goetybossservants.item;

import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Mirrors Goety's RedstoneMonstrosityHeadItem (remembers the owner and name), for all seven
 * material monstrosities: one item, with the material in its tag and in its name.
 */
public class MaterialMonstrosityHeadItem extends BlockItem {
    public MaterialMonstrosityHeadItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack create(MonstrosityMaterial material) {
        ItemStack stack = new ItemStack(ModRegistry.MATERIAL_MONSTROSITY_HEAD_ITEM.get());
        setMaterial(stack, material);
        return stack;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable("block.goetybossservants." + getMaterial(stack).entityName() + "_head");
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
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

    public static void setMaterial(ItemStack stack, MonstrosityMaterial material) {
        stack.getOrCreateTag().putString("material", material.getId());
    }

    public static MonstrosityMaterial getMaterial(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? MonstrosityMaterial.byId(tag.getString("material")) : MonstrosityMaterial.COPPER;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
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
                return com.saints.goetybossservants.client.MaterialHeadRenderer.ItemRenderer.getInstance();
            }
        });
    }
}
