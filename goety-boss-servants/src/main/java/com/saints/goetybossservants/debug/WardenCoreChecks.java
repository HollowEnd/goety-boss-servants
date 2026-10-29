package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.common.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.block.WardenCoreBlock;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.entity.FakePlayer;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * The Warden Core (see WardenCoreBlock): wardens drop it, it crafts into 4 echo shards, its
 * tooltip, its shape and heartbeat, and the Sculk Monstrosity built by right-clicking it with an
 * Animation Core in a complete mold (through the game's own right-click, as a player would), but
 * not in an incomplete one or by a spectator, and without using the Animation Core up in creative.
 * Also screenshots of the mold, the core dim and glowing, and its item. Steps are ticks after START.
 */
class WardenCoreChecks {
    static final int START = RiseChecks.START + RiseChecks.LENGTH;
    static final int LENGTH = 110;
    private static final BlockPos BASE = new BlockPos(160, 120, 220);
    private static final BlockPos SIDE_BY_SIDE = BASE.offset(0, -1, -12);
    private static ServerLevel level;
    private static boolean dimShot;
    private static boolean glowShot;
    // The action bar message the real client showed (read on its own thread).
    private static volatile String heardOnClient;

    static void step(ServerLevel serverLevel, FakePlayer player, int t) {
        level = serverLevel;
        switch (t) {
            case 0 -> {
                ChunkPos chunk = new ChunkPos(BASE);
                level.getChunkSource().addRegionTicket(TicketType.FORCED, chunk, 3, chunk);
                // (The client needs the area loaded before the first screenshot.)
                view(BASE, 9.0, 9.0, 12.0);
            }
            case 5 -> {
                for (BlockPos floor : BlockPos.betweenClosed(BASE.offset(-9, -2, -30), BASE.offset(9, -2, 9))) {
                    level.setBlockAndUpdate(floor, Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
                MaterialMonstrosityMold.placeMold(level, BASE, MonstrosityMaterial.SCULK);

                // Every warden drops one.
                BlockPos wardenPos = BASE.offset(0, -1, -20);
                Warden warden = EntityType.WARDEN.create(level);
                warden.moveTo(wardenPos.getX() + 0.5, wardenPos.getY(), wardenPos.getZ() + 0.5, 0.0f, 0.0f);
                level.addFreshEntity(warden);
                warden.hurt(level.damageSources().playerAttack(player), Float.MAX_VALUE);
                check(!warden.isAlive() && cores(wardenPos) == 1, "a warden drops a Warden Core (" + cores(wardenPos) + ")");
                level.getEntitiesOfClass(ItemEntity.class, new AABB(wardenPos).inflate(4)).forEach(Entity::discard);
                // Also where another loot table pulls the warden's in, and once (the loader's own parts: WardenCoreLootChecks).
                LootTable pullsIn = LootTable.lootTable().withPool(LootPool.lootPool().add(LootTableReference.lootTableReference(EntityType.WARDEN.getDefaultLootTable()))).build();
                int pulled = pullsIn.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY)).stream()
                        .filter(stack -> stack.is(ModRegistry.WARDEN_CORE_ITEM)).mapToInt(ItemStack::getCount).sum();
                check(pulled == 1, "a loot table that pulls in the warden's gives one Warden Core (" + pulled + ")");
                WardenCoreLootChecks.run(level);

                // Mined, it drops itself.
                BlockPos minedPos = BASE.offset(4, -1, -20);
                level.setBlockAndUpdate(minedPos, ModRegistry.WARDEN_CORE.defaultBlockState());
                check(level.getBlockState(minedPos).getShape(level, minedPos).max(Direction.Axis.Y) == 0.5, "Warden Core is half a block tall");
                level.destroyBlock(minedPos, true, player);
                check(cores(minedPos) == 1, "a mined Warden Core drops itself");
                level.getEntitiesOfClass(ItemEntity.class, new AABB(minedPos).inflate(4)).forEach(Entity::discard);

                // Crafted, it gives 4 echo shards.
                Recipe<?> recipe = level.getRecipeManager().byKey(new ResourceLocation(GoetyBossServants.MOD_ID, "echo_shards_from_warden_core")).orElse(null);
                ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
                check(recipe != null && recipe.getType() == RecipeType.CRAFTING && result.is(Items.ECHO_SHARD) && result.getCount() == 4
                        && recipe.getIngredients().size() == 1 && recipe.getIngredients().get(0).test(new ItemStack(ModRegistry.WARDEN_CORE_ITEM)),
                        "a Warden Core crafts into 4 echo shards");

                // Its tooltip names what it makes.
                List<Component> tooltip = new ArrayList<>();
                ItemStack core = new ItemStack(ModRegistry.WARDEN_CORE_ITEM);
                core.getItem().appendHoverText(core, level, tooltip, TooltipFlag.NORMAL);
                check(tooltip.stream().anyMatch(line -> line.getString().equals("Used to create the Sculk Monstrosity")),
                        "Warden Core's tooltip: " + tooltip.stream().map(Component::getString).toList());

                // A calm warden's heartbeat: a beat every two seconds, glowing for half a second, fading
                // smoothly between ticks, on a new world as on one played for a thousand hours.
                for (long start : new long[]{0L, 72_000_000L}) {
                    int lit = 0;
                    float brightest = 0.0f;
                    long beat = -1;
                    for (long time = start; time < start + WardenCoreBlock.BEAT; ++time) {
                        float glow = WardenCoreBlock.heartbeat(BASE, time, 0.0f);
                        lit += glow > 0.0f ? 1 : 0;
                        brightest = Math.max(brightest, glow);
                        beat = glow == 1.0f ? time : beat;
                    }
                    float halfway = WardenCoreBlock.heartbeat(BASE, beat, 0.5f);
                    check(lit == 10 && brightest == 1.0f && Math.abs(halfway - 0.95f) < 0.001f,
                            "Warden Core beats like a calm warden's heart at tick " + start + " (" + lit + " lit ticks, " + brightest + ", " + halfway + ")");
                }

                // One on its own between a sculk catalyst and a shrieker (in the mold its sides are hidden).
                level.setBlockAndUpdate(SIDE_BY_SIDE, ModRegistry.WARDEN_CORE.defaultBlockState());
                level.setBlockAndUpdate(SIDE_BY_SIDE.east(), Blocks.SCULK_SHRIEKER.defaultBlockState());
                level.setBlockAndUpdate(SIDE_BY_SIDE.west(), Blocks.SCULK_CATALYST.defaultBlockState());
                // And its item lying in front of them.
                ItemEntity item = new ItemEntity(level, SIDE_BY_SIDE.getX() + 0.5, SIDE_BY_SIDE.getY() + 0.1, SIDE_BY_SIDE.getZ() + 1.6,
                        new ItemStack(ModRegistry.WARDEN_CORE_ITEM));
                item.setNoGravity(true);
                item.setDeltaMovement(Vec3.ZERO);
                item.setPickUpDelay(32767);
                level.addFreshEntity(item);
            }
            // The whole mold, and the core between its shriekers: dim between beats, then glowing.
            case 10 -> view(BASE, 9.0, 9.0, 12.0);
            case 14 -> ClientSelfTest.screenshot("sculk_mold");
            case 15 -> view(BASE, 0.0, 3.2, 0.8);
            case 70 -> view(SIDE_BY_SIDE, 1.2, 1.3, 2.6);
            case 74 -> ClientSelfTest.screenshot("warden_core_side");
            case 80 -> {
                // Only this test's monstrosity counts against the limit.
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof MaterialMonstrosity monstrosity && monstrosity.material() == MonstrosityMaterial.SCULK) {
                        monstrosity.discard();
                    }
                }
                // Right-clicking the core with an Animation Core, as a player would: nothing while a shrieker
                // is missing, and Goety's "wrong blocks" message rather than silence (only for the Warden
                // Core: clicking a brick still says nothing, as in Goety)...
                level.setBlockAndUpdate(BASE.offset(-1, 0, 0), Blocks.SCULK_CATALYST.defaultBlockState());
                ItemStack animationCore = new ItemStack(ModItems.ANIMATION_CORE);
                Listener listener = new Listener(level);
                InteractionResult refused = use(listener, animationCore, BASE);
                check(refused == InteractionResult.FAIL && !animationCore.isEmpty() && level.getBlockState(BASE).is(ModRegistry.WARDEN_CORE)
                        && built().isEmpty(), "an incomplete sculk mold builds nothing (" + refused + ")");
                check(listener.heard.equals(List.of("info.goety.block.fail")), "an incomplete sculk mold says its blocks are wrong: " + listener.heard);
                listener.heard.clear();
                use(listener, animationCore, BASE.offset(0, -1, 0));
                check(listener.heard.isEmpty() && !animationCore.isEmpty(), "an Animation Core on a brick says nothing: " + listener.heard);
                // ...then it rises.
                level.setBlockAndUpdate(BASE.offset(-1, 0, 0), Blocks.SCULK_SHRIEKER.defaultBlockState());
                use(player, animationCore);
                List<MaterialMonstrosity> built = built();
                check(built.size() == 1 && player.getUUID().equals(built.get(0).getOwnerId()), "right-clicking the Warden Core with an Animation Core builds the sculk one");
                check(animationCore.isEmpty() && level.getBlockState(BASE).isAir() && level.getBlockState(BASE.offset(-1, 0, 0)).isAir()
                        && level.getBlockState(BASE.offset(2, 0, 2)).isAir(), "Animation Core, Warden Core, shriekers and catalysts used up");
                view(BASE, 8.0, 6.0, 10.0);
            }
            case 84 -> ClientSelfTest.screenshot("sculk_mold_built");
            case 86 -> {
                built().forEach(Entity::discard);
                MaterialMonstrosityMold.placeMold(level, BASE, MonstrosityMaterial.SCULK);
                // Who may use the Animation Core is vanilla's rule: not a spectator, even with CanPlaceOn for the core...
                player.setGameMode(GameType.SPECTATOR);
                ItemStack placesOnCore = canPlaceOn(ModRegistry.WARDEN_CORE);
                use(player, placesOnCore);
                check(built().isEmpty() && !placesOnCore.isEmpty() && level.getBlockState(BASE).is(ModRegistry.WARDEN_CORE), "a spectator can't build one");
                // ...in adventure mode only with CanPlaceOn for the core...
                player.setGameMode(GameType.ADVENTURE);
                ItemStack plain = new ItemStack(ModItems.ANIMATION_CORE);
                ItemStack placesOnStone = canPlaceOn(Blocks.STONE);
                use(player, plain);
                use(player, placesOnStone);
                check(built().isEmpty() && !plain.isEmpty() && !placesOnStone.isEmpty() && level.getBlockState(BASE).is(ModRegistry.WARDEN_CORE),
                        "in adventure mode one can't be built without CanPlaceOn for the core");
                use(player, placesOnCore);
                check(built().size() == 1 && placesOnCore.isEmpty(), "in adventure mode one can be built with CanPlaceOn for the core");
                // ...and in creative it isn't used up, as with Goety's own molds.
                built().forEach(Entity::discard);
                MaterialMonstrosityMold.placeMold(level, BASE, MonstrosityMaterial.SCULK);
                player.setGameMode(GameType.CREATIVE);
                use(player, plain);
                check(built().size() == 1 && plain.getCount() == 1, "in creative the Animation Core is kept");
                player.setGameMode(GameType.SURVIVAL);
            }
            // The real client's own right-click on an incomplete mold's core: the click must reach the
            // server (whose hook sends the "wrong blocks" message) with the client saying nothing itself.
            case 88 -> {
                built().forEach(Entity::discard);
                MaterialMonstrosityMold.placeMold(level, BASE, MonstrosityMaterial.SCULK);
                level.setBlockAndUpdate(BASE.offset(0, 0, -1), Blocks.SCULK_CATALYST.defaultBlockState());
                view(BASE, 0.0, 2.4, 1.4);
                ServerPlayer viewer = viewer();
                if (viewer != null) {
                    viewer.setGameMode(GameType.SURVIVAL);
                    viewer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANIMATION_CORE));
                }
            }
            case 93 -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    overlay(minecraft, null);
                    if (minecraft.player != null && minecraft.gameMode != null) {
                        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(new Vec3(BASE.getX() + 0.5, BASE.getY() + 0.5, BASE.getZ() + 0.5), Direction.UP, BASE, false));
                    }
                });
            }
            case 100 -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> heardOnClient = overlay(minecraft));
            }
            case 104 -> {
                ServerPlayer viewer = viewer();
                if (viewer != null) {
                    String expected = Component.translatable("info.goety.block.fail").getString();
                    check(expected.equals(heardOnClient) && built().isEmpty() && viewer.getMainHandItem().is(ModItems.ANIMATION_CORE),
                            "a player's click on an incomplete sculk mold reaches the server, which says its blocks are wrong: " + heardOnClient);
                    viewer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    viewer.setGameMode(GameType.SPECTATOR);
                }
            }
            default -> {
            }
        }
        // Close over the core: one shot well between beats, one just after a beat (the souls at their
        // brightest). The client runs a tick or two behind, so the glow shot waits three ticks past the beat.
        if (t == 18) {
            dimShot = false;
            glowShot = false;
        }
        if (t > 18 && t < 68) {
            long time = level.getGameTime();
            if (!dimShot && WardenCoreBlock.heartbeat(BASE, time - 4, 0.0f) == 0.0f && WardenCoreBlock.heartbeat(BASE, time + 4, 0.0f) == 0.0f) {
                dimShot = true;
                ClientSelfTest.screenshot("warden_core_dim_top");
            } else if (!glowShot && WardenCoreBlock.heartbeat(BASE, time - 3, 0.0f) >= 0.9f) {
                glowShot = true;
                ClientSelfTest.screenshot("warden_core_glow_top");
            }
        }
        if (t == 68) {
            check(dimShot && glowShot, "Warden Core seen between beats and glowing");
        }
    }

    // An Animation Core that adventure mode lets be used on {@code block} (CanPlaceOn).
    private static ItemStack canPlaceOn(Block block) {
        ItemStack stack = new ItemStack(ModItems.ANIMATION_CORE);
        ListTag blocks = new ListTag();
        blocks.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(block).toString()));
        stack.getOrCreateTag().put("CanPlaceOn", blocks);
        return stack;
    }

    @Nullable
    private static ServerPlayer viewer() {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    // The action bar message the client shows (Gui keeps it private; dev names), or sets it.
    @Nullable
    private static String overlay(Minecraft minecraft) {
        try {
            Field field = Gui.class.getDeclaredField("overlayMessageString");
            field.setAccessible(true);
            return field.get(minecraft.gui) instanceof Component message ? message.getString() : null;
        } catch (ReflectiveOperationException e) {
            return "(unreadable: " + e + ")";
        }
    }

    private static void overlay(Minecraft minecraft, @Nullable Component message) {
        try {
            Field field = Gui.class.getDeclaredField("overlayMessageString");
            field.setAccessible(true);
            field.set(minecraft.gui, message);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    // Item entities of the Warden Core near pos.
    private static int cores(BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4), item -> item.getItem().is(ModRegistry.WARDEN_CORE_ITEM))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static List<MaterialMonstrosity> built() {
        return level.getEntitiesOfClass(MaterialMonstrosity.class, new AABB(BASE).inflate(4), m -> m.material() == MonstrosityMaterial.SCULK);
    }

    // The game's own right-click on the core's top (fires the mods' right-click hooks).
    private static InteractionResult use(FakePlayer player, ItemStack stack) {
        return use(player, stack, BASE);
    }

    private static InteractionResult use(FakePlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        InteractionResult result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), Direction.UP, pos, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return result;
    }

    // A fake player that keeps the action bar messages it's sent (a plain one drops them).
    private static class Listener extends FakePlayer {
        final List<String> heard = new ArrayList<>();

        Listener(ServerLevel level) {
            super(level, new GameProfile(UUID.nameUUIDFromBytes("goetybossservants:warden_core_listener".getBytes()), "warden_core_listener"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
            this.heard.add(message.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : message.getString());
        }
    }

    // Moves the real (client) player to look at {@code target} from the given offset, in spectator mode.
    private static void view(BlockPos target, double dx, double dy, double dz) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer viewer = players.get(0);
        viewer.setGameMode(GameType.SPECTATOR);
        level.setDayTime(6000);
        double x = target.getX() + 0.5 + dx;
        double y = target.getY() + dy;
        double z = target.getZ() + 0.5 + dz;
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI) + 180.0f;
        float pitch = (float) (Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        viewer.teleportTo(level, x, y - viewer.getEyeHeight(), z, yaw, pitch);
    }
}
