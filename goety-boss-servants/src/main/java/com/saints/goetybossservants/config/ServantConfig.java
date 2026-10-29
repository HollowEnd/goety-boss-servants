package com.saints.goetybossservants.config;

import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ServantConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue CaptainCorneliaSummonLimit;
    public static final ForgeConfigSpec.DoubleValue CaptainCorneliaFollowRange;
    public static final ForgeConfigSpec.DoubleValue CaptainCorneliaShatterbladeMinHealth;
    public static final ForgeConfigSpec.DoubleValue CaptainCorneliaSaberBelow;
    public static final ForgeConfigSpec.DoubleValue CaptainCorneliaSaberUntil;
    public static final ForgeConfigSpec.IntValue CaptainCorneliaPhaseResetSeconds;
    public static final ForgeConfigSpec.IntValue VoidBlossomSummonLimit;
    public static final ForgeConfigSpec.IntValue VoidBlossomFlowerSeconds;
    public static final ForgeConfigSpec.BooleanValue BossesBreakBlocks;
    public static final ForgeConfigSpec.IntValue NightLichSummonLimit;
    public static final ForgeConfigSpec.IntValue NightLichMaxMinions;
    public static final ForgeConfigSpec.IntValue OldChampionSummonLimit;
    public static final ForgeConfigSpec.IntValue ReturningKnightSummonLimit;
    public static final ForgeConfigSpec.IntValue ReturningKnightMaxMinions;
    public static final ForgeConfigSpec.IntValue NethermanSummonLimit;
    public static final ForgeConfigSpec.IntValue NethermanMaxClones;
    public static final ForgeConfigSpec.BooleanValue DebugHealing;
    public static final ForgeConfigSpec.BooleanValue DebugTargeting;
    public static final ForgeConfigSpec.IntValue MaterialMonstrosityPlayerLimit;
    public static final ForgeConfigSpec.IntValue MaterialMonstrosityGlobalLimit;
    public static final ForgeConfigSpec.BooleanValue MaterialMonstrosityNeedsResearch;
    public static final ForgeConfigSpec.DoubleValue MaterialMonstrosityFollowRange;
    public static final Map<MonstrosityMaterial, MaterialStats> MaterialMonstrosityStats = new EnumMap<>(MonstrosityMaterial.class);
    public static final ForgeConfigSpec.IntValue IronMonstrosityMaxGolems;
    public static final ForgeConfigSpec.IntValue IronMonstrosityGolemSeconds;
    public static final ForgeConfigSpec.IntValue GoldMonstrosityGildSeconds;
    public static final ForgeConfigSpec.IntValue LapisMonstrosityBuffSeconds;
    public static final ForgeConfigSpec.IntValue NetheriteMonstrositySunderSeconds;
    public static final ForgeConfigSpec.IntValue NetheriteMonstrosityMinionSeconds;
    public static final ForgeConfigSpec.IntValue SculkDecaySeconds;
    public static final ForgeConfigSpec.DoubleValue SculkDecayDamage;
    public static final ForgeConfigSpec.IntValue SculkDecayMaxStacks;
    public static final ForgeConfigSpec.IntValue SculkDecayStacksPerHit;
    public static final ForgeConfigSpec.IntValue SculkMireSeconds;
    public static final ForgeConfigSpec.IntValue SculkZombieSeconds;
    public static final ForgeConfigSpec.IntValue SculkMaxZombies;
    public static final ForgeConfigSpec.EnumValue<RiseMode> SculkRiseMode;
    public static final ForgeConfigSpec.DoubleValue SculkRisenMaxHealth;
    public static final ForgeConfigSpec.BooleanValue SculkPuppetTicking;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SculkRiseStripKeys;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SculkRiseKeepKeys;

    /** How mobs that die with Sculk Decay rise (see RisenMobs). */
    public enum RiseMode {
        AUTO, PUPPET
    }

    /** Health, damage and armor of one material monstrosity. */
    public record MaterialStats(ForgeConfigSpec.DoubleValue health, ForgeConfigSpec.DoubleValue damage, ForgeConfigSpec.DoubleValue armor) {
    }


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        BossesBreakBlocks = builder.comment("Whether boss servants (Night Lich comets, the Returning Knight's block smashing, and later bosses) can break blocks. Their attacks still hurt enemies either way, Default: false")
                .define("bossesBreakBlocks", false);
        builder.pop();

        builder.push("material_monstrosities");
        // Goety's Redstone Monstrosity has 600 health, 21 damage and no armor; the defaults scale around it.
        MaterialMonstrosityPlayerLimit = builder.comment("How many monstrosities of each material (Copper, Iron, ...) a single player can have, Default: 2")
                .defineInRange("playerLimit", 2, 0, Integer.MAX_VALUE);
        MaterialMonstrosityGlobalLimit = builder.comment("How many monstrosities of each material can exist in a world in total, Default: 6")
                .defineInRange("globalLimit", 6, 0, Integer.MAX_VALUE);
        MaterialMonstrosityNeedsResearch = builder.comment("Whether building one from its mold requires the Terminus research, same as the Redstone Monstrosity, Default: true")
                .define("needsTerminusResearch", true);
        MaterialMonstrosityFollowRange = builder.comment("How far away they notice enemies, Default: 32 (same as the Redstone Monstrosity)")
                .defineInRange("followRange", 32.0, 1.0, 2048.0);
        IronMonstrosityMaxGolems = builder.comment("Most Iron Golems one Iron Monstrosity can have out at once (it summons 2 per cast; at the limit it slams instead), Default: 4")
                .defineInRange("ironMaxGolems", 4, 0, 64);
        IronMonstrosityGolemSeconds = builder.comment("How long the Iron Monstrosity's golems last, in seconds, Default: 60")
                .defineInRange("ironGolemSeconds", 60, 1, 3600);
        GoldMonstrosityGildSeconds = builder.comment("How long the Gold Monstrosity turns enemies to gold (stunned), in seconds, Default: 4")
                .defineInRange("goldGildSeconds", 4, 1, 60);
        LapisMonstrosityBuffSeconds = builder.comment("How long the Lapis Monstrosity's enchantment buffs last, in seconds, Default: 60")
                .defineInRange("lapisBuffSeconds", 60, 1, 3600);
        NetheriteMonstrositySunderSeconds = builder.comment("How long enemies the Netherite Monstrosity lands on stay Sundered (half armor), in seconds, Default: 10")
                .defineInRange("netheriteSunderSeconds", 10, 1, 60);
        NetheriteMonstrosityMinionSeconds = builder.comment("How long the half-size monstrosities the Netherite Monstrosity calls up last, in seconds, Default: 60")
                .defineInRange("netheriteMinionSeconds", 60, 1, 3600);
        SculkDecaySeconds = builder.comment("How long Sculk Decay lasts after the Sculk Monstrosity's last hit, in seconds, Default: 8")
                .defineInRange("sculkDecaySeconds", 8, 1, 60);
        SculkDecayDamage = builder.comment("Damage Sculk Decay deals every second for each stack, Default: 1.0")
                .defineInRange("sculkDecayDamage", 1.0, 0.0, 100.0);
        SculkDecayMaxStacks = builder.comment("Most stacks of Sculk Decay one entity can carry, Default: 10")
                .defineInRange("sculkDecayMaxStacks", 10, 1, 100);
        SculkDecayStacksPerHit = builder.comment("Stacks of Sculk Decay each of the Sculk Monstrosity's attacks adds (its sculk mires add 1 a second), Default: 2")
                .defineInRange("sculkDecayStacksPerHit", 2, 1, 100);
        SculkMireSeconds = builder.comment("How long the sculk the Sculk Monstrosity's attacks leave on the ground lasts, in seconds, Default: 5")
                .defineInRange("sculkMireSeconds", 5, 1, 60);
        SculkZombieSeconds = builder.comment("How long a mob that dies with Sculk Decay walks again (as itself or as a sculk zombie), in seconds, Default: 180")
                .defineInRange("sculkZombieSeconds", 180, 1, 3600);
        SculkMaxZombies = builder.comment("Most mobs one Sculk Monstrosity can have risen at once, as themselves or as sculk zombies (no more rise past it), Default: 8")
                .defineInRange("sculkMaxZombies", 8, 0, 64);
        SculkRiseMode = builder.comment("How mobs that die with Sculk Decay rise. AUTO: a mob with an attack of its own (and the mobs in the goetybossservants:rises_as_itself tag) rises as itself, with its own AI and attacks, and the rest as sculk zombies that look like them; PUPPET: all as sculk zombies, Default: AUTO")
                .defineEnum("sculkRiseMode", RiseMode.AUTO);
        SculkRisenMaxHealth = builder.comment("Most max health a mob risen as itself has (more is scaled down to it), Default: 200")
                .defineInRange("sculkRisenMaxHealth", 200.0, 1.0, Double.MAX_VALUE);
        SculkPuppetTicking = builder.comment("Whether the client runs the animations of the mobs sculk zombies look like (their idles, tails, swings). Turn off if a modded mob misbehaves as one; mobs in the goetybossservants:puppet_no_tick tag never run them, Default: true")
                .define("sculkPuppetTicking", true);
        SculkRiseStripKeys = builder.comment("Saved data names (regular expressions) left out when a mob rises, on top of the built-in ones: state such as a pose, mood or attack in progress that it shouldn't rise in, Default: []")
                .defineListAllowEmpty(List.of("sculkRiseStripKeys"), List::of, name -> name instanceof String);
        SculkRiseKeepKeys = builder.comment("Saved data names (regular expressions) always kept when a mob rises, even if the built-in rules or sculkRiseStripKeys would leave them out (what it carried, its owner and its AI being off are always left out), Default: []")
                .defineListAllowEmpty(List.of("sculkRiseKeepKeys"), List::of, name -> name instanceof String);
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            builder.push(material.getId());
            MaterialMonstrosityStats.put(material, new MaterialStats(
                    builder.comment("Max health, Default: " + (int) material.defaultHealth).defineInRange("health", material.defaultHealth, 1.0, Double.MAX_VALUE),
                    builder.comment("Melee damage, Default: " + (int) material.defaultDamage).defineInRange("damage", material.defaultDamage, 0.0, Double.MAX_VALUE),
                    builder.comment("Armor, Default: " + (int) material.defaultArmor).defineInRange("armor", material.defaultArmor, 0.0, 30.0)));
            builder.pop();
        }
        builder.pop();

        builder.push("captain_cornelia");
        // Health, armor, damage and speed come from Aquamirae's own Cornelia config, as on the boss.
        CaptainCorneliaSummonLimit = builder.comment("How many Captain Cornelia servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        CaptainCorneliaFollowRange = builder.comment("How far away the servant notices enemies, Default: 32 (the boss uses Aquamirae's corneliaFollowRange)")
                .defineInRange("followRange", 32.0, 1.0, 2048.0);
        CaptainCorneliaShatterbladeMinHealth = builder.comment("Targets with at least this much max health get the Shatterblade until its Health Shatter is fully stacked, Default: 150")
                .defineInRange("shatterbladeMinHealth", 150.0, 1.0, Double.MAX_VALUE);
        CaptainCorneliaSaberBelow = builder.comment("Below this fraction of her health she switches to the Remnant Saber (its hits heal her), Default: 0.5")
                .defineInRange("saberBelowHealth", 0.5, 0.0, 1.0);
        CaptainCorneliaSaberUntil = builder.comment("She keeps the Remnant Saber until healed back to this fraction, Default: 0.9")
                .defineInRange("saberUntilHealth", 0.9, 0.0, 1.0);
        CaptainCorneliaPhaseResetSeconds = builder.comment("Seconds out of combat (no target, not being hit) before her rage and last stand come back, Default: 60")
                .defineInRange("phaseResetSeconds", 60, 1, 3600);
        builder.pop();

        builder.push("void_blossom");
        VoidBlossomSummonLimit = builder.comment("How many Void Blossom servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        VoidBlossomFlowerSeconds = builder.comment("Seconds the healing flowers from its Blossom move last before they disappear (and the blocks they replaced come back), Default: 30")
                .defineInRange("flowerSeconds", 30, 1, 3600);
        builder.pop();

        builder.push("night_lich");
        NightLichSummonLimit = builder.comment("How many Night Lich servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        NightLichMaxMinions = builder.comment("Most phantom minions one Night Lich can have out at once (its rage tries to summon 9), Default: 6")
                .defineInRange("maxMinions", 6, 0, 64);
        builder.pop();

        builder.push("old_champion");
        OldChampionSummonLimit = builder.comment("How many Old Champion servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("returning_knight");
        ReturningKnightSummonLimit = builder.comment("How many Returning Knight servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        ReturningKnightMaxMinions = builder.comment("Most Remnants and Dark Sorcerers one Returning Knight can have out at once, Default: 6")
                .defineInRange("maxMinions", 6, 0, 64);
        builder.pop();

        builder.push("netherman");
        NethermanSummonLimit = builder.comment("How many Netherman servants a single player can have, Default: 1")
                .defineInRange("summonLimit", 1, 1, Integer.MAX_VALUE);
        NethermanMaxClones = builder.comment("Most clones one Netherman can have out at once (its Ice form summons Knights Quest's maxNethermanClones per cast), Default: 8")
                .defineInRange("maxClones", 8, 0, 64);
        builder.pop();

        builder.push("debug");
        DebugHealing = builder.comment("Log (to latest.log) what heals a boss servant each time its health goes up. For tracking down unexpected healing; can be switched on while the game runs. Default: false")
                .define("logHealing", false);
        DebugTargeting = builder.comment("Log (to latest.log) every change to a boss servant's target and what asked for it, plus every 2 seconds its target, owner, mode and running goals. For tracking down servants that won't fight; can be switched on while the game runs. Default: false")
                .define("logTargeting", false);
        builder.pop();
        SPEC = builder.build();
    }
}
