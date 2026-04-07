# Mythos

`Mythos` is a custom NeoForge mod for Entropia built around myth-based origins.

The project does not try to port Origins directly. Instead, it builds its own myth system with an emphasis on:
- clear myth identities
- multiplayer roles and trade niches
- readable in-game UX
- myth-specific crafting, items, and progression hooks

## Stack

- Minecraft `1.26.1`
- NeoForge `26.1.x`
- Java `25`

## Current State

The mod already includes:
- a myth selection screen
- an in-game guide book
- data-driven myth definitions
- myth-specific crafting and item upgrades
- several playable myths with different roles

## Current Myths

### Human
- near-minimum villager prices
- doubled villager trade stock during trading
- visible remaining trade count in the trade screen

### Elf
- speed in forest biomes and cherry grove
- higher bow and arrow damage
- reduced axe damage
- access to the `Elven Bow`

### Dwarf
- reduced body size
- faster mining, especially below `Y=0`
- ale dependency
- access to dwarven pickaxe tempering

### Fairy
- tiny body size
- half maximum health
- reduced melee damage
- low-altitude free flight
- soft glide and firework boosting above the flight limit

## In-Game Systems

### Myth Selection
- myth choice UI inspired by Origins, but adapted for Mythos
- brief myth summaries in selection
- complexity display

### Tome of Mythos
- detailed myth guide book
- explained mechanics in plain language
- unique crafting and myth-specific notes

### Myth Items
- named myth-specific items such as:
  - `Elven Bow`
  - `Dwarven Pickaxe`
  - `Dwarven Ale`
- custom tooltips for special items

## Project Layout

- `src/main/java/com/twily/mythos`  
  Main mod code, gameplay handlers, client UI, networking

- `src/main/resources/data/mythos/myths`  
  Myth definitions

- `src/main/resources/assets/mythos/lang`  
  Localizations

- `src/main/resources/assets/mythos/textures/gui`  
  GUI logos and myth art

## Build

```bash
./gradlew build
```

Build output:

```text
build/libs/
```

Current mod version:
- `0.2.5`

## Roadmap

- add more myths
- expand the data-driven myth framework
- polish movement-heavy myths such as Fairy
- improve special economy loops between myths
- keep refining the in-game guide and selection UX

## Author

- `Twily`
