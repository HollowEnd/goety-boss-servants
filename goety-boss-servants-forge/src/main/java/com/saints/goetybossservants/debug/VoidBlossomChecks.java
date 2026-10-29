package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.ally.undead.zombie.ZombieServant;
import com.Polarice3.Goety.utils.MobUtil;
import com.cerbon.bosses_of_mass_destruction.block.BMDBlocks;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.voidblossom.VoidBlossomServant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Void Blossom part of the dev self-test (see SelfTest). Steps are ticks after START. */
class VoidBlossomChecks {
    static final int START = 2500;
    private static final BlockPos CENTER = new BlockPos(0, 120, 160);

    private static VoidBlossomServant blossom;
    private static float healthBeforeDrop;
    private static BlockPos lightPos = BlockPos.ZERO;
    private static ArmorStand owner;
    private static Wolf pet;
    private static Mob enemy;
    private static List<BlockPos> flowers = new ArrayList<>();
    private static BlockPos burned;
    private static BlockPos fire;
    private static float burnStart;
    private static net.minecraft.world.entity.LivingEntity lastTarget;
    private static final List<String> targetLog = new ArrayList<>();

    static void step(ServerLevel level, FakePlayer player, int t) {
        if (blossom != null && t > 5 && t < 600 && blossom.getTarget() != lastTarget) {
            targetLog.add("t=" + t + " -> " + (blossom.getTarget() == null ? "none" : blossom.getTarget().getType().getDescriptionId()));
            lastTarget = blossom.getTarget();
        }
        switch (t) {
            case 0 -> {
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        ChunkPos chunk = new ChunkPos(CENTER.offset(dx * 16, 0, dz * 16));
                        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 1, chunk);
                    }
                }
                BlockPos.betweenClosed(CENTER.offset(-25, -1, -25), CENTER.offset(25, -1, 25))
                        .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", "summon_void_blossom")).isPresent(), "void blossom summon ritual loaded");
                check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", "revive_void_blossom")).isPresent(), "void blossom revive ritual loaded");
                blossom = ModRegistry.VOID_BLOSSOM.get().create(level);
                MobUtil.summonTame(blossom, player);
                blossom.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                blossom.finalizeSpawn(level, level.getCurrentDifficultyAt(CENTER), MobSpawnType.MOB_SUMMONED, null, null);
                level.addFreshEntity(blossom);
                check(blossom instanceof IServant && player.getUUID().equals(blossom.getOwnerId()), "void blossom tamed to the caster");
                check(blossom.isStaying(), "void blossom is planted (staying) when summoned");
                check(blossom.getMaxHealth() > 1.0f, "BMD void blossom attributes applied (health " + blossom.getMaxHealth() + ")");
            }
            case 5 -> {
                // Owner and a tamed wolf inside its 3-block thorn aura; a sturdy husk out in the spike range.
                owner = EntityType.ARMOR_STAND.create(level);
                owner.moveTo(CENTER.getX() + 2.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                level.addFreshEntity(owner);
                blossom.setTrueOwner(owner);
                pet = EntityType.WOLF.create(level);
                pet.moveTo(CENTER.getX() - 1.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                pet.setTame(true);
                pet.setOwnerUUID(player.getUUID());
                pet.setNoAi(true);
                level.addFreshEntity(pet);
                enemy = EntityType.HUSK.create(level);
                enemy.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 12.5, 0.0f, 0.0f);
                enemy.setNoAi(true);
                enemy.setPersistenceRequired();
                enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000.0);
                enemy.setHealth(enemy.getMaxHealth());
                level.addFreshEntity(enemy);
                blossom.setTarget(enemy);
            }
            case 600 -> {
                check(blossom.getTarget() == enemy, "void blossom fights a mob (not just players) " + targetLog);
                check(enemy.getHealth() < enemy.getMaxHealth(), "its attacks hurt the enemy (husk " + enemy.getHealth() + "/" + enemy.getMaxHealth() + ")");
                check(owner.getHealth() == owner.getMaxHealth(), "its spikes and thorns spare the owner (" + owner.getHealth() + ")");
                check(pet.getHealth() == pet.getMaxHealth() && !pet.hasEffect(MobEffects.POISON), "its spikes, thorns and spores spare pets (wolf " + pet.getHealth() + ")");
                // Its area attacks are all kept off others the same way: they hit a hostile summoner's
                // minions, but never a player's servants (each tried with its own damage, then gone).
                Mob minion = goetyServant(level, enemy, true);
                Mob tame = goetyServant(level, player, false);
                minion.hurt(blossom.damageSources().mobAttack(blossom), 4.0f);
                tame.hurt(blossom.damageSources().mobAttack(blossom), 4.0f);
                check(minion.getHealth() < minion.getMaxHealth(), "its area attacks hit a hostile summoner's minions (" + minion.getHealth() + ")");
                check(tame.getHealth() == tame.getMaxHealth(), "its area attacks spare a player's servants (" + tame.getHealth() + ")");
                minion.discard();
                tame.discard();
                // Drop it below 75% health: its next attack is the Blossom move.
                ServantConfig.VoidBlossomFlowerSeconds.set(20);
                blossom.hurt(level.damageSources().generic(), blossom.getMaxHealth() * 0.3f);
            }
            case 880 -> {
                // Its first flowers are out or still being planted; a sudden drop past its 50% milestone
                // must not queue another Blossom on top of them.
                healthBeforeDrop = blossom.getHealth();
                check(blossom.hasBlossomsOut() && !blossom.isBlossomPending(), "its first Blossom is out (flowers " + blossom.flowerPositions().size() + ")");
                blossom.setHealth(blossom.getMaxHealth() * 0.52f);
                blossom.hurt(level.damageSources().generic(), blossom.getMaxHealth() * 0.05f);
                check(!blossom.isBlossomPending(), "no new Blossom while its flowers are still out (health " + blossom.getHealth() + "/" + blossom.getMaxHealth() + ")");
                blossom.setHealth(healthBeforeDrop);
            }
            case 900 -> {
                flowers = new ArrayList<>(blossom.flowerPositions());
                check(!flowers.isEmpty(), "Blossom move plants healing flowers (" + flowers.size() + ")");
                check(flowers.stream().allMatch(pos -> Math.sqrt(pos.distSqr(blossom.blockPosition())) < 6.0), "flowers are close to it (about 4 blocks out)");
                check(flowers.stream().allMatch(pos -> level.getBlockState(pos).is(BMDBlocks.VOID_BLOSSOM.get()) && level.getBlockState(pos.below()).is(Blocks.STONE)), "flowers are BMD's Void Blossom on the existing ground (no moss)");
                check(BlockPos.betweenClosedStream(CENTER.offset(-8, 0, -8), CENTER.offset(8, 3, 8)).noneMatch(pos -> level.getBlockState(pos).is(BMDBlocks.VINE_WALL.get())), "no vine walls");
                if (!flowers.isEmpty()) {
                    burned = null;
                    // A flower whose fire spot (just outside it) is clear of the blossom's large hitbox: fire inside
                    // it would set the blossom itself alight, and a burning blossom drops all its flowers.
                    for (BlockPos flower : flowers) {
                        net.minecraft.world.phys.Vec3 outward = net.minecraft.world.phys.Vec3.atCenterOf(flower).subtract(blossom.position()).multiply(1.0, 0.0, 1.0).normalize();
                        BlockPos spot = BlockPos.containing(net.minecraft.world.phys.Vec3.atCenterOf(flower).add(outward));
                        if (!blossom.getBoundingBox().inflate(1.0).intersects(new AABB(spot))) {
                            burned = flower;
                            fire = spot;
                            break;
                        }
                    }
                    if (burned != null) {
                        level.setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
            case 902 -> {
                check(burned != null && !level.getBlockState(burned).is(BMDBlocks.VOID_BLOSSOM.get()) && !blossom.flowerPositions().contains(burned)
                        && level.getBlockState(burned.below()).is(Blocks.STONE), "a flower touched by fire disappears at once (ground restored)");
                if (fire != null) {
                    level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
                }
            }
            case 1320 -> {
                check(blossom.flowerPositions().isEmpty(), "flowers disappear when their time is up");
                check(flowers.stream().noneMatch(pos -> level.getBlockState(pos).is(BMDBlocks.VOID_BLOSSOM.get()) || level.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK)),
                        "the blocks they replaced are back");
                ServantConfig.VoidBlossomFlowerSeconds.set(30);
                // Below 50%: its next attack is another Blossom, for the burning check.
                blossom.setHealth(blossom.getMaxHealth() * 0.55f);
                blossom.hurt(level.damageSources().generic(), blossom.getMaxHealth() * 0.1f);
            }
            case 1620 -> {
                check(!blossom.flowerPositions().isEmpty(), "second Blossom planted flowers (" + blossom.flowerPositions().size() + ")");
                burnStart = blossom.getHealth();
                blossom.setSecondsOnFire(5);
            }
            case 1622 -> {
                check(blossom.isOnFire(), "void blossom can catch fire");
                check(blossom.flowerPositions().isEmpty(), "while it burns, all its flowers disappear");
            }
            case 1680 -> {
                check(blossom.getHealth() < burnStart, "fire hurts it (" + burnStart + " -> " + blossom.getHealth() + ")");
                blossom.clearFire();
                // Out of combat it should heal past its 75% milestone (BMD's own idle heal stops there).
                enemy.discard();
                blossom.setTarget(null);
                blossom.setHealth(blossom.getMaxHealth() * 0.6f);
            }
            case 2000 -> {
                check(blossom.getHealth() > blossom.getMaxHealth() * 0.76f, "heals out of combat past its milestones (" + blossom.getHealth() + "/" + blossom.getMaxHealth() + ")");
                lightPos = blossom.blockPosition();
                check(level.getBlockState(lightPos).is(Blocks.LIGHT), "it lights up where it stands (BMD's light block)");
                blossom.kill();
            }
            case 2030 -> {
                check(!level.getEntitiesOfClass(ItemEntity.class, new AABB(CENTER).inflate(16),
                        item -> item.getItem().is(ModRegistry.VOID_BLOSSOM_SOUL.get())).isEmpty(), "void blossom drops its soul on death");
                owner.discard();
                pet.discard();
            }
            case 2090 -> {
                check(BlockPos.betweenClosedStream(lightPos.below(4), lightPos.above()).noneMatch(pos -> level.getBlockState(pos).is(Blocks.LIGHT)), "its light goes when it dies");
                blossom = ModRegistry.VOID_BLOSSOM.get().create(level);
                MobUtil.summonTame(blossom, player);
                // Elsewhere, so a light left by the first one can't be mistaken for this one's.
                blossom.moveTo(CENTER.getX() + 6.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                blossom.finalizeSpawn(level, level.getCurrentDifficultyAt(CENTER), MobSpawnType.MOB_SUMMONED, null, null);
                level.addFreshEntity(blossom);
            }
            case 2120 -> {
                lightPos = blossom.blockPosition();
                check(level.getBlockState(lightPos).is(Blocks.LIGHT), "a new blossom lights up too");
                // Dismissed or stored away (Goety's Esoteric Tesseract) = discarded without dying.
                blossom.discard();
            }
            case 2122 -> {
                check(BlockPos.betweenClosedStream(lightPos.below(4), lightPos.above()).noneMatch(pos -> level.getBlockState(pos).is(Blocks.LIGHT)), "its light goes when it's discarded without dying");
            }
            default -> {
            }
        }
    }

    // A Goety zombie servant near the blossom (out of its own attacks' reach), owned by {@code owner}, hostile or not.
    private static Mob goetyServant(ServerLevel level, net.minecraft.world.entity.LivingEntity owner, boolean hostile) {
        ZombieServant servant = ModEntityType.ZOMBIE_SERVANT.get().create(level);
        servant.moveTo(CENTER.getX() + 6.5, CENTER.getY(), CENTER.getZ() - 6.5, 0.0f, 0.0f);
        servant.setNoAi(true);
        servant.setTrueOwner(owner);
        servant.setHostile(hostile);
        level.addFreshEntity(servant);
        return servant;
    }
}
