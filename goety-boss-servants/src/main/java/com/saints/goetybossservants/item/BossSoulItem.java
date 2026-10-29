package com.saints.goetybossservants.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Dropped when a boss servant dies; it's the activation item of that servant's revive ritual at a
 * Dark Altar. Fireproof so a servant dying in lava keeps it.
 */
public class BossSoulItem extends Item {
    public BossSoulItem() {
        super(new Item.Properties().stacksTo(16).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.goetybossservants.boss_soul.info").withStyle(ChatFormatting.GRAY));
    }
}
