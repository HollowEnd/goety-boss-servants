package com.saints.goetybossservants.debug;

import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.compat.PatchouliMolds;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.construct.MonstrosityMoldShape;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraft.advancements.Advancement;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.saints.goetybossservants.debug.SelfTest.check;

/**
 * This mod's section of Goety's Black Book (Patchouli, reached by reflection: it's only loaded in dev
 * runs): the book still loads; exactly the entries for the installed boss mods are there (with a
 * boss mod missing, its entries are left out, or the whole book would fail to load); the
 * monstrosity entries open once the Terminus scroll is read; and screenshots of its pages, with no
 * entity render failing. Steps are ticks after START.
 */
class BookChecks {
    static final int START = WardenCoreChecks.START + WardenCoreChecks.LENGTH;
    static final int LENGTH = 280;
    private static final ResourceLocation BOOK = new ResourceLocation("goety", "black_book");
    private static final String SECTION = "goety:goetybossservants";
    // Where the molds for the book's mold check are built (one per material, 20 blocks apart).
    private static final BlockPos MOLDS = new BlockPos(300, 120, 300);
    // Read on the client thread, reported on the server's.
    private static volatile List<String> clientResults;
    private static final List<String[]> shots = new ArrayList<>();

    static void step(ServerLevel level, int t) {
        if (t == 0) {
            ServerPlayer viewer = viewer(level);
            check(viewer != null, "a client player is in the world for the book checks");
            if (viewer != null) {
                // The monstrosity entries are secret until the Terminus scroll is read, like Goety's Redstone Monstrosity.
                Advancement terminus = level.getServer().getAdvancements().getAdvancement(new ResourceLocation("goety", "goety/read_terminus_scroll"));
                check(terminus != null, "Goety's Terminus scroll advancement exists");
                if (terminus != null) {
                    terminus.getCriteria().keySet().forEach(criterion -> viewer.getAdvancements().award(terminus, criterion));
                }
                viewer.setGameMode(GameType.SPECTATOR);
            }
            if (!Patchouli.loaded()) {
                check(false, "Patchouli is loaded in the dev run");
            }
        }
        if (!Patchouli.loaded()) {
            return;
        }
        if (t == 4) {
            checkMolds(level);
        }
        if (t == 10) {
            clientResults = null;
            Minecraft.getInstance().execute(() -> clientResults = checkContents());
        }
        if (t == 16) {
            List<String> results = clientResults;
            check(results != null, "the book was read on the client");
            if (results != null) {
                results.forEach(result -> check(result.startsWith("PASS "), result.substring(5)));
            }
            shots.clear();
            shot("introduction", 0, 2);
            shot("monstrosities/material_monstrosities", 0, 2);
            shot("monstrosities/gold", 0, 2, 4);
            // Every entity page is drawn (Patchouli only makes the entity once its page is shown).
            for (String material : List.of("copper", "iron", "lapis", "diamond", "netherite")) {
                shot("monstrosities/" + material, 0);
            }
            shot("monstrosities/sculk", 0, 2, 4, 6, 8);
            if (ModCompat.isLoaded("meowhem")) {
                shot("bosses/mira_monstrosity", 0, 2, 4, 6, 8);
            }
            if (ModCompat.isLoaded("aquamirae")) {
                shot("bosses/cornelia", 0, 2, 4);
            }
            if (ModCompat.isLoaded("bosses_of_mass_destruction")) {
                shot("bosses/void_blossom", 0, 2);
                shot("bosses/night_lich", 0, 2);
            }
            if (ModCompat.isLoaded("soulsweapons")) {
                shot("bosses/old_champion", 0, 2);
                shot("bosses/returning_knight", 0, 2);
            }
            if (ModCompat.isLoaded("knightquest")) {
                shot("bosses/netherman", 0, 2);
            }
        }
        // Each page spread opened, then shot once it has drawn.
        int shot = (t - 20) / 6;
        if (t >= 20 && shot < shots.size()) {
            String[] page = shots.get(shot);
            if ((t - 20) % 6 == 0) {
                Minecraft.getInstance().execute(() -> Patchouli.open(BOOK, new ResourceLocation("goety", "goetybossservants/" + page[0]), Integer.parseInt(page[1])));
            } else if ((t - 20) % 6 == 4) {
                ClientSelfTest.screenshot("book_" + page[0].replace('/', '_') + "_" + page[1]);
            }
        }
        if (t == LENGTH - 10) {
            clientResults = null;
            Minecraft.getInstance().execute(() -> {
                clientResults = checkRenders();
                Minecraft.getInstance().setScreen(null);
            });
        }
        if (t == LENGTH - 4) {
            List<String> results = clientResults;
            check(results != null, "the book's renders were checked on the client");
            if (results != null) {
                results.forEach(result -> check(result.startsWith("PASS "), result.substring(5)));
            }
        }
    }

    /**
     * The molds registered with Patchouli (PatchouliMolds), against real molds built in the world: each
     * is accepted every way round (Visualize turns the pattern to the player's facing; the mold itself
     * never turns), also with the other blocks the mold allows, and refused with a rim brick missing.
     */
    private static void checkMolds(ServerLevel level) {
        try {
            Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
            Method getMultiblock = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI").getMethod("getMultiblock", ResourceLocation.class);
            Method validate = Class.forName("vazkii.patchouli.api.IMultiblock").getMethod("validate", Level.class, BlockPos.class, Rotation.class);
            for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
                BlockPos core = MOLDS.offset(material.ordinal() * 20, 0, 0);
                Object mold = getMultiblock.invoke(api, PatchouliMolds.id(material));
                MaterialMonstrosityMold.placeMold(level, core, material);
                asTheBookShows(level, core);
                // The other blocks the mold takes: deepslate bricks, any copper, calibrated sensors.
                level.setBlockAndUpdate(core.offset(0, -1, 2), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                if (material == MonstrosityMaterial.COPPER) {
                    level.setBlockAndUpdate(core.offset(2, 0, 2), Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState());
                }
                if (material == MonstrosityMaterial.SCULK) {
                    level.setBlockAndUpdate(core.offset(MonstrosityMoldShape.RING_LOCATIONS.get(0)), Blocks.CALIBRATED_SCULK_SENSOR.defaultBlockState());
                }
                check(MaterialMonstrosityMold.findMaterial(level, core) == material, material + " mold built for the book's mold check");
                check(mold != null && everyWay(validate, mold, level, core, true), material + "'s book mold accepts its real mold every way round");
                level.setBlockAndUpdate(core.offset(1, 0, 6), Blocks.AIR.defaultBlockState());
                check(mold != null && everyWay(validate, mold, level, core, false), material + "'s book mold refuses it with a rim brick missing, every way round");
            }
            if (ModCompat.isLoaded("meowhem")) {
                // (Forge only: reached by name, as the Fabric copy of this test has no Mira Monstrosity.)
                BlockPos core = MOLDS.offset(0, 0, 20);
                Object mold = getMultiblock.invoke(api, new ResourceLocation("goetybossservants", "mira_monstrosity_mold"));
                Class<?> miraMold = Class.forName("com.saints.goetybossservants.construct.MiraMonstrosityMold");
                Class<?> variants = Class.forName("com.saints.goetybossservants.entity.MiraVariant");
                Object mira = variants.getField("MIRA").get(null);
                miraMold.getMethod("placeMold", Level.class, BlockPos.class, variants).invoke(null, level, core, mira);
                asTheBookShows(level, core);
                check(mold != null && everyWay(validate, mold, level, core, true), "the Mira Monstrosity's book mold accepts its real mold every way round");
                level.setBlockAndUpdate(core.offset(MonstrosityMoldShape.RING_LOCATIONS.get(0)), Blocks.OBSIDIAN.defaultBlockState());
                check(mold != null && everyWay(validate, mold, level, core, false), "the Mira Monstrosity's book mold refuses a mixed ring, as the mold does");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            check(false, "checking the book's molds: " + e);
        }
    }

    // The two rim bricks the mold never checks, which the book shows (as Goety's does) so its pattern is the same every way round.
    private static void asTheBookShows(ServerLevel level, BlockPos core) {
        level.setBlockAndUpdate(core.offset(-1, 0, 6), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(core.offset(-1, 0, -6), Blocks.STONE_BRICKS.defaultBlockState());
    }

    // Whether the mold (anchored on the brick under the core) gives the expected answer at every rotation.
    private static boolean everyWay(Method validate, Object mold, ServerLevel level, BlockPos core, boolean expected) throws ReflectiveOperationException {
        for (Rotation rotation : Rotation.values()) {
            if ((boolean) validate.invoke(mold, level, core.below(), rotation) != expected) {
                return false;
            }
        }
        return true;
    }

    private static void shot(String entry, int... pages) {
        for (int page : pages) {
            shots.add(new String[]{entry, Integer.toString(page)});
        }
    }

    private static ServerPlayer viewer(ServerLevel level) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    private static String result(boolean ok, String what) {
        return (ok ? "PASS " : "FAIL ") + what;
    }

    // On the client: what the book loaded.
    private static List<String> checkContents() {
        List<String> results = new ArrayList<>();
        try {
            Object contents = Patchouli.contents(BOOK);
            boolean errored = (boolean) contents.getClass().getMethod("isErrored").invoke(contents);
            results.add(result(!errored, "the Black Book loads with this mod's section ("
                    + (errored ? contents.getClass().getMethod("getException").invoke(contents) : "no error") + ")"));
            Map<?, ?> categories = (Map<?, ?>) contents.getClass().getField("categories").get(contents);
            Map<?, ?> entries = (Map<?, ?>) contents.getClass().getField("entries").get(contents);

            boolean anyBoss = false;
            Set<String> expected = new TreeSet<>(List.of("introduction", "monstrosities/material_monstrosities", "monstrosities/copper",
                    "monstrosities/iron", "monstrosities/gold", "monstrosities/lapis", "monstrosities/diamond", "monstrosities/netherite",
                    "monstrosities/sculk"));
            String[][] bosses = {{"meowhem", "bosses/mira_monstrosity"}, {"aquamirae", "bosses/cornelia"},
                    {"bosses_of_mass_destruction", "bosses/void_blossom"}, {"bosses_of_mass_destruction", "bosses/night_lich"},
                    {"soulsweapons", "bosses/old_champion"}, {"soulsweapons", "bosses/returning_knight"}, {"knightquest", "bosses/netherman"}};
            for (String[] boss : bosses) {
                if (ModCompat.isLoaded(boss[0])) {
                    expected.add(boss[1]);
                    anyBoss = true;
                }
            }
            Set<String> found = new TreeSet<>();
            for (Object id : entries.keySet()) {
                if (id.toString().startsWith(SECTION + "/")) {
                    found.add(id.toString().substring(SECTION.length() + 1));
                }
            }
            results.add(result(found.equals(expected), "the section has exactly the entries for the installed boss mods: " + found));
            results.add(result(categories.containsKey(new ResourceLocation(SECTION)) && categories.containsKey(new ResourceLocation(SECTION + "/monstrosities"))
                    && categories.containsKey(new ResourceLocation(SECTION + "/bosses")) == anyBoss,
                    "the section's categories are there (bound bosses only with a boss mod)"));
            results.addAll(checkReferences());
            // Unlocked by the Terminus scroll read above.
            Object gold = entries.get(new ResourceLocation(SECTION + "/monstrosities/gold"));
            results.add(result(gold != null && !(boolean) gold.getClass().getMethod("isLocked").invoke(gold),
                    "the monstrosity entries are open once the Terminus scroll is read"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            results.add(result(false, "reading the Black Book: " + e));
        }
        return results;
    }

    /**
     * On the client: every ritual and crafting recipe and every registered mold the section's pages name
     * (in the entries shown with the installed mods) exists. A missing ritual only shows a blank page, so
     * nothing else would notice; a missing mold falls back to the page's own copy.
     */
    private static List<String> checkReferences() throws ReflectiveOperationException {
        List<String> results = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
        Method getMultiblock = Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI").getMethod("getMultiblock", ResourceLocation.class);
        List<String> missing = new ArrayList<>();
        int rituals = 0, crafting = 0, molds = 0;
        for (Map.Entry<ResourceLocation, Resource> file : minecraft.getResourceManager()
                .listResources("patchouli_books/black_book/en_us/entries/goetybossservants", id -> id.getPath().endsWith(".json")).entrySet()) {
            if (!file.getKey().getNamespace().equals("goety")) {
                continue;
            }
            JsonObject entry;
            try (Reader reader = file.getValue().openAsReader()) {
                entry = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                missing.add(file.getKey() + " (unreadable)");
                continue;
            }
            String flag = entry.has("flag") ? entry.get("flag").getAsString() : "";
            if (flag.startsWith("mod:") && !ModCompat.isLoaded(flag.substring(4))) {
                continue;
            }
            for (JsonElement element : entry.getAsJsonArray("pages")) {
                JsonObject page = element.getAsJsonObject();
                String type = page.get("type").getAsString();
                if (type.equals("goety:ritual") || type.equals("patchouli:crafting")) {
                    ResourceLocation id = new ResourceLocation(page.get("recipe").getAsString());
                    Recipe<?> recipe = minecraft.level.getRecipeManager().byKey(id).orElse(null);
                    ResourceLocation recipeType = recipe == null ? null : BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
                    boolean ritual = type.equals("goety:ritual");
                    if (recipe == null || !String.valueOf(recipeType).equals(ritual ? "goety:ritual" : "minecraft:crafting")) {
                        missing.add(id + " (" + recipeType + ")");
                    }
                    if (ritual) {
                        ++rituals;
                    } else {
                        ++crafting;
                    }
                }
                if (page.has("multiblock_id")) {
                    ResourceLocation id = new ResourceLocation(page.get("multiblock_id").getAsString());
                    if (getMultiblock.invoke(api, id) == null) {
                        missing.add(id + " (mold not registered)");
                    }
                    ++molds;
                }
            }
        }
        boolean anyBoss = List.of("aquamirae", "bosses_of_mass_destruction", "soulsweapons", "knightquest").stream().anyMatch(ModCompat::isLoaded);
        results.add(result(missing.isEmpty() && (rituals > 0) == anyBoss && crafting > 0 && molds > 0,
                "every recipe and mold the section names exists (" + rituals + " rituals, " + crafting + " crafting, " + molds + " molds; missing: "
                        + missing + ")"));
        return results;
    }

    // On the client, once the pages have been drawn: no entity render on them failed.
    private static List<String> checkRenders() {
        List<String> results = new ArrayList<>();
        try {
            Map<?, ?> entries = (Map<?, ?>) Patchouli.contents(BOOK).getClass().getField("entries").get(Patchouli.contents(BOOK));
            int drawn = 0;
            int pages = 0;
            List<String> failed = new ArrayList<>();
            for (Map.Entry<?, ?> e : entries.entrySet()) {
                if (!e.getKey().toString().startsWith(SECTION + "/")) {
                    continue;
                }
                for (Object page : (Collection<?>) e.getValue().getClass().getMethod("getPages").invoke(e.getValue())) {
                    if (page.getClass().getSimpleName().equals("PageEntity")) {
                        ++pages;
                        Field errored = page.getClass().getDeclaredField("errored");
                        Field entity = page.getClass().getDeclaredField("entity");
                        errored.setAccessible(true);
                        entity.setAccessible(true);
                        if ((boolean) errored.get(page)) {
                            failed.add(e.getKey().toString());
                        } else if (entity.get(page) != null) {
                            ++drawn;
                        }
                    }
                }
            }
            results.add(result(failed.isEmpty() && drawn == pages && pages > 0,
                    "every entity page of the section renders (" + drawn + " of " + pages + " drawn, failed: " + failed + ")"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            results.add(result(false, "checking the Black Book's renders: " + e));
        }
        return results;
    }

    /** Patchouli's book registry and API, by reflection. */
    private static final class Patchouli {
        static boolean loaded() {
            try {
                Class.forName("vazkii.patchouli.api.PatchouliAPI");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        static Object contents(ResourceLocation book) throws ReflectiveOperationException {
            Class<?> registry = Class.forName("vazkii.patchouli.common.book.BookRegistry");
            Map<?, ?> books = (Map<?, ?>) registry.getField("books").get(registry.getField("INSTANCE").get(null));
            Object found = books.get(book);
            return found.getClass().getMethod("getContents").invoke(found);
        }

        static void open(ResourceLocation book, ResourceLocation entry, int page) {
            try {
                Object api = Class.forName("vazkii.patchouli.api.PatchouliAPI").getMethod("get").invoke(null);
                Class.forName("vazkii.patchouli.api.PatchouliAPI$IPatchouliAPI").getMethod("openBookEntry", ResourceLocation.class, ResourceLocation.class, int.class)
                        .invoke(api, book, entry, page);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
