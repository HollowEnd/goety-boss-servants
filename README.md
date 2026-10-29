# Goety Boss Servants

An unofficial add-on for [Goety](https://www.curseforge.com/minecraft/mc-mods/goety) on Minecraft
1.20.1. It lets you summon bosses from other mods as Goety servants, and adds seven pet
monstrosities built like Goety's Redstone Monstrosity: Copper, Iron, Gold, Lapis, Diamond,
Netherite and Sculk.

| Folder | What it is |
|---|---|
| [`goety-boss-servants-forge/`](goety-boss-servants-forge) | The Forge version (built and tested on Forge 47.4 with Goety 2.5.59). It also has the Mira Monstrosity (meowhem). |
| [`goety-boss-servants/`](goety-boss-servants) | The Fabric version (Fabric Loader 0.18.5+, Goety's Fabric port). |
| [`tools/`](tools) | Small Java programs that made the mod's textures, icon and Black Book section, and the pictures for its CurseForge page, plus a helper for inspecting textures. |

Each loader's README covers its servants: how to summon, revive and command them, and how to build
and test it.

## Building

Each project builds on its own with its Gradle wrapper (`gradlew build`): Forge with JDK 17, Fabric
with JDK 21 (its jar still targets Java 17). Both compile against other mods' jars, which aren't in
this repository: several of their licenses forbid redistributing them, and some of the local copies
are adjusted. Put them in each project's `libs/` folder first:

- **Forge:** `build.gradle` names each jar as `blank:<name>:<version>`, which it looks for as
  `libs/<name>-<version>.jar`, so rename the downloaded jars to match.
- **Fabric:** `build.gradle` names the exact file names under `libs/`.
- **Adjusted copies:** a few entries are made from the downloaded jars with the programs in each
  project's `tools/` folder. `OpenClasses.java` makes the Fabric `BOMD-…-open.jar` (Bosses of Mass
  Destruction with three classes made non-final), and `RenameMethods.java` makes the Saints Dragons
  `-dev` jars. The Forge `-devmixins` jars only leave out mixins that can't apply in a dev run. The
  comments in `build.gradle` and the project READMEs say more.

## License

GPL-3.0-only (see [`LICENSE`](LICENSE)). Each project's `NOTICE` and README have the credits and
third-party notices.
