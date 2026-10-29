package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.CaptainCorneliaServant;
import com.saints.goetybossservants.entity.CorneliaWeapons;
import com.saints.goetybossservants.registry.ModRegistry;
import dev.obscuria.aquamirae.registry.AquamiraeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Captain Cornelia part of the dev self-test (see SelfTest). Steps are ticks after START. */
class CorneliaChecks {
    static final int START = 330;
    private static final BlockPos CENTER = new BlockPos(0, 120, 40);

    private static CaptainCorneliaServant cornelia;
    private static ArmorStand owner;
    private static Mob enemy;
    private static Mob cow;
    private static IronGolem golem;
    private static Vec3 ownerSpot;
    private static float ownerHealth;
    private static float lastStandHealth;
    private static float afterWindowHealth;
    private static boolean saberSeen;
    private static boolean shatterbladeSeen;

    private static boolean ownerUnharmed() {
        return owner.getHealth() == ownerHealth && owner.position().distanceTo(ownerSpot) < 0.3 && owner.getActiveEffects().isEmpty();
    }

    private static String ownerState() {
        return "(health " + owner.getHealth() + "/" + ownerHealth + ", moved " + String.format("%.2f", owner.position().distanceTo(ownerSpot))
                + ", effects " + owner.getActiveEffects().size() + ")";
    }

    static void step(ServerLevel level, FakePlayer player, int t) {
        if (cornelia != null && t > 10 && t < 300) {
            shatterbladeSeen |= cornelia.getMainHandItem().is(AquamiraeItems.SHATTERBLADE.get()) && cornelia.getOffhandItem().is(AquamiraeItems.SHATTERBLADE.get());
        }
        if (cornelia != null && t > 300 && t < 500) {
            saberSeen |= cornelia.getMainHandItem().is(AquamiraeItems.REMNANT_SABER.get());
        }
        switch (t) {
            case 0 -> {
                ChunkPos chunk = new ChunkPos(CENTER);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 2, chunk);
                BlockPos.betweenClosed(CENTER.offset(-12, -1, -12), CENTER.offset(12, -1, 12))
                        .forEach(pos -> level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()));
                for (String id : new String[]{"summon_captain_cornelia", "revive_captain_cornelia"}) {
                    check(level.getRecipeManager().byKey(new ResourceLocation("goetybossservants", id)).isPresent(), id + " ritual loaded");
                }
                cornelia = ModRegistry.CAPTAIN_CORNELIA.get().create(level);
                MobUtil.summonTame(cornelia, player);
                cornelia.moveTo(CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                cornelia.finalizeSpawn(level, level.getCurrentDifficultyAt(CENTER), MobSpawnType.MOB_SUMMONED, null, null);
                level.addFreshEntity(cornelia);
                // Stored in a Tesseract and let out again (saved, removed, loaded back): she must not come
                // back hostile (Goety's checkHostility marks every Enemy hostile on load).
                net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
                cornelia.save(saved);
                cornelia.discard();
                cornelia = (CaptainCorneliaServant) EntityType.loadEntityRecursive(saved, level, entity -> entity);
                level.addFreshEntity(cornelia);
                check(!cornelia.isHostile(), "cornelia is not hostile after being stored and let out (Tesseract)");
                check(player.getUUID().equals(cornelia.getOwnerId()), "cornelia tamed to the caster");
                check(cornelia.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 32.0, "cornelia follow range 32");
                check(!cornelia.bossInfo.isVisible(), "cornelia has no boss bar");
                check(!cornelia.hasEffect(MobEffects.LEVITATION), "cornelia skips the boss's rising-from-the-sea levitation");
                check(cornelia.getMainHandItem().is(cornelia.getSeasonalWeapon()), "cornelia starts with her main weapon (" + cornelia.getMainHandItem() + ")");
            }
            case 5 -> {
                // Fight: a sturdy husk with the owner right beside it, inside her swing and rage spin.
                enemy = EntityType.HUSK.create(level);
                enemy.moveTo(CENTER.getX() + 3.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                enemy.setNoAi(true);
                enemy.setPersistenceRequired();
                enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0);
                enemy.setHealth(enemy.getMaxHealth());
                level.addFreshEntity(enemy);
                owner = EntityType.ARMOR_STAND.create(level);
                owner.moveTo(CENTER.getX() + 3.5, CENTER.getY(), CENTER.getZ() + 2.5, 0.0f, 0.0f);
                level.addFreshEntity(owner);
                cornelia.setTrueOwner(owner);
                golem = EntityType.IRON_GOLEM.create(level);
                golem.moveTo(CENTER.getX() - 4.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                level.addFreshEntity(golem);
            }
            case 10 -> {
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
                cornelia.setTarget(enemy);
            }
            case 150 -> {
                // Mid-fight the owner hits something else: she must switch to it (Goety's own goal wouldn't).
                cow = EntityType.COW.create(level);
                cow.moveTo(CENTER.getX() - 3.5, CENTER.getY(), CENTER.getZ() + 0.5, 0.0f, 0.0f);
                cow.setNoAi(true);
                level.addFreshEntity(cow);
                owner.setLastHurtMob(cow);
            }
            case 155 -> {
                check(cornelia.getTarget() == cow, "she switches to what her owner hits mid-fight (target " + cornelia.getTarget() + ")");
                cow.discard();
                cornelia.setTarget(enemy);
            }
            case 300 -> {
                check(enemy.getHealth() < 1024.0f, "cornelia hurts the enemy (husk " + enemy.getHealth() + "/1024, its max health cut by Health Shatter)");
                check(cornelia.getTarget() == enemy, "cornelia keeps fighting the enemy");
                check(ownerUnharmed(), "owner beside her target untouched by her swings " + ownerState());
                check(golem.getTarget() != cornelia, "iron golems leave her alone");
                check(shatterbladeSeen,
                        "she dual-wields the Shatterblade against a big target until its Health Shatter is full (now " + cornelia.getMainHandItem() + ")");
                int stacks = CorneliaWeapons.shatterStacks(enemy);
                net.minecraft.world.effect.MobEffectInstance shatter = enemy.getEffect(dev.obscuria.aquamirae.registry.AquamiraeEffects.HEALTH_SHATTER.get());
                check(stacks >= 2 && stacks % 2 == 0, "her Shatterblade adds Health Shatter 2 stacks per hit (" + stacks + ")");
                // A minute per hit landed, minus the time since that hit.
                check(shatter != null && shatter.getDuration() > (stacks / 2 - 1) * 1200 && shatter.getDuration() <= stacks / 2 * 1200,
                        "it lasts a minute per hit, up to 5 (" + (shatter == null ? 0 : shatter.getDuration() / 20) + " s left after " + stacks / 2 + " hits)");
                // Below 25% health: her rage (levitation and the spin around her) comes next, and below
                // half health she takes the Remnant Saber.
                cornelia.setHealth(cornelia.getMaxHealth() * 0.2f);
            }
            case 310 -> check(cornelia.getPhase() == 1 && cornelia.hasEffect(MobEffects.LEVITATION), "cornelia rages below 25% health (phase " + cornelia.getPhase() + ")");
            case 499 -> check(saberSeen, "below half health she switches to the Remnant Saber (her rage heals her back to 90%, and then she puts it away)");
            case 500 -> {
                check(ownerUnharmed(), "owner not spun or hurt by her rage " + ownerState());
                // Out of the fight, then the last stand: Aquamirae's slow regeneration at 20 health or
                // less only runs for one 10-second window.
                enemy.discard();
                golem.discard();
                cornelia.setTarget(null);
                cornelia.removeEffect(MobEffects.LEVITATION);
                cornelia.setHealth(10.0f);
                lastStandHealth = cornelia.getHealth();
            }
            case 600 -> check(cornelia.isLastStandActive() && cornelia.getHealth() > lastStandHealth,
                    "last stand regenerates (" + lastStandHealth + " -> " + cornelia.getHealth() + ")");
            case 720 -> afterWindowHealth = cornelia.getHealth();
            case 760 -> {
                check(cornelia.isLastStandUsed() && !cornelia.isLastStandActive() && cornelia.getHealth() == afterWindowHealth,
                        "last stand stops after its 10 seconds (" + afterWindowHealth + " -> " + cornelia.getHealth() + ")");
                // Healed first, so the shortened reset below doesn't start a new last stand.
                cornelia.setHealth(cornelia.getMaxHealth());
                ServantConfig.CaptainCorneliaPhaseResetSeconds.set(1);
            }
            case 800 -> {
                check(cornelia.getPhase() == 0 && !cornelia.isLastStandUsed(), "rage and last stand come back out of combat");
                cornelia.kill();
            }
            case 860 -> {
                AABB area = new AABB(CENTER).inflate(16);
                check(!level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(ModRegistry.CAPTAIN_CORNELIA_SOUL.get())).isEmpty(), "cornelia drops her soul on death");
                check(level.getEntitiesOfClass(ItemEntity.class, area, item -> !item.getItem().is(ModRegistry.CAPTAIN_CORNELIA_SOUL.get())).isEmpty(), "cornelia drops no weapon");
                check(level.getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(), "cornelia drops no experience");
                ServantConfig.CaptainCorneliaPhaseResetSeconds.set(60);
                owner.discard();
            }
            default -> {
            }
        }
    }
}
