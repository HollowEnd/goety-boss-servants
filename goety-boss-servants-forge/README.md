# Goety Boss Servants (Forge 1.20.1)

Forge port of the Goety Boss Servants addon, built against the **Prion - Consumption** instance
(Forge 47.4.x, Goety 2.5.59.2). Licensed under GPL-3.0 (see `LICENSE`); credits are at the end.

**Goety is required. Every boss mod is optional:** each one unlocks only its own servants. Without
it, that servant doesn't exist at all (no entity, soul, head or ritual), so it's unobtainable.

| Servants | Needs |
|---|---|
| Mira Monstrosity (and its Obsidian and Warden variants) | meowhem 1.0+ (with jerotes) |
| Captain Cornelia | Aquamirae 7.1+ (with Fragmentum) |
| Void Blossom, Night Lich | Bosses of Mass Destruction 1.1+ |
| Old Champion, Returning Knight | Marium's Soulslike Weaponry 1.4+ |
| Netherman | Knights Quest 1.9+ (with KnightLib) |
| Copper, Iron, Gold, Lapis, Diamond, Netherite and Sculk Monstrosities | nothing (Goety only) |

## In the game

- **Creative tab:** everything the mod adds is in its own **Goety Boss Servants** tab: the Warden
  Core, the monstrosity heads, the souls of the installed boss mods and the spawn eggs. The vanilla
  tabs still list them too.
- **Goety's Black Book** has a **Goety Boss Servants** section (needs Patchouli, like the book). It
  covers each servant in Goety's own style: a 3D render, its summon and revive rituals, molds as
  buildable multiblocks, its abilities, and the item each needs. A boss's entry only appears
  when its boss mod is installed; the opening entry lists which mod each one needs. The monstrosity
  entries are locked behind the Terminus scroll, like Goety's Redstone Monstrosity. The pages are
  written by `tools/BlackBookSection.java` into `assets/goety/patchouli_books/black_book/` (Patchouli
  only reads a book's pages from its own namespace). The molds are also registered with Patchouli
  (`compat/PatchouliMolds`), so the book's Visualize accepts what the molds do: any bricks, any copper,
  calibrated sensors.
- **Icon:** a crowned skull with soul-fire eyes, hung between chains (`tools/ModIcon.java`).

## Mira Monstrosity

### Summoning

Build the Redstone Monstrosity mold from Goety's Black Book, with two changes:

| Redstone Monstrosity mold | Mira mold |
|---|---|
| Redstone blocks (top layer fill) | **Orange terracotta** |
| Lava ring | **Lava** → Mira Monstrosity, **Obsidian** → Obsidian Monstrosity, **Sculk** → Warden Monstrosity |

Everything else is identical: the brick base (any `*bricks` block), the Diamond Mold Blocks and the
**Reinforced Redstone Block** core. Right-click the core with an **Animation Core**. Like Goety:

- requires the **Terminus** research (configurable),
- has a per-player limit (default 2) and a world limit (default 6) across all three variants,
- uses up the Animation Core, the core, the terracotta and the ring; the bricks and diamond molds stay.

A plain redstone mold still makes Goety's Redstone Monstrosity — the addon only takes over when the
terracotta fill is there.

### As a servant

- Goety wand commands work: follow / wander / stay / guard, command and order foci, patrols, and the
  "are you sure?" dismiss warning. Staying uses meowhem's sitting pose.
- Fights the owner's enemies with all of the boss's attacks, and never the owner or their allies.
  Whatever the owner hits (or is hit by) takes priority over a target it picked by itself, and it
  only notices enemies within 32 blocks (Goety's Redstone Monstrosity range; the boss uses 128).
- The Warden variant's roar shows vanilla sonic-boom particles, which can't move, so they hang in
  front of its face. That's meowhem's own visual, identical on the Warden boss.
- Heals like other Goety servants; **repair** by right-clicking with orange terracotta (small heal) or a
  Reinforced Redstone Block (a quarter of its health).
- **Rideable** by its owner (right-click with anything but a wand). Goety's autonomous-ride toggle works.
- No boss bar, boss music, boss loot or XP.
- On death it drops a **Mira Monstrosity Head** that remembers its owner, name and variant.

### Reviving from the head

Same shape as Goety's Redstone Monstrosity revive, with orange terracotta for the redstone blocks,
built upright:

```
~~~^~~~      ^ = Mira Monstrosity Head (place it last)
D#####D      # = orange terracotta
###H###      H = Reinforced Redstone Block
~#####~      D = Diamond Mold Block
~##D##~      ~ = air
```

The revived servant goes back to the head's original owner and keeps its name and variant.

### Variants

meowhem draws the Obsidian and Warden looks purely from the mob's name ("Obsidian Monstrosity",
"Warden Monstrosity"). The servant uses that as its type name, so the right skin, particles and sounds
appear automatically; a name tag overrides it exactly as on the boss.

### Config

`config/goetybossservants-common.toml`: player/world limits, whether Terminus research is needed, and
health (1020), damage (55) and armor (15), which match the boss, and follow range (32).

## Captain Cornelia (Aquamirae)

### Summoning and reviving

Dark Altar rituals, the same as the Fabric version:

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Deep (`craftType: deep`) | Frozen Key | Salvager Helmet, Suit, Waders, Boots | 20/s for 50 s |
| Revive | Deep | Captain Cornelia's Soul | none | 25/s for 20 s |

Needs Aquamirae 7 (a beta on CurseForge that has to be picked by hand; 6.x isn't supported any more).
Limit: 1 per player (config `captain_cornelia.summonLimit`). She drops her soul when she dies.

### As a servant

- Her own pull → swing → thrust attack chain, rage and animations.
- Goety servant modes and commands (including patrol); your attacks override a target she picked
  herself; 32-block detection range (config `captain_cornelia.followRange`).
- Friendly-fire safe: her swing and rage spin pass over you, your allies, other players, pets and
  player-owned servants (unless that's what she's fighting).
- Her rage brings no Shipbreaker ghost ship and starts no boss encounter.
- No boss bar, spawn cinematic, weapon drops or XP. Iron golems leave her alone, she doesn't vanish
  on Peaceful, and she doesn't stop you sleeping.
- Health, armor, damage and speed come from Aquamirae's own Cornelia config, as on the boss.

### Weapons (reworked for the servant)

She picks the best weapon for the fight instead of Aquamirae's weighted random pick:

1. Below 50% health: **Remnant Saber** (its hits heal her) until she's back to 90%.
2. Target her main weapon (**Tidepiercer**, or **Jawbreaker** in winter) kills in two hits (after
   armor): that.
3. Target with 150+ max health: **Shatterblade** until its Health Shatter is fully stacked. Her
   Shatterblade hits cut 10% max health each (two stacks), lasting a minute per hit: up to 50% for
   5 minutes after 5 hits, like the old Divider rework (Aquamirae's Shatterblade: 5% for 12 s per hit).
4. Otherwise a random pick of her main weapon, the **Shatterspike** (armor shred) and the **Terrible
   Blade** (only if the target can be poisoned, so never against undead), kept until her next switch.

She re-checks between attack chains (the moment a thrust ends), so a needed switch waits at most
one chain, and uses a quick switch (half of Aquamirae's). Aquamirae's own occasional switch still
happens, and rerolls step 4.

### Rage and last stand

- **Rage** (below 25% health): Aquamirae's levitation, spin, lightning and healing.
- **Last stand**: Aquamirae's slow regeneration at 20 health or less now only runs for one 10 s
  window per fight, instead of keeping her alive forever.
- Both **come back after 60 s out of combat** (no target and not being hit; config `phaseResetSeconds`).
- Heals on the client are ignored, so her displayed health never "heals" forever after a rage.

Config (`captain_cornelia`): `shatterbladeMinHealth` (150), `saberBelowHealth` (0.5), `saberUntilHealth` (0.9), `phaseResetSeconds` (60).

## Void Blossom (Bosses of Mass Destruction)

### Summoning and reviving

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Overgrown | Void Thorn | 2 Crystal Fruit, 2 Quick Growing Seeds | 20/s for 50 s |
| Revive | Overgrown | Void Blossom's Soul | none | 25/s for 20 s |

Limit: 1 per player (config `void_blossom.summonLimit`). It drops its soul when it dies.

### As a servant

- A rooted turret like the boss: it stays where it's summoned (stay and guard work; follow and
  wander can't move it).
- All of BMD's attacks, working against any target (BMD's only fire at players): spikes under the
  target, the three-ring spike wave, the poison spore ball and the petal blades, plus its thorn aura.
- Friendly-fire safe: none of its spikes, thorns, spores or blades hurt or poison you, your allies,
  other players, pets or player-owned servants (unless that's what it's fighting).
- **Blossom** (at 75/50/25% health): 8 healing Void Blossom flowers about 4 blocks out (BMD: 15),
  with no moss and no vine walls. They only grow into empty space, disappear after 30 s
  (config `void_blossom.flowerSeconds`), instantly when fire touches them or it starts burning, and
  when it dies; every
  block they replaced is put back.
- Unlike BMD's fire-immune boss, it can catch fire and take burn damage (it's a plant).
- Out of combat (no target, not hit for 5 s) it heals to full at BMD's idle rate; BMD's own idle heal
  stops at its 75/50/25% milestones. No boss bar or experience drop.
- Like the boss it lights up the spot it stands on; the light goes when it dies, is dismissed or
  is stored away.
- Health, armor and damage come from BMD's own Void Blossom config.

## Night Lich (Bosses of Mass Destruction)

### Summoning and reviving

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Necroturgy | Ancient Anima | Skeleton Skull, Bone, Book, Dark Wand | 20/s for 50 s |
| Revive | Necroturgy | Night Lich's Soul | none | 25/s for 20 s |

Limit: 1 per player (config `night_lich.summonLimit`). It drops its soul when it dies.

### As a servant

- Flies: it follows its owner through the air (steering like the boss), hovers over its guard spot,
  and teleports to its owner if left more than 32 blocks behind. Goety's stay/follow/wander/guard
  modes all work.
- All of BMD's attacks, working against any target (BMD's only fire at players): comets, missile
  volleys, phantom minions and its combat teleport (not while staying or guarding).
- **Second phase / rage** (at 75/50/25% health): the comet rage, volley rage and phantom rage, as on
  the boss.
- **Phantom minions** fight for it: they only attack what the lich may attack, never its owner,
  allies, players, pets or player-owned servants, and nothing on your side attacks them. Capped at
  6 alive at once (config `night_lich.maxMinions`). They don't burn in daylight and vanish as soon as
  their lich is gone: dead, unloaded, or stored away (e.g. in Goety's Esoteric Tesseract).
- Its death no longer kills every phantom within 100 blocks (only its own minions), showers no
  experience, and it brings no eternal night.
- Friendly-fire safe like the others, including comet blasts (no damage or knockback to friendlies).
- Out of combat (no target, not hit for 5 s) it heals at BMD's idle rate. No boss bar.
- Health and damage come from BMD's own lich config.

## Old Champion (Marium's Soulslike Weaponry)

### Summoning and reviving

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Necroturgy | Essence of Eventide | Skeleton Skull, Draugr, Shield, Bone | 20/s for 50 s |
| Revive | Necroturgy | Old Champion's Soul | none | 25/s for 20 s |

Limit: 1 per player (config `old_champion.summonLimit`). It drops its soul when it dies.

### As a servant

- The boss's whole shield-and-sword moveset, working against any target: counter, parry, shield
  bash, shield vault, swipes, heavy blow, ground slam, battle cry, sword leap and running thrust,
  with its bleed and shield-disabling hits.
- **Second phase** (at half health), as on the boss: a posture break, then Speed and Strength II for
  the rest of the fight. It turns projectiles back after a few hits.
- Friendly-fire safe: its ground slam, heavy blow and battle cry pass over you, your allies, other
  players, pets and player-owned servants (no damage, knockback or debuffs).
- Changed from the boss: it arrives ready (no rising-from-the-ground intro), doesn't get Slowness
  and Weakness under open sky, doesn't grow resistant to repeated hits from the same weapon (mobs
  always "hold" the same thing, so it became immune to them), and no Night Shade boss rises from its
  death. No boss bar, music or
  experience.

## Returning Knight (Marium's Soulslike Weaponry)

### Summoning and reviving

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Necroturgy | Lord Soul Rose | Nightfall, Arkenstone, 2 Soul Ingots | 20/s for 50 s |
| Revive | Necroturgy | Returning Knight's Soul | none | 25/s for 20 s |

Limit: 1 per player (config `returning_knight.summonLimit`). It drops its soul when it dies.

### As a servant

- The boss's attacks, working against any target: Obliterate, Blinding Reflection, Rupture and
  Mace of Spades, plus its summons.
- **Second phase** (at half health), as on the boss: its one-time Unbreakable stance, then Speed and
  Resistance for the rest of the fight, and its summons bring only Remnants.
- **Minions**, handled like Goety's Redstone Monstrosity cubes (the knight's, and through it yours):
  - Remnants are tamed to you, so everything treats them as your pets; they fight the knight's
    target and stay near the knight (catching up by teleport) instead of following you.
  - Dark Sorcerers beam heals into the knight.
  - Obliterate still raises Remnants from what it kills.
  - At most 6 at once (config `returning_knight.maxMinions`). They never target you or your side,
    nothing on your side targets them, they drop no loot or experience, and they vanish when the
    knight dies or is stored away (e.g. in Goety's Esoteric Tesseract).
- It stops about a block and a half short of its target instead of walking into it and shoving it
  around between attacks (the boss never stops advancing); all its attacks reach from there.
- Its walk and idle animations follow whether it is actually moving (the boss plays "walk" whenever
  it is in a fight, since it never stops advancing).
- Friendly-fire safe: Obliterate, Rupture and Mace of Spades pass over friendlies (no damage,
  launch or knockback). Like the boss it ignores projectiles, but it no longer turns arrows around
  (that sent its owner's arrows back). No boss bar or experience.

## Netherman (Knights Quest)

### Summoning and reviving

| Ritual | Altar setup | Activation item | Pedestals | Souls |
|---|---|---|---|---|
| Summon | Sabbath | Chaotic Essence | 2 Great Essence, Ender Pearl, Netherite Scrap | 20/s for 50 s |
| Revive | Sabbath | Netherman's Soul | none | 25/s for 20 s |

The same rituals as the Fabric version. KnightLib removes recipes that use an item from a feature no
installed mod turns on, and Knights Quest doesn't turn on the Homunculus, so the summon uses
Netherite Scrap instead. Limit: 1 per player (config `netherman.summonLimit`). It drops its soul when
it dies.

### As a servant

- All three forms, as on the boss: Fire above 66% health, Ice to 33%, Magic below. Each change is a
  short invulnerable transformation. As on the boss, it ignores damage for its first 2 seconds.
- Its melee, flame attack, teleport barrage, freeze, curses, clones and explosive orbs come from
  Knights Quest.
- **Clones and orbs** are its minions. Clones burst on reaching its target, and their freeze spares
  friendlies. At most 8 clones at once (`netherman.maxClones`). They vanish when it dies or is
  stored away.
- The darkness curse only hits its target, and the fire barrage leaves lava only when
  `bossesBreakBlocks` is on.
- No summoning cinematic, "teleport to the nearest player", boss bar, music, boss loot or XP. Its
  teleport when hit is a short hop (about 5 blocks, never down into a pit or cave).
- Each form attack opens early instead of after its full 10–15 s cycle: flame, freeze and darkness
  about 2.5 s after it gets a target or changes form, the barrage, clones and orbs about 3–3.5 s, then
  they repeat on Knights Quest's cycle. Its attacks teleport it around its target, so a Netherman
  told to stay still moves while fighting.
- Forge's Netherman refills its health from Knights Quest's config whenever it's added to a world;
  the servant keeps its saved health when its chunk loads or it leaves a Tesseract.

## Material monstrosities (Goety only)

Seven new versions of Goety's Redstone Monstrosity: **Copper, Iron, Gold, Lapis, Diamond, Netherite
and Sculk**. They need nothing but Goety. Each one is Goety's Redstone Monstrosity with its own skin,
stats and special attack. The melee smash, the scatter-bomb belch, riding, sitting, wand commands,
repairs and the head on death all work the same as Goety's. They can only be pets: there are no
hostile versions.

### The special attack

Goety's monstrosity slams the ground and raises Redstone Cubes. These do their own thing at the
moment of that slam instead (still on its 13 second cooldown, when an enemy is within 16 blocks):

| Monstrosity | Special |
|---|---|
| Copper | AoE slam (7 blocks): ¾ of its attack damage, flings enemies away and up |
| Iron | Two **Iron Golems** rise from Goety summon circles beside it. They are owned by the monstrosity (the same way the Redstone Monstrosity owns its cubes) and last 60 s, dying outright when their time is up. At most 4 at once; at the limit it does the slam instead |
| Gold | Everything within 8 blocks is **turned to gold** for 4 s: drawn as a gold statue, can't move, jump, attack or shoot |
| Lapis | Gives every ally within 16 blocks (owner, the owner's pets and servants, its own golems) an **enchantment buff** for 60 s (see below) |
| Diamond | **Diamond spikes** burst up under every enemy within 14 blocks (and at random spots around), launching them, then diamond spikes **rain from the sky** for 2 s |
| Netherite | Two attacks. When it has no minions out it **calls up two other monstrosities** (two different ones of Copper, Iron, Gold, Lapis and Diamond, at random) from summon circles beside it, at **half the size and health**, fighting for it; they drop nothing and die outright after 60 s. Otherwise, **Cataclysm**: its slam splits the ground into 12 **molten fissures** (glowing cracks in the ground that cool and fade) that race out 16 blocks, scorching (60% of its attack damage, 5 s of fire) and flinging everything they pass. Then it **leaps** at the strongest enemy left (most max health, within 24 blocks) and comes down on it like a **meteor** as its slam replays, leaving a glowing crater: everything within 7 blocks takes 150% of its attack damage, is set on fire, launched and **Sundered** for 10 s (half armor and armor toughness, so everyone's hits land harder) |
| Sculk | **Sculk tentacles** burst out of the ground beside its enemies, each slamming down on them twice, leaving lines of **sculk mire** (see its attacks below) |

Area attacks (Goety's own melee smash and slam included) never hit the owner, their pets and
servants, other players or player-owned servants, unless that's what the monstrosity is fighting. A
hostile summoner's minions are fair game.

### The Sculk Monstrosity's attacks

Everything the Sculk Monstrosity does revolves around **Sculk Decay**, a stacking damage-over-time effect:

- Each of its attacks (Goety's smash, its tentacle slam, its tentacles, its sculk globs) adds **2 stacks** to the foes it hurts. The sculk mire it leaves on the ground adds 1 stack a second to foes standing in it. Its foes are what it is fighting, whatever Goety's servants would attack on their own (monsters and hostile summoners' minions, not calm neutral mobs such as zombified piglins), and anything attacking it or its side. Bystanders such as villagers and animals never decay, though its attacks can still hurt them, as the other monstrosities' do. A hit a shield blocks completely adds no decay.
- Each stack deals **1 damage a second**, ignoring armor and with no knockback, up to **10 stacks**. It wears off **8 s** after the last stack is added. The damage comes once a second however often stacks are added. It waits out the short cooldown after a real hit, and adds no cooldown of its own, so it never weakens real hits and they never weaken it.
- **Rising again.** A mob that dies while decaying **rises again** for the Sculk Monstrosity that decayed it, climbing out of the ground where it fell (a flying mob from the ground below). While it rises it can't move or attack, and nothing can hurt it. It is overgrown with patches of sculk, which cover only its visible pixels, never its see-through parts, and keep their own colour on dyed layers such as sheep wool and collars.
  - **As itself**, if it has an attack of its own (a mob that attacks, shoots or is a monster, and isn't run by a brain): the same mob, with its own AI, attacks, abilities and animations. It keeps its variant, gear, name and stats (health capped at 200), but none of the state it died in: it rises awake and standing, not sitting, stunned, angry or with its AI off, and nobody's pet.
    - It fights what its monstrosity fights, and whatever hurts it. It never fights the owner's side, players, pets or bystanders, and nothing on that side (the owner's servants and pets, golems) fights it. Its explosions spare the same, and break no blocks unless `bossesBreakBlocks` is on.
    - It doesn't burn in daylight, despawn, or turn into another mob (no drowned, strays or zombified piglins), and nobody can tame, ride, feed, trade with, leash or breed it. A risen evoker's vexes fight for its monstrosity too.
  - **As a sculk zombie** otherwise: calm mobs, mobs run by a brain whose fighting can't be steered (piglins, goats, camels, wardens), and witches, elder guardians and shulkers (their potions, curse and copies of themselves would be on no one's side). It is a zombie servant that looks like the mob it was: that mob's own model, size and equipment (even gear the mob dropped as it died), animated as that mob is (its idle, walk, tail and swing). A tamed pet still looks tamed. A horse's saddle and a llama's carpet don't show. It is as tough and strong as it was, but at least as fast as a zombie, and it doesn't need air.
  - Either way it crumbles after **3 minutes**, or soon after its monstrosity is gone, and drops nothing. Each monstrosity can have at most 8 risen at once.
  - Other mods: Saints Dragons' dragons rise as themselves, never fall stunned to be tamed, and never go to sleep (a dragon sits down to fall asleep, and one that was stood up halfway stalled in its bind pose). A risen pet that sits is only stood up when it has something to do: a fight, or its monstrosity to catch up with. Raxiores' and The Dawn Era's fighters rise as themselves, and their calm mobs rise as sculk zombies that keep their animations. For any mob, the entity type tags `goetybossservants:rises_as_itself` and `goetybossservants:rises_as_puppet` choose how it rises, and `goetybossservants:puppet_no_tick` stops a sculk zombie's copy of it animating.
  - Players, bosses (including Captain Cornelia, the Netherman and Meowhem's), boss servants and their minions, other monstrosities, and mobs that already rose never rise.

It has **two melee attacks**, taking turns:

- **Goety's smash**: it leans back, raises both hands and slams them down.
- **Tentacle slam**: it raises only its tentacle arm high over its shoulder, the tentacle curling back behind it, then whips it down onto the ground ahead. While winding up it turns so the tentacle, on its right, comes down on its target. Everything along the tentacle (7 blocks) takes its full attack damage and is thrown up, and a line of **sculk mire** is left there for 5 s.

Its **belch** coughs up **sculk globs**: Goety's scatter bombs glowing sculk teal, which burst the same way and leave **pools of sculk mire**.

Its **special** calls up **sculk tentacles**, its own tentacle arm at the same size, out of the ground. There is one beside each foe within 14 blocks, its target first, then the nearest, up to 6 and never fewer than 4. Each one rises from the floor the foe stands on, writhing, then twice rears back and slams down flat on the nearest foe. Every slam deals half its attack damage along the tentacle and leaves a line of mire. The tentacles then sink back into the ground.

Config (`material_monstrosities`):

| Key | Default |
|---|---|
| `sculkDecaySeconds` | 8 |
| `sculkDecayDamage` | 1.0 per stack per second |
| `sculkDecayMaxStacks` | 10 |
| `sculkDecayStacksPerHit` | 2 |
| `sculkMireSeconds` | 5 |
| `sculkZombieSeconds` | 180 |
| `sculkMaxZombies` | 8 |
| `sculkRiseMode` | `AUTO` (`PUPPET`: every mob rises as a sculk zombie) |
| `sculkRisenMaxHealth` | 200 |
| `sculkPuppetTicking` | true (sculk zombies' copies animate) |
| `sculkRiseStripKeys`, `sculkRiseKeepKeys` | none (saved data names, as regular expressions, to leave out or keep when a mob rises) |

### Lapis enchantment buffs

Every buff is a **new effect of its own**, named after its enchantment and at the book's level
(Sharpness V gives Sharpness V). None of them is a vanilla effect, so they **stack** with Strength,
Speed, Resistance and the rest, with the enchantments on your gear, and with each other. L below is
the buff's level.

| Enchantment | Buff |
|---|---|
| Protection | 6% less damage of every kind per L |
| Fire / Blast / Projectile Protection | 15% less fire / explosion / projectile damage per L |
| Feather Falling | 15% less fall damage per L |
| (all protection buffs together) | At most 80% off |
| Thorns | Melee attackers have a 15%-per-L chance to take 1–4 damage |
| Respiration, Aqua Affinity, Depth Strider, Frost Walker, Soul Speed, Swift Sneak | Adds L to that enchantment on your gear, so it works exactly like the enchantment (vanilla's own caps still apply, e.g. Depth Strider III) |
| Efficiency | Adds L to Efficiency (faster mining with the right tool) |
| Looting | Adds L to Looting on kills |
| Fortune | Adds L to Fortune on blocks you break |
| Luck of the Sea | +L Luck (better fishing loot) |
| Sharpness | +0.5 attack damage per L, plus 0.5 (V: +3) |
| Smite / Bane of Arthropods | +2.5 melee damage per L against undead / arthropods; Bane also slows them (Slowness IV, 1–1.5 s) |
| Impaling | +2.5 damage per L (melee or shots) against anything in water or rain, and water mobs |
| Knockback | Melee hits knock back harder (players: adds L to Knockback) |
| Fire Aspect | Melee hits burn for 4 s per L (players: adds L to Fire Aspect) |
| Sweeping Edge | Melee hits splash L/(L+1) of the damage onto monsters right next to the target (players: adds L to their sword's Sweeping Edge) |
| Power | Shots (arrows, tridents, ...) hit 25% harder per L, plus 25% |
| Punch | Shots knock back harder |
| Flame | Shots set the target on fire for 5 s |
| Unbreaking | Each point of wear on your items has an L-in-(L+1) chance to be ignored |
| Mending | Every 2 s, each damaged item carried gets 2 durability per L back |

Without a book it picks one of these at random each time, at level I. **Give it an enchanted
book** (right-click it with the book, as its owner) and it grants that book's enchantment at the
book's level instead: a random one of the book's enchantments if it has several. Only books with at
least one enchantment from the table are accepted; anything else (Silk Touch, Infinity, Multishot,
Piercing, Quick Charge, Loyalty, Riptide, Channeling, Lure, curses, modded enchantments) is refused
with a message. The book floats open over its back pack, which is drawn as its enchanting table. Crouch and right-click with an empty
hand to take it back. It drops the book when it dies.

### Building one

The Redstone Monstrosity mold from Goety's Black Book, with the redstone block fill swapped for the
material's block. The fill decides which one you get:

| Fill (instead of redstone blocks) | Monstrosity |
|---|---|
| Copper blocks (any oxidation, waxed or not) | Copper |
| Iron blocks | Iron |
| Gold blocks | Gold |
| Lapis blocks | Lapis |
| Diamond blocks | Diamond |
| Netherite blocks | Netherite |

The fill is 96 blocks. The brick base, the Diamond Mold Blocks, the **lava** ring and the
**Reinforced Redstone Block** core are exactly Goety's. Right-click the core with an **Animation
Core**. Like Goety: it needs the **Terminus** research (configurable), and it uses up the Animation
Core, the core, the fill and the lava; the bricks and diamond molds stay. Each material has its own
limit: 2 per player and 6 per world by default.

The **Sculk Monstrosity** has its own mold, on the same shape with the same brick base and Diamond
Mold Blocks:

| Goety's mold | Sculk mold |
|---|---|
| Lava ring | **Sculk sensors** (calibrated ones count too) |
| Redstone block fill | **Sculk catalysts** (92) |
| The four fill blocks touching the core | **Sculk shriekers** |
| Reinforced Redstone Block core | **Warden Core** |

Right-click the Warden Core with an Animation Core. The shriekers shriek one last time as it rises,
and everything but the bricks and diamond molds is used up (in creative the Animation Core is kept, as
with Goety's molds). If a block is wrong it says so, like Goety's molds. A mold of lava and sculk
blocks no longer makes it.

**Warden Core**: every warden drops one. It's half a block tall, like the shriekers round it. Its
top is the warden's chest with its ribcage over the souls inside, which glow with each heartbeat (a
calm warden's, every two seconds, which you can hear). Crafting one gives 4 echo shards.

**Spawn eggs** exist for all seven (Spawn Eggs tab). The monstrosity belongs to whoever uses the egg.

### Repairing and reviving

Repair by right-clicking with its material's block (sculk blocks for the Sculk one; 1/16 of its health) or a Reinforced Redstone
Block (a quarter, as on Goety's). Plain redstone blocks don't work on them.

On death it drops a **head** of its material (Copper Monstrosity Head and so on) that remembers its
owner and name. Revive it like Goety's Redstone Monstrosity, with the material's block in place of
the redstone blocks:

```
~~~^~~~      ^ = the monstrosity's head (place it last)
D#####D      # = the material's block
###H###      H = Reinforced Redstone Block
~#####~      D = Diamond Mold Block
~##D##~      ~ = air
```

Like Goety's, the head stands two blocks tall (an invisible upper half above it), and is tilted like a block in inventories.

### Stats

Scaled around the Redstone Monstrosity (600 health, 21 damage, no armor). All configurable under
`material_monstrosities` in `goetybossservants-common.toml`, along with the limits, the Terminus
requirement, follow range (32), the Iron golem limit and lifetime, the Gold stun length and the
Lapis buff length. Goety's Redstone Monstrosity damage cap (25 per hit) and hurt range apply to them too.

| | Health | Damage | Armor |
|---|---|---|---|
| Copper | 450 | 16 | 2 |
| Iron | 650 | 22 | 6 |
| Gold | 380 | 14 | 4 |
| Lapis | 420 | 14 | 2 |
| Diamond | 800 | 26 | 10 |
| Netherite | 1000 | 30 | 15 |
| Sculk | 900 | 28 | 8 |

### Looks

Each skin is built by `../tools/MonstrositySkins.java` (in the folder above this project) on Goety's
Redstone Monstrosity UV layout. The surfaces are the actual vanilla block of each material (copper,
iron, gold, lapis, diamond and netherite blocks), tiled out from the middle of every face so the
seams fall evenly on both sides, with panel edges. Goety's cracks are left out. Each has its own
motif: patina blotches on copper, rivets and rust on iron, polished bevels, engraved diamonds and
rubies on gold, gold trim and glowing enchanting-table glyphs on lapis, sparkles on diamond, forged
war plate on netherite (black seams, a bevel lit from above, gilded corner brackets, and sunken
inner panels on the big plates). The energy channels glow in the material's colour, and
the eyes have a contrasting colour of their own so they stand out:

| | Channels | Eyes |
|---|---|---|
| Copper | Lightning yellow | Cyan |
| Iron | **Running water** (animated, with foam along the edges) | Amber |
| Gold | Royal crimson | Emerald |
| Lapis | **A starfield**: a slowly turning nebula with twinkling stars (animated) | Gold |
| Diamond | **Cut crystal**: clear, near-white facets with a glint sweeping across that trails a faint rainbow, and a few twinkling points (animated) | Red |
| Sculk | **Sculk**: dark, its specks pulsing and a charge running through its old channels (animated) | Sculk cyan |
| Netherite | **Molten netherite**: vanilla lava's own flow, white-hot to dark red, with a cooled crust along the edges (animated) | Soul-fire cyan |

Extra model pieces (`client/MonstrosityExtras.java`) follow Goety's bones and animations:

| Monstrosity | Pieces |
|---|---|
| Copper | Lightning rods on both shoulders and the head, with glowing tips |
| Iron | Vines over its chest and shoulders, a poppy on its head, bolted shoulder plates |
| Gold | A jewelled crown over a crimson velvet cap (also on its head block), an ermine mantle with a crimson cape embroidered with a gold crown, and a gold sceptre with a ruby-banded orb held in its right fist |
| Lapis | The pack on its back is its enchanting table (purple velvet cloth, gold corners and trim, lapis below), with its book floating open over it; purple candles on its shoulders; a ring of glowing glyphs circling above it |
| Diamond | Diamond crystal clusters on its shoulders and on the pack on its back |
| Netherite | Great warlord horns in place of Goety's horn stubs, growing from the sides of its head and sweeping out and up to gold tips, with molten netherite glowing at their bases (also on its head block). It is also **1.2 times bigger** than the others (model and hitbox), the strongest standing tallest |

Pieces on the back sit behind and below a rider's view. Goety's middle eye sits 1 pixel off centre; these draw it centred (and the socket behind it moves with it).

The **Sculk Monstrosity** is a Redstone Monstrosity the sculk killed. Its skin is Goety's own
pale stone, greyed and dead, with sculk in place of all the redstone and creeping out from it
(furthest over its legs and lower body), its specks pulsing and its old channels filled with
sculk, their edges glowing (animated). Its limbs fade from bone into sculk like the Warden's arms (in a sculk shrieker's
bone colours), and its chest is ribbed like the Warden's: five pairs of bone ribs either side of a
dark middle where the Warden's soul heart beats, set into sculk that darkens towards them. Its head is filled with sculk where its jaw was (underneath, and between its teeth). Its
shape is changed too: its head is knocked crooked, its jaw is gone (strands of sculk, swinging as it walks, hang from
under its head) and its right eye is an empty socket; its right horn is a pointed bone horn (a shrieker's bone)
curving forward, rooted deep in its head; its right forearm and hand are gone, a thick sculk tentacle in their place reaching the
ground and dragging behind it (swaying, swinging with its attacks and as it walks); the upper blocks of its
shoulders are sculk shriekers, built as the game builds one at its own pixel size: a shrieker's
base round the top of each side, the animated sculk inside a shrieker ready to summon on top (its
swirl kept near its own size and lit like a real one, not glowing), and on that a ring of
walls with the shrieker's horns (narrow tips, the notch over each mouth) and stepped corner caps, the sculk running on down into the shoulder and creeping out over the rest of it, and the animated tendrils of
sculk sensors grow out of the top of its back; sculk drips from under its body and
shoulders; and Warden tendrils stick out from the sides of its head like the Warden's ears, and
from its back and tentacle. Its head block has no jaw either.

To rebuild the skins:

```powershell
java ../tools/MonstrositySkins.java <folder with Goety's redstone_monstrosity textures> <assets/minecraft/textures from the 1.20.1 client jar> src/main/resources/assets/goetybossservants
```

## Block breaking (all boss servants)

`general.bossesBreakBlocks` (default **false**): whether boss servants may break blocks. Off, the
Mira Monstrosities' charges and quakes, the Night Lich's comets, the Returning Knight's
block smashing and the Netherman's lava leave terrain intact (explosions still hurt enemies). Future
boss servants follow the same option.

## How the boss servants are built (for the Fabric rework)

Each boss servant **extends the boss mod's own class** and implements `BossServant`
(`entity/servant/`), which is Goety's `IServant` backed by a `ServantData` object instead of
Goety's Owned/Summoned fields. So the original renderer, model, animations and attacks are reused
untouched. What each servant class still does itself:

- replace `registerGoals()` with Goety's servant goals (without calling the boss's version),
- forward `setTarget`, `isAlliedTo`, `hurt` and save/load to `BossServant`'s helpers, and call
  `ownedTick()` / `servantTick()` from `tick()`,
- switch off boss-only parts (boss bar, music, loot, XP),
- rely on the central friendly-fire guard (`ModEvents.onLivingAttack`): damage whose source is a
  boss servant never lands on its owner, allies, players, pets or player-owned servants unless it's
  the servant's target (a hostile summoner's minions are fair game), and effects added in the same
  tick are blocked too,
- keep every reference to the boss mod inside that mod's compat classes (`compat/*Compat`,
  `client/*Client`): registration in `ModRegistry` is wrapped in `ifLoaded(...)`, mixins are listed in
  `GoetyBossServantsMixinPlugin`, and rituals carry a `forge:mod_loaded` condition, so nothing of a
  missing boss mod is ever loaded,
- give summoned minions to `ServantMinions` (`entity/servant/`): tagged with their boss's UUID,
  capped, fighting its target, and discarded the moment the boss is gone (like Goety's Redstone
  Monstrosity cubes, they belong to the boss, which belongs to the player),
- undo `Monster` side effects when the boss extends `Monster` (Peaceful despawn, sleep blocking,
  iron golems — the last is a `LivingChangeTargetEvent` in `ModEvents`).

Where the boss's code is one big inline method, small mixins patch just the needed call. They use
`remap = false` and name both the dev and SRG method names (e.g. `{"baseTick", "m_6075_"}`), so no
refmap is needed. The Mira Monstrosity is the exception: meowhem's base class already has an int
`getOwnerId()`, so its `IServant` is attached by a mixin instead (see below).

## Building

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot"
.\gradlew.bat build
```

The jar lands in `build/libs/goetybossservants-<version>.jar`. The Goety, meowhem and jerotes jars in
`libs/` are copies from the instance; ForgeGradle remaps them for compiling.

Soulslike Weaponry and its Ranged Weapon API ship mixins written against SRG names with no refmap,
which can't apply in a dev run. Their `libs/` copies (`*-devmixins.jar`) keep only the mixins other
code relies on: Soulslike's six accessors (with a small hand-written SRG refmap) and the API's two
item-interface mixins. The game uses the real jars.

### Self-test

`.\gradlew.bat runClient -Pselftest` starts a dev client in `run-selftest/` (meowhem can't run on a
dedicated server), creates a flat world, builds each mold,
summons with a fake player, runs wand commands, kills and revives a servant, logs `SELFTEST PASS/FAIL`
lines plus `run-selftest/selftest_result.txt`, and exits. The test code (`debug/`) is excluded from the jar.
`-PselftestFrom=<tick>` skips straight to one boss's part after setup (e.g. `6600` for Soulslike
Weaponry, `7800` for the Netherman, `9900` for its fight against a warden, `11200` for the material monstrosities, `11920` for mobs rising from Sculk Decay, `12400` for the Warden Core, `12510` for the Black Book section). The monstrosity parts also save screenshots of each skin and special to `run-selftest/screenshots/`.
`-PwithoutMods=meowhem,aquamirae,bmd,soulslike,knightquest` (any of them) leaves those boss mods out
of the run: their checks are skipped, and the test checks their servants, souls and rituals don't
exist.
`-PwithRiseMods` also loads Raxiores, The Dawn Era (with AstemirLib) and Saints Dragons, and checks
that their mobs rise as they should.

## How it hooks into Goety and meowhem

- `MiraMonstrosityServant` **extends meowhem's `MiraMonstrosityEntity`**, so meowhem's own renderer,
  model, animations and attacks are reused untouched. Its AI goals are replaced with Goety's servant
  goals.
- Goety's `IServant` is added by a mixin (`MiraMonstrosityServantMixin`): `IOwned.getOwnerId()` returns
  a UUID while meowhem's base class already has an int `getOwnerId()`, which Java source can't express.
- The mold is detected in a `RightClickBlock` handler before Goety's Animation Core runs, because Goety
  only allows one mold per core block and the Reinforced Redstone Block already belongs to the
  Redstone Monstrosity.

## Credits and licenses

Goety Boss Servants is an unofficial add-on. It isn't affiliated with or endorsed by the authors of
the mods below. It ships none of their files: each boss's model, textures, sounds and behaviour come
from that mod, which must be installed. The one exception is the material monstrosity skins, which
are built on Goety's Redstone Monstrosity textures (MIT) and vanilla block textures, and the
Warden Core's on the vanilla warden's.

- **Goety** by Polarice3 (MIT). The servant system this builds on; the Mira Monstrosity mold reuses
  the block layout of Goety's Redstone Monstrosity mold. The material monstrosities extend Goety's
  Redstone Monstrosity, reuse its model, animations, renderer and head model, and their skins are
  recoloured from its textures.
- **meowhem** and **jerotes** by Jerotes_ (GPL-3.0). The Mira Monstrosity servant extends meowhem's
  Mira Monstrosity.
- **Aquamirae** and **Fragmentum** by Obscuria (Obscuria Modding License / Obscuria Ecosystem
  License). Captain Cornelia extends Aquamirae's Cornelia, and a mixin adjusts her rage and weapon
  choice at runtime. Used with the authors' permission for add-ons that require their mod.
- **Bosses of Mass Destruction** by Barribob, Forge port by CerbonXD (LGPL-3.0), with **Cerbon's
  API** (LGPL-3.0). The Night Lich and Void Blossom servants extend BMD's classes. Their attack
  classes and renderer setup are adapted from BMD's code; those files carry a notice saying what was
  changed.
- **Marium's Soulslike Weaponry** by Marium Bacchus (CC0-1.0). The Old Champion and Returning Knight
  servants extend its bosses.
- **Knights Quest** (GPL-3.0) and **KnightLib** (LGPL-3.0) by Xylonity. The Netherman servant
  extends Knights Quest's Netherman, and mixins adjust its clones and goals at runtime.
- **GeckoLib** by Gecko, Eliot, AzureDoom, DerToaster, Tslat and Witixin (MIT), used through the
  boss mods.

The jars in `libs/` are local copies of these mods for compiling and the dev self-test. They must
never be redistributed (Aquamirae's and Fragmentum's licenses allow distribution only through
CurseForge and Modrinth), and the `*-devmixins.jar` files are modified, so they're kept out of
version control by `.gitignore`.
