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

- `0.4.36`

## Core Systems

### Myth Selection

- players who do not have a myth must choose one
- myth choice is presented through a custom selection screen
- admins can force, clear, or assign myths with commands
- players can be reset through a one-use `Myth Sphere`
- selection summaries stay short and readable
- the mod requires both client and server

### Tome of Mythos

- an in-game book explains each myth in plain language
- the guide includes strengths, weaknesses, crafting, controls, and edge cases
- selection stays brief, while the book gives the full breakdown

### Client Configuration

- Mythos exposes a built-in NeoForge config screen from the mod list
- kitsune mask and tail colors can be set to `default`, `auto`, or `manual`
- manual color uses a `#RRGGBB` value

### Data-Driven Myth Definitions

- myth definitions are loaded from JSON
- powers are still executed by code, but myth content is structured for expansion

### Myth Artifacts and Forging

- several myths can craft unique items that other players can use
- forging is handled through custom smithing logic where needed
- smithing access is limited by myth where appropriate

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
- can temper enchanted pickaxes with a netherite ingot

### Fairy

- is much smaller than a normal player
- has half health, reduced defense, and reduced melee damage
- can hover close to the ground and transition into elytra-style flight
- can forge `Fairy Boots` and a `Fairy Minecart`
- can grant nearby players temporary night vision
- `Fairy Vision` lasts 3 minutes and has a 30-second cooldown
- the `Fairy Minecart` has tuned high-speed rail handling for turns, slopes, and launch ramps

### Kitsune

- can toggle mask and tails together
- at night, the mask unlocks night powers such as foxfire and heavenly wrath
- has a directional dash that works even during the day
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
- `Siren Elixir` lasts 15 minutes

## Myth Items and Utilities

Current myth-specific gear and utilities include:
- `Elven Bow`
- `Dwarven Pickaxe`
- `Dwarven Ale`
- `Fairy Boots`
- `Fairy Minecart`
- `Fox Lantern`
- `Siren Elixir`
- `Myth Sphere`
- `Tome of Mythos`

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
