package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.lich.NightLichServant;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.FakePlayer;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Night Lich part of the dev self-test (see SelfTest). Steps are ticks after START. */
class NightLichChecks {
    static final int START = 3200;
    static final int LENGTH = 1900;
    private static final BlockPos CENTER = new BlockPos(0, 120, -160);

    private static NightLichServant lich;
    private static ArmorStand owner;
    private static Mob enemy;
    private static Vec3 ownerSpot;
    private static float ownerHealth;
    private static final List<String> minionTargetedFriend = new ArrayList<>();
    private static int maxMinionsSeen;
    private static Phantom wild;

    private static NightLichServant summon(ServerLevel level, FakePlayer player, BlockPos at) {
        NightLichServant summoned = (NightLichServant) ModEntities.NIGHT_LICH.create(level);
        MobUtil.summonTame(summoned, player);
        summoned.moveTo(at.getX() + 0.5, at.getY() + 4, at.getZ() + 0.5, 0.0f, 0.0f);
        summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(summoned);
        return summoned;
    }

    static void step(ServerLevel level, FakePlayer player, int t) {
        if (lich != null && lich.isAlive()) {
            List<Mob> minions = level.getEntitiesOfClass(Mob.class, lich.getBoundingBox().inflate(128.0), mob -> ServantMinions.isMinionOf(mob, lich));
            maxMinionsSeen = Math.max(maxMinionsSeen, minions.size());
            for (Mob minion : minions) {
                if (minion.getTarget() == owner) {
                    minionTargetedFriend.add("t=" + t);
                }
            }
        }
        switch (t) {
            case 0 -> {
                for (int dx = -3; dx <= 5; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        ChunkPos chunk = new ChunkPos(CENTER.offset(dx * 16, 0, dz * 16));
                        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
                    }
                }
                BlockPos.betweenClosed(CENTER.offset(-30, -1, -30), CENTER.offset(70, -1, 30))
                        .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", "summon_lich_servant")).isPresent(), "night lich summon ritual loaded");
                check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", "revive_lich_servant")).isPresent(), "night lich revive ritual loaded");
                level.setDayTime(1000);
                lich = summon(level, player, CENTER);
                check(lich instanceof IServant && player.getUUID().equals(lich.getOwnerId()), "night lich tamed to the caster");
                check(lich.isFollowing(), "night lich follows its owner when summoned");
            }
            case 5 -> {
                owner = EntityType.ARMOR_STAND.create(level);
                owner.moveTo(CENTER.getX() + 20.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                level.addFreshEntity(owner);
                lich.setTrueOwner(owner);
            }
            case 200 -> {
                check(level.getDayTime() % 24000 < 12000, "no eternal night (day time " + level.getDayTime() % 24000 + ")");
                check(lich.distanceTo(owner) < 12.0f, "flies after its owner (" + String.format("%.1f", lich.distanceTo(owner)) + " blocks away)");
                owner.moveTo(CENTER.getX() + 65.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
            }
            case 260 -> {
                check(lich.distanceTo(owner) < 12.0f, "teleports to an owner left far behind (" + String.format("%.1f", lich.distanceTo(owner)) + " blocks away)");
                // Fight: a sturdy husk with the owner right beside it, inside the comets' blast.
                owner.moveTo(CENTER.getX() + 3.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                enemy = EntityType.HUSK.create(level);
                enemy.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                enemy.setNoAi(true);
                enemy.setPersistenceRequired();
                enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
                enemy.setHealth(enemy.getMaxHealth());
                level.addFreshEntity(enemy);
            }
            case 262 -> {
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
                lich.setTarget(enemy);
            }
            case 900 -> {
                check(enemy.getHealth() < enemy.getMaxHealth(), "its comets and missiles hurt the enemy (husk " + enemy.getHealth() + ")");
                check(owner.getHealth() == ownerHealth && owner.position().distanceTo(ownerSpot) < 0.3,
                        "owner beside the target takes no damage or knockback (moved " + String.format("%.2f", owner.position().distanceTo(ownerSpot)) + ")");
                check(BlockPos.betweenClosedStream(CENTER.offset(-8, -1, -8), CENTER.offset(8, -1, 8)).allMatch(pos -> level.getBlockState(pos).is(Blocks.STONE)),
                        "comet explosions break no blocks (bossesBreakBlocks off)");
                // Past 75% health: its rage (comets, volleys, nine phantoms) comes next.
                lich.hurt(level.damageSources().generic(), lich.getMaxHealth() * 0.3f);
            }
            case 1800 -> {
                check(maxMinionsSeen > 0, "rage summons phantom minions (most at once: " + maxMinionsSeen + ")");
                check(maxMinionsSeen <= ServantConfig.NightLichMaxMinions.get(), "minions capped at " + ServantConfig.NightLichMaxMinions.get());
                check(minionTargetedFriend.isEmpty(), "minions never target the owner " + minionTargetedFriend);
                check(owner.getHealth() == ownerHealth, "owner still unhurt after the rage");
                // Stored away (like Goety's Esoteric Tesseract) = removed: its minions go too.
                lich.discard();
            }
            case 1805 -> {
                check(level.getEntitiesOfClass(Phantom.class, new AABB(CENTER).inflate(128.0), ServantMinions::isMinion).isEmpty(), "minions despawn when their lich is gone");
                enemy.discard();
                lich = summon(level, player, CENTER);
                wild = EntityType.PHANTOM.create(level);
                wild.moveTo(CENTER.getX() + 5.5, CENTER.getY() + 3, CENTER.getZ() + 0.5, 0.0f, 0.0f);
                wild.setNoAi(true);
                wild.setPersistenceRequired();
                level.addFreshEntity(wild);
            }
            case 1810 -> lich.kill();
            case 1850 -> {
                check(!level.getEntitiesOfClass(ItemEntity.class, new AABB(lich.blockPosition()).inflate(16),
                        item -> item.getItem().is(ModItems.NIGHT_LICH_SOUL)).isEmpty(), "night lich drops its soul on death");
                check(level.getEntitiesOfClass(ExperienceOrb.class, new AABB(lich.blockPosition()).inflate(16)).isEmpty(), "no experience shower on death");
                check(wild.isAlive(), "an unrelated phantom nearby survives its death");
                wild.discard();
                owner.discard();
            }
            default -> {
            }
        }
    }
}
