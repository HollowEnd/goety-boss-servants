package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.knightquest.NethermanServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.mixin.NethermanAccessor;
import com.saints.goetybossservants.registry.ModRegistry;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import dev.xylonity.knightquest.common.entity.boss.NethermanProjectileChargeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Netherman part of the dev self-test (see SelfTest). Steps are ticks after START. */
class NethermanChecks {
    static final int START = 7800;
    static final int LENGTH = 2100;
    private static final BlockPos CENTER = new BlockPos(-160, 120, 0);

    private static NethermanServant netherman;
    private static ArmorStand owner;
    private static Mob enemy;
    private static Vec3 ownerSpot;
    private static float ownerHealth;
    private static int maxClones;
    private static boolean clonesAllMinions = true;
    private static boolean orbSeen;
    private static boolean orbsOwned = true;
    private static final List<String> targetedOwner = new ArrayList<>();

    private static NethermanServant summon(ServerLevel level, Player player) {
        NethermanServant summoned = ModRegistry.NETHERMAN.get().create(level);
        MobUtil.summonTame(summoned, player);
        summoned.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
        summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(CENTER), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(summoned);
        return summoned;
    }

    private static boolean ownerUnharmed() {
        return owner.getHealth() == ownerHealth && owner.position().distanceTo(ownerSpot) < 0.3 && owner.getTicksFrozen() == 0;
    }

    private static String ownerState() {
        return "(health " + owner.getHealth() + "/" + ownerHealth + ", moved " + String.format("%.2f", owner.position().distanceTo(ownerSpot))
                + ", frozen " + owner.getTicksFrozen() + ")";
    }

    private static String goals() {
        return netherman.goalSelector.getRunningGoals()
                .map(goal -> goal.getGoal().getClass().getSimpleName()).toList().toString();
    }

    static void step(ServerLevel level, Player player, int t) {
        if (netherman != null && netherman.isAlive() && t > 50 && t < 1900) {
            AABB area = netherman.getBoundingBox().inflate(64.0);
            List<NethermanCloneEntity> clones = level.getEntitiesOfClass(NethermanCloneEntity.class, area);
            maxClones = Math.max(maxClones, clones.size());
            for (NethermanCloneEntity clone : clones) {
                clonesAllMinions &= ServantMinions.isMinionOf(clone, netherman);
                if (clone.getTarget() == owner) {
                    targetedOwner.add("t=" + t);
                }
            }
            for (NethermanProjectileChargeEntity orb : level.getEntitiesOfClass(NethermanProjectileChargeEntity.class, area)) {
                orbSeen = true;
                orbsOwned &= orb.getOwner() == netherman;
            }
        }
        switch (t) {
            case 0 -> {
                for (int dx = -3; dx <= 3; ++dx) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        ChunkPos chunk = new ChunkPos(CENTER.offset(dx * 16, 0, dz * 16));
                        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
                    }
                }
                BlockPos.betweenClosed(CENTER.offset(-48, -1, -48), CENTER.offset(48, -1, 48))
                        .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                for (String id : new String[]{"summon_netherman", "revive_netherman"}) {
                    check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", id)).isPresent(), id + " ritual loaded");
                }
                netherman = summon(level, player);
                check(player.getUUID().equals(netherman.getOwnerId()), "netherman tamed to the caster");
                check(netherman.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 32.0, "netherman follow range 32");
                check(!((NethermanAccessor) netherman).goetybossservants$getBossInfo().isVisible(), "netherman has no boss bar");
                check(!netherman.getIsSummoning() && !netherman.getInvulnerability() && !netherman.getNoMovement(), "netherman skips the summoning cinematic");
                check(!netherman.shouldPlayBossMusic(player), "netherman plays no boss music");
            }
            case 5 -> {
                // Fight: a sturdy husk with the owner right beside it.
                enemy = EntityType.HUSK.create(level);
                enemy.moveTo(CENTER.getX() + 4.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                enemy.setNoAi(true);
                enemy.setPersistenceRequired();
                enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
                enemy.setHealth(enemy.getMaxHealth());
                level.addFreshEntity(enemy);
                owner = EntityType.ARMOR_STAND.create(level);
                owner.moveTo(CENTER.getX() + 4.5, CENTER.getY(), CENTER.getZ() + 2.5, 0.0f, 0.0f);
                level.addFreshEntity(owner);
                netherman.setTrueOwner(owner);
            }
            case 50 -> {
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
                netherman.setTarget(enemy);
            }
            case 400 -> {
                check(enemy.getHealth() < enemy.getMaxHealth(), "netherman hurts the enemy (husk " + enemy.getHealth() + ")");
                check(BlockPos.betweenClosedStream(CENTER.offset(-48, -1, -48), CENTER.offset(48, -1, 48)).noneMatch(pos -> level.getBlockState(pos).is(Blocks.LAVA)),
                        "its fire-form barrage leaves no lava (bossesBreakBlocks off)");
                // Into its Ice form (below 66%): clones come next.
                netherman.setHealth(netherman.getMaxHealth() * 0.6f);
                // The husk (1024 health at most) must last through all three forms: topped up at each change.
                enemy.setHealth(enemy.getMaxHealth());
            }
            case 1000 -> {
                check(netherman.getPhase() == 2, "netherman changes to its second form below 66% health (phase " + netherman.getPhase() + ")");
                check(maxClones > 0 && clonesAllMinions, "its clones are its minions (most at once: " + maxClones + ", counter " + netherman.getCounterSwitchPhase2() + ", goals " + goals() + ")");
                check(maxClones <= ServantConfig.NethermanMaxClones.get(), "clones capped at " + ServantConfig.NethermanMaxClones.get());
                // Into its Magic form (below 33%): explosive orbs come next.
                netherman.setHealth(netherman.getMaxHealth() * 0.3f);
                enemy.setHealth(enemy.getMaxHealth());
            }
            case 1890 -> {
                check(netherman.getPhase() == 3, "netherman changes to its third form below 33% health (phase " + netherman.getPhase() + ")");
                check(orbSeen && orbsOwned, "its explosive orbs are its own (seen " + orbSeen + ", counter " + netherman.getCounterSwitchPhase3() + ", goals " + goals() + ", husk " + enemy.getHealth() + (enemy.isAlive() ? " alive" : " dead") + " at " + String.format("%.1f", enemy.distanceTo(netherman)) + ")");
                check(targetedOwner.isEmpty(), "clones never target the owner " + targetedOwner);
                check(ownerUnharmed(), "owner beside its target untouched by curses, clones and orbs " + ownerState());
                // Stored away (like Goety's Esoteric Tesseract) = removed: its clones go too.
                netherman.discard();
            }
            case 1895 -> {
                check(level.getEntitiesOfClass(NethermanCloneEntity.class, new AABB(CENTER).inflate(64.0), NethermanCloneEntity::isAlive).isEmpty(), "clones go when it's gone");
                enemy.discard();
                netherman = summon(level, player);
            }
            case 1900 -> {
                // Stored in a Tesseract and let out again: Forge's Netherman refills its health from the
                // config whenever it's added to a world; a hurt servant must come back as hurt.
                // 320 of 450 keeps it in its first form (below 66% would start an invulnerable form change).
                netherman.setHealth(320.0f);
                net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
                netherman.save(saved);
                netherman.discard();
                netherman = (NethermanServant) EntityType.loadEntityRecursive(saved, level, entity -> entity);
                level.addFreshEntity(netherman);
                check(netherman.getHealth() == 320.0f, "a hurt netherman comes back from a Tesseract still hurt (" + netherman.getHealth() + "/" + netherman.getMaxHealth() + ")");
            }
            // It shrugs off all damage for its first 40 ticks (the boss's spawn grace).
            case 1945 -> netherman.kill();
            case 2045 -> {
                AABB area = new AABB(CENTER).inflate(16);
                check(netherman.isRemoved(), "netherman's death animation finishes");
                check(!level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(ModRegistry.NETHERMAN_SOUL.get())).isEmpty(), "netherman drops its soul on death");
                check(level.getEntitiesOfClass(ItemEntity.class, area, item -> !item.getItem().is(ModRegistry.NETHERMAN_SOUL.get())).isEmpty(), "netherman drops no boss loot");
                check(level.getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(), "netherman drops no experience");
                owner.discard();
            }
            default -> {
            }
        }
    }
}
