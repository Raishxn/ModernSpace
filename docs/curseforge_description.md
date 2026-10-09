![ModernSpace Banner](https://raw.githubusercontent.com/Raishxn/ModernSpace/main/banner.png)

# 🌌 ModernSpace

**ModernSpace brings personal, fully customizable pocket dimensions to modern Minecraft (Forge 1.20.1).**

It is a dedicated Forge 1.20.1 port and modern remaster of the legendary **[PersonalSpace](https://github.com/GTNewHorizons/PersonalSpace)** mod from *GregTech: New Horizons*. 

Craft a portal, place it down to launch the in-game world editor, design your private realm—flat terrain layers, biomes, sky colors, weather, daylight cycles, streets and urban building lots—and step right into your isolated pocket dimension.

---

## 💡 Why ModernSpace?

* 🏠 **Lag-Free Private Bases**: Move heavy machinery, complex automation, and sprawling multiblock setups into dedicated runtime dimensions, eliminating tick lag and FPS drops in the Overworld.
* 📐 **Urban Planning & Plot Grids**: The built-in streets and lots system provides crisp, configurable grid lines for factory plots, clean base layouts, and multiplayer claims.
* ⚡ **Zero Data Pack Hassles**: Dimensions are created dynamically at runtime via **Infiniverse**—no server restarts, dimension registration configs, or data pack reloads required.
* 🛡️ **Faithful to GTNH**: Preserves original chunk generator algorithms, widget designs, and preset strings, seamlessly converted to modern Minecraft registry IDs and tags.

---

## ✨ Features

### 🌀 Personal Space Portal & Portable Links
* **Altar Gateway**: Crafted with obsidian and eyes of ender surrounding a diamond block.
* **Instant Creation**: Placing an unlinked portal directly opens the in-game **World Editor GUI**.
* **Portable Link Memory**: Breaking an active portal safely keeps its dimensional destination stored inside the item. Relocate your portal anywhere in the world without losing access to your dimension!
* **Automatic Return**: Finalizing your settings generates the runtime dimension and automatically spawns and links the return portal.

### 🎨 In-Game World Editor GUI
* **Multi-Layer Terrain Builder**: Configure dimension floor layers block-by-block (Bedrock, Stone, Dirt, Grass, or any modded block) with customizable layer heights, search bar, and presets.
* **Atmosphere & Visual Controls**:
  * Customizable sky color via RGB picker.
  * Toggles for clouds, stars, and weather states.
  * Daylight cycle control: normal 24h cycle, eternal daytime, or eternal night.
* **Biome & Vegetation Control**: Pick any registered biome and toggle natural tree and flora generation.
* **Generation Lock**: Terrain generation is safely locked once initialized to prevent chunk corruption. Server operators can grant **one** redesign via `/pspace allow-worldgen-change`. Visual options (sky, clouds) can be re-tuned at any time.

### 📐 Streets & Lots Grid System
* Divides your flat world into an organized urban grid of building lots separated by roads.
* Configurable street widths, lot sizes, and custom road/border blocks (concrete, stone, asphalt, etc.).
* Optional center marker block to easily pinpoint the spawn origin.

### ⚡ Tech Mod & GregTech CEu Modern Integration
* **Solar Power Compatibility (`gtceuSolarPanels`)**: GregTech CEu Modern solar panels, solar covers, and solar boilers detect sunlight and generate power inside personal spaces (enabled by default).
* **Wildcard Block Rules**: Server configs support path globs (e.g., `gtceu:*_casing`, `antiblocksrechiseled:*`, `#forge:stone`).
* **Safe Full-Cube Validation**: Wildcard block rules automatically validate and restrict to solid full-cube blocks to prevent rendering glitches and server crashes.
* **Out-of-the-Box Mod Support**: Built-in support for GTCEu decorative blocks, GTCEu lamps, GTO ABS casings, and AntiBlocks Rechiseled.

---

## 🎮 Quick Start Guide

1. **Craft the Portal**: Craft the `Personal Space Portal` using obsidian and eyes of ender around a diamond block.
2. **Place & Configure**: Place the portal block down on the ground. The interactive **World Editor GUI** will automatically open.
3. **Choose Your Realm**: Pick a preset (e.g. Void, Garden, Mining) or tailor your layers, sky color, biome, and lots.
4. **Step Through**: Save your settings and walk into the portal to enter your private pocket dimension!

---

## ⌨️ Operator Commands (`/pspace`)

All commands require Permission Level 2 (OP):

* `/pspace ls` — Lists all active personal space dimensions and their owners.
* `/pspace where <player>` — Displays which dimension and coordinates a player is currently in.
* `/pspace tpx <player> <dim> [pos]` — Teleports a player to a personal space dimension.
* `/pspace give-portal <player> <dim> [pos]` — Gives a pre-linked portal item pointing to the target dimension.
* `/pspace allow-worldgen-change <dim>` — Grants permission for **one** world generation modification in the editor.
* `/pspace reload-config` — Reloads `config/modernspace/personal_space.json` live without restarting the server.

---

## 📦 Requirements & Installation

| Component | Required Version |
| :--- | :--- |
| **Minecraft** | `1.20.1` |
| **Forge** | `47.1.0` or newer |
| **[Infiniverse](https://www.curseforge.com/minecraft/mc-mods/infiniverse)** | `1.0.0.5` or newer (**Required on both client and server**) |

---

## 🛠️ Modpack Policy

ModernSpace is licensed under the **GNU Lesser General Public License v3.0 or later** (LGPL-3.0-or-later). You are completely free to include ModernSpace in any public or private modpack on CurseForge, Modrinth, or other launchers. Customizing the recipe via KubeJS, CraftTweaker, or data packs is fully supported!

---

## 🤝 Credits & Acknowledgements

* **[GTNewHorizons/PersonalSpace](https://github.com/GTNewHorizons/PersonalSpace)** (LGPL-3.0) by **eigenraven** and the GTNH team for the original mod design, presets, math, and widget sheet.
* **[Crazerium/PersonalSpace-Unofficial](https://github.com/Crazerium/PersonalSpace-Unofficial)** (LGPL-3.0) by **Crazerium** for chunk generator implementation reference.
* **[Infiniverse](https://github.com/Commoble/infiniverse)** (MIT) by **Commoble** for the runtime dimension API.
* **[GregTech Nexus Addon](https://github.com/Raishxn/GregTech-Nexus-Addon)** by **Raishxn** where the 1.20.1 port was originally developed.
