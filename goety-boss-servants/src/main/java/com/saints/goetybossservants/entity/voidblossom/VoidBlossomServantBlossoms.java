/*
 * Parts of this file are adapted from Bosses of Mass Destruction by Barribob, licensed under the
 * GNU Lesser General Public License v3.0.
 * Modified for Goety Boss Servants: the blossom move rewritten with a smaller radius, no
 * moss or vine walls, and flowers that expire and restore the blocks they replaced.
 * Modified by Saints, 2026.
 */
package com.saints.goetybossservants.entity.voidblossom;

import net.barribob.boss.block.ModBlocks;
import net.barribob.boss.mob.mobs.void_blossom.VoidBlossomEntity;
import com.saints.goetybossservants.entity.bmd.Bmd;
import net.barribob.boss.utils.ModUtils;
import net.barribob.boss.utils.NetworkUtils;
import net.barribob.maelstrom.general.event.EventScheduler;
import com.saints.goetybossservants.config.ServantConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * The servant's version of BMD's Blossom move. BMD plants 8 healing Void Blossom flowers on moss 15
 * blocks out (vine-walled when it's badly hurt) and leaves them there. For a servant that would
 * rebuild the owner's base, so here the flowers:
 * <ul>
 *     <li>go down {@value #RADIUS} blocks out instead of 15, with no moss and no vine walls (the
 *     flower block doesn't need moss),</li>
 *     <li>only grow into empty space on solid ground,</li>
 *     <li>disappear after a configurable time, instantly if fire touches them, and when the servant
 *     dies; every block they replaced is put back.</li>
 * </ul>
 * The flowers are BMD's own block, but it only heals BMD's own Void Blossom type, so the healing is
 * done here the same way ({@link #healFrom}).
 */
public class VoidBlossomServantBlossoms {
    private static final double RADIUS = 4.0;
    private static final int FLOWERS = 8;
    /** Ticks before the first flower goes in, and between flowers. */
    private static final int PLANT_DELAY = 40;
    private static final int PLANT_INTERVAL = 8;
    /** BMD's flower block heals every 64 ticks: 10 health, 16 ticks after its heal beam. */
    private static final int HEAL_INTERVAL = 64;
    private static final int HEAL_DELAY = 16;
    private static final float HEAL_AMOUNT = 10.0f;

    private final VoidBlossomServant entity;
    /** Each placed flower and the blocks it changed (position -> original state). */
    private final List<Cluster> clusters = new ArrayList<>();
    /** Game time its last Blossom move finishes planting (flowers go in over about 5 seconds). */
    private long plantingUntil;

    private static final class Cluster {
        final BlockPos flower;
        final long expiresAt;
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();

        Cluster(BlockPos flower, long expiresAt) {
            this.flower = flower;
            this.expiresAt = expiresAt;
        }
    }

    public VoidBlossomServantBlossoms(VoidBlossomServant entity) {
        this.entity = entity;
    }

    public void placeAround(ServerLevel level, EventScheduler scheduler, BooleanSupplier shouldCancel) {
        this.plantingUntil = level.getGameTime() + PLANT_DELAY + (FLOWERS - 1) * PLANT_INTERVAL;
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < FLOWERS; ++i) {
            double angle = Math.PI * 2 * i / FLOWERS;
            spots.add(BlockPos.containing(this.entity.position().add(Math.cos(angle) * RADIUS, 0.0, Math.sin(angle) * RADIUS)));
        }
        Collections.shuffle(spots);
        Bmd.playSound(level, this.entity.position(), Bmd.sounds().getSpikeWaveIndicator(), 2.0f, 0.7f, 64.0);
        for (int i = 0; i < FLOWERS; ++i) {
            BlockPos spot = spots.get(i);
            scheduler.addEvent(Bmd.after(PLANT_DELAY + i * PLANT_INTERVAL, shouldCancel, () -> this.plant(level, spot)));
        }
    }

    private void plant(ServerLevel level, BlockPos spot) {
        BlockPos flower = this.findFlowerSpot(level, spot);
        if (flower == null) {
            return;
        }
        long expires = level.getGameTime() + ServantConfig.VoidBlossomFlowerSeconds.get() * 20L;
        Cluster cluster = new Cluster(flower, expires);
        BlockPos ground = flower.below();
        this.change(level, cluster, flower, ModBlocks.INSTANCE.getVoidBlossom().defaultBlockState());
        this.clusters.add(cluster);
        NetworkUtils.Companion.sendPlacePacket(this.entity, Vec3.atCenterOf(ground));
        Bmd.playSound(level, Vec3.atLowerCornerOf(ground), Bmd.sounds().getPetalBlade(), 1.0f, ModUtils.INSTANCE.randomPitch(this.entity.getRandom()), 64.0);
    }

    /** An empty, replaceable spot with solid ground under it, searched a few blocks up and down. */
    private BlockPos findFlowerSpot(Level level, BlockPos around) {
        for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4}) {
            BlockPos pos = around.above(dy);
            if (level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    private void change(Level level, Cluster cluster, BlockPos pos, BlockState state) {
        cluster.originals.putIfAbsent(pos.immutable(), level.getBlockState(pos));
        level.setBlockAndUpdate(pos, state);
    }

    /** Server tick: remove flowers whose time is up, that fire has reached, or that were broken. */
    public void tick(ServerLevel level) {
        Iterator<Cluster> it = this.clusters.iterator();
        while (it.hasNext()) {
            Cluster cluster = it.next();
            if (!level.isLoaded(cluster.flower)) {
                continue;
            }
            boolean expired = level.getGameTime() >= cluster.expiresAt;
            boolean broken = !level.getBlockState(cluster.flower).is(ModBlocks.INSTANCE.getVoidBlossom());
            if (expired || broken || this.touchesFire(level, cluster.flower)) {
                this.restore(level, cluster);
                it.remove();
            } else if ((level.getGameTime() + cluster.flower.hashCode()) % HEAL_INTERVAL == 0) {
                this.healFrom(cluster.flower);
            }
        }
    }

    /**
     * BMD's flower block only heals entities of BMD's own Void Blossom type, so it never heals the
     * servant. Same heal as the block: a heal beam from the flower, then 10 health, never past the
     * next health milestone (so the flowers can't undo the milestone that planted them).
     */
    private void healFrom(BlockPos flower) {
        if (!this.entity.isAlive() || this.entity.getHealth() >= this.entity.getMaxHealth()) {
            return;
        }
        NetworkUtils.Companion.sendHealPacket(this.entity, Vec3.atCenterOf(flower), this.entity.position().add(0.0, 5.0, 0.0));
        this.entity.scheduler().addEvent(Bmd.after(HEAL_DELAY, () -> {
            if (!this.entity.isAlive()) {
                return;
            }
            float max = this.entity.getMaxHealth();
            float ratio = this.entity.getHealth() / max;
            float cap = 1.0f;
            for (float milestone : VoidBlossomEntity.Companion.getHpMilestones()) {
                if (milestone >= ratio - 1.0e-4f && milestone < cap) {
                    cap = milestone;
                }
            }
            float amount = Math.min(HEAL_AMOUNT, cap * max - this.entity.getHealth());
            if (amount > 0.0f) {
                this.entity.heal(amount);
            }
        }));
    }

    private boolean touchesFire(Level level, BlockPos flower) {
        for (BlockPos pos : BlockPos.betweenClosed(flower.offset(-1, -1, -1), flower.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts back what the flower replaced, but only where our block is still there. (Moss and vine
     * walls are only placed by older versions, but their saved flowers are cleaned up the same way.)
     */
    private void restore(Level level, Cluster cluster) {
        Block flower = ModBlocks.INSTANCE.getVoidBlossom();
        Block vine = ModBlocks.INSTANCE.getVineWall();
        List<BlockPos> order = new ArrayList<>(cluster.originals.keySet());
        Collections.reverse(order);
        for (BlockPos pos : order) {
            BlockState now = level.getBlockState(pos);
            if (now.is(flower) || now.is(vine) || now.is(Blocks.MOSS_BLOCK) && pos.equals(cluster.flower.below())) {
                level.setBlockAndUpdate(pos, cluster.originals.get(pos));
            }
        }
    }

    public void removeAll(Level level) {
        for (Cluster cluster : this.clusters) {
            this.restore(level, cluster);
        }
        this.clusters.clear();
    }

    /** Flowers are out or still being planted: a new Blossom move would only pile more on (see VoidBlossomServant.hurt). */
    public boolean isActive(Level level) {
        return !this.clusters.isEmpty() || level.getGameTime() <= this.plantingUntil;
    }

    public List<BlockPos> flowerPositions() {
        return this.clusters.stream().map(cluster -> cluster.flower).toList();
    }

    public ListTag save() {
        ListTag list = new ListTag();
        for (Cluster cluster : this.clusters) {
            CompoundTag tag = new CompoundTag();
            tag.put("Flower", NbtUtils.writeBlockPos(cluster.flower));
            tag.putLong("ExpiresAt", cluster.expiresAt);
            ListTag originals = new ListTag();
            cluster.originals.forEach((pos, state) -> {
                CompoundTag entry = new CompoundTag();
                entry.put("Pos", NbtUtils.writeBlockPos(pos));
                entry.put("State", NbtUtils.writeBlockState(state));
                originals.add(entry);
            });
            tag.put("Originals", originals);
            list.add(tag);
        }
        return list;
    }

    public void load(Level level, ListTag list) {
        this.clusters.clear();
        for (Tag t : list) {
            CompoundTag tag = (CompoundTag) t;
            Cluster cluster = new Cluster(NbtUtils.readBlockPos(tag.getCompound("Flower")), tag.getLong("ExpiresAt"));
            for (Tag e : tag.getList("Originals", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) e;
                cluster.originals.put(NbtUtils.readBlockPos(entry.getCompound("Pos")),
                        NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), entry.getCompound("State")));
            }
            this.clusters.add(cluster);
        }
    }
}
