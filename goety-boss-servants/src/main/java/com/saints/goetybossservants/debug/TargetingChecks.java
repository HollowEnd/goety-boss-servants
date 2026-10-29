package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * Goety's targeting goals on every loaded boss servant (the boss sections set targets directly),
 * owned by the real player standing among them, as in the game: in creative, then in survival after
 * being stored in a Tesseract and let out again (saved and loaded).
 * Each servant should pick a nearby monster by itself, and switch to whatever its owner hits.
 */
class TargetingChecks {
    static final int START = 8600;
    static final int ROUND = 400;
    static final int LENGTH = 2 * ROUND;
    private static final BlockPos CENTER = new BlockPos(160, 120, 160);

    private record Case(String name, Mob servant, BlockPos spot) {
    }

    private static final List<Case> cases = new ArrayList<>();
    private static final List<Mob> prey = new ArrayList<>();
    private static LivingEntity owner;
    private static Mob cow;
    private static Mob secondCow;

    static void step(ServerLevel level, Player fakePlayer, int t) {
        int round = t / ROUND;
        String mode = round == 0 ? "creative" : "survival, after a Tesseract";
        switch (t % ROUND) {
            case 0 -> {
                if (round == 0) {
                    for (int dx = -5; dx <= 5; ++dx) {
                        for (int dz = -4; dz <= 4; ++dz) {
                            ChunkPos chunk = new ChunkPos(CENTER.offset(dx * 16, 0, dz * 16));
                            level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
                        }
                    }
                    BlockPos.betweenClosed(CENTER.offset(-72, -1, -56), CENTER.offset(72, -1, 56))
                            .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                }
                List<ServerPlayer> players = level.players();
                if (!players.isEmpty()) {
                    ServerPlayer real = players.get(0);
                    real.setGameMode(round == 0 ? GameType.CREATIVE : GameType.SURVIVAL);
                    real.teleportTo(level, CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                    real.setInvulnerable(true);
                    owner = real;
                } else {
                    owner = EntityType.ARMOR_STAND.create(level);
                    owner.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                    level.addFreshEntity(owner);
                }
                EntityType<?>[] types = {ModEntities.CAPTAIN_CORNELIA, ModEntities.VOID_BLOSSOM, ModEntities.NIGHT_LICH,
                        ModEntities.OLD_CHAMPION, ModEntities.RETURNING_KNIGHT, ModEntities.NETHERMAN};
                int x = -25;
                for (EntityType<?> type : types) {
                    if (type == null) {
                        continue;
                    }
                    BlockPos spot = CENTER.offset(x, 0, 8);
                    x += 10;
                    Mob servant = (Mob) type.create(level);
                    MobUtil.summonTame(servant, fakePlayer);
                    servant.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0f, 0.0f);
                    servant.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.MOB_SUMMONED, null, null);
                    level.addFreshEntity(servant);
                    ((BossServant) servant).setTrueOwner(owner);
                    // Stays put, so each fights next to its own zombie. Not the Netherman: every one of its
                    // form attacks teleports it around its target, which a staying servant can't follow.
                    ((BossServant) servant).setStaying(type != ModEntities.NETHERMAN);
                    if (round == 1) {
                        // Stored in a Tesseract and let out again first: Goety used to make every Monster-based
                        // servant hostile on load, and hostile servants only go after players.
                        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
                        servant.save(saved);
                        servant.discard();
                        servant = (Mob) EntityType.loadEntityRecursive(saved, level, entity -> entity);
                        level.addFreshEntity(servant);
                    }
                    cases.add(new Case(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath(), servant, spot));
                }
            }
            case 60 -> {
                // A monster next to each: it should pick one as a target by itself.
                for (Case c : cases) {
                    prey.add(prey(level, EntityType.ZOMBIE, c.spot().offset(3, 0, 3)));
                }
            }
            case 200 -> {
                for (Case c : cases) {
                    check(c.servant().getTarget() != null && c.servant().getTarget().getType() == EntityType.ZOMBIE,
                            mode + ": " + c.name() + " targets a nearby monster by itself (target " + name(c.servant()) + ")");
                }
                prey.forEach(Mob::discard);
                prey.clear();
                // Something none of them would pick by itself, which their owner then hits.
                cow = prey(level, EntityType.COW, CENTER.offset(0, 0, 3));
            }
            case 205 -> {
                owner.setLastHurtMob(cow);
            }
            case 300 -> {
                for (Case c : cases) {
                    check(c.servant().getTarget() == cow, mode + ": " + c.name() + " attacks what its owner hits (target " + name(c.servant()) + ")");
                }
            }
            case 302 -> {
                // Already fighting the first cow; the owner now hits a second one.
                secondCow = prey(level, EntityType.COW, CENTER.offset(4, 0, 3));
                owner.setLastHurtMob(secondCow);
            }
            case 380 -> {
                for (Case c : cases) {
                    check(c.servant().getTarget() == secondCow, mode + ": " + c.name() + " switches to the next thing its owner hits (target " + name(c.servant()) + ")");
                }
            }
            case 390 -> {
                cow.discard();
                secondCow.discard();
                cases.forEach(c -> c.servant().discard());
                cases.clear();
                if (!(owner instanceof Player)) {
                    owner.discard();
                }
            }
            default -> {
            }
        }
    }

    private static Mob prey(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos) {
        Mob mob = type.create(level);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
        mob.setHealth(mob.getMaxHealth());
        level.addFreshEntity(mob);
        return mob;
    }

    private static String name(Mob servant) {
        return servant.getTarget() == null ? "none" : BuiltInRegistries.ENTITY_TYPE.getKey(servant.getTarget().getType()).getPath();
    }
}
