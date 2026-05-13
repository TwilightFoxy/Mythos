# Mythos

`Mythos` is a custom NeoForge mod built around myth-based races with distinct strengths, weaknesses, artifacts, and multiplayer roles.

Inspired by the general idea of fantasy origins and races, the mod builds its own myth system with an emphasis on:
- readable myth identities
- multiplayer trade niches
- myth-specific crafting and artifacts
- active abilities and visual identity
- clear in-game UX through a selection screen and guide book

## Stack

- Minecraft `26.1`
- NeoForge `26.1.x`
- Java `25`

## Current Version

- `0.7.12`

## Core Systems

### Myth Selection

- players who do not have a myth must choose one
- myth choice is presented through a custom selection screen
- long myth entries in the selection screen can be scrolled
- admins can force, clear, or assign myths with commands
- players can be reset through a one-use `Myth Sphere`
- selection summaries stay short and readable
- hidden utility myths can exist for admin-only command workflows
- the mod requires both client and server

### Tome of Mythos

- an in-game book explains each myth in plain language
- the guide includes strengths, weaknesses, crafting, controls, edge cases, and exact values
- myth crafting pages now show visual recipe cards for crafting table and smithing table workflows
- recipe cards also show where the item is made
- selection stays brief, while the book gives the full breakdown
- growth is shown as a separate section instead of being buried inside strengths

### Client Configuration

- Mythos exposes a built-in NeoForge config screen from the mod list
- kitsune mask and tail colors can be set to `default`, `auto`, or `manual`
- manual color uses a `#RRGGBB` value

### Data-Driven Myth Definitions

- myth definitions are loaded from JSON
- myths can inherit from other myths through `inherits`
- hidden command-only variants can extend an existing myth without appearing in the normal selection menu
- powers are still executed by code, but myth content is structured for expansion

### Myth Artifacts and Forging

- several myths can craft unique items that other players can use
- forging is handled through custom smithing logic where needed
- smithing access is limited by myth where appropriate
- reinforced shulker boxes use their own container line, loot handling, recolors, and tooltip support

## Current Myths

### Human

- receives near-minimum villager prices
- temporarily doubles villager trade stock while trading
- can see remaining uses of the selected villager trade

### Elf

- gains speed in forest biomes and cherry groves
- deals higher bow and arrow damage
- deals reduced axe damage
- can forge the `Elven Bow`

### Dwarf

- has a shorter body
- always has `Haste I`, and `Haste II` below `Y=0`
- depends on `Dwarven Ale` and suffers visible withdrawal without it
- uses named withdrawal stages and visible slowdown instead of hidden penalties
- can temper enchanted pickaxes with a netherite ingot
- now crafts `1` ale per recipe, and the honey bottle is fully consumed by crafting

### Fairy

- is much smaller than a normal player
- has half health and reduced melee damage
- keeps `Fairy Wings` in the chest slot instead of armor
- switches manually between free flight near support and full vanilla elytra flight
- can eat any flower, including the wither rose
- can forge `Fairy Boots` and a `Fairy Minecart`
- can grant nearby players temporary night vision
- `Fairy Vision` lasts 3 minutes and has a 30-second cooldown
- the `Fairy Minecart` has tuned high-speed rail handling for turns, slopes, and launch ramps
- ignores elytra collision damage while using its fairy elytra mode

### Kitsune

- can toggle mask and tails together
- at night, the mask unlocks night powers such as foxfire and heavenly wrath
- has a directional dash with a `4` second cooldown that works even during the day
- has reduced defense and reduced melee damage
- can forge a placed `Fox Lantern`
- mask and tail colors can be customized from the client config screen
- visible foxfire and invisible-state visuals are handled separately

### Siren

- always breathes underwater
- swims much faster than normal players
- sees clearly in water
- dries out on land with an inverted bubble mechanic
- can restore moisture with a plain water bottle
- can forge a `Siren Elixir` from a water bottle and a nautilus shell
- can use marine enchantments in reverse on land
- treats water cauldrons as a valid moisture source
- `Siren Elixir` lasts 15 minutes

### Oni

- deals `+30%` melee damage with swords and axes
- deals `-30%` ranged damage with bows and crossbows
- can activate a 3-minute battle form with increased size, extra hearts, full knockback resistance, and hostile glow sensing
- battle form has a heavy backlash when it ends and then goes on cooldown
- now shows a dedicated oni mask during battle form
- can forge a `Rage Talisman`

### Slimeborn

- changes mass stage based on occupied inventory slots
- uses four stages with heights of `1.0`, `1.5`, `2.0`, and `3.0` blocks
- each stage has its own visible status effect and stat package
- is fully immune to fall damage
- can grant wall-climbing through `Clinging Gel`
- uses a translucent slime-shell visual instead of a standard player silhouette
- splits into slime offspring on death depending on its current mass stage

### Shulkerborn

- Wardens always drop `1` `Resonance Shard`
- gains a personal 4th inventory row with `9` extra slots
- becomes `Overloaded (Slowness I)` at `37+` occupied slots
- stores its extra row separately and drops it together with the rest of the inventory on death
- is built around a late shellcraft progression instead of a direct End rush
- can craft a pre-End shulker box and upgrade it into a `Reinforced Shulker Box`
- reinforced shulkers support colors, hopper interaction, loot preservation, and tooltip previews

### Spirit

- deals `+30%` damage at night with normal defense
- keeps normal damage during the day, but loses `30%` armor and `30%` armor toughness
- becomes visibly ghostlike during the day
- uses a hold-and-release `Phase Transition` that previews the exit point before teleporting
- applies brief darkness during the transition and short fall / wall-impact protection afterward
- can forge an `Ethereal Candle`
- `Ethereal Candle` grants temporary ghostly scaffolding that appears under the user while moving

### Fireborn

- replaces normal hunger gameplay with a `5`-stage Heat resource
- is always immune to fire, lava, and magma
- can eat coal and charcoal, absorb lava buckets, and passively recover heat from fire, campfires, and lava
- becomes heavily debilitated below stable heat and loses access to abilities there
- gains visible speed and strength at high heat
- unlocks `Fireball` and `Fire Ring` at heated and overheated stages
- gets one effectively free cast when the heat bar is completely full
- can walk on lava and sink through it while sneaking
- can forge an `Ifrit Lighter`

## Myth Items and Utilities

Current myth-specific gear and utilities include:
- `Elven Bow`
- `Dwarven Pickaxe`
- `Dwarven Ale`
- `Fairy Wings`
- `Fairy Boots`
- `Fairy Minecart`
- `Fox Lantern`
- `Siren Elixir`
- `Rage Talisman`
- `Clinging Gel`
- `Resonance Shard`
- `Reinforced Shulker Box`
- `Ethereal Candle`
- `Ifrit Lighter`
- `Myth Sphere`
- `Tome of Mythos`

## Hidden Variants

- `player`
  A utility myth for manual admin-side scaling and testing.

- `femboy_kitsune`
  A hidden kitsune variant with its own joke utility behavior and login lightning toggle.

- `arch_fairy`
  A hidden fairy variant that can manually shrink into a tiny speed-boosted form.

## Testing Workflow

- significant gameplay changes are built before handoff
- test jars are copied into the dedicated Modrinth profile at:
  `/Users/twilightfoxy/Library/Application Support/ModrinthApp/profiles/26.1 ресурспак тест/mods/`
- the previous active Mythos jar is disabled so the profile always runs a single current build

## Project Layout

- `src/main/java/com/twily/mythos`
  Main gameplay logic, networking, client rendering, and UI

- `src/main/resources/data/mythos/myths`
  Myth definitions

- `src/main/resources/data/mythos/recipe`
  Myth-specific smithing and crafting recipes

- `src/main/resources/assets/mythos/lang`
  Localizations

- `src/main/resources/assets/mythos/textures/gui`
  Myth logos and GUI art

## Build

```bash
./gradlew build
```

Build output:

```text
build/libs/
```

## Roadmap

- stabilize and freeze existing myths one by one
- expand automated and manual test coverage for myth-specific systems
- add more myths with distinct economies and environments
- keep expanding the data-driven myth framework
- keep polishing visuals, controls, and in-game explanations
- refine social and multiplayer utility for each myth
- keep turning ad-hoc behavior into stable owned systems and config-backed features

## Author

- `Twily`
