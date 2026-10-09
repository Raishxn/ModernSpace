# ModernSpace Changelog

All notable changes to this project will be documented in this file.

---

## [0.1.0] - 2026-10-09

### Initial Release — Personal Dimensions for Modern Minecraft (Forge 1.20.1)

ModernSpace is a full standalone Forge 1.20.1 port and modern remaster of **PersonalSpace** from *GregTech: New Horizons*. It brings private, fully customizable pocket dimensions into modern Minecraft, powered at runtime by Infiniverse.

---

### 🌟 New Features & Highlights

#### 🌀 Personal Space Portal & Teleportation
* **Altar Gateway**: Craft and place the Personal Space Portal (obsidian base with an animated Nether Portal pool and floating enchanted spellbook).
* **One-Block Creation**: Placing an unlinked portal directly opens the in-game World Editor GUI.
* **Portable Link Memory**: Breaking an active portal retains its destination data inside the item stack, allowing players to relocate their portals anywhere in the world without losing access to their pocket realm.
* **Automatic Return Portal**: Saving your settings generates the runtime dimension and automatically spawns and links the return portal.

#### 🎨 Interactive In-Game World Editor GUI
* **Multi-Layer Terrain Builder**: Configure dimension floor layers block-by-block (Bedrock, Stone, Dirt, Grass, or any modded block) with customizable layer heights, search bar, and presets.
* **Legacy String & Registry Converter**: Full compatibility with classic GTNH preset strings and legacy `modid:name:meta` identifiers, automatically converting them to modern Minecraft resource locations and tags.
* **Atmosphere & Visual Controls**:
  * Customizable sky color via RGB picker.
  * Toggles for clouds, stars, and weather states.
  * Daylight cycle configuration: normal 24h cycle, eternal daytime, or eternal night.
* **Biome & Vegetation Control**: Pick any registered biome and toggle natural tree and flora generation.
* **Generation Lock Protection**: Terrain generation is locked upon first use to safeguard against chunk corruption. Server operators can grant **one** redesign via `/pspace allow-worldgen-change`. Visual settings (sky, clouds) can be re-tuned at any time.

#### 📐 Urban Planning: Streets & Lots Grid
* Divide your flat dimension into an organized urban grid of building plots (lots) separated by roads.
* Configurable street widths, lot sizes, and custom road/border blocks (concrete, stone, asphalt, etc.).
* Optional center marker block to easily locate the spawn origin.

#### ⚡ Tech Mod & GregTech CEu Modern Integration
* **Solar Power Compatibility (`gtceuSolarPanels`)**: GregTech CEu Modern solar panels, solar covers, and solar boilers detect sunlight and produce energy within personal space dimensions (enabled by default).
* **Wildcard Block Rules**: Server configs support path globs (e.g. `gtceu:*_casing`, `antiblocksrechiseled:*`, `#forge:stone`).
* **Safe Full-Cube Validation**: Wildcard block rules automatically validate that only solid, full-cube blocks are accepted, preventing client rendering glitches and server crashes.
* **Out-of-the-Box Mod Support**: Pre-configured support for GTCEu decorative blocks, GTCEu lamps, GTO ABS casings, and AntiBlocks Rechiseled.

#### ⌨️ Full Administration & Command Suite (`/pspace`)
Server-side permission-checked commands (Permission Level 2 / OP):
* `/pspace ls`: List all active personal space dimensions and their owners.
* `/pspace where <player>`: Inspect which dimension and coordinates a player is in.
* `/pspace tpx <player> <dim> [pos]`: Teleport players directly between dimensions.
* `/pspace give-portal <player> <dim> [pos]`: Generate pre-linked portal items for distribution or event rewards.
* `/pspace allow-worldgen-change <dim>`: Grant an operator override allowing a player one world generation redesign.
* `/pspace reload-config`: Live reload server configuration without downtime.

#### 🎨 Visual Identity & Assets
* Authorial Minecraft pixel art branding generated programmatically via Python.
* Full-resolution GitHub header banner (`banner.png`).
* Smooth 32-frame animated portal and floating book icon (`logo.gif`).
* In-game mod menu icon (`icon.png` / `logoFile` in `mods.toml`).

---

### 📦 Dependencies & Requirements

* **Minecraft**: `1.20.1`
* **Forge**: `47.1.0` or newer
* **[Infiniverse](https://www.curseforge.com/minecraft/mc-mods/infiniverse)**: `1.0.0.5` or newer (**Required on both client and server**)

---

### 🤝 Credits & Acknowledgements

* **GTNewHorizons/PersonalSpace** (LGPL-3.0) by **eigenraven** and the GTNH team for the original mod design, presets, math, and widget sheet.
* **Crazerium/PersonalSpace-Unofficial** (LGPL-3.0) by **Crazerium** for chunk generator implementation reference.
* **Infiniverse** (MIT) by **Commoble** for the runtime dimension API.
* **GregTech Nexus Addon** by **Raishxn** where the 1.20.1 port was originally developed.
