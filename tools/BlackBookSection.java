import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes this mod's section of Goety's Black Book (a Patchouli book) for both loaders. Patchouli
 * only reads a book's pages from its own namespace, so they go under assets/goety/patchouli_books/
 * black_book/en_us/{categories,entries}/goetybossservants/. The boss servants' entity and recipe ids
 * differ between the loaders, and the Mira Monstrosity is Forge only.
 *
 * Every page naming a boss mod's entity, item or recipe sits in an entry flagged "mod:<id>": Patchouli
 * then never builds it without that mod (an unknown entity would empty the whole Black Book). The
 * monstrosity entries are locked behind the Terminus scroll and secret, like Goety's Redstone
 * Monstrosity. Molds are inline multiblocks drawn from the same offsets as MonstrosityMoldShape.
 *
 * Usage: java BlackBookSection.java <forge resources dir> <fabric resources dir>
 */
public class BlackBookSection {
    static final String NS = "goetybossservants";
    static final String CATEGORY = "goety:goetybossservants";
    static final String BOSSES = CATEGORY + "/bosses";
    static final String MONSTROSITIES = CATEGORY + "/monstrosities";
    static final String TERMINUS = "goety:goety/read_terminus_scroll";

    record Loader(String name, boolean mira, Map<String, String> entity, Map<String, String> recipe) {
    }

    static final Loader FORGE = new Loader("forge", true,
            Map.of("cornelia", "captain_cornelia", "void_blossom", "void_blossom", "night_lich", "night_lich", "old_champion", "old_champion",
                    "returning_knight", "returning_knight", "netherman", "netherman"),
            Map.of("cornelia", "captain_cornelia", "void_blossom", "void_blossom", "night_lich", "night_lich", "old_champion", "old_champion",
                    "returning_knight", "returning_knight", "netherman", "netherman"));
    // Fabric kept its first ids so existing worlds carry over.
    static final Loader FABRIC = new Loader("fabric", false,
            Map.of("cornelia", "captain_cornelia_servant", "void_blossom", "void_blossom_servant", "night_lich", "lich_servant",
                    "old_champion", "draugr_boss_servant", "returning_knight", "returning_knight_servant", "netherman", "netherman_servant"),
            Map.of("cornelia", "captain_cornelia_servant", "void_blossom", "void_blossom_servant", "night_lich", "lich_servant",
                    "old_champion", "draugr_boss_servant", "returning_knight", "returning_knight_servant", "netherman", "netherman_servant"));

    public static void main(String[] args) throws Exception {
        write(FORGE, new File(args[0]));
        write(FABRIC, new File(args[1]));
    }

    static void write(Loader loader, File resources) throws Exception {
        File book = new File(resources, "assets/goety/patchouli_books/black_book/en_us");
        File categories = new File(book, "categories/" + NS);
        File entries = new File(book, "entries/" + NS);
        deleteTree(categories);
        deleteTree(entries);
        new File(book, "categories/" + NS + ".json").delete();

        // The section, and its two halves.
        json(new File(book, "categories/" + NS + ".json"), obj(
                "name", "Goety Boss Servants",
                "description", "Bosses of other worlds bound to serve a Dark Wizard, and Goety's Redstone Monstrosity cast in metals other than redstone.",
                "icon", NS + ":textures/gui/book_icon.png",
                "sortnum", 12));
        List<String> bossMods = new ArrayList<>(List.of("mod:aquamirae", "mod:bosses_of_mass_destruction", "mod:soulsweapons", "mod:knightquest"));
        if (loader.mira) {
            bossMods.add(0, "mod:meowhem");
        }
        json(new File(categories, "bosses.json"), obj(
                "name", "Bound Bosses",
                "description", "Bosses from other mods, bound to your will. Each appears here only when its own mod is installed.",
                "icon", "goety:howling_soul",
                "parent", CATEGORY,
                // Shown only with a boss mod installed (its entries are flagged one by one).
                "flag", "|" + String.join(",", bossMods),
                "sortnum", 1));
        json(new File(categories, "monstrosities.json"), obj(
                "name", "Material Monstrosities",
                "description", "The Redstone Monstrosity's mold, filled with copper, iron, gold, lapis, diamond, netherite or sculk.",
                "icon", "goety:animation_core",
                "parent", CATEGORY,
                "sortnum", 2));

        introduction(loader, entries);
        bosses(loader, entries);
        monstrosities(entries);
    }

    // ----- The section's first entry ----------------------------------------------------------

    static void introduction(Loader loader, File entries) throws Exception {
        String needs = (loader.mira ? "$(li)Mira Monstrosity ($(italic)meowhem$())" : "")
                + "$(li)Captain Cornelia ($(italic)Aquamirae$())"
                + "$(li)Void Blossom and Night Lich ($(italic)Bosses of Mass Destruction$())"
                + "$(li)Old Champion and Returning Knight ($(italic)Marium's Soulslike Weaponry$())"
                + "$(li)Netherman ($(italic)Knights Quest$())";
        json(new File(entries, "introduction.json"), obj(
                "name", "Boss Servants",
                "icon", "goety:dark_wand",
                "category", CATEGORY,
                "priority", true,
                "read_by_default", true,
                "sortnum", 0,
                "pages", List.of(
                        text("With the right ritual, a boss of another world can be called up to serve you, and Goety's Redstone Monstrosity "
                                + "can be cast in metals other than redstone.$(br2)Each boss comes from its own mod, and its pages appear in this "
                                + "book only when that mod is installed."),
                        text("$(bold)Who needs what$()$(br)" + needs + "$(br2)The seven $(l:" + NS + "/monstrosities/material_monstrosities)Material "
                                + "Monstrosities$() need nothing but Goety.", "Bound Bosses"),
                        text("Bound bosses are servants like any other. The Dark Wand orders them to follow, wander, stay or guard; they fight "
                                + "whatever you fight, and never turn on you, your allies or your pets.$(br2)They break no blocks unless the server "
                                + "allows it, and only one of each boss summoned by ritual will serve you at a time"
                                + (loader.mira ? "; the Mira Monstrosity, built in a mold, may serve two at once." : "."), "Servitude"),
                        text("A fallen boss summoned by ritual leaves its soul behind. Set the soul on a Dark Altar as the activation item, with "
                                + "nothing on the pedestals, and the boss rises again for 500 souls.$(br2)It then serves whoever performed the ritual."
                                + (loader.mira ? "$(br2)The Mira Monstrosity leaves its head instead, as the monstrosities do." : ""), "Souls"))));
    }

    // ----- Bound bosses ------------------------------------------------------------------------

    record Boss(String key, String name, String mod, String modName, String icon, String soul, String lore, String ritual, String servant,
                String soulText, String renderNbt) {
        Boss(String key, String name, String mod, String modName, String icon, String soul, String lore, String ritual, String servant,
             String soulText) {
            this(key, name, mod, modName, icon, soul, lore, ritual, servant, soulText, "");
        }
    }

    static void bosses(Loader loader, File entries) throws Exception {
        List<Boss> bosses = List.of(
                new Boss("cornelia", "Captain Cornelia", "aquamirae", "Aquamirae", "aquamirae:frozen_key", "captain_cornelia_soul",
                        "The drowned captain of the frozen seas, called back from the deep to sail under your banner.",
                        "Summoned with a $(l:rituals/deep_ritual)Deep Ritual$(): the Frozen Key on the altar, and a full set of Salvager armor "
                                + "on the pedestals.",
                        "She fights as she always has, hooking foes in, then swinging and thrusting, growing enraged as the fight wears on. "
                                + "She picks her weapon for each foe: the Remnant Saber when badly hurt, her own blade when it can finish the "
                                + "foe quickly, the Shatterblade against hulking brutes.$(br2)Once each fight, she stands her ground and heals.",
                        "When she falls, she leaves her soul behind. On a Dark Altar it brings her back for 500 souls."),
                new Boss("void_blossom", "Void Blossom", "bosses_of_mass_destruction", "Bosses of Mass Destruction",
                        "bosses_of_mass_destruction:void_thorn", "void_blossom_soul",
                        "A blossom of the void, grown in your service from a single thorn.",
                        "Summoned with an $(l:rituals/overgrown_ritual)Overgrown Ritual$(): a Void Thorn on the altar, with two Crystal Fruits "
                                + "and two Quick-Growing Seeds.",
                        "It is rooted where it grows, so order it to stay or guard; it cannot follow you. Anything that threatens you within "
                                + "its reach meets every one of its attacks.$(br2)At three quarters, half and a quarter of its health it grows "
                                + "healing flowers that last thirty seconds.",
                        "When it withers, it leaves its soul behind. On a Dark Altar it blooms again for 500 souls."),
                new Boss("night_lich", "Night Lich", "bosses_of_mass_destruction", "Bosses of Mass Destruction",
                        "bosses_of_mass_destruction:ancient_anima", "night_lich_soul",
                        "The lich of the frozen night, raised once more, this time to your side.",
                        "Summoned with a $(l:rituals/necroturgy_ritual)Necroturgy Ritual$(): Ancient Anima on the altar, with a Skeleton Skull, "
                                + "a Bone, a Book and a Dark Wand.",
                        "It flies at your side, and teleports back to you when left more than 32 blocks behind. It rains comets and volleys "
                                + "of missiles, teleports in battle, and rages at three quarters, half and a quarter of its health.$(br2)Up to six "
                                + "phantoms answer its call. It brings no eternal night.",
                        "When it falls, it leaves its soul behind. On a Dark Altar it rises again for 500 souls."),
                new Boss("old_champion", "Old Champion", "soulsweapons", "Marium's Soulslike Weaponry",
                        "soulsweapons:essence_of_eventide", "old_champion_soul",
                        "The undead champion of a forgotten war, risen to stand guard over you.",
                        "Summoned with a $(l:rituals/necroturgy_ritual)Necroturgy Ritual$(): an Essence of Eventide on the altar, with a "
                                + "Skeleton Skull, the Draugr, a Shield and a Bone.",
                        "It fights with its whole shield-and-sword art, and enters its second phase at half its health.$(br2)Unlike the "
                                + "boss, it is not slowed or weakened under the open sky.",
                        "When it falls, it leaves its soul behind. On a Dark Altar it rises again for 500 souls."),
                new Boss("returning_knight", "Returning Knight", "soulsweapons", "Marium's Soulslike Weaponry",
                        "soulsweapons:lord_soul_rose", "returning_knight_soul",
                        "The knight who always returns, and now returns to you.",
                        "Summoned with a $(l:rituals/necroturgy_ritual)Necroturgy Ritual$(): a Lord Soul on the altar, with Nightfall, an "
                                + "Arkenstone and two Soul Infused Ingots.",
                        "It brings down Obliterate, Blinding Reflection, Rupture and the Mace of Spades.$(br2)The Remnants it raises serve you "
                                + "too, and its Dark Sorcerers heal it; up to six of them at once.",
                        "When it falls, it leaves its soul behind. On a Dark Altar it returns once more for 500 souls."),
                new Boss("netherman", "Netherman", "knightquest", "Knights Quest", "knightquest:chaotic_essence", "netherman_soul",
                        "The shapeshifting lord of the nether's rifts, bound by the darkest of rites.",
                        "Summoned with a $(l:rituals/sabbath_ritual)Sabbath Ritual$(): Chaotic Essence on the altar, with two Great Essences, "
                                + "an Ender Pearl and Netherite Scrap.",
                        "It shifts between its Fire, Ice and Magic forms at two thirds and a third of its health, sends up to eight clones and "
                                + "its orbs after your foes, and blinks away when struck.$(br2)Its curse of darkness falls only on its target.",
                        "When it falls, it leaves its soul behind. On a Dark Altar it rises again for 500 souls.",
                        // A Netherman starts in its summon animation (rising from underground), which a book page never plays through.
                        "{shouldPlaySummonAnimation:0b}"));
        int sortnum = 1;
        if (loader.mira) {
            mira(entries, sortnum++);
        }
        for (Boss boss : bosses) {
            String entity = loader.entity.get(boss.key), recipe = loader.recipe.get(boss.key);
            json(new File(entries, "bosses/" + boss.key + ".json"), obj(
                    "name", boss.name,
                    "icon", boss.icon,
                    "category", BOSSES,
                    "flag", "mod:" + boss.mod,
                    "sortnum", sortnum++,
                    "pages", List.of(
                            text(boss.lore + "$(br2)" + boss.ritual + "$(br2)$(italic)Requires " + boss.modName + ".$()"),
                            obj("type", "patchouli:entity", "entity", NS + ":" + entity + boss.renderNbt, "scale", 0.8),
                            obj("type", "goety:ritual", "recipe", NS + ":summon_" + recipe),
                            text(boss.servant, "As a Servant"),
                            obj("type", "patchouli:spotlight", "item", NS + ":" + boss.soul, "text", boss.soulText),
                            obj("type", "goety:ritual", "recipe", NS + ":revive_" + recipe)),
                    "extra_recipe_mappings", obj(NS + ":" + boss.soul, 4)));
        }
    }

    static void mira(File entries, int sortnum) throws Exception {
        String entity = NS + ":mira_monstrosity";
        json(new File(entries, "bosses/mira_monstrosity.json"), obj(
                "name", "Mira Monstrosity",
                "icon", NS + ":mira_monstrosity_head",
                "category", BOSSES,
                "flag", "mod:meowhem",
                "advancement", TERMINUS,
                "secret", true,
                "sortnum", sortnum,
                "pages", List.of(
                        text("A colossus of fired clay, cast in the mold of the $(l:summons/redstone_monstrosity)Redstone Monstrosity$() with "
                                + "Orange Terracotta in place of redstone.$(br2)Building it needs the Terminus research, and uses up the Animation "
                                + "Core, the core, the terracotta and the ring.$(br2)$(italic)Requires meowhem.$()"),
                        obj("type", "patchouli:entity", "entity", entity, "scale", 0.75),
                        moldPage("mira_monstrosity_mold", "Mira Monstrosity Mold", "minecraft:lava", "minecraft:orange_terracotta", null, "goety:reinforced_redstone_block",
                                "Right click the center Reinforced Redstone Block with an Animation Core."),
                        text("The ring decides what rises from the mold:$(li)Lava: the Mira Monstrosity$(li)Obsidian: the Obsidian Monstrosity"
                                + "$(li)Sculk: the Warden Monstrosity$(br2)Two may serve you at once, of any kind.", "Its Kin"),
                        obj("type", "patchouli:entity", "entity", entity + "{MiraVariant:'obsidian'}", "scale", 0.75, "name", "Obsidian Monstrosity"),
                        obj("type", "patchouli:entity", "entity", entity + "{MiraVariant:'warden'}", "scale", 0.75, "name", "Warden Monstrosity"),
                        text("Right click it with anything but a wand to ride it. Repair it with Orange Terracotta, or with a Reinforced Redstone "
                                + "Block for far more.", "As a Servant"),
                        obj("type", "patchouli:spotlight", "item", NS + ":mira_monstrosity_head",
                                "text", "Should it fall, it leaves its head, which remembers its master. Build the frame on the next page and "
                                        + "set the head on top, in the middle, last."),
                        revivePage("Revival Frame", "minecraft:orange_terracotta")),
                "extra_recipe_mappings", obj(NS + ":mira_monstrosity_head", 7)));
    }

    // ----- Material monstrosities --------------------------------------------------------------

    record Material(String id, String name, String fill, String fillName, String lore, String stats, String special) {
    }

    static void monstrosities(File entries) throws Exception {
        json(new File(entries, "monstrosities/material_monstrosities.json"), obj(
                "name", "Material Monstrosities",
                "icon", "goety:animation_core",
                "category", MONSTROSITIES,
                "priority", true,
                "sortnum", 0,
                "pages", List.of(
                        text("The $(l:summons/redstone_monstrosity)Redstone Monstrosity$()'s mold need not be filled with redstone. Fill it "
                                + "with another material and a monstrosity of that material rises instead, with its own strength, and its own "
                                + "special attack in place of Redstone Cubes."),
                        text("$(li)Copper Blocks, of any age, waxed or not$(li)Iron Blocks$(li)Gold Blocks$(li)Lapis Blocks$(li)Diamond Blocks"
                                + "$(li)Netherite Blocks$(br2)The $(l:" + NS + "/monstrosities/sculk)Sculk Monstrosity$() has a mold of its own.",
                                "The Fills"),
                        text("As with the Redstone Monstrosity, building one needs the Terminus research and uses up the Animation Core, the "
                                + "core, the fill and the lava; the bricks and Diamond Mold Blocks stay.$(br2)Two of each kind may serve you at "
                                + "once.", "Building"),
                        text("Repair one with a block of its material, or with a Reinforced Redstone Block for far more.$(br2)When one falls it "
                                + "leaves its head, which remembers its master, and rises again from a frame of its material.", "Upkeep"))));

        List<Material> materials = List.of(
                new Material("copper", "Copper Monstrosity", "minecraft:copper_block", "Copper Blocks (any)",
                        "Lighter than the Redstone Monstrosity and cheaper to cast, its patinaed plates crackle with stored charge.",
                        "450 health, 16 attack, 2 armor",
                        "It slams the ground, striking everything within seven blocks for three quarters of its damage and flinging it away."),
                new Material("iron", "Iron Monstrosity", "minecraft:iron_block", "Iron Blocks",
                        "A sturdy foundry-cast hulk, water coursing through its channels.",
                        "650 health, 22 attack, 6 armor",
                        "It calls up two Iron Golems that fight beside it for a minute. It keeps at most four; with four out, it slams instead."),
                new Material("gold", "Gold Monstrosity", "minecraft:gold_block", "Gold Blocks",
                        "A royal colossus of engraved gold. Fragile for its size, but its touch is a curse.",
                        "380 health, 14 attack, 4 armor",
                        "It turns everything within eight blocks to gold for four seconds: gilded foes can neither move nor strike."),
                new Material("lapis", "Lapis Monstrosity", "minecraft:lapis_block", "Lapis Blocks",
                        "A starlit giant of lapis and gold, carrying an enchanting table on its back.",
                        "420 health, 14 attack, 2 armor",
                        "It enchants every ally within sixteen blocks for a minute, with a random enchantment.$(br2)Hand it an enchanted book and "
                                + "it grants that book's enchantment, at the book's level, instead. Crouch with an empty hand to take the book "
                                + "back.$(br2)These enchantments stack with your gear's and with potion effects."),
                new Material("diamond", "Diamond Monstrosity", "minecraft:diamond_block", "Diamond Blocks",
                        "A glittering giant of cut crystal, as hard as it is beautiful.",
                        "800 health, 26 attack, 10 armor",
                        "Diamond spikes burst from the ground beneath every foe within fourteen blocks, then rain down from above for two "
                                + "seconds."),
                new Material("netherite", "Netherite Monstrosity", "minecraft:netherite_block", "Netherite Blocks",
                        "The greatest of them all: a warlord of forged netherite, molten channels burning beneath its plate.",
                        "1000 health, 30 attack, 15 armor",
                        "With no minions out, it calls two half-sized monstrosities of copper, iron, gold, lapis or diamond to fight for a "
                                + "minute.$(br2)Otherwise it tears twelve molten fissures through the earth, then leaps skyward and lands like a "
                                + "meteor, Sundering whatever it crushes."));
        int sortnum = 1;
        for (Material material : materials) {
            String entity = NS + ":" + material.id + "_monstrosity";
            json(new File(entries, "monstrosities/" + material.id + ".json"), obj(
                    "name", material.name,
                    "icon", material.fill,
                    "category", MONSTROSITIES,
                    "advancement", TERMINUS,
                    "secret", true,
                    "sortnum", sortnum++,
                    "pages", List.of(
                            text(material.lore + "$(br2)$(bold)" + material.stats + "$()$(br)(by default)"),
                            obj("type", "patchouli:entity", "entity", entity, "scale", 0.75),
                            moldPage(material.id + "_monstrosity_mold", material.name + " Mold", "minecraft:lava", material.fill, null, "goety:reinforced_redstone_block",
                                    "Fill it with " + material.fillName + ", then right click the Reinforced Redstone Block with an Animation Core."),
                            text(material.special, "Special Attack"),
                            headPage(material.id),
                            revivePage("Revival Frame", material.fill)),
                    "extra_recipe_mappings", obj(NS + ":" + material.id + "_monstrosity_spawn_egg", 1)));
        }

        // The Sculk one: its own mold, around a Warden Core.
        json(new File(entries, "monstrosities/sculk.json"), obj(
                "name", "Sculk Monstrosity",
                "icon", NS + ":warden_core",
                "category", MONSTROSITIES,
                "advancement", TERMINUS,
                "secret", true,
                "sortnum", sortnum,
                "pages", List.of(
                        text("A Redstone Monstrosity that died in the deep dark, and was taken by the sculk. It does not rise from redstone, "
                                + "but around the heart of a Warden.$(br2)$(bold)900 health, 28 attack, 8 armor$()$(br)(by default)"),
                        obj("type", "patchouli:entity", "entity", NS + ":sculk_monstrosity", "scale", 0.75),
                        moldPage("sculk_monstrosity_mold", "Sculk Monstrosity Mold", "minecraft:sculk_sensor", "minecraft:sculk_catalyst", "minecraft:sculk_shrieker",
                                NS + ":warden_core",
                                "Right click the Warden Core with an Animation Core."),
                        text("$(li)Sculk Sensors for the ring (calibrated ones too)$(li)Sculk Catalysts for the fill$(li)Four Sculk Shriekers "
                                + "round the core$(li)A Warden Core at its heart$(br2)Everything but the bricks and Diamond Mold Blocks is used "
                                + "up as it rises, and its shriekers shriek one last time.", "The Mold"),
                        obj("type", "patchouli:spotlight", "item", NS + ":warden_core",
                                "text", "Every Warden carries a Warden Core, and gives it up when slain. The souls inside it still beat with the "
                                        + "Warden's heart.$(br2)Crafted on its own, it breaks into four Echo Shards."),
                        obj("type", "patchouli:crafting", "recipe", NS + ":echo_shards_from_warden_core"),
                        text("Every blow it lands spreads $(bold)Sculk Decay$(), which stacks up to ten times and eats through armor.$(br2)Whatever "
                                + "dies while decaying rises again to fight for it for three minutes, as itself or as a sculk zombie.", "Sculk Decay"),
                        text("It slams with its tentacle arm, and belches globs of sculk that leave a creeping mire.$(br2)Its special attack "
                                + "raises four to six tentacles from the ground around its foes, each slamming down twice.", "Special Attack"),
                        headPage("sculk"),
                        revivePage("Revival Frame", "minecraft:sculk")),
                "extra_recipe_mappings", obj(NS + ":warden_core", 4, NS + ":sculk_monstrosity_spawn_egg", 1)));
    }

    static Map<String, Object> headPage(String material) {
        return obj("type", "patchouli:spotlight",
                "item", NS + ":material_monstrosity_head{material:\"" + material + "\"}",
                "text", "Should it fall, it leaves its head, which remembers its master. Build the frame on the next page and set the head on "
                        + "top, in the middle, last.");
    }

    // ----- Multiblocks -------------------------------------------------------------------------

    // The mold's offsets from the core, as in MonstrosityMoldShape (x, y, z).
    static final int[][] BOTTOM_STONE = {{0, -1, 0}, {0, -1, 2}, {0, -1, 3}, {0, -1, 4}, {0, -1, -2}, {0, -1, -3}, {0, -1, -4},
            {1, -1, 1}, {1, -1, -1}, {1, -1, 2}, {1, -1, -2}, {1, -1, 3}, {1, -1, -3}, {1, -1, 5}, {1, -1, -5},
            {-1, -1, 1}, {-1, -1, -1}, {-1, -1, 2}, {-1, -1, -2}, {-1, -1, 3}, {-1, -1, -3}, {-1, -1, 5}, {-1, -1, -5},
            {2, -1, 0}, {2, -1, 1}, {2, -1, -1}, {2, -1, 2}, {2, -1, -2}, {2, -1, 3}, {2, -1, -3}, {2, -1, 4}, {2, -1, -4},
            {-2, -1, 0}, {-2, -1, 1}, {-2, -1, -1}, {-2, -1, 2}, {-2, -1, -2}, {-2, -1, 3}, {-2, -1, -3}, {-2, -1, 4}, {-2, -1, -4},
            {3, -1, 0}, {3, -1, 1}, {3, -1, -1}, {3, -1, 2}, {3, -1, -2}, {3, -1, 3}, {3, -1, -3}, {3, -1, 4}, {3, -1, -4},
            {-3, -1, 0}, {-3, -1, 1}, {-3, -1, -1}, {-3, -1, 2}, {-3, -1, -2}, {-3, -1, 3}, {-3, -1, -3}, {-3, -1, 4}, {-3, -1, -4},
            {4, -1, 0}, {4, -1, 2}, {4, -1, -2}, {4, -1, 3}, {4, -1, -3},
            {-4, -1, 0}, {-4, -1, 2}, {-4, -1, -2}, {-4, -1, 3}, {-4, -1, -3},
            {5, -1, 1}, {5, -1, -1}, {-5, -1, 1}, {-5, -1, -1}};
    static final int[][] ABOVE_STONE = {{1, 0, 6}, {1, 0, -6}, {3, 0, 5}, {3, 0, -5}, {-3, 0, 5}, {-3, 0, -5},
            {5, 0, 3}, {5, 0, -3}, {-5, 0, 3}, {-5, 0, -3}, {6, 0, 1}, {6, 0, -1}, {-6, 0, 1}, {-6, 0, -1},
            // Goety's mold leaves these two unchecked; its own book shows bricks there.
            {-1, 0, 6}, {-1, 0, -6}};
    static final int[][] DIAMOND = {{0, -1, 1}, {0, -1, -1}, {0, -1, 5}, {0, -1, -5}, {1, -1, 0}, {1, -1, 4}, {1, -1, -4},
            {-1, -1, 0}, {-1, -1, 4}, {-1, -1, -4}, {2, -1, 5}, {2, -1, -5}, {-2, -1, 5}, {-2, -1, -5}, {4, -1, 1}, {4, -1, -1},
            {-4, -1, 1}, {-4, -1, -1}, {5, -1, 0}, {5, -1, 2}, {5, -1, -2}, {-5, -1, 0}, {-5, -1, 2}, {-5, -1, -2},
            {0, 0, 6}, {0, 0, -6}, {2, 0, 6}, {2, 0, -6}, {-2, 0, 6}, {-2, 0, -6}, {4, 0, 4}, {4, 0, -4}, {-4, 0, 4}, {-4, 0, -4},
            {6, 0, 0}, {6, 0, 2}, {6, 0, -2}, {-6, 0, 0}, {-6, 0, 2}, {-6, 0, -2}};
    static final int[][] RING = {{0, -1, 6}, {0, -1, -6}, {1, -1, 6}, {1, -1, -6}, {-1, -1, 6}, {-1, -1, -6},
            {2, -1, 6}, {2, -1, -6}, {-2, -1, 6}, {-2, -1, -6}, {3, -1, 5}, {3, -1, -5}, {-3, -1, 5}, {-3, -1, -5},
            {4, -1, 4}, {4, -1, -4}, {-4, -1, 4}, {-4, -1, -4}, {5, -1, 3}, {5, -1, -3}, {-5, -1, 3}, {-5, -1, -3},
            {6, -1, 0}, {6, -1, 1}, {6, -1, -1}, {6, -1, 2}, {6, -1, -2}, {-6, -1, 0}, {-6, -1, 1}, {-6, -1, -1}, {-6, -1, 2}, {-6, -1, -2},
            {0, 0, 7}, {0, 0, -7}, {1, 0, 7}, {1, 0, -7}, {-1, 0, 7}, {-1, 0, -7}, {2, 0, 7}, {2, 0, -7}, {-2, 0, 7}, {-2, 0, -7},
            {3, 0, 6}, {3, 0, -6}, {-3, 0, 6}, {-3, 0, -6}, {4, 0, 5}, {4, 0, -5}, {-4, 0, 5}, {-4, 0, -5}, {5, 0, 4}, {5, 0, -4},
            {-5, 0, 4}, {-5, 0, -4}, {6, 0, 3}, {6, 0, -3}, {-6, 0, 3}, {-6, 0, -3}, {7, 0, 0}, {7, 0, 1}, {7, 0, -1}, {7, 0, 2},
            {7, 0, -2}, {-7, 0, 0}, {-7, 0, 1}, {-7, 0, -1}, {-7, 0, 2}, {-7, 0, -2}};

    /**
     * The 15x15 mold, two layers (Patchouli's pattern: layers top first, a layer's strings along x, each
     * string's characters along z), anchored ("0") on the brick under the core. {@code center} fills the
     * four spots touching the core (null: the fill). The page names the mold PatchouliMolds registers
     * under {@code id}, which tests blocks as the real mold does; this copy is only its fallback.
     */
    static Map<String, Object> moldPage(String id, String name, String ring, String fill, String center, String core, String text) {
        char[][][] grid = new char[2][15][15];
        for (char[][] layer : grid) {
            for (char[] row : layer) {
                java.util.Arrays.fill(row, '_');
            }
        }
        for (int x = -5; x <= 5; ++x) {
            int reach = new int[]{5, 5, 5, 4, 3, 2}[Math.abs(x)];
            for (int z = -reach; z <= reach; ++z) {
                boolean centerSpot = Math.abs(x) + Math.abs(z) == 1;
                grid[0][x + 7][z + 7] = centerSpot && center != null ? 'H' : 'F';
            }
        }
        mark(grid, BOTTOM_STONE, 'S');
        mark(grid, ABOVE_STONE, 'S');
        mark(grid, DIAMOND, 'D');
        mark(grid, RING, 'R');
        // The core, and the brick under it the mold is anchored on (as Goety's own, so Visualize sets it on the ground).
        grid[0][7][7] = 'C';
        grid[1][7][7] = '0';
        List<List<String>> pattern = new ArrayList<>();
        for (char[][] layer : grid) {
            List<String> rows = new ArrayList<>();
            for (char[] row : layer) {
                rows.add(new String(row));
            }
            pattern.add(rows);
        }
        Map<String, Object> mapping = obj("S", "minecraft:stone_bricks", "D", "goety:diamond_mold_block", "R", ring, "F", fill, "C", core,
                "0", "minecraft:stone_bricks");
        if (center != null) {
            mapping.put("H", center);
        }
        return obj("type", "patchouli:multiblock", "name", name, "multiblock_id", NS + ":" + id,
                "multiblock", obj("pattern", pattern, "mapping", mapping),
                "text", text);
    }

    static void mark(char[][][] grid, int[][] offsets, char c) {
        for (int[] o : offsets) {
            grid[o[1] == 0 ? 0 : 1][o[0] + 7][o[2] + 7] = c;
        }
    }

    // The upright revive frame (as MaterialMonstrosityHeadBlock's pattern, the head left out: it's on the page before).
    static Map<String, Object> revivePage(String name, String fill) {
        List<List<String>> pattern = List.of(List.of("D#####D"), List.of("###R###"), List.of("_#####_"), List.of("_##0##_"));
        return obj("type", "patchouli:multiblock", "name", name,
                "multiblock", obj("pattern", pattern, "mapping", obj("#", fill, "R", "goety:reinforced_redstone_block", "D",
                        "goety:diamond_mold_block", "0", "goety:diamond_mold_block")),
                "enable_visualize", false);
    }

    // ----- JSON --------------------------------------------------------------------------------

    static Map<String, Object> text(String text) {
        return obj("type", "patchouli:text", "text", text);
    }

    static Map<String, Object> text(String text, String title) {
        return obj("type", "patchouli:text", "title", title, "text", text);
    }

    static Map<String, Object> obj(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    static void json(File file, Object value) throws Exception {
        file.getParentFile().mkdirs();
        StringBuilder out = new StringBuilder();
        write(out, value, "");
        out.append('\n');
        Files.writeString(file.toPath(), out.toString(), StandardCharsets.UTF_8);
    }

    static void write(StringBuilder out, Object value, String indent) {
        if (value instanceof Map<?, ?> map) {
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.append(indent).append("  ");
                string(out, (String) e.getKey());
                out.append(": ");
                write(out, e.getValue(), indent + "  ");
                out.append(++i < map.size() ? ",\n" : "\n");
            }
            out.append(indent).append('}');
        } else if (value instanceof List<?> list) {
            // Lists of plain strings (pattern rows) stay on one line.
            boolean flat = list.stream().allMatch(v -> v instanceof String);
            out.append(flat ? "[" : "[\n");
            for (int i = 0; i < list.size(); ++i) {
                if (!flat) {
                    out.append(indent).append("  ");
                }
                write(out, list.get(i), indent + "  ");
                out.append(i + 1 < list.size() ? (flat ? ", " : ",\n") : (flat ? "" : "\n"));
            }
            out.append(flat ? "]" : indent + "]");
        } else if (value instanceof String s) {
            string(out, s);
        } else {
            out.append(value);
        }
    }

    static void string(StringBuilder out, String s) {
        out.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                default -> out.append(c);
            }
        }
        out.append('"');
    }

    static void deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
