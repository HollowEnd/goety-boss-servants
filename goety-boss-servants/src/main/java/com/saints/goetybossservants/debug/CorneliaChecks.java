package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.aquamirae.CaptainCorneliaServant;
import com.saints.goetybossservants.entity.aquamirae.CorneliaWeapons;
import dev.obscuria.aquamirae.registry.AquamiraeItems;
import com.saints.goetybossservants.registry.ModEntities;
import com.saints.goetybossservants.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffects;
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

/** Captain Cornelia part of the dev self-test (see SelfTest). Steps are ticks after START. */
class CorneliaChecks {
    static final int START = 10;
    static final int LENGTH = 900;
    private static final BlockPos CENTER = new BlockPos(0, 120, 40);

    private static CaptainCorneliaServant cornelia;
    private static ArmorStand owner;
    private static Mob enemy;
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

    static void step(ServerLevel level, Player player, int t) {
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
                cornelia = (CaptainCorneliaServant) ModEntities.CAPTAIN_CORNELIA.create(level);
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
                SelfTest.check(!cornelia.isHostile(), "cornelia is not hostile after being stored and let out (Tesseract)");
                SelfTest.check(player.getUUID().equals(cornelia.getOwnerId()), "cornelia tamed to the caster");
                SelfTest.check(cornelia.getAttributeBaseValue(Attributes.FOLLOW_RANGE) == 32.0, "cornelia follow range 32");
                SelfTest.check(!cornelia.bossInfo.isVisible(), "cornelia has no boss bar");
                SelfTest.check(!cornelia.hasEffect(MobEffects.LEVITATION), "cornelia skips the boss's rising-from-the-sea levitation");
                SelfTest.check(cornelia.getMainHandItem().is(cornelia.getSeasonalWeapon()), "cornelia starts with her main weapon (" + cornelia.getMainHandItem() + ")");
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
            }
            case 10 -> {
                ownerSpot = owner.position();
                ownerHealth = owner.getHealth();
                cornelia.setTarget(enemy);
            }
            case 300 -> {
                SelfTest.check(enemy.getHealth() < 1024.0f, "cornelia hurts the enemy (husk " + enemy.getHealth() + "/1024, its max health cut by Health Shatter)");
                SelfTest.check(cornelia.getTarget() == enemy, "cornelia keeps fighting the enemy");
                SelfTest.check(ownerUnharmed(), "owner beside her target untouched by her swings " + ownerState());
                SelfTest.check(shatterbladeSeen,
                        "she dual-wields the Shatterblade against a big target until its Health Shatter is full (now " + cornelia.getMainHandItem() + ")");
                int stacks = CorneliaWeapons.shatterStacks(enemy);
                net.minecraft.world.effect.MobEffectInstance shatter = enemy.getEffect(dev.obscuria.aquamirae.registry.AquamiraeEffects.HEALTH_SHATTER.get());
                SelfTest.check(stacks >= 2 && stacks % 2 == 0, "her Shatterblade adds Health Shatter 2 stacks per hit (" + stacks + ")");
                // A minute per hit landed, minus the time since that hit.
                SelfTest.check(shatter != null && shatter.getDuration() > (stacks / 2 - 1) * 1200 && shatter.getDuration() <= stacks / 2 * 1200,
                        "it lasts a minute per hit, up to 5 (" + (shatter == null ? 0 : shatter.getDuration() / 20) + " s left after " + stacks / 2 + " hits)");
                // Below 25% health: her rage (levitation and the spin around her) comes next.
                cornelia.setHealth(cornelia.getMaxHealth() * 0.2f);
            }
            case 310 -> {
                SelfTest.check(cornelia.getPhase() == 1 && cornelia.hasEffect(MobEffects.LEVITATION), "cornelia rages below 25% health (phase " + cornelia.getPhase() + ")");
            }
            case 499 -> SelfTest.check(saberSeen, "below half health she switches to the Remnant Saber (her rage heals her back to 90%, and then she puts it away)");
            case 500 -> {
                SelfTest.check(ownerUnharmed(), "owner not spun or hurt by her rage " + ownerState());
                // Out of the fight, then the last stand: Aquamirae's slow regeneration at 20 health or
                // less only runs for one 10-second window.
                enemy.discard();
                cornelia.setTarget(null);
                cornelia.removeEffect(MobEffects.LEVITATION);
                cornelia.setHealth(10.0f);
                lastStandHealth = cornelia.getHealth();
            }
            case 600 -> SelfTest.check(cornelia.isLastStandActive() && cornelia.getHealth() > lastStandHealth,
                    "last stand regenerates (" + lastStandHealth + " -> " + cornelia.getHealth() + ")");
            case 720 -> afterWindowHealth = cornelia.getHealth();
            case 760 -> {
                SelfTest.check(cornelia.isLastStandUsed() && !cornelia.isLastStandActive() && cornelia.getHealth() == afterWindowHealth,
                        "last stand stops after its 10 seconds (" + afterWindowHealth + " -> " + cornelia.getHealth() + ")");
                // Healed first, so the shortened reset below doesn't start a new last stand.
                cornelia.setHealth(cornelia.getMaxHealth());
                ServantConfig.CaptainCorneliaPhaseResetSeconds.set(1);
            }
            case 800 -> {
                SelfTest.check(cornelia.getPhase() == 0 && !cornelia.isLastStandUsed(), "rage and last stand come back out of combat");
                cornelia.kill();
            }
            case 860 -> {
                AABB area = new AABB(CENTER).inflate(16);
                SelfTest.check(!level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(ModItems.CAPTAIN_CORNELIA_SOUL)).isEmpty(), "cornelia drops her soul on death");
                SelfTest.check(level.getEntitiesOfClass(ItemEntity.class, area, item -> !item.getItem().is(ModItems.CAPTAIN_CORNELIA_SOUL)).isEmpty(), "cornelia drops no weapon");
                SelfTest.check(level.getEntitiesOfClass(ExperienceOrb.class, area).isEmpty(), "cornelia drops no experience");
                ServantConfig.CaptainCorneliaPhaseResetSeconds.set(60);
                owner.discard();
            }
            default -> {
            }
        }
    }
}
