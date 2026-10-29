package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.entity.knightquest.NethermanServant;
import com.saints.goetybossservants.registry.ModRegistry;
import dev.xylonity.knightquest.common.entity.boss.NethermanCloneEntity;
import dev.xylonity.knightquest.common.entity.boss.NethermanProjectileChargeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A Netherman servant against a warden, as in the game: logs everything it does (teleports, attacks,
 * forms) rather than checking exact results, plus a few sanity checks. Steps are ticks after START.
 */
class NethermanWardenChecks {
    static final int START = NethermanChecks.START + NethermanChecks.LENGTH;
    static final int LENGTH = 1300;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockPos CENTER = new BlockPos(-160, 120, 160);

    private static NethermanServant netherman;
    private static Warden warden;
    private static Vec3 lastPos;
    private static int lastPhase;
    private static final List<String> events = new ArrayList<>();
    private static int teleports;
    private static int maxClones;
    private static boolean orbSeen;
    private static boolean wardenBurned;
    private static int diedAt = -1;
    private static double lowestY = Double.MAX_VALUE;

    static void step(ServerLevel level, Player player, int t) {
        if (warden != null && t > 50 && diedAt < 0) {
            if (!netherman.isAlive()) {
                diedAt = t;
                events.add("t=" + t + " netherman died (warden " + (int) warden.getHealth() + " hp)");
            } else {
                Vec3 pos = netherman.position();
                lowestY = Math.min(lowestY, pos.y);
                if (lastPos != null && pos.distanceTo(lastPos) > 3.0) {
                    ++teleports;
                    events.add("t=" + t + " teleport " + String.format("%.1f", pos.distanceTo(lastPos)) + " blocks");
                }
                lastPos = pos;
                if (netherman.getPhase() != lastPhase) {
                    events.add("t=" + t + " form " + lastPhase + " -> " + netherman.getPhase() + " (" + (int) netherman.getHealth() + " hp)");
                    lastPhase = netherman.getPhase();
                }
                AABB area = netherman.getBoundingBox().inflate(48.0);
                int clones = level.getEntitiesOfClass(NethermanCloneEntity.class, area).size();
                if (clones > maxClones) {
                    events.add("t=" + t + " clones out: " + clones);
                    maxClones = clones;
                }
                if (!orbSeen && !level.getEntitiesOfClass(NethermanProjectileChargeEntity.class, area).isEmpty()) {
                    orbSeen = true;
                    events.add("t=" + t + " first orb");
                }
                if (!wardenBurned && warden.isOnFire()) {
                    wardenBurned = true;
                    events.add("t=" + t + " warden set on fire (flame attack)");
                }
            }
        }
        // A warden's blow (30) every 1.5 seconds, as in a real fight.
        if (warden != null && netherman.isAlive() && t > 60 && t % 30 == 0) {
            Vec3 before = netherman.position();
            boolean hurt = netherman.hurt(level.damageSources().mobAttack(warden), 30.0f);
            events.add("t=" + t + " warden hits for 30: " + (hurt ? "hurt" : "ignored") + ", moved " + String.format("%.1f", netherman.position().distanceTo(before))
                    + " (" + (int) netherman.getHealth() + " hp, form " + netherman.getPhase() + ")");
            lastPos = netherman.position();
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
                netherman = ModRegistry.NETHERMAN.get().create(level);
                MobUtil.summonTame(netherman, player);
                netherman.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                netherman.finalizeSpawn(level, level.getCurrentDifficultyAt(CENTER), MobSpawnType.MOB_SUMMONED, null, null);
                level.addFreshEntity(netherman);
                lastPhase = netherman.getPhase();
            }
            case 50 -> {
                warden = EntityType.WARDEN.create(level);
                warden.moveTo(CENTER.getX() + 6.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                warden.setPersistenceRequired();
                level.addFreshEntity(warden);
                warden.increaseAngerAt(netherman, 150, false);
                netherman.setTarget(warden);
            }
            case 1290 -> {
                LOGGER.info("SELFTEST netherman vs warden: {} teleports, most clones {}, orbs {}, fire {}, events:\n  {}",
                        teleports, maxClones, orbSeen, wardenBurned, String.join("\n  ", events));
                SelfTest.check(teleports > 0, "against a warden the netherman teleports with its attacks (" + teleports + " times)");
                SelfTest.check(maxClones > 0, "before the warden kills it, it sends clones (most " + maxClones + ")");
                SelfTest.check(orbSeen, "before the warden kills it, it throws explosive orbs");
                SelfTest.check(lowestY >= CENTER.getY() - 3.0, "its hops never drop it through the floor (lowest " + String.format("%.1f", lowestY - CENTER.getY()) + ")");
                if (netherman.isAlive()) {
                    netherman.discard();
                }
                warden.discard();
            }
            default -> {
            }
        }
    }
}
