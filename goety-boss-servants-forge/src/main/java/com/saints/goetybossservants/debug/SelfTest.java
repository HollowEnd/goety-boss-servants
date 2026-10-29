package com.saints.goetybossservants.debug;

import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.compat.ModCompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only end-to-end check, run with -Dgoetybossservants.selftest=true on a dedicated dev server
 * (never shipped: the jar task excludes this package). Builds each mold, summons with a fake player,
 * drives the servant through Goety's command paths, kills it and revives it from its head, then
 * stops the server. Results are logged with a "SELFTEST" prefix.
 */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID)
public class SelfTest {
    static final boolean ENABLED = Boolean.getBoolean(GoetyBossServants.MOD_ID + ".selftest");
    private static final int FINISH = BookChecks.START + BookChecks.LENGTH;
    private static int skipTo;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> failures = new ArrayList<>();
    private static final List<String> results = new ArrayList<>();
    private static int tick = -1;
    private static FakePlayer player;
    private static ServerLevel level;

    static void check(boolean ok, String what) {
        LOGGER.info("SELFTEST {} {}", ok ? "PASS" : "FAIL", what);
        results.add((ok ? "PASS " : "FAIL ") + what);
        if (!ok) {
            failures.add(what);
        }
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!ENABLED) {
            return;
        }
        // A fake player has no Goety research; the research gate itself is plain Goety code.
        com.saints.goetybossservants.config.ServantConfig.MiraMonstrosityNeedsResearch.set(false);
        // The test summons three at once; the default per-player limit is 2.
        com.saints.goetybossservants.config.ServantConfig.MiraMonstrosityPlayerLimit.set(3);
        com.saints.goetybossservants.config.ServantConfig.MaterialMonstrosityNeedsResearch.set(false);
        level = event.getServer().overworld();
        player = FakePlayerFactory.getMinecraft(level);
        tick = 0;
        // Setup (ticks 0 and 1: the Mira molds) always runs; then optionally jump to one boss's checks.
        skipTo = Integer.getInteger(GoetyBossServants.MOD_ID + ".selftest.from", 0);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tick < 0) {
            return;
        }
        try {
            step(event.getServer());
        } catch (Throwable t) {
            LOGGER.error("SELFTEST FAIL exception at tick {}", tick, t);
            failures.add("exception: " + t);
            java.io.StringWriter trace = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(trace));
            results.add("FAIL exception at tick " + tick + ": " + trace);
            finish(event.getServer());
            return;
        }
        ++tick;
    }

    // The game time the client's player was first seen in the world (-1: not yet).
    private static long joinedAt = -1;

    private static boolean waitForClient(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            joinedAt = -1;
            return true;
        }
        if (joinedAt < 0) {
            joinedAt = level.getGameTime();
        }
        return level.getGameTime() - joinedAt < 60;
    }

    private static void step(MinecraftServer server) {
        if (tick == 2 && skipTo > 2) {
            // (Only once the client has joined and settled: the checks skipped to may look at it.)
            if (waitForClient(server)) {
                --tick;
                return;
            }
            tick = skipTo;
        }
        if (tick == FINISH) {
            finish(server);
            return;
        }
        if (tick == 0) {
            OptionalModChecks.check(level);
        }
        // Each boss mod's checks only run when it's loaded (-PwithoutMods leaves some out).
        if (tick >= BookChecks.START) {
            BookChecks.step(level, tick - BookChecks.START);
        } else if (tick >= WardenCoreChecks.START) {
            WardenCoreChecks.step(level, player, tick - WardenCoreChecks.START);
        } else if (tick >= RiseChecks.START) {
            RiseChecks.step(level, player, tick - RiseChecks.START);
        } else if (tick >= MonstrosityChecks.START) {
            MonstrosityChecks.step(level, player, tick - MonstrosityChecks.START);
        } else if (tick >= NethermanWardenChecks.START) {
            if (ModCompat.knightquest()) {
                NethermanWardenChecks.step(level, player, tick - NethermanWardenChecks.START);
            }
        } else if (tick >= NethermanChecks.START) {
            if (ModCompat.knightquest()) {
                NethermanChecks.step(level, player, tick - NethermanChecks.START);
            }
        } else if (tick >= SoulslikeChecks.START) {
            if (ModCompat.soulslike()) {
                SoulslikeChecks.step(level, player, tick - SoulslikeChecks.START);
            }
        } else if (tick >= NightLichChecks.START) {
            if (ModCompat.bmd()) {
                NightLichChecks.step(level, player, tick - NightLichChecks.START);
            }
        } else if (tick >= VoidBlossomChecks.START) {
            if (ModCompat.bmd()) {
                VoidBlossomChecks.step(level, player, tick - VoidBlossomChecks.START);
            }
        } else if (tick >= CorneliaChecks.START) {
            if (ModCompat.aquamirae()) {
                CorneliaChecks.step(level, player, tick - CorneliaChecks.START);
            }
        } else if (ModCompat.meowhem()) {
            MiraChecks.step(level, player, tick - MiraChecks.START);
        }
    }

    private static void finish(MinecraftServer server) {
        tick = -10_000;
        LOGGER.info("SELFTEST DONE: {} failure(s) {}", failures.size(), failures);
        results.add("DONE: " + failures.size() + " failure(s)");
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("selftest_result.txt"), results);
        } catch (java.io.IOException e) {
            LOGGER.error("SELFTEST could not write results", e);
        }
        // The test runs in a dev client (see ClientSelfTest); just end the process.
        Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
    }
}
