package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.utils.MathHelper;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import com.saints.goetybossservants.entity.servant.BossServant;
import com.saints.goetybossservants.entity.servant.ServantMinions;
import com.saints.goetybossservants.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Sculk Monstrosity's gimmick. Every one of its attacks adds stacks of Sculk Decay to the foes
 * it hurts (the sculk it leaves on the ground adds one a second); each stack deals damage once a
 * second, ignoring armor, until it wears off. Whatever dies with it on rises again as a sculk zombie
 * fighting for the monstrosity that decayed it (see SculkZombie). Server side.
 */
public final class SculkDecay {
    /** Its damage: no attacker (so no knockback and no retaliation), ignoring armor (see the damage_type tags). */
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GoetyBossServants.MOD_ID, "sculk_decay"));
    // Bosses never rise: those in either loader's boss tag (mods tag one or the other), or in Jerotes'
    // (Meowhem's), and those whose mods tag none (this mod makes servants of them too), and this mod's
    // Mira Monstrosity (a servant, but not a BossServant).
    private static final List<TagKey<EntityType<?>>> BOSS_TAGS = List.of(bossTag("forge"), bossTag("c"), bossTag("jerotes"));
    private static final Set<ResourceLocation> UNTAGGED_BOSSES = Set.of(new ResourceLocation("aquamirae", "captain_cornelia"), new ResourceLocation("knightquest", "netherman"),
            new ResourceLocation(GoetyBossServants.MOD_ID, "mira_monstrosity"));
    // When each decaying entity takes its damage next (game time).
    private static final Map<LivingEntity, Long> NEXT_HIT = new WeakHashMap<>();
    private static final int INTERVAL = 20;

    private SculkDecay() {
    }

    static MobEffect effect() {
        return ModEffects.SCULK_DECAY.get();
    }

    /** Adds {@code stacks} stacks of Sculk Decay to {@code target} (up to the most allowed), and makes it last its full time again. */
    public static void apply(LivingEntity target, MaterialMonstrosity source, int stacks) {
        if (target.level().isClientSide || !target.isAlive() || stacks <= 0 || target instanceof SculkZombie || RisenMobs.isRisen(target)) {
            return;
        }
        MobEffectInstance old = target.getEffect(effect());
        int max = ServantConfig.SculkDecayMaxStacks.get();
        int amplifier = Math.min(max - 1, (old == null ? -1 : old.getAmplifier()) + stacks);
        target.addEffect(new MobEffectInstance(effect(), MathHelper.secondsToTicks(ServantConfig.SculkDecaySeconds.get()), amplifier), source);
        DecayedBy.set(target, source.getUUID());
    }

    /** How many stacks {@code entity} carries (0 for none). */
    public static int stacks(LivingEntity entity) {
        MobEffectInstance instance = entity.getEffect(effect());
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    /**
     * Every tick while decaying. Once a second, however often stacks are added, it takes its damage
     * and sculk bursts off it; but never while it can't be hurt after a real hit (the decay waits
     * that out), and it leaves none of that time itself, so it never takes anything off real hits.
     */
    public static void tick(ServerLevel level, LivingEntity entity, int amplifier) {
        long now = level.getGameTime();
        Long due = NEXT_HIT.get(entity);
        // Its first second (or the first since it last decayed a while ago).
        if (due == null || now > due + 3L * INTERVAL) {
            NEXT_HIT.put(entity, now + INTERVAL);
            return;
        }
        if (now < due || entity.invulnerableTime > 10) {
            return;
        }
        NEXT_HIT.put(entity, now + INTERVAL);
        float damage = (float) (ServantConfig.SculkDecayDamage.get() * (amplifier + 1));
        if (damage > 0.0f) {
            int invulnerable = entity.invulnerableTime;
            entity.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)), damage);
            // (Unless something its death or hurt set off gave it a time of its own, as a revival does.)
            if (entity.invulnerableTime == 20) {
                entity.invulnerableTime = invulnerable;
            }
        }
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 3 + amplifier,
                entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.02);
    }

    /**
     * One of a Sculk Monstrosity's attacks hurt {@code target} for {@code amount} (after any shield):
     * if it is a foe of the monstrosity's, it decays (not from the decay's own damage, nor a hit a
     * shield took all of).
     */
    public static void onHit(LivingEntity target, DamageSource source, float amount) {
        if (amount > 0.0f && source.getEntity() instanceof MaterialMonstrosity monstrosity && monstrosity.material() == MonstrosityMaterial.SCULK
                && !source.is(DAMAGE) && monstrosity != target && monstrosity.isFoe(target)) {
            apply(target, monstrosity, ServantConfig.SculkDecayStacksPerHit.get());
        }
    }

    /**
     * {@code dead} is dying (before it drops anything, so it keeps its gear): if it was decaying, it
     * rises for the monstrosity that decayed it, as itself if it can (see RisenMobs), or else as a
     * sculk zombie that looks like it.
     */
    public static void onDeath(LivingEntity dead) {
        if (!(dead.level() instanceof ServerLevel level) || !dead.hasEffect(effect())) {
            return;
        }
        UUID by = DecayedBy.take(dead);
        if (by == null || !canRise(dead)) {
            return;
        }
        Entity entity = level.getEntity(by);
        if (entity instanceof MaterialMonstrosity monstrosity && monstrosity.isAlive()
                && RisenMobs.count(level, monstrosity) < ServantConfig.SculkMaxZombies.get()) {
            // (As a sculk zombie, too, if it couldn't rise as itself after all.)
            if (!RisenMobs.risesAsItself(dead) || !RisenMobs.riseReal(level, monstrosity, (Mob) dead)) {
                SculkZombie.rise(level, monstrosity, dead);
            }
        }
    }

    // Mobs only (not players, armor stands, what has already risen, monstrosities, boss servants or their
    // minions), and no bosses.
    private static boolean canRise(LivingEntity dead) {
        return dead instanceof Mob && !(dead instanceof Player) && !(dead instanceof ArmorStand) && !(dead instanceof SculkZombie)
                && !RisenMobs.isRisen(dead) && !ServantMinions.isMinion(dead)
                && !(dead instanceof MaterialMonstrosity) && !(dead instanceof BossServant) && !(dead instanceof EnderDragon) && !(dead instanceof WitherBoss)
                && BOSS_TAGS.stream().noneMatch(dead.getType()::is) && !UNTAGGED_BOSSES.contains(EntityType.getKey(dead.getType()));
    }

    private static TagKey<EntityType<?>> bossTag(String namespace) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(namespace, "bosses"));
    }
}
