package com.saints.goetybossservants.compat;

import com.cerbon.bosses_of_mass_destruction.config.mob.LichConfig;
import com.cerbon.bosses_of_mass_destruction.config.mob.VoidBlossomConfig;
import com.cerbon.bosses_of_mass_destruction.entity.BMDEntities;
import com.saints.goetybossservants.entity.lich.NightLichServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The Void Blossom and Night Lich servants (Bosses of Mass Destruction). Only touched when BMD is
 * installed; see ModCompat.
 */
public final class BmdCompat {
    private BmdCompat() {
    }

    public static void registerEvents() {
        MinecraftForge.EVENT_BUS.register(BmdCompat.class);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Same as BMD's void_blossom attributes, from BMD's own config.
        VoidBlossomConfig voidBlossom = BMDEntities.mobConfig.voidBlossomConfig;
        event.put(ModRegistry.VOID_BLOSSOM.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, voidBlossom.health)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, voidBlossom.attack)
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ARMOR, voidBlossom.armor)
                .build());
        // BMD's lich attributes, from BMD's own config; follow range 32 like the other servants (boss: 64).
        LichConfig lich = BMDEntities.mobConfig.lichConfig;
        event.put(ModRegistry.NIGHT_LICH.get(), Mob.createMobAttributes()
                .add(Attributes.FLYING_SPEED, 5.0)
                .add(Attributes.MAX_HEALTH, lich.health)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, lich.missile.damage)
                .build());
    }

    /** A dying Night Lich servant's BMD code kills every phantom within 100 blocks; only its own minions may go. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        NightLichServant dying = NightLichServant.dying;
        if (dying != null && !victim.level().isClientSide && victim instanceof Phantom
                && event.getSource().is(DamageTypes.GENERIC_KILL) && !ServantMinions.isMinionOf(victim, dying)) {
            event.setCanceled(true);
        }
    }

    /** BMD's lich showers experience when it dies; not when it's a servant (no XP farm). */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ExperienceOrb orb) || NightLichServant.noExperience.isEmpty()) {
            return;
        }
        long now = event.getLevel().getGameTime();
        NightLichServant.noExperience.removeIf(block -> block.until() < now);
        for (NightLichServant.XpBlock block : NightLichServant.noExperience) {
            if (block.dimension() == event.getLevel().dimension() && orb.position().distanceToSqr(block.pos()) < 64.0) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
