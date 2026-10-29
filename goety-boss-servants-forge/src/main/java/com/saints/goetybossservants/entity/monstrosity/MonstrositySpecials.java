package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.client.particles.SlamParticleOption;
import com.Polarice3.Goety.common.entities.util.CameraShake;
import com.Polarice3.Goety.common.entities.util.SummonCircleVariant;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MobUtil;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.registry.ModEffects;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What each material monstrosity does when its summon attack slams the ground (where Goety's
 * Redstone Monstrosity raises Redstone Cubes). Server side only.
 */
public final class MonstrositySpecials {
    private static final double SLAM_RADIUS = 7.0;
    private static final double GILD_RADIUS = 8.0;
    private static final double SPIKE_RADIUS = 14.0;
    private static final double TENTACLE_RADIUS = 14.0;
    /** How many tentacles the Sculk Monstrosity calls up: one beside each enemy, up to the most, and never fewer than the least. */
    private static final int MOST_TENTACLES = 6;
    private static final int LEAST_TENTACLES = 4;
    private static final double BUFF_RADIUS = 16.0;
    /** How long the Diamond Monstrosity's spikes rain down after the ground spikes. */
    public static final int RAIN_TICKS = MathHelper.secondsToTicks(2);

    private MonstrositySpecials() {
    }

    static void perform(MaterialMonstrosity monstrosity) {
        if (!(monstrosity.level() instanceof ServerLevel level)) {
            return;
        }
        switch (monstrosity.material()) {
            case IRON -> {
                if (!summonGolems(level, monstrosity)) {
                    slam(level, monstrosity, SLAM_RADIUS);
                }
            }
            case GOLD -> gild(level, monstrosity);
            case LAPIS -> enchantAllies(level, monstrosity);
            case DIAMOND -> diamondSpikes(level, monstrosity);
            case NETHERITE -> {
                slamEffects(level, monstrosity, SLAM_RADIUS);
                // Calls up its minions when it has none out, otherwise the cataclysm.
                if (!summonMinions(level, monstrosity)) {
                    monstrosity.cataclysm = new NetheriteCataclysm(monstrosity);
                }
            }
            case SCULK -> sculkTentacles(level, monstrosity);
            default -> slam(level, monstrosity, SLAM_RADIUS);
        }
    }

    /**
     * Whether the special would do anything right now, checked before the summon attack starts
     * (Goety starts it whenever its target is within 16 blocks). Iron always has golems to raise
     * until its limit and Lapis always buffs itself; the others need an enemy inside their reach.
     */
    public static boolean worthUsing(MaterialMonstrosity monstrosity) {
        if (!(monstrosity.level() instanceof ServerLevel level)) {
            return false;
        }
        double radius = switch (monstrosity.material()) {
            case IRON -> golemsOut(level, monstrosity) < ServantConfig.IronMonstrosityMaxGolems.get() ? -1.0 : SLAM_RADIUS;
            case LAPIS -> -1.0;
            case GOLD -> GILD_RADIUS;
            case DIAMOND -> SPIKE_RADIUS;
            case NETHERITE -> NetheriteCataclysm.FISSURE_RADIUS;
            case SCULK -> TENTACLE_RADIUS;
            default -> SLAM_RADIUS;
        };
        if (radius < 0.0) {
            return true;
        }
        // (The Sculk one's tentacles go only after its foes, see sculkTentacles.)
        return !(monstrosity.material() == MonstrosityMaterial.SCULK ? foesAround(level, monstrosity, radius) : enemiesAround(level, monstrosity, radius)).isEmpty();
    }

    private static float attackDamage(MaterialMonstrosity monstrosity) {
        return (float) monstrosity.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    static List<LivingEntity> enemiesAround(ServerLevel level, MaterialMonstrosity monstrosity, double radius) {
        Vec3 center = monstrosity.position();
        return level.getEntitiesOfClass(LivingEntity.class, monstrosity.getBoundingBox().inflate(radius, 4.0, radius),
                entity -> monstrosity.canHitWithArea(entity) && horizontalDistance(entity.position(), center) <= radius);
    }

    /** Its foes within {@code radius} (see MaterialMonstrosity.isFoe). */
    static List<LivingEntity> foesAround(ServerLevel level, MaterialMonstrosity monstrosity, double radius) {
        Vec3 center = monstrosity.position();
        return level.getEntitiesOfClass(LivingEntity.class, monstrosity.getBoundingBox().inflate(radius, 4.0, radius),
                entity -> monstrosity.isFoe(entity) && horizontalDistance(entity.position(), center) <= radius);
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.sqrt(Mth.square(a.x - b.x) + Mth.square(a.z - b.z));
    }

    /** The ground shockwave every special shows: Goety's slam ring in the material's colour, debris of its block, a camera shake. */
    static void slamEffects(ServerLevel level, MaterialMonstrosity monstrosity, double radius) {
        MonstrosityMaterial material = monstrosity.material();
        Vec3 pos = monstrosity.position();
        level.sendParticles(new SlamParticleOption(new ColorUtil(material.accentColor), (float) radius * 2.0f, 20), pos.x, pos.y + 0.1, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        BlockState debris = material.fillBlock().defaultBlockState();
        for (int i = 0; i < 48; ++i) {
            double angle = i * Math.PI * 2.0 / 48.0;
            double distance = radius * (0.4 + 0.6 * monstrosity.getRandom().nextDouble());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, debris), pos.x + Math.cos(angle) * distance, pos.y + 0.2, pos.z + Math.sin(angle) * distance,
                    3, 0.2, 0.3, 0.2, 0.1);
        }
        CameraShake.cameraShake(level, pos, (float) radius * 3.0f, 0.3f, 0, 20);
        monstrosity.playSound(ModSounds.REDSTONE_MONSTROSITY_SMASH.get(), 3.0f, 0.4f);
    }

    /** Copper, and Iron at its golem limit: hurts and flings everything around it. */
    static void slam(ServerLevel level, MaterialMonstrosity monstrosity, double radius) {
        slamEffects(level, monstrosity, radius);
        monstrosity.playSound(SoundEvents.GENERIC_EXPLODE, 2.0f, 0.7f);
        float damage = attackDamage(monstrosity) * 0.75f;
        for (LivingEntity target : enemiesAround(level, monstrosity, radius)) {
            if (target.hurt(monstrosity.getServantAttack(), damage)) {
                launch(target, monstrosity.position(), 1.2, 0.6);
            }
        }
    }

    /** Pushes {@code target} away from {@code from} and up, less for knockback-resistant targets. */
    static void launch(LivingEntity target, Vec3 from, double outward, double upward) {
        double scale = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        Vec3 away = target.position().subtract(from).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
        target.push(away.x * outward * scale, upward * scale, away.z * outward * scale);
        target.hurtMarked = true;
    }

    /**
     * Iron: two Iron Golems rise from Goety summon circles beside it. The circles make the
     * monstrosity their owner, exactly as the Redstone Monstrosity's cubes are owned by it.
     * Returns false (no summon) when it already has its maximum out.
     */
    static boolean summonGolems(ServerLevel level, MaterialMonstrosity monstrosity) {
        int out = golemsOut(level, monstrosity);
        int max = ServantConfig.IronMonstrosityMaxGolems.get();
        if (out >= max) {
            return false;
        }
        slamEffects(level, monstrosity, SLAM_RADIUS);
        // Clear of its 4 block wide body: 3 blocks ahead and 4 to either side.
        Vec3 ahead = monstrosity.getHorizontalLookAngle().scale(3.0);
        Vec3 left = MobUtil.getHorizontalLeftLookAngle(monstrosity).scale(4.0);
        double[][] sides = {{monstrosity.getX() + ahead.x + left.x, monstrosity.getZ() + ahead.z + left.z},
                {monstrosity.getX() + ahead.x - left.x, monstrosity.getZ() + ahead.z - left.z}};
        for (int i = 0; i < Math.min(2, max - out); ++i) {
            MonstrosityIronGolem golem = ModRegistry.MONSTROSITY_IRON_GOLEM.get().create(level);
            if (golem == null) {
                continue;
            }
            Vec3 pos = findSpot(level, sides[i][0], monstrosity.getY(), sides[i][1], golem);
            golem.setTrueOwner(monstrosity);
            golem.setHasLifespan(true);
            golem.setLifespan(MathHelper.secondsToTicks(ServantConfig.IronMonstrosityGolemSeconds.get()));
            SummonCircleVariant circle = new SummonCircleVariant(level, pos, golem, monstrosity);
            circle.playSound(ModSounds.DIRT_DEBRIS.get(), 3.0f, 0.4f);
            level.addFreshEntity(circle);
        }
        monstrosity.playSound(SoundEvents.IRON_GOLEM_REPAIR, 3.0f, 0.6f);
        return true;
    }

    private static int golemsOut(ServerLevel level, MaterialMonstrosity monstrosity) {
        return level.getEntitiesOfClass(MonstrosityIronGolem.class, monstrosity.getBoundingBox().inflate(64.0),
                golem -> golem.isAlive() && monstrosity.getUUID().equals(golem.getOwnerId())).size();
    }

    /** The monstrosities the Netherite one may call up as minions (not itself, nor the Sculk one). */
    private static final List<MonstrosityMaterial> MINION_MATERIALS = List.of(MonstrosityMaterial.COPPER, MonstrosityMaterial.IRON,
            MonstrosityMaterial.GOLD, MonstrosityMaterial.LAPIS, MonstrosityMaterial.DIAMOND);

    /**
     * Netherite, when it has no minions out: two other monstrosities (two different ones of
     * MINION_MATERIALS, at random) rise from Goety summon circles beside it, at half size and
     * health, owned by it and gone after a while (see MaterialMonstrosity.isMinion). Returns false
     * (no summon) while any are still out.
     */
    static boolean summonMinions(ServerLevel level, MaterialMonstrosity monstrosity) {
        if (minionsOut(level, monstrosity) > 0) {
            return false;
        }
        // Clear of its 5 block wide body: 3 blocks ahead and 5 to either side.
        Vec3 ahead = monstrosity.getHorizontalLookAngle().scale(3.0);
        Vec3 left = MobUtil.getHorizontalLeftLookAngle(monstrosity).scale(5.0);
        double[][] sides = {{monstrosity.getX() + ahead.x + left.x, monstrosity.getZ() + ahead.z + left.z},
                {monstrosity.getX() + ahead.x - left.x, monstrosity.getZ() + ahead.z - left.z}};
        List<MonstrosityMaterial> materials = new ArrayList<>(MINION_MATERIALS);
        Collections.shuffle(materials, new java.util.Random(monstrosity.getRandom().nextLong()));
        for (int i = 0; i < 2; ++i) {
            MaterialMonstrosity minion = MaterialMonstrosity.create(level, materials.get(i));
            if (minion == null) {
                continue;
            }
            minion.setMinion(true);
            Vec3 pos = findSpot(level, sides[i][0], monstrosity.getY(), sides[i][1], minion);
            minion.setTrueOwner(monstrosity);
            minion.setHasLifespan(true);
            minion.setLifespan(MathHelper.secondsToTicks(ServantConfig.NetheriteMonstrosityMinionSeconds.get()));
            SummonCircleVariant circle = new SummonCircleVariant(level, pos, minion, monstrosity);
            circle.playSound(ModSounds.DIRT_DEBRIS.get(), 3.0f, 0.4f);
            level.addFreshEntity(circle);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.5, pos.z, 40, 1.0, 0.5, 1.0, 0.05);
        }
        monstrosity.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 3.0f, 0.5f);
        monstrosity.playSound(SoundEvents.WITHER_SPAWN, 1.5f, 1.4f);
        return true;
    }

    /** How many of its minions are out. */
    public static int minionsOut(ServerLevel level, MaterialMonstrosity monstrosity) {
        return level.getEntitiesOfClass(MaterialMonstrosity.class, monstrosity.getBoundingBox().inflate(64.0),
                minion -> minion.isAlive() && minion.isMinion() && monstrosity.getUUID().equals(minion.getOwnerId())).size();
    }

    // The spot beside it if the summoned mob fits there, otherwise right in front of it.
    private static Vec3 findSpot(ServerLevel level, double x, double y, double z, LivingEntity summoned) {
        for (int dy = 0; dy <= 3; ++dy) {
            Vec3 pos = new Vec3(x, Math.floor(y) + dy, z);
            if (level.noCollision(summoned, summoned.getDimensions(summoned.getPose()).makeBoundingBox(pos))) {
                return pos;
            }
        }
        return new Vec3(x, y, z);
    }

    /** Gold: everything around it is turned to gold for a few seconds, unable to move or fight. */
    static void gild(ServerLevel level, MaterialMonstrosity monstrosity) {
        slamEffects(level, monstrosity, GILD_RADIUS);
        int ticks = MathHelper.secondsToTicks(ServantConfig.GoldMonstrosityGildSeconds.get());
        float damage = attackDamage(monstrosity) * 0.3f;
        BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
        for (LivingEntity target : enemiesAround(level, monstrosity, GILD_RADIUS)) {
            target.hurt(monstrosity.getServantAttack(), damage);
            if (target.isAlive()) {
                target.addEffect(new MobEffectInstance(ModEffects.GILDED.get(), ticks, 0, false, false, true), monstrosity);
                target.setDeltaMovement(0.0, Math.min(0.0, target.getDeltaMovement().y), 0.0);
                target.hurtMarked = true;
                level.sendParticles(gold, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(), 30,
                        target.getBbWidth() / 2.0, target.getBbHeight() / 3.0, target.getBbWidth() / 2.0, 0.1);
            }
        }
        level.playSound(null, monstrosity.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0f, 0.8f);
    }

    /**
     * Lapis: an enchantment-like buff for every ally around it (owner, its pets and servants). The
     * held book's enchantment at the book's level (a random one of the book's if several),
     * otherwise a random enchantment at level I.
     */
    static void enchantAllies(ServerLevel level, MaterialMonstrosity monstrosity) {
        slamEffects(level, monstrosity, SLAM_RADIUS);
        List<LapisBuffs.Buff> held = LapisBuffs.fromBook(monstrosity.getHeldBook());
        LapisBuffs.Buff buff = held.isEmpty() ? LapisBuffs.random(monstrosity.getRandom()) : held.get(monstrosity.getRandom().nextInt(held.size()));
        int ticks = MathHelper.secondsToTicks(ServantConfig.LapisMonstrosityBuffSeconds.get());
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, monstrosity.getBoundingBox().inflate(BUFF_RADIUS),
                entity -> entity.isAlive() && monstrosity.isAlly(entity))) {
            ally.addEffect(new MobEffectInstance(buff.effect().get(), ticks, buff.amplifier()), monstrosity);
            level.sendParticles(ParticleTypes.ENCHANT, ally.getX(), ally.getY() + ally.getBbHeight() + 0.5, ally.getZ(), 40,
                    ally.getBbWidth() / 2.0, 0.5, ally.getBbWidth() / 2.0, 1.0);
        }
        level.playSound(null, monstrosity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 3.0f, 0.8f);
        level.playSound(null, monstrosity.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.5f, 0.6f);
    }

    /**
     * Diamond: spikes burst up under every enemy in a wide area (and at random spots around),
     * launching them, then diamond spikes rain from the sky for two seconds (see diamondRain).
     */
    static void diamondSpikes(ServerLevel level, MaterialMonstrosity monstrosity) {
        slamEffects(level, monstrosity, SPIKE_RADIUS);
        float damage = attackDamage(monstrosity) * 0.6f;
        for (LivingEntity target : enemiesAround(level, monstrosity, SPIKE_RADIUS)) {
            DiamondSpike.spawnGround(level, monstrosity, target.getX(), target.getZ(), target.getY(), damage);
        }
        for (int i = 0; i < 16; ++i) {
            double angle = monstrosity.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 4.0 + monstrosity.getRandom().nextDouble() * (SPIKE_RADIUS - 4.0);
            DiamondSpike.spawnGround(level, monstrosity, monstrosity.getX() + Math.cos(angle) * distance, monstrosity.getZ() + Math.sin(angle) * distance, monstrosity.getY(), damage);
        }
        monstrosity.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 4.0f, 0.5f);
        monstrosity.rainTicks = RAIN_TICKS;
    }

    /**
     * Sculk: sculk tentacles burst out of the ground beside its foes (one beside each, its target
     * first, then the nearest, and at least a few), each slamming down on them twice, leaving lines of
     * sculk mire, before sinking back (see SculkTentacle).
     */
    static void sculkTentacles(ServerLevel level, MaterialMonstrosity monstrosity) {
        slamEffects(level, monstrosity, SLAM_RADIUS);
        float damage = attackDamage(monstrosity) * 0.5f;
        // Its target first, then the nearest.
        List<LivingEntity> enemies = new ArrayList<>(foesAround(level, monstrosity, TENTACLE_RADIUS));
        enemies.sort(java.util.Comparator.<LivingEntity>comparingInt(enemy -> enemy == monstrosity.getTarget() ? 0 : 1).thenComparingDouble(monstrosity::distanceToSqr));
        int count = Math.max(LEAST_TENTACLES, Math.min(MOST_TENTACLES, enemies.size()));
        for (int i = 0; i < count; ++i) {
            // Beside an enemy (round them again for the extra ones), or ahead of it with none about.
            Vec3 at = enemies.isEmpty() ? monstrosity.position().add(monstrosity.getHorizontalLookAngle().scale(6.0)) : enemies.get(i % enemies.size()).position();
            double angle = monstrosity.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 2.5 + monstrosity.getRandom().nextDouble();
            Vec3 pos = at.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            float yaw = (float) (Mth.atan2(-(at.x - pos.x), at.z - pos.z) * Mth.RAD_TO_DEG);
            SculkTentacle.spawn(level, monstrosity, pos, yaw, damage);
        }
        monstrosity.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0f, 0.5f);
        monstrosity.playSound(SoundEvents.WARDEN_EMERGE, 2.0f, 1.2f);
    }

    /** Called every tick while the Diamond Monstrosity's rain lasts: drops spikes over its enemies and the area. */
    static void diamondRain(MaterialMonstrosity monstrosity) {
        if (!(monstrosity.level() instanceof ServerLevel level) || monstrosity.rainTicks % 2 != 0) {
            return;
        }
        float damage = attackDamage(monstrosity) * 0.25f;
        List<LivingEntity> enemies = enemiesAround(level, monstrosity, SPIKE_RADIUS);
        for (int i = 0; i < 3; ++i) {
            double x;
            double z;
            if (!enemies.isEmpty() && i < 2) {
                LivingEntity target = enemies.get(monstrosity.getRandom().nextInt(enemies.size()));
                x = target.getX() + (monstrosity.getRandom().nextDouble() - 0.5) * 3.0;
                z = target.getZ() + (monstrosity.getRandom().nextDouble() - 0.5) * 3.0;
            } else {
                double angle = monstrosity.getRandom().nextDouble() * Math.PI * 2.0;
                double distance = 3.0 + monstrosity.getRandom().nextDouble() * (SPIKE_RADIUS - 3.0);
                x = monstrosity.getX() + Math.cos(angle) * distance;
                z = monstrosity.getZ() + Math.sin(angle) * distance;
            }
            DiamondSpike.spawnFalling(level, monstrosity, x, monstrosity.getY() + 12.0 + monstrosity.getRandom().nextInt(4), z, damage);
        }
    }
}
