package com.saints.goetybossservants.entity.monstrosity;

import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.config.ServantConfig;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * What of a dead mob's saved data rises with it. Mobs save the state they died in along with what
 * they are: a dragon stunned for taming, a sitting pet, a mob part way through an attack, with its AI
 * off or angry, timers part way down. Loaded back as it was, that state comes back and nothing ever
 * clears it (a sculk zombie's copy is never told otherwise, and a mob risen as itself would rise
 * stunned or sitting). So only what the mob is (its variant, size, colours and gear, and for one
 * risen as itself its stats and name too) is kept, and the rest dropped by name: the names vanilla
 * uses, and the words mods use for such state (Sitting, TamingStunned, AttackTick, ...), unless they
 * name what it is (Variant, Color, Age, ...). The config can add names to either side.
 */
public final class RiseData {
    // Always dropped: what it carried, who owned it (see look), its AI being off, moods, poses, timers,
    // how it was moving, burning or breathing, and what it was riding or leashed to.
    private static final Set<String> GONE = Set.of("Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Team", "SleepingX", "SleepingY", "SleepingZ",
            "Brain", "ActiveEffects", "AbsorptionAmount", "FallFlying", "Leash", "DeathLootTable", "DeathLootTableSeed", "Owner", "OwnerClient",
            "Offers", "Inventory", "Gossips", "Xp", "LastRestock", "LastGossipDecay", "RestocksToday", "LifeTicks", "Items", "EnderItems",
            "AttackTick", "StunTick", "RoarTick", "Sitting", "Sleeping", "Crouching", "DrownedConversionTime", "InWaterTime", "ConversionTime",
            "StrayConversionTime", "NoAI", "Motion", "Fire", "Air", "FallDistance", "Passengers", "PortalCooldown", "AngerTime", "AngryAt",
            "InLove", "LoveCause", "forge:spawn_type", "IsPregnant", "isSwim", "isCircling", "isRunAway", "isWeak", "ignited", "UUID",
            "Invulnerable", "HasVisualFire", "TicksFrozen", "carriedBlockState", "RaidId");
    // ...and from a look, what only a live mob needs: its stats, name and drops.
    private static final Set<String> LOOK_GONE = Set.of("Attributes", "HandDropChances", "ArmorDropChances", "CustomName", "CustomNameVisible",
            "PersistenceRequired", "CanPickUpLoot");
    private static final Set<String> KEPT = Set.of("HandItems", "ArmorItems", "SourceUUID");
    // Words for what a mob is, kept whatever else the name says.
    private static final Pattern WHAT_IT_IS = Pattern.compile("(?i:variant|variation|texture|skin|colou?r|gender|female|baby|scale|pattern|saddle|armou?r"
            + "|chest|decor|collar|gene|marking)|Age|Size|Phase|Form|Type");
    // Words for state, in names (in any case)...
    private static final String[] STATE_WORDS = {"taming", "stunned", "sitting", "sleeping", "command", "flying", "takeoff", "hover", "landing",
            "landed", "rider", "leaping", "charging", "attacking", "roaring", "cooldown", "invuln", "animationstate", "animationtick",
            "animationplaying", "placeturn", "groundmovestate", "flightmode"};
    // ...and at their ends.
    private static final String[] STATE_ENDINGS = {"sit", "sitprogress", "sleep", "stun", "anim", "tick", "ticks", "timer", "timeout", "order",
            "attack", "roar", "stomp", "slash", "wave", "hidden", "hanging", "gripping", "crouching", "stalking", "running", "accelerating",
            "goingup", "goingdown", "swimming"};
    // MCreator mobs only play their idle and walk while flags their server code sets allow it, which a
    // copy never runs: its look has them on (and running and anger off), and its neck and tail aims gone.
    private static final Pattern MCREATOR_ON = Pattern.compile("Data(idle|Idle|walk|Walk)");
    private static final Pattern MCREATOR_OFF = Pattern.compile("Data(run|Run)|Data.*Angry.*");
    private static final Pattern MCREATOR_GONE = Pattern.compile("Data[A-V]1?");
    // Item data a look keeps (the rest of a much bigger item's data is dropped): enough for its glint,
    // dye, trim, banner patterns, head and loaded crossbow.
    private static final String[] ITEM_LOOK = {"Enchantments", "display", "Trim", "CustomModelData", "BlockEntityTag", "SkullOwner", "Charged", "ChargedProjectiles"};
    private static final int ITEM_TAG_BYTES = 8192;
    // A look goes to every player near it; past this much (in NBT bytes) it is left out altogether.
    private static final int LOOK_BYTES = 65536;
    private static final String MARK = GoetyBossServants.MOD_ID + ":";
    private static final String TAG_MARK = GoetyBossServants.MOD_ID + ".";

    private RiseData() {
    }

    /**
     * The part of a mob's saved data (addAdditionalSaveData) that its look needs: none of what it
     * carried, its state or stats, only the look of big items, and nothing at all if it is still too
     * big to send to every player near it. A tamed pet's owner is kept only as the empty one that just
     * makes it look tamed. (A horse's saddle and a llama's carpet don't show: those only ever come from
     * the server.) Taking the look of a look changes nothing.
     */
    public static CompoundTag look(CompoundTag saved) {
        boolean tame = saved.hasUUID("Owner") && Util.NIL_UUID.equals(saved.getUUID("Owner"));
        CompoundTag look = strip(saved, true);
        if (tame) {
            look.putUUID("Owner", Util.NIL_UUID);
        }
        // A piglin or hoglin doesn't shake as if turning into a zombie (it would outside the Nether).
        look.putBoolean("IsImmuneToZombification", true);
        for (String key : new ArrayList<>(look.getAllKeys())) {
            if (MCREATOR_GONE.matcher(key).matches()) {
                look.remove(key);
            } else if (look.getTagType(key) == Tag.TAG_BYTE && MCREATOR_ON.matcher(key).matches()) {
                look.putBoolean(key, true);
            } else if (look.getTagType(key) == Tag.TAG_BYTE && MCREATOR_OFF.matcher(key).matches()) {
                look.putBoolean(key, false);
            }
        }
        for (String list : new String[]{"HandItems", "ArmorItems"}) {
            ListTag items = look.getList(list, Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); ++i) {
                trimItem(items.getCompound(i));
            }
        }
        for (String item : new String[]{"SaddleItem", "ArmorItem", "DecorItem"}) {
            if (look.contains(item, Tag.TAG_COMPOUND)) {
                trimItem(look.getCompound(item));
            }
        }
        return look.sizeInBytes() > LOOK_BYTES ? new CompoundTag() : look;
    }

    /**
     * The part of a mob's full saved data (saveWithoutId) a mob risen as itself is loaded from: what
     * it is, its stats, gear, name and mods' data on it, but none of what it carried (nor gear it
     * picked up, which its corpse drops), its state, its UUID, or this mod's marks on it (who decayed
     * it, whose minion it was).
     */
    public static CompoundTag revive(CompoundTag saved) {
        CompoundTag tag = strip(saved, false);
        tag.getAllKeys().removeIf(key -> key.startsWith(MARK));
        if (tag.contains("ForgeData", Tag.TAG_COMPOUND)) {
            tag.getCompound("ForgeData").getAllKeys().removeIf(key -> key.startsWith(MARK));
        }
        if (tag.contains("Tags", Tag.TAG_LIST)) {
            tag.getList("Tags", Tag.TAG_STRING).removeIf(name -> name instanceof StringTag string && string.getAsString().startsWith(TAG_MARK));
        }
        // Gear the corpse is sure to drop (what it picked up) isn't kept: it would be there twice.
        clearGuaranteed(tag, "HandItems", "HandDropChances");
        clearGuaranteed(tag, "ArmorItems", "ArmorDropChances");
        return tag;
    }

    private static void clearGuaranteed(CompoundTag tag, String items, String chances) {
        ListTag gear = tag.getList(items, Tag.TAG_COMPOUND);
        ListTag drop = tag.getList(chances, Tag.TAG_FLOAT);
        for (int i = 0; i < Math.min(gear.size(), drop.size()); ++i) {
            if (drop.getFloat(i) > 1.0f) {
                gear.set(i, new CompoundTag());
            }
        }
    }

    // Its saved data without its state (and for a look, without its stats, name and drops either).
    static CompoundTag strip(CompoundTag saved, boolean forLook) {
        CompoundTag tag = saved.copy();
        List<Pattern> keep = patterns(ServantConfig.SculkRiseKeepKeys), drop = patterns(ServantConfig.SculkRiseStripKeys);
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            if (dropped(key, forLook, keep, drop)) {
                tag.remove(key);
            }
        }
        return tag;
    }

    private static boolean dropped(String key, boolean forLook, List<Pattern> keep, List<Pattern> drop) {
        if (GONE.contains(key) || forLook && LOOK_GONE.contains(key)) {
            return true;
        }
        if (KEPT.contains(key) || matches(keep, key)) {
            return false;
        }
        if (matches(drop, key)) {
            return true;
        }
        // Whatever else it carried (chests, packs: the corpse drops them).
        if (key.toLowerCase(Locale.ROOT).endsWith("items")) {
            return true;
        }
        if (WHAT_IT_IS.matcher(key).find()) {
            return false;
        }
        String name = key.toLowerCase(Locale.ROOT);
        for (String word : STATE_WORDS) {
            if (name.contains(word)) {
                return true;
            }
        }
        for (String ending : STATE_ENDINGS) {
            if (name.endsWith(ending)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(List<Pattern> patterns, String key) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(key).find()) {
                return true;
            }
        }
        return false;
    }

    // A config list of name patterns (one that isn't a valid pattern is taken as plain text).
    private static List<Pattern> patterns(ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        List<? extends String> names;
        try {
            names = value.get();
        } catch (IllegalStateException e) {
            // (Not loaded yet.)
            return List.of();
        }
        List<Pattern> patterns = new ArrayList<>(names.size());
        for (String name : names) {
            try {
                patterns.add(Pattern.compile(name));
            } catch (PatternSyntaxException e) {
                patterns.add(Pattern.compile(Pattern.quote(name)));
            }
        }
        return patterns;
    }

    // Keeps only the look of an item with a lot of data (a written book, a full shulker box...).
    private static void trimItem(CompoundTag item) {
        if (!item.contains("tag", Tag.TAG_COMPOUND) || item.getCompound("tag").sizeInBytes() <= ITEM_TAG_BYTES) {
            return;
        }
        CompoundTag tag = item.getCompound("tag");
        CompoundTag kept = new CompoundTag();
        for (String key : ITEM_LOOK) {
            Tag value = tag.get(key);
            if (key.equals("display") && value instanceof CompoundTag display) {
                if (display.contains("color")) {
                    CompoundTag colour = new CompoundTag();
                    colour.put("color", display.get("color"));
                    kept.put(key, colour);
                }
            } else if (value != null && value.sizeInBytes() <= ITEM_TAG_BYTES) {
                kept.put(key, value);
            }
        }
        item.put("tag", kept);
    }
}
