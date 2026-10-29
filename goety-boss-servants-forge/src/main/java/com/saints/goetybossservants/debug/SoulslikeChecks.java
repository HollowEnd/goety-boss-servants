package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.entity.soulslike.OldChampionServant;
import com.saints.goetybossservants.entity.soulslike.ReturningKnightServant;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.soulsweaponry.entity.mobs.DarkSorcerer;
import net.soulsweaponry.entity.mobs.NightShade;
import net.soulsweaponry.entity.mobs.Remnant;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Old Champion and Returning Knight part of the dev self-test (see SelfTest). Steps are ticks after START. */
class SoulslikeChecks {
    static final int START = 6600;
    private static final BlockPos CENTER = new BlockPos(160, 120, 0);

    private static OldChampionServant champion;
    private static ReturningKnightServant knight;
    private static ArmorStand owner;
    private static Mob enemy;
    private static Vec3 ownerSpot;
    private static float ownerHealth;
    private static final List<String> skyDebuffed = new ArrayList<>();
    private static final List<String> minionTargetedFriend = new ArrayList<>();
    private static int maxMinionsSeen;
    private static boolean remnantSeen;
    private static boolean remnantTamedToOwner = true;
    private static boolean sorcererSeen;
    private static boolean sorcererBeamsKnight;
    private static float knightHurtHealth;
    private static double knightWalkedWhileClose;
    private static Vec3 knightLastPos;
    private static Vec3 animLastPos;
    private static int knightStillTicks;
    private static int knightMovingTicks;
    private static int animSteadyTicks;
    private static int animWrongTicks;
    private static boolean animSawWalk;
    private static boolean animSawIdle;
    private static double knightClosestGap = Double.MAX_VALUE;

    private static <T extends Mob> T summon(EntityType<T> type, ServerLevel level, FakePlayer player, BlockPos at) {
        T summoned = type.create(level);
        MobUtil.summonTame(summoned, player);
        summoned.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        summoned.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(summoned);
        return summoned;
    }

    private static Mob sturdyEnemy(ServerLevel level, BlockPos at) {
        Mob mob = EntityType.HUSK.create(level);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
        mob.setHealth(mob.getMaxHealth());
        level.addFreshEntity(mob);
        return mob;
    }

    private static ArmorStand ownerStand(ServerLevel level, BlockPos at) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f);
        level.addFreshEntity(stand);
        return stand;
    }

    private static boolean dropped(ServerLevel level, BlockPos at, Item item) {
        return !level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(16), entity -> entity.getItem().is(item)).isEmpty();
    }

    private static boolean ownerUnharmed() {
        return owner.getHealth() == ownerHealth && owner.position().distanceTo(ownerSpot) < 0.3 && owner.getActiveEffects().isEmpty();
    }

    private static String ownerState() {
        return "(health " + owner.getHealth() + "/" + ownerHealth + ", moved " + String.format("%.2f", owner.position().distanceTo(ownerSpot))
                + ", effects " + owner.getActiveEffects().size() + ")";
    }

    static void step(ServerLevel level, FakePlayer player, int t) {
        if (champion != null && champion.isAlive() && t > 20 && t < 400
                && champion.getHealth() > champion.getMaxHealth() / 2.0f && champion.hasEffect(MobEffects.WEAKNESS)) {
            skyDebuffed.add("t=" + t);
        }
        // The client plays "walk" when isAggressive() is true there; that should follow actual movement
        // (standing in a fight, walking after its owner out of one).
        if (knight != null && knight.isAlive() && (t > 460 && t < 1000 || t > 1006 && t < 1090)) {
            if (animLastPos != null) {
                boolean moving = Math.hypot(knight.getX() - animLastPos.x, knight.getZ() - animLastPos.z) > 0.03;
                knightMovingTicks = moving ? knightMovingTicks + 1 : 0;
                knightStillTicks = moving ? 0 : knightStillTicks + 1;
                net.minecraft.world.entity.LivingEntity copy = ClientSelfTest.clientCopy(knight.getId());
                if (copy instanceof Mob clientKnight && (knightMovingTicks > 10 || knightStillTicks > 10)) {
                    ++animSteadyTicks;
                    boolean walkAnim = clientKnight.isAggressive();
                    animSawWalk |= walkAnim && knightMovingTicks > 10;
                    animSawIdle |= !walkAnim && knightStillTicks > 10;
                    if (walkAnim != knightMovingTicks > 10) {
                        ++animWrongTicks;
                    }
                }
            }
            animLastPos = knight.position();
        }
        if (knight != null && knight.isAlive() && t > 460 && t < 1000) {
            double gap = Math.hypot(enemy.getX() - knight.getX(), enemy.getZ() - knight.getZ()) - (knight.getBbWidth() + enemy.getBbWidth()) / 2.0;
            knightClosestGap = Math.min(knightClosestGap, gap);
            // How far it keeps walking once already within reach (it used to keep shoving into the target).
            if (t > 560 && gap < 1.4 && knightLastPos != null) {
                knightWalkedWhileClose += Math.hypot(knight.getX() - knightLastPos.x, knight.getZ() - knightLastPos.z);
            }
            knightLastPos = knight.position();
            List<Mob> minions = level.getEntitiesOfClass(Mob.class, knight.getBoundingBox().inflate(128.0), mob -> ServantMinions.isMinionOf(mob, knight));
            maxMinionsSeen = Math.max(maxMinionsSeen, minions.size());
            for (Mob minion : minions) {
                if (minion.getTarget() == owner) {
                    minionTargetedFriend.add(minion.getType().toShortString() + " t=" + t);
                }
                if (minion instanceof Remnant remnant) {
                    remnantSeen = true;
                    remnantTamedToOwner &= remnant.isTame() && owner.getUUID().equals(remnant.getOwnerUUID());
                }
                if (minion instanceof DarkSorcerer sorcerer) {
                    sorcererSeen = true;
                    sorcererBeamsKnight |= sorcerer.getTarget() == knight && sorcerer.getBeaming();
                }
            }
        }
        switch (t) {
            case 0 -> {
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        ChunkPos chunk = new ChunkPos(CENTER.offset(dx * 16, 0, dz * 16));
                        level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
                    }
                }
                BlockPos.betweenClosed(CENTER.offset(-30, -1, -30), CENTER.offset(30, -1, 30))
                        .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                for (String id : new String[]{"summon_old_champion", "revive_old_champion", "summon_returning_knight", "revive_returning_knight"}) {
                    check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", id)).isPresent(), id + " ritual loaded");
                }
                champion = summon(ModRegistry.OLD_CHAMPION.get(), level, player, CENTER);
                check(champion instanceof IServant && player.getUUID().equals(champion.getOwnerId()), "old champion tamed to the caster");
                check(champion.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 32.0, "old champion follow range 32");
            }
            case 5 -> {
                // Fight: a sturdy husk with the owner right beside it, inside the slam and battle cry.
                enemy = sturdyEnemy(level, CENTER.offset(4, 0, 0));
                owner = ownerStand(level, CENTER.offset(4, 0, 2));
                champion.setTrueOwner(owner);
            }
            case 10 -> {
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
                champion.setTarget(enemy);
            }
            case 400 -> {
                check(enemy.getHealth() < enemy.getMaxHealth(), "old champion hurts the enemy (husk " + enemy.getHealth() + ")");
                check(ownerUnharmed(), "owner beside its target untouched by slams and battle cries " + ownerState());
                check(skyDebuffed.isEmpty(), "no Slowness/Weakness under open sky " + skyDebuffed);
                // The boss grows resistant to hits from the same held item; an unarmed mob always counts as that.
                List<Float> taken = new ArrayList<>();
                for (int hit = 0; hit < 15; ++hit) {
                    champion.setHealth(champion.getMaxHealth());
                    champion.removeAllEffects();
                    champion.setShielding(false);
                    champion.invulnerableTime = 0;
                    champion.hurt(level.damageSources().mobAttack(enemy), 5.0f);
                    taken.add(champion.getMaxHealth() - champion.getHealth());
                }
                check(taken.get(0) > 0.0f && Math.abs(taken.get(14) - taken.get(0)) < 0.01f, "old champion takes full damage from the same unarmed attacker every hit (first " + taken.get(0) + ", 15th " + taken.get(14) + ")");
                champion.kill();
            }
            case 445 -> {
                check(level.getEntitiesOfClass(NightShade.class, new AABB(CENTER).inflate(64)).isEmpty(), "no Night Shade rises from its death");
                check(dropped(level, champion.blockPosition(), ModRegistry.OLD_CHAMPION_SOUL.get()), "old champion drops its soul on death");
                check(level.getEntitiesOfClass(ExperienceOrb.class, new AABB(champion.blockPosition()).inflate(16)).isEmpty(), "old champion drops no experience");
                level.getEntitiesOfClass(ItemEntity.class, new AABB(CENTER).inflate(32)).forEach(ItemEntity::discard);
                enemy.discard();
            }
            case 450 -> {
                // Bring the real client player over so the client tracks the knight (for the animation check).
                level.players().forEach(p -> p.teleportTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() - 20.5));
                knight = summon(ModRegistry.RETURNING_KNIGHT.get(), level, player, CENTER);
                check(player.getUUID().equals(knight.getOwnerId()), "returning knight tamed to the caster");
                enemy = sturdyEnemy(level, CENTER.offset(6, 0, 0));
                owner.moveTo(CENTER.getX() + 6.5, CENTER.getY(), CENTER.getZ() + 2.5, 0.0f, 0.0f);
                knight.setTrueOwner(owner);
            }
            case 455 -> {
                // Hurt a little (above half health): its Dark Sorcerers should heal it back up.
                knight.hurt(level.damageSources().generic(), knight.getMaxHealth() * 0.2f);
                knightHurtHealth = knight.getHealth();
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
            }
            case 460 -> knight.setTarget(enemy);
            case 1000 -> {
                check(enemy.getHealth() < enemy.getMaxHealth(), "returning knight hurts the enemy (husk " + enemy.getHealth() + ")");
                check(ownerUnharmed(), "owner beside its target untouched by Obliterate, Rupture and Mace of Spades " + ownerState());
                check(maxMinionsSeen > 0, "returning knight summons minions (most at once: " + maxMinionsSeen + ")");
                check(maxMinionsSeen <= ServantConfig.ReturningKnightMaxMinions.get(), "knight minions capped at " + ServantConfig.ReturningKnightMaxMinions.get());
                check(remnantSeen && remnantTamedToOwner, "its Remnants are tamed to its owner (seen " + remnantSeen + ")");
                check(sorcererSeen && sorcererBeamsKnight, "its Dark Sorcerers beam at the knight (seen " + sorcererSeen + ")");
                check(knight.getHealth() > knightHurtHealth, "the Sorcerers heal it (" + knightHurtHealth + " -> " + knight.getHealth() + ")");
                check(minionTargetedFriend.isEmpty(), "knight minions never target the owner " + minionTargetedFriend);
                check(knightClosestGap < 3.5, "returning knight closes in on its target (closest gap " + String.format("%.2f", knightClosestGap) + ")");
                check(knightWalkedWhileClose < 1.0, "returning knight stops short instead of walking into its target (walked " + String.format("%.2f", knightWalkedWhileClose) + " blocks while within reach)");
                // Stored away (like Goety's Esoteric Tesseract) = removed: its minions go too.
                knight.discard();
            }
            case 1005 -> {
                check(level.getEntitiesOfClass(PathfinderMob.class, new AABB(CENTER).inflate(128.0), ServantMinions::isMinion).isEmpty(), "knight minions despawn when it's gone");
                enemy.discard();
                knight = summon(ModRegistry.RETURNING_KNIGHT.get(), level, player, CENTER);
                // Out of combat, following its owner across the platform: the walk animation should play.
                owner.moveTo(CENTER.getX() + 22.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                knight.setTrueOwner(owner);
                animLastPos = null;
                knightMovingTicks = 0;
                knightStillTicks = 0;
            }
            case 1090 -> knight.kill();
            case 1180 -> {
                check(animSawWalk && animSawIdle && animWrongTicks * 10 < animSteadyTicks, "knight walk animation only while moving, idle while standing (walk seen " + animSawWalk + ", idle seen " + animSawIdle + ", wrong " + animWrongTicks + "/" + animSteadyTicks + ")");
                check(knight.isRemoved(), "returning knight's death animation finishes");
                check(dropped(level, knight.blockPosition(), ModRegistry.RETURNING_KNIGHT_SOUL.get()), "returning knight drops its soul on death");
                check(level.getEntitiesOfClass(ExperienceOrb.class, new AABB(knight.blockPosition()).inflate(16)).isEmpty(), "returning knight drops no experience");
                owner.discard();
            }
            default -> {
            }
        }
    }
}
