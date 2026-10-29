package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only: stages the CurseForge banner shots (gradlew runClient -Pshowcase). In the self-test's flat
 * world it builds a castle courtyard, lines the servants up in it, posed (no AI), and saves HUD-free
 * 1920x1080 shots from a few cameras at dusk and at night to run-showcase/screenshots, then quits.
 */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID)
public class Showcase {
    static final boolean ENABLED = Boolean.getBoolean(GoetyBossServants.MOD_ID + ".showcase");
    // The flat world's top layer, which becomes the courtyard floor.
    private static final int F = -61;
    private static final int SETTLE = 220, GAP = 70;

    /** A camera: eye position, pitch (looking south, along +z), field of view and time of day. */
    record Shot(String name, double x, double y, double z, float pitch, int fov, long time) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();

    static {
        // Looking a little up from low down, so the castle and sky fill the top (where the logo goes)
        // and the servants the lower part.
        long[] times = {12900, 13400};
        String[] when = {"dusk", "twilight"};
        for (int i = 0; i < times.length; ++i) {
            SHOTS.add(new Shot("hero_" + when[i], 0.5, -56.0, -15.0, -9.0f, 70, times[i]));
            SHOTS.add(new Shot("wide_" + when[i], 0.5, -55.0, -22.0, -7.0f, 66, times[i]));
            SHOTS.add(new Shot("close_" + when[i], 0.5, -57.0, -8.0, -10.0f, 76, times[i]));
        }
    }

    private static int tick = -1;

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        ServerLevel level = server.overworld();
        ServerPlayer player = players.get(0);
        ++tick;
        if (tick == 40) {
            GameRules rules = level.getGameRules();
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.setWeatherParameters(100_000, 0, false, false);
            List<Entity> others = new ArrayList<>();
            level.getAllEntities().forEach(entity -> {
                if (!(entity instanceof Player)) {
                    others.add(entity);
                }
            });
            others.forEach(Entity::discard);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            look(level, player, SHOTS.get(0));
            build(level);
            ClientSelfTest.prepareShowcase();
        }
        if (tick == 60) {
            spawnAll(level);
        }
        int shot = (tick - SETTLE) / GAP, at = (tick - SETTLE) % GAP;
        if (tick >= SETTLE && shot < SHOTS.size()) {
            if (at == 0) {
                look(level, player, SHOTS.get(shot));
            } else if (at == GAP - 10) {
                ClientSelfTest.capture(SHOTS.get(shot).name(), 1920, 1080, SHOTS.get(shot).fov());
            }
        } else if (tick >= SETTLE && at == 30) {
            Runtime.getRuntime().halt(0);
        }
    }

    private static void look(ServerLevel level, ServerPlayer player, Shot shot) {
        level.setDayTime(shot.time());
        player.teleportTo(level, shot.x(), shot.y() - player.getEyeHeight(), shot.z(), 0.0f, shot.pitch());
    }

    // ----- The courtyard ----------------------------------------------------------------------

    private static ServerLevel at;
    private static final RandomSource RANDOM = RandomSource.create(7);

    private static void set(int x, int y, int z, BlockState state) {
        at.setBlock(new BlockPos(x, y, z), state, 2);
    }

    private static void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
                    set(x, y, z, state);
                }
            }
        }
    }

    // Weathered masonry: mostly the first block, some cracked.
    private static BlockState worn(BlockState block, BlockState cracked, float chance) {
        return RANDOM.nextFloat() < chance ? cracked : block;
    }

    private static void wall(int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); ++x) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); ++y) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); ++z) {
                    set(x, y, z, y <= F + 2 ? worn(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 0.15f)
                            : worn(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(), 0.14f));
                }
            }
        }
    }

    // Battlements along the top of a wall running along x (at z) or along z (at x).
    private static void crenelsX(int x0, int x1, int y, int z) {
        for (int x = x0; x <= x1; ++x) {
            if (Math.floorMod(x, 2) == 0) {
                set(x, y, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
        }
    }

    private static void crenelsZ(int z0, int z1, int y, int x) {
        for (int z = z0; z <= z1; ++z) {
            if (Math.floorMod(z, 2) == 0) {
                set(x, y, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
        }
    }

    // A lit window: stained glass with a froglight behind it (deeper in the wall, along +z or -z).
    private static void window(int x, int y0, int y1, int z, int inward) {
        for (int y = y0; y <= y1; ++y) {
            set(x, y, z, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
            set(x, y, z + inward, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
        }
    }

    private static void tower(int cx, int z0, int top) {
        wall(cx - 3, F + 1, z0, cx + 3, top, z0 + 6);
        for (int y = F + 1; y <= top; ++y) {
            for (int dx : new int[]{-3, 3}) {
                set(cx + dx, y, z0, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
        for (int x = cx - 3; x <= cx + 3; ++x) {
            if (Math.floorMod(x - cx, 2) == 1 || Math.abs(x - cx) == 3) {
                set(x, top + 1, z0, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                set(x, top + 1, z0 + 6, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
            }
        }
        crenelsZ(z0, z0 + 6, top + 1, cx - 3);
        crenelsZ(z0, z0 + 6, top + 1, cx + 3);
        window(cx, top - 14, top - 11, z0, 1);
        window(cx, top - 6, top - 4, z0, 1);
        // Soul lanterns hung from brackets on its face.
        for (int dx : new int[]{-2, 2}) {
            set(cx + dx, top - 8, z0 - 1, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            set(cx + dx, top - 9, z0 - 1, Blocks.CHAIN.defaultBlockState());
            set(cx + dx, top - 10, z0 - 1, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private static void brazier(int x, int z) {
        set(x, F + 1, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        set(x, F + 2, z, Blocks.SOUL_SOIL.defaultBlockState());
        set(x, F + 3, z, Blocks.SOUL_FIRE.defaultBlockState());
    }

    private static void build(ServerLevel level) {
        at = level;
        box(-56, F + 1, -40, 56, F + 40, 44, Blocks.AIR.defaultBlockState());
        // The floor: deepslate tiles, worn; a blackstone aisle up the middle to the gate.
        for (int x = -56; x <= 56; ++x) {
            for (int z = -40; z <= 44; ++z) {
                BlockState floor;
                if (Math.abs(x) <= 2) {
                    floor = worn(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 0.12f);
                } else if (Math.abs(x) == 3) {
                    floor = Math.floorMod(z, 4) == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                } else {
                    float roll = RANDOM.nextFloat();
                    floor = roll < 0.16f ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                            : roll < 0.26f ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                            : roll < 0.30f ? Blocks.COBBLED_DEEPSLATE.defaultBlockState()
                            : Blocks.DEEPSLATE_TILES.defaultBlockState();
                }
                set(x, F, z, floor);
            }
        }

        // The back wall, with the gatehouse in the middle and towers along it.
        wall(-56, F + 1, 34, 56, F + 17, 36);
        crenelsX(-56, 56, F + 18, 34);
        crenelsX(-56, 56, F + 18, 36);
        for (int x : new int[]{-44, -40, -22, 22, 40, 44}) {
            window(x, F + 7, F + 11, 34, 1);
        }
        for (int x : new int[]{-26, -9, 9, 26}) {
            set(x, F + 9, 33, Blocks.PURPLE_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.NORTH));
        }
        for (int x : new int[]{-30, -18, 18, 30}) {
            set(x, F + 13, 33, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            set(x, F + 12, 33, Blocks.CHAIN.defaultBlockState());
            set(x, F + 11, 33, Blocks.CHAIN.defaultBlockState());
            set(x, F + 10, 33, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }

        // The gatehouse: a tall arch glowing with soul fire, crying obsidian at the back.
        wall(-7, F + 1, 33, 7, F + 23, 38);
        crenelsX(-7, 7, F + 24, 33);
        crenelsX(-7, 7, F + 24, 38);
        box(-3, F + 1, 33, 3, F + 9, 38, Blocks.AIR.defaultBlockState());
        box(-2, F + 10, 33, 2, F + 10, 38, Blocks.AIR.defaultBlockState());
        box(-1, F + 11, 33, 1, F + 11, 38, Blocks.AIR.defaultBlockState());
        box(-4, F + 1, 32, -4, F + 10, 32, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        box(4, F + 1, 32, 4, F + 10, 32, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        box(-3, F + 11, 32, -2, F + 11, 32, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        box(2, F + 11, 32, 3, F + 11, 32, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        box(-1, F + 12, 32, 1, F + 12, 32, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
        set(0, F + 12, 32, Blocks.GILDED_BLACKSTONE.defaultBlockState());
        box(-3, F, 33, 3, F, 39, Blocks.SOUL_SOIL.defaultBlockState());
        box(-3, F + 1, 39, 3, F + 12, 39, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        for (int x = -3; x <= 3; ++x) {
            for (int z = 36; z <= 38; ++z) {
                set(x, F + 1, z, Blocks.SOUL_FIRE.defaultBlockState());
            }
        }
        window(-4, F + 16, F + 19, 33, 1);
        window(4, F + 16, F + 19, 33, 1);

        tower(-15, 31, F + 26);
        tower(15, 31, F + 26);
        tower(-36, 31, F + 22);
        tower(36, 31, F + 22);

        // The side walls, closing the courtyard in.
        for (int side : new int[]{-1, 1}) {
            int x = side * 54;
            wall(x - 2, F + 1, -40, x + 2, F + 13, 36);
            crenelsZ(-40, 36, F + 14, x - side * 2);
        }
        for (int side : new int[]{-1, 1}) {
            for (int z = -32; z <= 28; z += 12) {
                int x = side * 52;
                for (int y = F + 5; y <= F + 9; ++y) {
                    set(x, y, z, Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
                    set(x + side, y, z, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
                }
            }
        }

        // Soul-fire braziers (no smoke) by the aisle.
        for (int[] spot : new int[][]{{4, 14}, {4, 26}, {9, -4}}) {
            brazier(spot[0], spot[1]);
            brazier(-spot[0], spot[1]);
        }
        // Hidden light over where the servants stand, so they show at dusk.
        for (int x = -42; x <= 42; x += 6) {
            for (int z = -2; z <= 26; z += 7) {
                set(x, F + 5, z, Blocks.LIGHT.defaultBlockState());
            }
        }
    }

    // ----- The servants -----------------------------------------------------------------------

    private static void spawnAll(ServerLevel level) {
        // Back row: the monstrosities (screen left to right is +x to -x, looking south).
        double[] back = {38.5, 27.5, 16.5, 5.5, -5.5, -16.5, -27.5, -38.5};
        List<String> backRow = new ArrayList<>();
        for (MonstrosityMaterial material : new MonstrosityMaterial[]{MonstrosityMaterial.COPPER, MonstrosityMaterial.GOLD, MonstrosityMaterial.IRON,
                MonstrosityMaterial.NETHERITE, MonstrosityMaterial.SCULK, MonstrosityMaterial.DIAMOND, MonstrosityMaterial.LAPIS}) {
            backRow.add(id(ModRegistry.MONSTROSITIES.get(material)));
        }
        backRow.add(id(ModRegistry.MIRA_MONSTROSITY));
        for (int i = 0; i < back.length; ++i) {
            spawn(level, backRow.get(i), back[i], 22.0);
        }
        // Front row, in the gaps: the bosses, the tallest at the sides (the logo goes over the middle).
        double[] front = {18.0, 12.0, 6.0, -6.0, -12.0, -18.0};
        String[] frontRow = {id(ModRegistry.RETURNING_KNIGHT), id(ModRegistry.NETHERMAN) + "{shouldPlaySummonAnimation:0b}", id(ModRegistry.CAPTAIN_CORNELIA),
                id(ModRegistry.OLD_CHAMPION), id(ModRegistry.NIGHT_LICH), id(ModRegistry.VOID_BLOSSOM)};
        for (int i = 0; i < front.length; ++i) {
            spawn(level, frontRow[i], front[i], 5.0);
        }
    }

    @org.jetbrains.annotations.Nullable
    private static String id(@org.jetbrains.annotations.Nullable RegistryObject<? extends EntityType<?>> type) {
        return type == null || !type.isPresent() ? null : String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(type.get()));
    }

    // Spawns a servant standing on the floor, turned to the cameras, posed: no AI, silent, kept.
    private static void spawn(ServerLevel level, @org.jetbrains.annotations.Nullable String spec, double x, double z) {
        if (spec == null || spec.startsWith("null")) {
            return;
        }
        try {
            int brace = spec.indexOf('{');
            CompoundTag tag = brace < 0 ? new CompoundTag() : net.minecraft.nbt.TagParser.parseTag(spec.substring(brace));
            tag.putString("id", brace < 0 ? spec : spec.substring(0, brace));
            tag.putBoolean("NoAI", true);
            tag.putBoolean("Silent", true);
            tag.putBoolean("PersistenceRequired", true);
            float yaw = (float) Math.toDegrees(Math.atan2(-(0.5 - x), -15.0 - z));
            Entity entity = EntityType.loadEntityRecursive(tag, level, e -> {
                e.moveTo(x, F + 1, z, yaw, 0.0f);
                e.setYHeadRot(yaw);
                e.setYBodyRot(yaw);
                return e;
            });
            if (entity != null) {
                level.addFreshEntity(entity);
            }
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(spec, e);
        }
    }
}
