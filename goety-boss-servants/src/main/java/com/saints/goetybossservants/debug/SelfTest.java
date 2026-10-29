package com.saints.goetybossservants.debug;

import com.mojang.logging.LogUtils;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.compat.ModCompat;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only end-to-end check, run with gradlew runClient -Pselftest (never shipped: the jar task
 * excludes this package). Creates a flat world (ClientSelfTest), runs each boss's checks for the
 * boss mods that are loaded, writes run-selftest/selftest_result.txt and exits.
 */
public class SelfTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FINISH = BookChecks.START + BookChecks.LENGTH;
    private static final List<String> failures = new ArrayList<>();
    private static final List<String> results = new ArrayList<>();
    private static int tick = -1;
    private static int skipTo;
    private static ServerLevel level;
    private static FakePlayer player;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            level = server.overworld();
            player = FakePlayer.get(level);
            // A fake player has no Goety research; the research gate itself is plain Goety code.
            com.saints.goetybossservants.config.ServantConfig.MaterialMonstrosityNeedsResearch.set(false);
            tick = 0;
            skipTo = Integer.getInteger(GoetyBossServants.MOD_ID + ".selftest.from", 0);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (tick < 0) {
                return;
            }
            try {
                step();
            } catch (Throwable t) {
                LOGGER.error("SELFTEST FAIL exception at tick {}", tick, t);
                failures.add("exception: " + t);
                java.io.StringWriter trace = new java.io.StringWriter();
                t.printStackTrace(new java.io.PrintWriter(trace));
                results.add("FAIL exception at tick " + tick + ": " + trace);
                finish();
                return;
            }
            ++tick;
        });
    }

    static void check(boolean ok, String what) {
        LOGGER.info("SELFTEST {} {}", ok ? "PASS" : "FAIL", what);
        results.add((ok ? "PASS " : "FAIL ") + what);
        if (!ok) {
            failures.add(what);
        }
    }

    // The game time the client's player was first seen in the world (-1: not yet).
    private static long joinedAt = -1;

    private static boolean waitForClient() {
        if (level.getServer().getPlayerList().getPlayers().isEmpty()) {
            joinedAt = -1;
            return true;
        }
        if (joinedAt < 0) {
            joinedAt = level.getGameTime();
        }
        return level.getGameTime() - joinedAt < 60;
    }

    private static void step() {
        if (tick == 1 && skipTo > 1) {
            // (Only once the client has joined and settled: the checks skipped to may look at it.)
            if (waitForClient()) {
                --tick;
                return;
            }
            tick = skipTo;
        }
        if (tick == FINISH) {
            finish();
            return;
        }
        if (tick == 0) {
            OptionalModChecks.check(level);
        }
        // Each boss mod's checks only run when it's loaded (-PwithoutMods leaves some out).
        if (tick >= CorneliaChecks.START && tick < CorneliaChecks.START + CorneliaChecks.LENGTH && ModCompat.aquamirae()) {
            CorneliaChecks.step(level, player, tick - CorneliaChecks.START);
        }
        if (tick >= VoidBlossomChecks.START && tick < VoidBlossomChecks.START + VoidBlossomChecks.LENGTH && ModCompat.bmd()) {
            VoidBlossomChecks.step(level, player, tick - VoidBlossomChecks.START);
        }
        if (tick >= NightLichChecks.START && tick < NightLichChecks.START + NightLichChecks.LENGTH && ModCompat.bmd()) {
            NightLichChecks.step(level, player, tick - NightLichChecks.START);
        }
        if (tick >= SoulslikeChecks.START && tick < SoulslikeChecks.START + SoulslikeChecks.LENGTH && ModCompat.soulslike()) {
            SoulslikeChecks.step(level, player, tick - SoulslikeChecks.START);
        }
        if (tick >= NethermanChecks.START && tick < NethermanChecks.START + NethermanChecks.LENGTH && ModCompat.knightquest()) {
            NethermanChecks.step(level, player, tick - NethermanChecks.START);
        }
        if (tick >= TargetingChecks.START && tick < TargetingChecks.START + TargetingChecks.LENGTH) {
            TargetingChecks.step(level, player, tick - TargetingChecks.START);
        }
        if (tick >= NethermanWardenChecks.START && tick < NethermanWardenChecks.START + NethermanWardenChecks.LENGTH && ModCompat.knightquest()) {
            NethermanWardenChecks.step(level, player, tick - NethermanWardenChecks.START);
        }
        if (tick >= MonstrosityChecks.START && tick < MonstrosityChecks.START + MonstrosityChecks.LENGTH) {
            MonstrosityChecks.step(level, player, tick - MonstrosityChecks.START);
        }
        if (tick >= RiseChecks.START && tick < RiseChecks.START + RiseChecks.LENGTH) {
            RiseChecks.step(level, player, tick - RiseChecks.START);
        }
        if (tick >= WardenCoreChecks.START && tick < WardenCoreChecks.START + WardenCoreChecks.LENGTH) {
            WardenCoreChecks.step(level, player, tick - WardenCoreChecks.START);
        }
        if (tick >= BookChecks.START) {
            BookChecks.step(level, tick - BookChecks.START);
        }
    }

    private static void finish() {
        tick = -10_000;
        LOGGER.info("SELFTEST DONE: {} failure(s) {}", failures.size(), failures);
        results.add("DONE: " + failures.size() + " failure(s)");
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("selftest_result.txt"), results);
        } catch (java.io.IOException e) {
            LOGGER.error("SELFTEST could not write results", e);
        }
        // The test runs in a dev client; just end the process.
        Runtime.getRuntime().halt(failures.isEmpty() ? 0 : 1);
    }
}
