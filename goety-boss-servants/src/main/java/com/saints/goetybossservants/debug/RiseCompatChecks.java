package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.RisenMobs;
import com.saints.goetybossservants.entity.monstrosity.SculkDecay;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * The mods whose mobs real revival and ticked copies were made for, when they're loaded (dev runs
 * with -PwithRiseMods): the fighters of Raxiores and The Dawn Era rise as themselves and fight,
 * their calm mobs rise as sculk zombies whose copies animate, and a Saints Dragons dragon that fell stunned to be tamed rises standing, and never
 * falls stunned again. Runs in RiseChecks once its own checks are done with the camera, further along.
 */
class RiseCompatChecks {
    private static final BlockPos BASE = new BlockPos(-150, 120, 300);
    // Each mob, and whether it should rise as itself.
    private static final Map<String, Boolean> MOBS = new LinkedHashMap<>();
    private static final Map<String, Entity> risen = new LinkedHashMap<>();
    private static final List<Mob> dying = new ArrayList<>();
    private static ServerLevel level;
    private static FakePlayer player;
    private static MaterialMonstrosity sculk;
    private static Mob husk;
    // At night, with nothing to fight: a Raevyx risen for an idle monstrosity, and a wild one.
    private static MaterialMonstrosity idleSculk;
    private static Mob wildDragon;
    private static Mob sleepyDragon;
    private static boolean risenSlept;
    private static boolean wildSlept;
    private static boolean wildAsleep;

    static {
        MOBS.put("ror:tyrannosaurus", true);
        MOBS.put("ror:alioramus", true);
        MOBS.put("ror:amargasaurus", false);
        MOBS.put("dawnera:tyrannosaurus", true);
        MOBS.put("dawnera:dodo", false);
        MOBS.put("saintsdragons:raevyx", true);
    }

    static boolean any() {
        return ModCompat.isLoaded("ror") || ModCompat.isLoaded("dawnera") || ModCompat.isLoaded(ModCompat.SAINTS_DRAGONS);
    }

    static void setUp(ServerLevel serverLevel, FakePlayer fakePlayer) {
        level = serverLevel;
        player = fakePlayer;
        for (int dz = -32; dz <= 48; dz += 16) {
            ChunkPos chunk = new ChunkPos(BASE.offset(0, 0, dz));
            level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 3, chunk);
        }
        for (BlockPos floor : BlockPos.betweenClosed(BASE.offset(-24, -1, -40), BASE.offset(24, -1, 56))) {
            level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
    }

    // A wild Raevyx as spawned in the world, its AI on, and ready for bed: fed, and sleepy enough to
    // sleep at once (a fresh one at night can take a minute or more, a hungry one longer). The rising
    // keeps the sleepiness.
    private static Mob dragon(BlockPos at) {
        Mob dragon = (Mob) type("saintsdragons:raevyx").create(level);
        dragon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90.0f, 0.0f);
        dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWN_EGG, null, null);
        net.minecraft.nbt.CompoundTag saved = dragon.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        saved.putFloat("SleepPressure", 95.0f);
        saved.putInt("Hunger", 100);
        dragon.load(saved);
        dragon.setPersistenceRequired();
        level.addFreshEntity(dragon);
        return dragon;
    }

    private static EntityType<?> type(String id) {
        ResourceLocation key = new ResourceLocation(id);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(key) ? BuiltInRegistries.ENTITY_TYPE.get(key) : null;
    }

    static void step(int t) {
        if (t > 207 && t < 340 && idleSculk != null) {
            wildSlept |= wildDragon != null && SaintsDragonChecks.sleepy(wildDragon);
            wildAsleep |= wildDragon != null && SaintsDragonChecks.asleep(wildDragon);
            risenSlept |= sleepyDragon != null && SaintsDragonChecks.sleepy(sleepyDragon);
        }
        // Both sent to bed as their own AI does when it decides to sleep (whether a wild one decides to
        // here is up to Saints Dragons): the wild one sits down and falls asleep; the risen one won't
        // (before, it sat down, was stood up, and stalled there, frozen in its bind pose).
        if (t == 230 && idleSculk != null) {
            if (wildDragon != null) {
                SaintsDragonChecks.sendToBed(wildDragon);
            }
            if (sleepyDragon != null) {
                SaintsDragonChecks.sendToBed(sleepyDragon);
            }
        }
        switch (t) {
            case 18 -> {
                sculk = MaterialMonstrosity.create(level, MonstrosityMaterial.SCULK);
                sculk.moveTo(BASE.getX() + 10.5, BASE.getY(), BASE.getZ() + 0.5, 0.0f, 0.0f);
                sculk.setTrueOwner(player);
                sculk.setNoAi(true);
                level.addFreshEntity(sculk);
                husk = EntityType.HUSK.create(level);
                husk.moveTo(BASE.getX() - 1.5, BASE.getY(), BASE.getZ() + 0.5, 90.0f, 0.0f);
                husk.setNoAi(true);
                husk.setPersistenceRequired();
                husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000.0);
                husk.setHealth(husk.getMaxHealth());
                level.addFreshEntity(husk);
                sculk.setTarget(husk);
            }
            // Spawned as in the world (their own spawn set-up, which some need to walk at all), a while
            // alive and decaying, then killed.
            case 20 -> {
                int i = 0;
                for (Map.Entry<String, Boolean> mob : MOBS.entrySet()) {
                    EntityType<?> type = type(mob.getKey());
                    if (type == null) {
                        continue;
                    }
                    check(type.create(level) instanceof Mob made && RisenMobs.risesAsItself(made) == mob.getValue(),
                            mob.getKey() + (mob.getValue() ? " rises as itself" : " rises as a sculk zombie"));
                    BlockPos at = BASE.offset(-12, 0, -25 + i++ * 10);
                    if (type.create(level) instanceof Mob living) {
                        living.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 90.0f, 0.0f);
                        living.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWN_EGG, null, null);
                        living.setNoAi(true);
                        level.addFreshEntity(living);
                        if (mob.getKey().startsWith("saintsdragons:")) {
                            SaintsDragonChecks.stun(living);
                        }
                        SculkDecay.apply(living, sculk, 2);
                        dying.add(living);
                    }
                }
            }
            case 60 -> {
                dying.forEach(Mob::kill);
                for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(BASE).inflate(60),
                        entity -> entity.isAlive() && (RisenMobs.isRisen(entity) || entity instanceof SculkZombie))) {
                    EntityType<?> was = entity instanceof SculkZombie zombie ? zombie.sourceType() : entity.getType();
                    risen.put(EntityType.getKey(was).toString(), entity);
                }
                level.getEntitiesOfClass(ItemEntity.class, new AABB(BASE).inflate(60)).forEach(Entity::discard);
                MOBS.forEach((id, itself) -> {
                    if (type(id) != null) {
                        Entity rose = risen.get(id);
                        check(itself ? rose instanceof Mob mob && RisenMobs.isRisen(mob) && !mob.isNoAi() : rose instanceof SculkZombie,
                                id + (itself ? " rose as itself, its AI on" : " rose as a sculk zombie"));
                    }
                });
                RiseChecks.view(BASE.offset(-12, 0, 0), -16.0, 7.0, 0.0);
            }
            case 90 -> {

                for (Map.Entry<String, Entity> rose : risen.entrySet()) {
                    if (rose.getValue() instanceof SculkZombie zombie) {
                        LivingEntity client = ClientSelfTest.clientCopy(zombie.getId());
                        check(client instanceof SculkZombie copy && copy.puppet != null && !copy.puppetFailed && !copy.puppetTickFailed,
                                "a sculk zombie's copy of " + rose.getKey() + " is drawn and ticked");
                    }
                }
            }
            case 94 -> ClientSelfTest.screenshot("risen_modded_row");
            // (A while after rising: big mobs take a while to reach the fight.)
            case 160 -> {
                List<String> idle = new ArrayList<>();
                risen.forEach((id, rose) -> {
                    if (rose instanceof Mob mob && RisenMobs.isRisen(mob) && mob.isAlive() && mob.getTarget() != husk) {
                        // (What it is doing instead.)
                        net.minecraft.nbt.CompoundTag saved = mob.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                        saved.getAllKeys().removeIf(key -> !key.startsWith("Data"));
                        idle.add(id + " targeting " + mob.getTarget() + ", " + (int) mob.distanceTo(husk) + " from it, following " + (int) mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
                                + ", path " + (mob.getNavigation().isDone() ? "none" : "on") + ", " + saved);
                    }
                });
                check(idle.isEmpty(), "modded mobs risen as themselves fight what their monstrosity fights (not: " + idle + ")");
                if (risen.get("saintsdragons:raevyx") instanceof Mob dragon) {
                    SaintsDragonChecks.checkRisen(dragon);
                }
                RiseChecks.view(BASE.offset(4, 0, 0), -6.0, 5.0, -10.0);
            }
            case 166 -> ClientSelfTest.screenshot("risen_modded_fight");
            case 200 -> sculk.discard();
            // A dragon goes to sleep by sitting down first, and waits until it's seated. A risen one
            // used to be stood up again at once, and stalled there, frozen in its bind pose; risen ones
            // now never go to sleep (SaintsDragonSleepMixin). The wild one shows the night is sleepy.
            case 205 -> {
                if (ModCompat.isLoaded(ModCompat.SAINTS_DRAGONS) && type("saintsdragons:raevyx") != null) {
                    level.setDayTime(18000);
                    BlockPos at = BASE.offset(0, 0, 40);
                    idleSculk = MaterialMonstrosity.create(level, MonstrosityMaterial.SCULK);
                    idleSculk.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
                    idleSculk.setTrueOwner(player);
                    idleSculk.setNoAi(true);
                    level.addFreshEntity(idleSculk);
                    wildDragon = dragon(at.offset(-12, 0, 8));
                    sleepyDragon = dragon(at.offset(-6, 0, 0));
                    SculkDecay.apply(sleepyDragon, idleSculk, 2);
                    risenSlept = false;
                    wildSlept = false;
                    wildAsleep = false;
                }
            }
            case 207 -> {
                if (sleepyDragon != null) {
                    sleepyDragon.kill();
                    sleepyDragon = level.getEntitiesOfClass(Mob.class, new AABB(BASE.offset(-6, 0, 40)).inflate(12),
                            mob -> RisenMobs.isRisen(mob) && EntityType.getKey(mob.getType()).toString().equals("saintsdragons:raevyx")).stream().findFirst().orElse(null);
                    check(sleepyDragon != null, "a raevyx rose for an idle monstrosity at night");
                }
            }
            // How the risen one looks at night (once it stalled, it stood frozen in its bind pose).
            case 300 -> {
                if (sleepyDragon != null) {
                    RiseChecks.view(sleepyDragon.blockPosition(), -9.0, 4.0, 6.0);
                }
            }
            case 306 -> {
                if (sleepyDragon != null) {
                    ClientSelfTest.screenshot("risen_raevyx_at_night");
                }
            }
            case 340 -> {
                if (idleSculk != null) {
                    check(wildSlept && wildAsleep, "a wild raevyx sent to bed sits down and falls asleep: " + SaintsDragonChecks.sleepState(wildDragon));
                    check(!risenSlept && sleepyDragon != null && sleepyDragon.isAlive() && !SaintsDragonChecks.sleepy(sleepyDragon),
                            "a risen raevyx never goes to sleep, even sent to bed (it would stall, frozen in its bind pose): "
                            + (sleepyDragon == null ? "gone" : SaintsDragonChecks.sleepState(sleepyDragon)));
                    level.setDayTime(6000);
                }
            }
            case 360 -> level.getEntitiesOfClass(Entity.class, new AABB(BASE).inflate(60), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
            default -> {
            }
        }
    }
}
