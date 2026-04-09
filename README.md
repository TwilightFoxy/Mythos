# Mythos

`Mythos` is a custom NeoForge mod for Entropia built around myth-based races with distinct strengths, weaknesses, and social roles.

Instead of cloning Origins one-to-one, the mod builds its own myth system with an emphasis on:
- readable myth identities
- multiplayer trade niches
- myth-specific crafting and artifacts
- clear in-game UX through a selection screen and guide book

## Stack

- Minecraft `26.1`
- NeoForge `26.1.x`
- Java `25`

## Current Version

- `0.4.2`

## Core Systems

### Myth Selection

- players who do not have a myth must choose one
- myth choice is presented through a custom selection screen
- selection summaries stay short and readable

### Tome of Mythos

- an in-game book explains each myth in plain language
- the guide includes strengths, weaknesses, crafting, controls, and edge cases
- selection stays brief, while the book gives the full breakdown

### Data-Driven Myth Definitions

- myth definitions are loaded from JSON
- powers are still executed by code, but myth content is structured for expansion

### Myth Artifacts and Forging

- several myths can craft unique items that other players can use
- forging is handled through custom smithing logic where needed

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
- mines faster, especially below `Y=0`
- depends on `Dwarven Ale`
- can temper enchanted pickaxes with a netherite ingot

### Fairy

- is much smaller than a normal player
- has half health, reduced defense, and reduced melee damage
- can hover close to the ground and transition into elytra-style flight
- can forge `Fairy Boots` and a `Fairy Minecart`
- can grant nearby players temporary night vision

### Kitsune

- can toggle mask and tails together
- at night, the mask unlocks night powers such as foxfire and heavenly wrath
- has a directional dash that works even during the day
- has reduced defense and reduced melee damage
- can forge a placed `Fox Lantern` and a `Fox Whistle`

### Siren

- always breathes underwater
- swims much faster than normal players
- sees clearly in water
- dries out on land with an inverted bubble mechanic
- can restore moisture with a plain water bottle
- can forge a `Siren Elixir` from a water bottle and a nautilus shell

## Myth Items and Utilities

Current myth-specific gear and utilities include:
- `Elven Bow`
- `Dwarven Pickaxe`
- `Dwarven Ale`
- `Fairy Boots`
- `Fairy Minecart`
- `Fox Lantern`
- `Fox Whistle`
- `Siren Elixir`
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

- add more myths with distinct economies and environments
- keep expanding the data-driven myth framework
- keep polishing visuals, controls, and in-game explanations
- refine social and multiplayer utility for each myth

## Author

- `Twily`
