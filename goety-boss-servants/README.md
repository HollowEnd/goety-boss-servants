# Goety Boss Servants (Fabric 1.20.1)

A Goety addon that lets you summon bosses from other mods as Goety servants. Built against the
"pet mod" instance (Fabric Loader 0.19.x, Fabric API 0.92, Goety Fabric 0.6.1). Licensed under
GPL-3.0 (see `LICENSE`); credits are at the end.

**Goety is required. Every boss mod is optional:** each one unlocks only its own servants. Without
it, that servant doesn't exist at all (no entity, soul or ritual), so it's unobtainable.

| Servants | Needs |
|---|---|
| Captain Cornelia | Aquamirae 7.1+ (with Fragmentum) |
| Void Blossom, Night Lich | Bosses of Mass Destruction 1.7+ |
| Old Champion, Returning Knight | Marium's Soulslike Weaponry 1.4+ |
| Netherman | Knights Quest 1.9+ (with KnightLib) |
| Copper, Iron, Gold, Lapis, Diamond, Netherite and Sculk Monstrosities | nothing (Goety only) |

The boss servants below are summoned at a Goety Dark Altar (the material monstrosities are built
from molds instead; see their section), and each drops its **soul** when it dies. Using the
soul on the same altar revives it (25 souls/s for 20 s = 500 souls, no pedestal items). Summon
rituals cost 20 souls/s for 50 s. The limit is 1 of each per player (config `<boss>.summonLimit`).

All of them share the servant basics: Goety's follow / wander / stay / guard commands, Goety's
targeting (they fight the owner's enemies and never the owner or their allies), no boss bar, boss
music, boss loot or experience, and no Peaceful despawn.

**Friendly fire:** damage from a boss servant (or its minions, projectiles and explosions) never lands
on its owner, their allies, other players, pets or player-owned servants unless that's what it's
fighting. A hostile summoner's minions (a hostile necromancer's undead, say) are fair game. Effects
from those hits are blocked too.

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

## Captain Cornelia (Aquamirae)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Deep | Frozen Key | Salvager Helmet, Suit, Waders, Boots |

- Her pull → swing → thrust chain, rage and animations are Aquamirae's own.
- Her swing and rage spin pass over friendlies, and her rage brings no Shipbreaker or boss encounter.
- **Weapons**: instead of Aquamirae's weighted random pick, she picks the best weapon for the fight,
  checked between attack chains, with a quick half-length switch:
  1. Below 50% health: **Remnant Saber** (its hits heal her) until she's back to 90%.
  2. A target her main weapon (**Tidepiercer**, or **Jawbreaker** in winter) kills in two hits: that.
  3. A target with 150+ max health: **Shatterblade** until its Health Shatter is fully stacked.
     Her Shatterblade hits cut 10% max health each (two stacks), lasting a minute per hit: up to
     50% for 5 minutes after 5 hits (Aquamirae's Shatterblade: 5% for 12 seconds per hit).
  4. Otherwise a random pick of her main weapon, the **Shatterspike**, and the **Terrible Blade**
     (only if the target can be poisoned), kept until her next switch.

  Config: `captain_cornelia.shatterbladeMinHealth` (150), `saberBelowHealth` (0.5), `saberUntilHealth` (0.9).
- Her low-health regeneration runs for one 10-second window per fight. Her rage and that window
  come back after 60 s out of combat (config `captain_cornelia.phaseResetSeconds`).
- No spawn cinematic or weapon drop. She notices enemies within 32 blocks
  (`captain_cornelia.followRange`).

## Void Blossom (Bosses of Mass Destruction)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Overgrown | Void Thorn | 2 Crystal Fruit, 2 Quick Growing Seeds |

- A rooted turret like the boss: it stays where it's summoned (stay and guard work; follow and
  wander can't move it).
- All of BMD's attacks, working against any target (BMD's only fire at players): spikes under the
  target, the spike wave, the spore ball and the petal blades, plus its thorn aura.
- **Blossom** (at 75/50/25% health): healing flowers close by, with no moss or vine walls. Each
  heals it 10 health every 3 seconds, up to the health milestone above it, as on the boss. They
  only grow into empty space and disappear after 30 s (`void_blossom.flowerSeconds`), when fire
  touches them, and when it dies. Every block they replaced is put back.
- Like the boss it lights up the spot it stands on; the light goes when it dies, is dismissed or
  is stored away.

## Night Lich (Bosses of Mass Destruction)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Necroturgy | Ancient Anima | Skeleton Skull, Bone, Book, Dark Wand |

- Flies: it follows its owner through the air, hovers while staying, and teleports over if left far behind.
- All of BMD's attacks against any target: comets, missile volleys, phantom minions and its combat
  teleport, with the rage phases at 75/50/25% health.
- **Phantom minions** fight for it, never against your side. At most 6 at once
  (`night_lich.maxMinions`). They vanish when the lich dies or is stored away (e.g. in Goety's
  Esoteric Tesseract).
- No eternal night, no 1500-experience shower, and its death only removes its own phantoms (the boss
  kills every phantom within 100 blocks).

## Old Champion (Marium's Soulslike Weaponry)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Necroturgy | Essence of Eventide | Skeleton Skull, Draugr, Shield, Bone |

- The boss's whole shield-and-sword moveset against any target, and its second phase at half health
  (posture break, then Speed and Strength II).
- Its ground slam and battle cry pass over friendlies.
- Changed from the boss: no Slowness and Weakness under open sky, and no growing resistance to repeated
  hits from the same weapon (mobs always "hold" the same thing, so it became immune to them). No
  Night Shade rises from its death.

## Returning Knight (Marium's Soulslike Weaponry)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Necroturgy | Lord Soul Rose | Nightfall, Arkenstone, 2 Soul Ingots |

- Obliterate, Blinding Reflection, Rupture and Mace of Spades against any target, and its second
  phase at half health.
- **Minions**, handled like Goety's Redstone Monstrosity cubes: Remnants are tamed to you and stay
  with the knight; Dark Sorcerers beam heals into it. At most 6 at once
  (`returning_knight.maxMinions`). They vanish when the knight dies or is stored away.
- It stops about a block and a half short of its target instead of shoving it around. Its walk and idle
  animations follow whether it is actually moving.
- Its area attacks pass over friendlies. It no longer turns arrows around (that sent its owner's
  arrows back).

## Netherman (Knights Quest)

| Ritual | Altar setup | Activation item | Pedestals |
|---|---|---|---|
| Summon | Sabbath | Chaotic Essence | 2 Great Essence, Ender Pearl, Netherite Scrap |

KnightLib removes every recipe that uses an item from a feature no installed mod turns on. Knights
Quest doesn't turn on the Homunculus, so the old ritual's Homunculus is now Netherite Scrap.

- All three forms, as on the boss: Fire above 66% health, Ice to 33%, Magic below. Each change is a
  short invulnerable transformation.
- Its melee, fire barrage, curses, clones and explosive orbs come from Knights Quest. As on the boss,
  it ignores damage for its first 2 seconds.
- **Clones and orbs** are its minions. Clones burst on reaching its target, and their freeze spares
  friendlies. At most 8 clones at once (`netherman.maxClones`). They vanish when it does.
- The darkness curse only hits its target, and the fire barrage leaves lava only when
  `bossesBreakBlocks` is on.
- No summoning cinematic or "teleport to the nearest player". Its teleport when hit is a short hop
  (about 5 blocks, never down into a pit or cave) that keeps it in the fight.
- Each form attack opens early instead of after its full 10–15 s cycle: flame, freeze and darkness
  about 2.5 s after it gets a target or changes form, the barrage, clones and orbs about 3–3.5 s, then
  they repeat on Knights Quest's cycle. Its attacks teleport it around its target, so a Netherman
  told to stay still moves while fighting.

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
  - Other mods: Saints Dragons' dragons rise as themselves, never fall stunned to be tamed, and never go to sleep (a dragon sits down to fall asleep, and one that was stood up halfway stalled in its bind pose). A risen pet that sits is only stood up when it has something to do: a fight, or its monstrosity to catch up with. Raxiores' fighters rise as themselves, and its calm mobs rise as sculk zombies that keep their animations. For any mob, the entity type tags `goetybossservants:rises_as_itself` and `goetybossservants:rises_as_puppet` choose how it rises, and `goetybossservants:puppet_no_tick` stops a sculk zombie's copy of it animating.
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

## Config

`config/goetybossservants-common.toml` (Forge-style config through Forge Config API Port).

- **Per boss:** a section with `summonLimit` and the options named above.
- **`general.bossesBreakBlocks`** (default **false**): whether boss servants may break blocks. Off,
  the Night Lich's comets, the Returning Knight's smashing and the Netherman's lava leave terrain
  intact (explosions still hurt enemies).
- **Health, armor and damage** come from each boss mod's own config, as on the bosses.
- **Debug** (both off by default, and can be switched on while the game runs): `debug.logHealing`
  logs what heals a boss servant; `debug.logTargeting` logs every change to a servant's target and
  what asked for it, plus every 2 seconds its target, owner, mode and running goals. Both write to
  `logs/latest.log`.

## How the boss servants are built

Each boss servant **extends the boss mod's own class** and implements `BossServant`
(`entity/servant/`). That's Goety's `IServant` backed by a `ServantData` object instead of Goety's
Owned/Summoned fields. The boss mod's own renderer, model, animations, sounds and attacks are
reused untouched; this addon ships none of them. Each servant class:

- replaces `registerGoals()` with Goety's servant goals (without calling the boss's version);
- forwards `setTarget`, `isAlliedTo`, `hurt` and save/load to `BossServant`'s helpers, and calls
  `ownedTick()` / `servantTick()` from `tick()`;
- switches off boss-only parts (boss bar, music, loot, XP).

Shared behaviour lives in a few places:

- **Friendly fire:** `ServantEvents` (`ServerLivingEntityEvents.ALLOW_DAMAGE`) plus small hooks on
  `LivingEntity`, `Mob`, `ServerLevel` and `Explosion`.
- **Minions:** `ServantMinions`. Minions are tagged with their boss's UUID (entity tags, since
  Fabric has no persistent data), capped, fight its target, and are discarded the moment the boss
  is gone.
- **Boss-mod isolation:** every reference to a boss mod is inside that mod's `compat/*Compat` and
  `client/*Client` classes, which only run when the mod is loaded. Mixins are listed in
  `GoetyBossServantsMixinPlugin`, and rituals carry a `fabric:all_mods_loaded` load condition, so
  nothing of a missing boss mod is ever loaded.

Where a boss's code is one big inline method, a small mixin patches just the needed call.

Bosses of Mass Destruction is written in Kotlin, so its classes are final. `tools/OpenClasses.java`
makes `libs/BOMD-1.7.5-1.20.1-open.jar` (finals removed) for compiling. At runtime,
`GoetyBossServantsMixinPlugin` removes `final` from the three classes the servants extend.

## Building

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.6.7-hotspot"
.\gradlew.bat build
```

The jar lands in `build/libs/goety-boss-servants-<version>.jar`. The project uses Mojang names with
Parchment, like the Forge version, so both share most of their code. The mod jars in `libs/` are
copies from the instance (`libs/nested/` holds their jar-in-jar libraries for dev runs).

### Self-test

`.\gradlew.bat runClient -Pselftest` starts a dev client in `run-selftest/` and creates a flat
world. It summons each boss servant for a fake player and fights it against a test mob with an armor
stand standing in for the owner. It then kills and revives servants, logs `SELFTEST PASS/FAIL`
lines plus `run-selftest/selftest_result.txt`, and exits. The test code (`debug/`) is left out of the jar.

- `-PselftestFrom=<tick>` skips straight to one boss's part: `10` Cornelia, `1000` Void Blossom,
  `3200` Night Lich, `5200` Soulslike Weaponry, `6500` Netherman, `10700` the material
  monstrosities (which also saves screenshots of each skin and special to `run-selftest/screenshots/`),
  `11420` mobs rising from Sculk Decay, `11900` the Warden Core, `12010` the Black Book section.
- `-PwithoutMods=aquamirae,bmd,soulslike,knightquest` (any of them) leaves those boss mods out of the
  run. Their checks are skipped, and the test checks that their servants, souls and rituals don't exist.
- `-PwithRiseMods` also loads Raxiores and Saints Dragons (and the libraries they bundle, from
  `libs/rise-nested/`), and checks that their mobs rise as they should.

## Credits and licenses

Goety Boss Servants is an unofficial add-on. It isn't affiliated with or endorsed by the authors of
the mods below. It ships none of their files: each boss's model, textures, sounds and behaviour come
from that mod, which must be installed. The one exception is the material monstrosity skins, which
are built on Goety's Redstone Monstrosity textures (MIT) and vanilla block textures, and the
Warden Core's on the vanilla warden's.

- **Goety** by Polarice3 (MIT), Fabric port by Sh1roCu (MIT). The servant system this builds on;
  the soul items use Goety's soul texture. The material monstrosities extend Goety's Redstone Monstrosity,
  reuse its model, animations, renderer and head model, and their skins are recoloured from its textures.
- **Aquamirae** and **Fragmentum** by Obscuria (Obscuria Modding License / Obscuria Ecosystem
  License). Captain Cornelia extends Aquamirae's Cornelia, and a mixin adjusts her rage at runtime.
  Used with the authors' permission for add-ons that require their mod.
- **Bosses of Mass Destruction** by Barribob (LGPL-3.0). The Night Lich and Void Blossom servants
  extend BMD's classes. Their attack classes and renderer setup are adapted from BMD's code; those
  files carry a notice saying what was changed.
- **Marium's Soulslike Weaponry** by Marium (CC0-1.0). The Old Champion and Returning Knight
  servants extend its bosses.
- **Knights Quest** (GPL-3.0) and **KnightLib** (LGPL-3.0) by Xylonity. The Netherman servant
  extends Knights Quest's Netherman, and mixins adjust its clones and goals at runtime.
- **GeckoLib** by Gecko, Eliot, AzureDoom, DerToaster, Tslat and Witixin (MIT), used through the
  boss mods.

The jars in `libs/` are local copies of these mods for compiling and the dev self-test. They must
never be redistributed (Aquamirae's and Fragmentum's licenses allow distribution only through
CurseForge and Modrinth), and `BOMD-…-open.jar` is modified, so `libs/` is kept out of version
control by `.gitignore`.
