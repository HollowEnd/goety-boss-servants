package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.ally.golem.RedstoneMonstrosity;
import com.Polarice3.Goety.common.items.ModItems;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.construct.MonstrosityMoldShape;
import com.saints.goetybossservants.effect.LapisBuffEffects;
import com.saints.goetybossservants.entity.monstrosity.DiamondSpike;
import com.saints.goetybossservants.entity.monstrosity.LapisBuffs;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityIronGolem;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.entity.monstrosity.MonstrositySpecials;
import com.saints.goetybossservants.entity.monstrosity.NetheriteRift;
import com.saints.goetybossservants.entity.monstrosity.SculkDecay;
import com.saints.goetybossservants.entity.monstrosity.SculkGlob;
import com.saints.goetybossservants.entity.monstrosity.SculkMire;
import com.saints.goetybossservants.entity.monstrosity.SculkTentacle;
import com.saints.goetybossservants.entity.monstrosity.SculkZombie;
import com.saints.goetybossservants.item.MaterialMonstrosityHeadItem;
import com.saints.goetybossservants.registry.ModEffects;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.FakePlayer;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * The seven material monstrosities (see SelfTest): molds, limits, stats, each special attack,
 * friendly fire, the Lapis book, the Gold stun's client sync, death, head revive and spawn eggs.
 * Steps are ticks after START.
 */
class MonstrosityChecks {
    static final int START = NethermanWardenChecks.START + NethermanWardenChecks.LENGTH;
    static final int LENGTH = 720;
    private static final Map<MonstrosityMaterial, MaterialMonstrosity> summoned = new EnumMap<>(MonstrosityMaterial.class);
    private static final Map<MonstrosityMaterial, Mob> targets = new EnumMap<>(MonstrosityMaterial.class);
    private static final Map<MonstrosityMaterial, Float> targetHealth = new EnumMap<>(MonstrosityMaterial.class);
    private static final Map<MonstrosityMaterial, MaterialMonstrosity> showcased = new EnumMap<>(MonstrosityMaterial.class);
    private static RedstoneMonstrosity original;
    private static Wolf goldPet;
    private static Wolf lapisPet;
    private static Wolf copperPet;
    private static LivingEntity expiringGolem;
    private static LivingEntity expiringMinion;
    private static float sculkTargetHealth;
    private static List<SculkZombie> sculkZombies = List.of();
    // A cow near the Sculk one's enemy (a bystander, not a foe), and a husk decayed every tick.
    private static Mob sculkBystander;
    private static Mob calmPiglin;
    private static Mob decayedEveryTick;
    private static float decayedEveryTickHealth;
    private static ServerLevel level;
    private static FakePlayer player;

    private static BlockPos core(MonstrosityMaterial material) {
        return new BlockPos(-150 + material.ordinal() * 48, 120, -90);
    }

    static void step(ServerLevel serverLevel, FakePlayer fakePlayer, int t) {
        level = serverLevel;
        player = fakePlayer;
        // A stack of Sculk Decay every tick for two seconds: it still hurts only once a second.
        if (t > 208 && t < 250 && decayedEveryTick != null) {
            SculkDecay.apply(decayedEveryTick, summoned.get(MonstrosityMaterial.SCULK), 1);
        }
        switch (t) {
            case 0 -> {
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    ChunkPos chunk = new ChunkPos(core(material));
                    level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 3, chunk);
                }
                // Goety's own mold (redstone fill) and the Mira mold (terracotta) are not material molds.
                BlockPos test = core(MonstrosityMaterial.COPPER).offset(0, 0, 60);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, new ChunkPos(test), 3, new ChunkPos(test));
                MonstrosityMoldShape.place(level, test, Blocks.LAVA.defaultBlockState(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "redstone-filled mold is left to Goety");
                MonstrosityMoldShape.place(level, test, Blocks.LAVA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "terracotta (Mira) mold is not a material mold");
                // The Sculk one's mold is its own: lava and sculk blocks no longer make it, and every part counts.
                MonstrosityMoldShape.place(level, test, Blocks.LAVA.defaultBlockState(), Blocks.SCULK.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "lava and sculk mold no longer makes the sculk one");
                MaterialMonstrosityMold.placeMold(level, test, MonstrosityMaterial.SCULK);
                level.setBlockAndUpdate(test, ModBlocks.REINFORCED_REDSTONE_BLOCK.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "sculk mold needs a Warden Core for its core");
                MaterialMonstrosityMold.placeMold(level, test, MonstrosityMaterial.SCULK);
                level.setBlockAndUpdate(test.offset(0, 0, -1), Blocks.SCULK_CATALYST.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "sculk mold needs its four shriekers round the core");
                MaterialMonstrosityMold.placeMold(level, test, MonstrosityMaterial.SCULK);
                level.setBlockAndUpdate(test.offset(2, 0, 2), Blocks.SCULK.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "sculk mold needs sculk catalysts for its fill");
                MaterialMonstrosityMold.placeMold(level, test, MonstrosityMaterial.SCULK);
                level.setBlockAndUpdate(test.offset(MonstrosityMoldShape.RING_LOCATIONS.get(40)), Blocks.LAVA.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, test) == null, "sculk mold needs sculk sensors all round");
                MaterialMonstrosityMold.placeMold(level, test, MonstrosityMaterial.SCULK);
                MonstrosityMoldShape.RING_LOCATIONS.stream().limit(20).forEach(offset -> level.setBlockAndUpdate(test.offset(offset), Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState()));
                check(MaterialMonstrosityMold.findMaterial(level, test) == MonstrosityMaterial.SCULK, "sculk mold accepts calibrated sculk sensors");
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    MaterialMonstrosityMold.placeMold(level, core(material), material);
                    check(MaterialMonstrosityMold.findMaterial(level, core(material)) == material, material + " mold detected");
                }
                // Any copper block counts, oxidized or waxed.
                level.setBlockAndUpdate(core(MonstrosityMaterial.COPPER).offset(1, 0, 1), Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState());
                check(MaterialMonstrosityMold.findMaterial(level, core(MonstrosityMaterial.COPPER)) == MonstrosityMaterial.COPPER, "copper mold accepts mixed copper blocks");
            }
            // A few ticks for the forced chunks to load.
            case 5 -> {
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    ItemStack stack = new ItemStack(ModItems.ANIMATION_CORE);
                    MaterialMonstrosityMold.spawnServant(level, core(material), material, player, stack);
                    List<MaterialMonstrosity> found = level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(core(material)).inflate(4));
                    check(found.size() == 1, material + " monstrosity built from its mold (" + found.size() + ")");
                    check(stack.isEmpty() && level.getBlockState(core(material)).isAir(), material + " core and animation core used up");
                    check(level.getBlockState(core(material).offset(1, 0, 1)).isAir(), material + " fill used up");
                    if (!found.isEmpty()) {
                        MaterialMonstrosity monstrosity = found.get(0);
                        summoned.put(material, monstrosity);
                        check(monstrosity.material() == material, material + " entity has its material");
                        check(player.getUUID().equals(monstrosity.getOwnerId()), material + " owned by its builder");
                        check(monstrosity.getMaxHealth() == (float) material.defaultHealth
                                && monstrosity.getAttributeValue(Attributes.ATTACK_DAMAGE) == material.defaultDamage, material + " stats from the config");
                        check(!monstrosity.isHostile(), material + " is never hostile");
                    }
                }
                // The Sculk one's shriekers and sensors are used up with its fill and core; the bricks and diamond molds stay.
                BlockPos sculk = core(MonstrosityMaterial.SCULK);
                check(level.getBlockState(sculk.offset(1, 0, 0)).isAir() && level.getBlockState(sculk.offset(MonstrosityMoldShape.RING_LOCATIONS.get(0))).isAir()
                        && level.getBlockState(sculk.offset(MonstrosityMoldShape.RING_LOCATIONS.get(50))).isAir(), "sculk mold's shriekers and sensors used up");
                check(level.getBlockState(sculk.offset(MonstrosityMoldShape.DIAMOND_LOCATIONS.get(0))).is(ModBlocks.DIAMOND_MOLD_BLOCK)
                        && level.getBlockState(sculk.offset(0, -1, 0)).is(Blocks.STONE_BRICKS), "sculk mold's bricks and diamond molds stay");
                // Limits are per material: a second copper is fine, a third isn't.
                BlockPos copper = core(MonstrosityMaterial.COPPER);
                MaterialMonstrosityMold.placeMold(level, copper.offset(0, 0, 30), MonstrosityMaterial.COPPER);
                ItemStack second = new ItemStack(ModItems.ANIMATION_CORE);
                MaterialMonstrosityMold.spawnServant(level, copper.offset(0, 0, 30), MonstrosityMaterial.COPPER, player, second);
                check(second.isEmpty(), "second copper monstrosity allowed");
                MaterialMonstrosityMold.placeMold(level, copper.offset(0, 0, 30), MonstrosityMaterial.COPPER);
                ItemStack third = new ItemStack(ModItems.ANIMATION_CORE);
                MaterialMonstrosityMold.spawnServant(level, copper.offset(0, 0, 30), MonstrosityMaterial.COPPER, player, third);
                check(!third.isEmpty(), "third copper monstrosity refused by the per-material limit");
                level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(copper.offset(0, 0, 30)).inflate(4)).forEach(m -> m.discard());

                // Spawn egg: the monstrosity belongs to whoever used it.
                BlockPos eggPos = copper.offset(0, -1, 90);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, new ChunkPos(eggPos), 2, new ChunkPos(eggPos));
                level.setBlockAndUpdate(eggPos, Blocks.STONE.defaultBlockState());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.MONSTROSITY_SPAWN_EGGS.get(MonstrosityMaterial.SCULK)));
                ModRegistry.MONSTROSITY_SPAWN_EGGS.get(MonstrosityMaterial.SCULK).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(eggPos), Direction.UP, eggPos, false)));
                List<MaterialMonstrosity> hatched = level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(eggPos).inflate(4));
                check(hatched.size() == 1 && hatched.get(0).material() == MonstrosityMaterial.SCULK, "spawn egg spawns its monstrosity");
                check(!hatched.isEmpty() && player.getUUID().equals(hatched.get(0).getOwnerId()), "spawn egg's monstrosity belongs to the user");
                hatched.forEach(m -> m.discard());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
            case 6 -> {
                // No AI: the specials are started by hand below, not by the monstrosities picking them.
                summoned.values().forEach(m -> m.setNoAi(true));
                for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                    targets.put(material, dummy(EntityType.HUSK, core(material).offset(5, 0, 0)));
                }
                goldPet = pet(core(MonstrosityMaterial.GOLD).offset(-4, 0, 0));
                lapisPet = pet(core(MonstrosityMaterial.LAPIS).offset(-4, 0, 0));
                copperPet = pet(core(MonstrosityMaterial.COPPER).offset(-4, 0, 0));
                sculkBystander = dummy(EntityType.COW, core(MonstrosityMaterial.SCULK).offset(5, 0, 8));
                calmPiglin = dummy(EntityType.ZOMBIFIED_PIGLIN, core(MonstrosityMaterial.SCULK).offset(5, 0, -8));
                // Ground round the Sculk one, for its globs to burst on and its tentacles and mire to lie on.
                for (BlockPos floor : BlockPos.betweenClosed(core(MonstrosityMaterial.SCULK).offset(-14, -1, -12), core(MonstrosityMaterial.SCULK).offset(16, -1, 12))) {
                    if (level.getBlockState(floor).isAir()) {
                        level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                    }
                }
            }
            // Building one plays Goety's 3.25 second power-up first.
            case 100 -> {
                // Only worth starting with an enemy in reach (Iron and Lapis always): a target 12 blocks
                // off is out of the Gold one's reach, in the Diamond one's.
                summoned.forEach((material, m) -> check(MonstrositySpecials.worthUsing(m), material + " special worth using with an enemy beside it"));
                for (MonstrosityMaterial material : List.of(MonstrosityMaterial.GOLD, MonstrosityMaterial.DIAMOND, MonstrosityMaterial.COPPER)) {
                    Mob target = targets.get(material);
                    target.teleportTo(target.getX() + 7.0, target.getY(), target.getZ());
                    check(MonstrositySpecials.worthUsing(summoned.get(material)) == (material == MonstrosityMaterial.DIAMOND),
                            material + " special " + (material == MonstrosityMaterial.DIAMOND ? "still" : "not") + " worth using with the enemy 12 blocks off");
                    target.teleportTo(target.getX() - 7.0, target.getY(), target.getZ());
                }
                check(MonstrositySpecials.worthUsing(summoned.get(MonstrosityMaterial.LAPIS)), "lapis special always worth using");
                summoned.values().forEach(MaterialMonstrosity::forceSpecial);
                targets.forEach((material, target) -> targetHealth.put(material, target.getHealth()));
            }
            case 125 -> {
                Mob slammed = targets.get(MonstrosityMaterial.COPPER);
                check(slammed.getHealth() < targetHealth.get(MonstrosityMaterial.COPPER) - 5.0f, "copper slam hurts the enemy (" + targetHealth.get(MonstrosityMaterial.COPPER) + " -> " + slammed.getHealth() + ")");
                check(slammed.getDeltaMovement().y > 0.1, "copper slam launches the enemy");
                List<SculkTentacle> tentacles = level.getEntitiesOfClass(SculkTentacle.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(20));
                check(tentacles.size() == 4, "sculk calls up four tentacles round its one enemy (" + tentacles.size() + ")");
                check(tentacles.stream().allMatch(tentacle -> tentacle.distanceTo(targets.get(MonstrosityMaterial.SCULK)) < 4.0), "the tentacles rise beside the enemy");
                check(copperPet.getHealth() == copperPet.getMaxHealth() && copperPet.getDeltaMovement().y <= 0.0, "slam spares the owner's pet (" + copperPet.getHealth() + ", " + copperPet.getDeltaMovement() + ")");
                Mob gilded = targets.get(MonstrosityMaterial.GOLD);
                check(gilded.hasEffect(ModEffects.GILDED), "gold turns the enemy to gold");
                check(gilded.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0, "gilded enemy can't move");
                LivingEntity clientGilded = ClientSelfTest.clientCopy(gilded.getId());
                check(clientGilded != null && clientGilded.hasEffect(ModEffects.GILDED), "gilded status reaches the client (for the statue look)");
                check(!goldPet.hasEffect(ModEffects.GILDED), "gold spares the owner's pet");
                check(lapisPet.getActiveEffects().stream().anyMatch(effect -> effect.getEffect().isBeneficial()), "lapis buffs the owner's pet: " + lapisPet.getActiveEffects());
                check(targets.get(MonstrosityMaterial.LAPIS).getActiveEffects().isEmpty(), "lapis doesn't buff enemies");
                check(!level.getEntitiesOfClass(DiamondSpike.class, new AABB(core(MonstrosityMaterial.DIAMOND)).inflate(20)).isEmpty(), "diamond raises spikes");
                Mob diamondTarget = targets.get(MonstrosityMaterial.DIAMOND);
                check(diamondTarget.getHealth() < targetHealth.get(MonstrosityMaterial.DIAMOND) - 5.0f, "diamond spike hurts the enemy");
                check(diamondTarget.getDeltaMovement().y > 0.5, "diamond spike knocks the enemy up");
                check(summoned.get(MonstrosityMaterial.DIAMOND).rainTicks > 0, "diamond rain starts");
                targetHealth.put(MonstrosityMaterial.DIAMOND, diamondTarget.getHealth());
            }
            // The Netherite one's second special (its minions are out): its fissures spread out, then it
            // leaps at the enemy and lands on it.
            case 234 -> {
                MaterialMonstrosity netherite = summoned.get(MonstrosityMaterial.NETHERITE);
                check(netherite.getY() > core(MonstrosityMaterial.NETHERITE).getY() + 5.0, "netherite leaps (height " + (netherite.getY() - core(MonstrosityMaterial.NETHERITE).getY()) + ")");
            }
            case 250 -> {
                MaterialMonstrosity netherite = summoned.get(MonstrosityMaterial.NETHERITE);
                Mob crushed = targets.get(MonstrosityMaterial.NETHERITE);
                // Horizontally: it may come down on top of leftover mold blocks (and has no AI here to step off).
                check(netherite.position().subtract(crushed.position()).horizontalDistance() < 1.5,
                        "netherite lands on the enemy (" + netherite.position().subtract(crushed.position()) + ")");
                check(crushed.hasEffect(ModEffects.SUNDERED), "netherite landing sunders the enemy");
                check(crushed.getAttributeValue(Attributes.ARMOR) == crushed.getAttributeBaseValue(Attributes.ARMOR) * 0.5, "sundered enemy has half armor (" + crushed.getAttributeValue(Attributes.ARMOR) + ")");
                check(crushed.isOnFire(), "netherite sets the enemy on fire");
                check(level.getEntitiesOfClass(NetheriteRift.class, new AABB(core(MonstrosityMaterial.NETHERITE)).inflate(24)).size() == 13, "netherite leaves 12 fissures and a crater");
                // Minions drop nothing when they die.
                minions().get(0).kill();
                expiringMinion = minions().get(1);
                minions().get(1).setLifespan(3);
                // Two hits of 10 stacks in the two seconds (not one a tick, as each new stack restarts its time).
                float lost = decayedEveryTickHealth - decayedEveryTick.getHealth();
                check(lost >= 15.0f && lost <= 30.0f, "sculk decay hurts once a second however often it is added (" + lost + " in 2 s at 10 stacks)");
            }
            // Away from where its tentacles slam (they're gone by now).
            case 208 -> {
                decayedEveryTick = dummy(EntityType.HUSK, core(MonstrosityMaterial.SCULK).offset(-6, 0, 8));
                SculkDecay.apply(decayedEveryTick, summoned.get(MonstrosityMaterial.SCULK), 10);
                decayedEveryTickHealth = decayedEveryTick.getHealth();
            }
            case 360 -> {
                check(expiringMinion != null && (expiringMinion.isDeadOrDying() || expiringMinion.isRemoved()), "netherite minion dies when its time is up");
                check(level.getEntitiesOfClass(ItemEntity.class, new AABB(core(MonstrosityMaterial.NETHERITE)).inflate(24)).isEmpty(), "netherite minions drop nothing");
            }
            case 160 -> {
                MaterialMonstrosity iron = summoned.get(MonstrosityMaterial.IRON);
                List<MonstrosityIronGolem> golems = level.getEntitiesOfClass(MonstrosityIronGolem.class, new AABB(core(MonstrosityMaterial.IRON)).inflate(24));
                check(golems.size() == 2, "iron summons two iron golems (" + golems.size() + ") at " + golems.stream().map(golem -> golem.position().subtract(iron.position()).toString()).toList() + ", monstrosity yaw " + iron.getYRot());
                check(golems.stream().allMatch(golem -> iron.getUUID().equals(golem.getOwnerId())), "the golems are owned by the monstrosity");
                check(golems.stream().allMatch(golem -> golem.hasLifespan()), "the golems are temporary");
                // When their time is up they die outright.
                if (!golems.isEmpty()) {
                    expiringGolem = golems.get(0);
                    golems.get(0).setLifespan(3);
                }
                Mob diamondTarget = targets.get(MonstrosityMaterial.DIAMOND);
                check(diamondTarget.getHealth() < targetHealth.get(MonstrosityMaterial.DIAMOND), "raining diamond spikes hurt the enemy");
                check(level.getEntitiesOfClass(LivingEntity.class, new AABB(core(MonstrosityMaterial.GOLD)).inflate(20)).stream()
                        .noneMatch(entity -> entity instanceof MaterialMonstrosity && entity.hasEffect(ModEffects.GILDED)), "gold doesn't gild itself");

                // Lapis book: only enchantments with a buff count.
                ItemStack silk = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SILK_TOUCH, 1));
                check(LapisBuffs.usableEnchantments(silk).isEmpty(), "silk touch book can't become a buff");
                ItemStack protection = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION, 4));
                check(LapisBuffs.usableEnchantments(protection).size() == 1, "protection book becomes a buff");
                MaterialMonstrosity lapis = summoned.get(MonstrosityMaterial.LAPIS);
                lapis.setHeldBook(protection);
                lapisPet.removeAllEffects();
                lapis.forceSpecial();
            }
            // The Netherite one's first special (no minions out yet) calls up two half-size monstrosities.
            case 189 -> {
                MaterialMonstrosity netherite = summoned.get(MonstrosityMaterial.NETHERITE);
                List<MaterialMonstrosity> minions = minions();
                check(minions.size() == 2 && minions.get(0).material() != minions.get(1).material(), "netherite calls up two different monstrosities: " + minions.stream().map(MaterialMonstrosity::material).toList());
                check(minions.stream().noneMatch(m -> m.material() == MonstrosityMaterial.NETHERITE || m.material() == MonstrosityMaterial.SCULK), "no netherite or sculk minions");
                check(minions.stream().allMatch(m -> netherite.getUUID().equals(m.getOwnerId()) && m.hasLifespan()), "minions are the netherite one's, and temporary");
                check(minions.stream().allMatch(m -> m.getMaxHealth() == (float) m.material().defaultHealth / 2.0f && m.getBbWidth() == 2.0f), "minions have half health and size: "
                        + minions.stream().map(m -> m.getMaxHealth() + "/" + m.getBbWidth()).toList());
                check(minions.stream().allMatch(m -> m.isAlly(player)), "minions count their master's owner as an ally");
                check(netherite.getBbWidth() == 4.8f, "netherite is 1.2 times bigger");
                targetHealth.put(MonstrosityMaterial.NETHERITE, targets.get(MonstrosityMaterial.NETHERITE).getHealth());
                netherite.forceSpecial();
            }
            case 190 -> {
                check(expiringGolem != null && expiringGolem.isDeadOrDying(), "iron golem dies when its time is up (health " + (expiringGolem == null ? "-" : expiringGolem.getHealth()) + ")");
                check(lapisPet.hasEffect(ModEffects.PROTECTION) && lapisPet.getEffect(ModEffects.PROTECTION).getAmplifier() == 3 && lapisPet.getActiveEffects().size() == 1, "held Protection IV book grants the Protection IV buff: " + lapisPet.getActiveEffects());
                MobEffectInstance buff = lapisPet.getEffect(ModEffects.PROTECTION);
                check(buff != null && buff.getDuration() > 1100, "lapis buff lasts about a minute");
                check(Math.abs(LapisBuffEffects.modifyDamage(lapisPet, lapisPet.damageSources().generic(), 10.0f) - 7.6f) < 0.01f, "Protection IV buff takes 24% off");
                // The buffs are their own effects, so they stack with vanilla ones and with gear.
                double baseDamage = lapisPet.getAttributeValue(Attributes.ATTACK_DAMAGE);
                lapisPet.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                lapisPet.addEffect(new MobEffectInstance(ModEffects.SHARPNESS, 200, 4));
                check(Math.abs(lapisPet.getAttributeValue(Attributes.ATTACK_DAMAGE) - baseDamage - 6.0) < 0.01, "Sharpness V buff stacks with Strength");
                lapisPet.addEffect(new MobEffectInstance(ModEffects.DEPTH_STRIDER, 200, 1));
                check(EnchantmentHelper.getDepthStrider(lapisPet) == 2, "Depth Strider II buff adds to the gear's Depth Strider");
                lapisPet.addEffect(new MobEffectInstance(ModEffects.LOOTING, 200, 2));
                check(EnchantmentHelper.getMobLooting(lapisPet) == 3, "Looting III buff adds to the gear's Looting");
            }
            case 215 -> {
                Mob scorched = targets.get(MonstrosityMaterial.NETHERITE);
                check(scorched.getHealth() < targetHealth.get(MonstrosityMaterial.NETHERITE) - 5.0f && scorched.getDeltaMovement().y > 0.1, "netherite fissures hurt and launch the enemy");
                Mob gilded = targets.get(MonstrosityMaterial.GOLD);
                check(!gilded.hasEffect(ModEffects.GILDED), "gold stun wears off");
                LivingEntity clientGilded = ClientSelfTest.clientCopy(gilded.getId());
                check(clientGilded != null && !clientGilded.hasEffect(ModEffects.GILDED), "client sees the gold stun end");
                summoned.get(MonstrosityMaterial.COPPER).kill();
                summoned.get(MonstrosityMaterial.LAPIS).kill();
            }
            case 330 -> {
                List<ItemEntity> heads = level.getEntitiesOfClass(ItemEntity.class, new AABB(core(MonstrosityMaterial.COPPER)).inflate(16),
                        item -> item.getItem().is(ModRegistry.MATERIAL_MONSTROSITY_HEAD_ITEM));
                check(heads.size() == 1, "copper head dropped on death");
                check(level.getEntitiesOfClass(ItemEntity.class, new AABB(core(MonstrosityMaterial.LAPIS)).inflate(16),
                        item -> item.getItem().is(net.minecraft.world.item.Items.ENCHANTED_BOOK)).size() == 1, "lapis drops its book on death");
                if (!heads.isEmpty()) {
                    ItemStack head = heads.get(0).getItem();
                    check(MaterialMonstrosityHeadItem.getMaterial(head) == MonstrosityMaterial.COPPER, "head remembers its material");
                    check(player.getUUID().equals(MaterialMonstrosityHeadItem.getOwnerID(head)), "head remembers its owner");
                    check(head.getHoverName().getString().equals("Copper Monstrosity Head"), "head named after its material: " + head.getHoverName().getString());
                    heads.get(0).discard();
                    // Revive shape (see MaterialMonstrosityHeadBlock), facing north.
                    BlockPos base = core(MonstrosityMaterial.COPPER).offset(-3, 0, -30);
                    String[] rows = {"~~~^~~~", "D#####D", "###H###", "~#####~", "~##D##~"};
                    BlockPos headPos = null;
                    for (int r = 0; r < rows.length; ++r) {
                        for (int c = 0; c < 7; ++c) {
                            BlockPos pos = base.offset(c, -r, 0);
                            switch (rows[r].charAt(c)) {
                                case '#' -> level.setBlockAndUpdate(pos, Blocks.COPPER_BLOCK.defaultBlockState());
                                case 'H' -> level.setBlockAndUpdate(pos, ModBlocks.REINFORCED_REDSTONE_BLOCK.defaultBlockState());
                                case 'D' -> level.setBlockAndUpdate(pos, ModBlocks.DIAMOND_MOLD_BLOCK.defaultBlockState());
                                case '^' -> headPos = pos;
                                default -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                    level.setBlockAndUpdate(headPos, ModRegistry.MATERIAL_MONSTROSITY_HEAD.defaultBlockState());
                    ModRegistry.MATERIAL_MONSTROSITY_HEAD.setPlacedBy(level, headPos, level.getBlockState(headPos), player, head);
                    List<MaterialMonstrosity> revived = level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(base).inflate(12));
                    check(revived.size() == 1 && revived.get(0).material() == MonstrosityMaterial.COPPER, "copper revived from its head");
                    check(!revived.isEmpty() && player.getUUID().equals(revived.get(0).getOwnerId()), "revived keeps its owner");
                    check(level.getBlockState(headPos).isAir(), "revive shape used up");
                }
            }
            // The tentacles' first slam (28 ticks after they rise at 120).
            case 150 -> {
                Mob husk = targets.get(MonstrosityMaterial.SCULK);
                check(husk.getHealth() < targetHealth.get(MonstrosityMaterial.SCULK) - 5.0f, "sculk tentacles hurt the enemy (" + targetHealth.get(MonstrosityMaterial.SCULK) + " -> " + husk.getHealth() + ")");
                check(SculkDecay.stacks(husk) >= 2, "sculk tentacles decay the enemy (" + SculkDecay.stacks(husk) + " stacks)");
                check(level.getEntitiesOfClass(SculkMire.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(20), mire -> !mire.isPool()).size() >= 4,
                        "sculk tentacles leave lines of mire");
                check(SculkDecay.stacks(copperPet) == 0, "sculk decay is only for the sculk monstrosity's hits");
                check(SculkDecay.stacks(sculkBystander) == 0, "sculk decay spares bystanders (a cow by its enemy)");
                check(SculkDecay.stacks(calmPiglin) == 0, "sculk decay spares calm neutral mobs (a zombified piglin by its enemy)");
                sculkTargetHealth = husk.getHealth();
            }
            case 170 -> {
                Mob husk = targets.get(MonstrosityMaterial.SCULK);
                check(husk.getHealth() < sculkTargetHealth, "sculk decay hurts over time (" + sculkTargetHealth + " -> " + husk.getHealth() + ")");
                SculkDecay.apply(husk, summoned.get(MonstrosityMaterial.SCULK), 50);
                check(SculkDecay.stacks(husk) == 10, "sculk decay stacks up to 10 (" + SculkDecay.stacks(husk) + ")");
            }
            // The tentacle slam, at its enemy 5 blocks off: it turns so its tentacle, on its right, comes
            // down on it (after the Netherite one's checks: the client must be near to see it).
            case 253 -> {
                MaterialMonstrosity sculk = summoned.get(MonstrosityMaterial.SCULK);
                sculk.setTarget(targets.get(MonstrosityMaterial.SCULK));
                sculk.startTentacleSlam();
                check(sculk.isTentacleSlamming() && sculk.isBelching(), "sculk tentacle slam starts (and counts as busy)");
            }
            case 258 -> {
                LivingEntity client = ClientSelfTest.clientCopy(summoned.get(MonstrosityMaterial.SCULK).getId());
                check(client instanceof MaterialMonstrosity monstrosity && monstrosity.isTentacleSlamming() && monstrosity.tentacleSlamState.isStarted(),
                        "client plays the tentacle slam");
            }
            case 272 -> sculkTargetHealth = targets.get(MonstrosityMaterial.SCULK).getHealth();
            case 275 -> {
                MaterialMonstrosity sculk = summoned.get(MonstrosityMaterial.SCULK);
                Mob husk = targets.get(MonstrosityMaterial.SCULK);
                Vec3 right = com.Polarice3.Goety.utils.MobUtil.getHorizontalRightLookAngle(sculk);
                double aside = husk.position().subtract(sculk.position()).dot(right);
                check(Math.abs(aside - 2.4) < 0.5, "sculk turns its tentacle onto the enemy (" + aside + " to its right)");
                check(husk.getHealth() < sculkTargetHealth - 20.0f, "sculk tentacle slam hurts the enemy (" + sculkTargetHealth + " -> " + husk.getHealth() + ")");
                check(husk.getDeltaMovement().y > 0.1, "sculk tentacle slam throws the enemy up");
                check(!level.getEntitiesOfClass(SculkMire.class, new AABB(sculk.tentacleStart(), sculk.tentacleStart()).inflate(1.0), mire -> !mire.isPool()).isEmpty(),
                        "sculk tentacle slam leaves a line of mire");
            }
            // Its belch: sculk globs, which leave pools of mire where they burst.
            case 299 -> {
                MaterialMonstrosity sculk = summoned.get(MonstrosityMaterial.SCULK);
                Mob husk = targets.get(MonstrosityMaterial.SCULK);
                check(!sculk.isTentacleSlamming() && !sculk.isBelching(), "sculk tentacle slam ends");
                float yaw = (float) (Math.atan2(-(husk.getX() - sculk.getX()), husk.getZ() - sculk.getZ()) * 180.0 / Math.PI);
                sculk.setYRot(yaw);
                sculk.setYBodyRot(yaw);
                sculk.setYHeadRot(yaw);
                sculk.belch();
                check(level.getEntitiesOfClass(SculkGlob.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(10)).size() == 8, "sculk belches eight sculk globs");
            }
            case 335 -> {
                check(level.getEntitiesOfClass(SculkGlob.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(30)).isEmpty(), "sculk globs burst");
                check(!level.getEntitiesOfClass(SculkMire.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(30), SculkMire::isPool).isEmpty(), "sculk globs leave pools of mire");
            }
            // Sculk zombies: what dies decaying rises again, for the monstrosity that decayed it. All as sculk
            // zombies here (sculkRiseMode PUPPET; RiseChecks has the mobs that rise as themselves).
            case 362 -> {
                com.saints.goetybossservants.config.ServantConfig.SculkRiseMode.set(com.saints.goetybossservants.config.ServantConfig.RiseMode.PUPPET);
                MaterialMonstrosity sculk = summoned.get(MonstrosityMaterial.SCULK);
                BlockPos at = core(MonstrosityMaterial.SCULK).offset(-10, 0, -4);
                List<EntityType<? extends Mob>> types = List.of(EntityType.PIG, EntityType.SKELETON, EntityType.SPIDER, EntityType.COW);
                for (int i = 0; i < types.size(); ++i) {
                    Mob mob = types.get(i).create(level);
                    mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5 + i * 3, 90.0f, 0.0f);
                    mob.setYHeadRot(90.0f);
                    mob.setYBodyRot(90.0f);
                    mob.setNoAi(true);
                    if (types.get(i) == EntityType.SKELETON) {
                        // (Enchanted: its glint is drawn joined with the bow.)
                        ItemStack bow = new ItemStack(net.minecraft.world.item.Items.BOW);
                        bow.enchant(Enchantments.POWER_ARROWS, 1);
                        mob.setItemSlot(EquipmentSlot.MAINHAND, bow);
                        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                        // Sure to drop its helmet when it dies (its zombie still wears it).
                        mob.setDropChance(EquipmentSlot.HEAD, 2.0f);
                    }
                    level.addFreshEntity(mob);
                    // The cow dies without it.
                    if (types.get(i) != EntityType.COW) {
                        SculkDecay.apply(mob, sculk, 2);
                    }
                    mob.kill();
                }
                // Decayed, then saved and loaded again (as when its chunk unloads): it still rises for the monstrosity.
                Mob saved = EntityType.PIG.create(level);
                saved.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 12.5, 90.0f, 0.0f);
                saved.setNoAi(true);
                level.addFreshEntity(saved);
                SculkDecay.apply(saved, sculk, 2);
                CompoundTag data = saved.saveWithoutId(new CompoundTag());
                data.remove("UUID");
                saved.discard();
                Mob loaded = EntityType.PIG.create(level);
                loaded.load(data);
                level.addFreshEntity(loaded);
                loaded.kill();
                com.saints.goetybossservants.config.ServantConfig.SculkRiseMode.set(com.saints.goetybossservants.config.ServantConfig.RiseMode.AUTO);
            }
            case 363 -> {
                MaterialMonstrosity sculk = summoned.get(MonstrosityMaterial.SCULK);
                BlockPos at = core(MonstrosityMaterial.SCULK).offset(-10, 0, -4);
                sculkZombies = level.getEntitiesOfClass(SculkZombie.class, new AABB(at).inflate(16));
                sculkZombies.sort(java.util.Comparator.comparingDouble(Entity::getZ));
                check(sculkZombies.size() == 4, "decaying mobs rise as sculk zombies (one saved and loaded too), the one that wasn't decaying doesn't (" + sculkZombies.size() + ")");
                check(sculkZombies.stream().map(SculkZombie::sourceType).toList().equals(List.of(EntityType.PIG, EntityType.SKELETON, EntityType.SPIDER, EntityType.PIG)),
                        "sculk zombies are the mobs they were: " + sculkZombies.stream().map(SculkZombie::sourceType).toList());
                ListTag armor = sculkZombies.size() < 2 ? new ListTag() : sculkZombies.get(1).look().getList("ArmorItems", Tag.TAG_COMPOUND);
                check(armor.stream().anyMatch(item -> ((CompoundTag) item).getString("id").equals("minecraft:iron_helmet")), "a sculk zombie keeps the gear the mob dropped");
                CompoundTag carrying = new CompoundTag();
                carrying.put("Items", new ListTag());
                carrying.putString("Variant", "test");
                CompoundTag look = SculkZombie.lookOf(carrying);
                check(!look.contains("Items") && look.contains("Variant"), "a sculk zombie's look leaves out what the mob carried");
                CompoundTag huge = new CompoundTag();
                huge.putString("Huge", "x".repeat(70000));
                check(SculkZombie.lookOf(huge).isEmpty(), "a sculk zombie's look is left out when too big to send");
                check(sculkZombies.stream().allMatch(zombie -> sculk.getUUID().equals(zombie.getOwnerId())), "sculk zombies are the sculk monstrosity's");
                check(sculkZombies.stream().allMatch(zombie -> zombie.hasLifespan() && zombie.getLifespan() > 3500 && zombie.getLifespan() <= 3600), "sculk zombies last 3 minutes");
                check(!sculkZombies.isEmpty() && sculkZombies.get(0).getBbWidth() == EntityType.PIG.getWidth() && sculkZombies.get(0).getBbHeight() == EntityType.PIG.getHeight(),
                        "sculk zombie keeps its size");
                check(sculkZombies.stream().allMatch(zombie -> !zombie.isHostile() && sculk.isAlly(zombie)), "sculk zombies are on its side");
                check(!sculkZombies.isEmpty() && !sculkZombies.get(0).hurt(sculkZombies.get(0).damageSources().generic(), 5.0f), "nothing hurts a sculk zombie while it rises");
                sculkZombies.forEach(zombie -> zombie.setNoAi(true));
                // (What the mobs dropped when they died, out of the way of the check that the zombies drop nothing.)
                level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(16)).forEach(Entity::discard);
            }
            case 396 -> {
                LivingEntity client = sculkZombies.size() < 2 ? null : ClientSelfTest.clientCopy(sculkZombies.get(1).getId());
                check(client instanceof SculkZombie zombie && zombie.sourceType() == EntityType.SKELETON && zombie.look().contains("HandItems"),
                        "client knows what a sculk zombie was (for its look)");
                check(client instanceof SculkZombie zombie && !zombie.puppetFailed && zombie.puppet != null && zombie.puppet.getType() == EntityType.SKELETON,
                        "a sculk skeleton holding an enchanted bow is drawn as itself");
            }
            // (After a close look at them.)
            case 401 -> {
                sculkZombies.get(0).setLifespan(3);
                sculkZombies.get(2).kill();
            }
            case 405 -> {
                check(sculkZombies.get(0).isDeadOrDying() || sculkZombies.get(0).isRemoved(), "sculk zombie dies when its time is up");
                List<SculkZombie> left = level.getEntitiesOfClass(SculkZombie.class, new AABB(core(MonstrosityMaterial.SCULK)).inflate(30), SculkZombie::isAlive);
                check(left.size() == 2, "sculk zombies don't rise again (" + left.size() + " left)");
                check(level.getEntitiesOfClass(ItemEntity.class, new AABB(sculkZombies.get(0).blockPosition()).inflate(2)).isEmpty(), "sculk zombies drop nothing");
            }
            // The Sculk one's tentacle slam again, on clean ground and from its side (no target: straight
            // ahead), and one of its special's tentacles on its own (see showcase).
            case 630 -> showcased.get(MonstrosityMaterial.SCULK).startTentacleSlam();
            case 660 -> {
                for (BlockPos floor : BlockPos.betweenClosed(showcase(MonstrosityMaterial.SCULK).offset(6, -1, -6), showcase(MonstrosityMaterial.SCULK).offset(10, -1, 6))) {
                    level.setBlockAndUpdate(floor, Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
                SculkTentacle.spawn(level, showcased.get(MonstrosityMaterial.SCULK), Vec3.atBottomCenterOf(showcase(MonstrosityMaterial.SCULK).offset(8, 0, -3)), 0.0f, 1.0f);
            }
            case 710 -> {
                level.getEntitiesOfClass(Entity.class, new AABB(core(MonstrosityMaterial.COPPER)).inflate(400),
                        entity -> entity instanceof SculkZombie || entity instanceof SculkMire || entity instanceof SculkTentacle || entity instanceof SculkGlob).forEach(Entity::discard);
                sculkBystander.discard();
                calmPiglin.discard();
                decayedEveryTick.discard();
                summoned.values().forEach(m -> m.discard());
                if (original != null) {
                    original.discard();
                }
                level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(core(MonstrosityMaterial.COPPER)).inflate(400)).forEach(m -> m.discard());
                level.getEntitiesOfClass(MonstrosityIronGolem.class, new AABB(core(MonstrosityMaterial.IRON)).inflate(64)).forEach(m -> m.discard());
                targets.values().forEach(m -> m.discard());
            }
            default -> showcase(t);
        }
    }

    /**
     * Screenshots for checking the looks by eye (screenshots/ in the run folder): each monstrosity,
     * a gilded enemy, the diamond spikes, the netherite leap and landing, the iron golems, the lapis
     * book and a head.
     */
    private static void showcase(int t) {
        // Fresh, still monstrosities in a row for close front and back shots.
        MonstrosityMaterial[] all = MonstrosityMaterial.values();
        if (t == 402) {
            for (MonstrosityMaterial material : all) {
                BlockPos pos = showcase(material);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, new ChunkPos(pos), 2, new ChunkPos(pos));
                for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-5, -1, -5), pos.offset(5, -1, 5))) {
                    level.setBlockAndUpdate(floor, Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
                MaterialMonstrosity monstrosity = MaterialMonstrosity.create(level, material);
                monstrosity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
                monstrosity.setYHeadRot(0.0f);
                monstrosity.setYBodyRot(0.0f);
                monstrosity.setNoAi(true);
                if (material == MonstrosityMaterial.LAPIS) {
                    monstrosity.setHeldBook(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 5)));
                }
                level.addFreshEntity(monstrosity);
                showcased.put(material, monstrosity);
            }
        }
        if (t == 402) {
            // Goety's own Redstone Monstrosity beside the Gold one, to compare sitting down.
            BlockPos pos = showcase(MonstrosityMaterial.GOLD).offset(0, 0, 30);
            for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-5, -1, -5), pos.offset(5, -1, 5))) {
                level.setBlockAndUpdate(floor, Blocks.POLISHED_ANDESITE.defaultBlockState());
            }
            original = ModEntityType.REDSTONE_MONSTROSITY.create(level);
            original.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
            original.setYHeadRot(0.0f);
            original.setYBodyRot(0.0f);
            original.setNoAi(true);
            level.addFreshEntity(original);
        }
        if (t >= 410 && t < 410 + all.length * 12) {
            MonstrosityMaterial material = all[(t - 410) / 12];
            switch ((t - 410) % 12) {
                case 0 -> view(showcase(material).offset(0, 3, 0), 0.0, 3.0, 12.0);
                case 3 -> ClientSelfTest.screenshot(material.getId() + "_front");
                case 4 -> view(showcase(material).offset(0, 4, 0), -8.0, 5.0, -10.0);
                case 7 -> ClientSelfTest.screenshot(material.getId() + "_back");
                case 8 -> view(showcase(material).offset(0, 4, 0), -3.0, 10.0, -9.0);
                case 11 -> ClientSelfTest.screenshot(material.getId() + "_top");
                default -> {
                }
            }
        }
        switch (t) {
            case 121 -> view(core(MonstrosityMaterial.GOLD).offset(5, 1, 0), 0.0, 2.0, 5.0);
            case 124 -> ClientSelfTest.screenshot("gilded");
            case 126 -> view(core(MonstrosityMaterial.DIAMOND), 0.0, 9.0, 18.0);
            case 129 -> ClientSelfTest.screenshot("diamond_spikes");
            case 140 -> ClientSelfTest.screenshot("diamond_rain");
            case 185 -> view(core(MonstrosityMaterial.NETHERITE).offset(0, 3, 0), 0.0, 6.0, 16.0);
            case 188 -> ClientSelfTest.screenshot("netherite_minions");
            case 216 -> view(core(MonstrosityMaterial.NETHERITE).offset(2, 2, 0), -6.0, 10.0, 14.0);
            case 220 -> ClientSelfTest.screenshot("netherite_fissures");
            case 231 -> view(core(MonstrosityMaterial.NETHERITE).offset(2, 5, 0), -8.0, 4.0, 13.0);
            case 235 -> ClientSelfTest.screenshot("netherite_leap");
            case 246 -> view(core(MonstrosityMaterial.NETHERITE).offset(5, 1, 0), -6.0, 10.0, 12.0);
            case 248 -> ClientSelfTest.screenshot("netherite_crater");
            case 194 -> view(core(MonstrosityMaterial.IRON), 0.0, 6.0, -16.0);
            case 198 -> ClientSelfTest.screenshot("iron_golems");
            case 176 -> view(core(MonstrosityMaterial.LAPIS).offset(0, 6, 0), 0.0, 3.0, 12.0);
            case 180 -> ClientSelfTest.screenshot("lapis_book");
            // The Sculk one's attacks: its tentacles rearing back and slamming down round its enemy, its
            // tentacle slam raised and landed (from its right), its globs and their pools, and sculk
            // zombies rising and risen.
            case 134 -> view(core(MonstrosityMaterial.SCULK).offset(5, 1, 0), -4.0, 6.0, 13.0);
            case 137 -> ClientSelfTest.screenshot("sculk_tentacles_rear");
            case 145 -> view(core(MonstrosityMaterial.SCULK).offset(5, 1, 0), -4.0, 6.0, 13.0);
            case 149 -> ClientSelfTest.screenshot("sculk_tentacles_slam");
            case 249 -> view(core(MonstrosityMaterial.SCULK).offset(3, 3, 0), 2.0, 2.0, 16.0);
            case 265 -> ClientSelfTest.screenshot("sculk_tentacle_raise");
            case 279 -> ClientSelfTest.screenshot("sculk_tentacle_slam");
            case 300 -> view(core(MonstrosityMaterial.SCULK).offset(3, 4, 0), 0.0, 3.0, 17.0);
            case 308 -> ClientSelfTest.screenshot("sculk_globs");
            case 326 -> view(core(MonstrosityMaterial.SCULK).offset(5, 0, 0), 0.0, 11.0, 10.0);
            case 329 -> ClientSelfTest.screenshot("sculk_pools");
            case 369 -> view(core(MonstrosityMaterial.SCULK).offset(-10, 1, 0), 6.0, 2.5, 5.0);
            case 372 -> ClientSelfTest.screenshot("sculk_zombies_rising");
            case 392 -> view(core(MonstrosityMaterial.SCULK).offset(-10, 1, 0), 6.0, 2.5, 5.0);
            case 395 -> ClientSelfTest.screenshot("sculk_zombies");
            case 397 -> view(core(MonstrosityMaterial.SCULK).offset(-10, 1, -2), 3.5, 1.2, 1.5);
            case 400 -> ClientSelfTest.screenshot("sculk_zombies_close");
            case 340 -> {
                BlockPos pos = core(MonstrosityMaterial.GOLD).offset(0, 0, 40);
                level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(pos, ModRegistry.MATERIAL_MONSTROSITY_HEAD.defaultBlockState());
                ModRegistry.MATERIAL_MONSTROSITY_HEAD.setPlacedBy(level, pos, level.getBlockState(pos), null, MaterialMonstrosityHeadItem.create(MonstrosityMaterial.GOLD));
                ItemEntity item = new ItemEntity(level, pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, MaterialMonstrosityHeadItem.create(MonstrosityMaterial.NETHERITE));
                item.setNoGravity(true);
                item.setPickUpDelay(32767);
                level.addFreshEntity(item);
                view(pos.offset(1, 0, 0), 0.0, 2.0, 5.0);
            }
            case 346 -> ClientSelfTest.screenshot("heads");
            case 494 -> view(showcase(MonstrosityMaterial.LAPIS).offset(0, 6, 0), -0.5, 4.0, -3.0);
            case 497 -> ClientSelfTest.screenshot("lapis_table");
            case 498 -> view(showcase(MonstrosityMaterial.GOLD).offset(0, 2, 0), -7.0, 2.0, 5.0);
            case 501 -> ClientSelfTest.screenshot("gold_sceptre");
            // What a rider sees (the Lapis table and the Diamond crystals must stay out of the way).
            case 502 -> ride(showcased.get(MonstrosityMaterial.LAPIS));
            case 506 -> ClientSelfTest.screenshot("lapis_riding");
            case 507 -> ride(showcased.get(MonstrosityMaterial.DIAMOND));
            case 511 -> ClientSelfTest.screenshot("diamond_riding");
            case 512 -> ride(null);
            case 513 -> view(showcase(MonstrosityMaterial.GOLD).offset(0, 4, 0), 0.0, 0.5, 4.0);
            case 516 -> ClientSelfTest.screenshot("gold_face");
            case 517 -> {
                showcased.get(MonstrosityMaterial.GOLD).setStaying(true);
                original.setStaying(true);
                view(showcase(MonstrosityMaterial.GOLD).offset(0, 1, 15), 22.0, 3.0, 0.0);
            }
            case 520, 527, 534, 545, 560 -> ClientSelfTest.screenshot("sit_" + (t - 517));
            default -> {
            }
        }
        switch (t) {
            // What a rider sees past the Netherite one's horns.
            case 594 -> ride(showcased.get(MonstrosityMaterial.NETHERITE));
            case 598 -> ClientSelfTest.screenshot("netherite_riding");
            case 599 -> ride(null);
            // Close looks at the Sculk one: its face and ribs, and its tentacle.
            case 600 -> view(showcase(MonstrosityMaterial.SCULK).offset(0, 3, 0), 0.5, -1.3, 8.0);
            case 603 -> ClientSelfTest.screenshot("sculk_face");
            case 604 -> view(showcase(MonstrosityMaterial.SCULK).offset(0, 2, 0), -13.0, 1.0, 0.0);
            case 607 -> ClientSelfTest.screenshot("sculk_tentacle");
            case 608 -> view(showcase(MonstrosityMaterial.SCULK).offset(0, 4, 1), -3.5, 0.5, 5.0);
            case 611 -> ClientSelfTest.screenshot("sculk_head");
            case 612 -> view(showcase(MonstrosityMaterial.SCULK).offset(3, 5, 0), 1.0, 0.3, 3.5);
            case 615 -> ClientSelfTest.screenshot("sculk_shoulder");
            case 616 -> view(showcase(MonstrosityMaterial.SCULK).offset(0, 2, 0), 0.0, -0.6, 4.5);
            case 619 -> ClientSelfTest.screenshot("sculk_chest");
            // A real sculk shrieker beside its shoulder, to compare the shoulder's with (same pixel size).
            case 620 -> {
                // At rest (its attack flash, held on for the screenshots above, off).
                bigGlow(showcased.get(MonstrosityMaterial.SCULK), false);
                level.setBlockAndUpdate(showcase(MonstrosityMaterial.SCULK).offset(6, 6, 0),
                        Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(net.minecraft.world.level.block.SculkShriekerBlock.CAN_SUMMON, true));
                view(showcase(MonstrosityMaterial.SCULK).offset(4, 6, 0), 0.5, 4.5, 2.5);
            }
            case 623 -> ClientSelfTest.screenshot("sculk_shrieker_compare");
            case 624 -> view(showcase(MonstrosityMaterial.SCULK).offset(4, 6, 0), 1.5, 1.2, 5.0);
            case 627 -> {
                ClientSelfTest.screenshot("sculk_shrieker_compare_side");
                level.setBlockAndUpdate(showcase(MonstrosityMaterial.SCULK).offset(6, 6, 0), Blocks.AIR.defaultBlockState());
            }
            // Its tentacle slam from its right (raised, then down on the ground), the mire it leaves
            // from above, and one of its special's tentacles upright, reared back and slammed down.
            case 631 -> view(showcase(MonstrosityMaterial.SCULK).offset(-3, 2, 4), -14.0, 2.5, 0.0);
            case 643 -> ClientSelfTest.screenshot("sculk_tentacle_raise_side");
            case 655 -> ClientSelfTest.screenshot("sculk_tentacle_slam_side");
            case 662 -> view(showcase(MonstrosityMaterial.SCULK).offset(-2, 0, 5), -0.5, 12.0, 0.5);
            case 665 -> ClientSelfTest.screenshot("sculk_mire_line");
            case 667 -> view(showcase(MonstrosityMaterial.SCULK).offset(8, 2, 0), 12.0, 1.5, 0.0);
            case 670 -> ClientSelfTest.screenshot("sculk_tentacle_upright");
            case 683 -> ClientSelfTest.screenshot("sculk_tentacle_rear");
            case 690 -> ClientSelfTest.screenshot("sculk_tentacle_down");
            default -> {
            }
        }
        // The flash when attacking (Goety's big glow), held on.
        if (t >= 566 && t < 566 + all.length * 4) {
            MonstrosityMaterial material = all[(t - 566) / 4];
            switch ((t - 566) % 4) {
                case 0 -> {
                    view(showcase(material).offset(0, 3, 0), 0.0, 3.0, 12.0);
                    bigGlow(showcased.get(material), true);
                }
                case 3 -> ClientSelfTest.screenshot(material.getId() + "_attack_glow");
                default -> {
                }
            }
        }
    }

    // Holds Goety's attack flash on for a while, or turns it off (it is a client-side field, so set on the client copy).
    private static void bigGlow(Entity entity, boolean on) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level != null && minecraft.level.getEntity(entity.getId()) instanceof MaterialMonstrosity monstrosity) {
                monstrosity.bigGlow = on ? 1.0f : 0.0f;
                monstrosity.bigGlowCool = on ? 40 : 0;
            }
        });
    }

    // The Netherite one's minions in the test.
    private static List<MaterialMonstrosity> minions() {
        return level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(core(MonstrosityMaterial.NETHERITE)).inflate(24), MaterialMonstrosity::isMinion);
    }

    // Puts the real (client) player on {@code vehicle}, facing the way it faces; null gets them off.
    private static void ride(@Nullable Entity vehicle) {
        List<net.minecraft.server.level.ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        net.minecraft.server.level.ServerPlayer viewer = players.get(0);
        viewer.stopRiding();
        if (vehicle == null) {
            viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            return;
        }
        viewer.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        viewer.teleportTo(level, vehicle.getX(), vehicle.getY() + vehicle.getBbHeight(), vehicle.getZ(), vehicle.getYRot(), 10.0f);
        viewer.startRiding(vehicle, true);
    }

    private static BlockPos showcase(MonstrosityMaterial material) {
        return new BlockPos(-150 + material.ordinal() * 30, 120, -150);
    }

    // Moves the real (client) player to look at {@code target} from the given offset, in spectator mode.
    private static void view(BlockPos target, double dx, double dy, double dz) {
        List<net.minecraft.server.level.ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        net.minecraft.server.level.ServerPlayer viewer = players.get(0);
        viewer.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        level.setDayTime(6000);
        double x = target.getX() + 0.5 + dx;
        double y = target.getY() + dy;
        double z = target.getZ() + 0.5 + dz;
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI) + 180.0f;
        float pitch = (float) (Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        viewer.teleportTo(level, x, y - viewer.getEyeHeight(), z, yaw, pitch);
    }

    // A stationary, nearly unkillable hostile mob.
    private static Mob dummy(EntityType<? extends Mob> type, BlockPos pos) {
        Mob mob = type.create(level);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000.0);
        mob.setHealth(mob.getMaxHealth());
        level.addFreshEntity(mob);
        return mob;
    }

    // A stationary wolf tamed by the test player.
    private static Wolf pet(BlockPos pos) {
        Wolf wolf = EntityType.WOLF.create(level);
        wolf.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        wolf.setTame(true);
        wolf.setOwnerUUID(player.getUUID());
        wolf.setNoAi(true);
        level.addFreshEntity(wolf);
        return wolf;
    }
}
