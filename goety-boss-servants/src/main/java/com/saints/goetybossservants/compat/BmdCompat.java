package com.saints.goetybossservants.compat;

import com.saints.goetybossservants.entity.bmd.Bmd;
import com.saints.goetybossservants.entity.lich.NightLichServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.voidblossom.VoidBlossomServant;
import com.saints.goetybossservants.event.ServantEvents;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.barribob.boss.config.LichConfig;
import net.barribob.boss.config.VoidBlossomConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;

/**
 * The Void Blossom and Night Lich servants (Bosses of Mass Destruction). Only touched when BMD is
 * installed; see ModCompat.
 */
public final class BmdCompat {
    private static boolean attributesRegistered;

    private BmdCompat() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        // Same size as BMD's void_blossom, but not fire-immune: the servant can burn.
        ModEntities.VOID_BLOSSOM = ModEntities.register("void_blossom_servant",
                EntityType.Builder.<VoidBlossomServant>of(VoidBlossomServant::new, MobCategory.MISC).sized(8.0f, 10.0f).clientTrackingRange(3));
        // Same size as BMD's lich.
        ModEntities.NIGHT_LICH = ModEntities.register("lich_servant",
                EntityType.Builder.<NightLichServant>of(NightLichServant::new, MobCategory.MISC).sized(1.8f, 3.0f).updateInterval(1));
        ModItems.VOID_BLOSSOM_SOUL = ModItems.registerSoul("void_blossom_soul");
        ModItems.NIGHT_LICH_SOUL = ModItems.registerSoul("night_lich_soul");
        // BMD's config (and so these attributes) is only ready once BMD itself has initialized.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> registerAttributes());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(BmdCompat::allowDamage);
        ServantEvents.SPAWN_FILTERS.add(entity -> !(entity instanceof ExperienceOrb orb && NightLichServant.blocksExperience(orb)));
    }

    /** From BMD's own config; follow range 32 like the other servants (boss: 64). Also called by the client. */
    @SuppressWarnings("unchecked")
    public static void registerAttributes() {
        if (attributesRegistered) {
            return;
        }
        attributesRegistered = true;
        VoidBlossomConfig voidBlossom = Bmd.voidBlossomConfig();
        FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) ModEntities.VOID_BLOSSOM, Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, voidBlossom.getHealth())
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, voidBlossom.getAttack())
                .add(Attributes.KNOCKBACK_RESISTANCE, 10.0)
                .add(Attributes.ARMOR, voidBlossom.getArmor()));
        LichConfig lich = Bmd.lichConfig();
        FabricDefaultAttributeRegistry.register((EntityType<? extends LivingEntity>) ModEntities.NIGHT_LICH, Mob.createMobAttributes()
                .add(Attributes.FLYING_SPEED, 5.0)
                .add(Attributes.MAX_HEALTH, lich.getHealth())
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, lich.getMissile().getDamage()));
    }

    /** A dying Night Lich servant's BMD code kills every phantom within 100 blocks; only its own minions may go. */
    private static boolean allowDamage(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        NightLichServant dying = NightLichServant.dying;
        return dying == null || victim.level().isClientSide || !(victim instanceof Phantom)
                || !source.is(DamageTypes.GENERIC_KILL) || ServantMinions.isMinionOf(victim, dying);
    }
}
