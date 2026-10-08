# ModernSpace

**Personal dimensions for modern Minecraft.** ModernSpace is a Forge 1.20.1 port of
[PersonalSpace](https://github.com/GTNewHorizons/PersonalSpace) from GregTech: New Horizons: place a portal,
design your own world (flat layers, biome, sky, weather, streets and lots) and step into it.

## Features

- **Personal Space Portal**: placing a new portal opens the world editor; saving the options creates a new
  dimension and links a return portal. Breaking a portal keeps its link in the item.
- **World editor**: block layers (with the original preset strings and legacy `modid:name:meta` names converted
  to modern IDs), biome, sky colour, stars, clouds, weather, day/night cycle, vegetation and trees, world
  borders, streets/lots and a center marker. Visual options can change after creation; generation is locked once
  used and an operator can allow **one** change.
- **Commands** (`/pspace`): `give-portal`, `tpx`, `ls`, `where`, `allow-worldgen-change`, with server-side
  permission checks.
- **Server config** at `config/modernspace/`: presets, allowed blocks and limits.
- Dimensions are created at runtime through [Infiniverse](https://github.com/Commoble/infiniverse); nothing needs
  to be declared in data packs.

## Requirements

| | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x |
| [Infiniverse](https://www.curseforge.com/minecraft/mc-mods/infiniverse) | 1.0.0.5 or newer (**required**) |

Install both jars on the client and the server.

## Recipe

By default the portal is crafted with obsidian and eyes of ender around a diamond block. Modpacks can replace
`modernspace:personal_space_portal` with KubeJS, CraftTweaker or a data pack.

## Building

```sh
./gradlew build
```
JDK 17 or newer. The jar is written to `build/libs/`.

## Credits

ModernSpace stands on the work of others. Thank you to:

- **[GTNewHorizons/PersonalSpace](https://github.com/GTNewHorizons/PersonalSpace)** (LGPL-3.0), the original
  1.7.10 mod this is a port of: dimension configuration and presets, chunk generation rules (layers, lots, streets,
  boundaries, center marker), portal, teleport and relinking logic, `/pspace` commands, the editor GUI design and
  its widget sheet (`widgets.png`, copied unchanged). Created by **eigenraven**, with contributions from
  **Dream-Master**, **ABKQPO**, **Caedis**, **alppp**, **Eldrinn-Elantey**, **Kiwi233**, **serenibyss**,
  **paulbatum**, **UltraProdigy** and the GTNH team.
- **[Crazerium/PersonalSpace-Unofficial](https://github.com/Crazerium/PersonalSpace-Unofficial)** (LGPL-3.0) by
  **Crazerium**, a Forge 1.20.1 port whose chunk generator API structure was used as an implementation reference.
- **[Infiniverse](https://github.com/Commoble/infiniverse)** (MIT) by **Commoble**, the runtime dimension API
  ModernSpace depends on. No Infiniverse code is bundled.
- **[GregTech Nexus Addon](https://github.com/Raishxn/GregTech-Nexus-Addon)**, where this port was first written
  before becoming a standalone mod.
- The **GregTech: New Horizons** community and the **Minecraft Forge** team.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.

## License

ModernSpace is licensed under the **GNU Lesser General Public License v3.0 or later**, the same license as the
original PersonalSpace. See [LICENSE](LICENSE) and [COPYING](COPYING).
