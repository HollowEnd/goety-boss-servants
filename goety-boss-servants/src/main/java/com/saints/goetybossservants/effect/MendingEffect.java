package com.saints.goetybossservants.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The Lapis Monstrosity's Mending buff: every two seconds, every damaged item carried gets 2 durability back per level. */
public class MendingEffect extends MobEffect {
    private static final int INTERVAL = 40;
    private static final int REPAIR = 2;

    public MendingEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % INTERVAL == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        int amount = REPAIR * (amplifier + 1);
        if (entity instanceof Player player) {
            player.getInventory().items.forEach(stack -> repair(stack, amount));
            player.getInventory().armor.forEach(stack -> repair(stack, amount));
            player.getInventory().offhand.forEach(stack -> repair(stack, amount));
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                repair(entity.getItemBySlot(slot), amount);
            }
        }
    }

    private static void repair(ItemStack stack, int amount) {
        if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
        }
    }
}
