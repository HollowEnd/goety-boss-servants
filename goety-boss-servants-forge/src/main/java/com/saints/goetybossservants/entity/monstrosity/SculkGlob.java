package com.saints.goetybossservants.entity.monstrosity;

import com.Polarice3.Goety.client.particles.CircleExplodeParticleOption;
import com.Polarice3.Goety.client.particles.DustCloudParticleOption;
import com.Polarice3.Goety.client.particles.SphereExplodeParticleOption;
import com.Polarice3.Goety.common.entities.projectiles.ScatterBomb;
import com.Polarice3.Goety.utils.BlockFinder;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.ServerParticleUtil;
import com.Polarice3.Goety.utils.SpellExplosion;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The Sculk Monstrosity's belch: Goety's scatter bomb, sculk teal instead of fiery yellow, that
 * bursts the same way (and decays what it hurts, as every Sculk Monstrosity attack does) and leaves
 * a pool of sculk mire where it lands.
 */
public class SculkGlob extends ScatterBomb {
    /** Sculk's glowing teal. */
    public static final int COLOR = 0x29DFEB;
    private static final int DARK = 0x0A3F4A;
    private static final double POOL_RADIUS = 2.5;

    public SculkGlob(EntityType<? extends SculkGlob> type, Level level) {
        super(type, level);
    }

    /** A glob coughed up by {@code owner}, from where Goety's scatter bombs come out of its mouth. */
    public static SculkGlob create(MaterialMonstrosity owner) {
        SculkGlob glob = new SculkGlob(ModRegistry.SCULK_GLOB.get(), owner.level());
        glob.setOwner(owner);
        glob.moveTo(owner.getX(), owner.getY() + 4.5, owner.getZ());
        return glob;
    }

    // Goety's scatter bomb burst, in sculk's colours, and the pool of mire.
    @Override
    protected void onHit(@NotNull HitResult result) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.playSound(SoundEvents.GENERIC_EXPLODE, 4.0f, (1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f) * 0.7f);
        this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 3.0f, 0.6f);
        Entity owner = this.getOwner();
        float damage = owner instanceof MaterialMonstrosity monstrosity ? (float) monstrosity.getAttributeValue(Attributes.ATTACK_DAMAGE) / 2.0f : 12.5f;
        new SpellExplosion(level, this, this.damageSources().explosion(this, owner), this.getX(), this.getY(), this.getZ(), 4.5f, damage) {
            @Override
            public void explodeHurt(Entity target, DamageSource damageSource, double x, double y, double z, double seen, float actualDamage) {
                super.explodeHurt(target, damageSource, x, y, z, seen, actualDamage);
                target.invulnerableTime = 15;
            }
        };
        double ground = BlockFinder.moveDownToGround(this);
        ColorUtil color = new ColorUtil(COLOR);
        ServerParticleUtil.addParticlesAroundSelf(level, ParticleTypes.SCULK_SOUL, this);
        ServerParticleUtil.addAuraParticles(level, ParticleTypes.SCULK_CHARGE_POP, this, 2.0f);
        level.sendParticles(new CircleExplodeParticleOption(color.red, color.green, color.blue, 4.5f, 1), this.getX(), ground, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(new SphereExplodeParticleOption(color.red, color.green, color.blue, 4.5f, 1), this.getX(), ground + 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        ServerParticleUtil.circularParticles(level, new DustCloudParticleOption(Vec3.fromRGB24(DARK).toVector3f(), 1.0f), this.getX(), this.getY() + 0.25, this.getZ(), 0.0, 0.14, 0.0, 1.0f);
        if (owner instanceof MaterialMonstrosity monstrosity && monstrosity.isAlive()) {
            SculkMire.pool(level, monstrosity, new Vec3(this.getX(), ground, this.getZ()), POOL_RADIUS);
        }
        this.discard();
    }
}
