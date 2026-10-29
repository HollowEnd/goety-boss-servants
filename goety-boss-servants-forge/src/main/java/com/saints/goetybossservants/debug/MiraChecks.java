package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.items.ModItems;
import com.saints.goetybossservants.construct.MiraMonstrosityMold;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import com.saints.goetybossservants.entity.MiraVariant;
import com.saints.goetybossservants.item.MiraMonstrosityHeadItem;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

import static com.saints.goetybossservants.debug.SelfTest.check;

/** Mira Monstrosity part of the dev self-test (see SelfTest). Steps are ticks after START. */
class MiraChecks {
    static final int START = 0;
    private static final List<MiraMonstrosityServant> summoned = new ArrayList<>();
    private static ServerLevel level;
    private static FakePlayer player;
    private static net.minecraft.world.entity.Mob ownChoice;
    private static net.minecraft.world.entity.Mob ownerChoice;

    private static BlockPos core(MiraVariant variant) {
        return new BlockPos(variant.ordinal() * 40, 120, 0);
    }

    static void step(ServerLevel serverLevel, FakePlayer fakePlayer, int t) {
        level = serverLevel;
        player = fakePlayer;
        switch (t) {
            case 0 -> {
                check(IServant.class.isAssignableFrom(MiraMonstrosityServant.class), "mixin made the servant an IServant");
                // A Redstone Monstrosity mold (redstone fill) must still be left to Goety.
                MiraMonstrosityMold.placeMold(level, core(MiraVariant.MIRA), MiraVariant.MIRA);
                level.setBlockAndUpdate(core(MiraVariant.MIRA).offset(1, 0, 0), Blocks.REDSTONE_BLOCK.defaultBlockState());
                check(MiraMonstrosityMold.findVariant(level, core(MiraVariant.MIRA)) == null, "incomplete / redstone mold is not a Mira mold");
                for (MiraVariant variant : MiraVariant.values()) {
                    level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED, new net.minecraft.world.level.ChunkPos(core(variant)), 3, new net.minecraft.world.level.ChunkPos(core(variant)));
                    MiraMonstrosityMold.placeMold(level, core(variant), variant);
                    check(MiraMonstrosityMold.findVariant(level, core(variant)) == variant, "mold detected as " + variant);
                }
            }
            case 1 -> {
                for (MiraVariant variant : MiraVariant.values()) {
                    ItemStack stack = new ItemStack(ModItems.ANIMATION_CORE.get());
                    MiraMonstrosityMold.spawnServant(level, core(variant), variant, player, stack);
                    List<MiraMonstrosityServant> found = level.getEntitiesOfClass(MiraMonstrosityServant.class, new AABB(core(variant)).inflate(4));
                    check(found.size() == 1, variant + " servant spawned");
                    check(stack.isEmpty(), variant + " animation core consumed");
                    check(level.getBlockState(core(variant)).isAir(), variant + " core block used up");
                    check(level.getBlockState(core(variant).below()).is(Blocks.STONE_BRICKS), variant + " brick base kept");
                    if (!found.isEmpty()) {
                        MiraMonstrosityServant servant = found.get(0);
                        summoned.add(servant);
                        check(servant.getVariant() == variant, variant + " variant set");
                        check(player.getUUID().equals(servant.servant().getOwnerId()), variant + " owner set via IOwned.getOwnerId()");
                        check(servant.isTame(), variant + " tamed for meowhem's own ally checks");
                    }
                }
                check(level.getEntitiesOfClass(MiraMonstrosityServant.class, new AABB(BlockPos.ZERO).inflate(200)).size() == 3, "three servants exist");
                // A fourth one is over the per-player limit and must be refused, leaving the mold intact.
                MiraMonstrosityMold.placeMold(level, core(MiraVariant.MIRA), MiraVariant.MIRA);
                ItemStack extra = new ItemStack(ModItems.ANIMATION_CORE.get());
                MiraMonstrosityMold.spawnServant(level, core(MiraVariant.MIRA), MiraVariant.MIRA, player, extra);
                check(!extra.isEmpty() && !level.getBlockState(core(MiraVariant.MIRA)).isAir(), "summon limit refuses a fourth servant");
            }
            case 40 -> {
                MiraMonstrosityServant obsidian = summoned.get(1);
                check(obsidian.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) == 32.0, "follow range is Goety's 32, not the boss's 128");
                ownChoice = dummy(net.minecraft.world.entity.EntityType.VINDICATOR, core(MiraVariant.OBSIDIAN).offset(3, 0, 3));
            }
            case 70 -> {
                MiraMonstrosityServant obsidian = summoned.get(1);
                check(obsidian.getTarget() == ownChoice, "picks a nearby enemy by itself (target: " + obsidian.getTarget() + ")");
                ownerChoice = dummy(net.minecraft.world.entity.EntityType.HUSK, core(MiraVariant.OBSIDIAN).offset(-3, 0, -3));
                // Goety looks owners up in the world, which a fake player isn't in, so an armor stand plays the owner here.
                net.minecraft.world.entity.decoration.ArmorStand stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level);
                stand.moveTo(core(MiraVariant.OBSIDIAN).getX() + 0.5, 120, core(MiraVariant.OBSIDIAN).getZ() - 5.5, 0.0f, 0.0f);
                level.addFreshEntity(stand);
                obsidian.servant().setTrueOwner(stand);
                stand.tickCount = 100;
                stand.setLastHurtMob(ownerChoice);
            }
            case 100 -> {
                MiraMonstrosityServant obsidian = summoned.get(1);
                check(obsidian.getTarget() == ownerChoice, "switches to the mob its owner hits (target: " + obsidian.getTarget() + ")");
                ownChoice.discard();
                ownerChoice.discard();
            }
            case 200 -> {
                for (MiraMonstrosityServant servant : summoned) {
                    check(servant.isAlive(), servant.getVariant() + " still alive after 10s of ticking");
                }
                MiraMonstrosityServant mira = summoned.get(0);
                check(mira.getName().getString().equals("Mira Monstrosity") || mira.getName().getString().equals("entity.goetybossservants.mira_monstrosity"), "mira name: " + mira.getName().getString());
                check(summoned.get(1).getName().getString().equals("Obsidian Monstrosity"), "obsidian name drives meowhem's skin");
                check(summoned.get(2).getName().getString().equals("Warden Monstrosity"), "warden name drives meowhem's skin");
                // Goety wand: cycles follow -> wander -> stay.
                mira.servant().updateMoveMode(player);
                check(mira.isWandering(), "wand command: wander");
                mira.servant().updateMoveMode(player);
                check(mira.isStaying(), "wand command: stay");
            }
            case 220 -> {
                MiraMonstrosityServant mira = summoned.get(0);
                check(mira.isInSittingPose(), "staying uses meowhem's sit pose");
                // Second wand dismissal kills it (first only warns).
                MiraMonstrosityServant warden = summoned.get(2);
                warden.servant().tryKill(player);
                check(warden.isAlive(), "first tryKill only warns");
                warden.servant().tryKill(player);
                check(!warden.isAlive() || warden.isDeadOrDying(), "second tryKill kills");
            }
            case 300 -> {
                List<ItemEntity> heads = level.getEntitiesOfClass(ItemEntity.class, new AABB(core(MiraVariant.WARDEN)).inflate(16),
                        item -> item.getItem().is(ModRegistry.MIRA_MONSTROSITY_HEAD_ITEM.get()));
                check(heads.size() == 1, "head dropped on death");
                if (!heads.isEmpty()) {
                    ItemStack head = heads.get(0).getItem();
                    check(MiraMonstrosityHeadItem.getVariant(head) == MiraVariant.WARDEN, "head remembers variant");
                    check(player.getUUID().equals(MiraMonstrosityHeadItem.getOwnerID(head)), "head remembers owner");
                    heads.get(0).discard();
                    // Revive shape (see MiraMonstrosityHeadBlock), built facing north at z = 60.
                    BlockPos base = new BlockPos(0, 120, 60);
                    String[] rows = {"~~~^~~~", "D#####D", "###H###", "~#####~", "~##D##~"};
                    BlockPos headPos = null;
                    for (int r = 0; r < rows.length; ++r) {
                        for (int c = 0; c < 7; ++c) {
                            BlockPos pos = base.offset(c, -r, 0);
                            switch (rows[r].charAt(c)) {
                                case '#' -> level.setBlockAndUpdate(pos, Blocks.ORANGE_TERRACOTTA.defaultBlockState());
                                case 'H' -> level.setBlockAndUpdate(pos, ModBlocks.REINFORCED_REDSTONE_BLOCK.get().defaultBlockState());
                                case 'D' -> level.setBlockAndUpdate(pos, ModBlocks.DIAMOND_MOLD_BLOCK.get().defaultBlockState());
                                case '^' -> headPos = pos;
                                default -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                    level.setBlockAndUpdate(headPos, ModRegistry.MIRA_MONSTROSITY_HEAD.get().defaultBlockState());
                    ModRegistry.MIRA_MONSTROSITY_HEAD.get().setPlacedBy(level, headPos, level.getBlockState(headPos), player, head);
                    List<MiraMonstrosityServant> revived = level.getEntitiesOfClass(MiraMonstrosityServant.class, new AABB(base).inflate(12));
                    check(revived.size() == 1, "revived from head");
                    if (!revived.isEmpty()) {
                        check(revived.get(0).getVariant() == MiraVariant.WARDEN, "revived keeps variant");
                        check(player.getUUID().equals(revived.get(0).servant().getOwnerId()), "revived keeps owner");
                    }
                    check(level.getBlockState(headPos).isAir(), "revive shape used up");
                }
            }
            default -> {
            }
        }
    }

    // A stationary, nearly unkillable hostile mob for the targeting checks.
    private static net.minecraft.world.entity.Mob dummy(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, BlockPos pos) {
        net.minecraft.world.entity.Mob mob = type.create(level);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1_000_000.0);
        mob.setHealth(mob.getMaxHealth());
        level.addFreshEntity(mob);
        return mob;
    }
}
